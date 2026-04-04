# 🖨️ Solución Actualizada: Impresión Automática de Tirilla (Opción 1)

## 📋 CAMBIOS SOLICITADOS

1. ✅ **Impresión automática**: Solo cuando `signAndSend()` retorna 200 con `idInvoice` (NO para borradores)
2. ✅ **Etiquetas condicionales**: FACTURA, CLAVE y AUTORIZACIÓN solo si existe `invoiceNumber`
3. ✅ **Totales detallados**: Mostrar subtotales por porcentaje de IVA (15%, 5%, 0%) según la imagen

---

## 🔄 FLUJO DE IMPRESIÓN AUTOMÁTICA

```
Usuario hace clic en "Facturar" (signAndSend())
         │
         ▼
POST /api/invoice/register
         │
         ▼
Respuesta 200 OK con { code: 1, data: { idInvoice: 123, invoiceNumber: "..." } }
         │
         ▼
✅ IMPRESIÓN AUTOMÁTICA (se abre ventana y ejecuta window.print())
         │
         ▼
Usuario selecciona impresora EPSON y confirma
```

**IMPORTANTE**: Si es borrador (`saveDraft()`), **NO se imprime automáticamente**.

---

## 📝 IMPLEMENTACIÓN ACTUALIZADA

### **1. Modificar BillingComponent**

#### **`billing.component.ts`** (MODIFICAR método `signAndSend()`):

```typescript
signAndSend(): void {
  // Validar formularios antes de guardar
  if (!this.validateInvoiceData()) {
    return;
  }

  // Activar estado de carga
  this.isProcessingInvoice = true;
  this.cdRef.detectChanges();

  const invoiceDto = this.buildInvoicePayload('AUTHORIZED');

  // ... código existente de autenticación ...

  this.purchaseProofService.register(invoiceDto).subscribe({
    next: (response) => {
      this.isProcessingInvoice = false;
      this.cdRef.detectChanges();

      // ✅ VERIFICAR: Respuesta exitosa (code === 1) y tiene idInvoice
      if (response?.code === 1 && response?.data?.idInvoice) {
        const invoiceId = response.data.idInvoice;

        // Snackbar de éxito mejorado
        this.snackBar.open('✅ Factura guardada correctamente', 'OK', {
          duration: 5000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
        this.logger.debug('Factura registrada exitosamente', response);

        // ✅ IMPRESIÓN AUTOMÁTICA DE TIRILLA
        // Esperar un breve momento para que el mensaje de éxito se muestre
        setTimeout(() => {
          this.printTicketAutomatically(invoiceId);
        }, 500);

      } else {
        // Respuesta exitosa pero sin idInvoice (caso raro)
        this.logger.warn('Respuesta exitosa pero sin idInvoice', response);
        this.snackBar.open('✅ Factura guardada correctamente', 'OK', {
          duration: 5000,
        });
      }

      // Opcional: Limpiar el formulario o redirigir
      // this.resetForm();
      // this.router.navigate(['/invoices']);
    },
    error: (err: HttpErrorResponse) => {
      // ... código de error existente ...
    },
  });
}

/**
 * ✅ NUEVO MÉTODO: Imprime la tirilla automáticamente
 * Solo se llama desde signAndSend() cuando la respuesta es exitosa
 */
private printTicketAutomatically(invoiceId: number): void {
  const url = `/billing/ticket/${invoiceId}`;
  const printWindow = window.open(url, '_blank', 'width=400,height=600');

  if (!printWindow) {
    this.snackBar.open('⚠️ No se pudo abrir la ventana de impresión. Verifique el bloqueador de ventanas emergentes.', 'OK', {
      duration: 5000,
    });
    this.logger.warn('No se pudo abrir ventana de impresión (bloqueador de popups)');
    return;
  }

  this.logger.debug('✅ Impresión automática iniciada', { invoiceId });
}

/**
 * ✅ MÉTODO OPCIONAL: Imprimir tirilla manualmente (desde botón)
 * Solo disponible si hay una factura recién creada
 */
printTicket(): void {
  if (!this.lastCreatedInvoiceId) {
    this.snackBar.open('⚠️ No hay factura disponible para imprimir', 'OK', {
      duration: 3000,
    });
    return;
  }

  this.printTicketAutomatically(this.lastCreatedInvoiceId);
}
```

**NOTA**: El método `saveDraft()` **NO debe llamar** a `printTicketAutomatically()`.

