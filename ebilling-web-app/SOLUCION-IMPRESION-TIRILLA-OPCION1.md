# 🖨️ Solución: Impresión de Tirilla (Opción 1 - HTML + window.print())

## 📋 RESUMEN DE LA IMPLEMENTACIÓN

Esta solución implementa la impresión de tirilla de factura usando HTML + CSS + window.print(), tomando datos del `InvoiceDto` (retornado por el backend) y datos de sesión (localStorage).

---

## 🗂️ ARCHIVOS A CREAR/MODIFICAR

### **Nuevos archivos a crear:**

1. `src/app/pages/billing/ticket-print/ticket-print.component.ts`
2. `src/app/pages/billing/ticket-print/ticket-print.component.html`
3. `src/app/pages/billing/ticket-print/ticket-print.component.css`

### **Archivos a modificar:**

1. `src/app/pages/billing/billing.module.ts` - Agregar ruta y componente
2. `src/app/pages/billing/billing.component.ts` - Agregar método para imprimir y guardar invoiceId
3. `src/app/pages/billing/billing.component.html` - Agregar botón "Imprimir Tirilla"

---

## 📊 FUENTES DE DATOS

### **Datos del InvoiceDto (desde el backend):**

```typescript
interface InvoiceDto {
  idInvoice?: number;
  invoiceNumber?: string;           // ✅ "001-001-000001234"
  accessKey?: string;                // ✅ Clave de acceso del SRI
  issueDate: string;                 // ✅ Fecha de emisión (ISO 8601)
  authorizationDate?: string;        // ✅ Fecha de autorización (ISO 8601)
  authorizationNumber?: string;      // ✅ Número de autorización del SRI
  clientDto: ClientDto;              // ✅ Datos del cliente
  invoiceDetails: InvoiceDetailDto[]; // ✅ Detalle de productos
  payments?: PaymentDto[];           // ✅ Formas de pago
  totalAmount: number;               // ✅ Total a pagar
  subtotalIva: number;               // ✅ Subtotal con IVA
  subtotalZero: number;              // ✅ Subtotal 0%
  subtotalIce: number;               // ✅ ICE
  discount: number;                  // ✅ Descuento total
  tip: number;                       // ✅ Propina
  establishment: EstablishmentDto;   // ✅ Establecimiento
  companyDto?: CompanyDto;           // ⚠️ Puede venir o no
}
```

### **Datos de localStorage (sesión):**

```typescript
// Datos de la empresa
localStorage.getItem(environment.ID_COMPANY)           // ID de la compañía
localStorage.getItem(environment.IDENTIFICATION)       // RUC de la empresa
localStorage.getItem(environment.NAME_COMPANY)         // Nombre comercial
localStorage.getItem('COMPANY_CONTACTS')               // JSON con teléfonos y direcciones

// Ejemplo de COMPANY_CONTACTS:
[
  { contactType: 'TEL', value: '02-1234567', status: true },
  { contactType: 'DIR', value: 'Av. Principal 123, Quito', status: true }
]
```

---

## 📝 IMPLEMENTACIÓN DETALLADA

### **1. Componente TicketPrintComponent**

#### **`ticket-print.component.ts`**

