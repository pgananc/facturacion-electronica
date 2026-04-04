import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Inject,
} from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastService } from '../../../../_service/notification/toast.service';
import { Subject, forkJoin, of } from 'rxjs';
import { takeUntil, finalize, catchError, map } from 'rxjs/operators';
import { DebitNoteService } from '../../../../_service/invoice/debit-note.service';
import { InvoiceService } from '../../../../_service/invoice/invoice.service';
import { ProductCacheService } from '../../../../_service/product/product-cache.service';
import { ProductService } from '../../../../_service/product/product.service';
import { PaymentMethodService } from '../../../../_service/invoice/payment-method.service';
import { CompanyService } from '../../../../_service/customer/company.service';
import { DebitNoteDto } from '../../../../_model/invoice/debit-note-dto';
import {
  InvoiceDto,
  InvoiceDetailDto,
  PaymentDto,
} from '../../../../_model/invoice/invoice-dto';
import { ProductDto } from '../../../../_model/product/product-dto';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { environment } from '../../../../../environments/environment';
import { SelectProductDialogComponent } from '../../select-product-dialog/select-product-dialog.component';
import { AddPaymentDialogComponent } from '../../add-payment-dialog/add-payment-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { PermissionService } from '../../../../_service/permission/permission.service';
import {
  SuccessDebitNoteDialogComponent,
  SuccessDebitNoteDialogData,
} from '../success-debit-note-dialog/success-debit-note-dialog.component';

export interface CreateDebitNoteDialogData {
  invoice?: InvoiceDto;
  draftId?: number; // ID del borrador a editar (opcional)
}

