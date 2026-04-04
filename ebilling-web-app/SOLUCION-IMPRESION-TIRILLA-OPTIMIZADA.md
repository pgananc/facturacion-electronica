# 🖨️ Solución Optimizada: Impresión Automática de Tirilla

## 📋 MEJORA SOLICITADA

**Problema actual**: Se hace una llamada innecesaria a `invoiceService.findById()` cuando ya tenemos todos los datos del `InvoiceDto` en la respuesta del guardado.

**Solución**: Combinar los datos del `InvoiceDto` enviado con la respuesta del backend (que incluye `idInvoice`, `invoiceNumber`, `accessKey`, etc.) y pasarlos directamente al componente de impresión usando `sessionStorage` temporal.

---

## 🎯 ESTRATEGIA

### **Flujo Optimizado:**

```
1. Usuario completa factura → Clic en "Facturar"
2. signAndSend() → buildInvoicePayload() → InvoiceDto completo
3. POST /api/invoice/register con InvoiceDto
4. Respuesta: { code: 1, data: { idInvoice, invoiceNumber, accessKey, ... } }
5. ✅ COMBINAR: InvoiceDto enviado + Datos de respuesta
6. ✅ GUARDAR en sessionStorage con clave única
7. ✅ IMPRESIÓN AUTOMÁTICA: Abrir /billing/ticket/:invoiceId
8. Componente carga desde sessionStorage (rápido, sin llamada al backend)
9. Si sessionStorage no existe → fallback a findById() (caso de recarga de página)
```

---