---

### **2. Actualizar TicketPrintComponent**

#### **`ticket-print.component.ts`** (ACTUALIZADO):

```typescript
import { Component, OnInit, AfterViewInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { InvoiceService } from "../../../_service/invoice/invoice.service";
import { InvoiceDto, InvoiceDetailDto } from "../../../_model/invoice/invoice-dto";
import { environment } from "../../../../environments/environment";
import { LoggerService } from "../../../_service/logger/logger.service";

@Component({
  selector: "app-ticket-print",
  templateUrl: "./ticket-print.component.html",
  styleUrls: ["./ticket-print.component.css"],
})
export class TicketPrintComponent implements OnInit, AfterViewInit {
  invoice: InvoiceDto | null = null;
  isLoading = true;
  error: string | null = null;

  // Datos de la empresa desde localStorage
  companyName: string = "";
  companyRuc: string = "";
  companyPhone: string = "";
  companyAddress: string = "";

  // ✅ Subtotales calculados desde invoiceDetails
  subtotalsByIva: { ivaPercent: number; subtotal: number; ivaValue: number }[] = [];
  subtotalSinImpuestos: number = 0;
  totalDescuento: number = 0;
  totalICE: number = 0;
  totalIVA: number = 0;

  constructor(private route: ActivatedRoute, private router: Router, private invoiceService: InvoiceService, private logger: LoggerService) {}

  ngOnInit(): void {
    const invoiceId = this.route.snapshot.paramMap.get("invoiceId");

    if (!invoiceId) {
      this.error = "No se proporcionó ID de factura";
      this.isLoading = false;
      return;
    }

    this.loadCompanyData();
    this.loadInvoice(Number(invoiceId));
  }

  ngAfterViewInit(): void {
    // Auto-imprimir cuando la página esté lista
    setTimeout(() => {
      window.print();
      // Opcional: cerrar ventana después de imprimir
      // setTimeout(() => window.close(), 1000);
    }, 500);
  }

  private loadCompanyData(): void {
    this.companyName = localStorage.getItem(environment.NAME_COMPANY) || "EMPRESA";
    this.companyRuc = localStorage.getItem(environment.IDENTIFICATION) || "";

    const contactsStr = localStorage.getItem("COMPANY_CONTACTS");
    if (contactsStr) {
      try {
        const contacts = JSON.parse(contactsStr);
        const phoneContact = contacts.find((c: any) => c.contactType === "TEL" && c.status);
        const addressContact = contacts.find((c: any) => c.contactType === "DIR" && c.status);

        this.companyPhone = phoneContact?.value || "";
        this.companyAddress = addressContact?.value || "";
      } catch (error) {
        this.logger.warn("Error al parsear COMPANY_CONTACTS", error);
      }
    }
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

          // ✅ CALCULAR SUBTOTALES POR % DE IVA
          this.calculateSubtotalsByIva();

          this.isLoading = false;
          this.logger.debug("✅ Factura cargada para impresión", this.invoice);
        } else {
          this.error = "No se pudo cargar la factura";
          this.isLoading = false;
        }
      },
      error: (err) => {
        this.logger.error("❌ Error al cargar factura para impresión", err);
        this.error = "Error al cargar la factura";
        this.isLoading = false;
      },
    });
  }

  /**
   * ✅ CALCULA SUBTOTALES POR % DE IVA (15%, 5%, 0%)
   * Basado en los invoiceDetails, agrupando por ivaPercentage de cada producto
   */
  private calculateSubtotalsByIva(): void {
    if (!this.invoice) return;

    const details = this.getInvoiceDetails();

    // Map para agrupar subtotales por % de IVA
    const subtotalsByIvaMap = new Map<number, number>([
      [15, 0], // IVA 15%
      [5, 0], // IVA 5%
      [0, 0], // IVA 0%
    ]);

    let subtotalSinImpuestos = 0;
    let totalDescuento = 0;
    let totalICE = 0;

    details.forEach((item: InvoiceDetailDto) => {
      // Obtener porcentaje de IVA del producto
      const ivaPercent = item.ivaPercentage || item.productDto?.ivaTaxType?.value || 0;

      // Calcular subtotal de la línea (precio × cantidad)
      const lineSubtotal = (item.unitPrice || 0) * (item.quantity || 1);

      // Descuento en valor monetario (el backend retorna descuento en valor, no %)
      const lineDiscount = item.discount || 0;

      // Base imponible después del descuento
      const baseImponible = lineSubtotal - lineDiscount;

      // Agrupar subtotal por % de IVA
      const currentSubtotal = subtotalsByIvaMap.get(ivaPercent) || 0;
      subtotalsByIvaMap.set(ivaPercent, currentSubtotal + baseImponible);

      // Acumular totales generales
      subtotalSinImpuestos += lineSubtotal;
      totalDescuento += lineDiscount;

      // ICE (si existe en el detalle, se calcula aparte)
      // Por ahora usamos el totalICE del invoice
    });

    // Calcular IVA por cada tarifa y construir array
    this.subtotalsByIva = [];
    let totalIVA = 0;

    subtotalsByIvaMap.forEach((subtotal, ivaPercent) => {
      const ivaValue = subtotal * (ivaPercent / 100);

      const subtotalRounded = Math.round(subtotal * 100) / 100;
      const ivaValueRounded = Math.round(ivaValue * 100) / 100;

      totalIVA += ivaValueRounded;

      // Solo agregar si el subtotal es mayor a 0
      if (subtotalRounded > 0) {
        this.subtotalsByIva.push({
          ivaPercent: ivaPercent,
          subtotal: subtotalRounded,
          ivaValue: ivaValueRounded,
        });
      }
    });

    // Ordenar por % de IVA descendente (15%, 5%, 0%)
    this.subtotalsByIva.sort((a, b) => b.ivaPercent - a.ivaPercent);

    // Asignar totales
    this.subtotalSinImpuestos = Math.round(subtotalSinImpuestos * 100) / 100;
    this.totalDescuento = this.invoice.discount || totalDescuento;
    this.totalICE = this.invoice.subtotalIce || 0;
    this.totalIVA = Math.round(totalIVA * 100) / 100;
  }

  /**
   * Obtiene el subtotal para un % de IVA específico
   */
  getSubtotalByIva(ivaPercent: number): number {
    const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
    return item?.subtotal || 0;
  }

  /**
   * Obtiene el IVA para un % específico
   */
  getIvaByPercent(ivaPercent: number): number {
    const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
    return item?.ivaValue || 0;
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return "";
    try {
      const date = new Date(dateStr);
      const day = String(date.getDate()).padStart(2, "0");
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const year = date.getFullYear();
      const hours = String(date.getHours()).padStart(2, "0");
      const minutes = String(date.getMinutes()).padStart(2, "0");
      return `${day}/${month}/${year}  ${hours}:${minutes}`;
    } catch (error) {
      return dateStr;
    }
  }

  formatAccessKey(accessKey: string | undefined): string {
    if (!accessKey) return "N/A";
    if (accessKey.length <= 30) return accessKey;
    return `${accessKey.substring(0, 15)}...${accessKey.substring(accessKey.length - 10)}`;
  }

  calculateChange(): number {
    if (!this.invoice?.payments) return 0;

    const cashPayment = this.invoice.payments.find((p) => p.paymentType?.toLowerCase().includes("efectivo") || p.paymentMethodCode === "01");

    if (!cashPayment) return 0;

    const paid = cashPayment.value || cashPayment.total || 0;
    const total = this.invoice.totalAmount || 0;
    return Math.max(0, paid - total);
  }

  getIdentificationTypeName(idType: number | undefined): string {
    if (!idType) return "";
    const types: { [key: number]: string } = {
      1: "RUC",
      2: "CEDULA",
      3: "PASAPORTE",
      4: "CONSUMIDOR FINAL",
    };
    return types[idType] || "OTRO";
  }

  getInvoiceDetails() {
    return this.invoice?.invoiceDetails || this.invoice?.details || [];
  }

  getPayments() {
    return this.invoice?.payments || [];
  }

  /**
   * ✅ Verifica si la factura tiene número (está autorizada)
   */
  hasInvoiceNumber(): boolean {
    return !!this.invoice?.invoiceNumber;
  }
}
```

