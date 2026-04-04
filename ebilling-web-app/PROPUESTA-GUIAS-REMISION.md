# 📋 Propuesta de Implementación - Guías de Remisión

## 🎯 Resumen Ejecutivo

Esta propuesta presenta la implementación completa del módulo de **Guías de Remisión** para Angular, siguiendo los mismos patrones de diseño, estructura y estilos del módulo de facturación existente.

---

## 📁 Estructura de Archivos Propuesta

```
src/app/
├── pages/
│   └── guides/                          # ✨ NUEVO MÓDULO
│       ├── guide-form/
│       │   ├── guide-form.component.ts
│       │   ├── guide-form.component.html
│       │   └── guide-form.component.css
│       ├── guide-list/
│       │   ├── guide-list.component.ts
│       │   ├── guide-list.component.html
│       │   └── guide-list.component.css
│       ├── guides-routing.module.ts
│       └── guides.module.ts
├── _model/
│   └── guide/                           # ✨ NUEVOS MODELOS
│       ├── guide-dto.ts
│       ├── guide-detail-dto.ts
│       ├── guide-document-reference-dto.ts
│       ├── carrier-dto.ts
│       └── guide-response.ts
└── _service/
    └── guide/                           # ✨ NUEVOS SERVICIOS
        ├── guide.service.ts
        └── carrier.service.ts
```

---

## 🎨 Diseño Visual - Alineado con Facturación

### Características del Diseño

1. **Misma estructura de secciones con `mat-card` y `toolbar-primary-sm`**
2. **Mismo sistema de labels (15% label, 85% campo)**
3. **Mismos estilos de botones y acciones**
4. **Misma tabla de productos con estilos consistentes**
5. **Responsive design completo**

### Layout Propuesto

```
┌─────────────────────────────────────────────────┐
│  TÍTULO: Guía de remisión                        │
├─────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────┐  │
│  │ Datos de Emisión                          │  │
│  │ [Establecimiento*] [Nombre comercial]      │  │
│  │ [Punto de emisión*]                        │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────┐  ┌──────────────────┐    │
│  │ Transportista    │  │ Traslado         │    │
│  │ [Identificación*] │  │ [Dir. partida*] │    │
│  │ [Tipo ID*]       │  │ [Inicio*]       │    │
│  │ [Razón social*]  │  │ [Fin*]          │    │
│  │ [Email*]         │  │ [Placa*]        │    │
│  └──────────────────┘  └──────────────────┘    │
│                                                  │
│  ┌──────────────────┐  ┌──────────────────┐    │
│  │ Destinatario      │  │ Comprobante      │    │
│  │ [Identificación*] │  │ [Tipo emisión]  │    │
│  │ [Tipo ID*]       │  │ [Documento]     │    │
│  │ [Razón social*]  │  │ [Buscar por]    │    │
│  │ [Dir. destino*]   │  │ [Validar]       │    │
│  │ [Ruta]            │  │ [Clave acceso]  │    │
│  │ [Motivo*]         │  │ [Nro. autoriz.] │    │
│  │ [Doc. aduanero]   │  │ [Fecha emisión] │    │
│  └──────────────────┘  └──────────────────┘    │
│                                                  │
│  ┌───────────────────────────────────────────┐  │
│  │ Detalle de productos                       │  │
│  │ [Buscar producto] [Buscar en listado]     │  │
│  │ ┌─────────────────────────────────────┐   │  │
│  │ │ Código │ Cantidad │ Descripción │   │   │  │
│  │ │ ...    │ ...      │ ...         │   │   │  │
│  │ └─────────────────────────────────────┘   │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌───────────────────────────────────────────┐  │
│  │ Campos adicionales                        │  │
│  │ [Tabla de campos personalizados]         │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌───────────────────────────────────────────┐  │
│  │ [Firmar y enviar] [Guardar sin firmar]   │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

---

## 📋 Constantes - `constants.ts`

Agregar las siguientes constantes al archivo `src/app/_constants/constants.ts`:

```typescript
//=========================CATALOGOS GUÍAS DE REMISIÓN===========================

/**
 * Tipos de emisión para guías de remisión
 */
export const GUIDE_EMISSION_TYPE = [
  { code: "FISICA", value: "Física" },
  { code: "ELECTRONICA", value: "Electrónica" },
];

/**
 * Tipos de documentos de sustento para guías de remisión
 * Basado en los códigos del SRI
 */
export const GUIDE_DOCUMENT_TYPE = [
  {
    code: "01",
    value: "Factura",
    description: "Es el más común. Se usa cuando el traslado es por una venta ya realizada.",
  },
  {
    code: "03",
    value: "Liquidación de Compra",
    description: "Cuando compras a alguien que no tiene RUC (artesanos, agricultores) y tú emites el documento.",
  },
  {
    code: "04",
    value: "Nota de Crédito",
    description: "Se usa cuando el motivo es una devolución de mercadería por parte del cliente.",
  },
  {
    code: "05",
    value: "Nota de Débito",
    description: "Menos común en traslados, pero se usa si hay un ajuste de precio/cantidad que afecte el envío.",
  },
  {
    code: "41",
    value: "Comprobante de Retención",
    description: "En casos específicos donde la ley lo permite como sustento.",
  },
  {
    code: "DAI",
    value: "Declaración Aduanera (DAI/DAE)",
    description: "Para importaciones o exportaciones (se suele poner el número de la declaración).",
  },
];

/**
 * Tipos de búsqueda para comprobante de sustento
 */
export const GUIDE_SEARCH_BY_TYPE = [
  { code: "ACCESS_KEY", value: "Clave de acceso" },
  { code: "DOCUMENT_NUMBER", value: "Número de documento" },
];

/**
 * Tipos de transporte para guías de remisión
 */
export const GUIDE_TRANSPORT_TYPE = [
  { code: "Terrestre", value: "Terrestre" },
  { code: "Aéreo", value: "Aéreo" },
  { code: "Marítimo", value: "Marítimo" },
  { code: "Fluvial", value: "Fluvial" },
];

/**
 * Motivos de traslado comunes para guías de remisión
 */
export const GUIDE_REASON_FOR_TRANSFER = [
  { code: "Venta", value: "Venta" },
  { code: "Traslado entre establecimientos", value: "Traslado entre establecimientos" },
  { code: "Exportación", value: "Exportación" },
  { code: "Importación", value: "Importación" },
  { code: "Devolución", value: "Devolución" },
  { code: "Consignación", value: "Consignación" },
  { code: "Reparación", value: "Reparación" },
  { code: "Otro", value: "Otro" },
];

/**
 * Códigos de identificación para transportistas
 * Similar a IDENTIFICATION_TYPE pero específico para transportistas
 */