```typescript
import { Component, OnInit, AfterViewInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InvoiceService } from '../../../_service/invoice/invoice.service';
import { InvoiceDto } from '../../../_model/invoice/invoice-dto';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../_service/logger/logger.service';

@Component({
  selector: 'app-ticket-print',
  templateUrl: './ticket-print.component.html',
  styleUrls: ['./ticket-print.component.css']
})
export class TicketPrintComponent implements OnInit, AfterViewInit {
  invoice: InvoiceDto | null = null;
  isLoading = true;
  error: string | null = null;

  // Datos de la empresa desde localStorage
  companyName: string = '';
  companyRuc: string = '';
  companyPhone: string = '';
  companyAddress: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invoiceService: InvoiceService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    // Obtener invoiceId de la ruta
    const invoiceId = this.route.snapshot.paramMap.get('invoiceId');
    
    if (!invoiceId) {
      this.error = 'No se proporcionó ID de factura';
      this.isLoading = false;
      return;
    }

    // Cargar datos de la empresa desde localStorage
    this.loadCompanyData();

    // Cargar datos de la factura
    this.loadInvoice(Number(invoiceId));
  }

  ngAfterViewInit(): void {
    // Auto-imprimir cuando la página esté lista
    // Esperar un poco para que el contenido se renderice
    setTimeout(() => {
      window.print();
      // Opcional: cerrar ventana después de imprimir (descomentar si se desea)
      // setTimeout(() => window.close(), 1000);
    }, 500);
  }

  private loadCompanyData(): void {
    // Nombre comercial
    this.companyName = localStorage.getItem(environment.NAME_COMPANY) || 'EMPRESA';

    // RUC
    this.companyRuc = localStorage.getItem(environment.IDENTIFICATION) || '';

    // Contactos (teléfono y dirección)
    const contactsStr = localStorage.getItem('COMPANY_CONTACTS');
    if (contactsStr) {
      try {
        const contacts = JSON.parse(contactsStr);
        const phoneContact = contacts.find((c: any) => c.contactType === 'TEL' && c.status);
        const addressContact = contacts.find((c: any) => c.contactType === 'DIR' && c.status);
        
        this.companyPhone = phoneContact?.value || '';
        this.companyAddress = addressContact?.value || '';
      } catch (error) {
        this.logger.warn('Error al parsear COMPANY_CONTACTS', error);
      }
    }

    // Si no hay dirección en contactos, intentar desde establishment del invoice
    // (se completará después de cargar el invoice)
  }

  private loadInvoice(invoiceId: number): void {
    this.invoiceService.findById(invoiceId).subscribe({
      next: (response) => {
        if (response?.code === 1 && response?.data) {
          this.invoice = response.data;
          
          // Completar dirección si no se encontró en contactos
          if (!this.companyAddress && this.invoice.establishment?.address) {
            this.companyAddress = this.invoice.establishment.address;
          }
          
          // Usar nombre de la empresa del invoice si está disponible
          if (this.invoice.companyDto?.name) {
            this.companyName = this.invoice.companyDto.name;
          }

          this.isLoading = false;
          this.logger.debug('✅ Factura cargada para impresión', this.invoice);
        } else {
          this.error = 'No se pudo cargar la factura';
          this.isLoading = false;
        }
      },
      error: (err) => {
        this.logger.error('❌ Error al cargar factura para impresión', err);
        this.error = 'Error al cargar la factura';
        this.isLoading = false;
      }
    });
  }

  /**
   * Formatea una fecha ISO a formato legible
   */
  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const day = String(date.getDate()).padStart(2, '0');
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const year = date.getFullYear();
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${day}/${month}/${year}  ${hours}:${minutes}`;
    } catch (error) {
      return dateStr;
    }
  }

  /**
   * Formatea la clave de acceso para mostrarla abreviada
   */
  formatAccessKey(accessKey: string | undefined): string {
    if (!accessKey) return 'N/A';
    if (accessKey.length <= 30) return accessKey;
    return `${accessKey.substring(0, 15)}...${accessKey.substring(accessKey.length - 10)}`;
  }

  /**
   * Calcula el vuelto si hay pago en efectivo
   */
  calculateChange(): number {
    if (!this.invoice?.payments) return 0;
    
    const cashPayment = this.invoice.payments.find(
      p => p.paymentType?.toLowerCase().includes('efectivo') || 
           p.paymentMethodCode === '01'
    );
    
    if (!cashPayment) return 0;
    
    const paid = cashPayment.value || cashPayment.total || 0;
    const total = this.invoice.totalAmount || 0;
    return Math.max(0, paid - total);
  }

  /**
   * Obtiene el nombre del tipo de identificación
   */
  getIdentificationTypeName(idType: number | undefined): string {
    if (!idType) return '';
    const types: { [key: number]: string } = {
      1: 'RUC',
      2: 'CEDULA',
      3: 'PASAPORTE',
      4: 'CONSUMIDOR FINAL'
    };
    return types[idType] || 'OTRO';
  }

  /**
   * Obtiene los detalles de productos (puede venir en invoiceDetails o details)
   */
  getInvoiceDetails() {
    return this.invoice?.invoiceDetails || this.invoice?.details || [];
  }

  /**
   * Obtiene las formas de pago
   */
  getPayments() {
    return this.invoice?.payments || [];
  }

  /**
   * Calcula el IVA basado en el subtotal con IVA
   */
  calculateIva(): number {
    if (!this.invoice) return 0;
    // El subtotalIva ya incluye el IVA, así que calculamos:
    // IVA = subtotalIva - (subtotalIva / 1.12)
    // O simplemente usamos: subtotalIva * 0.12 / 1.12
    return this.invoice.subtotalIva * 0.12 / 1.12;
  }
}
```

---

#### **`ticket-print.component.html`**

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Tirilla de Factura</title>
</head>
<body>
  <!-- Contenedor principal del ticket -->
  <div class="ticket-container" *ngIf="!isLoading && !error && invoice">
    
    <!-- ENCABEZADO: Datos de la empresa -->
    <div class="ticket-header">
      <div class="company-name">{{ companyName }}</div>
      <div class="company-ruc">RUC: {{ companyRuc }}</div>
      <div class="company-address" *ngIf="companyAddress">{{ companyAddress }}</div>
      <div class="company-phone" *ngIf="companyPhone">Tel: {{ companyPhone }}</div>
    </div>

    <!-- Separador -->
    <div class="separator">----------------------------------------</div>

    <!-- DATOS DE LA FACTURA -->
    <div class="invoice-info">
      <div class="invoice-line">
        <strong>FACTURA:</strong> {{ invoice.invoiceNumber || 'N/A' }}
      </div>
      <div class="invoice-line">
        <strong>FECHA:</strong> {{ formatDate(invoice.issueDate) }}
      </div>
      <div class="invoice-line" *ngIf="invoice.accessKey">
        <strong>CLAVE:</strong> {{ formatAccessKey(invoice.accessKey) }}
      </div>
      <div class="invoice-line" *ngIf="invoice.authorizationNumber">
        <strong>AUTORIZACIÓN:</strong> {{ invoice.authorizationNumber.substring(0, 20) }}...
      </div>
    </div>

    <div class="separator">----------------------------------------</div>

    <!-- DATOS DEL CLIENTE -->
    <div class="client-info">
      <div class="client-line">
        <strong>CLIENTE:</strong> {{ invoice.clientDto?.name || 'N/A' }}
      </div>
      <div class="client-line">
        <strong>{{ getIdentificationTypeName(invoice.clientDto?.idType) }}:</strong>
        {{ invoice.clientDto?.identification || 'N/A' }}
      </div>
      <div class="client-line" *ngIf="invoice.clientDto?.address">
        <strong>DIR:</strong> {{ invoice.clientDto.address }}
      </div>
      <div class="client-line" *ngIf="invoice.clientDto?.phone">
        <strong>TEL:</strong> {{ invoice.clientDto.phone }}
      </div>
    </div>

    <div class="separator">----------------------------------------</div>

    <!-- DETALLE DE PRODUCTOS -->
    <div class="products-header">
      <div class="products-row header">
        <span class="col-qty">CANT</span>
        <span class="col-desc">DESCRIPCIÓN</span>
        <span class="col-price">TOTAL</span>
      </div>
    </div>

    <div class="products-list">
      <div class="products-row" *ngFor="let item of getInvoiceDetails()">
        <span class="col-qty">{{ item.quantity || 1 }}</span>
        <span class="col-desc">
          {{ item.description || item.productDto?.name || item.code || 'Producto' }}
          <span *ngIf="item.productDto?.code" class="product-code">({{ item.productDto.code }})</span>
        </span>
        <span class="col-price">${{ (item.subtotal || item.unitPrice * (item.quantity || 1)).toFixed(2) }}</span>
      </div>
    </div>

    <div class="separator">----------------------------------------</div>

    <!-- TOTALES -->
    <div class="totals">
      <div class="total-row" *ngIf="invoice.subtotalZero > 0">
        <span class="total-label">SUBTOTAL 0%:</span>
        <span class="total-value">${{ invoice.subtotalZero.toFixed(2) }}</span>
      </div>
      <div class="total-row" *ngIf="invoice.subtotalIva > 0">
        <span class="total-label">SUBTOTAL 12%:</span>
        <span class="total-value">${{ invoice.subtotalIva.toFixed(2) }}</span>
      </div>
      <div class="total-row" *ngIf="calculateIva() > 0">
        <span class="total-label">IVA 12%:</span>
        <span class="total-value">${{ calculateIva().toFixed(2) }}</span>
      </div>
      <div class="total-row" *ngIf="invoice.subtotalIce > 0">
        <span class="total-label">ICE:</span>
        <span class="total-value">${{ invoice.subtotalIce.toFixed(2) }}</span>
      </div>
      <div class="total-row" *ngIf="invoice.discount > 0">
        <span class="total-label">DESCUENTO:</span>
        <span class="total-value">-${{ invoice.discount.toFixed(2) }}</span>
      </div>
      <div class="total-row" *ngIf="invoice.tip > 0">
        <span class="total-label">PROPINA 10%:</span>
        <span class="total-value">${{ invoice.tip.toFixed(2) }}</span>
      </div>
      <div class="total-row total-final">
        <span class="total-label"><strong>TOTAL:</strong></span>
        <span class="total-value"><strong>${{ invoice.totalAmount.toFixed(2) }}</strong></span>
      </div>
    </div>

    <div class="separator">----------------------------------------</div>

    <!-- FORMAS DE PAGO -->
    <div class="payments" *ngIf="getPayments().length > 0">
      <div class="payments-header"><strong>FORMA DE PAGO:</strong></div>
      <div class="payment-row" *ngFor="let payment of getPayments()">
        <span class="payment-type">{{ payment.paymentType || payment.paymentMethod || 'Pago' }}:</span>
        <span class="payment-value">${{ (payment.value || payment.total || 0).toFixed(2) }}</span>
      </div>
      <div class="payment-row change-row" *ngIf="calculateChange() > 0">
        <span class="payment-type"><strong>VUELTO:</strong></span>
        <span class="payment-value"><strong>${{ calculateChange().toFixed(2) }}</strong></span>
      </div>
    </div>

    <div class="separator">----------------------------------------</div>

    <!-- LEYENDA LEGAL -->
    <div class="legal-notice">
      <div class="legal-text">
        <strong>DOCUMENTO NO VÁLIDO COMO</strong>
      </div>
      <div class="legal-text">
        <strong>COMPROBANTE DE VENTA AUTORIZADO</strong>
      </div>
      <div class="legal-subtext">
        Consulte su factura electrónica en el portal del SRI
      </div>
      <div class="legal-subtext" *ngIf="invoice.environment">
        Ambiente: {{ invoice.environment === '1' ? 'PRODUCCIÓN' : 'PRUEBAS' }}
      </div>
    </div>

    <div class="separator">----------------------------------------</div>

    <!-- PIE DE PÁGINA -->
    <div class="footer">
      <div class="footer-text">¡Gracias por su compra!</div>
    </div>

  </div>

  <!-- Estado de carga -->
  <div class="loading" *ngIf="isLoading">
    <p>Cargando factura...</p>
  </div>

  <!-- Estado de error -->
  <div class="error" *ngIf="error">
    <p>{{ error }}</p>
    <button (click)="window.close()">Cerrar</button>
  </div>
</body>
</html>
```

