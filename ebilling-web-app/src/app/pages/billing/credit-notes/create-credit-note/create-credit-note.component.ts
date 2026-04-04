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
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { CreditNoteService } from '../../../../_service/invoice/credit-note.service';
import { InvoiceService } from '../../../../_service/invoice/invoice.service';
import {
  CreditNoteReasonDto,
  InvoiceAvailableForCreditNoteDto,
  InvoiceDetailAvailableDto,
  CreateCreditNoteRequest,
  CreditNoteDto,
} from '../../../../_model/invoice/credit-note-dto';
import { InvoiceDto } from '../../../../_model/invoice/invoice-dto';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { environment } from '../../../../../environments/environment';
import { extractUserFriendlyErrorMessage } from '../../../../_utils/error-message.util';
import { PermissionService } from '../../../../_service/permission/permission.service';

export interface CreateCreditNoteDialogData {
  invoice?: InvoiceDto;
  draftId?: number; // ID del borrador a editar (opcional)
}

@Component({
  selector: 'app-create-credit-note',
  templateUrl: './create-credit-note.component.html',
  styleUrls: ['./create-credit-note.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateCreditNoteComponent implements OnInit, OnDestroy {
  // Datos
  reasons: CreditNoteReasonDto[] = [];
  invoiceData: InvoiceAvailableForCreditNoteDto | null = null;
  selectedInvoice: InvoiceDto;
  draftId?: number; // ID del borrador que se está editando (si existe)

  // Formulario
  form: FormGroup;

  // Estados
  currentStep = 1;
  isLoadingReasons = false;
  isLoadingInvoice = false;
  isSavingDraft = false;
  isSendingToSri = false;

  // Totales calculados
  subtotalZero = 0;
  subtotalIva = 0;
  totalIva = 0;
  totalAmount = 0;

  // Fechas válidas para emisión NC
  minDate: string = '';
  maxDate: string = '';
  minDateDisplay: string = '';
  maxDateDisplay: string = '';

  private destroy$ = new Subject<void>();

  constructor(
    public dialogRef: MatDialogRef<CreateCreditNoteComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateCreditNoteDialogData,
    private readonly fb: FormBuilder,
    private readonly creditNoteService: CreditNoteService,
    private readonly invoiceService: InvoiceService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef,
    private readonly permissionService: PermissionService
  ) {
    this.selectedInvoice = data.invoice || ({} as InvoiceDto);
    this.initDateLimits();
    this.initForm();
  }

  /**
   * Inicializa los límites de fecha para la NC
   * Min: fecha de emisión de la factura
   * Max: fecha actual
   */
  private initDateLimits(): void {
    // Fecha máxima: hoy
    const today = new Date();
    this.maxDate = today.toISOString().split('T')[0];
    this.maxDateDisplay = this.formatDateDisplay(today);

    // Fecha mínima: fecha de la factura o creación
    const invoiceDate =
      this.selectedInvoice.issueDate ||
      this.selectedInvoice.creationDate ||
      this.maxDate;

    const minDateObj = new Date(invoiceDate);
    this.minDate = minDateObj.toISOString().split('T')[0];
    this.minDateDisplay = this.formatDateDisplay(minDateObj);

    this.logger.debug('Date limits:', {
      minDate: this.minDate,
      maxDate: this.maxDate,
    });
  }

  /**
   * Formatea fecha para mostrar al usuario
   */
  private formatDateDisplay(date: Date): string {
    return date.toLocaleDateString('es-EC', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  ngOnInit(): void {
    this.loadReasons();
    
    // Si hay draftId, cargar el borrador; si no, cargar detalles de la factura
    if (this.data.draftId) {
      this.draftId = this.data.draftId;
      this.loadDraftForEditing(this.data.draftId);
    } else {
      this.loadInvoiceDetails();
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      idReason: [null, Validators.required],
      observation: [''],
      issueDate: [new Date().toISOString().split('T')[0], Validators.required],
      details: this.fb.array([]),
    });
  }

  get detailsArray(): FormArray {
    return this.form.get('details') as FormArray;
  }

  /**
   * Carga los motivos de NC
   */
  private loadReasons(): void {
    this.isLoadingReasons = true;
    this.cdr.markForCheck();

    this.creditNoteService
      .getReasons()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (reasons) => {
          this.reasons = reasons;
          this.isLoadingReasons = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logger.error('Error al cargar motivos', err);
          this.isLoadingReasons = false;
          this.showError('Error al cargar motivos de nota de crédito');
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Carga un borrador para edición
   */
  private loadDraftForEditing(draftId: number): void {
    this.isLoadingInvoice = true;
    this.cdr.markForCheck();

    this.logger.debug('📝 Cargando borrador con ID:', draftId);

    // Esperar a que se carguen los motivos antes de continuar
    const loadDraft = () => {
      this.creditNoteService
        .getDraftById(draftId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (draft: CreditNoteDto) => {
            this.logger.debug('✅ Borrador cargado:', draft);

            // Cargar la factura original asociada
            const originalInvoiceId = draft.originalInvoiceId;
            if (!originalInvoiceId) {
              this.showError('No se encontró la factura original asociada');
              this.isLoadingInvoice = false;
              this.cdr.markForCheck();
              return;
            }

            // Obtener idCompany del borrador o del localStorage antes de cargar la factura
            const companyId = draft.idCompany || this.getCompanyId();
            if (!companyId || companyId === 0) {
              this.showError('No se encontró el ID de la compañía');
              this.isLoadingInvoice = false;
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
            this.creditNoteService
              .getInvoiceAvailableForCreditNote(originalInvoiceId)
              .pipe(takeUntil(this.destroy$))
              .subscribe({
                next: (invoiceData) => {
                  this.invoiceData = invoiceData;

                  // Poblar el formulario con los datos del borrador
                  this.populateFormFromDraft(draft);
                  this.isLoadingInvoice = false;
                  this.cdr.markForCheck();
                },
                error: (err) => {
                  this.logger.error('Error al cargar factura original', err);
                  this.showError('Error al cargar la factura original');
                  this.isLoadingInvoice = false;
                  this.cdr.markForCheck();
                },
              });
          },
          error: (err) => {
            this.logger.error('Error al cargar borrador', err);
            this.showError('Error al cargar el borrador');
            this.isLoadingInvoice = false;
            this.cdr.markForCheck();
          },
        });
    };

    // Si los motivos ya están cargados, proceder inmediatamente
    if (this.reasons.length > 0) {
      loadDraft();
    } else {
      // Esperar a que se carguen los motivos (máximo 5 segundos)
      const checkReasons = setInterval(() => {
        if (this.reasons.length > 0) {
          clearInterval(checkReasons);
          loadDraft();
        }
      }, 100);
      
      setTimeout(() => {
        clearInterval(checkReasons);
        if (this.reasons.length === 0) {
          this.logger.warn('Motivos no cargados después de 5 segundos, continuando de todas formas');
          loadDraft();
        }
      }, 5000);
    }
  }

  /**
   * Pobla el formulario con los datos del borrador
   */
  private populateFormFromDraft(draft: CreditNoteDto): void {
    // Establecer motivo
    if (draft.reason && this.reasons.length > 0) {
      // Buscar el motivo por descripción o código
      const reason = this.reasons.find(
        (r) => r.description === draft.reason || r.code === draft.reason
      );
      if (reason) {
        this.form.patchValue({ idReason: reason.idReason });
      }
    }

    // Establecer fecha de emisión
    if (draft.issueDate) {
      const issueDate = new Date(draft.issueDate);
      this.form.patchValue({
        issueDate: issueDate.toISOString().split('T')[0],
      });
    }

    // Establecer observación
    if (draft.additionalInformation) {
      this.form.patchValue({ observation: draft.additionalInformation });
    }

    // Cargar detalles del borrador
    const draftDetails = draft.creditNoteDetails || draft.details || [];
    if (draftDetails.length > 0 && this.invoiceData) {
      // Mapear los detalles del borrador a InvoiceDetailAvailableDto
      const invoiceData = this.invoiceData; // Guardar referencia para evitar problemas de null
      const availableDetails: InvoiceDetailAvailableDto[] = draftDetails.map(
        (detail) => {
          // Buscar el detalle original en invoiceData
          const originalDetail = invoiceData.details?.find(
            (d) => d.idInvoiceDetail === detail.idInvoiceDetail
          );

          return {
            idInvoiceDetail: detail.idInvoiceDetail || 0,
            idProduct: detail.idProduct || 0,
            mainCode:
              originalDetail?.mainCode ||
              detail.productDto?.code ||
              `PROD-${detail.idProduct}`,
            auxCode: originalDetail?.auxCode,
            description:
              detail.description ||
              detail.productDto?.description ||
              detail.productDto?.name ||
              'Producto',
            quantityInvoiced: originalDetail?.quantityInvoiced || detail.quantity || 0,
            quantityReturned: originalDetail?.quantityReturned || 0,
            quantityAvailable:
              (originalDetail?.quantityInvoiced || detail.quantity || 0) -
              (originalDetail?.quantityReturned || 0),
            unitPrice: detail.unitPrice || 0,
            discount: detail.discount || 0,
            subtotal: detail.subtotal || 0,
            ivaPercentage: detail.ivaPercentage || 15,
          };
        }
      );

      // Construir el FormArray con los detalles del borrador
      this.buildDetailsFormArray(availableDetails);

      // Marcar como seleccionados los items que están en el borrador
      this.detailsArray.controls.forEach((control, index) => {
        const detail = availableDetails[index];
        if (detail && detail.quantityAvailable > 0) {
          control.get('selected')?.setValue(true);
          control.get('quantity')?.setValue(detail.quantityAvailable);
        }
      });

      // Recalcular totales
      this.calculateTotals();
    }

    this.logger.debug('✅ Formulario poblado con datos del borrador');
  }

  /**
   * Carga los detalles de la factura disponibles para NC
   */
  private loadInvoiceDetails(): void {
    const idInvoice = this.selectedInvoice.idInvoice;
    if (!idInvoice) {
      this.showError('No se encontró el ID de la factura');
      return;
    }

    this.isLoadingInvoice = true;
    this.cdr.markForCheck();

    // Cargar directamente los datos disponibles para NC
    // El endpoint available-for-credit-note contiene toda la información necesaria
    // y es más confiable que cargar la factura completa
    this.logger.debug('Loading invoice available data for credit note:', idInvoice);
    this.loadInvoiceAvailableData(idInvoice);
  }

  /**
   * Carga los datos disponibles para NC desde el endpoint específico
   */
  private loadInvoiceAvailableData(idInvoice: number): void {
    this.creditNoteService
      .getInvoiceAvailableForCreditNote(idInvoice)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.logger.debug('Invoice available data:', data);
          this.invoiceData = data;

          // Si el endpoint devuelve details, usarlos
          if (data.details && data.details.length > 0) {
            this.buildDetailsFormArray(data.details);
          } else {
            // Fallback: usar los detalles de la factura original
            this.logger.debug(
              'No details from endpoint, using invoice details:',
              this.selectedInvoice.invoiceDetails
            );
            this.buildDetailsFromInvoice();
          }

          this.isLoadingInvoice = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logger.error('Error al cargar detalle de factura', err);
          // Fallback: usar los detalles de la factura original
          this.logger.debug(
            'Using fallback - invoice details:',
            this.selectedInvoice.invoiceDetails
          );
          this.buildInvoiceDataFromSelected();
          this.buildDetailsFromInvoice();
          this.isLoadingInvoice = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Construye invoiceData desde la factura seleccionada (fallback)
   */
  private buildInvoiceDataFromSelected(): void {
    const inv = this.selectedInvoice;
    const authObj = inv.authorizationSri as any;

    this.invoiceData = {
      idInvoice: inv.idInvoice!,
      invoiceNumber:
        inv.invoiceNumber ||
        `${inv.establishment?.code || '001'}-001-${String(
          inv.idInvoice
        ).padStart(9, '0')}`,
      issueDate: inv.issueDate || inv.creationDate || '',
      accessKey: inv.accessKey || authObj?.accessKey || '',
      authorizationNumber:
        inv.authorizationNumber || authObj?.authorizationNumber || '',
      client: {
        idClient: inv.clientDto?.idClient || inv.idClient || 0,
        identification:
          inv.buyerIdentification || inv.clientDto?.identification || '',
        name: inv.buyerName || inv.clientDto?.name || '',
        email: inv.buyerMail || inv.clientDto?.email || '',
        address: inv.buyerAddress || inv.clientDto?.address,
        phone: inv.buyerPhone || inv.clientDto?.phone,
      },
      totalInvoice: inv.totalAmount || 0,
      totalCreditNotesApplied: 0,
      availableAmount: inv.totalAmount || 0,
      details: [],
    };
  }

  /**
   * Construye el FormArray desde los detalles de la factura original
   */
  private buildDetailsFromInvoice(): void {
    const invoiceDetails =
      this.selectedInvoice.invoiceDetails || this.selectedInvoice.details || [];

    this.logger.debug('Building details from invoice:', invoiceDetails);

    const mappedDetails: InvoiceDetailAvailableDto[] = invoiceDetails.map(
      (detail) => ({
        idInvoiceDetail: detail.idInvoiceDetail || 0,
        idProduct: detail.idProduct,
        mainCode:
          detail.code || detail.productDto?.code || `PROD-${detail.idProduct}`,
        auxCode: undefined,
        description:
          detail.description ||
          detail.productDto?.description ||
          detail.productDto?.name ||
          'Producto',
        quantityInvoiced: detail.quantity,
        quantityReturned: 0,
        quantityAvailable: detail.quantity,
        unitPrice: detail.unitPrice,
        discount: detail.discount || 0,
        subtotal: detail.subtotal,
        ivaPercentage: detail.ivaPercentage ?? 15, // Default 15% si no viene
      })
    );

    this.logger.debug('Mapped details:', mappedDetails);
    this.buildDetailsFormArray(mappedDetails);
  }

  /**
   * Construye el FormArray de detalles
   */
  private buildDetailsFormArray(details: InvoiceDetailAvailableDto[]): void {
    this.detailsArray.clear();

    this.logger.debug('Building form array with details:', details);

    details.forEach((detail: any) => {
      // Obtener mainCode y description con fallbacks
      const mainCode =
        detail.mainCode ||
        detail.code ||
        detail.productCode ||
        `PROD-${detail.idProduct}`;
      const description =
        detail.description ||
        detail.productDescription ||
        detail.productName ||
        `Producto #${detail.idProduct}`;

      this.logger.debug(`Detail: ${mainCode} - ${description}`);

      const group = this.fb.group({
        selected: [false],
        idInvoiceDetail: [detail.idInvoiceDetail],
        idProduct: [detail.idProduct],
        mainCode: [mainCode],
        description: [description],
        quantityAvailable: [detail.quantityAvailable],
        quantity: [
          0,
          [Validators.min(0), Validators.max(detail.quantityAvailable)],
        ],
        unitPrice: [detail.unitPrice],
        discount: [detail.discount || 0],
        ivaPercentage: [detail.ivaPercentage ?? 15],
        subtotal: [{ value: 0, disabled: true }],
      });

      // Escuchar cambios en cantidad para recalcular
      group.get('quantity')?.valueChanges.subscribe(() => {
        this.calculateItemSubtotal(group);
        this.calculateTotals();
      });

      group.get('selected')?.valueChanges.subscribe((selected) => {
        if (selected) {
          // Al seleccionar, poner cantidad disponible por defecto
          group.get('quantity')?.setValue(detail.quantityAvailable);
        } else {
          group.get('quantity')?.setValue(0);
        }
      });

      this.detailsArray.push(group);
    });

    this.logger.debug(
      'Form array built with',
      this.detailsArray.length,
      'items'
    );
  }

  /**
   * Calcula el subtotal de un item
   */
  private calculateItemSubtotal(group: FormGroup): void {
    const quantity = group.get('quantity')?.value || 0;
    const unitPrice = group.get('unitPrice')?.value || 0;
    const discount = group.get('discount')?.value || 0;
    const subtotal = quantity * unitPrice - discount;
    group.get('subtotal')?.setValue(subtotal);
  }

  /**
   * Obtiene el subtotal formateado de un item (para la UI)
   */
  getItemSubtotal(detail: any): string {
    const quantity = detail.get('quantity')?.value || 0;
    const unitPrice = detail.get('unitPrice')?.value || 0;
    const discount = detail.get('discount')?.value || 0;
    const subtotal = quantity * unitPrice - discount;
    return this.formatCurrency(subtotal);
  }

  /**
   * Calcula los totales
   */
  calculateTotals(): void {
    this.subtotalZero = 0;
    this.subtotalIva = 0;
    this.totalIva = 0;

    this.detailsArray.controls.forEach((control) => {
      const group = control as FormGroup;
      if (group.get('selected')?.value) {
        const quantity = group.get('quantity')?.value || 0;
        const unitPrice = group.get('unitPrice')?.value || 0;
        const discount = group.get('discount')?.value || 0;
        const ivaPercentage = group.get('ivaPercentage')?.value || 0;

        const subtotal = quantity * unitPrice - discount;

        if (ivaPercentage > 0) {
          this.subtotalIva += subtotal;
          this.totalIva += subtotal * (ivaPercentage / 100);
        } else {
          this.subtotalZero += subtotal;
        }
      }
    });

    this.totalAmount = this.subtotalZero + this.subtotalIva + this.totalIva;
    this.cdr.markForCheck();
  }

  /**
   * Selecciona/deselecciona todos los items
   */
  toggleSelectAll(selectAll: boolean): void {
    this.detailsArray.controls.forEach((control) => {
      control.get('selected')?.setValue(selectAll);
    });
  }

  /**
   * Verifica si todos están seleccionados
   */
  get allSelected(): boolean {
    return this.detailsArray.controls.every(
      (c) => c.get('selected')?.value === true
    );
  }

  /**
   * Verifica si hay al menos un item seleccionado
   */
  get hasSelectedItems(): boolean {
    const result = this.detailsArray.controls.some((c) => {
      const selected = c.get('selected')?.value === true;
      const quantity = c.get('quantity')?.value;
      const hasQuantity = quantity > 0;
      // this.logger.debug('Item check - selected:', selected, 'quantity:', quantity, 'hasQuantity:', hasQuantity);
      return selected && hasQuantity;
    });
    return result;
  }

  /**
   * Avanza al siguiente paso
   */
  nextStep(): void {
    if (this.currentStep < 3) {
      this.currentStep++;
      this.cdr.markForCheck();
    }
  }

  /**
   * Retrocede al paso anterior
   */
  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      this.cdr.markForCheck();
    }
  }

  /**
   * Valida si puede avanzar al siguiente paso
   */
  canProceed(): boolean {
    switch (this.currentStep) {
      case 1:
        const reasonValid = this.form.get('idReason')?.valid === true;
        this.logger.debug('Step 1 - Reason valid:', reasonValid);
        return reasonValid;
      case 2:
        const hasItems = this.hasSelectedItems;
        const hasAmount = this.totalAmount > 0;
        this.logger.debug(
          'Step 2 - hasSelectedItems:',
          hasItems,
          'totalAmount:',
          this.totalAmount,
          'canProceed:',
          hasItems && hasAmount
        );
        return hasItems && hasAmount;
      case 3:
        return true;
      default:
        return false;
    }
  }

  /**
   * Guarda como borrador
   */
  saveDraft(): void {
    if (!this.validateForm()) return;

    this.isSavingDraft = true;
    this.cdr.markForCheck();

    const request = this.buildRequest();

    this.creditNoteService
      .saveDraft(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isSavingDraft = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response: any) => {
          // Backend devuelve: { code: 200, status: "OK", idCreditNote: X }
          if (
            response.status === 'OK' ||
            response.code === 200 ||
            response.success
          ) {
            this.showSuccess(
              `Nota de crédito guardada como borrador (ID: ${response.idCreditNote})`
            );
            this.dialogRef.close({ success: true, draft: true, response });
          } else {
            this.showError(response.message || 'Error al guardar borrador');
          }
        },
        error: (err) => {
          this.logger.error('Error al guardar borrador', err);
          this.showError(err.error?.message || 'Error al guardar borrador');
        },
      });
  }

  /**
   * Envía la NC al SRI
   * La contraseña de firma se obtiene del backend (asociada a la compañía)
   */
  submit(): void {
    if (!this.validateForm()) return;

    // Validar límites de documentos antes de crear nota de crédito
    this.permissionService.canEmitDocument('CREDIT_NOTE').subscribe({
      next: (allowed) => {
        if (!allowed) {
          this.showError(
            'Has alcanzado el límite de notas de crédito de tu plan. Por favor, actualiza tu plan para continuar emitiendo documentos.'
          );
          this.logger.warn('Intento de crear nota de crédito con límite alcanzado');
          return;
        }

        // Si puede emitir, continuar con el proceso normal
        this.processCreditNoteCreation();
      },
      error: (err) => {
        this.logger.error('Error al validar límites de documentos', err);
        // En caso de error, permitir continuar (fallback)
        this.processCreditNoteCreation();
      },
    });
  }

  /**
   * Procesa la creación de la nota de crédito después de validar límites
   */
  private processCreditNoteCreation(): void {
    this.isSendingToSri = true;
    this.cdr.markForCheck();

    const request = this.buildRequest();
    this.logger.debug('[CreditNote] Enviando request al SRI', request);

    this.creditNoteService
      .createAndSend(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isSendingToSri = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response: any) => {
          this.logger.debug('[CreditNote] Respuesta del backend', response);
          if (
            response.status === 'OK' ||
            response.code === 200 ||
            response.success
          ) {
            const ncNumber =
              response.creditNoteNumber || response.idCreditNote || 'N/A';
            this.showSuccess(
              `Nota de crédito creada exitosamente (ID: ${ncNumber})`
            );
            this.dialogRef.close({ success: true, draft: false, response });
          } else {
            const errorMsg =
              response.errors?.map((e: any) => e.message).join(', ') ||
              response.message ||
              'Error al procesar nota de crédito';
            this.showError(errorMsg);
          }
        },
        error: (err) => {
          // Log completo del error para debugging (solo en consola)
          this.logger.error('Error al enviar NC al SRI', {
            status: err?.status,
            url: err?.url,
            message: err?.error?.message,
            error: err,
          });

          // Mostrar mensaje amigable al usuario (sin detalles técnicos)
          const userMessage = this.getUserFriendlyErrorMessage(
            err,
            'enviar la nota de crédito al SRI'
          );
          this.showError(userMessage);
        },
      });
  }

  /**
   * Construye el request para el backend
   */
  private buildRequest(): CreateCreditNoteRequest {
    const selectedDetails = this.detailsArray.controls
      .filter((c) => c.get('selected')?.value && c.get('quantity')?.value > 0)
      .map((c) => {
        const qty = c.get('quantity')?.value || 0;
        const price = c.get('unitPrice')?.value || 0;
        const disc = c.get('discount')?.value || 0;
        return {
          idInvoiceDetail: c.get('idInvoiceDetail')?.value,
          idProduct: c.get('idProduct')?.value,
          quantity: qty,
          unitPrice: price,
          discount: disc,
          subtotal: qty * price - disc,
        };
      });

    // Obtener IDs necesarios
    const issuancePointId = this.getIssuancePointId();
    const companyId = this.getCompanyId();
    const clientId =
      this.invoiceData?.client?.idClient || this.selectedInvoice.idClient || 0;

    const request: CreateCreditNoteRequest = {
      idInvoice: this.selectedInvoice.idInvoice!,
      idReason: this.form.get('idReason')?.value,
      idIssuancePoint: issuancePointId,
      observation: this.form.get('observation')?.value || '',
      issueDate: this.form.get('issueDate')?.value,
      details: selectedDetails,
      companyDto: {
        idCompany: companyId,
      },
      clientDto: {
        idClient: clientId,
      },
    };

    // Si estamos editando un borrador, incluir el ID
    if (this.draftId) {
      (request as any).idCreditNote = this.draftId;
    }

    return request;
  }

  /**
   * Obtiene el ID de la compañía desde localStorage
   */
  private getCompanyId(): number {
    const companyStr = localStorage.getItem(environment.ID_COMPANY);
    if (companyStr) {
      const companyId = Number(companyStr);
      if (!isNaN(companyId) && companyId > 0) {
        return companyId;
      }
    }
    // Intentar obtener del token o selectedInvoice
    return this.selectedInvoice?.idCompany || 0;
  }

  /**
   * Obtiene el ID del punto de emisión
   * Prioridad: 1) Factura seleccionada, 2) localStorage
   */
  private getIssuancePointId(): number {
    // Cast a any para acceder a campos dinámicos del backend
    const invoice = this.selectedInvoice as any;

    // Opción 1: Desde idIssuancePoint directo en la factura
    if (invoice?.idIssuancePoint) {
      this.logger.debug(
        'Usando punto de emisión de factura.idIssuancePoint:',
        invoice.idIssuancePoint
      );
      return invoice.idIssuancePoint;
    }

    // Opción 2: Desde establishment.issuancePoint
    if (invoice?.establishment?.issuancePoint?.idIssuancePoint) {
      this.logger.debug(
        'Usando establishment.issuancePoint.idIssuancePoint:',
        invoice.establishment.issuancePoint.idIssuancePoint
      );
      return invoice.establishment.issuancePoint.idIssuancePoint;
    }

    // Opción 3: Desde INVOICE_POINT_DATA en localStorage
    const pointDataStr = localStorage.getItem('INVOICE_POINT_DATA');
    if (pointDataStr) {
      try {
        const pointData = JSON.parse(pointDataStr);
        if (pointData?.id) {
          this.logger.debug(
            'Usando punto de emisión de INVOICE_POINT_DATA:',
            pointData.id
          );
          return pointData.id;
        }
      } catch (e) {
        this.logger.warn('Error parsing INVOICE_POINT_DATA', e);
      }
    }

    // Opción 4: Desde environment.ID_INVOICE_POINT en localStorage
    const invoicePointId = localStorage.getItem(environment.ID_INVOICE_POINT);
    if (invoicePointId) {
      this.logger.debug('Usando environment.ID_INVOICE_POINT:', invoicePointId);
      return Number(invoicePointId);
    }

    // Si no hay punto de emisión, mostrar error con debug
    this.logger.error('No se encontró punto de emisión. Invoice data:', {
      idIssuancePoint: invoice?.idIssuancePoint,
      establishment: invoice?.establishment,
    });
    return 0;
  }

  /**
   * Valida el formulario
   */
  private validateForm(): boolean {
    if (!this.form.get('idReason')?.valid) {
      this.showError('Debe seleccionar un motivo');
      return false;
    }
    if (!this.hasSelectedItems) {
      this.showError('Debe seleccionar al menos un item');
      return false;
    }
    if (this.totalAmount <= 0) {
      this.showError('El total debe ser mayor a 0');
      return false;
    }
    if (
      this.invoiceData &&
      this.totalAmount > this.invoiceData.availableAmount
    ) {
      this.showError(
        `El total no puede superar el saldo disponible ($${this.invoiceData.availableAmount.toFixed(
          2
        )})`
      );
      return false;
    }
    // Validar punto de emisión
    const issuancePointId = this.getIssuancePointId();
    if (!issuancePointId || issuancePointId <= 0) {
      this.showError(
        'No se encontró el punto de emisión. Por favor, vuelva a iniciar sesión.'
      );
      return false;
    }
    return true;
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
    return `$${value.toFixed(2)}`;
  }

  /**
   * Obtiene el nombre del motivo seleccionado
   */
  getSelectedReasonName(): string {
    const idReason = this.form.get('idReason')?.value;
    const reason = this.reasons.find((r) => r.idReason === idReason);
    return reason?.description || '-';
  }

  private showSuccess(message: string): void {
    this.toast.success(message);
  }

  private showError(message: string): void {
    this.toast.error(message);
  }

  /**
   * Extrae un mensaje de error amigable para el usuario
   * Filtra mensajes técnicos (excepciones Java, stack traces, etc.)
   */
  private getUserFriendlyErrorMessage(error: any, context: string): string {
    return extractUserFriendlyErrorMessage(error, context);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