export const CARRIER_IDENTIFICATION_TYPE = [
  { code: "04", value: "RUC" }, // RUC para empresas
  { code: "05", value: "Cédula" }, // Cédula para personas
  { code: "07", value: "Consumidor Final" },
  { code: "08", value: "Identificación del Exterior" },
];
```

---

## 📝 Componente TypeScript - `guide-form.component.ts`

```typescript
import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef, ElementRef, AfterViewInit } from "@angular/core";
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { MatTableDataSource } from "@angular/material/table";
import { MatDialog } from "@angular/material/dialog";
import { ActivatedRoute, Router } from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";
import { GuideService } from "../../../_service/guide/guide.service";
import { CarrierService } from "../../../_service/guide/carrier.service";
import { ClientService } from "../../../_service/customer/client.service";
import { ProductService } from "../../../_service/product/product.service";
import { CompanyService } from "../../../_service/customer/company.service";
import { InvoicePointService } from "../../../_service/auth/invoice-point.service";
import { GuideDto, GuideDetailDto, GuideDocumentReferenceDto, GuideResponse } from "../../../_model/guide/guide-dto";
import { CarrierDto } from "../../../_model/guide/carrier-dto";
import { Client } from "../../../_model/customer/client";
import { ProductDto } from "../../../_model/product/product-dto";
import { environment } from "../../../../environments/environment";
import { LoggerService } from "../../../_service/logger/logger.service";
import { IDENTIFICATION_TYPE, GUIDE_EMISSION_TYPE, GUIDE_DOCUMENT_TYPE, GUIDE_SEARCH_BY_TYPE, GUIDE_TRANSPORT_TYPE, GUIDE_REASON_FOR_TRANSFER, CARRIER_IDENTIFICATION_TYPE } from "../../../_constants/constants";
import { SelectProductDialogComponent } from "../billing/select-product-dialog/select-product-dialog.component";

