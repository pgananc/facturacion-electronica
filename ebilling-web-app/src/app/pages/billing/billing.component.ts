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
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ADMIN_ROLE,
  CONTACT_TYPE_ADRESS,
  CONTACT_TYPE_MAIL,
  CONTACT_TYPE_MOBIL,
  CONTACT_TYPE_PHONE,
  IDENTIFICATION_TYPE,
  SUPER_ADMIN_ROLE,
  ID_TYPE_CONSUMIDOR_FINAL,
} from '../../_constants/constants';

import { CompanyFilter } from '../../_model/customer/company-filter';
import { decodeToken } from '../../_functions/functions';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ProductService } from '../../_service/product/product.service';
import { ProductCacheService } from '../../_service/product/product-cache.service';
import { InvoiceDetail } from '../../_model/invoice/invoice-detail';
import { Payment } from '../../_model/invoice/payment';
import { InvoiceService } from '../../_service/invoice/invoice.service';
import { DraftInvoiceService } from '../../_service/invoice/draft-invoice.service';
import { ProductDto } from '../../_model/product/product-dto';
import { ClientService } from 'src/app/_service/customer/client.service';
import { Client } from 'src/app/_model/customer/client';
import { ClientRequestDto } from 'src/app/_model/customer/client-request-dto';
import { ClientResponse } from 'src/app/_model/customer/client-response';
import { environment } from 'src/environments/environment';
import { LoggerService } from '../../_service/logger/logger.service';
import { HttpErrorResponse } from '@angular/common/http';
import { CompanyService } from '../../_service/customer/company.service';
import { InvoicePointService } from '../../_service/auth/invoice-point.service';
import { SelectProductDialogComponent } from './select-product-dialog/select-product-dialog.component';
import { AddPaymentDialogComponent } from './add-payment-dialog/add-payment-dialog.component';
import {
  SuccessInvoiceDialogComponent,
  SuccessInvoiceDialogData,
} from './success-invoice-dialog/success-invoice-dialog.component';
import { PaymentMethodService } from '../../_service/invoice/payment-method.service';
import { InvoiceDto } from '../../_model/invoice/invoice-dto';
import { ToastService } from '../../_service/notification/toast.service';
import { forkJoin, of } from 'rxjs';
import {
  UpdateClientComponent,
  ClientDialogData,
} from '../customer/client/update-client/update-client.component';
import {
  UpdateProductComponent,
  ProductDialogData,
} from '../product/update-product/update-product.component';
import { catchError } from 'rxjs/operators';
import { PermissionService } from '../../_service/permission/permission.service';
import { PermissionAdminService } from '../../_service/permission/permission-admin.service';
import { UserRoleService } from '../../_service/auth/user-role.service';