---

#### **`ticket-print.component.css`**

```css
/* Estilos para pantalla (vista previa) */
.ticket-container {
  max-width: 75mm;
  margin: 0 auto;
  padding: 10px;
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 10pt;
  line-height: 1.4;
  background: white;
  color: black;
}

.ticket-header {
  text-align: center;
  margin-bottom: 8px;
}

.company-name {
  font-weight: bold;
  font-size: 11pt;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.company-ruc {
  font-size: 9pt;
  margin-bottom: 2px;
}

.company-address {
  font-size: 8pt;
  margin-bottom: 2px;
}

.company-phone {
  font-size: 8pt;
}

.separator {
  text-align: center;
  margin: 8px 0;
  font-size: 9pt;
  color: #666;
}

.invoice-info,
.client-info {
  margin: 6px 0;
  font-size: 9pt;
}

.invoice-line,
.client-line {
  margin: 3px 0;
  word-wrap: break-word;
}

.client-line strong {
  display: inline-block;
  min-width: 60px;
}

.products-header {
  margin: 6px 0;
}

.products-row {
  display: flex;
  justify-content: space-between;
  margin: 4px 0;
  font-size: 9pt;
}

.products-row.header {
  font-weight: bold;
  border-bottom: 1px dashed #ccc;
  padding-bottom: 3px;
  margin-bottom: 6px;
}

.col-qty {
  width: 35px;
  text-align: right;
  margin-right: 5px;
}

.col-desc {
  flex: 1;
  margin: 0 5px;
  word-wrap: break-word;
  line-height: 1.3;
}

.product-code {
  font-size: 7pt;
  color: #666;
  display: block;
}

.col-price {
  width: 70px;
  text-align: right;
  font-weight: bold;
}

.totals {
  margin: 8px 0;
  font-size: 9pt;
}

.total-row {
  display: flex;
  justify-content: space-between;
  margin: 3px 0;
}

.total-label {
  flex: 1;
}

.total-value {
  text-align: right;
  min-width: 80px;
}

.total-final {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 2px solid #000;
  font-size: 10pt;
}

.payments {
  margin: 8px 0;
  font-size: 9pt;
}

.payments-header {
  margin-bottom: 4px;
  font-weight: bold;
}

.payment-row {
  display: flex;
  justify-content: space-between;
  margin: 3px 0;
}

.payment-type {
  flex: 1;
}

.payment-value {
  text-align: right;
  min-width: 80px;
}

.change-row {
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px dashed #ccc;
}

.legal-notice {
  text-align: center;
  margin: 10px 0;
  padding: 8px 0;
}

.legal-text {
  font-size: 9pt;
  margin: 4px 0;
  color: #d32f2f;
}

.legal-subtext {
  font-size: 7pt;
  margin: 3px 0;
  color: #666;
}

.footer {
  text-align: center;
  margin-top: 10px;
  font-size: 8pt;
  color: #666;
}

.loading,
.error {
  text-align: center;
  padding: 50px 20px;
  font-size: 12pt;
}

/* ============================================
   ESTILOS PARA IMPRESIÓN (window.print())
   ============================================ */

@media print {
  @page {
    size: 75mm auto;
    margin: 2mm 3mm;
  }

  body {
    margin: 0;
    padding: 0;
    background: white;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .ticket-container {
    max-width: 100%;
    width: 100%;
    margin: 0;
    padding: 0;
    font-size: 9pt;
  }

  .separator {
    font-size: 8pt;
  }

  .company-name {
    font-size: 10pt;
  }

  .company-ruc,
  .company-address,
  .company-phone {
    font-size: 7pt;
  }

  .invoice-info,
  .client-info {
    font-size: 8pt;
  }

  .products-row {
    font-size: 8pt;
  }

  .totals,
  .payments {
    font-size: 8pt;
  }

  .legal-text {
    font-size: 8pt;
  }

  .legal-subtext {
    font-size: 6pt;
  }

  /* Ocultar elementos no necesarios en impresión */
  .loading,
  .error {
    display: none;
  }

  /* Evitar saltos de página dentro del ticket */
  .ticket-container {
    page-break-inside: avoid;
  }
}
```