@Component({
  selector: "app-guide-form",
  templateUrl: "./guide-form.component.html",
  styleUrls: ["./guide-form.component.css"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuideFormComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild("productSearchInput", { read: ElementRef })
  productSearchInput!: ElementRef;

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

  // Tipos de identificación (usando constantes)
  idTypes = IDENTIFICATION_TYPE.map((t) => ({
    value: t.code,
    viewValue: t.value,
  }));
  identificationTypes = IDENTIFICATION_TYPE;

  // Tabla de productos
  productCtrl = new FormControl("");
  guideDetailDataSource = new MatTableDataSource<GuideDetailDto>([]);
  displayedColumns: string[] = ["code", "quantity", "description", "actions"];

  // Estados
  loading = false;
  isProcessingGuide = false;
  isSavingDraft = false;
  currentDraftId: number | null = null;
  errorMessage = "";

  // Tipos de emisión y documento (usando constantes)
  emissionTypes = GUIDE_EMISSION_TYPE;
  documentTypes = GUIDE_DOCUMENT_TYPE;
  searchByTypes = GUIDE_SEARCH_BY_TYPE;

  // Tipos de transporte y motivos (usando constantes)
  transportTypes = GUIDE_TRANSPORT_TYPE;
  reasonForTransferTypes = GUIDE_REASON_FOR_TRANSFER;

  // Tipos de identificación para transportistas
  carrierIdTypes = CARRIER_IDENTIFICATION_TYPE;

  get guideDetails(): FormArray {
    return this.detailsForm.get("items") as FormArray;
  }

  constructor(private readonly route: ActivatedRoute, private readonly router: Router, private readonly fb: FormBuilder, private readonly guideService: GuideService, private readonly carrierService: CarrierService, private readonly clientService: ClientService, private readonly productService: ProductService, private readonly companyService: CompanyService, private readonly invoicePointService: InvoicePointService, private readonly snackBar: MatSnackBar, private readonly logger: LoggerService, private readonly dialog: MatDialog, private readonly cdRef: ChangeDetectorRef) {
    // Inicializar formularios
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadInitialData();
    this.checkForDraft();
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
    // Cleanup si es necesario
  }

  private initializeForms(): void {
    // Formulario de emisión
    this.emissionForm = this.fb.group({
      establishment: [null, Validators.required],
      commercialName: [""],
      emissionPoint: [null, Validators.required],
    });

    // Formulario de transportista
    this.carrierForm = this.fb.group({
      identification: ["", Validators.required],
      identificationType: [null, Validators.required],
      name: ["", Validators.required],
      email: ["", [Validators.required, Validators.email]],
      address: [""],
      phone: [""],
    });

    // Formulario de traslado
    this.transferForm = this.fb.group({
      originAddress: ["", Validators.required],
      startDate: ["", Validators.required],
      endDate: ["", Validators.required],
      transportPlate: ["", Validators.required],
      transportType: [GUIDE_TRANSPORT_TYPE[0].code], // Por defecto: Terrestre
    });

    // Formulario de destinatario
    this.recipientForm = this.fb.group({
      identification: ["", Validators.required],
      identificationType: [null, Validators.required],
      name: ["", Validators.required],
      destinationAddress: ["", Validators.required],
      route: [""],
      reasonForTransfer: [GUIDE_REASON_FOR_TRANSFER[0].code, Validators.required], // Por defecto: Venta
      customsDocument: [""],
    });

    // Formulario de comprobante sustento
    this.documentForm = this.fb.group({
      emissionType: [GUIDE_EMISSION_TYPE[0].code], // Por defecto: Física
      documentType: [GUIDE_DOCUMENT_TYPE[0].code], // Por defecto: Factura (01)
      searchBy: [GUIDE_SEARCH_BY_TYPE[0].code], // Por defecto: Clave de acceso
      searchValue: [""],
      accessKey: [""],
      authorizationNumber: [""],
      issueDate: [""],
      documentNumber: [""],
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

  private async loadInitialData(): Promise<void> {
    try {
      // Cargar establecimientos, puntos de emisión, transportistas, etc.
      await Promise.all([this.loadEstablishments(), this.loadEmissionPoints(), this.loadCarriers(), this.loadCompanyData()]);
      this.cdRef.detectChanges();
    } catch (error) {
      this.logger.error("Error al cargar datos iniciales", error);
      this.showError("Error al cargar datos iniciales");
    }
  }

  private async loadEstablishments(): Promise<void> {
    // Implementar carga de establecimientos
  }

  private async loadEmissionPoints(): Promise<void> {
    // Implementar carga de puntos de emisión
  }

  private async loadCarriers(): Promise<void> {
    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    if (companyId) {
      this.carrierService.getCarriers(companyId, true, 0, 100).subscribe({
        next: (page) => {
          this.carriers = page.content;
          this.cdRef.detectChanges();
        },
        error: (error) => {
          this.logger.error("Error al cargar transportistas", error);
        },
      });
    }
  }

  private async loadCompanyData(): Promise<void> {
    // Implementar carga de datos de empresa
  }

  private checkForDraft(): void {
    // Verificar si hay un borrador para editar
    const draftId = this.route.snapshot.params["id"];
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
        this.logger.error("Error al cargar borrador", error);
        this.showError("Error al cargar el borrador");
        this.loading = false;
        this.cdRef.detectChanges();
      },
    });
  }

  private populateForm(guide: GuideDto): void {
    // Poblar todos los formularios con los datos del borrador
    // Implementación detallada...
  }

  // ========== MÉTODOS DE TRANSPORTISTA ==========

  searchCarrier(): void {
    const identification = this.carrierForm.get("identification")?.value;
    if (!identification) {
      this.showError("Ingrese una identificación");
      return;
    }

    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));
    // Buscar transportista por identificación
    // Implementar búsqueda...
  }

  onCarrierIdentificationEnter(event: KeyboardEvent): void {
    if (event.key === "Enter") {
      this.searchCarrier();
    }
  }

  // ========== MÉTODOS DE DESTINATARIO ==========

  searchRecipient(): void {
    const identification = this.recipientForm.get("identification")?.value;
    if (!identification) {
      this.showError("Ingrese una identificación");
      return;
    }

    // Buscar cliente por identificación
    this.clientService.getClientByIdentification(identification).subscribe({
      next: (client) => {
        this.recipientForm.patchValue({
          identification: client.identification,
          identificationType: client.idType,
          name: client.name,
          destinationAddress: client.address || "",
        });
        this.cdRef.detectChanges();
      },
      error: () => {
        // Cliente no encontrado, permitir crear nuevo
        this.recipientForm.patchValue({
          identificationType: this.detectIdType(identification),
        });
        this.cdRef.detectChanges();
      },
    });
  }

  onRecipientIdentificationEnter(event: KeyboardEvent): void {
    if (event.key === "Enter") {
      this.searchRecipient();
    }
  }

  private detectIdType(identification: string): number {
    if (identification.length === 10) return 1; // Cédula
    if (identification.length === 13) return 2; // RUC
    return 3; // Pasaporte
  }

  // ========== MÉTODOS DE PRODUCTOS ==========

  searchProduct(): void {
    const searchTerm = this.productCtrl.value;
    if (!searchTerm || searchTerm.trim().length === 0) {
      this.openProductDialog();
      return;
    }

    // Buscar producto
    this.productService.searchProducts(searchTerm, 0, 20).subscribe({
      next: (page) => {
        if (page.content.length === 1) {
          this.addProductToDetails(page.content[0]);
        } else {
          this.openProductDialog();
        }
      },
      error: (error) => {
        this.logger.error("Error al buscar producto", error);
        this.openProductDialog();
      },
    });
  }

  openProductDialog(): void {
    const dialogRef = this.dialog.open(SelectProductDialogComponent, {
      width: "900px",
      maxWidth: "95vw",
      data: { searchTerm: this.productCtrl.value },
    });

    dialogRef.afterClosed().subscribe((product: ProductDto | null) => {
      if (product) {
        this.addProductToDetails(product);
      }
    });
  }

  addProductToDetails(product: ProductDto): void {
    const items = this.guideDetails;
    const existingIndex = items.controls.findIndex((control) => control.get("idProduct")?.value === product.idProduct);

    if (existingIndex >= 0) {
      // Incrementar cantidad si ya existe
      const currentQuantity = items.at(existingIndex).get("quantity")?.value || 0;
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
    this.productCtrl.setValue("");
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
      code: item.productDto?.mainCode || "",
      description: item.description || item.productDto?.name || "",
    }));
  }

  // ========== MÉTODOS DE COMPROBANTE SUSTENTO ==========

  validateDocument(): void {
    const searchBy = this.documentForm.get("searchBy")?.value;
    const searchValue = this.documentForm.get("searchValue")?.value;

    if (!searchValue) {
      this.showError("Ingrese un valor para buscar");
      return;
    }

    // Validar documento según tipo de búsqueda
    // Implementar validación...
  }

  // ========== MÉTODOS DE GUARDADO Y ENVÍO ==========

  signAndSend(): void {
    if (!this.validateGuide()) {
      this.showError("Por favor complete todos los campos obligatorios");
      return;
    }

    this.isProcessingGuide = true;
    this.errorMessage = "";

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
    this.errorMessage = "";

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
    const emission = this.emissionForm.value;
    const carrier = this.carrierForm.value;
    const transfer = this.transferForm.value;
    const recipient = this.recipientForm.value;
    const document = this.documentForm.value;
    const details = this.guideDetails.value;

    const companyId = Number(localStorage.getItem(environment.ID_COMPANY));

    return {
      companyDto: {
        idCompany: companyId,
        name: this.companyData?.name || "",
        nameCommercial: emission.commercialName || "",
        identification: this.companyData?.identification || "",
      },
      clientDto: {
        idClient: null, // Se buscará o creará
        name: recipient.name,
        identification: recipient.identification,
        idType: recipient.identificationType,
        contacts: [
          {
            contactType: "MAI",
            value: "", // Se puede agregar email del destinatario
            status: true,
          },
        ],
      },
      carrierDto: {
        idCompany: companyId,
        name: carrier.name,
        identification: carrier.identification,
        identificationType: carrier.identificationType,
        email: carrier.email,
        address: carrier.address,
        phone: carrier.phone,
      },
      establishment: {
        idEstablishment: emission.establishment,
        code: this.branchOffices.find((b) => b.id === emission.establishment)?.code || "",
        address: this.branchOffices.find((b) => b.id === emission.establishment)?.address || "",
        issuancePoint: {
          idIssuancePoint: emission.emissionPoint,
          code: this.emissionPoints.find((p) => p.id === emission.emissionPoint)?.code || "",
        },
      },
      issueDate: new Date().toISOString().split("T")[0],
      startDate: transfer.startDate,
      endDate: transfer.endDate,
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
              issueDate: document.issueDate,
            },
          ]
        : [],
    };
  }

  private createAndSendGuide(guideDto: GuideDto): void {
    this.guideService.createGuide(guideDto).subscribe({
      next: (response: GuideResponse) => {
        this.isProcessingGuide = false;
        if (response.code === 200) {
          this.showSuccess(`Guía creada exitosamente: ${response.guideDto?.guideNumber}`);
          this.router.navigate(["/guides"]);
        } else {
          this.errorMessage = response.message || "Error al crear guía";
        }
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.isProcessingGuide = false;
        this.errorMessage = error.error?.message || "Error al crear guía";
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
          this.showSuccess("Borrador guardado exitosamente");
        } else {
          this.errorMessage = response.message || "Error al guardar borrador";
        }
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.isSavingDraft = false;
        this.errorMessage = error.error?.message || "Error al guardar borrador";
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
          this.showSuccess("Borrador actualizado exitosamente");
        } else {
          this.errorMessage = response.message || "Error al actualizar borrador";
        }
        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.isSavingDraft = false;
        this.errorMessage = error.error?.message || "Error al actualizar borrador";
        this.cdRef.detectChanges();
      },
    });
  }

  private updateAndSendDraft(guideDto: GuideDto): void {
    // Primero actualizar, luego enviar
    this.updateDraft(guideDto);
    // Luego enviar al SRI
    // Implementar envío...
  }

  private validateGuide(): boolean {
    return this.emissionForm.valid && this.carrierForm.valid && this.transferForm.valid && this.recipientForm.valid && this.guideDetails.length > 0;
  }

  // ========== UTILIDADES ==========

  private showSuccess(message: string): void {
    this.snackBar.open(message, "Cerrar", {
      duration: 3000,
      horizontalPosition: "end",
      verticalPosition: "top",
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, "Cerrar", {
      duration: 5000,
      horizontalPosition: "end",
      verticalPosition: "top",
      panelClass: ["error-snackbar"],
    });
  }
}
```

---

## 🎨 Template HTML - `guide-form.component.html`

```html
<div class="guide-page">
  <!-- 1) TÍTULO -->
  <mat-toolbar class="toolbar-primary">Guía de remisión</mat-toolbar>

  <!-- 2) DATOS DE EMISIÓN -->
  <mat-card class="card-padding mt-16">
    <mat-toolbar class="toolbar-primary-sm">Datos de Emisión</mat-toolbar>
    <mat-card-content>
      <div fxLayout="row" fxLayoutAlign="start stretch">
        <div fxFlex="100%">
          <form [formGroup]="emissionForm">
            <!-- Fila 1: Establecimiento + Nombre comercial -->
            <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
              <!-- Label Establecimiento (15%) -->
              <div fxFlex="15%">
                <div class="input-label">Establecimiento <span class="required"></span></div>
              </div>
              <!-- Campo Establecimiento (85%) -->
              <div fxFlex="85%">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-select formControlName="establishment">
                    <mat-option *ngFor="let b of branchOffices" [value]="b.id"> {{ b.code }} – {{ b.address }} </mat-option>
                  </mat-select>
                </mat-form-field>
              </div>

              <!-- Label Nombre comercial (15%) -->
              <div fxFlex="15%">
                <div class="input-label">Nombre comercial</div>
              </div>
              <!-- Campo Nombre comercial (85%) -->
              <div fxFlex="85%">
                <mat-form-field appearance="outline" class="full-width">
                  <input matInput formControlName="commercialName" readonly />
                </mat-form-field>
              </div>
            </div>

            <!-- Fila 2: Punto de emisión -->
            <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
              <!-- Label Punto emisión (15%) -->
              <div fxFlex="15%">
                <div class="input-label">Punto de emisión <span class="required"></span></div>
              </div>
              <!-- Campo Punto emisión (85%) -->
              <div fxFlex="85%">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-select formControlName="emissionPoint">
                    <mat-option *ngFor="let p of emissionPoints" [value]="p.id"> {{ p.code }} </mat-option>
                  </mat-select>
                </mat-form-field>
              </div>
            </div>
          </form>
        </div>
      </div>
    </mat-card-content>
  </mat-card>

  <!-- 3) TRANSPORTISTA Y TRASLADO (2 columnas) -->
  <div fxLayout="row" fxLayoutGap="16px" fxLayout.xs="column">
    <!-- TRANSPORTISTA -->
    <mat-card class="card-padding mt-16" fxFlex="50%" fxFlex.xs="100%">
      <mat-toolbar class="toolbar-primary-sm">Transportista</mat-toolbar>
      <mat-card-content>
        <form [formGroup]="carrierForm">
          <!-- Identificación -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Identificación <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput placeholder="RUC / CI / Pasaporte / Id exterior" formControlName="identification" (keydown.enter)="onCarrierIdentificationEnter($event)" />
                <button type="button" mat-icon-button matSuffix (click)="searchCarrier()">
                  <mat-icon>search</mat-icon>
                </button>
              </mat-form-field>
            </div>
          </div>

          <!-- Tipo identificación -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Tipo identificación <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <mat-select formControlName="identificationType">
                  <mat-option *ngFor="let t of idTypes" [value]="t.value"> {{ t.viewValue }} </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>

          <!-- Razón social -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Razón social <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="name" />
              </mat-form-field>
            </div>
          </div>

          <!-- Correo electrónico -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Correo electrónico <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput type="email" formControlName="email" />
              </mat-form-field>
            </div>
          </div>

          <p class="hint mt-8">Recuerda revisar el correo electrónico para garantizar que el comprobante sea entregado exitosamente</p>
        </form>
      </mat-card-content>
    </mat-card>

    <!-- TRASLADO -->
    <mat-card class="card-padding mt-16" fxFlex="50%" fxFlex.xs="100%">
      <mat-toolbar class="toolbar-primary-sm">Traslado</mat-toolbar>
      <mat-card-content>
        <form [formGroup]="transferForm">
          <!-- Dirección de partida -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Dirección de partida <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <textarea matInput formControlName="originAddress" rows="2"></textarea>
              </mat-form-field>
            </div>
          </div>

          <!-- Inicio de transporte -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Inicio de transporte <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput [matDatepicker]="startPicker" formControlName="startDate" [max]="maxDate" />
                <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
                <mat-datepicker #startPicker></mat-datepicker>
              </mat-form-field>
            </div>
          </div>

          <!-- Fin de transporte -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Fin de transporte <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput [matDatepicker]="endPicker" formControlName="endDate" [max]="maxDate" />
                <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
                <mat-datepicker #endPicker></mat-datepicker>
              </mat-form-field>
            </div>
          </div>

          <!-- Placa -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Placa <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="transportPlate" />
              </mat-form-field>
            </div>
          </div>

          <!-- Tipo de transporte -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Tipo de transporte</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <mat-select formControlName="transportType">
                  <mat-option *ngFor="let t of transportTypes" [value]="t.code"> {{ t.value }} </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  </div>

  <!-- 4) DESTINATARIO Y COMPROBANTE SUSTENTO (2 columnas) -->
  <div fxLayout="row" fxLayoutGap="16px" fxLayout.xs="column">
    <!-- DESTINATARIO/CLIENTE -->
    <mat-card class="card-padding mt-16" fxFlex="50%" fxFlex.xs="100%">
      <mat-toolbar class="toolbar-primary-sm">Destinatario/Cliente</mat-toolbar>
      <mat-card-content>
        <form [formGroup]="recipientForm">
          <!-- Identificación -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Identificación <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput placeholder="RUC/CI/Pasaporte" formControlName="identification" (keydown.enter)="onRecipientIdentificationEnter($event)" />
                <button type="button" mat-icon-button matSuffix (click)="searchRecipient()">
                  <mat-icon>search</mat-icon>
                </button>
              </mat-form-field>
            </div>
          </div>

          <!-- Tipo identificación -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Tipo identificación <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <mat-select formControlName="identificationType">
                  <mat-option *ngFor="let t of idTypes" [value]="t.value"> {{ t.viewValue }} </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>

          <!-- Razón social -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Razón social <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="name" />
              </mat-form-field>
            </div>
          </div>

          <!-- Dirección destino -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Dirección destino <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <textarea matInput formControlName="destinationAddress" rows="2"></textarea>
              </mat-form-field>
            </div>
          </div>

          <!-- Ruta -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Ruta</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="route" />
              </mat-form-field>
            </div>
          </div>

          <!-- Motivo -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Motivo <span class="required"></span></div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <mat-select formControlName="reasonForTransfer">
                  <mat-option *ngFor="let r of reasonForTransferTypes" [value]="r.code"> {{ r.value }} </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>

          <!-- Documento aduanero -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Documento aduanero</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="customsDocument" />
              </mat-form-field>
            </div>
          </div>
        </form>
      </mat-card-content>
    </mat-card>

    <!-- COMPROBANTE SUSTENTO -->
    <mat-card class="card-padding mt-16" fxFlex="50%" fxFlex.xs="100%">
      <mat-toolbar class="toolbar-primary-sm">Comprobante sustento</mat-toolbar>
      <mat-card-content>
        <form [formGroup]="documentForm">
          <!-- Tipo de emisión -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Tipo de emisión</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <mat-select formControlName="emissionType">
                  <mat-option *ngFor="let t of emissionTypes" [value]="t.code"> {{ t.value }} </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>

          <!-- Documento -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Documento</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <mat-select formControlName="documentType">
                  <mat-option *ngFor="let t of documentTypes" [value]="t.code" [title]="t.description"> {{ t.code }} - {{ t.value }} </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>

          <!-- Buscar por -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Buscar por</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <mat-select formControlName="searchBy">
                  <mat-option *ngFor="let t of searchByTypes" [value]="t.code"> {{ t.value }} </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>

          <!-- Campo de búsqueda y botón Validar -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%"></div>
            <div fxFlex="85%" fxLayout="row" fxLayoutGap="8px">
              <div fxFlex>
                <mat-form-field appearance="outline" class="full-width">
                  <input matInput formControlName="searchValue" />
                </mat-form-field>
              </div>
              <div fxFlex="none">
                <button type="button" mat-raised-button color="primary" (click)="validateDocument()">Validar</button>
              </div>
            </div>
          </div>

          <!-- Clave de acceso -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Clave de acceso</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="accessKey" readonly />
              </mat-form-field>
            </div>
          </div>

          <!-- Número autorización -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Número autorización</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="authorizationNumber" readonly />
              </mat-form-field>
            </div>
          </div>

          <!-- Fecha de emisión -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Fecha de emisión</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="issueDate" readonly />
              </mat-form-field>
            </div>
          </div>

          <!-- Número comprobante -->
          <div fxLayout="row" fxLayoutGap="16px" fxLayoutAlign="start baseline" class="mt-12">
            <div fxFlex="15%">
              <div class="input-label">Número comprobante</div>
            </div>
            <div fxFlex="85%">
              <mat-form-field appearance="outline" class="full-width">
                <input matInput formControlName="documentNumber" readonly />
              </mat-form-field>
            </div>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  </div>

  <!-- 5) DETALLE DE PRODUCTOS -->
  <mat-card class="card-padding mt-24">
    <mat-toolbar class="toolbar-primary-sm">Detalle de productos</mat-toolbar>
    <mat-card-content>
      <form [formGroup]="detailsForm">
        <!-- Buscador de productos -->
        <div fxLayout="row" fxLayoutAlign="start baseline" fxLayoutGap="16px" class="mt-16">
          <div fxFlex="15%">
            <div class="input-label">Código / Descripción</div>
          </div>
          <div fxFlex="85%" fxLayout="row" fxLayoutGap="16px">
            <div fxFlex>
              <mat-form-field appearance="outline" class="full-width">
                <input #productSearchInput matInput placeholder="Escribe una letra o palabra, después seleccione el producto" [formControl]="productCtrl" (keyup.enter)="searchProduct()" />
              </mat-form-field>
            </div>
            <div fxFlex="none">
              <button type="button" mat-raised-button color="primary" (click)="searchProduct()">
                <mat-icon>search</mat-icon>
                Buscar en listado de productos
              </button>
            </div>
          </div>
        </div>

        <!-- Tabla de productos -->
        <div class="mt-16 guide-detail-table" formArrayName="items">
          <mat-table [dataSource]="guideDetailDataSource" matSort class="mat-elevation-z1 full-width">
            <!-- Código -->
            <ng-container matColumnDef="code">
              <mat-header-cell *matHeaderCellDef mat-sort-header> Código </mat-header-cell>
              <mat-cell *matCellDef="let row; let i = index" [formGroupName]="i">
                <input type="text" [value]="row.code" readonly />
              </mat-cell>
            </ng-container>

            <!-- Cantidad -->
            <ng-container matColumnDef="quantity">
              <mat-header-cell *matHeaderCellDef mat-sort-header> Cantidad </mat-header-cell>
              <mat-cell *matCellDef="let row; let i = index" [formGroupName]="i">
                <input type="number" formControlName="quantity" min="1" step="1" />
              </mat-cell>
            </ng-container>

            <!-- Descripción -->
            <ng-container matColumnDef="description">
              <mat-header-cell *matHeaderCellDef mat-sort-header> Descripción </mat-header-cell>
              <mat-cell *matCellDef="let row; let i = index" [formGroupName]="i">
                <input type="text" formControlName="description" readonly />
              </mat-cell>
            </ng-container>

            <!-- Acciones -->
            <ng-container matColumnDef="actions">
              <mat-header-cell *matHeaderCellDef>Acciones</mat-header-cell>
              <mat-cell *matCellDef="let row; let i = index">
                <button type="button" mat-icon-button color="warn" (click)="removeProductDetail(i)" title="Eliminar producto">
                  <mat-icon>delete</mat-icon>
                </button>
              </mat-cell>
            </ng-container>

            <!-- Filas -->
            <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
            <mat-row *matRowDef="let row; columns: displayedColumns"></mat-row>

            <!-- Fila para cuando no hay datos -->
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell" [attr.colspan]="displayedColumns.length">No existen productos</td>
            </tr>
          </mat-table>
        </div>
      </form>
    </mat-card-content>
  </mat-card>

  <!-- 6) CAMPOS ADICIONALES -->
  <mat-card class="card-padding mt-24">
    <mat-toolbar class="toolbar-primary-sm">Campos adicionales</mat-toolbar>
    <mat-card-content>
      <div class="mt-16">
        <mat-table [dataSource]="[]" class="mat-elevation-z1 full-width">
          <ng-container matColumnDef="name">
            <mat-header-cell *matHeaderCellDef>Nombre</mat-header-cell>
            <mat-cell *matCellDef="let row">{{ row.name }}</mat-cell>
          </ng-container>

          <ng-container matColumnDef="description">
            <mat-header-cell *matHeaderCellDef>Descripción</mat-header-cell>
            <mat-cell *matCellDef="let row">{{ row.description }}</mat-cell>
          </ng-container>

          <ng-container matColumnDef="actions">
            <mat-header-cell *matHeaderCellDef>Acciones</mat-header-cell>
            <mat-cell *matCellDef="let row; let i = index">
              <button type="button" mat-icon-button color="warn" title="Eliminar campo">
                <mat-icon>delete</mat-icon>
              </button>
            </mat-cell>
          </ng-container>

          <mat-header-row *matHeaderRowDef="['name', 'description', 'actions']"></mat-header-row>
          <mat-row *matRowDef="let row; columns: ['name', 'description', 'actions']"></mat-row>

          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell" colspan="3">No existen campos adicionales</td>
          </tr>
        </mat-table>

        <div class="mt-16">
          <button type="button" mat-raised-button color="primary" (click)="addAdditionalField()">
            <mat-icon>add</mat-icon>
            Añadir campo adicional
          </button>
        </div>
      </div>
    </mat-card-content>
  </mat-card>

  <!-- 7) BOTONES FINALES -->
  <mat-card class="actions-card mt-24">
    <mat-card-content>
      <div class="actions-container">
        <!-- Botón primario: Firmar y enviar -->
        <button type="button" mat-raised-button color="primary" class="btn-primary-action" (click)="signAndSend()" [disabled]="isProcessingGuide" matTooltip="Firma digitalmente y envía la guía al SRI" matTooltipPosition="above">
          <mat-spinner *ngIf="isProcessingGuide" diameter="20" class="btn-spinner"></mat-spinner>
          <mat-icon *ngIf="!isProcessingGuide">check_circle</mat-icon>
          <span class="btn-text">
            <span class="btn-title">{{ isProcessingGuide ? "Procesando..." : "Firmar y enviar" }}</span>
            <span class="btn-subtitle">{{ isProcessingGuide ? "Firmando y enviando al SRI" : "Firmar y enviar al SRI" }}</span>
          </span>
        </button>

        <!-- Botón secundario: Guardar sin firmar -->
        <button type="button" mat-stroked-button class="btn-secondary-action" (click)="saveDraft()" [disabled]="isSavingDraft" matTooltip="Guarda temporalmente sin procesar. Podrá editarlo más tarde." matTooltipPosition="above">
          <mat-spinner *ngIf="isSavingDraft" diameter="20" class="btn-spinner-draft"></mat-spinner>
          <mat-icon *ngIf="!isSavingDraft">drafts</mat-icon>
          <span class="btn-text">
            <span class="btn-title">{{ isSavingDraft ? "Guardando..." : "Guardar sin firmar" }}</span>
            <span class="btn-subtitle">Sin procesar ni enviar</span>
          </span>
        </button>
      </div>

      <!-- Indicador de estado de borrador -->
      <div class="draft-status-info" *ngIf="currentDraftId">
        <mat-icon class="status-icon">info</mat-icon>
        <span class="status-text"> Editando borrador #{{ currentDraftId }}. Use "Firmar y enviar" para completar el proceso. </span>
      </div>

      <!-- Mensaje de error -->
      <div *ngIf="errorMessage" class="error-message mt-16">
        <mat-icon>error</mat-icon>
        <span>{{ errorMessage }}</span>
      </div>
    </mat-card-content>
  </mat-card>