---

#### **`ticket-print.component.html`** (ACTUALIZADO con condiciones y totales):

```html
<div class="ticket-container" *ngIf="!isLoading && !error && invoice">
  <!-- ENCABEZADO: Datos de la empresa -->
  <div class="ticket-header">
    <div class="company-name">{{ companyName }}</div>
    <div class="company-ruc">RUC: {{ companyRuc }}</div>
    <div class="company-address" *ngIf="companyAddress">{{ companyAddress }}</div>
    <div class="company-phone" *ngIf="companyPhone">Tel: {{ companyPhone }}</div>
  </div>

  <div class="separator">----------------------------------------</div>

  <!-- DATOS DE LA FACTURA -->
  <!-- ✅ CONDICIONAL: Solo mostrar si hay invoiceNumber -->
  <div class="invoice-info" *ngIf="hasInvoiceNumber()">
    <div class="invoice-line" *ngIf="invoice.invoiceNumber"><strong>FACTURA:</strong> {{ invoice.invoiceNumber }}</div>
    <div class="invoice-line"><strong>FECHA:</strong> {{ formatDate(invoice.issueDate) }}</div>
    <div class="invoice-line" *ngIf="invoice.accessKey"><strong>CLAVE:</strong> {{ formatAccessKey(invoice.accessKey) }}</div>
    <div class="invoice-line" *ngIf="invoice.authorizationNumber"><strong>AUTORIZACIÓN:</strong> {{ invoice.authorizationNumber.substring(0, 20) }}...</div>
  </div>

  <!-- Si no hay invoiceNumber, mostrar solo fecha -->
  <div class="invoice-info" *ngIf="!hasInvoiceNumber()">
    <div class="invoice-line"><strong>FECHA:</strong> {{ formatDate(invoice.issueDate) }}</div>
    <div class="invoice-line warning">
      <strong>BORRADOR - PENDIENTE DE AUTORIZACIÓN</strong>
    </div>
  </div>

  <div class="separator">----------------------------------------</div>

  <!-- DATOS DEL CLIENTE -->
  <div class="client-info">
    <div class="client-line"><strong>CLIENTE:</strong> {{ invoice.clientDto?.name || 'N/A' }}</div>
    <div class="client-line">
      <strong>{{ getIdentificationTypeName(invoice.clientDto?.idType) }}:</strong>
      {{ invoice.clientDto?.identification || 'N/A' }}
    </div>
    <div class="client-line" *ngIf="invoice.clientDto?.address"><strong>DIR:</strong> {{ invoice.clientDto.address }}</div>
    <div class="client-line" *ngIf="invoice.clientDto?.phone"><strong>TEL:</strong> {{ invoice.clientDto.phone }}</div>
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

  <!-- ✅ TOTALES DETALLADOS (según imagen) -->
  <div class="totals">
    <!-- SUBTOTAL 15% -->
    <div class="total-row" *ngIf="getSubtotalByIva(15) > 0">
      <span class="total-label">SUBTOTAL 15%:</span>
      <span class="total-value">${{ getSubtotalByIva(15).toFixed(2) }}</span>
    </div>

    <!-- SUBTOTAL 5% -->
    <div class="total-row" *ngIf="getSubtotalByIva(5) > 0">
      <span class="total-label">SUBTOTAL 5%:</span>
      <span class="total-value">${{ getSubtotalByIva(5).toFixed(2) }}</span>
    </div>

    <!-- SUBTOTAL 0% -->
    <div class="total-row" *ngIf="getSubtotalByIva(0) > 0">
      <span class="total-label">SUBTOTAL 0%:</span>
      <span class="total-value">${{ getSubtotalByIva(0).toFixed(2) }}</span>
    </div>

    <!-- SUBTOTAL SIN IMPUESTOS -->
    <div class="total-row">
      <span class="total-label">SUBTOTAL SIN IMPUESTOS:</span>
      <span class="total-value">${{ subtotalSinImpuestos.toFixed(2) }}</span>
    </div>

    <!-- DESCUENTO -->
    <div class="total-row" *ngIf="totalDescuento > 0">
      <span class="total-label">DESCUENTO:</span>
      <span class="total-value">-${{ totalDescuento.toFixed(2) }}</span>
    </div>

    <!-- ICE -->
    <div class="total-row" *ngIf="totalICE > 0">
      <span class="total-label">ICE:</span>
      <span class="total-value">${{ totalICE.toFixed(2) }}</span>
    </div>

    <!-- IVA 15% -->
    <div class="total-row" *ngIf="getIvaByPercent(15) > 0">
      <span class="total-label">IVA 15%:</span>
      <span class="total-value">${{ getIvaByPercent(15).toFixed(2) }}</span>
    </div>

    <!-- IVA 5% -->
    <div class="total-row" *ngIf="getIvaByPercent(5) > 0">
      <span class="total-label">IVA 5%:</span>
      <span class="total-value">${{ getIvaByPercent(5).toFixed(2) }}</span>
    </div>

    <!-- IVA 0% -->
    <div class="total-row" *ngIf="getIvaByPercent(0) > 0">
      <span class="total-label">IVA 0%:</span>
      <span class="total-value">${{ getIvaByPercent(0).toFixed(2) }}</span>
    </div>

    <!-- Propina 10% -->
    <div class="total-row" *ngIf="invoice.tip > 0">
      <span class="total-label">Propina 10%:</span>
      <span class="total-value">${{ invoice.tip.toFixed(2) }}</span>
    </div>

    <!-- Valor a pagar -->
    <div class="total-row total-final">
      <span class="total-label"><strong>Valor a pagar:</strong></span>
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
    <div class="legal-subtext">Consulte su factura electrónica en el portal del SRI</div>
    <div class="legal-subtext" *ngIf="invoice.environment">Ambiente: {{ invoice.environment === '1' ? 'PRODUCCIÓN' : 'PRUEBAS' }}</div>
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
```