---

### **2. Modificar BillingModule**

#### **`billing.module.ts`** (AGREGAR):

```typescript
// ... imports existentes ...
import { TicketPrintComponent } from './ticket-print/ticket-print.component';

const routes: Routes = [
  {
    path: 'drafts',
    component: DraftInvoicesComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'credit-notes',
    component: CreditNotesComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'ticket/:invoiceId',  // ← NUEVA RUTA
    component: TicketPrintComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: '',
    component: BillingComponent,
    canActivate: [CanActiveUserGuard],
  },
];

@NgModule({
  declarations: [
    BillingComponent,
    DraftInvoicesComponent,
    CreditNotesComponent,
    SelectProductDialogComponent,
    AddPaymentDialogComponent,
    SelectInvoiceDialogComponent,
    CreateCreditNoteComponent,
    TicketPrintComponent,  // ← NUEVO COMPONENTE
  ],
  // ... resto del módulo ...
})
```

---

### **3. Modificar BillingComponent**

#### **`billing.component.ts`** (AGREGAR):

```typescript
// ... imports existentes ...

export class BillingComponent implements OnInit, OnDestroy {
  // ... propiedades existentes ...
  
  // NUEVA propiedad: ID de la factura recién creada
  lastCreatedInvoiceId: number | null = null;

  // ... constructor y métodos existentes ...

  signAndSend(): void {
    // ... código existente hasta el subscribe ...

    this.purchaseProofService.register(invoiceDto).subscribe({
      next: (response) => {
        this.isProcessingInvoice = false;
        this.cdRef.detectChanges();

        // ✅ GUARDAR EL ID DE LA FACTURA CREADA
        if (response?.data?.idInvoice) {
          this.lastCreatedInvoiceId = response.data.idInvoice;
        }

        // Snackbar de éxito mejorado
        this.snackBar.open('✅ Factura guardada correctamente', 'OK', {
          duration: 5000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
        this.logger.debug('Factura registrada exitosamente', response);

        // ... resto del código existente ...
      },
      error: (err: HttpErrorResponse) => {
        // ... código de error existente ...
      },
    });
  }

  /**
   * Abre la ventana de impresión de tirilla
   * Solo funciona si hay una factura recién creada
   */
  printTicket(): void {
    if (!this.lastCreatedInvoiceId) {
      this.snackBar.open('⚠️ No hay factura disponible para imprimir', 'OK', {
        duration: 3000,
      });
      this.logger.warn('Intento de imprimir tirilla sin invoiceId');
      return;
    }

    const url = `/billing/ticket/${this.lastCreatedInvoiceId}`;
    const printWindow = window.open(url, '_blank', 'width=400,height=600');
    
    if (!printWindow) {
      this.snackBar.open('❌ No se pudo abrir la ventana de impresión. Verifique el bloqueador de ventanas emergentes.', 'OK', {
        duration: 5000,
      });
      return;
    }

    this.logger.debug('Ventana de impresión abierta', { invoiceId: this.lastCreatedInvoiceId });
  }
}
```

