import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  ElementRef,
  AfterViewInit,
} from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GuideService } from '../../../_service/guide/guide.service';
import { CarrierService } from '../../../_service/guide/carrier.service';
import { ClientService } from '../../../_service/customer/client.service';
import { ClientHelperService } from '../../../_service/customer/client-helper.service';
import { ProductService } from '../../../_service/product/product.service';
import { CompanyService } from '../../../_service/customer/company.service';
import { InvoicePointService } from '../../../_service/auth/invoice-point.service';
import { InvoiceService } from '../../../_service/invoice/invoice.service';
import { DebitNoteService } from '../../../_service/invoice/debit-note.service';
import { CreditNoteService } from '../../../_service/invoice/credit-note.service';
import { ToastService } from '../../../_service/notification/toast.service';
import {
  DocumentQueryDto,
  DocumentSearchResponseDto,
} from '../../../_model/invoice/document-query-dto';
import {
  InvoiceSearchByNumberOrAccessKeyDto,
  DebitNoteSearchByNumberOrAccessKeyDto,
  CreditNoteSearchByNumberOrAccessKeyDto,
  DocumentBasicInfoDto,
  ProductInfoDto,
} from '../../../_model/guide/document-search-dto';
import {
  GuideDto,
  GuideDetailDto,
  GuideDocumentReferenceDto,
  GuideResponse,
} from '../../../_model/guide/guide-dto';
import { CarrierDto } from '../../../_model/guide/carrier-dto';
import { Client } from '../../../_model/customer/client';
import { ClientRequestDto } from '../../../_model/customer/client-request-dto';
import { ProductDto } from '../../../_model/product/product-dto';
import { ProductResponse } from '../../../_model/product/product-response';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../_service/logger/logger.service';
import {
  IDENTIFICATION_TYPE,
  GUIDE_EMISSION_TYPE,
  GUIDE_DOCUMENT_TYPE,
  GUIDE_SEARCH_BY_TYPE,
  GUIDE_TRANSPORT_TYPE,
  GUIDE_REASON_FOR_TRANSFER,
  CARRIER_IDENTIFICATION_TYPE,
} from '../../../_constants/constants';
import { SelectProductDialogComponent } from '../../billing/select-product-dialog/select-product-dialog.component';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import {
  AddAdditionalFieldDialogComponent,
  AdditionalFieldData,
} from '../add-additional-field-dialog/add-additional-field-dialog.component';
import { UpdateCarrierComponent } from '../../carrier/update-carrier/update-carrier.component';
import { UpdateClientComponent } from '../../customer/client/update-client/update-client.component';
import {
  UpdateProductComponent,
  ProductDialogData,
} from '../../product/update-product/update-product.component';
import {
  SuccessGuideDialogComponent,
  SuccessGuideDialogData,
} from '../success-guide-dialog/success-guide-dialog.component';
import { PermissionService } from '../../../_service/permission/permission.service';
import { decodeToken } from '../../../_functions/functions';
import { forkJoin, of, Observable, Subject } from 'rxjs';
import { catchError, map, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-guide-form',
  templateUrl: './guide-form.component.html',
  styleUrls: ['./guide-form.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuideFormComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('productSearchInput', { read: ElementRef })
  productSearchInput!: ElementRef;

  @ViewChild('originAddressInput', { read: ElementRef })
  originAddressInput!: ElementRef;

  @ViewChild('routeInput', { read: ElementRef })
  routeInput!: ElementRef;

  // Formularios
  guideForm: FormGroup;
  emissionForm: FormGroup;
  carrierForm: FormGroup;
  transferForm: FormGroup;
  recipientForm: FormGroup;
  documentForm: FormGroup;
  detailsForm: FormGroup;

  // Datos
  branchOffices: { id: number; code: string; address: string }[] = [];
  emissionPoints: { id: number; code: string }[] = [];
  carriers: CarrierDto[] = [];
  clients: Client[] = [];
  companyData: any = null;
  maxDate = new Date();
  minDate = new Date(); // Para fecha de inicio (solo hoy)
  todayDate = new Date(); // Fecha de hoy para comparaciones

  // Tipos de identificación (usando constantes)
  idTypes = IDENTIFICATION_TYPE.map((t) => ({
    value: t.code,
    viewValue: t.value,
  }));
  identificationTypes = IDENTIFICATION_TYPE;

  // Tabla de productos
  productCtrl = new FormControl('');
  guideDetailDataSource = new MatTableDataSource<GuideDetailDto>([]);
  displayedColumns: string[] = ['code', 'quantity', 'description', 'actions'];

  // Estados
  loading = false;
  isProcessingGuide = false;
  isSavingDraft = false;
  currentDraftId: number | null = null;
  errorMessage = '';
  canCreateProduct = false; // Se inicializa en ngOnInit

  // Subject para manejar suscripciones
  private destroy$ = new Subject<void>();

  // Tipos de emisión y documento (usando constantes)
  emissionTypes = GUIDE_EMISSION_TYPE;
  documentTypes = GUIDE_DOCUMENT_TYPE;
  searchByTypes = GUIDE_SEARCH_BY_TYPE;

  // Tipos de transporte y motivos (usando constantes)
  transportTypes = GUIDE_TRANSPORT_TYPE;
  reasonForTransferTypes = GUIDE_REASON_FOR_TRANSFER;

  // Tipos de identificación para transportistas
  carrierIdTypes = CARRIER_IDENTIFICATION_TYPE;

  // Productos del documento validado (para mostrar en el diálogo cuando se hace clic en "Buscar en listado")
  private validatedDocumentProducts: ProductDto[] = [];
  private validatedProductInfoMap: Map<number, ProductInfoDto> = new Map();

  // Campos adicionales
  additionalFields: Array<{ name: string; description: string }> = [];
  additionalFieldsDataSource = new MatTableDataSource<{
    name: string;
    description: string;
  }>([]);
  displayedAdditionalFieldsColumns: string[] = [
    'name',
    'description',
    'actions',
  ];

  get guideDetails(): FormArray {
    return this.detailsForm.get('items') as FormArray;
  }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly guideService: GuideService,
    private readonly carrierService: CarrierService,
    private readonly clientService: ClientService,
    private readonly clientHelper: ClientHelperService,
    private readonly productService: ProductService,
    private readonly companyService: CompanyService,
    private readonly invoicePointService: InvoicePointService,
    private readonly invoiceService: InvoiceService,
    private readonly debitNoteService: DebitNoteService,
    private readonly creditNoteService: CreditNoteService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly dialog: MatDialog,
    private readonly cdRef: ChangeDetectorRef,
    private readonly permissionService: PermissionService
  ) {
    // Inicializar formularios
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadInitialData();
    this.checkForDraft();

    // Listener para habilitar campos cuando se cambia o limpia la identificación del transportista
    this.carrierForm
      .get('identification')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value: string) => {
        if (!value || value.trim() === '') {
          // Si se limpia la identificación, habilitar campos para permitir crear nuevo
          this.carrierForm.get('identificationType')?.enable();
          this.carrierForm.get('name')?.enable();
          this.carrierForm.get('email')?.enable();
          // Limpiar otros campos
          this.carrierForm.patchValue({
            identificationType: null,
            name: '',
            email: '',
            address: '',
            phone: '',
            idCarrier: null,
          });
          this.cdRef.detectChanges();
        }
      });

    // Listener para habilitar campos cuando se limpia el valor de búsqueda del documento
    this.documentForm
      .get('searchValue')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value: string) => {
        if (!value || value.trim() === '') {
          // Si se limpia el valor de búsqueda, habilitar campos y limpiar valores
          this.documentForm.get('accessKey')?.enable();
          this.documentForm.get('authorizationNumber')?.enable();
          this.documentForm.get('issueDate')?.enable();
          this.documentForm.get('documentNumber')?.enable();
          // Limpiar campos
          this.documentForm.patchValue({
            accessKey: '',
            authorizationNumber: '',
            issueDate: '',
            documentNumber: '',
          });
          this.cdRef.detectChanges();
        }
      });

    // Listener para habilitar campos cuando se cambia o limpia la identificación del destinatario
    this.recipientForm
      .get('identification')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value: string) => {
        if (!value || value.trim() === '') {
          // Si se limpia la identificación, habilitar campos para permitir crear nuevo
          this.recipientForm.get('identificationType')?.enable();
          this.recipientForm.get('name')?.enable();
          this.recipientForm.get('destinationAddress')?.enable();
          // Limpiar otros campos
          this.recipientForm.patchValue({
            identificationType: null,
            name: '',
            destinationAddress: '',
            idClient: null,
            email: '',
          });
          this.cdRef.detectChanges();
        }
      });

    // Verificar permiso para crear productos (el backend decide según rol y plan)
    this.permissionService.canAccess('PRODUCT_MANAGEMENT').subscribe({
      next: (hasPermission: boolean) => {
        this.canCreateProduct = hasPermission;
        this.cdRef.markForCheck();
        this.logger.debug('Permiso para crear productos:', hasPermission);
      },
      error: (err) => {
        this.logger.error('Error al verificar permiso de productos', err);
        this.canCreateProduct = false; // Por defecto, no permitir si hay error
        this.cdRef.markForCheck();
      },
    });
  }

  ngAfterViewInit(): void {
    // Focus en primer campo si es nueva guía
    if (!this.currentDraftId) {
      setTimeout(() => {
        // Lógica de focus inicial
      }, 100);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    // Formulario de emisión
    this.emissionForm = this.fb.group({
      establishment: [null, Validators.required],
      commercialName: [''],
      emissionPoint: [null, Validators.required],
      issueDate: [this.getTodayDateString(), Validators.required],
    });

    // Formulario de transportista
    this.carrierForm = this.fb.group({
      identification: ['', Validators.required],
      identificationType: [null, Validators.required],
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      address: [''],
      phone: [''],
      idCarrier: [null], // ID del transportista si ya existe
    });

    // Formulario de traslado
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Normalizar a medianoche para evitar problemas de zona horaria
    this.transferForm = this.fb.group({
      originAddress: ['', Validators.required],
      startDate: [today, Validators.required], // Fecha actual por defecto como objeto Date
      endDate: ['', Validators.required],
      transportPlate: ['', Validators.required],
      transportType: [GUIDE_TRANSPORT_TYPE[0].code], // Por defecto: Terrestre
    });

    // Formulario de destinatario
    this.recipientForm = this.fb.group({
      identification: ['', Validators.required],
      identificationType: [null, Validators.required],
      name: ['', Validators.required],
      destinationAddress: ['', Validators.required],
      route: [''],
      reasonForTransfer: [
        GUIDE_REASON_FOR_TRANSFER[0].code,
        Validators.required,
      ], // Por defecto: Venta
      customsDocument: [''],
      idClient: [null], // ID del cliente si ya existe
      email: [''], // Email del cliente/destinatario
    });

    // Formulario de comprobante sustento
    this.documentForm = this.fb.group({
      emissionType: [GUIDE_EMISSION_TYPE[1].code], // Por defecto: Electrónica
      documentType: [GUIDE_DOCUMENT_TYPE[0].code], // Por defecto: Factura (01)
      searchBy: [GUIDE_SEARCH_BY_TYPE[1].code], // Por defecto: Número de documento
      searchValue: [''],
      accessKey: [''],
      authorizationNumber: [''],
      issueDate: [''],
      documentNumber: [''],
    });

    // Formulario de detalles
    this.detailsForm = this.fb.group({
      items: this.fb.array([]),
    });

    // Formulario principal
    this.guideForm = this.fb.group({
      emission: this.emissionForm,
      carrier: this.carrierForm,
      transfer: this.transferForm,
      recipient: this.recipientForm,
      document: this.documentForm,
      details: this.detailsForm,
    });
  }

  private loadInitialData(): void {
    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    const establishmentId = localStorage.getItem(environment.ID_ESTABLISHMENT)
      ? Number(localStorage.getItem(environment.ID_ESTABLISHMENT))
      : null;
    const invoicePointId = localStorage.getItem(environment.ID_INVOICE_POINT)
      ? Number(localStorage.getItem(environment.ID_INVOICE_POINT))
      : null;

    if (!companyId) {
      this.logger.error('No se encontró ID de compañía');
      this.showError('Error: No se encontró información de la compañía');
      return;
    }

    this.loadCompanyData(companyId, establishmentId, invoicePointId);
    this.loadCarriers(companyId);
  }

  private loadCompanyData(
    companyId: number,
    establishmentId: number | null,
    invoicePointId: number | null
  ): void {
    this.companyService.findByIds([companyId]).subscribe({
      next: (response) => {
        const companies = response.companyDetailsDtos || [];

        if (companies.length === 0) {
          this.logger.error('No se encontró la empresa');
          return;
        }

        const company = companies[0];
        this.companyData = company;

        if (company?.establishments && company.establishments.length > 0) {
          this.branchOffices = company.establishments.map((est: any) => ({
            id: est.idEstablishment,
            code: est.code,
            address: est.address,
          }));

          const selectedEstablishmentId =
            establishmentId || this.branchOffices[0]?.id || null;

          if (!establishmentId && selectedEstablishmentId) {
            localStorage.setItem(
              environment.ID_ESTABLISHMENT,
              selectedEstablishmentId.toString()
            );
          }

          this.emissionForm.patchValue({
            establishment: selectedEstablishmentId,
            commercialName: company.nameCommercial || company.name,
            issueDate: this.getTodayDateString(),
          });

          // Cargar punto de emisión desde localStorage
          const invoicePointDataStr =
            localStorage.getItem('INVOICE_POINT_DATA');

          if (invoicePointDataStr) {
            try {
              const invoicePointData = JSON.parse(invoicePointDataStr);
              this.emissionPoints = [
                {
                  id: invoicePointData.id,
                  code: invoicePointData.code,
                },
              ];

              this.emissionForm.patchValue({
                emissionPoint: invoicePointData.id,
              });

              this.emissionForm.get('emissionPoint')?.disable();
            } catch (error) {
              this.logger.error('Error al parsear INVOICE_POINT_DATA', error);
            }
          }

          this.cdRef.detectChanges();
        }
      },
      error: (error) => {
        this.logger.error('Error al cargar datos de empresa', error);
        this.showError('Error al cargar datos de empresa');
      },
    });
  }

  private loadCarriers(companyId: number): void {
    this.carrierService.getCarriers(companyId, true, 0, 100).subscribe({
      next: (page) => {
        this.carriers = page.content;
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.logger.error('Error al cargar transportistas', error);
      },
    });
  }

  private checkForDraft(): void {
    const draftId = this.route.snapshot.params['id'];
    if (draftId) {
      this.loadDraft(Number(draftId));
    }
  }

  private loadDraft(id: number): void {
    this.loading = true;
    this.guideService.getGuideById(id).subscribe({
      next: (guide) => {
        this.populateForm(guide);
        this.currentDraftId = id;
        this.loading = false;
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.logger.error('Error al cargar borrador', error);
        this.showError('Error al cargar el borrador');
        this.loading = false;
        this.cdRef.detectChanges();
      },
    });
  }

  private populateForm(guide: GuideDto): void {
    // Poblar formulario de emisión
    if (guide.establishment) {
      this.emissionForm.patchValue({
        establishment: guide.establishment.idEstablishment,
        emissionPoint: guide.establishment.issuancePoint.idIssuancePoint,
        commercialName:
          guide.companyDto.nameCommercial || guide.companyDto.name,
        issueDate: guide.issueDate
          ? this.parseDateForPicker(guide.issueDate)
          : new Date(),
      });
    }

    // Poblar formulario de transportista
    if (guide.carrierDto) {
      this.carrierForm.patchValue({
        identification: guide.carrierDto.identification,
        identificationType: guide.carrierDto.identificationType,
        name: guide.carrierDto.name,
        email: guide.carrierDto.email || '',
        address: guide.carrierDto.address || '',
        phone: guide.carrierDto.phone || '',
        idCarrier: guide.carrierDto.idCarrier || null,
      });

      // Si hay datos del transportista, deshabilitar campos (son readonly)
      if (guide.carrierDto.idCarrier) {
        this.carrierForm.get('identificationType')?.disable();
        this.carrierForm.get('name')?.disable();
        this.carrierForm.get('email')?.disable();
      }
    }

    // Poblar formulario de traslado
    this.transferForm.patchValue({
      originAddress: guide.originAddress,
      startDate: guide.startDate
        ? this.parseDateForPicker(guide.startDate)
        : null,
      endDate: guide.endDate ? this.parseDateForPicker(guide.endDate) : null,
      transportPlate: guide.transportPlate || '',
      transportType: guide.transportType || GUIDE_TRANSPORT_TYPE[0].code,
    });

    // Poblar formulario de destinatario
    if (guide.clientDto) {
      this.recipientForm.patchValue({
        identification: guide.clientDto.identification,
        identificationType: guide.clientDto.idType,
        name: guide.clientDto.name,
        destinationAddress: guide.destinationAddress,
        route: guide.route || '',
        reasonForTransfer:
          guide.reasonForTransfer || GUIDE_REASON_FOR_TRANSFER[0].code,
        customsDocument: '',
        idClient: guide.clientDto.idClient ?? null,
      });

      // Si hay datos del cliente, deshabilitar campos (son readonly)
      if (guide.clientDto.idClient) {
        this.recipientForm.get('identificationType')?.disable();
        this.recipientForm.get('name')?.disable();
        this.recipientForm.get('destinationAddress')?.disable();
      }
    }

    // Poblar formulario de comprobante
    if (guide.documentReferences && guide.documentReferences.length > 0) {
      const doc = guide.documentReferences[0];
      this.documentForm.patchValue({
        documentType: doc.documentType,
        accessKey: doc.accessKey || '',
        authorizationNumber: '', // No disponible en documentReferences, se obtiene al validar
        issueDate: doc.issueDate ? this.formatDateOnly(doc.issueDate) : '',
        documentNumber: doc.documentNumber,
      });

      // Si hay datos del documento, deshabilitar campos (son readonly)
      if (doc.accessKey || doc.issueDate || doc.documentNumber) {
        this.documentForm.get('accessKey')?.disable();
        this.documentForm.get('authorizationNumber')?.disable();
        this.documentForm.get('issueDate')?.disable();
        this.documentForm.get('documentNumber')?.disable();
      }

      // Si hay accessKey o documentNumber, cargar los productos del documento validado
      // para que estén disponibles cuando se haga clic en "Buscar en listado de productos"
      if (doc.accessKey || doc.documentNumber) {
        this.loadValidatedDocumentProducts(
          doc.documentType,
          doc.accessKey || '',
          doc.documentNumber || ''
        );
      }
    }

    // Poblar detalles de productos
    if (guide.guideDetails && guide.guideDetails.length > 0) {
      guide.guideDetails.forEach((detail) => {
        const newItem = this.fb.group({
          idProduct: [detail.idProduct, Validators.required],
          quantity: [detail.quantity, [Validators.required, Validators.min(1)]],
          description: [detail.description || ''],
          productDto: [detail.productDto],
        });
        this.guideDetails.push(newItem);
      });
      this.updateDetailsTable();
    }

    // Poblar campos adicionales
    if (guide.additionalInformation) {
      this.parseAdditionalFields(guide.additionalInformation);
    } else {
      this.additionalFields = [];
      this.additionalFieldsDataSource.data = [];
    }
  }

  // ========== MÉTODOS DE TRANSPORTISTA ==========

  searchCarrier(): void {
    const identification = this.carrierForm.get('identification')?.value;
    if (!identification) {
      this.showError('Ingrese una identificación');
      return;
    }

    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!companyId) {
      this.showError('Error: No se encontró información de la compañía');
      return;
    }

    // Buscar transportista por identificación
    this.carrierService.getCarriers(companyId, false, 0, 100).subscribe({
      next: (page) => {
        const found = page.content.find(
          (c) => c.identification === identification
        );
        if (found) {
          this.carrierForm.patchValue({
            identification: found.identification,
            identificationType: found.identificationType,
            name: found.name,
            email: found.email || '',
            address: found.address || '',
            phone: found.phone || '',
            idCarrier: found.idCarrier || null,
          });

          // Deshabilitar campos después de cargar transportista (excepto identificación para poder buscar otro)
          this.carrierForm.get('identificationType')?.disable();
          this.carrierForm.get('name')?.disable();
          this.carrierForm.get('email')?.disable();

          this.showSuccess('Transportista encontrado y cargado');
          this.cdRef.detectChanges();

          // Enfocar el campo de Dirección de partida
          this.focusOriginAddress();
        } else {
          // Transportista no encontrado, habilitar campos para crear nuevo
          this.carrierForm.patchValue({
            identificationType: this.detectCarrierIdType(identification),
            name: '',
            email: '',
            address: '',
            phone: '',
            idCarrier: null,
          });

          // Habilitar campos para permitir crear nuevo transportista
          this.carrierForm.get('identificationType')?.enable();
          this.carrierForm.get('name')?.enable();
          this.carrierForm.get('email')?.enable();

          // Mostrar toast informativo como en facturación
          this.toast.info(
            'Transportista no encontrado. Puede crear uno nuevo desde el botón de agregar.'
          );

          this.logger.debug(
            'Transportista no encontrado, limpiando formulario y mostrando opción para crear'
          );
          this.cdRef.detectChanges();
        }
      },
      error: (error) => {
        this.logger.error('Error al buscar transportista', error);
        // Permitir crear nuevo transportista
        this.carrierForm.patchValue({
          identificationType: this.detectCarrierIdType(identification),
          name: '',
          email: '',
          address: '',
          phone: '',
          idCarrier: null,
        });

        // Habilitar campos para permitir crear nuevo transportista
        this.carrierForm.get('identificationType')?.enable();
        this.carrierForm.get('name')?.enable();
        this.carrierForm.get('email')?.enable();

        // Mostrar toast de error
        this.toast.error('Error al buscar transportista. Intente nuevamente.');
        this.cdRef.detectChanges();
      },
    });
  }

  openCreateCarrierDialog(): void {
    // Obtener valores actuales del formulario
    const currentIdentification =
      this.carrierForm.get('identification')?.value ?? '';
    const currentIdType = this.carrierForm.get('identificationType')?.value;

    const dialogRef = this.dialog.open(UpdateCarrierComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: true,
      data: {
        mode: 'create',
        initialIdentification: currentIdentification,
        initialIdType: currentIdType,
      },
      panelClass: 'custom-dialog-container',
      autoFocus: false,
    });

    dialogRef.afterClosed().subscribe((result: CarrierDto | null) => {
      if (result && result.idCarrier) {
        // Transportista creado exitosamente, cargar datos en el formulario
        this.carrierForm.patchValue({
          identification: result.identification,
          identificationType: result.identificationType,
          name: result.name,
          email: result.email || '',
          address: result.address || '',
          phone: result.phone || '',
          idCarrier: result.idCarrier || null,
        });

        // Deshabilitar campos después de crear transportista (excepto identificación para poder buscar otro)
        this.carrierForm.get('identificationType')?.disable();
        this.carrierForm.get('name')?.disable();
        this.carrierForm.get('email')?.disable();

        this.cdRef.detectChanges();
        this.showSuccess('Transportista creado exitosamente');
      }
    });
  }

  onCarrierIdentificationEnter(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.searchCarrier();
    }
  }

  private detectCarrierIdType(identification: string): string {
    if (identification.length === 13) return '2'; // RUC
    if (identification.length === 10) return '1'; // Cédula
    return '3'; // Pasaporte
  }

  private focusOriginAddress(): void {
    // Usar setTimeout para asegurar que el DOM esté actualizado después del cambio de detección
    setTimeout(() => {
      try {
        if (this.originAddressInput?.nativeElement) {
          // Buscar el textarea dentro del mat-form-field
          const textarea = this.originAddressInput.nativeElement.querySelector('textarea');
          if (textarea && typeof textarea.focus === 'function') {
            textarea.focus();
            this.logger.debug('✅ Campo de Dirección de partida enfocado');
          } else {
            this.logger.warn(
              'El textarea no está disponible o no tiene método focus'
            );
          }
        } else {
          this.logger.warn('originAddressInput no está disponible');
        }
      } catch (error) {
        this.logger.warn(
          'No se pudo enfocar el campo de Dirección de partida',
          error
        );
      }
    }, 200);
  }

  private focusRoute(): void {
    // Usar setTimeout para asegurar que el DOM esté actualizado después del cambio de detección
    setTimeout(() => {
      try {
        if (this.routeInput?.nativeElement) {
          // Buscar el input dentro del mat-form-field
          const input = this.routeInput.nativeElement.querySelector('input');
          if (input && typeof input.focus === 'function') {
            input.focus();
            this.logger.debug('✅ Campo de Ruta enfocado');
          } else {
            this.logger.warn(
              'El input no está disponible o no tiene método focus'
            );
          }
        } else {
          this.logger.warn('routeInput no está disponible');
        }
      } catch (error) {
        this.logger.warn(
          'No se pudo enfocar el campo de Ruta',
          error
        );
      }
    }, 200);
  }

  getSearchValuePlaceholder(): string {
    const searchBy = this.documentForm.get('searchBy')?.value;
    if (!searchBy) {
      return 'Seleccione cómo buscar';
    }
    
    const searchType = this.searchByTypes.find(t => t.code === searchBy);
    if (searchType) {
      return `Ingrese ${searchType.value.toLowerCase()}`;
    }
    
    return 'Ingrese el valor a buscar';
  }

  // ========== MÉTODOS DE DESTINATARIO ==========

  searchRecipient(): void {
    const identification = this.recipientForm.get('identification')?.value;
    if (!identification) {
      this.showError('Ingrese una identificación');
      return;
    }

    // Buscar cliente por identificación usando search
    const companyIdentification = localStorage.getItem(
      environment.IDENTIFICATION
    );
    if (!companyIdentification) {
      this.showError('No se encontró identificación de la compañía');
      return;
    }

    const clientRequest: ClientRequestDto = {
      identification: identification,
      name: '',
      clientType: 0,
      status: true,
      companyIdentification: companyIdentification,
    };

    this.clientService
      .searchPageable(0, 1, clientRequest)
      .pipe(
        catchError(() => {
          // Cliente no encontrado, permitir crear nuevo
          this.recipientForm.patchValue({
            identificationType: this.detectIdType(identification),
          });
          this.cdRef.detectChanges();
          return of({ content: [] });
        })
      )
      .subscribe((page: any) => {
        if (page.content && page.content.length > 0) {
          const clientList = page.content as Client[];
          // Agregar información de contactos (dirección, email, teléfono)
          this.clientHelper.addContactInformation(clientList);
          const client = clientList[0];

          this.recipientForm.patchValue({
            identification: client.identification,
            identificationType: client.idType,
            name: client.name,
            destinationAddress: client.address || '',
            idClient: client.idClient || null,
            email: client.mail || '',
          });

          // Deshabilitar campos después de cargar cliente (excepto identificación para poder buscar otro)
          this.recipientForm.get('identificationType')?.disable();
          this.recipientForm.get('name')?.disable();
          this.recipientForm.get('destinationAddress')?.disable();

          this.showSuccess('Cliente encontrado y cargado');
          this.cdRef.detectChanges();

          // Enfocar el campo de Ruta
          this.focusRoute();
        } else {
          // Cliente no encontrado, limpiar formulario y mostrar opción para crear
          this.recipientForm.patchValue({
            identificationType: this.detectIdType(identification),
            name: '',
            destinationAddress: '',
            idClient: null,
            email: '',
          });

          // Mostrar toast informativo como en facturación
          this.toast.info(
            'Cliente no encontrado. Puede crear uno nuevo desde el botón de agregar.'
          );

          this.logger.debug(
            'Cliente no encontrado, limpiando formulario y mostrando opción para crear'
          );
          this.cdRef.detectChanges();
        }
      });
  }

  onRecipientIdentificationEnter(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.searchRecipient();
    }
  }

  private detectIdType(identification: string): number {
    if (identification.length === 10) return 1; // Cédula
    if (identification.length === 13) return 2; // RUC
    return 3; // Pasaporte
  }

  openCreateClientDialog(): void {
    // Obtener valores actuales del formulario
    const currentIdentification =
      this.recipientForm.get('identification')?.value ?? '';
    const currentIdType = this.recipientForm.get('identificationType')?.value;

    // Verificar si existe Consumidor Final y obtener email de compañía
    this.checkConsumidorFinalExists().then(
      ({ hasConsumidorFinal, companyEmail }) => {
        const dialogRef = this.dialog.open(UpdateClientComponent, {
          width: '700px',
          maxWidth: '90vw',
          maxHeight: '90vh',
          disableClose: true,
          data: {
            mode: 'create',
            hasConsumidorFinal,
            companyEmail,
            initialIdentification: currentIdentification,
            initialIdType: currentIdType,
          },
          panelClass: 'custom-dialog-container',
          autoFocus: false,
        });

        dialogRef.afterClosed().subscribe((result: Client | null) => {
          if (result && result.idClient) {
            // Cliente creado exitosamente, cargar datos en el formulario
            this.loadRecipientIntoForm(result);
          }
        });
      }
    );
  }

  private checkConsumidorFinalExists(): Promise<{
    hasConsumidorFinal: boolean;
    companyEmail?: string;
  }> {
    return new Promise((resolve) => {
      const companyIdentification =
        localStorage.getItem(environment.IDENTIFICATION) ?? '';

      const consumidorFinalRequest: ClientRequestDto = {
        identification: '9999999999999',
        name: '',
        clientType: 0,
        status: true,
        companyIdentification,
      };

      this.clientService
        .searchPageable(0, 1, consumidorFinalRequest)
        .pipe(
          catchError(() => {
            this.logger.warn('Error verificando consumidor final');
            return of({ content: [], totalElements: 0 });
          })
        )
        .subscribe((data: any) => {
          const clients = (data?.content || []) as Client[];
          // ID_TYPE_CONSUMIDOR_FINAL = 4 según constantes
          const hasConsumidorFinal =
            clients.some((c) => c.idType === 4 && c.status === true) ?? false;

          const companyEmail = this.companyData?.email || undefined;

          this.logger.debug('Verificación Consumidor Final:', {
            hasConsumidorFinal,
            companyEmail,
          });

          resolve({ hasConsumidorFinal, companyEmail });
        });
    });
  }

  private loadRecipientIntoForm(client: Client): void {
    // Agregar información de contactos (dirección, email, teléfono)
    this.clientHelper.addContactInformation([client]);

    // Actualizar formulario con datos del cliente
    this.recipientForm.patchValue({
      identification: client.identification,
      identificationType: client.idType,
      name: client.name,
      destinationAddress: client.address || '',
      idClient: client.idClient || null,
      email: client.mail || '',
    });

    // Deshabilitar campos después de cargar cliente (excepto identificación para poder buscar otro)
    this.recipientForm.get('identificationType')?.disable();
    this.recipientForm.get('name')?.disable();

    this.showSuccess('Cliente creado y cargado exitosamente');
    this.cdRef.detectChanges();
  }

  // ========== MÉTODOS DE PRODUCTOS ==========

  searchProduct(): void {
    const searchTerm = this.productCtrl.value;
    if (!searchTerm || searchTerm.trim().length === 0) {
      this.openProductDialog();
      return;
    }

    // Buscar producto usando el servicio
    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    this.productService
      .searchPageable(0, 20, {
        idCompany: companyId,
        name: searchTerm,
        status: true,
      })
      .pipe(
        catchError((error) => {
          this.logger.error('Error al buscar producto', error);
          return of({ content: [], totalElements: 0 });
        })
      )
      .subscribe({
        next: (page: any) => {
          if (page.content && page.content.length === 1) {
            this.addProductToDetails(page.content[0]);
          } else {
            this.openProductDialog();
          }
        },
      });
  }

  openProductDialog(): void {
    // Si hay productos del documento validado, usarlos; si no, abrir diálogo vacío
    if (this.validatedDocumentProducts.length > 0) {
      // Usar los productos del documento validado
      this.openProductDialogWithProducts(
        this.validatedDocumentProducts,
        this.validatedProductInfoMap
      );
    } else {
      // Abrir diálogo vacío para búsqueda normal
      const dialogRef = this.dialog.open(SelectProductDialogComponent, {
        width: '900px',
        maxWidth: '95vw',
        data: { products: [], searchTerm: this.productCtrl.value },
      });

      dialogRef
        .afterClosed()
        .subscribe((selectedProducts: ProductDto[] | ProductDto | null) => {
          if (selectedProducts) {
            // Manejar tanto array como producto único
            const products = Array.isArray(selectedProducts)
              ? selectedProducts
              : [selectedProducts];
            products.forEach((product) => {
              if (product) {
                this.addProductToDetails(product);
              }
            });
          }
        });
    }
  }

  /**
   * Abre el modal para crear un nuevo producto desde la pantalla de guía de remisión
   * El botón solo se muestra si el usuario tiene el permiso PRODUCT_MANAGEMENT
   */
  openCreateProductDialog(): void {
    const dialogRef = this.dialog.open(UpdateProductComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: true,
      data: {
        isEdit: false,
      } as ProductDialogData,
      panelClass: 'custom-dialog-container',
      autoFocus: false,
    });

    dialogRef
      .afterClosed()
      .subscribe((result: boolean | { mainCode: string } | null) => {
        if (result && typeof result === 'object' && result.mainCode) {
          // Producto creado exitosamente, buscarlo y agregarlo a la guía
          const companyId = Number(
            localStorage.getItem(environment.ID_COMPANY)
          );
          this.productService
            .searchPageable(0, 1, {
              idCompany: companyId,
              mainCode: result.mainCode,
              status: true,
            })
            .subscribe({
              next: (page: any) => {
                if (page.content && page.content.length > 0) {
                  // Agregar el producto directamente a la guía
                  this.addProductToDetails(page.content[0]);
                  this.productCtrl.setValue(''); // Limpiar el campo de búsqueda
                  this.toast.success('Producto creado y agregado exitosamente');
                  this.logger.debug(
                    'Producto creado y agregado a la guía:',
                    result.mainCode
                  );
                } else {
                  this.toast.success('Producto creado exitosamente');
                  this.logger.warn(
                    'Producto creado pero no se pudo encontrar para agregarlo'
                  );
                }
              },
              error: (err) => {
                this.logger.error('Error al buscar producto creado', err);
                this.toast.success('Producto creado exitosamente');
              },
            });
        } else if (result === true) {
          // Fallback por compatibilidad
          this.toast.success('Producto creado exitosamente');
        }
      });
  }

  addProductToDetails(product: ProductDto): void {
    const items = this.guideDetails;
    const existingIndex = items.controls.findIndex(
      (control) => control.get('idProduct')?.value === product.idProduct
    );

    if (existingIndex >= 0) {
      // Incrementar cantidad si ya existe
      const currentQuantity =
        items.at(existingIndex).get('quantity')?.value || 0;
      items.at(existingIndex).patchValue({ quantity: currentQuantity + 1 });
    } else {
      // Agregar nuevo producto
      const newItem = this.fb.group({
        idProduct: [product.idProduct, Validators.required],
        quantity: [1, [Validators.required, Validators.min(1)]],
        description: [product.description || product.name],
        productDto: [product],
      });

      items.push(newItem);
    }

    this.updateDetailsTable();
    this.productCtrl.setValue('');
    this.cdRef.detectChanges();
  }

  removeProductDetail(index: number): void {
    this.guideDetails.removeAt(index);
    this.updateDetailsTable();
    this.cdRef.detectChanges();
  }

  private updateDetailsTable(): void {
    const items = this.guideDetails.value;
    this.guideDetailDataSource.data = items.map((item: any) => ({
      ...item,
      code: item.productDto?.mainCode || '',
      description: item.description || item.productDto?.name || '',
    }));
  }

  // ========== MÉTODOS DE COMPROBANTE SUSTENTO ==========

  validateDocument(): void {
    const searchBy = this.documentForm.get('searchBy')?.value;
    const searchValue = this.documentForm.get('searchValue')?.value;
    const documentType = this.documentForm.get('documentType')?.value;

    if (!searchValue) {
      this.showError('Ingrese un valor para buscar');
      return;
    }

    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!companyId) {
      this.showError('Error: No se encontró información de la compañía');
      return;
    }

    this.loading = true;
    this.cdRef.detectChanges();

    // Determinar qué servicio y DTO usar según el tipo de documento
    let searchObservable: Observable<
      DocumentBasicInfoDto | DocumentBasicInfoDto[]
    >;

    if (documentType === '01') {
      // Factura
      const request: InvoiceSearchByNumberOrAccessKeyDto = {
        idCompany: companyId,
      };
      if (searchBy === 'DOCUMENT_NUMBER') {
        request.invoiceNumber = searchValue;
      } else if (searchBy === 'ACCESS_KEY') {
        request.accessKey = searchValue;
      }
      searchObservable = this.invoiceService.searchAuthorizedActive(request);
    } else if (documentType === '05') {
      // Nota de débito
      const request: DebitNoteSearchByNumberOrAccessKeyDto = {
        idCompany: companyId,
      };
      if (searchBy === 'DOCUMENT_NUMBER') {
        request.debitNoteNumber = searchValue;
      } else if (searchBy === 'ACCESS_KEY') {
        request.accessKey = searchValue;
      }
      searchObservable = this.debitNoteService.searchAuthorizedActive(request);
    } else if (documentType === '04') {
      // Nota de crédito
      const request: CreditNoteSearchByNumberOrAccessKeyDto = {
        idCompany: companyId,
      };
      if (searchBy === 'DOCUMENT_NUMBER') {
        request.creditNoteNumber = searchValue;
      } else if (searchBy === 'ACCESS_KEY') {
        request.accessKey = searchValue;
      }
      searchObservable = this.creditNoteService.searchAuthorizedActive(request);
    } else {
      this.loading = false;
      this.showError('Tipo de documento no válido');
      this.cdRef.detectChanges();
      return;
    }

    searchObservable
      .pipe(
        catchError((error) => {
          this.loading = false;
          this.logger.error('Error al validar documento', error);
          this.showError(
            error.error?.message ||
              'Error al validar documento. Verifique que el documento esté autorizado y activo.'
          );
          this.cdRef.detectChanges();
          return of(null);
        })
      )
      .subscribe({
        next: (
          response: DocumentBasicInfoDto | DocumentBasicInfoDto[] | null
        ) => {
          this.loading = false;

          if (!response) {
            return;
          }

          // El backend puede retornar un array o un objeto directo
          let documentInfo: DocumentBasicInfoDto;
          if (Array.isArray(response)) {
            if (response.length === 0) {
              this.showError(
                'No se encontró ningún documento con los criterios especificados'
              );
              this.cdRef.detectChanges();
              return;
            }
            documentInfo = response[0];
          } else {
            documentInfo = response;
          }

          // Llenar los campos del formulario con los datos encontrados
          this.documentForm.patchValue({
            accessKey: documentInfo.accessKey || '',
            authorizationNumber: documentInfo.authorizationNumber || '',
            issueDate: documentInfo.issueDate
              ? new Date(documentInfo.issueDate).toISOString().split('T')[0]
              : '',
            documentNumber: documentInfo.invoiceNumber || '',
          });

          // Deshabilitar campos después de cargar documento (son readonly)
          this.documentForm.get('accessKey')?.disable();
          this.documentForm.get('authorizationNumber')?.disable();
          this.documentForm.get('issueDate')?.disable();
          this.documentForm.get('documentNumber')?.disable();

          // Cargar productos en la tabla de detalles
          if (documentInfo.products && documentInfo.products.length > 0) {
            this.loadProductsFromDocument(documentInfo.products);
          } else {
            this.showSuccess('Documento encontrado y campos llenados.');
          }

          this.cdRef.detectChanges();
        },
      });
  }

  private loadProductsFromDocument(products: ProductInfoDto[]): void {
    // Usar forkJoin para cargar todos los productos en paralelo
    const productRequests = products
      .filter((productInfo) => productInfo.idProduct)
      .map((productInfo) => {
        return this.productService.findById(productInfo.idProduct!).pipe(
          catchError((error) => {
            this.logger.warn(
              `No se pudo cargar producto ${productInfo.idProduct}, usando datos básicos`,
              error
            );
            // Si no se puede cargar el producto completo, crear uno básico con los datos disponibles
            const basicProduct: ProductDto = {
              idProduct: productInfo.idProduct,
              mainCode: productInfo.mainCode || '',
              name: productInfo.name || '',
              description: productInfo.name || '',
              unitPrice: productInfo.unitPrice || 0,
            } as ProductDto;
            return of({ productDtos: [basicProduct] } as ProductResponse);
          }),
          // Mapear a un objeto con el producto y la información original
          map((response: ProductResponse) => ({
            product:
              response.productDtos && response.productDtos.length > 0
                ? response.productDtos[0]
                : null,
            productInfo,
          }))
        );
      });

    if (productRequests.length === 0) {
      this.showSuccess(
        'Documento encontrado, pero no se encontraron productos.'
      );
      this.cdRef.detectChanges();
      return;
    }

    forkJoin(productRequests).subscribe({
      next: (results) => {
        // Filtrar productos válidos
        const validProducts: {
          product: ProductDto;
          productInfo: ProductInfoDto;
        }[] = [];
        const productInfoMap = new Map<number, ProductInfoDto>();

        results.forEach(({ product, productInfo }) => {
          if (product && product.idProduct) {
            validProducts.push({ product, productInfo });
            productInfoMap.set(product.idProduct, productInfo);
          } else {
            this.logger.warn(
              `No se encontró producto con ID ${productInfo.idProduct}`
            );
          }
        });

        if (validProducts.length === 0) {
          this.showError('No se pudieron cargar los productos del documento');
          this.cdRef.detectChanges();
          return;
        }

        // Extraer solo los ProductDto para el popup
        const productsForDialog: ProductDto[] = validProducts.map(
          ({ product }) => product
        );

        // Guardar los productos para usarlos cuando se haga clic en "Buscar en listado de productos"
        this.validatedDocumentProducts = productsForDialog;
        this.validatedProductInfoMap = productInfoMap;

        // Abrir el popup de selección con los productos cargados
        this.openProductDialogWithProducts(productsForDialog, productInfoMap);
        this.showSuccess(
          `Documento encontrado. Se encontraron ${validProducts.length} producto(s) para seleccionar.`
        );
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.logger.error('Error al cargar productos del documento', error);
        this.showError('Error al cargar productos del documento');
        this.cdRef.detectChanges();
      },
    });
  }

  /**
   * Carga los productos del documento validado cuando se carga un borrador
   * (sin abrir el diálogo, solo para tenerlos disponibles cuando se haga clic en "Buscar en listado")
   */
  private loadValidatedDocumentProducts(
    documentType: string,
    accessKey: string,
    documentNumber: string
  ): void {
    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!companyId) {
      this.logger.warn(
        'No se pudo obtener idCompany para cargar productos del documento'
      );
      return;
    }

    let searchObservable: Observable<
      DocumentBasicInfoDto | DocumentBasicInfoDto[]
    >;

    if (documentType === '01') {
      // Factura
      const request: InvoiceSearchByNumberOrAccessKeyDto = {
        idCompany: companyId,
      };
      if (documentNumber) {
        request.invoiceNumber = documentNumber;
      } else if (accessKey) {
        request.accessKey = accessKey;
      } else {
        return; // No hay manera de buscar
      }
      searchObservable = this.invoiceService.searchAuthorizedActive(request);
    } else if (documentType === '05') {
      // Nota de débito
      const request: DebitNoteSearchByNumberOrAccessKeyDto = {
        idCompany: companyId,
      };
      if (documentNumber) {
        request.debitNoteNumber = documentNumber;
      } else if (accessKey) {
        request.accessKey = accessKey;
      } else {
        return;
      }
      searchObservable = this.debitNoteService.searchAuthorizedActive(request);
    } else if (documentType === '04') {
      // Nota de crédito
      const request: CreditNoteSearchByNumberOrAccessKeyDto = {
        idCompany: companyId,
      };
      if (documentNumber) {
        request.creditNoteNumber = documentNumber;
      } else if (accessKey) {
        request.accessKey = accessKey;
      } else {
        return;
      }
      searchObservable = this.creditNoteService.searchAuthorizedActive(request);
    } else {
      return; // Tipo de documento no válido
    }

    searchObservable
      .pipe(
        catchError((error) => {
          this.logger.warn(
            'Error al cargar productos del documento validado',
            error
          );
          return of(null);
        })
      )
      .subscribe({
        next: (
          response: DocumentBasicInfoDto | DocumentBasicInfoDto[] | null
        ) => {
          if (!response) {
            return;
          }

          let documentInfo: DocumentBasicInfoDto;
          if (Array.isArray(response)) {
            if (response.length === 0) {
              return;
            }
            documentInfo = response[0];
          } else {
            documentInfo = response;
          }

          // Actualizar authorizationNumber y issueDate en el formulario si están disponibles
          if (documentInfo.authorizationNumber || documentInfo.issueDate) {
            const updateData: any = {};
            if (documentInfo.authorizationNumber) {
              updateData.authorizationNumber = documentInfo.authorizationNumber;
            }
            if (documentInfo.issueDate) {
              updateData.issueDate = this.formatDateOnly(
                documentInfo.issueDate
              );
            }
            this.documentForm.patchValue(updateData);
            this.cdRef.detectChanges();
          }

          // Si hay productos, cargarlos
          if (documentInfo.products && documentInfo.products.length > 0) {
            this.loadProductsFromDocumentForEdit(documentInfo.products);
          }
        },
      });
  }

  /**
   * Carga productos del documento para edición (sin abrir diálogo)
   */
  private loadProductsFromDocumentForEdit(products: ProductInfoDto[]): void {
    const productRequests = products
      .filter((productInfo) => productInfo.idProduct)
      .map((productInfo) => {
        return this.productService.findById(productInfo.idProduct!).pipe(
          catchError((error) => {
            this.logger.warn(
              `No se pudo cargar producto ${productInfo.idProduct} para edición`,
              error
            );
            const basicProduct: ProductDto = {
              idProduct: productInfo.idProduct,
              mainCode: productInfo.mainCode || '',
              name: productInfo.name || '',
              description: productInfo.name || '',
              unitPrice: productInfo.unitPrice || 0,
            } as ProductDto;
            return of({ productDtos: [basicProduct] } as ProductResponse);
          }),
          map((response: ProductResponse) => ({
            product:
              response.productDtos && response.productDtos.length > 0
                ? response.productDtos[0]
                : null,
            productInfo,
          }))
        );
      });

    if (productRequests.length === 0) {
      return;
    }

    forkJoin(productRequests).subscribe({
      next: (results) => {
        const validProducts: {
          product: ProductDto;
          productInfo: ProductInfoDto;
        }[] = [];
        const productInfoMap = new Map<number, ProductInfoDto>();

        results.forEach(({ product, productInfo }) => {
          if (product && product.idProduct) {
            validProducts.push({ product, productInfo });
            productInfoMap.set(product.idProduct, productInfo);
          }
        });

        if (validProducts.length > 0) {
          const productsForDialog: ProductDto[] = validProducts.map(
            ({ product }) => product
          );

          // Guardar los productos para usarlos cuando se haga clic en "Buscar en listado de productos"
          this.validatedDocumentProducts = productsForDialog;
          this.validatedProductInfoMap = productInfoMap;

          this.logger.debug(
            `Productos del documento validado cargados: ${productsForDialog.length}`
          );
        }
      },
      error: (error) => {
        this.logger.error(
          'Error al cargar productos del documento para edición',
          error
        );
      },
    });
  }

  /**
   * Abre el diálogo de selección de productos con productos precargados
   */
  private openProductDialogWithProducts(
    products: ProductDto[],
    productInfoMap: Map<number, ProductInfoDto>
  ): void {
    const dialogRef = this.dialog.open(SelectProductDialogComponent, {
      width: '900px',
      maxWidth: '95vw',
      maxHeight: '95vh',
      data: { products, searchTerm: '' },
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((selectedProducts: ProductDto[]) => {
      if (selectedProducts && selectedProducts.length > 0) {
        // Agregar los productos seleccionados con sus cantidades del documento
        selectedProducts.forEach((product) => {
          const productInfo = productInfoMap.get(product.idProduct!);
          if (productInfo) {
            // Usar la cantidad del documento original
            this.addProductToDetailsWithQuantity(
              product,
              productInfo.quantity || 1,
              productInfo.name || product.description || product.name || ''
            );
          } else {
            // Si no hay información del documento, agregar con cantidad 1
            this.addProductToDetails(product);
          }
        });
        this.showSuccess(
          `${selectedProducts.length} producto(s) agregado(s) a la guía.`
        );
        this.cdRef.detectChanges();
      }
    });
  }

  /**
   * Agrega un producto a los detalles con una cantidad específica
   */
  private addProductToDetailsWithQuantity(
    product: ProductDto,
    quantity: number,
    description?: string
  ): void {
    const items = this.guideDetails;
    const existingIndex = items.controls.findIndex(
      (control) => control.get('idProduct')?.value === product.idProduct
    );

    if (existingIndex >= 0) {
      // Incrementar cantidad si ya existe
      const currentQuantity =
        items.at(existingIndex).get('quantity')?.value || 0;
      items.at(existingIndex).patchValue({
        quantity: currentQuantity + quantity,
      });
    } else {
      // Agregar nuevo producto
      const newItem = this.fb.group({
        idProduct: [product.idProduct, Validators.required],
        quantity: [quantity, [Validators.required, Validators.min(1)]],
        description: [
          description || product.description || product.name || '',
          Validators.required,
        ],
        productDto: [product],
      });

      this.guideDetails.push(newItem);
    }

    this.updateDetailsTable();
  }

  /**
   * Obtiene los contactos de la compañía desde localStorage
   */
  private getCompanyContacts(): any[] {
    try {
      const contactsJson = localStorage.getItem('COMPANY_CONTACTS');
      if (contactsJson) {
        const contacts = JSON.parse(contactsJson);
        return Array.isArray(contacts) ? contacts : [];
      }
    } catch (error) {
      this.logger.warn('Error al obtener contactos de la compañía', error);
    }
    return [];
  }

  /**
   * Formatea una fecha a formato yyyy-MM-dd (solo fecha, sin hora)
   * Similar a formatDate pero siempre retorna solo la fecha sin hora
   */
  private formatDateOnly(date: string | Date | null | undefined): string {
    if (!date) {
      return '';
    }

    // Si ya es un string en formato yyyy-MM-dd, retornarlo
    if (typeof date === 'string') {
      // Si es un string ISO completo (con hora), extraer solo la fecha
      if (date.includes('T')) {
        return date.split('T')[0];
      }
      // Si ya es yyyy-MM-dd, retornarlo
      if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
        return date;
      }
    }

    // Si es un objeto Date, formatearlo a yyyy-MM-dd
    if (date instanceof Date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }

    return '';
  }

  /**
   * Parsea una fecha string a objeto Date para el datepicker
   * Acepta string ISO o string yyyy-MM-dd
   */
  private parseDateForPicker(
    date: string | Date | null | undefined
  ): Date | null {
    if (!date) {
      return null;
    }

    // Si ya es un objeto Date, retornarlo
    if (date instanceof Date) {
      return date;
    }

    // Si es un string, parsearlo
    if (typeof date === 'string') {
      // Si es un string ISO completo, extraer solo la fecha
      const dateStr = date.includes('T') ? date.split('T')[0] : date;

      // Parsear en formato yyyy-MM-dd
      if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
        const parts = dateStr.split('-');
        const year = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1; // Los meses en Date son 0-indexed
        const day = parseInt(parts[2], 10);
        return new Date(year, month, day);
      }
    }

    return null;
  }

  /**
   * Formatea una fecha a formato yyyy-MM-dd
   * Acepta Date, string ISO, o string yyyy-MM-dd
   */
  /**
   * Obtiene la fecha de hoy en formato YYYY-MM-DD usando fecha local (sin conversión de zona horaria)
   */
  private getTodayDateString(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDate(date: string | Date | null | undefined): string {
    if (!date) {
      return '';
    }

    // Si ya es un string en formato yyyy-MM-dd, retornarlo
    if (typeof date === 'string') {
      // Si es un string ISO completo, extraer solo la fecha
      if (date.includes('T')) {
        return date.split('T')[0];
      }
      // Si ya es yyyy-MM-dd, retornarlo
      if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
        return date;
      }
    }

    // Si es un objeto Date, formatearlo usando fecha local (sin conversión de zona horaria)
    if (date instanceof Date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }

    return '';
  }

  // ========== MÉTODOS DE GUARDADO Y ENVÍO ==========

  signAndSend(): void {
    const validationResult = this.validateGuide();
    if (!validationResult.isValid) {
      this.showError(validationResult.message);
      return;
    }

    this.isProcessingGuide = true;
    this.errorMessage = '';

    const guideDto = this.buildGuideDto();

    if (this.currentDraftId) {
      // Actualizar borrador y enviar
      this.updateAndSendDraft(guideDto);
    } else {
      // Crear nueva guía y enviar
      this.createAndSendGuide(guideDto);
    }
  }

  saveDraft(): void {
    this.isSavingDraft = true;
    this.errorMessage = '';

    const guideDto = this.buildGuideDto();

    if (this.currentDraftId) {
      // Actualizar borrador existente
      this.updateDraft(guideDto);
    } else {
      // Crear nuevo borrador
      this.createDraft(guideDto);
    }
  }

  private buildGuideDto(): GuideDto {
    // Usar getRawValue() para obtener valores incluso si los campos están deshabilitados
    const emission = this.emissionForm.getRawValue();
    const carrier = this.carrierForm.getRawValue();
    const transfer = this.transferForm.getRawValue();
    const recipient = this.recipientForm.getRawValue();
    const document = this.documentForm.getRawValue();
    const details = this.guideDetails.value;

    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));

    // Si estamos editando un borrador, incluir el idGuide para que el backend actualice en lugar de crear uno nuevo
    const guideDto: GuideDto = {
      // Incluir idGuide si hay un borrador siendo editado
      ...(this.currentDraftId ? { idGuide: this.currentDraftId } : {}),
      companyDto: {
        idCompany: companyId,
        identification: this.companyData?.identification || '',
        name: this.companyData?.name || '',
        nameCommercial:
          emission.commercialName || this.companyData?.nameCommercial || '',
        idType: this.companyData?.idType || null,
        forcedToAccounting: this.companyData?.forcedToAccounting || false,
        regime: this.companyData?.regime || null,
        specialTaxpayerResolution:
          this.companyData?.specialTaxpayerResolution || null,
        status:
          this.companyData?.status !== undefined
            ? this.companyData.status
            : true,
        logoPath: this.companyData?.logoPath || null,
        contacts: this.getCompanyContacts(),
      },
      clientDto: {
        idClient: recipient.idClient ?? null,
        name: recipient.name,
        identification: recipient.identification,
        idType: recipient.identificationType,
        contacts: [
          {
            contactType: 'MAI',
            value: recipient.email || '',
            status: true,
          },
        ],
      },
      carrierDto: {
        idCarrier: carrier.idCarrier || null,
        idCompany: companyId,
        name: carrier.name,
        identification: carrier.identification,
        identificationType: carrier.identificationType,
        email: carrier.email,
        address: carrier.address,
        phone: carrier.phone,
      },
      establishment: (() => {
        const selectedBranch = this.branchOffices.find(
          (b) => b.id === emission.establishment
        );
        const code = selectedBranch?.code || '001';
        const address = selectedBranch?.address || 'DIRECCION MATRIZ';

        // Determinar el tipo: MATRIZ si el código es '001', SUCURSAL en caso contrario
        const type = code === '001' ? 'MATRIZ' : 'SUCURSAL';

        return {
          idEstablishment: emission.establishment,
          name: this.companyData?.name || 'MATRIZ',
          code: code,
          type: type, // MATRIZ o SUCURSAL según el código
          address: address,
          issuancePoint: (() => {
            const emissionPointId = emission.emissionPoint;

            if (!emissionPointId) {
              this.logger.warn(
                'No se encontró ID de punto de emisión en el formulario'
              );
              return {
                idIssuancePoint: 0,
                code: '',
              };
            }

            // Buscar el punto de emisión en el array
            const emissionPoint = this.emissionPoints.find(
              (p) => p.id === emissionPointId
            );

            if (!emissionPoint) {
              this.logger.warn(
                `No se encontró punto de emisión con ID: ${emissionPointId} en la lista de ${this.emissionPoints.length} puntos disponibles`
              );
              this.logger.debug(
                'Puntos de emisión disponibles:',
                this.emissionPoints
              );
              // Retornar un objeto válido con el ID pero sin código
              return {
                idIssuancePoint: emissionPointId,
                code: '',
              };
            }

            const result = {
              idIssuancePoint: emissionPoint.id,
              code: emissionPoint.code || '',
            };

            this.logger.debug('Punto de emisión seleccionado:', result);
            return result;
          })(),
        };
      })(),
      issueDate: emission.issueDate ? this.formatDate(emission.issueDate) : this.formatDate(new Date()),
      startDate: this.formatDate(transfer.startDate),
      endDate: this.formatDate(transfer.endDate),
      originAddress: transfer.originAddress,
      destinationAddress: recipient.destinationAddress,
      transportPlate: transfer.transportPlate,
      transportType: transfer.transportType || GUIDE_TRANSPORT_TYPE[0].code,
      reasonForTransfer: recipient.reasonForTransfer,
      route: recipient.route,
      guideDetails: details.map((item: any) => ({
        idProduct: item.idProduct,
        quantity: item.quantity,
        description: item.description,
      })),
      documentReferences: document.accessKey
        ? [
            {
              documentType: document.documentType,
              documentNumber: document.documentNumber,
              accessKey: document.accessKey,
              issueDate: this.formatDate(document.issueDate),
            },
          ]
        : [],
      additionalInformation: this.formatAdditionalFields(),
    };

    // Log para debugging
    if (this.currentDraftId) {
      this.logger.debug('📝 Construyendo GuideDto para actualizar borrador', {
        idGuide: this.currentDraftId,
        guideNumber: guideDto.guideNumber,
      });
    }

    return guideDto;
  }

  private createAndSendGuide(guideDto: GuideDto): void {
    this.guideService.createGuide(guideDto).subscribe({
      next: (response: GuideResponse) => {
        this.isProcessingGuide = false;
        if (response.code === 200) {
          // ✅ VERIFICAR: Respuesta exitosa y tiene guideNumber
          const guideId = response.idGuide || response.guideDto?.idGuide;
          const guideNumber =
            response.guideNumber || response.guideDto?.guideNumber;

          if (guideId && guideNumber) {
            // ✅ COMBINAR: GuideDto enviado + Datos de respuesta del backend
            const responseData = response.guideDto || response;
            const completeGuideData = this.mergeGuideData(
              guideDto,
              responseData
            );

            // ✅ GUARDAR en sessionStorage con clave única
            const storageKey = `GUIDE_TICKET_${guideId}`;
            sessionStorage.setItem(
              storageKey,
              JSON.stringify(completeGuideData)
            );

            this.logger.debug(
              '✅ Datos combinados guardados en sessionStorage',
              {
                storageKey,
                guideId,
              }
            );

            // ✅ MOSTRAR DIALOG DE ÉXITO con opción de imprimir
            const recipient = this.recipientForm.getRawValue();
            const transfer = this.transferForm.getRawValue();

            // Obtener authorizationStatusSri de la respuesta
            const authorizationStatusSri = response.authorizationStatusSri;

            const dialogData: SuccessGuideDialogData = {
              guideId: guideId,
              guideNumber: guideNumber,
              recipientName: recipient.name,
              startDate: transfer.startDate
                ? this.formatDate(transfer.startDate)
                : undefined,
              endDate: transfer.endDate
                ? this.formatDate(transfer.endDate)
                : undefined,
              authorizationStatusSri: authorizationStatusSri,
            };

            const dialogRef = this.dialog.open(SuccessGuideDialogComponent, {
              width: '450px',
              disableClose: false, // Permitir cerrar con ESC o clic fuera
              data: dialogData,
            });

            // Limpiar el formulario cuando se cierre el diálogo
            dialogRef.afterClosed().subscribe(() => {
              this.logger.debug(
                '🔄 Diálogo de éxito cerrado, navegando a lista de guías'
              );
              this.router.navigate(['/delivery-guides']);
            });

            this.logger.debug('✅ Diálogo de éxito mostrado', {
              guideId,
              guideNumber,
            });
          } else {
            // Respuesta exitosa pero sin guideNumber (caso raro)
            this.showSuccess('Guía guardada correctamente');
            this.router.navigate(['/delivery-guides']);
          }
        } else {
          this.errorMessage = response.message || 'Error al crear guía';
        }
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.isProcessingGuide = false;
        this.errorMessage = error.error?.message || 'Error al crear guía';
        this.cdRef.detectChanges();
      },
    });
  }

  private createDraft(guideDto: GuideDto): void {
    this.guideService.saveDraft(guideDto).subscribe({
      next: (response: GuideResponse) => {
        this.isSavingDraft = false;
        if (response.code === 200) {
          this.currentDraftId = response.guideDto?.idGuide || null;
          // Mostrar mensaje de éxito y redirigir al listado
          this.showSuccess('Borrador guardado exitosamente');
          this.logger.debug('✅ Borrador guardado exitosamente', {
            draftId: this.currentDraftId,
            guideNumber: response.guideDto?.guideNumber,
          });
          // Redirigir al listado de guías después de un breve delay para que se vea el mensaje
          setTimeout(() => {
            this.router.navigate(['/delivery-guides']);
          }, 1000);
        } else {
          this.errorMessage = response.message || 'Error al guardar borrador';
        }
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.isSavingDraft = false;
        this.errorMessage = error.error?.message || 'Error al guardar borrador';
        this.cdRef.detectChanges();
      },
    });
  }

  private updateDraft(guideDto: GuideDto): void {
    if (!this.currentDraftId) return;

    this.guideService.updateGuide(this.currentDraftId, guideDto).subscribe({
      next: (response: GuideResponse) => {
        this.isSavingDraft = false;
        if (response.code === 200) {
          // Mostrar mensaje de éxito y redirigir al listado
          this.showSuccess('Borrador actualizado exitosamente');
          this.logger.debug('✅ Borrador actualizado exitosamente', {
            draftId: this.currentDraftId,
          });
          // Redirigir al listado de guías después de un breve delay para que se vea el mensaje
          setTimeout(() => {
            this.router.navigate(['/delivery-guides']);
          }, 1000);
        } else {
          this.errorMessage =
            response.message || 'Error al actualizar borrador';
        }
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.isSavingDraft = false;
        this.errorMessage =
          error.error?.message || 'Error al actualizar borrador';
        this.cdRef.detectChanges();
      },
    });
  }

  private updateAndSendDraft(guideDto: GuideDto): void {
    if (!this.currentDraftId) {
      this.showError('ID de guía inválido');
      return;
    }

    // Comportamiento igual que createAndSendGuide: llamar directamente a createGuide
    // El backend manejará la actualización y envío automáticamente
    this.guideService.createGuide(guideDto).subscribe({
      next: (response: GuideResponse) => {
        this.isProcessingGuide = false;
        if (response.code === 200) {
          // ✅ VERIFICAR: Respuesta exitosa y tiene guideNumber
          const guideId = response.idGuide || response.guideDto?.idGuide;
          const guideNumber =
            response.guideNumber || response.guideDto?.guideNumber;

          if (guideId && guideNumber) {
            // ✅ COMBINAR: GuideDto enviado + Datos de respuesta del backend
            const responseData = response.guideDto || response;
            const completeGuideData = this.mergeGuideData(
              guideDto,
              responseData
            );

            // ✅ GUARDAR en sessionStorage con clave única
            const storageKey = `GUIDE_TICKET_${guideId}`;
            sessionStorage.setItem(
              storageKey,
              JSON.stringify(completeGuideData)
            );

            this.logger.debug(
              '✅ Datos combinados guardados en sessionStorage',
              {
                storageKey,
                guideId,
              }
            );

            // ✅ MOSTRAR DIALOG DE ÉXITO con opción de imprimir
            const recipient = this.recipientForm.getRawValue();
            const transfer = this.transferForm.getRawValue();

            // Obtener authorizationStatusSri de la respuesta
            const authorizationStatusSri = response.authorizationStatusSri;

            const dialogData: SuccessGuideDialogData = {
              guideId: guideId,
              guideNumber: guideNumber,
              recipientName: recipient.name,
              startDate: transfer.startDate
                ? this.formatDate(transfer.startDate)
                : undefined,
              endDate: transfer.endDate
                ? this.formatDate(transfer.endDate)
                : undefined,
              authorizationStatusSri: authorizationStatusSri,
            };

            const dialogRef = this.dialog.open(SuccessGuideDialogComponent, {
              width: '450px',
              disableClose: false, // Permitir cerrar con ESC o clic fuera
              data: dialogData,
            });

            // Limpiar el formulario cuando se cierre el diálogo
            dialogRef.afterClosed().subscribe(() => {
              this.logger.debug(
                '🔄 Diálogo de éxito cerrado, navegando a lista de guías'
              );
              this.router.navigate(['/delivery-guides']);
            });

            this.logger.debug('✅ Diálogo de éxito mostrado', {
              guideId,
              guideNumber,
            });
          } else {
            // Respuesta exitosa pero sin guideNumber (caso raro)
            this.showSuccess('Guía guardada correctamente');
            this.router.navigate(['/delivery-guides']);
          }
        } else {
          this.errorMessage = response.message || 'Error al crear guía';
        }
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.isProcessingGuide = false;
        this.errorMessage = error.error?.message || 'Error al crear guía';
        this.cdRef.detectChanges();
      },
    });
  }

  private validateGuide(): { isValid: boolean; message: string } {
    const errors: string[] = [];

    // Validar formulario de emisión
    if (!this.emissionForm.valid) {
      this.emissionForm.markAllAsTouched();
      if (this.emissionForm.get('establishment')?.invalid) {
        errors.push('Establecimiento');
      }
      if (this.emissionForm.get('emissionPoint')?.invalid) {
        errors.push('Punto de emisión');
      }
      if (this.emissionForm.get('issueDate')?.invalid) {
        errors.push('Fecha de emisión');
      }
    }

    // Validar formulario de transportista
    if (!this.carrierForm.valid) {
      this.carrierForm.markAllAsTouched();
      if (this.carrierForm.get('identification')?.invalid) {
        errors.push('Identificación del transportista');
      }
      if (this.carrierForm.get('identificationType')?.invalid) {
        errors.push('Tipo de identificación del transportista');
      }
      if (this.carrierForm.get('name')?.invalid) {
        errors.push('Nombre del transportista');
      }
      if (this.carrierForm.get('email')?.invalid) {
        errors.push('Email del transportista');
      }
    }

    // Validar formulario de traslado
    if (!this.transferForm.valid) {
      this.transferForm.markAllAsTouched();
      if (this.transferForm.get('originAddress')?.invalid) {
        errors.push('Dirección de origen');
      }
      // Verificar startDate: debe tener valor y ser válido
      const startDate = this.transferForm.get('startDate');
      if (startDate?.invalid || !startDate?.value) {
        errors.push('Fecha de inicio de transporte');
      }
      if (this.transferForm.get('endDate')?.invalid) {
        errors.push('Fecha de fin de transporte');
      }
      if (this.transferForm.get('transportPlate')?.invalid) {
        errors.push('Placa del vehículo');
      }
    }

    // Validar formulario de destinatario
    if (!this.recipientForm.valid) {
      this.recipientForm.markAllAsTouched();
      if (this.recipientForm.get('identification')?.invalid) {
        errors.push('Identificación del destinatario');
      }
      if (this.recipientForm.get('identificationType')?.invalid) {
        errors.push('Tipo de identificación del destinatario');
      }
      if (this.recipientForm.get('name')?.invalid) {
        errors.push('Nombre del destinatario');
      }
      if (this.recipientForm.get('destinationAddress')?.invalid) {
        errors.push('Dirección de destino');
      }
    }

    // Validar que haya al menos un producto
    if (this.guideDetails.length === 0) {
      errors.push('Al menos un producto en los detalles');
    }

    if (errors.length > 0) {
      const fieldsList = errors.join(', ');
      return {
        isValid: false,
        message: `Por favor complete los siguientes campos obligatorios: ${fieldsList}`,
      };
    }

    return { isValid: true, message: '' };
  }

  // ========== UTILIDADES ==========

  /**
   * Combina los datos del GuideDto enviado con la respuesta del backend
   * Similar a mergeInvoiceData en billing.component.ts
   */
  private mergeGuideData(guideDto: GuideDto, responseData: any): GuideDto {
    // El guideDto ya tiene todos los datos: clientDto, carrierDto, guideDetails, companyDto, etc.
    // responseData tiene los datos adicionales: idGuide, guideNumber, accessKey, authorizationNumber, etc.

    return {
      ...guideDto, // Mantener todos los datos originales
      // Sobrescribir/agregar con datos de la respuesta
      idGuide: responseData.idGuide,
      guideNumber: responseData.guideNumber,
      accessKey: responseData.accessKey,
      authorizationNumber: responseData.authorizationNumber,
      authorizationDate: responseData.authorizationDate,
      issueDate: responseData.issueDate || guideDto.issueDate,
      status: responseData.status || 'AUTHORIZED',
      // Mantener todos los detalles del guideDto original
      guideDetails: guideDto.guideDetails || [],
      documentReferences: guideDto.documentReferences || [],
      // Mantener todos los DTOs del guideDto original (que ya tienen companyDto completo)
      clientDto: guideDto.clientDto,
      carrierDto: guideDto.carrierDto,
      companyDto: guideDto.companyDto, // ✅ Mantener el companyDto del request que ya está completo
      establishment: guideDto.establishment,
    };
  }

  private showSuccess(message: string): void {
    this.toast.success(message);
  }

  private showError(message: string): void {
    this.toast.error(message);
  }

  // ========== MÉTODOS DE CAMPOS ADICIONALES ==========

  /**
   * Agrega un nuevo campo adicional
   */
  addAdditionalField(): void {
    const dialogRef = this.dialog.open(AddAdditionalFieldDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      disableClose: false,
      data: null,
    });

    dialogRef.afterClosed().subscribe((result: AdditionalFieldData | null) => {
      if (result) {
        this.additionalFields.push(result);
        this.additionalFieldsDataSource.data = [...this.additionalFields];
        this.showSuccess('Campo adicional agregado exitosamente');
        this.cdRef.detectChanges();
      }
    });
  }

  /**
   * Edita un campo adicional existente
   */
  editAdditionalField(index: number): void {
    const field = this.additionalFields[index];
    if (!field) {
      return;
    }

    const dialogRef = this.dialog.open(AddAdditionalFieldDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      disableClose: false,
      data: { ...field },
    });

    dialogRef.afterClosed().subscribe((result: AdditionalFieldData | null) => {
      if (result) {
        this.additionalFields[index] = result;
        this.additionalFieldsDataSource.data = [...this.additionalFields];
        this.showSuccess('Campo adicional actualizado exitosamente');
        this.cdRef.detectChanges();
      }
    });
  }

  /**
   * Elimina un campo adicional
   */
  deleteAdditionalField(index: number): void {
    if (index >= 0 && index < this.additionalFields.length) {
      this.additionalFields.splice(index, 1);
      this.additionalFieldsDataSource.data = [...this.additionalFields];
      this.showSuccess('Campo adicional eliminado exitosamente');
      this.cdRef.detectChanges();
    }
  }

  /**
   * Formatea los campos adicionales en un string JSON para enviar al backend
   */
  private formatAdditionalFields(): string | undefined {
    if (this.additionalFields.length === 0) {
      return undefined;
    }
    return JSON.stringify(this.additionalFields);
  }

  /**
   * Parsea el string de campos adicionales desde el backend
   */
  private parseAdditionalFields(additionalInformation?: string): void {
    this.additionalFields = [];

    if (!additionalInformation || additionalInformation.trim() === '') {
      this.additionalFieldsDataSource.data = [];
      return;
    }

    try {
      const parsed = JSON.parse(additionalInformation);
      if (Array.isArray(parsed)) {
        this.additionalFields = parsed.filter(
          (item) => item && item.name && item.description
        );
      }
    } catch (error) {
      // Si no es JSON válido, intentar parsear como formato antiguo (texto plano)
      this.logger.warn(
        'Error al parsear campos adicionales como JSON, intentando formato texto plano',
        error
      );
      // Podríamos intentar parsear un formato texto plano si existe
    }

    this.additionalFieldsDataSource.data = [...this.additionalFields];
  }
}