@Component({
  selector: 'app-billing',
  templateUrl: './billing.component.html',
  styleUrls: ['./billing.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BillingComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('productSearchInput', { read: ElementRef })
  productSearchInput!: ElementRef;

  @ViewChild('invoiceButton', { read: ElementRef })
  invoiceButton!: ElementRef;

  // Exponer Math para usarlo en el template
  Math = Math;

  roles: string[] = [];
  companiesIds: number[] = [];
  invoiceForm: FormGroup;
  buyerForm!: FormGroup;
  clientType: number = 0;
  detailsForm!: FormGroup;

  // ID del borrador que se está editando (null si es una factura nueva)
  // Público para poder acceder desde el template
  currentDraftId: number | null = null;

  get items(): FormArray {
    return this.detailsForm.get('items') as FormArray;
  }

  productCtrl = new FormControl('');
  invoiceDetailDataSource = new MatTableDataSource<InvoiceDetail>([]);
  displayedColumnsPurchaseProofDetail: string[] = [
    'code',
    'quantity',
    'description',
    'unitPrice',
    'discount',
    'totalValue',
    'iva',
    'ice',
  ];
  dataSource = new MatTableDataSource<CompanyFilter>([]);
  status: boolean = true;
  quantity: number = 0;

  branchOffices: { id: number; code: string; address: string }[] = [];
  emissionPoints: { id: number; code: string }[] = [];
  companyData: any = null; // Información completa de la empresa
  maxDate = new Date();

  // IVA global para toda la factura (modificable)
  globalIvaPercentage = new FormControl(15, [
    Validators.min(0),
    Validators.max(100),
  ]);

  paymentsDataSource = new MatTableDataSource<Payment>([]);
  displayedPaymentColumns = ['paymentType', 'value', 'term', 'time', 'actions'];

  totals: { label: string; value: number }[] = [
    { label: 'Subtotal sin impuestos', value: 0 },
    { label: 'Total descuento', value: 0 },
    { label: 'Subtotal con descuento', value: 0 },
    { label: 'IVA', value: 0 },
    { label: 'Valor ICE', value: 0 },
  ];

  // ✅ Subtotales agrupados por % de IVA
  subtotalsByIva: { ivaPercent: number; subtotal: number; ivaValue: number }[] =
    [];

  paymentsForm!: FormGroup;

  get paymentItems(): FormArray {
    return this.paymentsForm.get('items') as FormArray;
  }

  // Tabla de valores según especificación del backend:
  // CÉDULA = 1, RUC = 2, PASAPORTE = 3, CONSUMIDOR FINAL = 4, EXTERIOR = 5
  idTypes = [
    { value: 1, viewValue: 'Cédula' }, // idType: 1, Longitud: 10 dígitos
    { value: 2, viewValue: 'RUC' }, // idType: 2, Longitud: 13 dígitos
    { value: 3, viewValue: 'Pasaporte' }, // idType: 3, Longitud: Variable
    { value: 4, viewValue: 'Consumidor final' }, // idType: 4, Longitud: 13 (9999999999999)
    { value: 5, viewValue: 'Identificación del exterior' }, // idType: 5, Longitud: Variable
  ];
  identificationTypes = IDENTIFICATION_TYPE;
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    public route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly productService: ProductService,
    private readonly productCacheService: ProductCacheService,
    private readonly purchaseProofService: InvoiceService,
    private readonly draftInvoiceService: DraftInvoiceService,
    private readonly toast: ToastService,
    private readonly clientService: ClientService,
    private readonly logger: LoggerService,
    private readonly companyService: CompanyService,
    private readonly invoicePointService: InvoicePointService,
    private readonly dialog: MatDialog,
    private readonly cdRef: ChangeDetectorRef,
    private readonly paymentMethodService: PaymentMethodService,
    private readonly permissionService: PermissionService,
    private readonly permissionAdminService: PermissionAdminService,
    private readonly userRoleService: UserRoleService
  ) {
    // Inicializar FormGroups en el constructor para evitar errores si no hay token
    this.invoiceForm = this.fb.group({
      branchOffice: [null, Validators.required],
      legalName: [''], // Sin validación required ya que se setea automáticamente
      issuedDate: [new Date(), Validators.required],
      emissionPoint: [null, Validators.required],
      tip10: [false],
    });

    this.buyerForm = this.fb.group({
      idClient: [null], // ✅ ID del cliente si ya existe en la BD
      identification: ['', Validators.required],
      idType: [null, Validators.required],
      businessName: ['', Validators.required],
      address: [''],
      phone: [''],
      email: ['', [Validators.required, Validators.email]],
    });

    this.detailsForm = this.fb.group({
      items: this.fb.array([]),
    });

    this.paymentsForm = this.fb.group({
      items: this.fb.array([]),
    });
  }

  ngOnInit() {
    this.logger.debug('Inicializando componente de billing');

    const token = localStorage.getItem(environment.TOKEN);
    if (!token) {
      this.logger.warn('No hay token en localStorage, redirigiendo a login');
      return;
    }

    const decodedToken = decodeToken();
    if (!decodedToken) {
      this.logger.warn('Token inválido, redirigiendo a login');
      return;
    }

    // Obtener companiesIds del token (necesario para filtrado)
    this.companiesIds = decodedToken.companies || [];

    // Cargar roles de la compañía actual usando el servicio centralizado
    this.userRoleService.getCurrentCompanyRoles().subscribe({
      next: (roles) => {
        this.roles = roles;
        this.logger.debug('Roles cargados desde UserRoleService:', this.roles);

        // Actualizar columnas según roles
        if (
          this.roles.includes(SUPER_ADMIN_ROLE.name) ||
          this.roles.includes(ADMIN_ROLE.name)
        ) {
          if (!this.displayedColumnsPurchaseProofDetail.includes('actions')) {
            this.displayedColumnsPurchaseProofDetail.push('actions');
          }
        }
        this.cdRef.markForCheck();
      },
      error: (err) => {
        this.logger.error('Error al cargar roles:', err);
        this.roles = [];
        this.cdRef.markForCheck();
      },
    });

    // Pre-cargar formas de pago para que el caché esté disponible
    this.logger.debug('Pre-cargando formas de pago para botones rápidos');
    this.paymentMethodService.findAll().subscribe({
      next: (methods) => {
        this.logger.debug(
          `✅ ${methods.length} formas de pago disponibles en caché`
        );
      },
      error: (err) => {
        this.logger.error('Error al pre-cargar formas de pago', err);
      },
    });

    // Pre-cargar productos en cache para búsquedas rápidas y carga de borradores
    this.logger.debug('🗄️ Pre-cargando productos en cache...');
    this.productCacheService.loadProducts().subscribe({
      next: (products) => {
        this.logger.debug(
          `✅ ${products.length} productos cargados en cache y disponibles para facturación`
        );
      },
      error: (err) => {
        this.logger.error('❌ Error al pre-cargar productos en cache', err);
        // No bloqueamos la facturación si falla la carga del cache
        // El usuario podrá buscar productos normalmente
      },
    });

    // ✅ Verificar permiso para crear productos (el backend decide según rol y plan)
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

    // ✅ Validar permisos del rol del usuario consultando al backend
    this.validateUserRolePermissions();

    const savedCompanyId = localStorage.getItem(environment.ID_COMPANY);
    let savedEstablishmentId = localStorage.getItem(
      environment.ID_ESTABLISHMENT
    );
    const savedInvoicePointId = localStorage.getItem(
      environment.ID_INVOICE_POINT
    );

    const invoicePointDataStr = localStorage.getItem('INVOICE_POINT_DATA');
    if (invoicePointDataStr) {
      try {
        const invoicePointData = JSON.parse(invoicePointDataStr);
        this.logger.debug('INVOICE_POINT_DATA encontrado:', invoicePointData);
      } catch (e) {
        this.logger.error('Error al parsear INVOICE_POINT_DATA:', e);
      }
    }

    // ✅ Si no hay ID_ESTABLISHMENT, intentar obtenerlo de INVOICE_POINT_DATA
    if (!savedEstablishmentId) {
      const invoicePointDataStr = localStorage.getItem('INVOICE_POINT_DATA');
      if (invoicePointDataStr) {
        try {
          const invoicePointData = JSON.parse(invoicePointDataStr);
          if (invoicePointData.establishmentId) {
            savedEstablishmentId = invoicePointData.establishmentId.toString();
            // Guardar en localStorage para futuras referencias
            if (savedEstablishmentId) {
              localStorage.setItem(
                environment.ID_ESTABLISHMENT,
                savedEstablishmentId
              );
            }
            this.logger.debug(
              `✅ ID_ESTABLISHMENT obtenido de INVOICE_POINT_DATA: ${savedEstablishmentId}`
            );
          }
        } catch (error) {
          this.logger.error('Error al parsear INVOICE_POINT_DATA', error);
        }
      }
    }

    // Establecer nombre comercial automáticamente desde localStorage
    const companyName = localStorage.getItem(environment.NAME_COMPANY);
    if (companyName) {
      this.invoiceForm.patchValue({ legalName: companyName });
      // Deshabilitar el campo para que no sea modificable
      this.invoiceForm.get('legalName')?.disable();
      this.logger.debug(
        `✅ Nombre comercial establecido automáticamente: ${companyName}`
      );
    }

    // ✅ Cargar datos de la compañía si hay ID_COMPANY (el establecimiento puede ser null)
    if (savedCompanyId) {
      this.loadCompanyData(
        Number(savedCompanyId),
        savedEstablishmentId ? Number(savedEstablishmentId) : null,
        savedInvoicePointId ? Number(savedInvoicePointId) : null
      );
    } else {
      this.logger.warn(
        '⚠️ No hay ID_COMPANY en localStorage, no se pueden cargar los datos'
      );
    }

    this.invoiceDetailDataSource.sort = this.sort;

    // Listener para recalcular total cuando cambia la propina
    this.invoiceForm.get('tip10')?.valueChanges.subscribe(() => {
      this.cdRef.markForCheck();
    });

    // Listener para recalcular IVA cuando cambia el porcentaje global
    this.globalIvaPercentage.valueChanges.subscribe(() => {
      this.calculateTotals();
    });

    // Verificar si venimos de la lista de borradores con un ID para editar
    this.route.queryParams.subscribe((params) => {
      const draftId = params['draftId'];

      if (draftId) {
        this.logger.debug('🔍 Detectado draftId en query params:', draftId);
        this.loadDraftForEditing(Number(draftId));

        // Limpiar el query param para que no se recargue si el usuario refresca
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true,
        });
      } else {
        // Si no hay draftId en queryParams, limpiar localStorage y crear factura nueva
        // Esto asegura que al hacer clic en "Nueva Factura" no se recupere un borrador anterior
        this.currentDraftId = null;
        localStorage.removeItem('CURRENT_DRAFT_ID');
        this.logger.debug(
          '📝 Modo: Nueva factura (no hay draftId en query params, localStorage limpiado)'
        );
      }
    });

    // ✅ Inicializar totales con valores en 0 para que los campos SIEMPRE sean visibles
    this.calculateTotals();
  }

  /**
   * Lifecycle hook - Se ejecuta cuando el componente se destruye
   * Nota: El heartbeat es manejado por InvoicePointSessionService (global),
   * por lo que NO necesitamos detenerlo aquí. El heartbeat se mantiene activo
   * incluso si el usuario navega a otros componentes.
   */
  ngAfterViewInit(): void {
    // Inicialización después de que la vista esté lista
  }

  ngOnDestroy(): void {
    this.logger.debug('Componente de billing destruido');
  }

  /**
   * Verifica si el total de pagos está completo y enfoca el botón Facturar
   */
  private checkAndFocusInvoiceButton(): void {
    const totalInvoice = this.getTotalInvoice();
    const totalPayments = this.getTotalPayments();

    // Tolerancia de 0.01 para errores de redondeo
    const difference = Math.abs(totalInvoice - totalPayments);

    // Si el total está completo (diferencia <= 0.01), enfocar el botón Facturar
    if (difference <= 0.01 && totalPayments > 0) {
      setTimeout(() => {
        this.focusInvoiceButton();
      }, 200);
    }
  }

  /**
   * Enfoca el botón Facturar
   */
  private focusInvoiceButton(): void {
    try {
      if (this.invoiceButton?.nativeElement) {
        const buttonElement = this.invoiceButton
          .nativeElement as HTMLButtonElement;
        if (buttonElement && typeof buttonElement.focus === 'function') {
          buttonElement.focus();
          this.logger.debug('✅ Botón Facturar enfocado');
        }
      }
    } catch (error) {
      this.logger.warn('No se pudo enfocar el botón Facturar', error);
    }
  }

  /**
   * Enfoca el campo de búsqueda de productos
   */
  private focusProductSearch(): void {
    // Primero, quitar el foco del campo actual (especialmente importante cuando se presiona Enter)
    const activeElement = document.activeElement as HTMLElement;
    if (activeElement && activeElement.blur) {
      activeElement.blur();
    }

    // Usar setTimeout para asegurar que el DOM esté actualizado después del cambio de detección
    setTimeout(() => {
      try {
        if (this.productSearchInput?.nativeElement) {
          // El ViewChild con ElementRef apunta directamente al elemento input
          const inputElement = this.productSearchInput
            .nativeElement as HTMLInputElement;

          if (inputElement && typeof inputElement.focus === 'function') {
            inputElement.focus();
            this.logger.debug('✅ Campo de búsqueda de productos enfocado');
          } else {
            this.logger.warn(
              'El elemento no es un input válido o no tiene método focus'
            );
          }
        } else {
          this.logger.warn('productSearchInput no está disponible');
        }
      } catch (error) {
        this.logger.warn(
          'No se pudo enfocar el campo de búsqueda de productos',
          error
        );
      }
    }, 400);
  }

  searchBuyer(event?: Event): void {
    // Prevenir comportamiento por defecto y propagación del evento
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    const id = this.buyerForm.get('identification')!.value;

    if (!id || id.trim() === '') {
      this.toast.warning('Ingrese una identificación para buscar');
      return;
    }

    this.isSearchingBuyer = true; // ✅ Activar estado de carga
    this.cdRef.detectChanges();
    this.findClient();
  }

  /**
   * Maneja el evento Enter en el campo de identificación
   * Permite buscar al cliente presionando Enter además del botón de búsqueda
   */
  onIdentificationEnter(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (keyboardEvent.key === 'Enter' || keyboardEvent.keyCode === 13) {
      keyboardEvent.preventDefault(); // Prevenir el comportamiento por defecto del Enter
      keyboardEvent.stopPropagation(); // Evitar que el evento se propague

      // Pequeño delay para asegurar que el evento se procese completamente
      setTimeout(() => {
        this.searchBuyer();
      }, 0);
    }
  }

  searchProduct(): void {
    const term = this.productCtrl.value?.trim() ?? '';
    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));

    const token = localStorage.getItem(environment.TOKEN);

    // Pasar companyId explícitamente para filtrar productos
    this.productService
      .searchActiveProductsForBilling(term, companyId)
      .subscribe({
        next: (products: ProductDto[]) => {
          if (products.length === 0) {
            // Mostrar toast con opción para crear producto
            this.toast.info(
              'Producto no encontrado. Puede crear uno nuevo desde el botón de agregar.'
            );
            this.logger.debug(
              'Producto no encontrado, mostrando opción para crear'
            );
            return;
          }

          this.logger.debug(`✅ ${products.length} producto(s) encontrado(s)`);
          this.openProductSelectionDialog(products, term);
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 401) {
            this.toast.error(
              'Sesión expirada. Por favor, inicie sesión nuevamente.'
            );
            this.logger.error(
              '❌ Token inválido o expirado al buscar productos',
              {
                status: err.status,
                url: err.url,
                message: err.message,
              }
            );
          } else {
            this.toast.error('Error al buscar productos. Intente nuevamente.');
            this.logger.error('Error al buscar productos', err);
          }
        },
      });
  }

  private openProductSelectionDialog(
    products: ProductDto[],
    searchTerm: string
  ): void {
    const dialogRef = this.dialog.open(SelectProductDialogComponent, {
      width: '90vw',
      maxWidth: '1200px',
      maxHeight: '90vh',
      data: { products, searchTerm },
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((selectedProducts: ProductDto[]) => {
      if (selectedProducts && selectedProducts.length > 0) {
        this.addProductsToInvoice(selectedProducts);
        this.productCtrl.setValue('');
      }
    });
  }

  private addProductsToInvoice(products: ProductDto[]): void {
    const details: InvoiceDetail[] = products.map((p) => ({
      idProduct: p.idProduct,
      code: p.mainCode, // ✅ Usar mainCode del producto
      description: p.description || p.name,
      unitPrice: p.unitPrice,
      quantity: 1,
      discount: 0,
      ice: p.iceTaxType?.value || 0, // ✅ Usar iceTaxType.value
      totalValue: p.unitPrice,
      iva: p.ivaTaxType?.value || 0, // ✅ Usar ivaTaxType.value
      product: p, // ✅ Guardar el producto completo
    }));

    const currentDetails = this.invoiceDetailDataSource.data;
    this.invoiceDetailDataSource.data = [...currentDetails, ...details];

    this.initDetailsForm(this.invoiceDetailDataSource.data);
    this.calculateTotals();

    this.toast.success(
      `${products.length} producto(s) agregado(s) a la factura`
    );

    this.logger.debug('Productos agregados a la factura', {
      count: products.length,
    });
  }

  removeDetailItem(index: number): void {
    if (!confirm('¿Eliminar este producto de la factura?')) return;

    this.invoiceDetailDataSource.data =
      this.invoiceDetailDataSource.data.filter((_, i) => i !== index);

    this.items.removeAt(index);
    this.calculateTotals();
  }

  private initDetailsForm(products: InvoiceDetail[]) {
    this.items.clear();
    products.forEach((p, index) => {
      const group = this.fb.group({
        code: [p.code || p.product?.mainCode || p.idProduct], // ✅ Prioridad: code > mainCode > idProduct
        quantity: [p.quantity, [Validators.required, Validators.min(1)]],
        description: [p.description],
        unitPrice: [p.unitPrice, [Validators.required, Validators.min(0)]],
        discount: [p.discount || 0, [Validators.min(0), Validators.max(100)]], // % de descuento
        totalValue: [p.totalValue],
        iva: [
          p.iva || p.product?.ivaTaxType?.value || 0,
          [Validators.min(0), Validators.max(100)],
        ], // ✅ % de IVA
        ice: [
          p.ice || p.product?.iceTaxType?.value || 0,
          [Validators.min(0), Validators.max(100)],
        ], // ✅ % de ICE
      });

      group.get('quantity')?.valueChanges.subscribe(() => {
        this.updateDetailTotal(index);
        this.calculateTotals();
      });

      group.get('unitPrice')?.valueChanges.subscribe(() => {
        this.updateDetailTotal(index);
        this.calculateTotals();
      });

      group.get('discount')?.valueChanges.subscribe(() => {
        this.updateDetailTotal(index);
        this.calculateTotals();
      });

      group.get('ice')?.valueChanges.subscribe(() => {
        this.calculateTotals();
      });

      this.items.push(group);
    });

    // Forzar detección de cambios con OnPush
    this.cdRef.markForCheck();
  }

  private updateDetailTotal(index: number): void {
    const group = this.items.at(index);
    if (!group) return;

    const quantity = group.get('quantity')?.value || 0;
    const unitPrice = group.get('unitPrice')?.value || 0;
    const discountPercent = group.get('discount')?.value || 0;

    // Calcular subtotal
    const subtotal = quantity * unitPrice;

    // Calcular descuento en valor monetario: subtotal * (discountPercent / 100)
    const discountAmount = subtotal * (discountPercent / 100);

    // Total = subtotal - descuento
    // ✅ Usar Math.round() para redondeo matemático correcto a 2 decimales
    const total = Math.round((subtotal - discountAmount) * 100) / 100;

    group.get('totalValue')?.setValue(total, { emitEvent: false });

    if (this.invoiceDetailDataSource.data[index]) {
      this.invoiceDetailDataSource.data[index].totalValue = total;
    }
  }

  calculateTotals(): void {
    let subtotalSinImpuestos = 0;
    let totalDescuentoMonetario = 0;
    let totalICE = 0;

    // ✅ Map para agrupar subtotales por % de IVA
    // Inicializar SIEMPRE con las tarifas comunes en 0
    const subtotalsByIvaMap = new Map<number, number>([
      [15, 0], // IVA 15%
      [5, 0], // IVA 5%
      [0, 0], // IVA 0%
    ]);

    this.items.controls.forEach((group) => {
      const quantity = group.get('quantity')?.value || 0;
      const unitPrice = group.get('unitPrice')?.value || 0;
      const discountPercent = group.get('discount')?.value || 0;
      const icePercent = group.get('ice')?.value || 0;
      const ivaPercent = group.get('iva')?.value || 0; // ✅ % de IVA del producto

      // Subtotal de la línea (precio × cantidad)
      const lineSubtotal = quantity * unitPrice;

      // Descuento en valor monetario de la línea
      const lineDiscount = lineSubtotal * (discountPercent / 100);

      // Base imponible después del descuento
      const baseImponible = lineSubtotal - lineDiscount;

      // ICE se calcula sobre la base imponible
      const lineICE = baseImponible * (icePercent / 100);

      subtotalSinImpuestos += lineSubtotal;
      totalDescuentoMonetario += lineDiscount;
      totalICE += lineICE;

      // ✅ Agrupar subtotales por % de IVA
      const currentSubtotal = subtotalsByIvaMap.get(ivaPercent) || 0;
      subtotalsByIvaMap.set(ivaPercent, currentSubtotal + baseImponible);
    });

    // Subtotal después de descuentos
    const subtotalConDescuento = subtotalSinImpuestos - totalDescuentoMonetario;

    // ✅ Calcular subtotales e IVA por cada tarifa
    this.subtotalsByIva = [];
    let totalIVA = 0;

    subtotalsByIvaMap.forEach((subtotal, ivaPercent) => {
      const ivaValue = subtotal * (ivaPercent / 100);

      // ✅ Usar Math.round() para redondeo matemático correcto (no toFixed)
      const subtotalRounded = Math.round(subtotal * 100) / 100;
      const ivaValueRounded = Math.round(ivaValue * 100) / 100;

      totalIVA += ivaValueRounded; // Sumar valores ya redondeados

      this.subtotalsByIva.push({
        ivaPercent: ivaPercent,
        subtotal: subtotalRounded,
        ivaValue: ivaValueRounded,
      });
    });

    // ✅ Ordenar por % de IVA descendente (15%, 5%, 0%)
    this.subtotalsByIva.sort((a, b) => b.ivaPercent - a.ivaPercent);

    this.totals = [
      {
        label: 'Subtotal sin impuestos',
        value: Math.round(subtotalSinImpuestos * 100) / 100,
      },
      {
        label: 'Total descuento',
        value: Math.round(totalDescuentoMonetario * 100) / 100,
      },
      {
        label: 'Subtotal con descuento',
        value: Math.round(subtotalConDescuento * 100) / 100,
      },
      {
        label: 'IVA Total',
        value: Math.round(totalIVA * 100) / 100, // ✅ Redondeo matemático
      },
      {
        label: 'Valor ICE',
        value: Math.round(totalICE * 100) / 100,
      },
    ];

    // Forzar detección de cambios para actualizar la vista
    this.cdRef.markForCheck();
  }

  getTotalInvoice(): number {
    const subtotalConDescuento =
      this.totals.find((t) => t.label === 'Subtotal con descuento')?.value || 0;
    const iva = this.totals.find((t) => t.label === 'IVA Total')?.value || 0; // ✅ Cambiar 'IVA' a 'IVA Total'
    const ice = this.totals.find((t) => t.label === 'Valor ICE')?.value || 0;

    let total = subtotalConDescuento + iva + ice;

    // Agregar propina del 10% si está marcado (sobre el subtotal con descuento)
    if (this.invoiceForm.get('tip10')?.value) {
      total += this.getTipAmount();
    }

    // ✅ Usar Math.round() para redondeo matemático correcto a 2 decimales
    // Ejemplo: 173.075 se redondea correctamente a 173.08
    return Math.round(total * 100) / 100;
  }

  getTipAmount(): number {
    if (this.invoiceForm.get('tip10')?.value) {
      const subtotalConDescuento =
        this.totals.find((t) => t.label === 'Subtotal con descuento')?.value ||
        0;
      // ✅ Usar Math.round() para redondeo matemático correcto a 2 decimales
      return Math.round(subtotalConDescuento * 0.1 * 100) / 100;
    }
    return 0;
  }

  getSubtotal(): number {
    return (
      this.totals.find((t) => t.label === 'Subtotal sin impuestos')?.value || 0
    );
  }

  /**
   * Agrega rápidamente una forma de pago común (Efectivo, Tarjeta de débito/crédito)
   * Usa el servicio con caché para evitar llamadas innecesarias al backend
   *
   * @param paymentCode - Código SRI de la forma de pago ("01", "16", "19")
   */
  addQuickPayment(paymentCode: string): void {
    this.logger.debug(`Agregando forma de pago rápida: código ${paymentCode}`);

    // Siempre llamar a findAll() - si hay petición en curso, la reutiliza
    // Si ya está en caché, devuelve inmediatamente el caché
    this.paymentMethodService.findAll().subscribe({
      next: (methods) => {
        const method = methods.find((m) => m.paymentMethodCode === paymentCode);

        if (method) {
          this.addPaymentToTable(method);
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
        this.toast.error('Error al cargar formas de pago. Intente nuevamente.');
        this.logger.error('Error al cargar formas de pago', err);
      },
    });
  }

  /**
   * Agrega una forma de pago a la tabla usando el método del backend
   * Valida que no se repita el mismo tipo de pago
   */
  private addPaymentToTable(method: {
    idPaymentMethod: number;
    paymentMethodType: string;
    paymentMethodCode: string;
  }): void {
    // Validar que no exista ya esa forma de pago
    const alreadyExists = this.paymentsDataSource.data.some(
      (payment) => payment.paymentMethodCode === method.paymentMethodCode
    );

    if (alreadyExists) {
      this.toast.warning(
        `${method.paymentMethodType} ya está agregado. No se puede duplicar.`
      );
      this.logger.warn('Intento de agregar forma de pago duplicada', {
        code: method.paymentMethodCode,
        type: method.paymentMethodType,
      });
      return;
    }

    const paymentData = {
      idPaymentMethod: method.idPaymentMethod, // ✅ Guardar el ID del backend
      paymentType: method.paymentMethodType,
      paymentMethodCode: method.paymentMethodCode,
      value: 0.0, // Iniciar en 0.00, el usuario ingresará el valor manualmente
      term: undefined,
      time: undefined,
    };

    // Agregar a la tabla
    this.paymentsDataSource.data = [
      ...this.paymentsDataSource.data,
      paymentData,
    ];

    // Crear FormGroup
    const newPaymentGroup = this.fb.group({
      idPaymentMethod: [paymentData.idPaymentMethod], // ✅ Incluir en el FormGroup
      paymentType: [paymentData.paymentType],
      paymentMethodCode: [paymentData.paymentMethodCode],
      value: [paymentData.value, [Validators.required, Validators.min(0.01)]],
      term: [paymentData.term],
      time: [paymentData.time],
    });

    // Agregar al FormArray
    this.paymentItems.push(newPaymentGroup);

    this.toast.success(
      `${method.paymentMethodType} agregado. Ingrese el valor.`
    );

    this.logger.debug('Forma de pago rápida agregada', paymentData);
  }

  /**
   * Se ejecuta cuando el usuario termina de editar un valor de forma de pago
   * (blur o Enter)
   */
  onPaymentValueBlur(index: number): void {
    const paymentGroup = this.paymentItems.at(index);
    const newValue = paymentGroup.get('value')?.value ?? 0;

    this.updatePaymentDataSource(index, newValue);

    // Verificar si el total está completo y enfocar el botón Facturar
    this.checkAndFocusInvoiceButton();
  }

  /**
   * Actualiza el DataSource de formas de pago cuando cambia un valor
   * Valida que no exceda el total de la factura
   */
  private updatePaymentDataSource(index: number, newValue: number): void {
    const totalInvoice = this.getTotalInvoice();
    const numericValue = Number(newValue) || 0;

    // Si el valor excede el total, mostrar advertencia (pero NO ajustar automáticamente)
    if (numericValue > totalInvoice) {
      this.toast.warning(
        `El valor ingresado ($${numericValue.toFixed(
          2
        )}) supera el total de la factura ($${totalInvoice.toFixed(2)})`
      );
    }

    // Actualizar el DataSource sin modificar el valor ingresado
    const currentData = [...this.paymentsDataSource.data];
    if (currentData[index]) {
      currentData[index] = {
        ...currentData[index],
        value: numericValue,
      };
      this.paymentsDataSource.data = currentData;
      this.cdRef.markForCheck();
    }
  }

  /**
   * Abre el diálogo para agregar una nueva forma de pago
   * Carga las formas de pago desde el backend (con caché) y permite seleccionar
   * Valida que no se repita el mismo tipo de pago
   */
  addPayment() {
    const dialogRef = this.dialog.open(AddPaymentDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((paymentData) => {
      if (paymentData) {
        // Validar que no exista ya esa forma de pago
        const alreadyExists = this.paymentsDataSource.data.some(
          (payment) =>
            payment.paymentMethodCode === paymentData.paymentMethodCode
        );

        if (alreadyExists) {
          this.toast.warning(
            `${paymentData.paymentType} ya está agregado. No se puede duplicar.`
          );
          this.logger.warn('Intento de agregar forma de pago duplicada', {
            code: paymentData.paymentMethodCode,
            type: paymentData.paymentType,
          });
          return;
        }

        // Agregar la nueva forma de pago a la tabla
        this.paymentsDataSource.data = [
          ...this.paymentsDataSource.data,
          paymentData,
        ];

        // Crear FormGroup
        const newPaymentGroup = this.fb.group({
          paymentType: [paymentData.paymentType],
          paymentMethodCode: [paymentData.paymentMethodCode],
          value: [
            paymentData.value,
            [Validators.required, Validators.min(0.01)],
          ],
          term: [paymentData.term],
          time: [paymentData.time],
        });

        // Agregar al FormArray
        this.paymentItems.push(newPaymentGroup);

        // Actualizar DataSource con el valor inicial del diálogo
        const index = this.paymentItems.length - 1;
        this.updatePaymentDataSource(index, paymentData.value);

        this.logger.debug('Forma de pago agregada', paymentData);
        this.toast.success('Forma de pago agregada');
      }
    });
  }

  removePayment(index: number) {
    this.paymentsDataSource.data = this.paymentsDataSource.data.filter(
      (_, i) => i !== index
    );

    if (this.paymentItems.length > index) {
      this.paymentItems.removeAt(index);
    }

    this.cdRef.markForCheck();

    // Verificar si el total sigue siendo completo después de eliminar
    this.checkAndFocusInvoiceButton();
  }

  /**
   * Obtiene la suma total de todas las formas de pago
   */
  getTotalPayments(): number {
    return this.paymentsDataSource.data.reduce((sum, payment) => {
      return sum + (Number(payment.value) || 0);
    }, 0);
  }

  /**
   * Valida que la suma de formas de pago sea igual al total de la factura
   */
  validatePaymentTotal(): boolean {
    const totalInvoice = this.getTotalInvoice();
    const totalPayments = this.getTotalPayments();

    // Tolerancia de 0.01 para errores de redondeo
    const difference = Math.abs(totalInvoice - totalPayments);

    if (difference > 0.01) {
      this.toast.warning(
        `El total de formas de pago ($${totalPayments.toFixed(
          2
        )}) no coincide con el total de la factura ($${totalInvoice.toFixed(
          2
        )})`
      );
      return false;
    }

    return true;
  }

  // Estado de carga para el botón de facturar
  isProcessingInvoice = false;
  isSavingDraft = false;
  isSearchingBuyer = false; // ✅ Estado de carga para búsqueda de cliente

  // ✅ Permisos para mostrar/ocultar botones
  canCreateProduct = false; // Se inicializa en ngOnInit

  /**
   * Guardar factura completa (registra, firma y envía al SRI)
   * POST /api/invoice con status: AUTHORIZED
   */
  signAndSend(): void {
    // Validar formularios antes de guardar
    if (!this.validateInvoiceData()) {
      return;
    }

    // Primero verificar que haya una suscripción activa
    const subscription = this.permissionService.getSubscriptionValue();

    // Log detallado para diagnóstico
    this.logger.debug(
      '[BillingComponent] Validando suscripción antes de crear factura',
      {
        hasSubscription: !!subscription,
        subscription: subscription
          ? {
              idSubscription: subscription.idSubscription,
              idCompany: subscription.idCompany,
              status: subscription.status,
              planCode: subscription.plan?.code,
              expiresAt: subscription.expiresAt,
              // Log completo para ver qué campos tiene realmente
              allFields: Object.keys(subscription),
            }
          : null,
      }
    );

    // Validar suscripción: puede tener 'status' o 'subscriptionStatus'
    const subscriptionStatus =
      (subscription as any)?.status ||
      (subscription as any)?.subscriptionStatus;

    if (
      !subscription ||
      !subscriptionStatus ||
      subscriptionStatus !== 'ACTIVE'
    ) {
      this.toast.error(
        'No se encontró una suscripción activa para la compañía. Por favor, contacte al administrador para activar su suscripción.'
      );
      this.logger.warn(
        '[BillingComponent] Intento de crear factura sin suscripción activa',
        {
          hasSubscription: !!subscription,
          status: subscriptionStatus,
          subscriptionObject: subscription,
        }
      );
      return;
    }

    // Validar límites de documentos antes de crear factura
    this.permissionService.canEmitDocumentWithDetails('INVOICE').subscribe({
      next: (response) => {
        this.logger.debug(
          `[BillingComponent] Validación de límites: allowed=${response.allowed}, usage=${response.currentUsage}, limit=${response.limit}, remaining=${response.remaining}, message=${response.message}`
        );

        if (!response.allowed) {
          const errorMessage =
            response.message ||
            'Has alcanzado el límite de facturas de tu plan. Por favor, actualiza tu plan para continuar emitiendo documentos.';
          this.toast.error(errorMessage);
          this.logger.warn(
            `Intento de crear factura con límite alcanzado: usage=${response.currentUsage}, limit=${response.limit}, remaining=${response.remaining}`
          );
          return;
        }

        // Si puede emitir, continuar con el proceso normal
        this.processInvoiceCreation();
      },
      error: (err) => {
        this.logger.error('Error al validar límites de documentos', err);

        // Si el error es específico de "no hay suscripción activa", mostrar mensaje claro
        if (
          err.status === 400 &&
          err.error?.message?.includes('suscripción activa')
        ) {
          this.toast.error(
            'No se encontró una suscripción activa para la compañía. Por favor, contacte al administrador para activar su suscripción.'
          );
          return;
        }

        // En caso de otros errores, permitir continuar (fallback)
        this.processInvoiceCreation();
      },
    });
  }

  /**
   * Procesa la creación de la factura después de validar límites
   */
  private processInvoiceCreation(): void {
    // Activar estado de carga
    this.isProcessingInvoice = true;
    this.cdRef.detectChanges();

    // 🔍 Restaurar currentDraftId desde localStorage si no está definido
    if (!this.currentDraftId) {
      const savedDraftId = localStorage.getItem('CURRENT_DRAFT_ID');
      if (savedDraftId) {
        this.currentDraftId = Number(savedDraftId);
        this.logger.debug(
          `💾 [processInvoiceCreation] currentDraftId restaurado desde localStorage: ${this.currentDraftId}`
        );
      }
    }

    // 🔍 Verificar currentDraftId antes de construir el payload
    this.logger.debug('🔍 [processInvoiceCreation] Estado actual:', {
      currentDraftId: this.currentDraftId || 'NO DEFINIDO',
      localStorageDraftId: localStorage.getItem('CURRENT_DRAFT_ID') || 'NO ENCONTRADO',
    });

    const invoiceDto = this.buildInvoicePayload('AUTHORIZED');

    // Si estamos editando un borrador existente, incluir el idInvoice para actualizar
    // (Aunque ya debería estar incluido en buildInvoicePayload, lo verificamos aquí también)
    if (this.currentDraftId && !invoiceDto.idInvoice) {
      invoiceDto.idInvoice = this.currentDraftId;
      this.logger.debug(
        `📝 [processInvoiceCreation] idInvoice agregado manualmente: ${this.currentDraftId}`
      );
    } else if (this.currentDraftId && invoiceDto.idInvoice) {
      this.logger.debug(
        `✅ [processInvoiceCreation] idInvoice ya incluido en payload: ${invoiceDto.idInvoice}`
      );
    } else {
      this.logger.warn(
        `⚠️ [processInvoiceCreation] NO hay currentDraftId - se creará una nueva factura`
      );
    }

    // 🔍 LOG FINAL: Verificar que idInvoice está en el payload antes de enviar
    this.logger.debug('📤 [processInvoiceCreation] Payload final antes de enviar:', {
      idInvoice: invoiceDto.idInvoice || 'NO INCLUIDO',
      currentDraftId: this.currentDraftId || 'NO DEFINIDO',
      payloadKeys: Object.keys(invoiceDto),
    });
    this.logger.debug('📤 [processInvoiceCreation] Payload completo:', JSON.stringify(invoiceDto, null, 2));

    // 🔍 Diagnóstico de autenticación
    const token = localStorage.getItem(environment.TOKEN);
    const decodedToken = decodeToken();

    this.purchaseProofService.register(invoiceDto).subscribe({
      next: (response) => {
        this.isProcessingInvoice = false;
        this.cdRef.detectChanges();

        // 🔍 LOGS DE DIAGNÓSTICO

        // ✅ OBTENER idInvoice: Puede estar en response.data.idInvoice o response.idInvoice
        // También verificar código de éxito: puede ser 1, 200, o status 'OK'
        const invoiceId = response?.data?.idInvoice || response?.idInvoice;
        const isSuccess =
          response?.code === 1 ||
          response?.code === 200 ||
          response?.status === 'OK' ||
          (response?.code >= 200 && response?.code < 300);

        // ✅ VERIFICAR: Respuesta exitosa y tiene idInvoice
        if (isSuccess && invoiceId) {
          this.logger.debug(
            '✅ Condición cumplida: respuesta exitosa y tiene idInvoice',
            {
              invoiceId,
            }
          );

          // ✅ COMBINAR: InvoiceDto enviado + Datos de respuesta del backend
          // La respuesta puede tener idInvoice directamente o en data
          const responseData = response?.data || response;
          const completeInvoiceData = this.mergeInvoiceData(
            invoiceDto,
            responseData
          );

          // ✅ GUARDAR en sessionStorage con clave única
          const storageKey = `INVOICE_TICKET_${invoiceId}`;
          sessionStorage.setItem(
            storageKey,
            JSON.stringify(completeInvoiceData)
          );

          // ✅ VERIFICAR si la factura está autorizada para imprimir automáticamente
          const authorizationStatus = responseData.authorizationStatusSri || 
                                     responseData.authorizationSri?.authorizationStatusSri;
          const isAuthorized = authorizationStatus === 'AUTORIZADO' || 
                              authorizationStatus === 'AUTHORIZED' ||
                              (responseData.invoiceNumber && responseData.invoiceNumber !== '-');

          if (isAuthorized) {
            this.logger.debug('✅ Factura autorizada, iniciando impresión automática del recibo', {
              invoiceId,
              invoiceNumber: responseData.invoiceNumber,
              authorizationStatus,
            });

            // Esperar un breve momento para que el diálogo se muestre antes de imprimir
            setTimeout(() => {
              const printSuccess = this.printTicketAutomatically(invoiceId);
              if (!printSuccess) {
                this.logger.warn('⚠️ La ventana de impresión fue bloqueada por el navegador');
                this.toast.warning('Por favor, permita las ventanas emergentes para imprimir automáticamente');
              }
            }, 1000);
          } else {
            this.logger.debug('ℹ️ Factura no autorizada aún, no se imprimirá automáticamente', {
              invoiceId,
              authorizationStatus,
            });
          }

          // ✅ MOSTRAR DIALOG DE ÉXITO con opción de imprimir recibo
          const dialogData: SuccessInvoiceDialogData = {
            invoiceId: invoiceId,
            invoiceNumber: responseData.invoiceNumber,
            clientName: invoiceDto.clientDto?.name,
            total: invoiceDto.total,
          };

          const dialogRef = this.dialog.open(SuccessInvoiceDialogComponent, {
            width: '450px',
            disableClose: false, // Permitir cerrar con ESC o clic fuera
            data: dialogData,
          });

          // Limpiar el formulario cuando se cierre el diálogo
          dialogRef.afterClosed().subscribe(() => {
            this.logger.debug(
              '🔄 Diálogo de éxito cerrado, limpiando formulario'
            );
            this.resetInvoiceForm();
          });

          this.logger.debug('✅ Diálogo de éxito mostrado', {
            invoiceId,
            invoiceNumber: responseData.invoiceNumber,
          });
        } else {
          // Respuesta exitosa pero sin idInvoice (caso raro)

          this.toast.success('Factura guardada correctamente');
        }

        // Opcional: Limpiar el formulario o redirigir
        // this.resetForm();
        // this.router.navigate(['/invoices']);
      },
      error: (err: HttpErrorResponse) => {
        this.isProcessingInvoice = false;
        this.cdRef.detectChanges();

        this.logger.error('Error al registrar factura', err);

        let errorMessage = 'Error al registrar la factura';

        // 🔍 Diagnóstico específico para 400 - No hay suscripción activa
        if (
          err.status === 400 &&
          err.error?.message?.includes('suscripción activa')
        ) {
          errorMessage =
            'No se encontró una suscripción activa para la compañía. Por favor, contacte al administrador para activar su suscripción.';

          this.logger.error('❌ Error 400 - No hay suscripción activa:', {
            status: err.status,
            message: err.error?.message,
            url: err.url,
          });
        }
        // 🔍 Diagnóstico específico para 401
        else if (err.status === 401) {
          errorMessage =
            '🔒 No autorizado: Su sesión expiró o no tiene permisos para crear facturas';

          this.logger.error('❌ Error 401 - Detalles:', {
            status: err.status,
            statusText: err.statusText,
            url: err.url,
            errorBody: err.error,
            headers: err.headers
              .keys()
              .map((key) => `${key}: ${err.headers.get(key)}`),
          });

          // Sugerir relogin después de 3 segundos
          setTimeout(() => {
            if (confirm('Su sesión ha expirado. ¿Desea volver al login?')) {
              localStorage.clear();
              this.router.navigate(['/login']);
            }
          }, 2000);
        } else if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (err.message) {
          errorMessage = err.message;
        }

        // Toast de error
        this.toast.error(errorMessage);
      },
    });
  }

  /**
   * ✅ COMBINA InvoiceDto enviado con datos de respuesta del backend
   *
   * @param invoiceDto - El InvoiceDto completo que se envió al backend
   * @param responseData - Los datos adicionales que retorna el backend (idInvoice, invoiceNumber, accessKey, etc.)
   * @returns InvoiceDto completo combinado
   */
  private mergeInvoiceData(invoiceDto: any, responseData: any): InvoiceDto {
    // El invoiceDto ya tiene todos los datos: clientDto, invoiceDetails, payments, totals, etc.
    // responseData tiene los datos adicionales: idInvoice, invoiceNumber, accessKey, authorizationNumber, etc.

    return {
      ...invoiceDto, // Mantener todos los datos originales
      // Sobrescribir/agregar con datos de la respuesta
      idInvoice: responseData.idInvoice,
      invoiceNumber: responseData.invoiceNumber,
      accessKey: responseData.accessKey,
      authorizationNumber: responseData.authorizationNumber,
      authorizationDate: responseData.authorizationDate,
      authorizationSri: responseData.authorizationSri,
      environment: responseData.environment,
      status: responseData.status || 'AUTHORIZED',
      // Asegurar que los totales estén presentes (ya vienen en invoiceDto)
      totalAmount: invoiceDto.totalAmount,
      discount: invoiceDto.discount,
      tip: invoiceDto.tip,
      subtotalZero: invoiceDto.subtotalZero,
      subtotalIva: invoiceDto.subtotalIva,
      subtotalIce: invoiceDto.subtotalIce,
      subtotalDiscount: invoiceDto.subtotalDiscount,
      // Mantener todos los detalles y pagos del invoiceDto original
      invoiceDetails: invoiceDto.invoiceDetails || invoiceDto.details || [],
      details: invoiceDto.details || invoiceDto.invoiceDetails || [],
      // ✅ Mapear paymentMethods a payments (el backend puede retornar payments, pero enviamos paymentMethods)
      payments: this.mapPaymentMethodsToPayments(
        invoiceDto.payments || invoiceDto.paymentMethods || []
      ),
      clientDto: invoiceDto.clientDto,
      establishment: invoiceDto.establishment,
      companyDto: invoiceDto.companyDto,
    };
  }

  /**
   * ✅ Mapea paymentMethods a payments (formato PaymentDto)
   * Convierte el formato que se envía al backend al formato que espera el componente de impresión
   */
  private mapPaymentMethodsToPayments(paymentMethods: any[]): any[] {
    if (!paymentMethods || paymentMethods.length === 0) {
      return [];
    }

    return paymentMethods.map((pm: any) => ({
      idPaymentMethod: pm.idPaymentMethod,
      paymentType: pm.paymentMethodType || pm.paymentType,
      paymentMethod: pm.paymentMethodType || pm.paymentType,
      paymentMethodCode: pm.paymentMethodCode,
      value: pm.value,
      total: pm.value,
      term: pm.term,
      timeUnit: pm.timeUnit,
      time: pm.timeUnit || pm.time,
    }));
  }

  /**
   * ✅ IMPRIME la tirilla automáticamente
   * Solo se llama desde signAndSend() cuando la respuesta es exitosa
   * @returns true si la ventana se abrió exitosamente, false si fue bloqueada
   */
  private printTicketAutomatically(invoiceId: number): boolean {
    this.logger.debug('🖨️ Iniciando impresión automática', { invoiceId });

    // ✅ CORRECCIÓN: La ruta correcta es /invoices/ticket/:invoiceId (no /billing/ticket/:invoiceId)
    // porque el módulo de billing se carga bajo la ruta 'invoices' en app-routing.module.ts
    // La ruta dentro del módulo es 'ticket/:invoiceId', pero el prefijo es '/invoices'
    const relativeUrl = `/invoices/ticket/${invoiceId}`;
    // Construir URL completa usando window.location
    const fullUrl = `${window.location.origin}${relativeUrl}`;
    this.logger.debug('🖨️ URLs de impresión:', { relativeUrl, fullUrl });

    try {
      // Intentar abrir ventana nueva
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
        // Si window.open falla (bloqueador de popups)
        this.logger.warn(
          '⚠️ window.open falló (posible bloqueador de popups), intentando con URL completa...'
        );

        // Intentar una segunda vez con URL completa
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
          this.logger.error(
            '❌ No se pudo abrir ventana de impresión después de múltiples intentos'
          );
          this.toast.warning(
            'No se pudo abrir la ventana de impresión. Verifique el bloqueador de ventanas emergentes o abra manualmente: ' +
              fullUrl
          );
          return false;
        } else {
          this.logger.debug(
            '✅ Ventana de impresión abierta exitosamente (segundo intento)',
            {
              invoiceId,
            }
          );
          return true;
        }
      } else {
        this.logger.debug('✅ Ventana de impresión abierta exitosamente', {
          invoiceId,
          windowExists: !!printWindow,
        });
        return true;
      }
    } catch (error) {
      this.logger.error('❌ Error al abrir ventana de impresión', {
        invoiceId,
        error,
      });
      this.toast.error(
        `Error al abrir la ventana de impresión: ${error}. URL: ${fullUrl}`
      );
      return false;
    }
  }

  saveDraft(): void {
    // Validar que al menos tenga información básica
    if (!this.buyerForm.valid) {
      this.showValidationSnackbar(
        'Complete la información del comprador',
        'warning'
      );
      return;
    }

    // Validar que haya al menos un producto
    if (this.invoiceDetailDataSource.data.length === 0) {
      this.showValidationSnackbar(
        'Agregue al menos un producto a la factura',
        'warning'
      );
      return;
    }

    // 🔍 Restaurar currentDraftId desde localStorage si no está definido
    if (!this.currentDraftId) {
      const savedDraftId = localStorage.getItem('CURRENT_DRAFT_ID');
      if (savedDraftId) {
        this.currentDraftId = Number(savedDraftId);
        this.logger.debug(
          `💾 [saveDraft] currentDraftId restaurado desde localStorage: ${this.currentDraftId}`
        );
      }
    }

    // Activar estado de carga
    this.isSavingDraft = true;
    this.cdRef.detectChanges();

    // Construir el payload del borrador
    // Nota: Para borradores no necesitamos secuencial, firma ni autorización del SRI
    const invoiceDto = this.buildInvoicePayload('DRAFT');

    // El idInvoice ya está incluido en buildInvoicePayload si currentDraftId existe
    if (this.currentDraftId) {
      this.logger.debug(
        `📝 Actualizando borrador existente con ID: ${this.currentDraftId}`
      );
    } else {
      this.logger.debug('📝 Creando nuevo borrador');
    }

    // 🔍 LOGS DETALLADOS PARA DEBUGGING
    this.logger.debug('═══════════════════════════════════════════════');
    this.logger.debug('📤 PAYLOAD DEL BORRADOR:');
    this.logger.debug('═══════════════════════════════════════════════');
    this.logger.debug('🆔 idInvoice:', invoiceDto.idInvoice || 'null (nuevo)');
    this.logger.debug('🆔 idClient (raíz):', invoiceDto.idClient);
    this.logger.debug('👤 clientDto:', invoiceDto.clientDto);
    this.logger.debug('📦 Productos:', invoiceDto.invoiceDetails?.length || 0);
    this.logger.debug(
      '💳 Formas de pago:',
      invoiceDto.paymentMethods?.length || 0
    );
    this.logger.debug('═══════════════════════════════════════════════');
    this.logger.debug('📋 DATOS DEL FORMULARIO buyerForm:');
    this.logger.debug('═══════════════════════════════════════════════');
    this.logger.debug('idClient:', this.buyerForm.value.idClient);
    this.logger.debug('identification:', this.buyerForm.value.identification);
    this.logger.debug('businessName:', this.buyerForm.value.businessName);
    this.logger.debug('email:', this.buyerForm.value.email);
    this.logger.debug('═══════════════════════════════════════════════');

    this.logger.debug('Guardando borrador de factura', invoiceDto);

    this.draftInvoiceService.saveDraft(invoiceDto).subscribe({
      next: (response) => {
        this.isSavingDraft = false;
        this.cdRef.detectChanges();

        const action = this.currentDraftId ? 'actualizado' : 'guardado';
        this.toast.success(`Borrador ${action} exitosamente`);
        this.logger.debug(`Borrador ${action}`, response);

        // Si era un nuevo borrador, guardar el ID retornado
        if (!this.currentDraftId && response?.idInvoice) {
          this.currentDraftId = response.idInvoice;
          localStorage.setItem('CURRENT_DRAFT_ID', String(this.currentDraftId));
          this.logger.debug(
            `💾 Nuevo borrador creado con ID: ${this.currentDraftId} (guardado en localStorage)`
          );
        }

        // Opcional: Limpiar el formulario o redirigir a la lista de borradores
        // this.resetForm();
        // this.router.navigate(['/billing/drafts']);
      },
      error: (err: HttpErrorResponse) => {
        this.isSavingDraft = false;
        this.cdRef.detectChanges();

        this.logger.error('Error al guardar borrador', err);

        let errorMessage = 'Error al guardar el borrador';
        if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (err.message) {
          errorMessage = err.message;
        }

        this.toast.error(errorMessage);
      },
    });
  }

  /**
   * Muestra un snackbar de validación personalizado
   */
  private showValidationSnackbar(
    message: string,
    type: 'error' | 'warning' | 'info' | 'success' = 'warning'
  ): void {
    switch (type) {
      case 'success':
        this.toast.success(message);
        break;
      case 'warning':
        this.toast.warning(message);
        break;
      case 'info':
        this.toast.info(message);
        break;
      case 'error':
      default:
        this.toast.error(message);
        break;
    }
  }

  /**
   * Valida que todos los datos requeridos estén completos antes de guardar la factura
   */
  private validateInvoiceData(): boolean {
    // Validar formulario de cabecera
    if (!this.invoiceForm.valid) {
      this.showValidationSnackbar(
        'Complete la información de la cabecera de la factura',
        'warning'
      );
      return false;
    }

    // Validar formulario del comprador
    if (!this.buyerForm.valid) {
      this.showValidationSnackbar(
        'Complete la información del comprador',
        'warning'
      );
      return false;
    }

    // Validar que haya al menos un producto
    if (this.invoiceDetailDataSource.data.length === 0) {
      this.showValidationSnackbar(
        'Agregue al menos un producto a la factura',
        'warning'
      );
      return false;
    }

    // Validar que haya al menos una forma de pago
    if (this.paymentsDataSource.data.length === 0) {
      this.showValidationSnackbar(
        'Agregue al menos una forma de pago',
        'warning'
      );
      return false;
    }

    // Validar que todas las formas de pago tengan valores válidos
    for (let i = 0; i < this.paymentItems.length; i++) {
      const paymentControl = this.paymentItems.at(i);
      if (paymentControl.invalid) {
        this.showValidationSnackbar(
          'Complete todos los valores de las formas de pago correctamente',
          'warning'
        );
        return false;
      }
    }

    // Validar que la suma de formas de pago = total de la factura
    if (!this.validatePaymentTotal()) {
      return false;
    }

    return true;
  }

  /**
   * Construye el InvoiceDto según la estructura del backend
   *
   * IMPORTANTE: Este método construye el DTO con TODAS las formas de pago
   * La suma de los valores debe ser igual al total de la factura
   */
  private buildInvoicePayload(status: 'DRAFT' | 'AUTHORIZED'): any {
    const companyIdRaw = localStorage.getItem(environment.ID_COMPANY);
    const companyId = companyIdRaw ? Number(companyIdRaw) : null;

    // Validar que tenemos un idCompany válido
    if (!companyId || isNaN(companyId)) {
      this.logger.error(
        '[BillingComponent] No se pudo obtener idCompany válido',
        {
          companyIdRaw,
          companyId,
        }
      );
      throw new Error(
        'No se pudo obtener el ID de la compañía. Por favor, seleccione una compañía.'
      );
    }

    // ✅ Usar el valor del formulario primero, si no existe usar localStorage como fallback
    const establishmentId = Number(
      this.invoiceForm.value.branchOffice ||
        localStorage.getItem(environment.ID_ESTABLISHMENT)
    );
    const companyIdentification =
      localStorage.getItem(environment.IDENTIFICATION) || '';

    // Mapear TODAS las formas de pago
    const paymentMethods = this.paymentsDataSource.data.map((payment) => ({
      idPaymentMethod: payment.idPaymentMethod, // ID de la forma de pago (para la relación en BD)
      paymentMethodType: payment.paymentType,
      paymentMethodCode: payment.paymentMethodCode,
      value: Number(payment.value),
      term: payment.term || null,
      timeUnit: payment.time || null,
    }));

    // Obtener contactos de la compañía desde localStorage
    const companyContactsStr = localStorage.getItem('COMPANY_CONTACTS');
    let companyContacts: any[] = [];

    if (companyContactsStr) {
      try {
        companyContacts = JSON.parse(companyContactsStr);
      } catch (e) {
        this.logger.error('Error al parsear contactos de la compañía', e);
      }
    }

    // Si no hay contactos en localStorage, usar valores por defecto
    if (!companyContacts || companyContacts.length === 0) {
      companyContacts = [
        { contactType: 'MAI', value: 'info@empresa.com', status: true },
        { contactType: 'TEL', value: '0999999999', status: true },
        {
          contactType: 'DIR',
          value:
            this.branchOffices.find((b) => b.id === establishmentId)?.address ||
            'DIRECCION',
          status: true,
        },
      ];
    }

    // IMPORTANTE: Obtener valores del COMPRADOR (NO de la compañía)
    // El idType debe ser SIEMPRE del comprador, no de la compañía
    const buyerIdTypeValue = this.buyerForm.getRawValue().idType;
    const buyerIdentificationValue =
      this.buyerForm.getRawValue().identification;
    const buyerBusinessNameValue = this.buyerForm.getRawValue().businessName;
    const buyerAddressValue = this.buyerForm.getRawValue().address;
    const buyerPhoneValue = this.buyerForm.getRawValue().phone;
    const buyerEmailValue = this.buyerForm.getRawValue().email;
    const buyerIdClientValue = this.buyerForm.getRawValue().idClient;

    // Validar que tenemos el idType del comprador
    if (!buyerIdTypeValue) {
      this.logger.warn(
        '⚠️ [Billing] buyerIdType es null/undefined. Verificar que el formulario tenga un valor válido.'
      );
    }

    // Log para debugging - comparar con el idType de la compañía para detectar errores
    this.logger.debug('📤 [Billing] Valores del comprador que se enviarán:', {
      buyerIdType: buyerIdTypeValue,
      companyIdType: this.companyData?.idType, // Solo para comparación/debugging
      buyerIdentification: buyerIdentificationValue,
      buyerBusinessName: buyerBusinessNameValue,
    });

    // Construir InvoiceDto según el backend
    const invoiceDto: any = {
      // ✅ idCompany a nivel raíz (requerido por el backend para validación de suscripción)
      idCompany: companyId,

      // CompanyDto completo (requerido por el backend)
      companyDto: {
        idCompany: companyId,
        identification: companyIdentification, // RUC de la empresa (requerido para clave de acceso)
        name:
          this.companyData?.name ||
          localStorage.getItem(environment.NAME_COMPANY) ||
          'EMPRESA',
        nameCommercial: this.companyData?.nameCommercial || null,
        idType: this.companyData?.idType || null, // ✅ Este es el idType de la COMPAÑÍA (correcto aquí)
        forcedToAccounting: this.companyData?.forcedToAccounting || false,
        regime: this.companyData?.regime || null, // ✅ Régimen tributario (GENERAL, RIMPE_POPULAR, RIMPE_STARTUP, SPECIAL)
        specialTaxpayerResolution:
          this.companyData?.specialTaxpayerResolution || null, // ✅ Resolución del contribuyente especial (solo si regime es SPECIAL)
        status:
          this.companyData?.status !== undefined
            ? this.companyData.status
            : true,
        logoPath: this.companyData?.logoPath || null, // ✅ Ruta del logo de la compañía
        // Contactos de la empresa (requeridos por el SRI para el XML)
        contacts: companyContacts,
      },

      // ID del cliente (a nivel raíz para relación en BD)
      idClient: buyerIdClientValue || null,

      // Cliente (comprador)
      clientDto: {
        idClient: buyerIdClientValue || null, // ✅ ID del cliente si ya existe
        identification: buyerIdentificationValue,
        idType: buyerIdTypeValue, // ✅ SIEMPRE el idType del COMPRADOR, NO el de la compañía
        name: buyerBusinessNameValue,
        address: buyerAddressValue || 'N/A',
        phone: buyerPhoneValue || 'N/A',
        email: buyerEmailValue,
      },

      // Campos buyerXXX para el backend (usados para generar el XML del SRI)
      // NOTA: El backend debe generar el XML con el orden correcto según XSD del SRI:
      // - Si hay guiaRemision, debe ir ANTES de identificacionComprador
      // - razonSocialComprador debe ir DESPUÉS de identificacionComprador
      // - El orden esperado por el SRI: [guiaRemision?] → identificacionComprador → razonSocialComprador → ...
      buyerIdentification: buyerIdentificationValue || null,
      buyerIdType: buyerIdTypeValue || null, // ✅ SIEMPRE el idType del COMPRADOR, NO el de la compañía
      buyerName: buyerBusinessNameValue || null, // → razonSocialComprador en XML
      buyerAddress: buyerAddressValue || null,
      buyerPhone: buyerPhoneValue || null,
      buyerMail: buyerEmailValue || null,
      // guiaRemision: null (no se envía porque no se usa en facturas normales)

      // paymentMethod e idPaymentMethod eliminados - ya no se requieren por el backend
      // Array completo de formas de pago
      paymentMethods: paymentMethods,

      // Establecimiento con punto de emisión
      establishment: (() => {
        const selectedBranch = this.branchOffices.find(
          (b) => b.id === establishmentId
        );
        const code = selectedBranch?.code || '001';
        const address = selectedBranch?.address || 'DIRECCION MATRIZ';

        // Determinar el tipo: MATRIZ si el código es '001', SUCURSAL en caso contrario
        const type = code === '001' ? 'MATRIZ' : 'SUCURSAL';

        return {
          idEstablishment: establishmentId,
          name: this.companyData?.name || 'MATRIZ',
          code: code,
          type: type, // ✅ Agregado: tipo de establecimiento según código
          address: address,
          // Punto de emisión (requerido por el backend para generar el XML)
          issuancePoint: this.getIssuancePointFromSession(),
        };
      })(),

      // Fecha de emisión (usar fecha local sin conversión de zona horaria)
      issueDate: this.invoiceForm.value.issuedDate
        ? this.formatDateToISO(this.invoiceForm.value.issuedDate)
        : this.formatDateToISO(new Date()),

      // Ambiente del SRI según tabla del SRI:
      // "1" = PRUEBAS
      // "2" = PRODUCCIÓN
      environment: environment.production ? '2' : '1',

      // Totales calculados según nomenclatura del SRI
      // Nota: El backend genera el XML del SRI a partir de invoiceDetails, no de estos totales
      // Estos campos son para guardar en la BD y para logs
      totalAmount: this.getTotalInvoice(), // Total final de la factura
      discount:
        this.totals.find((t) => t.label === 'Total descuento')?.value || 0, // Total descuentos
      tip: this.getTipAmount(), // Propina

      // subtotalZero: Subtotal con tarifa 0% de IVA (productos sin IVA)
      subtotalZero: 0, // Por ahora todos los productos tienen IVA 15%

      // subtotalIva: BASE IMPONIBLE para IVA (subtotal DESPUÉS de descuentos + ICE)
      // Según regulaciones del SRI, el ICE forma parte de la base imponible del IVA
      subtotalIva: (() => {
        const subtotalConDescuento =
          this.totals.find((t) => t.label === 'Subtotal con descuento')
            ?.value || 0;
        const totalICE =
          this.totals.find((t) => t.label === 'Valor ICE')?.value || 0;
        // ✅ Usar Math.round() para redondeo matemático correcto a 2 decimales
        return Math.round((subtotalConDescuento + totalICE) * 100) / 100;
      })(),

      // subtotalIce: Valor total del ICE (impuesto)
      // Nota: Si los productos tienen ICE, el backend debe agregarlo en los impuestos de cada detalle
      subtotalIce: this.totals.find((t) => t.label === 'Valor ICE')?.value || 0,

      // subtotalDiscount: Total de descuentos (duplicado de discount)
      subtotalDiscount:
        this.totals.find((t) => t.label === 'Total descuento')?.value || 0,

      // Estado de la factura
      status: status,

      // Estado de pago: todas las facturas se marcan como pagadas automáticamente
      // (en el futuro se podrá implementar crédito comercial con plazos)
      paymentStatus: status === 'AUTHORIZED' ? 'PAID' : undefined, // Solo marcar como pagada si está autorizada
      amountPaid: status === 'AUTHORIZED' ? this.getTotalInvoice() : undefined, // Monto pagado = total de la factura

      // Información adicional
      additionalInformation: this.invoiceForm.getRawValue().legalName || '',

      // Detalles de productos (InvoiceDetailDto[])
      invoiceDetails: this.detailsForm.value.items.map(
        (item: any, index: number) => {
          const quantity = Number(item.quantity) || 0;
          const unitPrice = Number(item.unitPrice) || 0;
          const discountPercent = Number(item.discount) || 0;
          const icePercent = Number(item.ice) || 0; // ✅ Obtener porcentaje de ICE

          // Calcular descuento monetario
          const discountAmount = (unitPrice * quantity * discountPercent) / 100;

          // Calcular subtotal (después del descuento)
          const subtotal = unitPrice * quantity - discountAmount;

          // Obtener el producto original para los datos fiscales
          const originalProduct = this.invoiceDetailDataSource.data[index];
          const ivaPercent = Number(item.iva) || 0; // ✅ Usar IVA del producto, no el global
          const productData = originalProduct?.product || {};

          // Construir el productDto con todos los campos necesarios
          const productDto: any = {
            idProduct:
              originalProduct?.idProduct ||
              originalProduct?.product?.idProduct ||
              null, // ✅ ID real de la BD
            mainCode: item.code || productData.mainCode || `PROD-${index}`, // ✅ Código alfanumérico
            auxiliarCode: productData.auxiliarCode || null,
            productType: productData.productType || 1,
            name: item.description || '',
            description: item.description || '',
            unitPrice: unitPrice,
            discount: 0,
            status: true,
            idCompany: companyId,
            // Campos requeridos por el backend para generar XML del SRI - IVA
            rate: ivaPercent, // Porcentaje de IVA (ej: 15)
            taxCode: '2', // Código de impuesto: 2 = IVA
            taxCodPercentage: this.getTaxCodePercentage(ivaPercent), // Código del porcentaje según tabla SRI
          };

          // ✅ Si el producto tiene ICE, agregar campos adicionales para ICE
          if (icePercent > 0) {
            productDto.iceRate = icePercent; // Porcentaje de ICE
            productDto.iceTaxCode = '3'; // Código de impuesto: 3 = ICE
            productDto.iceTaxCodPercentage =
              this.getICETaxCodePercentage(icePercent); // Código del porcentaje de ICE según SRI
          }

          const detail = {
            idProduct: Number(item.code),
            productDto: productDto,
            unitPrice: unitPrice,
            discount: discountAmount, // Backend espera el valor monetario del descuento
            subtotal: subtotal, // Subtotal después de descuento (base imponible)
            quantity: quantity,
            ivaPercentage: ivaPercent, // ✅ Agregar porcentaje de IVA para el componente de impresión
          };

          return detail;
        }
      ),

      // Password de la firma digital (si está configurado)
      // password: '', // Este campo se puede obtener de alguna configuración
    };

    // ✅ IMPORTANTE: Incluir idInvoice SIEMPRE que exista currentDraftId (después de construir el objeto base)
    // Esto asegura que se incluya incluso si el spread operator no funcionó
    if (this.currentDraftId) {
      invoiceDto.idInvoice = this.currentDraftId;
      this.logger.debug(
        `✅ [buildInvoicePayload] idInvoice agregado: ${this.currentDraftId} (status: ${status})`
      );
    }

    // ✅ IMPORTANTE: Para borradores, asegurarse de que NO se envíen campos de autorización
    // El backend no debe intentar acceder a authorizationSri en borradores
    if (status === 'DRAFT') {
      // Eliminar explícitamente cualquier campo relacionado con autorización
      delete (invoiceDto as any).authorizationSri;
      delete (invoiceDto as any).accessKey;
      delete (invoiceDto as any).authorizationNumber;
      delete (invoiceDto as any).authorizationDate;
      this.logger.debug('[Billing] Borrador: Campos de autorización eliminados del payload');
    }

    // 🔍 LOG: Verificar que idInvoice se incluyó correctamente
    this.logger.debug('📤 [buildInvoicePayload] Payload construido:', {
      idInvoice: invoiceDto.idInvoice || 'NO INCLUIDO',
      currentDraftId: this.currentDraftId || 'NO DEFINIDO',
      status: status,
    });

    this.logger.debug('InvoiceDto construido', invoiceDto);

    this.logger.debug('InvoiceDto construido:', {
      company: invoiceDto.companyDto?.name,
      client: invoiceDto.clientDto?.name,
      products: invoiceDto.invoiceDetails?.length || 0,
      total: invoiceDto.totalAmount,
      status: invoiceDto.status,
    });

    return invoiceDto;
  }

  /**
   * Formatea una fecha a formato YYYY-MM-DD usando fecha local (sin conversión de zona horaria)
   * Esto evita que la fecha cambie de día debido a la conversión UTC
   */
  private formatDateToISO(dateValue: string | Date): string {
    if (typeof dateValue === 'string') {
      // Si ya está en formato YYYY-MM-DD, retornarlo tal cual
      if (dateValue.match(/^\d{4}-\d{2}-\d{2}$/)) {
        return dateValue;
      }
      // Si tiene hora, extraer solo la fecha
      if (dateValue.includes('T')) {
        return dateValue.split('T')[0];
      }
      // Parsear como fecha local
      const date = new Date(dateValue);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }

    // Si es un objeto Date, obtener componentes de fecha local (sin conversión de zona horaria)
    const year = dateValue.getFullYear();
    const month = String(dateValue.getMonth() + 1).padStart(2, '0');
    const day = String(dateValue.getDate()).padStart(2, '0');

    // Retornar solo la fecha en formato YYYY-MM-DD
    return `${year}-${month}-${day}`;
  }

  /**
   * Obtiene el punto de emisión desde localStorage
   * El punto de emisión se guarda al hacer login/seleccionar compañía
   *
   * IMPORTANTE: El backend REQUIERE el idIssuancePoint para generar el secuencial
   */
  private getIssuancePointFromSession(): any {
    this.logger.debug('[BillingComponent] Obteniendo punto de emisión...');
    const formEmissionPointId = this.invoiceForm.getRawValue().emissionPoint;
    this.logger.debug('[BillingComponent] Formulario emissionPoint:', { formEmissionPointId });

    if (formEmissionPointId) {
      // Buscar el punto de emisión en la lista cargada para obtener el código
      const selectedPoint = this.emissionPoints.find(
        (p) => p.id === formEmissionPointId
      );
      if (selectedPoint) {
        this.logger.debug(
          `✅ Usando punto de emisión del formulario: ${selectedPoint.id} - ${selectedPoint.code}`
        );
        return {
          idIssuancePoint: selectedPoint.id, // ✅ Nombre para el backend
          code: selectedPoint.code,
        };
      }
    }

    // ✅ Prioridad 2: Usar INVOICE_POINT_DATA de localStorage
    const invoicePointDataStr = localStorage.getItem('INVOICE_POINT_DATA');
    const invoicePointId = localStorage.getItem(environment.ID_INVOICE_POINT);

    if (invoicePointDataStr) {
      try {
        const invoicePointData = JSON.parse(invoicePointDataStr);
        const pointId = invoicePointData.id || Number(invoicePointId);
        const pointCode = invoicePointData.code;
        
        if (!pointCode) {
          this.logger.warn('⚠️ Punto de emisión sin código en INVOICE_POINT_DATA:', invoicePointData);
        }

        if (pointId) {
          this.logger.debug(
            `✅ Usando punto de emisión de localStorage: ${pointId} - ${pointCode}`
          );
          return {
            idIssuancePoint: pointId, // ✅ Nombre para el backend
            code: pointCode,
          };
        }
      } catch (e) {
        this.logger.error('Error al parsear datos del punto de emisión', e);
      }
    }

    // ✅ Prioridad 3: Usar solo ID_INVOICE_POINT si está disponible (caso raro)
    if (invoicePointId) {
      this.logger.error(
        '❌ INVOICE_POINT_DATA no encontrado, solo hay ID_INVOICE_POINT. El punto debería haberse guardado durante el login.',
        { invoicePointId }
      );
      // Sin el código, el backend puede tener problemas. Esto no debería pasar en el flujo normal.
      return {
        idIssuancePoint: Number(invoicePointId), // ✅ Nombre para el backend
        code: null, // No hardcode, el backend debería manejar esto
      };
    }

    // Error crítico: no hay datos del punto de emisión
    this.logger.error(
      '❌ ERROR CRÍTICO: No hay datos del punto de emisión. El punto debería haberse guardado durante el login.'
    );
    this.logger.error(
      '❌ El backend NO podrá generar el secuencial sin el punto de emisión.'
    );
    // No retornar valores hardcodeados, dejar que el backend maneje el error
    return {
      idIssuancePoint: null,
      code: null,
    };
  }

  /**
   * Mapea el porcentaje de IVA al código que espera el SRI
   * Según la tabla de códigos del SRI para IVA:
   * - 0% → '0'
   * - 15% → '4' (IVA 15%)
   * - 12% → '2' (IVA 12%)
   * - No objeto de impuesto → '6'
   * - Exento → '7'
   */
  private getTaxCodePercentage(ivaPercent: number): string {
    if (ivaPercent === 0) return '0'; // IVA 0%
    if (ivaPercent === 12) return '2'; // IVA 12%
    if (ivaPercent === 15) return '4'; // IVA 15%
    if (ivaPercent === 14) return '3'; // IVA 14%
    if (ivaPercent === 13) return '10'; // IVA 13%
    if (ivaPercent === 5) return '5'; // IVA 5%

    // Por defecto, retornar código para 15%
    this.logger.warn(
      `Porcentaje de IVA no reconocido: ${ivaPercent}%, usando código para 15%`
    );
    return '4';
  }

  /**
   * Mapea el porcentaje de ICE al código de porcentaje según tabla del SRI
   * Los códigos varían según el tipo de producto y la tarifa vigente
   * @param icePercent Porcentaje de ICE (0-100)
   * @returns Código del porcentaje según SRI
   */
  private getICETaxCodePercentage(icePercent: number): string {
    // Códigos comunes de ICE según el SRI
    // Nota: Estos códigos pueden variar según regulaciones del SRI
    const iceCodeMap: { [key: number]: string } = {
      5: '3072', // ICE 5%
      10: '3073', // ICE 10%
      15: '3074', // ICE 15%
      20: '3075', // ICE 20%
      25: '3077', // ICE 25%
      30: '3078', // ICE 30%
      35: '3079', // ICE 35%
      40: '3080', // ICE 40%
      50: '3081', // ICE 50%
      60: '3082', // ICE 60%
      75: '3083', // ICE 75%
      100: '3084', // ICE 100%
      150: '3085', // ICE 150%
      300: '3086', // ICE 300%
    };

    if (iceCodeMap[icePercent]) {
      return iceCodeMap[icePercent];
    }

    // Si no encuentra el código exacto, loguear advertencia y devolver el porcentaje como string
    this.logger.warn(
      `⚠️ Porcentaje de ICE no reconocido: ${icePercent}%. Usando porcentaje como código.`
    );

    // Devolver el porcentaje como string para que el backend lo procese
    return icePercent.toString();
  }

  private loadCompanyData(
    companyId: number,
    establishmentId: number | null,
    invoicePointId: number | null
  ): void {
    const currentUser = decodeToken()?.userName;

    this.companyService.findByIds([companyId]).subscribe({
      next: (response) => {
        const companies = response.companyDetailsDtos || [];

        if (companies.length === 0) {
          this.logger.error('No se encontró la empresa');
          return;
        }

        const company = companies[0];

        // ✅ Guardar datos completos de la empresa
        this.companyData = company;

        if (company?.establishments && company.establishments.length > 0) {
          this.branchOffices = company.establishments.map((est: any) => ({
            id: est.idEstablishment,
            code: est.code,
            address: est.address,
          }));

          // ✅ Priorizar el establishmentId que viene del parámetro (del login)
          // Si no existe o no está en la lista, usar el primero disponible
          let selectedEstablishmentId: number | null = null;
          
          if (establishmentId) {
            // Verificar que el establecimiento seleccionado existe en la lista
            const establishmentExists = this.branchOffices.some(
              (b) => b.id === establishmentId
            );
            if (establishmentExists) {
              selectedEstablishmentId = establishmentId;
              this.logger.debug(
                `✅ Establecimiento seleccionado durante login encontrado: ${establishmentId}`
              );
            } else {
              this.logger.warn(
                `⚠️ Establecimiento ${establishmentId} no existe en la lista, usando el primero disponible`
              );
              selectedEstablishmentId = this.branchOffices[0]?.id || null;
            }
          } else {
            // Si no hay establishmentId, usar el primero disponible
            selectedEstablishmentId = this.branchOffices[0]?.id || null;
            this.logger.debug(
              `✅ No hay establecimiento seleccionado, usando el primero disponible: ${selectedEstablishmentId}`
            );
          }

          // Guardar en localStorage si no estaba guardado o si cambió
          if (selectedEstablishmentId) {
            const currentSavedId = localStorage.getItem(environment.ID_ESTABLISHMENT);
            if (currentSavedId !== selectedEstablishmentId.toString()) {
              localStorage.setItem(
                environment.ID_ESTABLISHMENT,
                selectedEstablishmentId.toString()
              );
              this.logger.debug(
                `✅ ID_ESTABLISHMENT guardado/actualizado: ${selectedEstablishmentId}`
              );
            }
          }

          // Establecer el valor en el formulario después de que branchOffices esté disponible
          this.invoiceForm.patchValue({
            branchOffice: selectedEstablishmentId,
          });

          // ✅ Deshabilitar el campo para que sea de solo lectura (viene precargado del login)
          this.invoiceForm.get('branchOffice')?.disable();

          // Forzar detección de cambios para actualizar la vista
          this.cdRef.detectChanges();

          this.logger.debug('Empresa y establecimientos cargados', {
            companyName: company.name,
            totalEstablishments: this.branchOffices.length,
            selectedEstablishmentId: selectedEstablishmentId,
            establishmentIdFromParam: establishmentId,
          });

          // Cargar punto de emisión desde localStorage
          // El punto ya fue seleccionado al hacer login
          const invoicePointDataStr = localStorage.getItem('INVOICE_POINT_DATA');

          if (invoicePointDataStr) {
            try {
              const invoicePointData = JSON.parse(invoicePointDataStr);

              // Setear el único punto disponible (el que ya se seleccionó)
              this.emissionPoints = [
                {
                  id: invoicePointData.id,
                  code: invoicePointData.code,
                },
              ];

              // Pre-seleccionarlo en el formulario
              this.invoiceForm.patchValue({
                emissionPoint: invoicePointData.id,
              });

              // Deshabilitar el campo para que sea readonly
              this.invoiceForm.get('emissionPoint')?.disable();
            } catch (error) {
              this.logger.error('Error al parsear INVOICE_POINT_DATA', error);
            }
          } else {
            this.logger.error('No hay INVOICE_POINT_DATA en localStorage. El punto de emisión debería haberse guardado durante el login.');
          }

          // ✅ Forzar detección de cambios para actualizar la vista (OnPush)
          this.cdRef.detectChanges();
        } else {
          this.logger.error('No hay establecimientos en la empresa');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('Error al cargar empresa', err);
      },
    });

    // ✅ Cargar punto de emisión desde localStorage como fallback
    this.loadEmissionPointFromSession();
  }

  /**
   * Carga el punto de emisión desde localStorage (método auxiliar)
   * Se usa como fallback si no se cargó en loadCompanyData
   */
  private loadEmissionPointFromSession(): void {
    const invoicePointDataStr = localStorage.getItem('INVOICE_POINT_DATA');

    if (invoicePointDataStr) {
      try {
        const invoicePointData = JSON.parse(invoicePointDataStr);

        // Solo agregar si no existe ya en la lista
        const exists = this.emissionPoints.some(
          (p) => p.id === invoicePointData.id
        );
        
        if (!exists) {
          this.emissionPoints = [
            {
              id: invoicePointData.id,
              code: invoicePointData.code,
            },
          ];
        }

        // Pre-seleccionarlo en el formulario
        this.invoiceForm.patchValue({
          emissionPoint: invoicePointData.id,
        });

        // Deshabilitar el campo para que sea readonly
        this.invoiceForm.get('emissionPoint')?.disable();
        this.cdRef.detectChanges();
      } catch (error) {
        this.logger.error('Error al parsear punto de emisión', error);
      }
    }
  }

  isDataRow = (_: number, row: any) => row != null;

  findClient() {
    const clientRequest: ClientRequestDto = {
      identification: this.buyerForm.get('identification')?.value ?? '',
      name: this.buyerForm.get('name')?.value ?? '',
      clientType: this.clientType ?? 0,
      status: this.status ?? true,
      companyIdentification:
        localStorage.getItem(environment.IDENTIFICATION) ?? '',
    };


    this.clientService.searchPageable(0, 10, clientRequest).subscribe({
      next: (data) => {
        this.isSearchingBuyer = false; // ✅ Desactivar estado de carga
        this.cdRef.detectChanges();

        let clientList = data.content as Client[];

        if (clientList.length === 0) {
          // Limpiar datos anteriores del formulario cuando no se encuentra el cliente
          this.clearBuyerFormData();

          // Mostrar toast informativo
          this.toast.info(
            'Cliente no encontrado. Puede crear uno nuevo desde el botón de agregar.'
          );

          this.logger.debug(
            'Cliente no encontrado, limpiando formulario y mostrando opción para crear'
          );
          return;
        }

        this.addContactInformation(clientList);
        const client = clientList[0];

        if (client.contacts && client.contacts.length > 0) {
          // Forzar detección de cambios antes de enfocar el input
          this.cdRef.detectChanges();
          this.buyerForm.patchValue({
            idClient: client.idClient, // ✅ Guardar el ID del cliente
            identification: client.identification,
            idType: client.idType,
            businessName: client.name,
            address: client.address,
            phone: client.phone,
            email: client.mail,
            clientType: client.clientType,
          });

          // Deshabilitar campos después de cargar cliente (excepto identificación para poder buscar otro)
          this.buyerForm.get('idType')?.disable();
          this.buyerForm.get('businessName')?.disable();
          this.buyerForm.get('address')?.disable();
          this.buyerForm.get('phone')?.disable();
          this.buyerForm.get('email')?.disable();

          this.toast.success('Cliente encontrado y cargado');

          this.logger.debug('Cliente cargado exitosamente', {
            idClient: client.idClient,
            identification: client.identification,
            name: client.name,
          });

          // Forzar detección de cambios antes de enfocar (necesario con OnPush)
          this.cdRef.detectChanges();

          // ✅ Enfocar automáticamente el campo de búsqueda de productos
          this.focusProductSearch();
        } else {
          this.toast.warning(
            'Cliente encontrado pero sin información de contacto'
          );
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isSearchingBuyer = false; // ✅ Desactivar estado de carga en error
        this.cdRef.detectChanges();
        this.toast.error('Error al buscar cliente. Intente nuevamente.');
        this.logger.error('Error al buscar cliente', err);
      },
    });
  }

  addContactInformation(clientList: Client[]) {
    clientList.forEach((client) => {
      client.contacts.forEach((contact) => {
        if (contact.contactType === CONTACT_TYPE_ADRESS.code) {
          client.address = contact.value;
        }
        if (contact.contactType === CONTACT_TYPE_MAIL.code) {
          client.mail = contact.value;
        }
        if (contact.contactType === CONTACT_TYPE_MOBIL.code) {
          client.mobil = contact.value;
        }
        if (contact.contactType === CONTACT_TYPE_PHONE.code) {
          client.phone = contact.value;
        }
        this.identificationTypes.forEach((type) => {
          if (type.code === client.idType) {
            client.idTypeEnum = type;
          }
        });
      });
    });
  }

  /**
   * Carga los contactos de un cliente por su ID
   * Se usa al editar borradores para cargar teléfono, móvil, dirección, email
   */
  private loadClientContactsByIdClient(idClient: number): void {
    this.logger.debug(`📞 Buscando contactos del cliente ID: ${idClient}`);

    // Buscar el cliente por ID para obtener sus contactos
    this.clientService.findById(idClient).subscribe({
      next: (response: ClientResponse) => {
        if (response && response.clientDtos && response.clientDtos.length > 0) {
          // Procesar contactos
          this.addContactInformation(response.clientDtos);
          const client = response.clientDtos[0];

          this.logger.debug('✅ Contactos del cliente cargados:', {
            phone: client.phone,
            mobil: client.mobil,
            address: client.address,
            mail: client.mail,
          });

          // Actualizar solo los campos de contacto (sin sobrescribir identificación y nombre)
          const currentValues = this.buyerForm.value;
          this.buyerForm.patchValue({
            address: client.address || currentValues.address || '',
            phone: client.phone || currentValues.phone || '',
            email: client.mail || currentValues.email || '',
          });

          // Deshabilitar campos después de cargar contactos del cliente
          this.buyerForm.get('idType')?.disable();
          this.buyerForm.get('businessName')?.disable();
          this.buyerForm.get('address')?.disable();
          this.buyerForm.get('phone')?.disable();
          this.buyerForm.get('email')?.disable();

          this.logger.debug(
            '✅ buyerForm actualizado con contactos:',
            this.buyerForm.value
          );
        } else {
          this.logger.warn(`⚠️ No se encontró cliente con ID: ${idClient}`);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('❌ Error al cargar contactos del cliente', err);
      },
    });
  }

  /**
   * ✅ Verifica si ya existe un cliente Consumidor Final activo para la compañía
   * Retorna también el email de la compañía para pre-llenar el campo
   */
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
        .subscribe((data) => {
          const clients = (data?.content || []) as Client[];
          const hasConsumidorFinal =
            clients.some(
              (c) => c.idType === ID_TYPE_CONSUMIDOR_FINAL && c.status === true
            ) ?? false;

          const companyEmail = this.companyData?.email || undefined;

          this.logger.debug('Verificación Consumidor Final:', {
            hasConsumidorFinal,
            companyEmail,
          });

          resolve({ hasConsumidorFinal, companyEmail });
        });
    });
  }

  /**
   * Abre el modal para crear un nuevo cliente desde la pantalla de factura
   */
  openCreateClientDialog(): void {
    // Obtener valores actuales del formulario
    const currentIdentification =
      this.buyerForm.get('identification')?.value ?? '';
    const currentIdType = this.buyerForm.get('idType')?.value;

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
            // ✅ Nuevos campos para pre-llenar desde factura
            initialIdentification: currentIdentification,
            initialIdType: currentIdType,
          } as ClientDialogData & {
            initialIdentification?: string;
            initialIdType?: number;
          },
          panelClass: 'custom-dialog-container',
          autoFocus: false,
        });

        dialogRef.afterClosed().subscribe((result: Client | null) => {
          if (result && result.idClient) {
            // Cliente creado exitosamente, cargar datos en el formulario
            this.loadClientIntoForm(result);
          }
        });
      }
    );
  }

  /**
   * Limpia completamente el formulario de factura para crear una nueva
   * Se llama después de cerrar el diálogo de éxito
   */
  private resetInvoiceForm(): void {
    this.logger.debug('🧹 Limpiando formulario de factura completo');

    // 1. Limpiar formulario de emisión (mantener datos de la empresa)
    const companyName = localStorage.getItem(environment.NAME_COMPANY);
    const savedCompanyId = localStorage.getItem(environment.ID_COMPANY);
    const savedEstablishmentId = localStorage.getItem(
      environment.ID_ESTABLISHMENT
    );
    const savedInvoicePointId = localStorage.getItem(
      environment.ID_INVOICE_POINT
    );

    this.invoiceForm.patchValue({
      branchOffice: savedEstablishmentId ? Number(savedEstablishmentId) : null,
      legalName: companyName || '',
      issuedDate: new Date(),
      emissionPoint: savedInvoicePointId ? Number(savedInvoicePointId) : null,
      tip10: false,
    });

    // 2. Limpiar formulario de adquiriente
    this.buyerForm.reset();
    this.buyerForm.patchValue({
      identification: '',
      idType: null,
      businessName: '',
      address: '',
      phone: '',
      email: '',
      clientType: null,
    });
    // Habilitar todos los campos
    this.buyerForm.get('idType')?.enable();
    this.buyerForm.get('businessName')?.enable();
    this.buyerForm.get('address')?.enable();
    this.buyerForm.get('phone')?.enable();
    this.buyerForm.get('email')?.enable();

    // 3. Limpiar productos (detalles)
    while (this.items.length > 0) {
      this.items.removeAt(0);
    }
    this.invoiceDetailDataSource.data = [];

    // 4. Limpiar formas de pago
    while (this.paymentItems.length > 0) {
      this.paymentItems.removeAt(0);
    }
    this.paymentsDataSource.data = [];

    // 5. Resetear IVA global
    this.globalIvaPercentage.setValue(15);

    // 6. Resetear totales
    this.calculateTotals();

    // 7. Limpiar ID de borrador actual
    this.currentDraftId = null;
    localStorage.removeItem('CURRENT_DRAFT_ID');

    // 8. Limpiar control de búsqueda de productos
    this.productCtrl.setValue('');

    // 9. Forzar detección de cambios
    this.cdRef.detectChanges();

    this.logger.debug('✅ Formulario de factura limpiado exitosamente');
  }

  /**
   * Limpia los datos del formulario de Adquiriente (excepto identificación)
   */
  private clearBuyerFormData(): void {
    // Guardar la identificación actual para mantenerla en el campo
    const currentIdentification = this.buyerForm.get('identification')?.value;

    // Limpiar todos los campos excepto identificación
    this.buyerForm.patchValue({
      idClient: null,
      idType: null,
      businessName: '',
      address: '',
      phone: '',
      email: '',
    });

    // Restaurar la identificación
    if (currentIdentification) {
      this.buyerForm.patchValue({ identification: currentIdentification });
    }

    // Habilitar todos los campos para que puedan ser editados
    this.buyerForm.get('idType')?.enable();
    this.buyerForm.get('businessName')?.enable();
    this.buyerForm.get('address')?.enable();
    this.buyerForm.get('phone')?.enable();
    this.buyerForm.get('email')?.enable();

    this.cdRef.markForCheck();
  }

  /**
   * Valida los permisos del rol del usuario consultando al backend dinámicamente
   * Muestra en consola qué features tiene habilitadas según su rol
   */
  private validateUserRolePermissions(): void {
    // Usar roles de la compañía actual desde el servicio centralizado
    const roles = this.userRoleService.getCurrentRolesSync();
    if (!roles || roles.length === 0) {
      this.logger.warn('[BillingComponent] No se pudo obtener roles de la compañía actual');
      // Intentar cargar roles de forma asíncrona
      this.userRoleService.getCurrentCompanyRoles().subscribe({
        next: (loadedRoles) => {
          if (loadedRoles && loadedRoles.length > 0) {
            this.validateRoles(loadedRoles);
          }
        },
        error: (err) => {
          this.logger.error('[BillingComponent] Error al cargar roles:', err);
        },
      });
      return;
    }

    this.validateRoles(roles);
  }

  private validateRoles(roles: string[]): void {
    // Mapear nombres de roles a IDs según la documentación
    const roleIdMap: { [key: string]: number } = {
      super_admin: 1,
      member: 2,
      admin: 3,
    };

    // Obtener el primer rol del usuario (normalmente solo tiene uno)
    const userRole = roles[0];
    const roleId = roleIdMap[userRole];

    if (!roleId) {
      this.logger.warn('[BillingComponent] Rol no reconocido:', userRole);
      return;
    }

    // Consultar al backend para obtener los permisos del rol
    this.permissionAdminService.getRolePermissions(roleId).subscribe({
      next: (permissions) => {
        // Asegurar que permissions sea un array
        const permissionsArray = Array.isArray(permissions) ? permissions : [];

        this.logger.debug(`Validación de permisos para rol: ${userRole} (id=${roleId})`, {
          totalFeatures: permissionsArray.length,
          permissions: permissionsArray,
        });

        // Log también en el servicio de logger
        this.logger.debug('Permisos del rol validados', {
          role: userRole,
          roleId: roleId,
          totalFeatures: permissionsArray.length,
          features: permissionsArray.map((p) => p.featureCode),
        });
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('Error al validar permisos del rol:', err);
        this.logger.error('Error al validar permisos del rol', err);
      },
    });
  }

  /**
   * Abre el modal para crear un nuevo producto desde la pantalla de factura
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
          // Producto creado exitosamente, buscarlo y agregarlo a la factura
          const companyId = Number(
            localStorage.getItem(environment.ID_COMPANY)
          );
          this.productService
            .searchActiveProductsForBilling(result.mainCode, companyId)
            .subscribe({
              next: (products: ProductDto[]) => {
                if (products.length > 0) {
                  // Agregar el producto directamente a la factura
                  this.addProductsToInvoice(products);
                  this.productCtrl.setValue(''); // Limpiar el campo de búsqueda
                  this.toast.success('Producto creado y agregado exitosamente');
                  this.logger.debug(
                    'Producto creado y agregado a la factura:',
                    result.mainCode
                  );
                } else {
                  this.toast.success('Producto creado exitosamente');
                  this.logger.warn(
                    'Producto creado pero no se pudo encontrar para agregarlo'
                  );
                }
              },
              error: (err: HttpErrorResponse) => {
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

  /**
   * Carga los datos del cliente creado en el formulario de factura
   */
  private loadClientIntoForm(client: Client): void {
    // Cargar información de contacto si está disponible
    this.addContactInformation([client]);

    // Actualizar formulario con datos del cliente
    this.buyerForm.patchValue({
      idClient: client.idClient,
      identification: client.identification,
      idType: client.idType,
      businessName: client.name,
      address: client.address,
      phone: client.phone,
      email: client.mail,
      clientType: client.clientType,
    });

    // Deshabilitar campos después de cargar
    this.buyerForm.get('idType')?.disable();
    this.buyerForm.get('businessName')?.disable();
    this.buyerForm.get('address')?.disable();
    this.buyerForm.get('phone')?.disable();
    this.buyerForm.get('email')?.disable();

    this.toast.success('Cliente creado y cargado exitosamente');

    this.logger.debug('Cliente cargado en formulario de factura:', {
      idClient: client.idClient,
      identification: client.identification,
      name: client.name,
    });

    // Forzar detección de cambios antes de enfocar (necesario con OnPush)
    this.cdRef.detectChanges();

    // ✅ Enfocar automáticamente el campo de búsqueda de productos
    this.focusProductSearch();
  }

  /**
   * Carga un borrador para continuar editándolo
   * Se llama cuando el usuario hace clic en "Editar" desde la lista de borradores
   */
  loadDraftForEditing(draftId: number): void {
    this.logger.debug('📝 Cargando borrador con ID:', draftId);

    // Guardar el ID del borrador que estamos editando (en memoria Y localStorage)
    // Usar el idInvoice del borrador si está disponible, de lo contrario usar el draftId del parámetro
    this.currentDraftId = draftId;
    localStorage.setItem('CURRENT_DRAFT_ID', draftId.toString());
    this.logger.debug(
      `💾 currentDraftId establecido a: ${this.currentDraftId} (guardado en localStorage)`
    );

    // Verificar que el token existe antes de intentar cargar
    const token = localStorage.getItem(environment.TOKEN);
    if (!token) {
      this.logger.error('❌ No hay token JWT para cargar el borrador');
      this.toast.error('Sesión expirada. Por favor inicie sesión nuevamente.');
      this.router.navigate(['/login']);
      return;
    }

    this.draftInvoiceService.getDraftById(draftId).subscribe({
      next: (draft: InvoiceDto) => {
        this.logger.debug('✅ Borrador cargado desde backend:', draft);

        // 🔍 ACTUALIZAR currentDraftId con el idInvoice del borrador si está disponible
        if (draft.idInvoice && draft.idInvoice !== this.currentDraftId) {
          this.currentDraftId = draft.idInvoice;
          localStorage.setItem('CURRENT_DRAFT_ID', draft.idInvoice.toString());
          this.logger.debug(
            `💾 currentDraftId actualizado con idInvoice del borrador: ${this.currentDraftId}`
          );
        }

        // 🔍 LOGS DETALLADOS PARA DEBUGGING DE CLIENTE
        this.logger.debug('═══════════════════════════════════════════════');
        this.logger.debug('🔍 DATOS DEL CLIENTE EN EL BORRADOR:');
        this.logger.debug('═══════════════════════════════════════════════');
        this.logger.debug('draft.idInvoice:', draft.idInvoice);
        this.logger.debug('currentDraftId:', this.currentDraftId);
        this.logger.debug('draft.idClient:', draft.idClient);
        this.logger.debug(
          'draft.buyerIdentification:',
          draft.buyerIdentification
        );
        this.logger.debug('draft.buyerIdType:', draft.buyerIdType);
        this.logger.debug('draft.buyerName:', draft.buyerName);
        this.logger.debug('draft.buyerAddress:', draft.buyerAddress);
        this.logger.debug('draft.buyerPhone:', draft.buyerPhone);
        this.logger.debug('draft.buyerMail:', draft.buyerMail);
        this.logger.debug('draft.clientDto:', draft.clientDto);
        this.logger.debug('═══════════════════════════════════════════════');

        // 1. Cargar datos del comprador
        // Intentar primero con buyerXXX, si no existen usar clientDto
        const buyerIdentification =
          draft.buyerIdentification || draft.clientDto?.identification;
        const buyerIdType = draft.buyerIdType || draft.clientDto?.idType;
        const buyerName = draft.buyerName || draft.clientDto?.name;
        const buyerAddress = draft.buyerAddress || draft.clientDto?.address;
        const buyerPhone = draft.buyerPhone || draft.clientDto?.phone;
        const buyerMail = draft.buyerMail || draft.clientDto?.email;

        if (buyerIdentification && buyerIdType) {
          this.logger.debug('👤 Cargando datos del comprador:', {
            identification: buyerIdentification,
            idType: buyerIdType,
            name: buyerName,
          });

          this.buyerForm.patchValue({
            idClient: draft.idClient || draft.clientDto?.idClient || null,
            identification: buyerIdentification,
            idType: buyerIdType,
            businessName: buyerName || '',
            address: buyerAddress || '',
            phone: buyerPhone || '',
            email: buyerMail || '',
          });

          // Deshabilitar campos después de cargar datos del borrador (excepto identificación para poder buscar otro)
          this.buyerForm.get('idType')?.disable();
          this.buyerForm.get('businessName')?.disable();
          this.buyerForm.get('address')?.disable();
          this.buyerForm.get('phone')?.disable();
          this.buyerForm.get('email')?.disable();

          // Marcar tipo de cliente (final consumer, etc)
          if (buyerIdType) {
            this.clientType = buyerIdType;
          }

          // 🔧 FIX: Cargar contactos del cliente desde el backend
          const idClient = draft.idClient || draft.clientDto?.idClient;
          if (idClient) {
            this.logger.debug(
              `📞 Cargando contactos del cliente ID: ${idClient}`
            );
            this.loadClientContactsByIdClient(idClient);
          } else {
            this.logger.warn(
              '⚠️ No hay idClient, no se pueden cargar contactos'
            );
          }

          this.logger.debug(
            '✅ buyerForm después de patchValue:',
            this.buyerForm.value
          );
        } else {
          this.logger.warn('⚠️ No hay datos del comprador en el borrador');
        }

        // 2. Cargar detalles de la factura (productos)
        const details = draft.details || draft.invoiceDetails || [];
        this.logger.debug(`📦 Cargando ${details.length} productos`);

        if (details.length > 0) {
          // Limpiar el FormArray actual
          while (this.items.length) {
            this.items.removeAt(0);
          }

          // Limpiar el dataSource actual
          this.invoiceDetailDataSource.data = [];

          // 🔍 Buscar todos los productos en paralelo (cache o backend)
          const productRequests = details.map((detail) =>
            this.productCacheService.getProductByIdWithFallback(
              detail.idProduct
            )
          );

          forkJoin(productRequests).subscribe({
            next: (productsFromCache) => {
              this.logger.debug(
                `✅ ${productsFromCache.length} productos cargados (cache/backend)`
              );

              // Agregar cada detalle del borrador con los datos del cache/backend
              details.forEach((detail, index) => {
                const productFromCache = productsFromCache[index];

                this.logger.debug(`  Producto ${index + 1}:`, detail);
                this.logger.debug(`    - idProduct: ${detail.idProduct}`);
                this.logger.debug(`    - productFromCache:`, productFromCache);

                // Obtener la descripción del producto con múltiples fallbacks
                // Prioridad: 1. Cache/Backend lookup, 2. Backend (detail), 3. Backend (productDto), 4. Fallback
                const productDescription =
                  productFromCache?.name ||
                  productFromCache?.description ||
                  detail.description ||
                  detail.productDto?.name ||
                  detail.productDto?.description ||
                  `Producto ID ${detail.idProduct}`;

                const productCode =
                  productFromCache?.mainCode ||
                  detail.code ||
                  detail.productDto?.code ||
                  detail.idProduct?.toString() ||
                  '';

                const source = productFromCache
                  ? 'CACHE/BACKEND'
                  : 'DRAFT_DATA';
                this.logger.debug(
                  `    ✅ Descripción final: ${productDescription} (fuente: ${source})`
                );
                this.logger.debug(`    ✅ Código final: ${productCode}`);

                // Obtener IVA: prioridad 1. detail.ivaPercentage, 2. productFromCache.ivaTaxType.value, 3. 0
                const ivaPercent =
                  detail.ivaPercentage ||
                  productFromCache?.ivaTaxType?.value ||
                  0;

                // Obtener ICE: prioridad 1. productFromCache.iceTaxType.value, 2. 0
                const icePercent = productFromCache?.iceTaxType?.value || 0;

                this.logger.debug(
                  `    ✅ IVA: ${ivaPercent}% (fuente: ${
                    detail.ivaPercentage
                      ? 'DRAFT_DATA'
                      : productFromCache?.ivaTaxType?.value
                      ? 'CACHE/BACKEND'
                      : 'DEFAULT'
                  })`
                );
                this.logger.debug(
                  `    ✅ ICE: ${icePercent}% (fuente: ${
                    productFromCache?.iceTaxType?.value
                      ? 'CACHE/BACKEND'
                      : 'DEFAULT'
                  })`
                );

                const itemForm = this.fb.group({
                  code: [productCode, Validators.required],
                  quantity: [
                    detail.quantity || 1,
                    [Validators.required, Validators.min(1)],
                  ],
                  description: [productDescription, Validators.required],
                  unitPrice: [
                    detail.unitPrice || 0,
                    [Validators.required, Validators.min(0)],
                  ],
                  discount: [detail.discount || 0, [Validators.min(0)]],
                  iva: [ivaPercent, [Validators.min(0), Validators.max(100)]], // ✅ Cambiado de ivaPercentage a iva
                  ice: [icePercent, [Validators.min(0), Validators.max(100)]], // ✅ Obtener ICE del producto
                  subtotal: [detail.subtotal || 0],
                  totalValue: [detail.subtotal || 0], // Campo requerido por el template
                });

                // Agregar listeners para recalcular totales cuando cambien los valores
                itemForm.get('quantity')?.valueChanges.subscribe(() => {
                  this.updateDetailTotal(index);
                  this.calculateTotals();
                });

                itemForm.get('unitPrice')?.valueChanges.subscribe(() => {
                  this.updateDetailTotal(index);
                  this.calculateTotals();
                });

                itemForm.get('discount')?.valueChanges.subscribe(() => {
                  this.updateDetailTotal(index);
                  this.calculateTotals();
                });

                itemForm.get('iva')?.valueChanges.subscribe(() => {
                  this.updateDetailTotal(index);
                  this.calculateTotals();
                });

                itemForm.get('ice')?.valueChanges.subscribe(() => {
                  this.updateDetailTotal(index);
                  this.calculateTotals();
                });

                this.items.push(itemForm);

                // Agregar al dataSource para mostrar en la tabla
                const invoiceDetail: InvoiceDetail = {
                  idProduct: detail.idProduct,
                  description: productDescription,
                  unitPrice: detail.unitPrice,
                  discount: detail.discount || 0, // ✅ Usar el descuento del detalle
                  ice: icePercent, // ✅ Incluir ICE del producto
                  iva: ivaPercent, // ✅ Incluir IVA del detalle o producto
                  quantity: detail.quantity,
                  totalValue: detail.subtotal,
                  product: productFromCache, // ✅ Incluir el producto completo para tener acceso a taxTypes
                };

                const currentData = this.invoiceDetailDataSource.data;
                currentData.push(invoiceDetail);
                this.invoiceDetailDataSource.data = currentData;
              });

              this.logger.debug(
                '✅ Productos cargados. Total en tabla:',
                this.invoiceDetailDataSource.data.length
              );

              // Continuar con la carga de pagos y totales
              this.loadDraftPaymentsAndTotals(draft);
            },
            error: (err) => {
              this.logger.error(
                '❌ Error al cargar productos del borrador',
                err
              );
              this.toast.warning(
                'Error al cargar algunos productos del borrador'
              );
              // Continuar con la carga de pagos aunque fallen los productos
              this.loadDraftPaymentsAndTotals(draft);
            },
          });
        } else {
          this.logger.warn('⚠️ No hay productos en el borrador');
          // Continuar con la carga de pagos
          this.loadDraftPaymentsAndTotals(draft);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.handleDraftLoadError(err);
      },
    });
  }

  /**
   * Carga las formas de pago y recalcula los totales del borrador
   * Método auxiliar para loadDraftForEditing
   */
  private loadDraftPaymentsAndTotals(draft: InvoiceDto): void {
    // 3. Cargar formas de pago
    let payments = draft.payments || [];

    // Si no hay array de payments pero sí hay paymentMethod, crear un array con ese único pago
    if (payments.length === 0 && draft.paymentMethod) {
      payments = [
        {
          idPaymentMethod:
            draft.paymentMethod.idPaymentMethod || draft.idPaymentMethod,
          paymentMethod: draft.paymentMethod.paymentType,
          paymentType: draft.paymentMethod.paymentType,
          paymentMethodCode: undefined, // No disponible en PaymentMethodDto
          total: draft.paymentMethod.value || draft.totalAmount,
          value: draft.paymentMethod.value || draft.totalAmount,
          term: draft.paymentMethod.term,
          timeUnit: draft.paymentMethod.timeUnit,
          time: draft.paymentMethod.timeUnit,
        },
      ];
    }

    this.logger.debug(`💳 Cargando ${payments.length} formas de pago`);

    if (payments.length > 0) {
      // Limpiar el FormArray actual
      while (this.paymentItems.length) {
        this.paymentItems.removeAt(0);
      }

      // Limpiar el dataSource actual
      this.paymentsDataSource.data = [];

      // Agregar cada forma de pago del borrador
      payments.forEach((payment, index) => {
        this.logger.debug(`  Pago ${index + 1}:`, payment);

        const paymentForm = this.fb.group({
          idPaymentMethod: [
            payment.idPaymentMethod || null,
            Validators.required,
          ],
          paymentType: [
            payment.paymentType || payment.paymentMethod || '',
            Validators.required,
          ],
          value: [
            payment.total || payment.value || 0,
            [Validators.required, Validators.min(0)],
          ],
          term: [payment.term || 0],
          time: [payment.timeUnit || payment.time || 'DÍAS'],
          paymentMethodCode: [payment.paymentMethodCode || ''],
        });

        this.paymentItems.push(paymentForm);

        // Agregar al dataSource para mostrar en la tabla
        const paymentDetail: Payment = {
          idPaymentMethod: payment.idPaymentMethod,
          paymentType: payment.paymentType || payment.paymentMethod || '',
          paymentMethodCode: payment.paymentMethodCode,
          value: payment.total || payment.value || 0,
          term: payment.term,
          time: payment.timeUnit || payment.time,
        };

        const currentPaymentData = this.paymentsDataSource.data;
        currentPaymentData.push(paymentDetail);
        this.paymentsDataSource.data = currentPaymentData;
      });

      this.logger.debug(
        '✅ Formas de pago cargadas. Total en tabla:',
        this.paymentsDataSource.data.length
      );
    } else {
      this.logger.warn('⚠️ No hay formas de pago en el borrador');
    }

    // 4. Recalcular totales
    this.logger.debug('🧮 Recalculando totales...');
    this.calculateTotals();

    // 5. Forzar detección de cambios
    this.cdRef.markForCheck();

    // 6. Notificar al usuario
    this.toast.info(
      'Borrador cargado. Puede continuar editando y guardar o procesar la factura.'
    );

    this.logger.debug('✅ Borrador cargado exitosamente en el formulario');
  }

  /**
   * Maneja los errores al cargar un borrador
   * Método auxiliar para loadDraftForEditing
   */
  private handleDraftLoadError(err: HttpErrorResponse): void {
    this.logger.error('❌ Error al cargar borrador:', err);

    let errorMessage = 'Error al cargar el borrador';
    let shouldNavigate = true;

    if (err.status === 404) {
      errorMessage =
        'Borrador no encontrado. Puede haber sido eliminado o procesado.';
    } else if (err.status === 401) {
      // 401 puede significar token expirado o endpoint sin permisos
      const token = localStorage.getItem(environment.TOKEN);
      if (!token) {
        errorMessage = 'Sesión expirada. Por favor inicie sesión nuevamente.';
        this.logger.warn('⚠️ No hay token en localStorage');
        // Redirigir a login
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
        shouldNavigate = false;
      } else {
        errorMessage =
          'No tiene permisos para acceder a este borrador o su sesión expiró.';
        this.logger.warn('⚠️ Token existe pero backend retorna 401');
        this.logger.warn(
          '📝 Puede ser que el endpoint requiera configuración adicional en el backend.'
        );
      }
    } else if (err.status === 0) {
      errorMessage = 'No se puede conectar con el servidor de facturación.';
      this.logger.warn('⚠️ Error de conexión (Status 0)');
    } else if (err.error?.message) {
      errorMessage = err.error.message;
    } else if (err.message) {
      errorMessage = err.message;
    }

    this.toast.error(errorMessage);

    // Regresar a la lista de borradores si debe navegar
    if (shouldNavigate) {
      this.router.navigate(['/billing/drafts']);
    }
  }
}