@Component({
  selector: 'app-create-debit-note',
  templateUrl: './create-debit-note.component.html',
  styleUrls: ['./create-debit-note.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateDebitNoteComponent implements OnInit, OnDestroy {
  // Datos
  selectedInvoice: InvoiceDto;
  companyData: any = null;

  // Formulario
  form: FormGroup;

  // Estados
  isLoadingCompany = false;
  isLoadingProducts = false;
  isSavingDraft = false;
  isSendingToSri = false;

  // Productos y pagos
  productsDataSource = new MatTableDataSource<any>([]);
  displayedProductColumns = [
    'code',
    'description',
    'quantity',
    'unitPrice',
    'discount',
    'subtotal',
    'actions',
  ];

  paymentsDataSource: PaymentDto[] = [];
  displayedPaymentColumns = ['paymentType', 'value', 'term', 'time', 'actions'];

  // Totales calculados
  subtotalZero = 0;
  subtotalIva = 0;
  subtotalIce = 0;
  totalDiscount = 0;
  totalIva = 0;
  totalIce = 0;
  valorModificacion = 0;
  valorTotal = 0;
  subtotalsByIva: { ivaPercent: number; subtotal: number; ivaValue: number }[] =
    [];

  // Fechas válidas para emisión ND
  minDate: string = '';
  maxDate: string = '';

  Math = Math;

  private destroy$ = new Subject<void>();

  draftId?: number; // ID del borrador que se está editando (si existe)

  constructor(
    public dialogRef: MatDialogRef<CreateDebitNoteComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateDebitNoteDialogData,
    private readonly fb: FormBuilder,
    private readonly debitNoteService: DebitNoteService,
    private readonly invoiceService: InvoiceService,
    private readonly productCacheService: ProductCacheService,
    private readonly productService: ProductService,
    private readonly paymentMethodService: PaymentMethodService,
    private readonly companyService: CompanyService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly permissionService: PermissionService
  ) {
    this.selectedInvoice = data.invoice || ({} as InvoiceDto);
    this.initDateLimits();
    this.initForm();
    this.loadCompanyData();
  }

  /**
   * Inicializa los límites de fecha para la ND
   * Min: fecha de emisión de la factura
   * Max: fecha actual
   */
  private initDateLimits(): void {
    // Fecha máxima: hoy
    const today = new Date();
    this.maxDate = today.toISOString().split('T')[0];

    // Fecha mínima: fecha de la factura o creación
    const invoiceDate =
      this.selectedInvoice.issueDate ||
      this.selectedInvoice.creationDate ||
      this.maxDate;

    const minDateObj = new Date(invoiceDate);
    this.minDate = minDateObj.toISOString().split('T')[0];
  }

  private initForm(): void {
    // Obtener información de autorización de la factura
    const authObj = this.selectedInvoice.authorizationSri as any;

    // Obtener fecha actual en formato local (sin conversión de zona horaria)
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    this.form = this.fb.group({
      issueDate: [todayStr, Validators.required],
      observation: [''],
      originalInvoiceId: [this.selectedInvoice.idInvoice, Validators.required],
      originalInvoiceNumber: [
        this.selectedInvoice.invoiceNumber || '',
        Validators.required,
      ],
      originalIssueDate: [
        this.selectedInvoice.issueDate ||
          this.selectedInvoice.creationDate ||
          '',
        Validators.required,
      ],
      originalAccessKey: [
        this.selectedInvoice.accessKey || authObj?.accessKey || '',
      ],
      documentTypeModified: ['01'], // Default: Factura
    });
  }

  ngOnInit(): void {
    // Si hay draftId, cargar el borrador; si no, usar la factura proporcionada
    if (this.data.draftId) {
      this.draftId = this.data.draftId;
      this.loadDraftForEditing(this.data.draftId);
    } else {
      this.selectedInvoice = this.data.invoice || ({} as InvoiceDto);
    this.logger.debug('CreateDebitNoteComponent initialized', {
      invoiceId: this.selectedInvoice.idInvoice,
    });
    this.logger.debug('Datos de factura recibidos:', {
      invoice: this.selectedInvoice,
      hasBuyerName: !!this.selectedInvoice.buyerName,
      hasBuyerIdentification: !!this.selectedInvoice.buyerIdentification,
      hasClientDto: !!this.selectedInvoice.clientDto,
      clientDto: this.selectedInvoice.clientDto,
    });

    // Cargar datos completos de la factura si solo tenemos el ID
    if (
      this.selectedInvoice.idInvoice &&
      !this.hasClientData(this.selectedInvoice)
    ) {
      this.logger.debug('Cargando detalles completos de factura...');
      this.loadInvoiceDetails(this.selectedInvoice.idInvoice);
    } else {
      this.logger.debug('La factura ya tiene datos del cliente:', {
        clientName: this.getClientName(),
        clientIdentification: this.getClientIdentification(),
      });
      }
    }
  }

  /**
   * Carga un borrador para edición
   */
  private loadDraftForEditing(draftId: number): void {
    this.isLoadingCompany = true;
    this.cdr.markForCheck();

    this.logger.debug('📝 Cargando borrador de nota de débito con ID:', draftId);

    this.debitNoteService
      .getDraftById(draftId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft: DebitNoteDto) => {
          this.logger.debug('✅ Borrador cargado:', draft);

          // Cargar la factura original asociada
          const originalInvoiceId = draft.originalInvoiceId;
          if (!originalInvoiceId) {
            this.toast.error('No se encontró la factura original asociada');
            this.isLoadingCompany = false;
            this.cdr.markForCheck();
            return;
          }

          // Obtener idCompany del borrador o del localStorage antes de cargar la factura
          const companyId = draft.idCompany || this.getCompanyId();
          if (!companyId || companyId === 0) {
            this.toast.error('No se encontró el ID de la compañía');
            this.isLoadingCompany = false;
            this.cdr.markForCheck();
            return;
          }

          // Establecer selectedInvoice con idCompany antes de cargar la factura
          this.selectedInvoice = {
            idInvoice: originalInvoiceId,
            invoiceNumber: draft.originalInvoiceNumber,
            issueDate: draft.originalIssueDate,
            accessKey: draft.originalAccessKey,
            idCompany: companyId,
          } as InvoiceDto;

          // Cargar la factura original
          this.invoiceService
            .findById(originalInvoiceId)
            .pipe(
              takeUntil(this.destroy$),
              catchError((error) => {
                this.logger.error('Error al cargar factura original', error);
                this.toast.error('Error al cargar la factura original');
                this.isLoadingCompany = false;
                this.cdr.markForCheck();
                return of(null);
              })
            )
            .subscribe((response: any) => {
              if (response) {
                const invoiceData = response.data || response;
                this.selectedInvoice = { ...this.selectedInvoice, ...invoiceData };
                this.logger.debug('Factura original cargada', this.selectedInvoice);
              }

              // Poblar el formulario con los datos del borrador
              this.populateFormFromDraft(draft);
              this.isLoadingCompany = false;
              this.cdr.markForCheck();
            });
        },
        error: (err) => {
          this.logger.error('Error al cargar borrador', err);
          this.toast.error('Error al cargar el borrador');
          this.isLoadingCompany = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Pobla el formulario con los datos del borrador
   */
  private populateFormFromDraft(draft: DebitNoteDto): void {
    // Establecer fecha de emisión
    if (draft.issueDate) {
      const issueDate = new Date(draft.issueDate);
      this.form.patchValue({
        issueDate: issueDate.toISOString().split('T')[0],
      });
    }

    // Establecer observación
    if (draft.observation !== undefined) {
      this.form.patchValue({ observation: draft.observation });
    }

    // Actualizar campos requeridos de la factura original con los datos del borrador
    // Esto asegura que el formulario sea válido cuando se carga un borrador
    this.form.patchValue({
      originalInvoiceId: draft.originalInvoiceId || this.selectedInvoice.idInvoice,
      originalInvoiceNumber: draft.originalInvoiceNumber || this.selectedInvoice.invoiceNumber || '',
      originalIssueDate: draft.originalIssueDate || this.selectedInvoice.issueDate || this.selectedInvoice.creationDate || '',
      originalAccessKey: draft.originalAccessKey || this.selectedInvoice.accessKey || '',
    });

    // Cargar detalles del borrador
    const draftDetails = draft.debitNoteDetails || draft.details || [];
    if (draftDetails.length > 0) {
      // Limpiar detalles actuales
      this.productsDataSource.data = [];

      // Cargar productos completos desde el servicio
      const productLoadPromises = draftDetails.map((detail) => {
        if (!detail.idProduct) {
          // Si no hay idProduct, usar los datos del borrador directamente
          return of({
            idProduct: detail.idProduct,
            code: detail.productDto?.code || detail.code || '',
            description: detail.description || detail.productDto?.name || detail.productDto?.description || '',
            quantity: detail.quantity || 0,
            unitPrice: detail.unitPrice || 0,
            discount: detail.discount || 0,
            subtotal: detail.subtotal || (detail.quantity || 0) * (detail.unitPrice || 0) - (detail.discount || 0),
            ivaPercentage: detail.ivaPercentage || 0,
            icePercentage: (detail as any).icePercentage || 0,
            productDto: detail.productDto,
          });
        }

        // Intentar obtener el producto desde el caché primero
        const cachedProduct = this.productCacheService.getProductById(detail.idProduct);
        if (cachedProduct) {
          return of({
            idProduct: detail.idProduct,
            code: cachedProduct.mainCode || '',
            description: detail.description || cachedProduct.name || cachedProduct.description || '',
            quantity: detail.quantity || 0,
            unitPrice: detail.unitPrice || 0,
            discount: detail.discount || 0,
            subtotal: detail.subtotal || (detail.quantity || 0) * (detail.unitPrice || 0) - (detail.discount || 0),
            ivaPercentage: detail.ivaPercentage || cachedProduct.ivaTaxType?.value || 0,
            icePercentage: (detail as any).icePercentage || cachedProduct.iceTaxType?.value || 0,
            productDto: cachedProduct,
          });
        }

        // Si no está en caché, cargarlo desde el servicio
        return this.productService.findById(detail.idProduct).pipe(
          map((response: any) => {
            const product = response.productDto || response;
            return {
              idProduct: detail.idProduct,
              code: product.mainCode || '',
              description: detail.description || product.name || product.description || '',
              quantity: detail.quantity || 0,
              unitPrice: detail.unitPrice || 0,
              discount: detail.discount || 0,
              subtotal: detail.subtotal || (detail.quantity || 0) * (detail.unitPrice || 0) - (detail.discount || 0),
              ivaPercentage: detail.ivaPercentage || product.ivaTaxType?.value || 0,
              icePercentage: (detail as any).icePercentage || product.iceTaxType?.value || 0,
              productDto: product,
            };
          }),
          catchError((err) => {
            this.logger.error(`Error al cargar producto ${detail.idProduct}`, err);
            // Si falla, usar los datos del borrador
            return of({
              idProduct: detail.idProduct,
              code: detail.productDto?.code || detail.code || '',
              description: detail.description || detail.productDto?.name || detail.productDto?.description || '',
              quantity: detail.quantity || 0,
              unitPrice: detail.unitPrice || 0,
              discount: detail.discount || 0,
              subtotal: detail.subtotal || (detail.quantity || 0) * (detail.unitPrice || 0) - (detail.discount || 0),
              ivaPercentage: detail.ivaPercentage || 0,
              icePercentage: (detail as any).icePercentage || 0,
              productDto: detail.productDto,
            });
          })
        );
      });

      // Esperar a que se carguen todos los productos
      forkJoin(productLoadPromises).subscribe({
        next: (productsData) => {
          this.productsDataSource.data = productsData;
          // Recalcular totales
          this.calculateTotals();
          this.cdr.markForCheck();
          this.logger.debug('✅ Productos cargados del borrador:', productsData);
        },
        error: (err) => {
          this.logger.error('Error al cargar productos del borrador', err);
          // Si falla, usar los datos del borrador directamente
          const productsData = draftDetails.map((detail) => ({
            idProduct: detail.idProduct,
            code: detail.productDto?.code || detail.code || '',
            description: detail.description || detail.productDto?.name || detail.productDto?.description || '',
            quantity: detail.quantity || 0,
            unitPrice: detail.unitPrice || 0,
            discount: detail.discount || 0,
            subtotal: detail.subtotal || (detail.quantity || 0) * (detail.unitPrice || 0) - (detail.discount || 0),
            ivaPercentage: detail.ivaPercentage || 0,
            icePercentage: (detail as any).icePercentage || 0,
            productDto: detail.productDto,
          }));
          this.productsDataSource.data = productsData;
          this.calculateTotals();
          this.cdr.markForCheck();
        },
      });
    }

    // Cargar formas de pago del borrador
    const draftPayments = draft.paymentMethods || [];
    if (draftPayments.length > 0) {
      // Limpiar pagos actuales
      this.paymentsDataSource = [];

      // Cargar métodos de pago desde el servicio para obtener los nombres
      this.paymentMethodService.findAll().subscribe({
        next: (paymentMethods) => {
          // Mapear pagos del borrador con los nombres de los métodos de pago
          const paymentsData: PaymentDto[] = draftPayments.map((payment: any) => {
            // Buscar el método de pago por código o ID
            const method = paymentMethods.find(
              (m) =>
                m.paymentMethodCode === payment.paymentMethodCode ||
                m.idPaymentMethod === payment.idPaymentMethod
            );

            return {
              idPaymentMethod: payment.idPaymentMethod,
              paymentType: method?.paymentMethodType || payment.paymentType || payment.paymentMethod || '',
              paymentMethod: method?.paymentMethodType || payment.paymentMethod || payment.paymentType || '',
              paymentMethodCode: payment.paymentMethodCode || '',
              value: payment.value || payment.total || 0,
              total: payment.total || payment.value || 0,
              term: payment.term ?? undefined,
              timeUnit: payment.timeUnit || payment.time || undefined,
              time: payment.time || payment.timeUnit || undefined,
            };
          });

          this.paymentsDataSource = paymentsData;
          this.cdr.markForCheck();
          this.logger.debug('✅ Formas de pago cargadas del borrador:', paymentsData);
        },
        error: (err) => {
          this.logger.error('Error al cargar métodos de pago', err);
          // Si falla, usar los datos sin el nombre del método
          const paymentsData: PaymentDto[] = draftPayments.map((payment: any) => ({
            idPaymentMethod: payment.idPaymentMethod,
            paymentType: payment.paymentType || payment.paymentMethod || '',
            paymentMethod: payment.paymentMethod || payment.paymentType || '',
            paymentMethodCode: payment.paymentMethodCode || '',
            value: payment.value || payment.total || 0,
            total: payment.total || payment.value || 0,
            term: payment.term ?? undefined,
            timeUnit: payment.timeUnit || payment.time || undefined,
            time: payment.time || payment.timeUnit || undefined,
          }));
          this.paymentsDataSource = paymentsData;
          this.cdr.markForCheck();
        },
      });
    }

    this.cdr.markForCheck();
    this.logger.debug('✅ Formulario poblado con datos del borrador');
  }


  /**
   * Verifica si la factura tiene datos del cliente
   */
  private hasClientData(invoice: InvoiceDto): boolean {
    return !!(
      invoice.buyerName ||
      invoice.buyerIdentification ||
      invoice.clientDto?.name ||
      invoice.clientDto?.identification ||
      (invoice as any).clientName ||
      (invoice as any).clientIdentification
    );
  }

  /**
   * Carga los detalles completos de la factura desde el servicio
   */
  private loadInvoiceDetails(invoiceId: number): void {
    this.isLoadingCompany = true;
    this.cdr.markForCheck();

    this.invoiceService
      .findById(invoiceId)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error al cargar detalles de factura', error);
          this.toast.error('Error al cargar los detalles de la factura');
          this.isLoadingCompany = false;
          this.cdr.markForCheck();
          return of(null);
        })
      )
      .subscribe((response: any) => {
        this.isLoadingCompany = false;
        this.cdr.markForCheck();

        if (response) {
          // Actualizar la factura seleccionada con los datos completos
          const invoiceData = response.data || response;
          this.selectedInvoice = { ...this.selectedInvoice, ...invoiceData };
          this.logger.debug('Factura completa cargada', this.selectedInvoice);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * ✅ Combina el payload enviado con la respuesta del backend
   * Similar a mergeInvoiceData en billing.component.ts
   * 
   * @param debitNoteDto - El DebitNoteDto completo que se envió al backend
   * @param responseData - Los datos adicionales que retorna el backend (idDebitNote, debitNoteNumber, accessKey, etc.)
   * @returns DebitNoteDto completo combinado
   */
  private mergeDebitNoteData(debitNoteDto: DebitNoteDto, responseData: any): DebitNoteDto {
    // El debitNoteDto ya tiene todos los datos: clientDto, debitNoteDetails, paymentMethods, totals, etc.
    // responseData tiene los datos adicionales: idDebitNote, debitNoteNumber, accessKey, authorizationNumber, etc.
    // IMPORTANTE: Preservar clientDto del payload original, no sobrescribir con el de responseData
    // ya que el backend puede no devolver todos los campos del cliente (address, phone, etc.)

    return {
      ...debitNoteDto, // Mantener todos los datos originales del payload
      // Sobrescribir/agregar SOLO con datos de la respuesta que no están en el payload original
      idDebitNote: responseData.idDebitNote || debitNoteDto.idDebitNote,
      debitNoteNumber: responseData.debitNoteNumber || debitNoteDto.debitNoteNumber,
      accessKey: responseData.accessKey || debitNoteDto.accessKey,
      authorizationNumber: responseData.authorizationNumber || debitNoteDto.authorizationNumber,
      authorizationDate: responseData.authorizationDate || debitNoteDto.authorizationDate,
      authorizationStatusSri: responseData.authorizationStatusSri || debitNoteDto.authorizationStatusSri,
      environment: responseData.environment || debitNoteDto.environment,
      status: responseData.status || debitNoteDto.status || 'AUTHORIZED',
      debitNoteStatus: responseData.debitNoteStatus || responseData.status || debitNoteDto.debitNoteStatus || 'AUTHORIZED',
      // ✅ PRESERVAR datos del payload original (pueden estar más completos que los del backend)
      // Preservar fechas del payload original (en formato YYYY-MM-DD) para evitar problemas de zona horaria
      issueDate: debitNoteDto.issueDate,
      originalIssueDate: debitNoteDto.originalIssueDate,
      debitNoteDetails: debitNoteDto.debitNoteDetails || debitNoteDto.details || [],
      details: debitNoteDto.details || debitNoteDto.debitNoteDetails || [],
      paymentMethods: debitNoteDto.paymentMethods || [],
      // ✅ CRÍTICO: Usar clientDto del payload original (tiene todos los campos: name, identification, address, phone)
      // NO usar responseData.clientDto ya que puede estar incompleto
      clientDto: debitNoteDto.clientDto,
      establishment: debitNoteDto.establishment,
      companyDto: debitNoteDto.companyDto,
    };
  }

  /**
   * Obtiene el ID de la compañía
   * Prioridad: 1) localStorage, 2) factura seleccionada
   */
  private getCompanyId(): number {
    // Primero intentar desde localStorage
    const savedCompanyId = localStorage.getItem(environment.ID_COMPANY);
    if (savedCompanyId) {
      const companyId = Number(savedCompanyId);
      if (companyId && companyId > 0) {
        return companyId;
      }
    }

    // Si no está en localStorage, intentar desde la factura seleccionada
    if (this.selectedInvoice?.idCompany) {
      return this.selectedInvoice.idCompany;
    }

    // Si la factura tiene companyDto
    if (this.selectedInvoice?.companyDto?.idCompany) {
      return this.selectedInvoice.companyDto.idCompany;
    }

    this.logger.warn('No se pudo obtener idCompany', {
      fromLocalStorage: savedCompanyId,
      fromInvoice: this.selectedInvoice?.idCompany,
    });
    
    this.toast.error('Error: No se pudo obtener el ID de la compañía');

    return 0;
  }

  /**
   * Obtiene el ID del punto de emisión desde localStorage
   */
  private getIssuancePointId(): number | undefined {
    // Prioridad 1: Usar INVOICE_POINT_DATA de localStorage
    const invoicePointDataStr = localStorage.getItem('INVOICE_POINT_DATA');
    if (invoicePointDataStr) {
      try {
        const invoicePointData = JSON.parse(invoicePointDataStr);
        if (invoicePointData.id) {
          return invoicePointData.id;
        }
      } catch (e) {
        this.logger.error('Error al parsear INVOICE_POINT_DATA', e);
      }
    }

    // Prioridad 2: Usar ID_INVOICE_POINT de localStorage
    const invoicePointId = localStorage.getItem(environment.ID_INVOICE_POINT);
    if (invoicePointId) {
      return Number(invoicePointId);
    }

    // Prioridad 3: Intentar desde la factura original (si tiene)
    const invoicePointIdFromInvoice = (this.selectedInvoice as any)
      .idIssuancePoint;
    if (invoicePointIdFromInvoice) {
      return invoicePointIdFromInvoice;
    }

    return undefined;
  }

  /**
   * Carga los datos de la compañía
   */
  private loadCompanyData(): void {
    const savedCompanyId = localStorage.getItem(environment.ID_COMPANY);
    if (!savedCompanyId) {
      this.logger.warn('No hay ID_COMPANY en localStorage');
      this.toast.error('Error: No se encontró información de la compañía. Por favor, recargue la página.');
      return;
    }

    this.isLoadingCompany = true;
    this.cdr.markForCheck();

    // Cargar datos de la compañía
    this.companyService
      .findById(Number(savedCompanyId))
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error al cargar datos de compañía', error);
          this.isLoadingCompany = false;
          this.cdr.markForCheck();
          return of(null);
        })
      )
      .subscribe((response: any) => {
        this.isLoadingCompany = false;
        this.cdr.markForCheck();

        if (response?.data) {
          this.companyData = Array.isArray(response.data)
            ? response.data[0]
            : response.data;
        } else if (response?.company) {
          this.companyData = response.company;
        }

        this.logger.debug('Datos de compañía cargados', this.companyData);
      });
  }

  /**
   * Abre el diálogo para seleccionar productos
   */
  openSelectProductDialog(): void {
    // Obtener todos los productos disponibles
    this.isLoadingProducts = true;
    this.cdr.markForCheck();

    // Cargar productos desde el servicio
    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!companyId) {
      this.toast.error('Error: No se encontró ID de compañía');
      this.isLoadingProducts = false;
      this.cdr.markForCheck();
      return;
    }

    this.productService
      .findByIdCompany(companyId)
      .pipe(
        takeUntil(this.destroy$),
        map((response: any) => {
          // Manejar diferentes formatos de respuesta
          if (Array.isArray(response)) {
            return response;
          } else if (
            response?.productDtos &&
            Array.isArray(response.productDtos)
          ) {
            return response.productDtos;
          } else if (response?.data && Array.isArray(response.data)) {
            return response.data;
          }
          return [];
        }),
        catchError((error) => {
          this.logger.error('Error al cargar productos', error);
          this.isLoadingProducts = false;
          this.cdr.markForCheck();
          this.toast.error(
            'Error al cargar productos. Intente nuevamente.'
          );
          return of([]);
        })
      )
      .subscribe((products: ProductDto[]) => {
        this.isLoadingProducts = false;
        this.cdr.markForCheck();

        // Asegurar que products es siempre un array
        const productsArray = Array.isArray(products) ? products : [];

        this.logger.debug('Productos cargados para nota de débito', {
          count: productsArray.length,
          products: productsArray,
        });

        const dialogRef = this.dialog.open(SelectProductDialogComponent, {
          width: '900px',
          maxWidth: '95vw',
          maxHeight: '95vh',
          data: { products: productsArray, searchTerm: '' },
        });

        dialogRef.afterClosed().subscribe((selectedProducts: ProductDto[]) => {
          if (selectedProducts && selectedProducts.length > 0) {
            this.addProducts(selectedProducts);
          }
        });
      });
  }

  /**
   * Agrega productos seleccionados a la lista
   */
  private addProducts(products: ProductDto[]): void {
    const currentData = this.productsDataSource.data;

    products.forEach((product) => {
      // Verificar si el producto ya está agregado
      const exists = currentData.some((p) => p.idProduct === product.idProduct);

      if (!exists) {
        const productData = {
          idProduct: product.idProduct,
          code: product.mainCode || '',
          description: product.name || product.description || '',
          quantity: 1,
          unitPrice: product.unitPrice || 0,
          discount: 0,
          subtotal: product.unitPrice || 0,
          ivaPercentage: product.ivaTaxType?.value || 0,
          icePercentage: product.iceTaxType?.value || 0,
          productDto: product,
        };

        currentData.push(productData);
      }
    });

    // Actualizar el dataSource con los nuevos datos
    this.productsDataSource.data = [...currentData];

    this.calculateTotals();
    this.cdr.markForCheck();

    this.logger.debug('Productos agregados', {
      total: this.productsDataSource.data.length,
    });
  }

  /**
   * Elimina un producto de la lista
   */
  removeProduct(index: number): void {
    const currentData = this.productsDataSource.data;
    currentData.splice(index, 1);
    this.productsDataSource.data = [...currentData];
    this.calculateTotals();
    this.cdr.markForCheck();
  }

  /**
   * Actualiza un producto en la lista
   */
  updateProduct(index: number, field: string, value: any): void {
    const currentData = this.productsDataSource.data;
    const product = currentData[index];

    if (!product) return;

    // Convertir a número si es necesario
    const numValue = [
      'quantity',
      'unitPrice',
      'discount',
      'ivaPercentage',
      'icePercentage',
    ].includes(field)
      ? parseFloat(value) || 0
      : value;

    product[field] = numValue;

    // Solo recalcular subtotal si NO es servicio (productType !== 2)
    // Para servicios, el subtotal se ingresa manualmente
    if (product.productDto?.productType !== 2) {
      const quantity = product.quantity || 0;
      const unitPrice = product.unitPrice || 0;
      const discount = product.discount || 0;
      product.subtotal = quantity * unitPrice - discount;
    }

    // Actualizar el dataSource con los datos modificados
    this.productsDataSource.data = [...currentData];

    this.calculateTotals();
    this.cdr.markForCheck();
  }

  /**
   * Actualiza el subtotal de un producto manualmente (para servicios)
   */
  updateProductSubtotal(index: number, value: any): void {
    const currentData = this.productsDataSource.data;
    const product = currentData[index];

    if (!product) return;

    // Convertir a número y asegurar que sea >= 0
    const numValue = Math.max(0, parseFloat(value) || 0);
    
    // Redondear a 2 decimales
    product.subtotal = Math.round(numValue * 100) / 100;

    // Actualizar el dataSource con los datos modificados
    this.productsDataSource.data = [...currentData];

    this.calculateTotals();
    this.cdr.markForCheck();
  }

  /**
   * Agrega rápidamente una forma de pago común (Efectivo, Tarjeta de débito/crédito)
   * Usa el servicio con caché para evitar llamadas innecesarias al backend
   *
   * @param paymentCode - Código SRI de la forma de pago ("01", "16", "19")
   */
  addQuickPayment(paymentCode: string): void {
    this.logger.debug(`Agregando forma de pago rápida: código ${paymentCode}`);

    // Validar que no exista ya esa forma de pago
    const alreadyExists = this.paymentsDataSource.some(
      (payment: PaymentDto) => payment.paymentMethodCode === paymentCode
    );

    if (alreadyExists) {
      this.toast.warning(
        'Esta forma de pago ya está agregada. No se puede duplicar.'
      );
      this.logger.warn('Intento de agregar forma de pago duplicada', {
        code: paymentCode,
      });
      return;
    }

    // Siempre llamar a findAll() - si hay petición en curso, la reutiliza
    // Si ya está en caché, devuelve inmediatamente el caché
    this.paymentMethodService.findAll().subscribe({
      next: (methods) => {
        const method = methods.find((m) => m.paymentMethodCode === paymentCode);

        if (method) {
          const paymentData = {
            idPaymentMethod: method.idPaymentMethod,
            paymentType: method.paymentMethodType,
            paymentMethodCode: method.paymentMethodCode,
            value: 0.0, // Iniciar en 0.00, el usuario ingresará el valor manualmente
            term: undefined,
            time: undefined,
          };
          this.addPayment(paymentData);
        } else {
          this.toast.warning(
            `Forma de pago con código ${paymentCode} no encontrada`
          );
          this.logger.error(
            `Forma de pago no encontrada: código ${paymentCode}`
          );
        }
      },
      error: (err) => {
        this.toast.error(
          'Error al cargar formas de pago. Intente nuevamente.'
        );
        this.logger.error('Error al cargar formas de pago', err);
      },
    });
  }

  /**
   * Abre el diálogo para agregar forma de pago
   */
  openAddPaymentDialog(): void {
    const dialogRef = this.dialog.open(AddPaymentDialogComponent, {
      width: '500px',
      data: {},
    });

    dialogRef.afterClosed().subscribe((paymentData: any) => {
      if (paymentData) {
        // Validar que no exista ya esa forma de pago
        const alreadyExists = this.paymentsDataSource.some(
          (payment: PaymentDto) =>
            payment.paymentMethodCode === paymentData.paymentMethodCode
        );

        if (alreadyExists) {
          this.toast.warning(
            `${paymentData.paymentType} ya está agregado. No se puede duplicar.`
          );
          return;
        }

        this.addPayment(paymentData);
      }
    });
  }

  /**
   * Agrega una forma de pago
   */
  private addPayment(paymentData: any): void {
    const payment: PaymentDto = {
      idPaymentMethod: paymentData.idPaymentMethod,
      paymentType: paymentData.paymentType,
      paymentMethodCode: paymentData.paymentMethodCode,
      value: paymentData.value,
      total: paymentData.value,
      term: paymentData.term,
      timeUnit: paymentData.time,
      time: paymentData.time,
    };

    // Crear una nueva referencia del array para que Angular detecte el cambio con OnPush
    this.paymentsDataSource = [...this.paymentsDataSource, payment];
    this.cdr.markForCheck();
  }

  /**
   * Elimina una forma de pago
   */
  removePayment(index: number): void {
    // Crear una nueva referencia del array para que Angular detecte el cambio con OnPush
    this.paymentsDataSource = this.paymentsDataSource.filter((_, i) => i !== index);
    this.calculateTotals();
    this.cdr.markForCheck();
  }

  /**
   * Calcula los totales de la nota de débito
   */
  calculateTotals(): void {
    this.subtotalZero = 0;
    this.subtotalIva = 0;
    this.subtotalIce = 0;
    this.totalDiscount = 0;
    this.totalIva = 0;
    this.totalIce = 0;
    this.valorModificacion = 0;

    // Agrupar por porcentaje de IVA (15%, 5%, 0%)
    const subtotalsByIvaMap = new Map<number, number>([
      [15, 0],
      [5, 0],
      [0, 0],
    ]);

    this.productsDataSource.data.forEach((product) => {
      const subtotal = product.subtotal || 0;
      const discount = product.discount || 0;
      const ivaPercent = product.ivaPercentage || 0;
      const icePercent = product.icePercentage || 0;

      // Calcular subtotal con descuento
      const subtotalWithDiscount = subtotal - discount;
      this.totalDiscount += discount;

      // Agrupar por IVA
      const currentSubtotal = subtotalsByIvaMap.get(ivaPercent) || 0;
      subtotalsByIvaMap.set(ivaPercent, currentSubtotal + subtotalWithDiscount);

      // Calcular IVA e ICE
      if (ivaPercent > 0) {
        const ivaValue = (subtotalWithDiscount * ivaPercent) / 100;
        this.totalIva += ivaValue;
      }

      if (icePercent > 0) {
        const iceValue = (subtotalWithDiscount * icePercent) / 100;
        this.totalIce += iceValue;
      }

      // Agrupar en subtotales
      if (ivaPercent === 0 && icePercent === 0) {
        this.subtotalZero += subtotalWithDiscount;
      } else {
        this.subtotalIva += subtotalWithDiscount;
      }

      if (icePercent > 0) {
        this.subtotalIce += subtotalWithDiscount;
      }

      this.valorModificacion += subtotalWithDiscount;
    });

    // Calcular subtotales e IVA por cada tarifa
    this.subtotalsByIva = [];
    subtotalsByIvaMap.forEach((subtotal, ivaPercent) => {
      const ivaValue = subtotal * (ivaPercent / 100);
      const subtotalRounded = Math.round(subtotal * 100) / 100;
      const ivaValueRounded = Math.round(ivaValue * 100) / 100;

      this.subtotalsByIva.push({
        ivaPercent: ivaPercent,
        subtotal: subtotalRounded,
        ivaValue: ivaValueRounded,
      });
    });

    // Ordenar por % de IVA descendente (15%, 5%, 0%)
    this.subtotalsByIva.sort((a, b) => b.ivaPercent - a.ivaPercent);

    // Valor total = valorModificacion + totalIva + totalIce
    // Aplicar redondeo matemático correcto a 2 decimales
    // Ejemplo: 173.075 se redondea correctamente a 173.08
    const totalCalculado = this.valorModificacion + this.totalIva + this.totalIce;
    this.valorTotal = Math.round(totalCalculado * 100) / 100;

    this.cdr.markForCheck();
  }

  /**
   * Formatea una fecha a formato YYYY-MM-DD (solo fecha, sin hora)
   */
  private formatDateOnly(dateValue: string | Date | null | undefined): string {
    if (!dateValue) {
      return '';
    }

    // Si es un string ISO (con hora), extraer solo la fecha
    if (typeof dateValue === 'string') {
      // Si contiene 'T', tomar solo la parte antes de 'T'
      if (dateValue.includes('T')) {
        return dateValue.split('T')[0];
      }
      // Si ya está en formato YYYY-MM-DD, retornarlo tal cual
      return dateValue;
    }

    // Si es un objeto Date, convertir a YYYY-MM-DD usando fecha local (sin conversión de zona horaria)
    if (dateValue instanceof Date) {
      const year = dateValue.getFullYear();
      const month = String(dateValue.getMonth() + 1).padStart(2, '0');
      const day = String(dateValue.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }

    return '';
  }

  /**
   * Obtiene el total de formas de pago
   */
  getTotalPayments(): number {
    const total = this.paymentsDataSource.reduce((total, payment) => {
      return total + (payment.total || payment.value || 0);
    }, 0);
    // Redondear a 2 decimales para evitar problemas de precisión
    return Math.round(total * 100) / 100;
  }

  /**
   * Verifica si el total de pagos coincide con el valor total (con tolerancia de 0.01)
   */
  paymentsMatchTotal(): boolean {
    const totalPayments = this.getTotalPayments();
    const difference = Math.abs(this.valorTotal - totalPayments);
    // Tolerancia de 0.01 para errores de redondeo
    return difference <= 0.01;
  }

  /**
   * Actualiza el valor de un pago
   */
  updatePaymentValue(index: number, value: number): void {
    if (this.paymentsDataSource[index]) {
      const payment = this.paymentsDataSource[index];
      const numericValue = parseFloat(value.toString()) || 0;
      const totalInvoice = this.valorTotal;

      // Redondear el valor ingresado a 2 decimales
      const roundedValue = Math.round(numericValue * 100) / 100;

      // Si el valor excede el total (con tolerancia de 0.01), mostrar advertencia
      const difference = roundedValue - totalInvoice;
      if (difference > 0.01) {
        this.toast.warning(
          `El valor ingresado ($${roundedValue.toFixed(
            2
          )}) supera el valor a pagar ($${totalInvoice.toFixed(2)})`
        );
      }

      payment.value = roundedValue;
      payment.total = roundedValue;
      this.cdr.markForCheck();
    }
  }

  /**
   * Previene el submit del formulario
   */
  preventFormSubmit(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
  }

  /**
   * Maneja el evento Enter en el input de valor de pago
   * Previene el submit del formulario y actualiza el valor
   */
  onPaymentValueEnter(
    event: KeyboardEvent,
    index: number,
    value: any
  ): boolean {
    // Prevenir el comportamiento por defecto (submit del formulario)
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();

    // Obtener el valor actual del input
    const inputElement = event.target as HTMLInputElement;
    const currentValue = parseFloat(inputElement.value) || 0;

    // Usar el método updatePaymentValue para mantener la consistencia y validación
    this.updatePaymentValue(index, currentValue);

    // Hacer blur después de un pequeño delay para asegurar que el valor se actualizó
    setTimeout(() => {
      inputElement.blur();
    }, 0);

    // Retornar false para prevenir cualquier otro comportamiento
    return false;
  }

  /**
   * Construye el payload para crear/actualizar la nota de débito
   */
  private buildDebitNotePayload(): DebitNoteDto {
    const formValue = this.form.value;
    const authObj = this.selectedInvoice.authorizationSri as any;

    // Construir detalles de la nota de débito con productDto completo
    const debitNoteDetails: InvoiceDetailDto[] =
      this.productsDataSource.data.map((product) => {
        // Obtener valores de IVA desde productDto.ivaTaxType
        const ivaValue =
          product.productDto?.ivaTaxType?.value || product.ivaPercentage || 0;
        const rate = ivaValue / 100; // Convertir porcentaje a decimal (15% → 0.15)

        return {
          idProduct: product.idProduct,
          productDto: product.productDto
            ? {
                idProduct: product.productDto.idProduct,
                mainCode: product.productDto.mainCode || '',
                name: product.productDto.name || '',
                description:
                  product.productDto.description ||
                  product.productDto.name ||
                  '',
                unitPrice: product.unitPrice,
                rate: rate,
                taxCode: '2', // Código de impuesto IVA
                taxCodPercentage: '4', // Código de porcentaje IVA
                status:
                  product.productDto.status !== undefined
                    ? product.productDto.status
                    : true,
              }
            : undefined,
          unitPrice: product.unitPrice,
          discount: product.discount || 0,
          subtotal: product.subtotal,
          quantity: product.quantity,
          ivaPercentage: product.ivaPercentage,
        };
      });

    // Obtener idCompany (prioridad: factura seleccionada, luego localStorage)
    const companyId = this.getCompanyId();
    if (!companyId || companyId === 0) {
      this.logger.error('No se pudo obtener idCompany', {
        fromInvoice: this.selectedInvoice.idCompany,
        fromLocalStorage: localStorage.getItem(environment.ID_COMPANY),
      });
      throw new Error('No se pudo obtener el ID de la compañía');
    }

    // Obtener idIssuancePoint
    const issuancePointId = this.getIssuancePointId();
    if (!issuancePointId || issuancePointId <= 0) {
      this.logger.error('No se pudo obtener idIssuancePoint', {
        fromInvoice: (this.selectedInvoice as any)?.establishment?.issuancePoint
          ?.idIssuancePoint,
        fromLocalStorage: localStorage.getItem('INVOICE_POINT_DATA'),
      });
      throw new Error('No se pudo obtener el punto de emisión');
    }

    // Construir companyDto con todos los campos requeridos
    const companyDto = this.companyData
      ? {
          idCompany: companyId,
          identification: this.companyData.identification || '',
          name: this.companyData.name || '',
          status:
            this.companyData.status !== undefined
              ? this.companyData.status
              : true,
        }
      : {
          idCompany: companyId,
          identification: '',
          name: '',
          status: true,
        };

    // Construir clientDto con email (campo requerido)
    const clientId =
      this.selectedInvoice.idClient ||
      this.selectedInvoice.clientDto?.idClient ||
      0;

    // Obtener email con prioridad y asegurar que siempre tenga un valor válido
    const clientEmail =
      this.selectedInvoice.buyerMail ||
      this.selectedInvoice.clientDto?.email ||
      '';

    // Validar que el email esté presente
    if (!clientEmail || clientEmail.trim() === '') {
      this.logger.warn(
        'Email del cliente no encontrado, usando valor por defecto',
        {
          buyerMail: this.selectedInvoice.buyerMail,
          clientDtoEmail: this.selectedInvoice.clientDto?.email,
        }
      );
    }

    // Construir clientDto completo incluyendo todos los campos posibles
    // Buscar datos del cliente en múltiples ubicaciones dentro de selectedInvoice
    const invoice = this.selectedInvoice as any;
    const clientDto = {
      idClient: clientId,
      identification:
        this.selectedInvoice.buyerIdentification ||
        this.selectedInvoice.clientDto?.identification ||
        invoice.clientIdentification ||
        invoice.client?.identification ||
        '',
      name:
        this.selectedInvoice.buyerName ||
        this.selectedInvoice.clientDto?.name ||
        invoice.clientName ||
        invoice.client?.name ||
        invoice.nameClient ||
        '',
      idType:
        this.selectedInvoice.buyerIdType ||
        this.selectedInvoice.clientDto?.idType ||
        invoice.clientIdType ||
        invoice.client?.idType ||
        4,
      email: clientEmail || '', // Asegurar que siempre sea un string válido
      // Incluir campos adicionales si están disponibles
      address:
        this.selectedInvoice.buyerAddress ||
        this.selectedInvoice.clientDto?.address ||
        invoice.clientAddress ||
        invoice.client?.address ||
        undefined,
      phone:
        this.selectedInvoice.buyerPhone ||
        this.selectedInvoice.clientDto?.phone ||
        invoice.clientPhone ||
        invoice.client?.phone ||
        undefined,
    };

    // Log para debugging
    this.logger.debug('ClientDto construido para nota de débito:', {
      clientDto,
      selectedInvoiceKeys: Object.keys(this.selectedInvoice),
      hasBuyerName: !!this.selectedInvoice.buyerName,
      hasBuyerIdentification: !!this.selectedInvoice.buyerIdentification,
      hasClientDto: !!this.selectedInvoice.clientDto,
    });

    // Obtener información del establishment y issuance point desde INVOICE_POINT_DATA
    let establishmentCode = '001';
    let issuancePointCode = '001';
    let establishmentType = 'MATRIZ';
    let idEstablishment = 0;

    // Intentar obtener desde INVOICE_POINT_DATA
    const invoicePointDataStr = localStorage.getItem('INVOICE_POINT_DATA');
    if (invoicePointDataStr) {
      try {
        const invoicePointData = JSON.parse(invoicePointDataStr);
        if (invoicePointData?.code) {
          issuancePointCode = invoicePointData.code;
        }
        if (invoicePointData?.establishmentCode) {
          establishmentCode = invoicePointData.establishmentCode;
        }
        if (invoicePointData?.establishmentType) {
          establishmentType = invoicePointData.establishmentType;
        }
        if (invoicePointData?.establishmentId) {
          idEstablishment = invoicePointData.establishmentId;
        }
      } catch (e) {
        this.logger.warn('Error al parsear INVOICE_POINT_DATA', e);
      }
    }

    // Fallback a selectedInvoice.establishment si está disponible
    if (this.selectedInvoice.establishment) {
      idEstablishment =
        this.selectedInvoice.establishment.idEstablishment || idEstablishment;
      establishmentCode =
        this.selectedInvoice.establishment.code || establishmentCode;
    }

    // Construir establishment según formato esperado por el backend
    const establishment = {
      idEstablishment: idEstablishment,
      type: establishmentType,
      code: establishmentCode,
      issuancePoint: {
        idIssuancePoint: issuancePointId,
        code: issuancePointCode,
      },
    };

    // Construir paymentMethods con idPaymentMethod, paymentMethodCode, paymentType y total
    // Incluir paymentType para que se muestre correctamente en el ticket
    const paymentMethods = this.paymentsDataSource.map((payment) => ({
      idPaymentMethod: payment.idPaymentMethod,
      paymentMethodCode:
        payment.paymentMethodCode || payment.paymentType || '01',
      paymentType: payment.paymentType || payment.paymentMethod || 'Pago',
      paymentMethod: payment.paymentMethod || payment.paymentType || 'Pago',
      total: payment.total || payment.value || 0,
      value: payment.value || payment.total || 0,
    }));

    // Construir DTO
    const debitNoteDto: DebitNoteDto = {
      idCompany: companyId,
      companyDto: companyDto,
      clientDto: clientDto,
      establishment: establishment,
      idIssuancePoint: issuancePointId,
      issueDate: this.formatDateOnly(formValue.issueDate),
      originalInvoiceId: formValue.originalInvoiceId,
      originalInvoiceNumber: formValue.originalInvoiceNumber,
      originalIssueDate: this.formatDateOnly(formValue.originalIssueDate),
      originalAccessKey: formValue.originalAccessKey,
      documentTypeModified: formValue.documentTypeModified || '01',
      observation: formValue.observation || '',
      debitNoteDetails: debitNoteDetails,
      paymentMethods: paymentMethods,

      // Totales (se calculan automáticamente en el backend, pero los enviamos también)
      totalAmount: this.valorTotal,
      discount: this.totalDiscount,
      subtotalZero: this.subtotalZero,
      subtotalIva: this.subtotalIva,
      subtotalIce: this.subtotalIce,
      totalIva: this.totalIva,
      totalIce: this.totalIce,
      valorModificacion: this.valorModificacion,
      valorTotal: this.valorTotal,
    };

    // Si estamos editando un borrador, incluir el ID
    if (this.draftId) {
      debitNoteDto.idDebitNote = this.draftId;
    }

    return debitNoteDto;
  }

  /**
   * Guarda la nota de débito como borrador
   */
  saveDraft(): void {
    // Validaciones
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Complete todos los campos requeridos');
      return;
    }

    if (this.productsDataSource.data.length === 0) {
      this.toast.error('Agregue al menos un producto o servicio');
      return;
    }

    // Validar que tenemos idCompany antes de continuar
    const companyId = this.getCompanyId();
    if (!companyId || companyId === 0) {
      this.toast.error(
        'Error: No se pudo obtener el ID de la compañía. Por favor, recargue la página e intente nuevamente.'
      );
      this.logger.error('idCompany es 0 o no existe al guardar borrador', {
        fromLocalStorage: localStorage.getItem(environment.ID_COMPANY),
        fromInvoice: this.selectedInvoice?.idCompany,
      });
      return;
    }

    this.isSavingDraft = true;
    this.cdr.markForCheck();

    const payload = this.buildDebitNotePayload();

    this.debitNoteService
      .createDraft(payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isSavingDraft = false;
          this.cdr.markForCheck();
        }),
        catchError((error) => {
          this.logger.error('Error al guardar borrador', error);
          this.toast.error(
            error.error?.message || 'Error al guardar el borrador'
          );
          return of(null);
        })
      )
      .subscribe((response) => {
        if (response?.code === 200 || response?.idDebitNote) {
          this.toast.success('Borrador guardado exitosamente');

          // Cerrar diálogo y retornar éxito
          this.dialogRef.close({
            success: true,
            draft: true,
            response,
          });
        }
      });
  }

  /**
   * Crea la nota de débito y la envía al SRI
   */
  createAndSend(): void {
    // Validaciones
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Complete todos los campos requeridos');
      return;
    }

    if (this.productsDataSource.data.length === 0) {
      this.toast.error('Agregue al menos un producto o servicio');
      return;
    }

    if (this.paymentsDataSource.length === 0) {
      this.toast.error('Agregue al menos una forma de pago');
      return;
    }

    // Nota: El backend ajusta las formas de pago si no coinciden exactamente
    // Solo validamos que haya al menos una forma de pago agregada
    // La contraseña de firma se obtiene del backend (asociada a la compañía)

    // Validar que tenemos idCompany antes de continuar
    const companyId = this.getCompanyId();
    if (!companyId || companyId === 0) {
      this.toast.error(
        'Error: No se pudo obtener el ID de la compañía. Por favor, recargue la página e intente nuevamente.'
      );
      this.logger.error('idCompany es 0 o no existe', {
        fromLocalStorage: localStorage.getItem(environment.ID_COMPANY),
        fromInvoice: this.selectedInvoice?.idCompany,
      });
      return;
    }

    // Validar límites de documentos antes de crear nota de débito
    this.permissionService.canEmitDocument('DEBIT_NOTE').subscribe({
      next: (allowed) => {
        if (!allowed) {
          this.toast.error(
            'Has alcanzado el límite de notas de débito de tu plan. Por favor, actualiza tu plan para continuar emitiendo documentos.'
          );
          this.logger.warn('Intento de crear nota de débito con límite alcanzado');
          return;
        }

        // Si puede emitir, continuar con el proceso normal
        this.processDebitNoteCreation();
      },
      error: (err) => {
        this.logger.error('Error al validar límites de documentos', err);
        this.toast.warning('Advertencia: No se pudo validar los límites de documentos. Continuando...');
        // En caso de error, permitir continuar (fallback)
        this.processDebitNoteCreation();
      },
    });
  }

  /**
   * Procesa la creación de la nota de débito después de validar límites
   */
  private processDebitNoteCreation(): void {
    this.isSendingToSri = true;
    this.cdr.markForCheck();

    const payload = this.buildDebitNotePayload();
    this.logger.debug('Request completo:', payload);

    this.debitNoteService
      .create(payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isSendingToSri = false;
          this.cdr.markForCheck();
        }),
        catchError((error) => {
          this.logger.error('Error al crear nota de débito', error);
          this.toast.error(
            error.error?.message || 'Error al crear la nota de débito'
          );
          return of(null);
        })
      )
      .subscribe((response) => {
        if (
          response?.code === 200 ||
          response?.status === 'OK' ||
          response?.idDebitNote
        ) {
          // Guardar datos en sessionStorage para impresión inmediata del ticket
          // El componente de ticket buscará primero en sessionStorage antes de llamar al backend
          if (response.idDebitNote) {
            const storageKey = `DEBIT_NOTE_TICKET_${response.idDebitNote}`;
            try {
              // ✅ COMBINAR: Payload enviado + Datos de respuesta del backend
              // Similar a cómo se hace en billing.component.ts con mergeInvoiceData
              // DebitNoteResponse puede tener debitNoteDto o los datos directamente en response
              const responseData = (response as any)?.data || response?.debitNoteDto || response;
              const completeDebitNoteData = this.mergeDebitNoteData(
                payload,
                responseData
              );
              
              sessionStorage.setItem(
                storageKey,
                JSON.stringify(completeDebitNoteData)
              );
              this.logger.debug('✅ Datos de nota de débito guardados en sessionStorage', {
                debitNoteId: response.idDebitNote,
                storageKey,
              });
            } catch (error) {
              this.logger.warn('Error al guardar en sessionStorage', error);
              // Continuar aunque falle el sessionStorage
            }
          }

          // Mostrar diálogo de éxito
          const dialogData: SuccessDebitNoteDialogData = {
            debitNoteId: response.idDebitNote!,
            debitNoteNumber: response.debitNoteNumber || undefined,
            clientName:
              this.selectedInvoice.buyerName ||
              this.selectedInvoice.clientDto?.name ||
              '',
            total: this.valorTotal,
            authorizationStatusSri: (response as any).authorizationStatusSri || response.debitNoteDto?.authorizationStatusSri || undefined,
          };

          this.dialog.open(SuccessDebitNoteDialogComponent, {
            width: '450px',
            disableClose: false,
            data: dialogData,
          });

          // Cerrar diálogo de creación y retornar éxito
          this.dialogRef.close({
            success: true,
            draft: false,
            response,
          });
        } else {
          const errorMsg =
            response?.errors?.map((e: any) => e.message).join(', ') ||
            response?.message ||
            'Error al procesar nota de débito';
          this.toast.error(errorMsg);
        }
      });
  }

  /**
   * Cancela y cierra el diálogo
   */
  cancel(): void {
    this.dialogRef.close(null);
  }

  /**
   * Formatea moneda
   */
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-EC', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  }

  /**
   * Obtiene el número de factura original
   */
  getOriginalInvoiceNumber(): string {
    return (
      this.selectedInvoice.invoiceNumber ||
      this.form.value.originalInvoiceNumber ||
      '-'
    );
  }

  /**
   * Obtiene el nombre del cliente
   * Busca en todas las posibles ubicaciones
   */
  getClientName(): string {
    const inv = this.selectedInvoice as any;

    // Buscar en todas las posibles ubicaciones del nombre
    if (this.selectedInvoice.buyerName) return this.selectedInvoice.buyerName;
    if (this.selectedInvoice.clientDto?.name)
      return this.selectedInvoice.clientDto.name;
    if (inv.clientName) return inv.clientName;
    if (inv.client?.name) return inv.client.name;
    if (inv.nameClient) return inv.nameClient;

    // Si solo tenemos idClient, mostrarlo como referencia
    if (inv.idClient || this.selectedInvoice.idClient) {
      return `Cliente #${inv.idClient || this.selectedInvoice.idClient}`;
    }

    return '-';
  }

  /**
   * Obtiene la identificación del cliente
   * Busca en todas las posibles ubicaciones
   */
  getClientIdentification(): string {
    const inv = this.selectedInvoice as any;

    // Buscar en todas las posibles ubicaciones de la identificación
    if (this.selectedInvoice.buyerIdentification) {
      return this.selectedInvoice.buyerIdentification;
    }
    if (this.selectedInvoice.clientDto?.identification) {
      return this.selectedInvoice.clientDto.identification;
    }
    if (inv.clientIdentification) return inv.clientIdentification;
    if (inv.client?.identification) return inv.client.identification;
    if (inv.identification) return inv.identification;

    // Si solo tenemos idClient
    if (inv.idClient || this.selectedInvoice.idClient) {
      return `ID: ${inv.idClient || this.selectedInvoice.idClient}`;
    }

    return '-';
  }
}