---

#### **`billing.component.html`** (AGREGAR botón):

```html
<!-- ... código existente ... -->

<!-- Agregar después del botón "Guardar como borrador" -->
<div class="action-buttons" fxLayout="row" fxLayoutGap="16px">
  <!-- Botón principal: Facturar -->
  <button
    type="button"
    mat-raised-button
    color="primary"
    class="btn-primary-action"
    (click)="signAndSend()"
    [disabled]="isProcessingInvoice"
    matTooltip="Guarda la factura, la firma digitalmente y la envía al SRI."
    matTooltipPosition="above"
  >
    <!-- ... código existente ... -->
  </button>

  <!-- Botón secundario: Guardar como borrador -->
  <button
    type="button"
    mat-stroked-button
    class="btn-secondary-action"
    (click)="saveDraft()"
    [disabled]="isSavingDraft"
    matTooltip="Guarda temporalmente sin procesar. Podrá editarlo más tarde."
    matTooltipPosition="above"
  >
    <!-- ... código existente ... -->
  </button>

  <!-- ✅ NUEVO BOTÓN: Imprimir Tirilla -->
  <button
    type="button"
    mat-stroked-button
    color="accent"
    class="btn-print-action"
    (click)="printTicket()"
    [disabled]="!lastCreatedInvoiceId || isProcessingInvoice"
    matTooltip="Imprime la tirilla informativa de la factura recién creada"
    matTooltipPosition="above"
  >
    <mat-icon>print</mat-icon>
    <span>Imprimir Tirilla</span>
  </button>
</div>

<!-- ... resto del código existente ... -->
```

