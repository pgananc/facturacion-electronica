import {
  Component,
  OnDestroy,
  OnInit,
  AfterViewInit,
  ViewChild,
} from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { ToastService } from '../../../_service/notification/toast.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, takeUntil, fromEvent, Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { InvoiceService } from '../../../_service/invoice/invoice.service';
import { CreditNoteService } from '../../../_service/invoice/credit-note.service';
import {
  DocumentQueryDto,
  DocumentSearchResponseDto,
} from '../../../_model/invoice/document-query-dto';
import { environment } from '../../../../environments/environment';

interface InvoiceListItem {
  id: number;
  idInvoice?: number;
  idCreditNote?: number;
  number: string;
  date: string;
  nameClient: string;
  total: number;
  authorizationStatusSri: string;
  authorizationNumber: string;
  storagePath: string;
  documentType: 'INVOICE' | 'CREDIT_NOTE';
  documentTypeNumber?: number; // 1 = Factura, 2 = Nota de crédito
}

@Component({
  selector: 'app-viewing-invoices',
  templateUrl: './viewing-invoices.component.html',
  styleUrls: ['./viewing-invoices.component.css'],
})
export class ViewingInvoicesComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  displayedColumns: string[] = [
    'documentType',
    'number',
    'date',
    'nameClient',
    'total',
    'authorizationStatusSri',
    'actions',
  ];
  dataSource = new MatTableDataSource<InvoiceListItem>([]);
  quantity = 0;
  pageSize = 10;

  searchForm: FormGroup;

  private readonly destroy$ = new Subject<void>();

  // Control de inactividad
  private inactivityTimer: any = null;
  private lastActivityTime: number = Date.now();
  private activitySubscriptions: Subscription[] = [];
  private readonly inactivityTimeout = environment.INACTIVITY_TIMEOUT_MS;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private readonly invoiceService: InvoiceService,
    private readonly creditNoteService: CreditNoteService,
    private readonly toast: ToastService,
    private readonly fb: FormBuilder,
    private readonly router: Router
  ) {
    this.searchForm = this.fb.group({
      documentType: ['0', Validators.required], // 0 = Todos, 1 = Factura, 2 = Nota de crédito
      invoiceNumber: [''],
      buyerIdentification: [{ value: '', disabled: true }],
      receptionStatusSri: [''],
      creationDateFrom: [null, Validators.required],
      creationDateTo: [null, Validators.required],
    });
  }

  ngOnInit(): void {
    // Check if we have a stored identification number from the consultation entry
    const storedId = localStorage.getItem('consultationIdentificationNumber');
    if (storedId) {
      // Pre-fill the buyer identification field with the stored identification number
      this.searchForm.patchValue({
        buyerIdentification: storedId,
      });
    }

    // Limpiar tabla cuando cambie el tipo de comprobante
    this.searchForm
      .get('documentType')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.dataSource = new MatTableDataSource<InvoiceListItem>([]);
        this.quantity = 0;
      });

    // Iniciar detección de inactividad
    this.startInactivityTimer();
    this.setupActivityListeners();
  }

  ngAfterViewInit(): void {
    // Asegurar que el sort y paginator estén asignados después de la inicialización de la vista
    if (this.dataSource) {
      this.dataSource.sort = this.sort;
      this.dataSource.paginator = this.paginator;

      // Configurar sorting personalizado para fechas y números
      this.dataSource.sortingDataAccessor = (
        item: InvoiceListItem,
        property: string
      ): string | number => {
        switch (property) {
          case 'date':
            return new Date(item.date).getTime();
          case 'total':
            return item.total;
          case 'number':
            return item.number;
          case 'nameClient':
            return item.nameClient.toLowerCase();
          case 'authorizationStatusSri':
            return item.authorizationStatusSri.toLowerCase();
          default:
            const value = item[property as keyof InvoiceListItem];
            if (value === undefined || value === null) {
              return '';
            }
            if (typeof value === 'string' || typeof value === 'number') {
              return value;
            }
            return String(value);
        }
      };
    }
  }

  ngOnDestroy(): void {
    // Limpiar listeners y timer de inactividad
    this.stopInactivityTimer();
    this.removeActivityListeners();

    this.destroy$.next();
    this.destroy$.complete();
  }

  applyFilter(): void {
    if (this.searchForm.valid) {
      this.loadDocuments(0, this.pageSize);
    }
  }

  get isFormValid(): boolean {
    const dateFrom = this.searchForm.get('creationDateFrom')?.value;
    const dateTo = this.searchForm.get('creationDateTo')?.value;
    return !!dateFrom && !!dateTo;
  }

  clearFilters(): void {
    const buyerId = this.searchForm.get('buyerIdentification')?.value || '';
    this.searchForm.reset({
      documentType: '0',
      invoiceNumber: '',
      buyerIdentification: buyerId,
      receptionStatusSri: '',
      creationDateFrom: null,
      creationDateTo: null,
    });
    // Restaurar el estado disabled del campo buyerIdentification
    this.searchForm.get('buyerIdentification')?.disable();

    // Limpiar datos de la tabla
    this.dataSource = new MatTableDataSource<InvoiceListItem>([]);
    this.quantity = 0;
  }

  goBackToConsultation(): void {
    // Clear the stored identification number and navigate back with reset flag
    localStorage.removeItem('consultationIdentificationNumber');
    localStorage.removeItem('clientEmail');
    localStorage.removeItem('clientToken');
    this.router.navigate(['/invoice-consultation'], {
      queryParams: { reset: 'true' },
    });
  }

  showMore(event: any): void {
    this.loadDocuments(event.pageIndex, event.pageSize);
  }

  private loadDocuments(page: number, size: number): void {
    const queryRequest: DocumentQueryDto = this.buildDocumentQueryRequest();
    queryRequest.receptionStatusSri = 'RECIBIDA';
    queryRequest.authorizationStatusSri = 'AUTORIZADO';

    const token = localStorage.getItem(environment.TOKEN);

    if (!token) {
      this.toast.error(
        'Sesión no válida. Por favor, inicie sesión nuevamente.'
      );
      this.router.navigate(['/login']);
      return;
    }

    this.invoiceService
      .queryDocuments(page, size, queryRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          // Handle both pagination response and direct DTO response
          if (response.content && response.totalElements !== undefined) {
            // Standard pagination response
            this.quantity = response.totalElements;
            const items: InvoiceListItem[] = (response.content ?? []).map(
              (doc: DocumentSearchResponseDto) => {
                const isInvoice = doc.documentType === 1;
                return {
                  id: doc.idDocument ?? 0,
                  idInvoice: isInvoice ? doc.idDocument : undefined,
                  idCreditNote: !isInvoice ? doc.idDocument : undefined,
                  number: doc.documentNumber ?? '-',
                  date: doc.creationDate ?? '-',
                  nameClient: doc.nameClient ?? '-',
                  total: doc.totalAmount ?? 0,
                  authorizationStatusSri:
                    doc.authorizationStatusSri ?? doc.receptionStatusSri ?? '-',
                  authorizationNumber: doc.authorizationNumber ?? '',
                  storagePath: '',
                  documentType: isInvoice ? 'INVOICE' : 'CREDIT_NOTE',
                  documentTypeNumber: doc.documentType,
                };
              }
            );
            this.dataSource = new MatTableDataSource(items);
            this.setupDataSource();
          } else if (Array.isArray(response)) {
            // Direct array response
            this.quantity = response.length;
            const items: InvoiceListItem[] = (response ?? []).map(
              (doc: DocumentSearchResponseDto) => {
                const isInvoice = doc.documentType === 1;
                return {
                  id: doc.idDocument ?? 0,
                  idInvoice: isInvoice ? doc.idDocument : undefined,
                  idCreditNote: !isInvoice ? doc.idDocument : undefined,
                  number: doc.documentNumber ?? '-',
                  date: doc.creationDate ?? '-',
                  nameClient: doc.nameClient ?? '-',
                  total: doc.totalAmount ?? 0,
                  authorizationStatusSri:
                    doc.authorizationStatusSri ?? doc.receptionStatusSri ?? '-',
                  authorizationNumber: doc.authorizationNumber ?? '',
                  storagePath: '',
                  documentType: isInvoice ? 'INVOICE' : 'CREDIT_NOTE',
                  documentTypeNumber: doc.documentType,
                };
              }
            );
            this.dataSource = new MatTableDataSource(items);
            this.setupDataSource();
          } else {
            // Fallback for unexpected response format
            this.quantity = 0;
            this.dataSource = new MatTableDataSource<InvoiceListItem>([]);
            this.setupDataSource();
          }
        },
        error: (error) => {
          // Limpiar datos de la tabla cuando hay un error
          this.quantity = 0;
          this.dataSource = new MatTableDataSource<InvoiceListItem>([]);
          this.setupDataSource();

          if (error.status === 401) {
            this.toast.error(
              'Sesión expirada. Por favor, inicie sesión nuevamente.'
            );
          } else if (error.status === 429) {
            // Intentar extraer el mensaje del backend
            // Nota: Si hay error de CORS, error.error puede estar vacío
            let rateLimitMessage =
              'Se ha excedido el límite de solicitudes. Por favor, espera unos momentos e intenta nuevamente.';

            // Intentar obtener el mensaje del backend en diferentes ubicaciones
            if (error?.error?.message) {
              rateLimitMessage = error.error.message;
            } else if (typeof error?.error === 'string' && error.error.trim()) {
              rateLimitMessage = error.error;
            } else if (
              error?.message &&
              !error.message.includes('Http failure response')
            ) {
              // Solo usar error.message si no es un mensaje técnico de Angular
              rateLimitMessage = error.message;
            }

            this.toast.warning(rateLimitMessage);
          } else {
            this.toast.error(
              'Error al cargar documentos. Intente nuevamente.'
            );
          }
        },
      });
  }

  private buildDocumentQueryRequest(): DocumentQueryDto {
    // Usar getRawValue() para incluir campos deshabilitados
    const formValue = this.searchForm.getRawValue();

    // Formatear fechas como string en formato ISO (YYYY-MM-DD)
    const formatDate = (date: Date | null): string | undefined => {
      if (!date) return undefined;
      const d = new Date(date);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    // Convertir documentType de string a number (0, 1, o 2)
    const documentType = formValue.documentType
      ? parseInt(formValue.documentType, 10)
      : 0;

    return {
      documentNumber: formValue.invoiceNumber || '',
      buyerIdentification: formValue.buyerIdentification || '',
      creationDateFrom: formatDate(formValue.creationDateFrom),
      creationDateTo: formatDate(formValue.creationDateTo),
      documentType: documentType,
      receptionStatusSri: formValue.receptionStatusSri || '',
    };
  }

  private setupDataSource(): void {
    if (this.dataSource) {
      if (this.sort) {
        this.dataSource.sort = this.sort;
      }
      if (this.paginator) {
        this.dataSource.paginator = this.paginator;
      }

      // Configurar sorting personalizado
      this.dataSource.sortingDataAccessor = (
        item: InvoiceListItem,
        property: string
      ): string | number => {
        switch (property) {
          case 'date':
            return new Date(item.date).getTime();
          case 'total':
            return item.total;
          case 'number':
            return item.number;
          case 'nameClient':
            return item.nameClient.toLowerCase();
          case 'authorizationStatusSri':
            return item.authorizationStatusSri.toLowerCase();
          default:
            const value = item[property as keyof InvoiceListItem];
            if (value === undefined || value === null) {
              return '';
            }
            if (typeof value === 'string' || typeof value === 'number') {
              return value;
            }
            return String(value);
        }
      };
    }
  }

  downloadPdf(
    idInvoice: number,
    invoiceNumber: string,
    documentTypeNumber?: number
  ): void {
    // documentType: 1 = Factura, 2 = Nota de Crédito
    const isCreditNote = documentTypeNumber === 2;
    const service = isCreditNote
      ? this.creditNoteService.downloadPdf(idInvoice)
      : this.invoiceService.downloadPdf(idInvoice);

    service.subscribe({
      next: (blob: Blob) => {
        // Verificar si el blob es realmente un PDF o un error JSON
        if (blob.type === 'application/json' || blob.size < 100) {
          // Es probablemente un error, leer el contenido
          blob.text().then((text) => {
            try {
              const error = JSON.parse(text);
              this.toast.error(
                error.message || 'Error al descargar el PDF'
              );
            } catch (e) {
              this.toast.error(
                'Error al descargar el PDF. Intente nuevamente.'
              );
            }
          });
          return;
        }

        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        const cleanFileName = invoiceNumber.replace(/[^a-zA-Z0-9-]/g, '_');
        link.download = `${cleanFileName}.pdf`;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
        this.toast.success('PDF descargado exitosamente');
      },
      error: async (error) => {
        // Si el error es un Blob, intentar leerlo
        if (error.error instanceof Blob) {
          try {
            const errorText = await error.error.text();
            const errorJson = JSON.parse(errorText);
            this.toast.error(
              errorJson.message || 'Error al descargar el PDF'
            );
          } catch (e) {
            this.toast.error(
              'Error al descargar el PDF. Intente nuevamente.'
            );
          }
        } else {
          this.toast.error(
            error.error?.message ||
              'Error al descargar el PDF. Intente nuevamente.'
          );
        }
      },
    });
  }

  downloadXml(
    idInvoice: number,
    authorizationNumber: string,
    documentTypeNumber?: number
  ): void {
    // documentType: 1 = Factura, 2 = Nota de Crédito
    const isCreditNote = documentTypeNumber === 2;
    const service = isCreditNote
      ? this.creditNoteService.downloadXml(idInvoice)
      : this.invoiceService.downloadXml(idInvoice);

    service.subscribe({
      next: (blob: Blob) => {
        // Verificar si el blob es realmente un XML o un error JSON
        if (blob.type === 'application/json' || blob.size < 100) {
          // Es probablemente un error, leer el contenido
          blob.text().then((text) => {
            try {
              const error = JSON.parse(text);
              this.toast.error(
                error.message || 'Error al descargar el XML'
              );
            } catch (e) {
              this.toast.error(
                'Error al descargar el XML. Intente nuevamente.'
              );
            }
          });
          return;
        }

        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        const cleanFileName = authorizationNumber
          ? authorizationNumber.replace(/[^a-zA-Z0-9-]/g, '_')
          : `documento-${idInvoice}`;
        link.download = `${cleanFileName}.xml`;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
        this.toast.success('XML descargado exitosamente');
      },
      error: async (error) => {
        // Si el error es un Blob, intentar leerlo
        if (error.error instanceof Blob) {
          try {
            const errorText = await error.error.text();
            const errorJson = JSON.parse(errorText);
            this.toast.error(
              errorJson.message || 'Error al descargar el XML'
            );
          } catch (e) {
            this.toast.error(
              'Error al descargar el XML. Intente nuevamente.'
            );
          }
        } else {
          this.toast.error(
            error.error?.message ||
              'Error al descargar el XML. Intente nuevamente.'
          );
        }
      },
    });
  }

  /**
   * Imprime el recibo/ticket de la factura
   * Solo funciona para facturas (documentTypeNumber === 1)
   */
  printTicket(idInvoice: number, documentTypeNumber?: number): void {
    // Solo permitir imprimir facturas, no notas de crédito
    if (documentTypeNumber === 2) {
      this.toast.warning(
        'No se puede imprimir recibo para notas de crédito'
      );
      return;
    }

    const relativeUrl = `/invoices/ticket/${idInvoice}`;
    const fullUrl = `${window.location.origin}${relativeUrl}`;

    try {
      const printWindow = window.open(
        relativeUrl,
        '_blank',
        'width=400,height=600'
      );

      if (
        !printWindow ||
        printWindow.closed ||
        typeof printWindow.closed === 'undefined'
      ) {
        // Si falla, intentar con URL completa
        const printWindow2 = window.open(
          fullUrl,
          '_blank',
          'width=400,height=600'
        );

        if (
          !printWindow2 ||
          printWindow2.closed ||
          typeof printWindow2.closed === 'undefined'
        ) {
          this.toast.error(
            'No se pudo abrir la ventana de impresión. Verifique el bloqueador de ventanas emergentes.'
          );
        }
      }
    } catch (error) {
      this.toast.error('Error al abrir la ventana de impresión');
    }
  }

  /**
   * Configura los listeners de actividad del usuario
   * Detecta eventos de mouse, teclado, scroll, touch y click
   */
  private setupActivityListeners(): void {
    // Eventos que indican actividad del usuario
    const events = [
      'mousedown',
      'mousemove',
      'keypress',
      'scroll',
      'touchstart',
      'click',
    ];

    events.forEach((eventName) => {
      const subscription = fromEvent(document, eventName)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          this.resetInactivityTimer();
        });

      this.activitySubscriptions.push(subscription);
    });
  }

  /**
   * Remueve los listeners de actividad
   */
  private removeActivityListeners(): void {
    this.activitySubscriptions.forEach((sub) => sub.unsubscribe());
    this.activitySubscriptions = [];
  }

  /**
   * Inicia el timer de inactividad
   */
  private startInactivityTimer(): void {
    this.lastActivityTime = Date.now();
    this.checkInactivity();
  }

  /**
   * Reinicia el timer cuando hay actividad del usuario
   */
  private resetInactivityTimer(): void {
    this.lastActivityTime = Date.now();
  }

  /**
   * Verifica periódicamente si el usuario está inactivo
   */
  private checkInactivity(): void {
    if (this.inactivityTimer) {
      clearTimeout(this.inactivityTimer);
    }

    this.inactivityTimer = setTimeout(() => {
      const timeSinceLastActivity = Date.now() - this.lastActivityTime;

      if (timeSinceLastActivity >= this.inactivityTimeout) {
        // Usuario inactivo por más del tiempo permitido
        this.handleInactivity();
      } else {
        // Continuar verificando
        this.checkInactivity();
      }
    }, 1000); // Verificar cada segundo
  }

  /**
   * Detiene el timer de inactividad
   */
  private stopInactivityTimer(): void {
    if (this.inactivityTimer) {
      clearTimeout(this.inactivityTimer);
      this.inactivityTimer = null;
    }
  }

  /**
   * Maneja la inactividad: cierra sesión y redirige
   */
  private handleInactivity(): void {
    // Limpiar el timer
    this.stopInactivityTimer();

    // Mostrar mensaje al usuario
    this.toast.warning(
      'Sesión cerrada por inactividad. Por favor, inicie el proceso nuevamente.'
    );

    // Limpiar el token
    localStorage.removeItem(environment.TOKEN);

    // Limpiar el número de identificación almacenado
    localStorage.removeItem('consultationIdentificationNumber');

    // Redirigir a invoice-consultation-entry
    setTimeout(() => {
      this.router.navigate(['/invoice-consultation']);
    }, 500);
  }
}