</div>
```

---

## 🎨 Estilos CSS - `guide-form.component.css`

```css
@import "../../_shared/custom-checkbox.css";
@import "../../_shared/toolbar-styles.css";
@import "../../_shared/shared-styles.css";
@import "../../_shared/table-styles.css";
@import "../../_shared/responsive-styles.css";

/* ======================
   PÁGINA PRINCIPAL
====================== */
.guide-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  background-color: var(--background-color, #f9fafb);
  min-height: calc(100vh - 64px);
}

/* ======================
   ESTILOS DE FORMULARIO
====================== */
.input-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 16px;
  padding-right: 12px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
  min-height: 56px;
  box-sizing: border-box;
  line-height: 1.5;
  text-align: right;
}

.input-label .required {
  color: #d32f2f;
  margin-left: 2px;
  font-weight: 600;
}

.input-label .required::before {
  content: "*";
}

.hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.6);
  margin-top: 8px;
  font-style: italic;
}

/* ======================
   TABLA DE PRODUCTOS
====================== */
.guide-detail-table {
  width: 100%;
  overflow-x: auto;
  border-radius: 4px;
  overflow: hidden;
  margin-top: 16px;
}

.guide-detail-table .mat-mdc-header-cell {
  background-color: #eceff1;
  color: #263238;
  font-weight: 700;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 12px 8px !important;
  border-bottom: 2px solid var(--primary-color, #0ea5e9);
}

.guide-detail-table .mat-mdc-cell {
  padding: 8px !important;
  font-size: 14px;
}

.guide-detail-table input {
  width: 100% !important;
  padding: 8px 10px !important;
  border: 1px solid #ddd !important;
  border-radius: 4px !important;
  font-size: 14px !important;
  font-family: inherit !important;
  transition: border-color 0.3s, box-shadow 0.3s !important;
  background-color: #ffffff !important;
  box-sizing: border-box !important;
}

.guide-detail-table input:focus {
  outline: none !important;
  border-color: var(--primary-color, #0ea5e9) !important;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.1) !important;
}

.guide-detail-table input:disabled,
.guide-detail-table input[readonly] {
  background-color: #f5f5f5 !important;
  color: #666 !important;
  cursor: not-allowed !important;
}

.guide-detail-table input:hover:not(:disabled):not([readonly]) {
  border-color: var(--primary-color, #0ea5e9);
}

/* Anchos de columnas */
.guide-detail-table .mat-column-code {
  flex: 0 0 150px;
  max-width: 150px;
}

.guide-detail-table .mat-column-quantity {
  flex: 0 0 120px;
  max-width: 120px;
}

.guide-detail-table .mat-column-description {
  flex: 1 1 auto;
  min-width: 300px;
}

.guide-detail-table .mat-column-actions {
  flex: 0 0 80px;
  max-width: 80px;
  text-align: center;
}

/* Filas */
.guide-detail-table .mat-mdc-row:nth-child(odd) {
  background-color: #ffffff;
}

.guide-detail-table .mat-mdc-row:nth-child(even) {
  background-color: #f8fafc;
}

.guide-detail-table .mat-mdc-row:hover {
  background-color: rgba(14, 165, 233, 0.05) !important;
}

/* ======================
   BOTONES DE ACCIÓN
====================== */
.actions-card {
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-top: 4px solid var(--primary-color, #0ea5e9);
}

.actions-card .mat-mdc-card-content {
  padding: 32px;
}

.actions-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}

.btn-primary-action {
  min-width: 280px;
  height: 72px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);
  position: relative;
  overflow: hidden;
}

.btn-primary-action::before {
  content: "";
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.btn-primary-action:hover::before {
  left: 100%;
}

.btn-primary-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.4);
}

.btn-primary-action:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
}