---

## 🎯 FLUJO DE USO

1. **Usuario completa la factura** y hace clic en "Facturar" (`signAndSend()`)
2. **Backend procesa y retorna** la factura creada con `idInvoice`
3. **Se guarda `lastCreatedInvoiceId`** en el componente
4. **Aparece el botón "Imprimir Tirilla"** habilitado
5. **Usuario hace clic en "Imprimir Tirilla"** → Se abre nueva ventana en `/billing/ticket/:invoiceId`
6. **TicketPrintComponent carga**:
   - Datos de la factura desde el backend (usando `invoiceService.findById()`)
   - Datos de la empresa desde `localStorage`
7. **Se renderiza el HTML** con formato de ticket
8. **Se ejecuta `window.print()`** automáticamente
9. **Usuario selecciona la impresora EPSON** y confirma
10. **Se imprime la tirilla** 📄

---

## ✅ VENTAJAS DE ESTA IMPLEMENTACIÓN

1. ✅ **Datos completos**: Toma datos del `InvoiceDto` (backend) + `localStorage` (sesión)
2. ✅ **Formato optimizado**: CSS específico para impresión en papel de 76mm
3. ✅ **Auto-impresión**: Se ejecuta automáticamente al cargar la página
4. ✅ **Manejo de errores**: Muestra mensajes si no se puede cargar la factura
5. ✅ **Reutilizable**: El componente puede usarse desde otras partes (ej: desde lista de facturas)
6. ✅ **Sin dependencias nuevas**: Usa solo Angular estándar

