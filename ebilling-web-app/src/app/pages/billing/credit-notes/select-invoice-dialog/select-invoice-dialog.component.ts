import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Inject,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastService } from '../../../../_service/notification/toast.service';
import { Subject, of } from 'rxjs';
import {
  takeUntil,
  debounceTime,
  distinctUntilChanged,
  catchError,
} from 'rxjs/operators';
import { InvoiceService } from '../../../../_service/invoice/invoice.service';
import { InvoiceDto, InvoiceSearchForDebitNoteDto, InvoiceSearchForCreditNoteDto, InvoiceForDebitNoteResponseDto } from '../../../../_model/invoice/invoice-dto';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { environment } from '../../../../../environments/environment';

export interface SelectInvoiceDialogData {
  hideAvailableAmount?: boolean; // Para ocultar saldo disponible en notas de débito
}

@Component({
  selector: 'app-select-invoice-dialog',
  templateUrl: './select-invoice-dialog.component.html',
  styleUrls: ['./select-invoice-dialog.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectInvoiceDialogComponent implements OnInit, OnDestroy {
  searchControl = new FormControl('');
  invoices: InvoiceDto[] = [];
  filteredInvoices: InvoiceDto[] = [];
  selectedInvoice: InvoiceDto | null = null;
  isLoading = false;
  hideAvailableAmount = false; // Por defecto mostrar saldo disponible (para notas de crédito)

  private destroy$ = new Subject<void>();

  constructor(
    public dialogRef: MatDialogRef<SelectInvoiceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SelectInvoiceDialogData | null,
    private readonly invoiceService: InvoiceService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef
  ) {
    // Si se pasa data y hideAvailableAmount es true, ocultar saldo disponible
    this.hideAvailableAmount = data?.hideAvailableAmount || false;
  }

  ngOnInit(): void {
    this.loadAuthorizedInvoices();

    // Escuchar cambios en el campo de búsqueda para filtrar
    this.searchControl.valueChanges
      .pipe(takeUntil(this.destroy$), debounceTime(300), distinctUntilChanged())
      .subscribe((searchTerm) => {
        this.filterInvoices(searchTerm || '');
      });
  }

  /**
   * Carga las facturas autorizadas
   * - Para notas de débito: usa search-for-debit-note
   * - Para notas de crédito: usa search-for-credit-note
   * Ambos endpoints requieren idCompany obligatorio
   */
  private loadAuthorizedInvoices(): void {
    // Obtener idCompany desde localStorage
    const idCompanyStr = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyStr) {
      this.logger.error('No se encontró ID de compañía en localStorage');
      this.toast.error(
        'Error: No se encontró información de la compañía. Por favor, inicie sesión nuevamente.'
      );
      this.dialogRef.close(null);
      return;
    }

    const idCompany = Number(idCompanyStr);
    if (!idCompany || isNaN(idCompany)) {
      this.logger.error('ID de compañía inválido:', idCompanyStr);
      this.toast.error(
        'Error: ID de compañía inválido. Por favor, inicie sesión nuevamente.'
      );
      this.dialogRef.close(null);
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    // Si es para notas de débito, usar el endpoint específico
    if (this.hideAvailableAmount) {
      this.loadInvoicesForDebitNote(idCompany);
    } else {
      // Para notas de crédito, usar findAll
      this.loadInvoicesForCreditNote(idCompany);
    }
  }

  /**
   * Carga facturas usando el endpoint search-for-debit-note (para notas de débito)
   */
  private loadInvoicesForDebitNote(idCompany: number): void {
    // Construir el DTO de búsqueda
    const searchDto: InvoiceSearchForDebitNoteDto = {
      idCompany: idCompany,
    };

    this.invoiceService
      .searchForDebitNote(searchDto, 0, 100)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.logger.debug('Invoices response from search-for-debit-note:', response);

          if (response && response.content && Array.isArray(response.content)) {
            // Mapear InvoiceForDebitNoteResponseDto a InvoiceDto para mantener compatibilidad
            this.invoices = response.content.map((item: InvoiceForDebitNoteResponseDto) => {
              // Convertir InvoiceForDebitNoteResponseDto a InvoiceDto
              const invoice: InvoiceDto = {
                idInvoice: item.idInvoice,
                idCompany: item.idCompany || idCompany,
                invoiceNumber: item.invoiceNumber,
                issueDate: item.issueDate || item.creationDate?.split('T')[0] || '',
                creationDate: item.creationDate,
                totalAmount: item.totalAmount || 0,
                tip: item.tip || 0,
                accessKey: item.accessKey,
                authorizationSri: item.authorizationSri as any,
                clientDto: item.clientDto,
                buyerName: item.buyerName || item.clientDto?.name,
                buyerIdentification: item.buyerIdentification || item.clientDto?.identification,
                idIssuancePoint: item.idIssuancePoint,
                // Campos requeridos por InvoiceDto pero que no vienen en la respuesta
                discount: 0,
                subtotalZero: 0,
                subtotalIva: 0,
                subtotalIce: 0,
                subtotalDiscount: 0,
                invoiceDetails: [],
                establishment: {} as any,
              } as InvoiceDto;

              // Agregar availableAmount como propiedad adicional (solo para notas de crédito)
              if (item.availableAmount !== undefined && item.availableAmount !== null) {
                (invoice as any).availableAmount = item.availableAmount;
              }

              // Agregar sequential si existe
              if (item.sequential !== undefined) {
                (invoice as any).sequential = item.sequential;
              }

              return invoice;
            });

            this.filteredInvoices = [...this.invoices];
            this.logger.debug('Authorized invoices loaded:', this.invoices.length);
          } else {
            this.invoices = [];
            this.filteredInvoices = [];
            this.logger.warn('No invoices found in response');
          }

          this.cdr.markForCheck();
        },
        error: (error) => {
          this.handleLoadError(error);
        },
      });
  }

  /**
   * Carga facturas usando search-for-credit-note (para notas de crédito)
   * Requiere idCompany obligatorio para filtrar las facturas por compañía
   */
  private loadInvoicesForCreditNote(idCompany: number): void {
    // Validar que idCompany sea válido (obligatorio)
    if (!idCompany || isNaN(idCompany) || idCompany <= 0) {
      this.logger.error('ID de compañía inválido para buscar facturas:', idCompany);
      this.toast.error(
        'Error: ID de compañía inválido. No se pueden cargar las facturas.'
      );
      this.dialogRef.close(null);
      return;
    }

    // Construir el DTO de búsqueda con idCompany obligatorio
    const searchDto: InvoiceSearchForCreditNoteDto = {
      idCompany: idCompany,
    };

    this.invoiceService
      .searchForCreditNote(searchDto, 0, 100)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.logger.debug('Invoices response from search-for-credit-note:', response);

          if (response && response.content && Array.isArray(response.content)) {
            // Log del primer item para debug
            if (response.content.length > 0) {
              this.logger.debug('Primer invoice del response search-for-credit-note:', response.content[0]);
              this.logger.debug('Estructura del primer invoice:', Object.keys(response.content[0]));
            }

            // Mapear InvoiceForDebitNoteResponseDto a InvoiceDto para mantener compatibilidad
            this.invoices = response.content.map((item: any) => {
              // Extraer datos del cliente de todas las posibles ubicaciones
              // El backend retorna clientName y clientIdentification directamente
              const clientName = 
                item.clientName || 
                item.buyerName || 
                item.clientDto?.name || 
                undefined;
              
              const clientIdentification = 
                item.clientIdentification || 
                item.buyerIdentification || 
                item.clientDto?.identification || 
                undefined;

              // Construir clientDto si no existe pero tenemos datos del cliente
              let clientDto = item.clientDto;
              if (!clientDto && (clientName || clientIdentification)) {
                clientDto = {
                  name: clientName || '',
                  identification: clientIdentification || '',
                  idClient: item.idClient,
                  idType: item.clientDto?.idType || 4, // Default: Consumidor Final
                  email: item.clientDto?.email || '',
                  address: item.clientDto?.address,
                  phone: item.clientDto?.phone,
                } as any;
              }

              // Mapear authorizationSri correctamente - preservar tal como viene del backend
              let authorizationSri: any = item.authorizationSri;
              // Si authorizationSri existe pero no tiene authorizationNumber, usar accessKey si está disponible
              if (authorizationSri && !authorizationSri.authorizationNumber && authorizationSri.accessKey) {
                authorizationSri.authorizationNumber = authorizationSri.accessKey;
              }
              // Si no hay authorizationSri pero hay accessKey en el item, construir un objeto básico
              if (!authorizationSri && item.accessKey) {
                authorizationSri = {
                  accessKey: item.accessKey,
                  authorizationNumber: item.accessKey,
                };
              }

              // Convertir InvoiceForDebitNoteResponseDto a InvoiceDto
              const invoice: InvoiceDto = {
                idInvoice: item.idInvoice,
                idCompany: item.idCompany || idCompany,
                invoiceNumber: item.invoiceNumber,
                issueDate: item.issueDate || item.creationDate?.split('T')[0] || '',
                creationDate: item.creationDate,
                totalAmount: item.totalAmount || 0,
                tip: item.tip || 0,
                accessKey: item.accessKey || authorizationSri?.accessKey,
                authorizationSri: authorizationSri,
                clientDto: clientDto,
                buyerName: clientName,
                buyerIdentification: clientIdentification,
                idIssuancePoint: item.idIssuancePoint,
                // Campos requeridos por InvoiceDto pero que no vienen en la respuesta
                discount: 0,
                subtotalZero: 0,
                subtotalIva: 0,
                subtotalIce: 0,
                subtotalDiscount: 0,
                invoiceDetails: [],
                establishment: {} as any,
              } as InvoiceDto;

              // Agregar availableAmount como propiedad adicional (para notas de crédito)
              if (item.availableAmount !== undefined && item.availableAmount !== null) {
                (invoice as any).availableAmount = item.availableAmount;
              }

              // Agregar sequential si existe
              if (item.sequential !== undefined) {
                (invoice as any).sequential = item.sequential;
              }

              return invoice;
            });

            this.filteredInvoices = [...this.invoices];
            this.logger.debug('Authorized invoices loaded:', this.invoices.length);
          } else {
            this.invoices = [];
            this.filteredInvoices = [];
            this.logger.warn('No invoices found in response');
          }

          this.cdr.markForCheck();
        },
        error: (error) => {
          this.handleLoadError(error);
        },
      });
  }

  /**
   * Maneja errores al cargar facturas
   */
  private handleLoadError(error: any): void {
    this.isLoading = false;
    this.logger.error('Error loading invoices', error);

    // Mensaje específico según el tipo de error
    let errorMessage = 'Error al cargar facturas autorizadas';
    let shouldClose = false;

    if (error.status === 401) {
      errorMessage =
        'Sesión expirada. Por favor, inicie sesión nuevamente.';
      shouldClose = true;
    } else if (error.status === 400) {
      errorMessage =
        error.error?.message || 'Error en la solicitud. Verifique los datos e intente nuevamente.';
    } else if (error.status === 0) {
      errorMessage = 'No se pudo conectar con el servidor';
      shouldClose = true;
    } else {
      errorMessage =
        error.error?.message || 'Error al cargar facturas. Contacte al administrador.';
    }

    this.toast.error(errorMessage);

    if (shouldClose) {
      this.dialogRef.close(null);
    }

    this.cdr.markForCheck();
  }

  /**
   * Filtra las facturas según el término de búsqueda
   */
  private filterInvoices(searchTerm: string): void {
    if (!searchTerm.trim()) {
      this.filteredInvoices = [...this.invoices];
    } else {
      const term = searchTerm.toLowerCase().trim();
      this.filteredInvoices = this.invoices.filter((inv) => {
        const authObj = inv.authorizationSri as any;
        return (
          // Buscar por número de autorización
          authObj?.authorizationNumber?.toLowerCase().includes(term) ||
          // Buscar por clave de acceso
          authObj?.accessKey?.toLowerCase().includes(term) ||
          // Buscar por número de factura
          inv.invoiceNumber?.toLowerCase().includes(term) ||
          this.getInvoiceNumber(inv).toLowerCase().includes(term) ||
          // Buscar por nombre del cliente
          this.getClientName(inv).toLowerCase().includes(term) ||
          // Buscar por identificación del cliente
          this.getClientIdentification(inv).toLowerCase().includes(term)
        );
      });
    }
    this.cdr.markForCheck();
  }

  /**
   * Maneja el click en una factura
   * No permite seleccionar facturas completamente cubiertas SOLO para notas de crédito
   * Para notas de débito, permite seleccionar cualquier factura autorizada
   */
  onInvoiceClick(invoice: InvoiceDto): void {
    // Para notas de débito (hideAvailableAmount = true), permitir seleccionar cualquier factura
    if (this.hideAvailableAmount) {
      this.selectInvoice(invoice);
      return;
    }

    // Para notas de crédito, validar que no esté completamente cubierta
    if (this.isFullyCovered(invoice)) {
      // Mostrar mensaje informativo
      this.toast.info(
        'Esta factura ya fue completamente cubierta por notas de crédito'
      );
      return;
    }
    this.selectInvoice(invoice);
  }

  /**
   * Selecciona una factura
   */
  selectInvoice(invoice: InvoiceDto): void {
    this.selectedInvoice = invoice;
    this.cdr.markForCheck();
  }

  /**
   * Verifica si la factura está completamente cubierta por NC
   * Para notas de débito, siempre retorna false (no se valida saldo disponible)
   */
  isFullyCovered(invoice: InvoiceDto): boolean {
    // Para notas de débito, nunca considerar una factura como "completamente cubierta"
    // porque las notas de débito pueden crearse sobre cualquier factura autorizada
    if (this.hideAvailableAmount) {
      return false;
    }

    const inv = invoice as any;
    // Si el backend envía availableAmount
    if (inv.availableAmount !== undefined && inv.availableAmount !== null) {
      return inv.availableAmount <= 0;
    }
    // Si el backend envía saldoDisponible
    if (inv.saldoDisponible !== undefined && inv.saldoDisponible !== null) {
      return inv.saldoDisponible <= 0;
    }
    // Por defecto, no está cubierta (permitir selección)
    return false;
  }

  /**
   * Obtiene el monto disponible para NC
   */
  getAvailableAmount(invoice: InvoiceDto): number | null {
    const inv = invoice as any;
    if (inv.availableAmount !== undefined && inv.availableAmount !== null) {
      return inv.availableAmount;
    }
    if (inv.saldoDisponible !== undefined && inv.saldoDisponible !== null) {
      return inv.saldoDisponible;
    }
    return null; // No tenemos esta información
  }

  /**
   * Verifica si la factura tiene información de saldo disponible
   */
  hasAvailableAmount(invoice: InvoiceDto): boolean {
    return this.getAvailableAmount(invoice) !== null;
  }

  /**
   * Obtiene el monto disponible de forma segura (para el template)
   */
  getAvailableAmountSafe(invoice: InvoiceDto): number {
    return this.getAvailableAmount(invoice) ?? 0;
  }

  /**
   * Verifica si la factura tiene propina
   */
  hasTip(invoice: InvoiceDto): boolean {
    const inv = invoice as any;
    return inv.tip !== undefined && inv.tip !== null && inv.tip > 0;
  }

  /**
   * Obtiene el valor de la propina
   */
  getTip(invoice: InvoiceDto): number {
    const inv = invoice as any;
    return inv.tip ?? 0;
  }

  /**
   * Confirma la selección
   */
  confirm(): void {
    if (this.selectedInvoice) {
      this.dialogRef.close(this.selectedInvoice);
    }
  }

  /**
   * Cancela y cierra el diálogo
   */
  cancel(): void {
    this.dialogRef.close(null);
  }

  /**
   * Obtiene el número de factura
   */
  getInvoiceNumber(invoice: InvoiceDto): string {
    // El backend envía 'sequential' como número de secuencia
    // Construir el número de factura completo si es posible
    const inv = invoice as any;
    if (invoice.invoiceNumber) return invoice.invoiceNumber;
    if (inv.sequential) {
      // Formatear: 001-001-000000XXX
      const seq = String(inv.sequential).padStart(9, '0');
      return `001-001-${seq}`;
    }
    return '-';
  }

  /**
   * Obtiene el número de autorización del SRI
   */
  getAuthorizationNumber(invoice: InvoiceDto): string {
    const inv = invoice as any;
    const authObj = invoice.authorizationSri as any;
    
    // Buscar authorizationNumber en varias ubicaciones posibles
    if (authObj?.authorizationNumber) return authObj.authorizationNumber;
    if (authObj?.accessKey) return authObj.accessKey;
    if (inv.authorizationNumber) return inv.authorizationNumber;
    if (invoice.accessKey) return invoice.accessKey;
    
    return '-';
  }

  /**
   * Obtiene el nombre del cliente
   * Nota: El endpoint /api/invoice solo devuelve idClient, no los datos del cliente
   */
  getClientName(invoice: InvoiceDto): string {
    const inv = invoice as any;
    // Buscar en todas las posibles ubicaciones del nombre
    if (invoice.buyerName) return invoice.buyerName;
    if (invoice.clientDto?.name) return invoice.clientDto.name;
    if (inv.clientName) return inv.clientName;
    if (inv.client?.name) return inv.client.name;
    // Si solo tenemos idClient, mostrarlo como referencia
    if (inv.idClient) return `Cliente #${inv.idClient}`;
    return 'Sin datos de cliente';
  }

  /**
   * Obtiene la identificación del cliente
   * Nota: El endpoint /api/invoice solo devuelve idClient, no los datos del cliente
   */
  getClientIdentification(invoice: InvoiceDto): string {
    const inv = invoice as any;
    // Buscar en todas las posibles ubicaciones de la identificación
    if (invoice.buyerIdentification) return invoice.buyerIdentification;
    if (invoice.clientDto?.identification)
      return invoice.clientDto.identification;
    if (inv.clientIdentification) return inv.clientIdentification;
    if (inv.client?.identification) return inv.client.identification;
    // Si solo tenemos idClient
    if (inv.idClient) return `ID: ${inv.idClient}`;
    return '-';
  }

  /**
   * Formatea una fecha
   */
  formatDate(date: string | undefined): string {
    if (!date) return '-';
    try {
      return new Date(date).toLocaleDateString('es-EC', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    } catch {
      return '-';
    }
  }

  /**
   * Formatea un monto
   */
  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '$0.00';
    return `$${amount.toFixed(2)}`;
  }

  /**
   * Verifica si una factura está seleccionada
   */
  isSelected(invoice: InvoiceDto): boolean {
    return this.selectedInvoice?.idInvoice === invoice.idInvoice;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