.btn-primary-action mat-icon {
  font-size: 28px;
  width: 28px;
  height: 28px;
  margin-right: 12px;
}

.btn-primary-action:disabled {
  opacity: 0.7 !important;
  cursor: not-allowed !important;
  background: linear-gradient(135deg, var(--primary-color, #0ea5e9) 0%, var(--primary-dark, #0a82bb) 100%) !important;
}

.btn-secondary-action {
  min-width: 280px;
  height: 72px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 12px;
  border: 2px solid #607d8b;
  color: #37474f;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #ffffff;
}

.btn-secondary-action:hover {
  background-color: #eceff1;
  border-color: #455a64;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(96, 125, 139, 0.2);
}

.btn-secondary-action:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(96, 125, 139, 0.15);
}

.btn-secondary-action mat-icon {
  font-size: 28px;
  width: 28px;
  height: 28px;
  margin-right: 12px;
  color: #607d8b;
}

.btn-secondary-action:disabled {
  opacity: 0.6 !important;
  cursor: not-allowed !important;
}

.btn-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.btn-title {
  font-size: 16px;
  font-weight: 600;
}

.btn-subtitle {
  font-size: 12px;
  font-weight: 400;
  opacity: 0.85;
  margin-top: 2px;
}

.draft-status-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 12px 20px;
  background-color: rgba(14, 165, 233, 0.08);
  border-left: 4px solid var(--primary-color, #0ea5e9);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.1);
}

.draft-status-info .status-icon {
  color: var(--primary-color, #0ea5e9);
  font-size: 20px;
  width: 20px;
  height: 20px;
}

.draft-status-info .status-text {
  color: var(--primary-dark, #0a82bb);
  font-size: 14px;
  font-weight: 500;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: #ffebee;
  border-left: 4px solid #f44336;
  border-radius: 4px;
  color: #c62828;
  font-size: 14px;
  font-weight: 500;
}

.error-message mat-icon {
  color: #f44336;
}

/* ======================
   FORM FIELDS GLOBALES
====================== */
::ng-deep .guide-page .mat-mdc-form-field {
  width: 100%;
  font-size: 14px;
}

::ng-deep .guide-page .mat-mdc-form-field-infix {
  min-height: 56px;
  display: flex;
  align-items: center;
}

::ng-deep .guide-page .mat-mdc-text-field-wrapper {
  padding-bottom: 0;
}

::ng-deep .guide-page .mat-mdc-form-field-subscript-wrapper {
  margin-top: 4px;
}

::ng-deep .btn-spinner {
  margin-right: 8px;
}

::ng-deep .btn-spinner circle {
  stroke: #ffffff !important;
}

::ng-deep .btn-spinner-draft {
  margin-right: 8px;
}

::ng-deep .btn-spinner-draft circle {
  stroke: var(--primary-color, #0ea5e9) !important;
}

/* ======================
   RESPONSIVE DESIGN
====================== */
@media (max-width: 959px) {
  .guide-page {
    padding: 16px;
  }

  .actions-container {
    flex-direction: column;
    gap: 16px;
  }

  .btn-primary-action,
  .btn-secondary-action {
    width: 100%;
    min-width: unset;
  }

  .actions-card .mat-mdc-card-content {
    padding: 20px 16px;
  }

  .draft-status-info {
    flex-direction: column;
    text-align: center;
  }

  /* En móviles, los campos de 2 columnas se apilan */
  .input-label {
    padding-top: 8px;
    padding-right: 8px;
    font-size: 13px;
    min-height: 48px;
  }

  /* Ajustar espaciado en cards */
  .card-padding {
    margin-top: 12px !important;
  }
}

@media (max-width: 599px) {
  .guide-page {
    padding: 12px;
  }

  .toolbar-primary {
    font-size: 18px;
    padding: 12px 16px;
  }

  .toolbar-primary-sm {
    font-size: 14px;
    padding: 10px 14px;
  }

  /* Tabla de productos con scroll horizontal en móviles */
  .guide-detail-table {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  ::ng-deep .guide-detail-table .mat-mdc-table {
    min-width: 600px;
  }
}

/* ======================
   UTILIDADES
====================== */
.mt-8 {
  margin-top: 8px;
}

.mt-12 {
  margin-top: 12px;
}

.mt-16 {
  margin-top: 16px;
}

.mt-24 {
  margin-top: 24px;
}

.full-width {
  width: 100%;
}

.card-padding {
  padding: 0;
}

.card-padding mat-card-content {
  padding: 24px;
}
```

---

## 📦 Modelos TypeScript

### `guide-dto.ts`

```typescript
export interface GuideDto {
  // Campos generados automáticamente
  idGuide?: number;
  idCompany?: number;
  idClient?: number;
  idCarrier?: number;
  idEstablishment?: number;
  idIssuancePoint?: number;
  sequential?: number;
  creationDate?: string;
  accessKey?: string;
  guideNumber?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  status?: string;
  environment?: string;
  emailSent?: boolean;
  emailSentDate?: string;

  // Campos obligatorios
  companyDto: CompanyDto;
  clientDto: ClientDto;
  carrierDto: CarrierDto;
  issueDate: string; // yyyy-MM-dd
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
  originAddress: string;
  destinationAddress: string;
  guideDetails: GuideDetailDto[];

  // Campos opcionales
  establishment?: EstablishmentDto;
  transportType?: string;
  transportPlate?: string;
  reasonForTransfer?: string;
  route?: string;
  additionalInformation?: string;
  documentReferences?: GuideDocumentReferenceDto[];
}

export interface GuideDetailDto {
  idGuideDetail?: number;
  idGuide?: number;
  idProduct: number;
  productDto?: ProductDto;
  quantity: number;
  description?: string;
  unit?: string;
  weight?: number;
  additionalInfo?: string;
}

export interface GuideDocumentReferenceDto {
  idGuideDocumentReference?: number;
  documentType: string; // "01" = Factura, "04" = Nota de Crédito
  documentNumber: string;
  accessKey?: string;
  issueDate?: string;
}

export interface GuideResponse {
  code: number;
  status: string;
  message?: string;
  guideDto?: GuideDto;
  guideDtos?: GuideDto[];
  idGuide?: number;
  guideNumber?: string;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  receptionStatusSri?: string;
  authorizationStatusSri?: string;
  storagePath?: string;
}

// DTOs compartidos (ya existen en el proyecto)
export interface CompanyDto {
  idCompany: number;
  name: string;
  nameCommercial?: string;
  identification: string;
}

export interface ClientDto {
  idClient: number;
  name: string;
  identification: string;
  idType: number;
  contacts?: ContactDto[];
}

export interface ContactDto {
  contactType: string;
  value: string;
  status?: boolean;
}

export interface EstablishmentDto {
  idEstablishment: number;
  code: string;
  address: string;
  issuancePoint: IssuancePointDto;
}

export interface IssuancePointDto {
  idIssuancePoint: number;
  code: string;
  sequentialInvoice?: string;
}

export interface ProductDto {
  idProduct: number;
  mainCode: string;
  name: string;
  description?: string;
}
```

### `carrier-dto.ts`

```typescript
export interface CarrierDto {
  idCarrier?: number;
  idCompany: number;
  identificationType: string; // "04" = RUC, "05" = Cédula, "07" = Consumidor Final
  identification: string;
  name: string;
  address?: string;
  phone?: string;
  email?: string;
  licenseNumber?: string;
  vehicleType?: string;
  status?: boolean;
}
```

---

## 🔧 Servicios

### `guide.service.ts`

```typescript
import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { GuideDto, GuideResponse, GuideListResponseDto } from "../../_model/guide/guide-dto";
import { Page } from "../../_model/page.model";

@Injectable({
  providedIn: "root",
})
export class GuideService {
  private readonly apiUrl = `${environment.HOST_EBILLING}/api/guides`;

  constructor(private readonly http: HttpClient) {}

  getAllGuides(): Observable<GuideListResponseDto[]> {
    return this.http.get<GuideListResponseDto[]>(this.apiUrl);
  }

  getGuideById(id: number): Observable<GuideDto> {
    return this.http.get<GuideDto>(`${this.apiUrl}/${id}`);
  }

  createGuide(guide: GuideDto): Observable<GuideResponse> {
    return this.http.post<GuideResponse>(this.apiUrl, guide);
  }

  saveDraft(guide: GuideDto): Observable<GuideResponse> {
    return this.http.post<GuideResponse>(`${this.apiUrl}/draft`, guide);
  }

  updateGuide(id: number, guide: GuideDto): Observable<GuideResponse> {
    return this.http.put<GuideResponse>(`${this.apiUrl}/${id}`, guide);
  }

  deleteGuide(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  sendToSri(id: number, password: string): Observable<GuideResponse> {
    const params = new HttpParams().set("password", password);
    return this.http.post<GuideResponse>(`${this.apiUrl}/${id}/send-to-sri`, null, { params });
  }

  searchGuides(
    filters: {
      guideNumber?: string;
      idClient?: number;
      idCarrier?: number;
    },
    page: number = 0,
    size: number = 20,
    sort: string = "creationDate,desc"
  ): Observable<Page<any>> {
    let params = new HttpParams().set("page", page.toString()).set("size", size.toString()).set("sort", sort);

    if (filters.guideNumber) {
      params = params.set("guideNumber", filters.guideNumber);
    }
    if (filters.idClient) {
      params = params.set("idClient", filters.idClient.toString());
    }
    if (filters.idCarrier) {
      params = params.set("idCarrier", filters.idCarrier.toString());
    }

    return this.http.get<Page<any>>(`${this.apiUrl}/search`, { params });
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, {
      responseType: "blob",
    });
  }

  downloadXml(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/xml`, {
      responseType: "blob",
    });
  }

  sendByEmail(id: number, email?: string): Observable<void> {
    let params = new HttpParams();
    if (email) {
      params = params.set("email", email);
    }
    return this.http.post<void>(`${this.apiUrl}/${id}/send-email`, null, {
      params,
    });
  }
}
```

### `carrier.service.ts`

```typescript
import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { CarrierDto } from "../../_model/guide/carrier-dto";
import { Page } from "../../_model/page.model";

@Injectable({
  providedIn: "root",
})
export class CarrierService {
  private readonly apiUrl = `${environment.HOST_EBILLING}/api/carrier`;

  constructor(private readonly http: HttpClient) {}

  getCarriers(idCompany: number, activeOnly: boolean = false, page: number = 0, size: number = 10, sort: string = "name,asc"): Observable<Page<CarrierDto>> {
    let params = new HttpParams().set("idCompany", idCompany.toString()).set("activeOnly", activeOnly.toString()).set("page", page.toString()).set("size", size.toString()).set("sort", sort);

    return this.http.get<Page<CarrierDto>>(this.apiUrl, { params });
  }

  getCarrierById(id: number): Observable<CarrierDto> {
    return this.http.get<CarrierDto>(`${this.apiUrl}/${id}`);
  }

  createCarrier(carrier: CarrierDto): Observable<CarrierDto> {
    return this.http.post<CarrierDto>(this.apiUrl, carrier);
  }

  updateCarrier(id: number, carrier: CarrierDto): Observable<CarrierDto> {
    return this.http.put<CarrierDto>(`${this.apiUrl}/${id}`, carrier);
  }

  deleteCarrier(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

---

## 🔗 Configuración de Routing y Menú

### Routing - `app-routing.module.ts`

Agregar la siguiente ruta:

```typescript
{
  path: 'guides',
  canActivate: [SessionValidationGuard],
  loadChildren: () =>
    import('./pages/guides/guides.module').then((m) => m.GuidesModule),
},
```

### Menú - `menu-filter.service.ts`

Agregar el mapeo de URL a feature code:

```typescript
private readonly urlToFeatureMap: { [key: string]: string } = {
  // ... otros mapeos ...

  // Facturación
  '/invoices': 'INVOICE_CREATE',
  '/credit-note': 'CREDIT_NOTE_CREATE',
  '/drafts': 'DRAFT_MANAGEMENT',
  '/guides': 'GUIDE_MANAGEMENT', // ✨ NUEVO
};
```

**Nota:** El backend debe configurar el menú en la base de datos con:

- **URL:** `/guides`
- **Feature Code:** `GUIDE_MANAGEMENT`
- **Icono:** (sugerido: `local_shipping` o `assignment`)

---

## ✅ Checklist de Implementación

- [ ] Crear estructura de carpetas
- [ ] Crear modelos TypeScript
- [ ] Crear servicios (GuideService, CarrierService)
- [ ] Crear componente de formulario
- [ ] Crear componente de listado
- [ ] Implementar routing (`app-routing.module.ts`)
- [ ] Agregar mapeo de menú (`menu-filter.service.ts`)
- [ ] Configurar menú en base de datos (backend)
- [ ] Implementar validaciones
- [ ] Implementar búsqueda de productos
- [ ] Implementar búsqueda de transportistas
- [ ] Implementar búsqueda de destinatarios
- [ ] Implementar guardado de borradores
- [ ] Implementar envío al SRI
- [ ] Implementar descarga de PDF/XML
- [ ] Implementar envío por email
- [ ] Pruebas unitarias
- [ ] Pruebas de integración

---

## 🎯 Características Principales

1. ✅ **Diseño Responsive**: Adaptado para móviles, tablets y desktop
2. ✅ **Estilos Consistentes**: Mismo look & feel que facturación
3. ✅ **Validaciones Completas**: Todos los campos obligatorios validados
4. ✅ **Búsqueda Inteligente**: Autocompletado para productos, transportistas y clientes
5. ✅ **Borradores**: Guardar sin enviar al SRI
6. ✅ **Integración Completa**: Con todos los servicios del backend

---

**Última actualización:** Diciembre 2025  
**Versión:** 1.0.0