---

## 📋 CHECKLIST DE VALIDACIÓN

Antes de implementar, verificar:

- [ ] El endpoint `GET /api/invoice/{id}` retorna los datos completos del `InvoiceDto`
- [ ] `localStorage` tiene `environment.NAME_COMPANY` y `environment.IDENTIFICATION`
- [ ] `localStorage` tiene `COMPANY_CONTACTS` en formato JSON válido
- [ ] La impresora EPSON está instalada en Windows como impresora genérica
- [ ] El formato de papel está configurado a 76mm continuo

---

## 🔧 AJUSTES POST-IMPLEMENTACIÓN

Después de implementar, es posible que necesites ajustar:

1. **Tamaños de fuente**: Ajustar `font-size` en CSS si el texto es muy grande/pequeño
2. **Ancho de columnas**: Ajustar `col-qty`, `col-desc`, `col-price` si hay desbordamiento
3. **Márgenes de página**: Ajustar `@page { margin: ... }` según la impresora
4. **Cierre automático**: Descomentar `window.close()` si se desea cerrar la ventana después de imprimir

---

## 🚀 LISTO PARA IMPLEMENTAR

Esta solución está completa y lista para aplicar. Todos los archivos y modificaciones están documentados arriba.

¿Deseas que proceda con la implementación?