---

#### **`ticket-print.component.css`** (agregar estilo para warning):

```css
/* ... estilos existentes ... */

.invoice-line.warning {
  color: #ff9800;
  font-weight: bold;
  text-align: center;
  margin-top: 4px;
}

/* ... resto de estilos igual que antes ... */
```

---

## ✅ RESUMEN DE CAMBIOS

### **1. Impresión Automática:**

- ✅ Se ejecuta automáticamente cuando `signAndSend()` retorna 200 con `idInvoice`
- ✅ NO se imprime cuando es borrador (`saveDraft()`)
- ✅ El método `printTicketAutomatically()` se llama desde `signAndSend()` después de guardar

### **2. Etiquetas Condicionales:**

- ✅ `FACTURA`, `CLAVE` y `AUTORIZACIÓN` solo se muestran si `invoice.invoiceNumber` existe
- ✅ Si no hay `invoiceNumber`, se muestra mensaje "BORRADOR - PENDIENTE DE AUTORIZACIÓN"

### **3. Totales Detallados:**

- ✅ SUBTOTAL 15%, SUBTOTAL 5%, SUBTOTAL 0% (calculados desde `invoiceDetails`)
- ✅ SUBTOTAL SIN IMPUESTOS
- ✅ DESCUENTO
- ✅ ICE
- ✅ IVA 15%, IVA 5%, IVA 0% (calculados desde subtotales)
- ✅ Propina 10%
- ✅ Valor a pagar