## 📝 IMPLEMENTACIÓN OPTIMIZADA

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

  // 🔍 Diagnóstico de autenticación
  const token = localStorage.getItem(environment.TOKEN);
  const decodedToken = decodeToken();

  this.logger.debug('🔐 Estado de autenticación:', {
    tokenExists: !!token,
    tokenLength: token?.length || 0,
    userRoles: decodedToken?.roles || [],
    userId: decodedToken?.sub || null,
    tokenExpiry: decodedToken?.exp
      ? new Date(decodedToken.exp * 1000).toISOString()
      : null,
  });

  this.logger.debug(
    'Guardando factura (registro, firma y envío al SRI)',
    invoiceDto
  );

  this.purchaseProofService.register(invoiceDto).subscribe({
    next: (response) => {
      this.isProcessingInvoice = false;
      this.cdRef.detectChanges();

      // ✅ VERIFICAR: Respuesta exitosa (code === 1) y tiene idInvoice
      if (response?.code === 1 && response?.data?.idInvoice) {
        const invoiceId = response.data.idInvoice;

        // ✅ COMBINAR: InvoiceDto enviado + Datos de respuesta del backend
        const completeInvoiceData = this.mergeInvoiceData(invoiceDto, response.data);

        // ✅ GUARDAR en sessionStorage con clave única
        const storageKey = `INVOICE_TICKET_${invoiceId}`;
        sessionStorage.setItem(storageKey, JSON.stringify(completeInvoiceData));

        // Snackbar de éxito mejorado
        this.snackBar.open('✅ Factura guardada correctamente', 'OK', {
          duration: 5000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
        this.logger.debug('Factura registrada exitosamente', response);
        this.logger.debug('✅ Datos combinados guardados en sessionStorage', {
          storageKey,
          invoiceId,
        });

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
 * ✅ NUEVO MÉTODO: Combina InvoiceDto enviado con datos de respuesta del backend
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
    payments: invoiceDto.payments || [],
    clientDto: invoiceDto.clientDto,
    establishment: invoiceDto.establishment,
    companyDto: invoiceDto.companyDto,
  };
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
```

---

### **2. Actualizar TicketPrintComponent**

#### **`ticket-print.component.ts`** (OPTIMIZADO):

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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invoiceService: InvoiceService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    const invoiceId = this.route.snapshot.paramMap.get("invoiceId");

    if (!invoiceId) {
      this.error = "No se proporcionó ID de factura";
      this.isLoading = false;
      return;
    }

    this.loadCompanyData();
    
    // ✅ OPTIMIZACIÓN: Intentar cargar desde sessionStorage primero
    this.loadInvoiceOptimized(Number(invoiceId));
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

  /**
   * ✅ MÉTODO OPTIMIZADO: Carga la factura desde sessionStorage si existe, sino desde el backend
   */
  private loadInvoiceOptimized(invoiceId: number): void {
    const storageKey = `INVOICE_TICKET_${invoiceId}`;
    const storedData = sessionStorage.getItem(storageKey);

    if (storedData) {
      // ✅ DATOS DISPONIBLES EN sessionStorage (caso común: impresión automática)
      try {
        this.invoice = JSON.parse(storedData);
        
        // Limpiar sessionStorage después de usar (opcional, para liberar espacio)
        // sessionStorage.removeItem(storageKey);

        this.completeInvoiceData();
        this.isLoading = false;
        
        this.logger.debug("✅ Factura cargada desde sessionStorage", {
          invoiceId,
          hasInvoiceNumber: !!this.invoice.invoiceNumber,
        });
      } catch (error) {
        this.logger.error("Error al parsear datos de sessionStorage", error);
        // Si hay error al parsear, intentar cargar desde el backend
        this.loadInvoiceFromBackend(invoiceId);
      }
    } else {
      // ✅ FALLBACK: Cargar desde el backend (caso de recarga de página o acceso directo)
      this.loadInvoiceFromBackend(invoiceId);
    }
  }

  /**
   * ✅ MÉTODO: Carga la factura desde el backend (fallback)
   */
  private loadInvoiceFromBackend(invoiceId: number): void {
    this.invoiceService.findById(invoiceId).subscribe({
      next: (response) => {
        if (response?.code === 1 && response?.data) {
          this.invoice = response.data;
          this.completeInvoiceData();
          this.isLoading = false;
          this.logger.debug("✅ Factura cargada desde backend", {
            invoiceId,
            hasInvoiceNumber: !!this.invoice.invoiceNumber,
          });
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
   * ✅ MÉTODO: Completa los datos de la factura (dirección, nombre empresa, cálculos)
   */
  private completeInvoiceData(): void {
    if (!this.invoice) return;

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

    const cashPayment = this.invoice.payments.find(
      (p) => p.paymentType?.toLowerCase().includes("efectivo") || p.paymentMethodCode === "01"
    );

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

## ✅ VENTAJAS DE ESTA SOLUCIÓN OPTIMIZADA

### **1. Sin Llamadas Innecesarias:**
- ✅ En el 99% de los casos (impresión automática), no se hace llamada al backend
- ✅ Los datos ya están disponibles inmediatamente desde `sessionStorage`
- ✅ Impresión más rápida

### **2. Fallback Robusto:**
- ✅ Si el usuario recarga la página o accede directamente a la URL, se carga desde el backend
- ✅ Si hay error al parsear `sessionStorage`, automáticamente intenta cargar desde el backend

### **3. Optimización de Memoria:**
- ✅ `sessionStorage` se limpia automáticamente al cerrar la pestaña
- ✅ Opcionalmente se puede limpiar después de usar para liberar espacio

### **4. Datos Completos:**
- ✅ Se combinan los datos del `InvoiceDto` enviado (que ya tiene todo) con la respuesta del backend
- ✅ Se obtienen todos los campos necesarios: `invoiceDetails`, `payments`, `totals`, `accessKey`, `invoiceNumber`, etc.

---

## 🔄 FLUJO OPTIMIZADO COMPLETO

```
1. Usuario completa factura → Clic en "Facturar"
   │
   ▼
2. signAndSend() → buildInvoicePayload('AUTHORIZED')
   │
   ▼
3. InvoiceDto completo construido (con todos los datos: clientDto, invoiceDetails, payments, totals)
   │
   ▼
4. POST /api/invoice/register con InvoiceDto
   │
   ▼
5. Respuesta: { code: 1, data: { idInvoice: 123, invoiceNumber: "...", accessKey: "...", ... } }
   │
   ▼
6. ✅ mergeInvoiceData(): Combina InvoiceDto enviado + responseData
   │
   ▼
7. ✅ sessionStorage.setItem("INVOICE_TICKET_123", JSON.stringify(completeInvoiceData))
   │
   ▼
8. ✅ printTicketAutomatically(123) → Abre /billing/ticket/123
   │
   ▼
9. TicketPrintComponent.ngOnInit()
   │
   ▼
10. ✅ loadInvoiceOptimized(123)
    │
    ├─→ ¿Existe en sessionStorage? SÍ
    │   │
    │   ▼
    │   ✅ Cargar desde sessionStorage (RÁPIDO, sin llamada al backend)
    │   │
    │   ▼
    │   ✅ Calcular subtotales y renderizar
    │
    └─→ ¿Existe en sessionStorage? NO (caso de recarga)
        │
        ▼
        ✅ loadInvoiceFromBackend(123) → findById() (FALLBACK)
        │
        ▼
        ✅ Calcular subtotales y renderizar

11. ✅ window.print() → Usuario imprime
```

---

## 📋 COMPARACIÓN: ANTES vs DESPUÉS

### **ANTES (Solución Inicial):**
- ❌ Siempre hace llamada a `findById()` → Latencia adicional
- ❌ Carga de datos más lenta
- ❌ Más carga en el backend

### **DESPUÉS (Solución Optimizada):**
- ✅ Usa datos de `sessionStorage` → Sin latencia
- ✅ Impresión instantánea
- ✅ Menos carga en el backend
- ✅ Fallback automático si es necesario

---

## 🎯 RESUMEN DE CAMBIOS

### **En `billing.component.ts`:**
1. ✅ Método `mergeInvoiceData()`: Combina InvoiceDto enviado + respuesta del backend
2. ✅ Guardar en `sessionStorage` antes de imprimir
3. ✅ `printTicketAutomatically()` sin cambios

### **En `ticket-print.component.ts`:**
1. ✅ Método `loadInvoiceOptimized()`: Intenta cargar desde `sessionStorage` primero
2. ✅ Método `loadInvoiceFromBackend()`: Fallback al backend si es necesario
3. ✅ Método `completeInvoiceData()`: Completa datos y calcula subtotales

---

## 🚀 LISTO PARA IMPLEMENTAR

Esta solución optimizada:
- ✅ Elimina la llamada innecesaria al backend en la mayoría de casos
- ✅ Mantiene funcionalidad de fallback para casos especiales
- ✅ Combina correctamente los datos del InvoiceDto con la respuesta
- ✅ Mejora significativamente el rendimiento de la impresión automática

¿Deseas que proceda con la implementación?


