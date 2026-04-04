import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ProformaService } from '../../../../_service/proforma/proforma.service';
import { CompanyService } from '../../../../_service/customer/company.service';
import { ClientService } from '../../../../_service/customer/client.service';
import { ClientHelperService } from '../../../../_service/customer/client-helper.service';
import { ProductService } from '../../../../_service/product/product.service';
import { IssuancePointService } from '../../../../_service/customer/issuance-point.service';
import { SelectProductDialogComponent } from '../../select-product-dialog/select-product-dialog.component';
import {
  ProformaCreateRequestDto,
  ProformaDetailRequestDto,
  ProformaDto,
} from '../../../../_model/proforma/proforma-dto';
import { ToastService } from '../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { environment } from '../../../../../environments/environment';
import { HttpErrorResponse } from '@angular/common/http';
import { ProductDto } from '../../../../_model/product/product-dto';
import {
  UpdateProductComponent,
  ProductDialogData,
} from '../../../product/update-product/update-product.component';
import { Client } from '../../../../_model/customer/client';
import {
  UpdateClientComponent,
  ClientDialogData,
} from '../../../customer/client/update-client/update-client.component';

@Component({
  selector: 'app-create-proforma',
  templateUrl: './create-proforma.component.html',
  styleUrls: ['./create-proforma.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateProformaComponent implements OnInit {
  @ViewChild('productSearchInput', { read: ElementRef })
  productSearchInput!: ElementRef;
  form: FormGroup;
  branchOffices: { id: number; code: string; address: string }[] = [];
  emissionPoints: { id: number; code: string }[] = [];
  client:
    | {
        idClient: number;
        identification: string;
        name: string;
        idType?: number;
        address?: string;
        phone?: string;
        email?: string;
      }
    | null = null;
  productCtrl = new FormControl('');
  /** DataSource para la tabla de productos (igual que facturación) */
  detailsDataSource = new MatTableDataSource<FormGroup>([]);
  isLoading = false;
  isSaving = false;
  editId: number | null = null;

  /** Totales y subtotales (mismo modelo que facturación) */
  totals: { label: string; value: number }[] = [
    { label: 'Subtotal sin impuestos', value: 0 },
    { label: 'Total descuento', value: 0 },
    { label: 'Subtotal con descuento', value: 0 },
    { label: 'IVA Total', value: 0 },
    { label: 'Valor ICE', value: 0 },
  ];

  subtotalsByIva: { ivaPercent: number; subtotal: number; ivaValue: number }[] =
    [];

  // Mismos tipos de identificación que en facturación
  idTypes = [
    { value: 1, viewValue: 'Cédula' },
    { value: 2, viewValue: 'RUC' },
    { value: 3, viewValue: 'Pasaporte' },
    { value: 4, viewValue: 'Consumidor final' },
    { value: 5, viewValue: 'Identificación del exterior' },
  ];

  displayedColumns = [
    'code',
    'quantity',
    'description',
    'unitPrice',
    'discount',
    'totalValue',
    'iva',
    'ice',
    'actions',
  ];

  /** Fecha actual (para restringir emisión a solo hoy en modo creación) */
  get todayDate(): Date {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  }

  /** Fecha mínima para "válida hasta" (igual o posterior a hoy) */
  get minValidUntilDate(): Date {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  }

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly proformaService: ProformaService,
    private readonly companyService: CompanyService,
    private readonly clientService: ClientService,
    private readonly clientHelper: ClientHelperService,
    private readonly productService: ProductService,
    private readonly issuancePointService: IssuancePointService,
    private readonly dialog: MatDialog,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdRef: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      clientIdentification: [''], // Para búsqueda
      idEstablishment: [null, Validators.required],
      idIssuancePoint: [null, Validators.required],
      idClient: [null, Validators.required],
      issueDate: [new Date(), Validators.required],
      validUntil: [new Date(), Validators.required],
      additionalInformation: [''],
      details: this.fb.array([], Validators.required),
    });
  }

  private focusProductSearch(): void {
    const activeElement = document.activeElement as HTMLElement;
    if (activeElement && activeElement.blur) {
      activeElement.blur();
    }

    setTimeout(() => {
      try {
        if (this.productSearchInput?.nativeElement) {
          const inputElement =
            this.productSearchInput.nativeElement as HTMLInputElement;
          if (inputElement && typeof inputElement.focus === 'function') {
            inputElement.focus();
          }
        }
      } catch {
        // noop: si falla el enfoque no rompemos el flujo
      }
    }, 400);
  }

  private addMonth(d: Date): Date {
    const next = new Date(d);
    next.setMonth(next.getMonth() + 1);
    return next;
  }

  get details(): FormArray {
    return this.form.get('details') as FormArray;
  }

  /** Sincroniza el DataSource con el FormArray (igual que facturación) */
  private syncDetailsDataSource(): void {
    this.detailsDataSource.data = [...this.details.controls] as FormGroup[];
     this.calculateTotals();
    this.cdRef.markForCheck();
  }

  /**
   * Configura los listeners de cambio para un detalle
   * para recalcular totales cuando el usuario modifica cantidad, precio, descuento, IVA o ICE.
   */
  private configureDetailSubscriptions(group: FormGroup): void {
    const recalc = () => this.calculateTotals();

    group.get('quantity')?.valueChanges.subscribe(recalc);
    group.get('unitPrice')?.valueChanges.subscribe(recalc);
    group.get('discount')?.valueChanges.subscribe(recalc);
    group.get('ivaPercentage')?.valueChanges.subscribe(recalc);
    group.get('icePercentage')?.valueChanges.subscribe(recalc);
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = Number(id);
      this.loadProforma(this.editId);
    } else {
      this.loadCompanyData();
    }
  }

  /**
   * Construye el objeto client desde ProformaDto para mostrar en modo edición.
   * Extrae idType, address, phone, email de clientDto (directo o desde contacts).
   */
  private buildClientFromProformaDto(
    p: ProformaDto,
  ): {
    idClient: number;
    identification: string;
    name: string;
    idType?: number;
    address?: string;
    phone?: string;
    email?: string;
  } | null {
    if (!p.idClient || !p.clientDto) return null;
    const cd = p.clientDto as any;
    const tempClient: Client = {
      idClient: p.idClient,
      identification: cd.identification ?? '',
      name: cd.name ?? '',
      idType: cd.idType ?? 1,
      clientType: cd.clientType ?? 1,
      status: cd.status ?? true,
      companyIdentification: cd.companyIdentification ?? '',
      contacts: cd.contacts ?? [],
    };
    this.clientHelper.addContactInformation([tempClient]);
    return {
      idClient: p.idClient,
      identification: cd.identification ?? '',
      name: cd.name ?? '',
      idType: cd.idType != null ? Number(cd.idType) : undefined,
      address: tempClient.address ?? cd.address ?? '',
      phone: tempClient.phone ?? tempClient.mobil ?? cd.phone ?? '',
      email: tempClient.mail ?? cd.email ?? '',
    };
  }

  private loadProforma(id: number): void {
    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!idCompany) {
      this.toast.error('No se encontró compañía.');
      return;
    }

    this.isLoading = true;
    this.cdRef.markForCheck();

    this.proformaService.findById(id, idCompany).subscribe({
      next: (p: ProformaDto) => {
        this.loadCompanyData();
        this.client = this.buildClientFromProformaDto(p);
        this.form.patchValue({
          idEstablishment: p.idEstablishment,
          idIssuancePoint: p.idIssuancePoint,
          idClient: p.idClient,
          clientIdentification: p.clientDto?.identification ?? '',
          issueDate: p.issueDate ? new Date(p.issueDate) : new Date(),
          validUntil: p.validUntil ? new Date(p.validUntil) : this.addMonth(new Date()),
          additionalInformation: p.additionalInformation ?? '',
        });
        this.details.clear();
        (p.details || []).forEach((d) => {
          const group = this.fb.group({
            idProduct: [d.idProduct, Validators.required],
            productCode: [d.productCode ?? ''],
            description: [d.description ?? ''],
            quantity: [d.quantity, [Validators.required, Validators.min(0.01)]],
            unitPrice: [d.unitPrice, [Validators.required, Validators.min(0)]],
            discount: [d.discount ?? 0, [Validators.min(0), Validators.max(100)]],
            ivaPercentage: [d.ivaPercentage ?? 12, [Validators.required, Validators.min(0), Validators.max(100)]],
            icePercentage: [d.icePercentage ?? 0, [Validators.min(0), Validators.max(100)]],
            lineOrder: [d.lineOrder ?? this.details.length + 1],
          });
          this.configureDetailSubscriptions(group);
          this.details.push(group);
        });
        this.syncDetailsDataSource();
        this.isLoading = false;
        this.cdRef.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('Error loading proforma', err);
        this.isLoading = false;
        this.cdRef.markForCheck();
        this.toast.error('Error al cargar la proforma.');
      },
    });
  }

  private loadCompanyData(): void {
    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!idCompany) return;

    const applyEstablishmentAndPoint = (estId: number | null, ptId: number | null) => {
      if (estId != null && !this.form.get('idEstablishment')?.value) {
        this.form.patchValue({ idEstablishment: estId });
      }
      if (ptId != null && !this.form.get('idIssuancePoint')?.value) {
        this.form.patchValue({ idIssuancePoint: ptId });
      }
      this.cdRef.markForCheck();
    };

    const ptStr = localStorage.getItem('INVOICE_POINT_DATA');
    if (ptStr) {
      try {
        const pt = JSON.parse(ptStr);
        if (pt?.id != null && pt?.establishmentId != null) {
          this.emissionPoints = [{ id: pt.id, code: pt.code || '' }];
          applyEstablishmentAndPoint(pt.establishmentId, pt.id);
          return;
        }
      } catch {}
    }

    this.companyService.findByIds([idCompany]).subscribe({
      next: (res) => {
        const company = res.companyDetailsDtos?.[0];
        if (company?.establishments?.length) {
          this.branchOffices = company.establishments.map((e: any) => ({
            id: e.idEstablishment,
            code: e.code,
            address: e.address ?? '',
          }));
          const estId = localStorage.getItem(environment.ID_ESTABLISHMENT);
          const selEst = estId
            ? this.branchOffices.find((b) => b.id === Number(estId))
            : this.branchOffices[0];
          if (selEst) {
            this.form.patchValue({ idEstablishment: selEst.id });
          }
        }

        const estId = this.form.get('idEstablishment')?.value;
        if (!this.form.get('idIssuancePoint')?.value) {
          const loadPoint = (points: { idInvoiceIssuancePoint: number; code?: string }[]) => {
            const first = (points ?? []).find((p: any) => p.status !== false) ?? points?.[0];
            if (first) {
              this.emissionPoints = [{ id: first.idInvoiceIssuancePoint, code: first.code ?? '' }];
              this.form.patchValue({ idIssuancePoint: first.idInvoiceIssuancePoint });
            }
            this.cdRef.markForCheck();
          };
          if (estId) {
            this.issuancePointService.findByEstablishment(estId).subscribe({
              next: loadPoint,
              error: () => this.cdRef.markForCheck(),
            });
          } else {
            this.issuancePointService.findByCompany(idCompany).subscribe({
              next: (points) => {
                const first = (points ?? []).find((p: any) => p.status !== false) ?? points?.[0];
                if (first) {
                  this.emissionPoints = [{ id: first.idInvoiceIssuancePoint, code: first.code ?? '' }];
                  this.form.patchValue({
                    idIssuancePoint: first.idInvoiceIssuancePoint,
                    idEstablishment: first.establishmentId,
                  });
                }
                this.cdRef.markForCheck();
              },
              error: () => this.cdRef.markForCheck(),
            });
          }
        } else {
          this.cdRef.markForCheck();
        }
      },
      error: () => this.cdRef.markForCheck(),
    });
  }

  searchClient(): void {
    const ident = (this.form.get('clientIdentification')?.value as string)?.trim();
    if (!ident) {
      this.toast.warning('Ingrese una identificación para buscar');
      return;
    }

    const companyIdent = localStorage.getItem(environment.IDENTIFICATION) ?? '';
    const req = {
      identification: ident,
      name: '',
      clientType: 0,
      status: true,
      companyIdentification: companyIdent,
    };
    this.clientService.searchPageable(0, 10, req).subscribe({
      next: (res: any) => {
        const list = res?.content ?? [];
        // Mapear información de contacto (dirección, teléfono, correo) igual que en facturación
        this.clientHelper.addContactInformation(list);
        const c = list[0];
        if (c) {
          this.client = {
            idClient: c.idClient ?? c.id,
            identification: c.identification ?? ident,
            name: c.name ?? c.businessName ?? '',
            idType: c.idType != null ? Number(c.idType) : undefined,
            address: c.address ?? '',
            phone: c.phone ?? c.mobil ?? '',
            email: c.mail ?? '',
          };
          this.form.patchValue({
            idClient: this.client!.idClient,
            clientIdentification: this.client!.identification,
          });
          this.toast.success('Cliente encontrado y cargado');
          // Enfocar búsqueda de producto igual que en facturación
          this.cdRef.detectChanges();
          this.focusProductSearch();
        } else {
          this.client = null;
          this.toast.info(
            'Cliente no encontrado. Puede crear uno nuevo desde el botón de agregar.'
          );
        }
        this.cdRef.markForCheck();
      },
      error: () => {
        this.client = null;
        this.toast.error('Error al buscar cliente');
        this.cdRef.markForCheck();
      },
    });
  }

  openCreateClientDialog(): void {
    const currentIdentification =
      (this.form.get('clientIdentification')?.value as string) ?? '';

    const dialogRef = this.dialog.open(UpdateClientComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: true,
      data: {
        mode: 'create',
        initialIdentification: currentIdentification,
      } as ClientDialogData,
      panelClass: 'custom-dialog-container',
      autoFocus: false,
    });

    dialogRef.afterClosed().subscribe((result: Client | null) => {
      if (result && result.idClient) {
        this.client = {
          idClient: result.idClient,
          identification: result.identification,
          name: result.name,
          idType: result.idType != null ? Number(result.idType) : undefined,
          address: result.address,
          phone: result.phone ?? result.mobil,
          email: result.mail ?? '',
        };
        this.form.patchValue({
          idClient: this.client.idClient,
          clientIdentification: this.client.identification,
        });
        this.toast.success('Cliente creado y cargado');
        this.cdRef.detectChanges();
        this.focusProductSearch();
      }
    });
  }

  onClientIdentificationEnter(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (keyboardEvent.key === 'Enter' || keyboardEvent.keyCode === 13) {
      keyboardEvent.preventDefault();
      keyboardEvent.stopPropagation();
      this.searchClient();
    }
  }

  searchProduct(): void {
    // Igual que facturación: no validar término vacío, dejar que el API responda
    const term = (this.productCtrl.value ?? '').toString().trim();
    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!companyId) {
      this.toast.error('No se encontró compañía.');
      return;
    }

    this.productService
      .searchActiveProductsForBilling(term, companyId)
      .subscribe({
        next: (products: ProductDto[]) => {
          if (!products?.length) {
            this.toast.info(
              'Producto no encontrado. Puede crear uno nuevo desde el botón de agregar.'
            );
            return;
          }
          const ref = this.dialog.open(SelectProductDialogComponent, {
            width: '90vw',
            maxWidth: '1200px',
            maxHeight: '90vh',
            data: { products, searchTerm: term },
            disableClose: false,
          });
          ref.afterClosed().subscribe((selected: ProductDto[]) => {
            if (selected?.length) {
              selected.forEach((p) => this.addProduct(p));
              this.syncDetailsDataSource();
              this.toast.success(
                `${selected.length} producto(s) agregado(s) a la proforma`
              );
              this.productCtrl.setValue('');
            }
          });
        },
        error: () => this.toast.error('Error al buscar productos'),
      });
  }

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
          const companyId = Number(
            localStorage.getItem(environment.ID_COMPANY)
          );
          this.productService
            .searchActiveProductsForBilling(result.mainCode, companyId)
            .subscribe({
              next: (products: ProductDto[]) => {
                if (products.length > 0) {
                  products.forEach((p) => this.addProduct(p));
                  this.syncDetailsDataSource();
                  this.productCtrl.setValue('');
                  this.toast.success(
                    'Producto creado y agregado exitosamente a la proforma'
                  );
                } else {
                  this.toast.success('Producto creado exitosamente');
                }
              },
              error: () => {
                this.toast.success('Producto creado exitosamente');
              },
            });
        } else if (result === true) {
          this.toast.success('Producto creado exitosamente');
        }
      });
  }

  addProduct(p: ProductDto): void {
    // Mismo mapeo que facturación: ivaTaxType.value, iceTaxType.value
    const iva = p.ivaTaxType?.value ?? 12;
    const ice = p.iceTaxType?.value ?? 0;
    const group = this.fb.group({
      idProduct: [p.idProduct, Validators.required],
      productCode: [p.mainCode ?? ''],
      description: [p.description ?? p.name ?? ''],
      quantity: [1, [Validators.required, Validators.min(0.01)]],
      unitPrice: [p.unitPrice ?? 0, [Validators.required, Validators.min(0)]],
      discount: [0, [Validators.min(0), Validators.max(100)]],
      ivaPercentage: [iva, [Validators.required, Validators.min(0), Validators.max(100)]],
      icePercentage: [ice, [Validators.min(0), Validators.max(100)]],
      lineOrder: [this.details.length + 1],
    });
    this.configureDetailSubscriptions(group);
    this.details.push(group);
  }

  removeDetail(i: number): void {
    this.details.removeAt(i);
    this.details.controls.forEach((c, idx) => c.patchValue({ lineOrder: idx + 1 }));
    this.syncDetailsDataSource();
  }

  getSubtotal(i: number): number {
    const g = this.details.at(i) as FormGroup;
    const q = g.get('quantity')?.value ?? 0;
    const p = g.get('unitPrice')?.value ?? 0;
    const d = (g.get('discount')?.value ?? 0) / 100;
    return q * p * (1 - d);
  }

  getTotalAmount(): number {
    return this.details.controls.reduce((sum, _, i) => sum + this.getSubtotal(i), 0);
  }

  /**
   * Calcula subtotales, descuentos, ICE e IVA agrupado por porcentaje,
   * replicando la lógica de facturación pero sobre los detalles de proforma.
   */
  private calculateTotals(): void {
    let subtotalSinImpuestos = 0;
    let totalDescuentoMonetario = 0;
    let totalICE = 0;

    // Inicializar siempre las tarifas comunes
    const subtotalsByIvaMap = new Map<number, number>([
      [15, 0],
      [5, 0],
      [0, 0],
    ]);

    this.details.controls.forEach((group) => {
      const quantity = group.get('quantity')?.value || 0;
      const unitPrice = group.get('unitPrice')?.value || 0;
      const discountPercent = group.get('discount')?.value || 0;
      const icePercent = group.get('icePercentage')?.value || 0;
      const ivaPercent = group.get('ivaPercentage')?.value || 0;

      const lineSubtotal = quantity * unitPrice;
      const lineDiscount = lineSubtotal * (discountPercent / 100);
      const baseImponible = lineSubtotal - lineDiscount;
      const lineICE = baseImponible * (icePercent / 100);

      subtotalSinImpuestos += lineSubtotal;
      totalDescuentoMonetario += lineDiscount;
      totalICE += lineICE;

      const currentSubtotal = subtotalsByIvaMap.get(ivaPercent) || 0;
      subtotalsByIvaMap.set(ivaPercent, currentSubtotal + baseImponible);
    });

    const subtotalConDescuento = subtotalSinImpuestos - totalDescuentoMonetario;

    // Calcular subtotales e IVA por cada tarifa
    this.subtotalsByIva = [];
    let totalIVA = 0;

    subtotalsByIvaMap.forEach((subtotal, ivaPercent) => {
      const ivaValue = subtotal * (ivaPercent / 100);
      const subtotalRounded = Math.round(subtotal * 100) / 100;
      const ivaValueRounded = Math.round(ivaValue * 100) / 100;

      totalIVA += ivaValueRounded;

      this.subtotalsByIva.push({
        ivaPercent,
        subtotal: subtotalRounded,
        ivaValue: ivaValueRounded,
      });
    });

    // Ordenar por % de IVA descendente (15, 5, 0)
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
        value: Math.round(totalIVA * 100) / 100,
      },
      {
        label: 'Valor ICE',
        value: Math.round(totalICE * 100) / 100,
      },
    ];
  }

  getTotalProforma(): number {
    const subtotalConDescuento =
      this.totals.find((t) => t.label === 'Subtotal con descuento')?.value || 0;
    const iva =
      this.totals.find((t) => t.label === 'IVA Total')?.value || 0;
    const ice =
      this.totals.find((t) => t.label === 'Valor ICE')?.value || 0;

    const total = subtotalConDescuento + iva + ice;
    return Math.round(total * 100) / 100;
  }

  save(): void {
    // Validar cliente seleccionado (mismo criterio que facturación: comprador completo)
    if (!this.form.get('idClient')?.value) {
      this.toast.warning('Complete la información del cliente');
      return;
    }

    // Validar que haya al menos un producto
    if (this.details.length === 0) {
      this.toast.warning('Agregue al menos un producto a la proforma');
      return;
    }

    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!idCompany) {
      this.toast.error('No se encontró compañía.');
      return;
    }

    const v = this.form.value;
    const toDateStr = (d: Date | string): string => {
      if (!d) return '';
      const date = d instanceof Date ? d : new Date(d);
      if (isNaN(date.getTime())) return '';
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    };

    const details: ProformaDetailRequestDto[] = this.details.controls.map((c, i) => ({
      idProduct: c.get('idProduct')?.value,
      productCode: c.get('productCode')?.value ?? undefined,
      description: c.get('description')?.value ?? undefined,
      quantity: c.get('quantity')?.value,
      unitPrice: c.get('unitPrice')?.value,
      discount: c.get('discount')?.value ?? 0,
      ivaPercentage: c.get('ivaPercentage')?.value ?? 12,
      icePercentage: c.get('icePercentage')?.value ?? 0,
      lineOrder: i + 1,
    }));

    const dto: ProformaCreateRequestDto = {
      idCompany,
      idEstablishment: v.idEstablishment,
      idIssuancePoint: v.idIssuancePoint,
      idClient: v.idClient,
      issueDate: toDateStr(v.issueDate),
      validUntil: toDateStr(v.validUntil),
      additionalInformation: v.additionalInformation || undefined,
      details,
    };

    this.isSaving = true;
    this.cdRef.markForCheck();

    const req = this.editId
      ? this.proformaService.update(this.editId, dto)
      : this.proformaService.create(dto);

    req.subscribe({
      next: () => {
        this.toast.success(this.editId ? 'Proforma actualizada' : 'Proforma creada');
        this.router.navigate(['/invoices/proformas']);
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('Error saving proforma', err);
        this.isSaving = false;
        this.cdRef.markForCheck();
        this.toast.error(err.error?.message ?? 'Error al guardar la proforma.');
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/invoices/proformas']);
  }
}