### **4. Cálculo de Subtotales:**

- ✅ Se calculan agrupando `invoiceDetails` por `ivaPercentage` de cada producto
- ✅ Se ordenan por porcentaje descendente (15%, 5%, 0%)
- ✅ Solo se muestran si el subtotal > 0

---

## 🎯 FLUJO COMPLETO ACTUALIZADO

1. Usuario completa factura → Clic en "Facturar"
2. `signAndSend()` → POST /api/invoice/register
3. Respuesta 200 OK con `{ code: 1, data: { idInvoice: 123, invoiceNumber: "..." } }`
4. ✅ **IMPRESIÓN AUTOMÁTICA**: Se abre `/billing/ticket/123`
5. Se carga factura del backend y se calculan subtotales por IVA
6. Se renderiza HTML con formato de ticket
7. Se ejecuta `window.print()` automáticamente
8. Usuario selecciona impresora EPSON y confirma
9. ✅ **Tirilla impresa** con todos los totales detallados

---

## 📋 CHECKLIST DE VALIDACIÓN

Antes de implementar:

- [ ] Verificar que el backend retorna `ivaPercentage` en `invoiceDetails`
- [ ] Verificar formato de respuesta: `{ code: 1, data: { idInvoice, invoiceNumber, ... } }`
- [ ] Verificar que `saveDraft()` NO llama a impresión automática
- [ ] Verificar que solo se imprime cuando `response.code === 1 && response.data.idInvoice`

---

## 🚀 LISTO PARA IMPLEMENTAR

Esta solución actualizada está completa y lista para aplicar. Incluye:

- ✅ Impresión automática solo en `signAndSend()`
- ✅ Etiquetas condicionales según `invoiceNumber`
- ✅ Totales detallados según la imagen proporcionada

¿Deseas que proceda con la implementación?

