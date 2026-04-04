# 📄 Propuesta de Impresión de Tirilla de Factura
## Para Impresoras Térmicas EPSON TM-U220D (76mm)

---

## 🎯 OBJETIVO

Implementar funcionalidad para imprimir una **tirilla informativa** de factura en impresoras térmicas tipo EPSON TM-U220D (76mm de ancho). Esta tirilla **NO es un documento autorizado** por el SRI, solo sirve como comprobante informativo para el cliente.

---

## 📊 ANÁLISIS DEL FLUJO DE FACTURACIÓN

### **Punto de Generación del Número de Factura**

Basado en el código actual (`billing.component.ts`):

1. **Estado INICIAL (Formulario)**: 
   - ❌ NO tiene número de factura aún
   - El formulario está en memoria local (no guardado en BD)

2. **Estado BORRADOR (`saveDraft()`)**: 
   - ⚠️ **DEPENDE DEL BACKEND**:
     - Si el backend asigna número de factura al guardar borrador → ✅ SÍ hay número
     - Si el backend NO asigna número hasta autorizar → ❌ NO hay número
   - Estado: `status: "DRAFT"`

3. **Estado AUTORIZADO (`signAndSend()`)**: 
   - ✅ **SIEMPRE tiene número de factura** (`invoiceNumber: "001-001-000001234"`)
   - ✅ Tiene `accessKey` (clave de acceso del SRI)
   - ✅ Tiene `authorizationNumber` y `authorizationDate`
   - Estado: `status: "AUTHORIZED"`

### **Respuesta del Backend al Registrar Factura**

Según `BACKEND-INTEGRATION-FINAL.md`:

```json
{
  "code": 1,
  "message": "Factura registrada exitosamente",
  "data": {
    "idInvoice": 123,
    "invoiceNumber": "001-001-000001234",  // ← Generado por el backend
    "status": "AUTHORIZED" | "DRAFT"
  }
}
```

**CONCLUSIÓN**: El número de factura se genera **en el momento de guardar** (tanto borrador como autorizado), por lo tanto:
- ✅ **SÍ se debe tener el número de factura** para imprimir la tirilla
- ⚠️ **La tirilla solo puede imprimirse DESPUÉS de guardar** (borrador o autorizado)

---

## 🔄 CUÁNDO MOSTRAR/IMPRIMIR LA TIRILLA

### **Opción A: Solo después de autorizar (Recomendada)**

**Comportamiento**:
- El botón "Imprimir Tirilla" aparece **solo después de hacer clic en "Facturar"** (signAndSend) y que la respuesta sea exitosa
- La tirilla mostrará: `invoiceNumber`, `accessKey`, `authorizationDate`
- **Ventaja**: El cliente recibe la tirilla con todos los datos completos de la factura autorizada

### **Opción B: Después de guardar borrador O autorizar**

**Comportamiento**:
- El botón "Imprimir Tirilla" aparece después de `saveDraft()` o `signAndSend()` exitoso
- Si es borrador: muestra `invoiceNumber` (si existe) + mensaje "BORRADOR - PENDIENTE DE AUTORIZACIÓN"
- Si es autorizado: muestra todos los datos completos
- **Ventaja**: Permite imprimir una tirilla de referencia incluso antes de autorizar

**RECOMENDACIÓN**: Usar **Opción A** porque la tirilla es un comprobante físico que debe corresponder a una factura válida autorizada por el SRI.

---

## 🖨️ OPCIONES TÉCNICAS DE IMPRESIÓN

### **OPCIÓN 1: HTML + window.print() + CSS @media print**

#### **Descripción**
- Generar una página HTML con el contenido del ticket
- Usar CSS con `@media print` y `@page { size: 75mm auto }`
- Abrir en nueva ventana/pestaña y ejecutar `window.print()` automáticamente
- El usuario selecciona la impresora EPSON en el diálogo de impresión del navegador

#### **Implementación Técnica**
```typescript
// billing.component.ts
printTicket(invoiceId: number): void {
  const url = `/ticket/${invoiceId}`;
  const printWindow = window.open(url, '_blank', 'width=300,height=600');
  printWindow.onload = () => {
    printWindow.print();
    // Opcional: cerrar después de imprimir
    setTimeout(() => printWindow.close(), 1000);
  };
}
```

```html
<!-- ticket-print.component.html -->
<div class="ticket-container">
  <!-- Contenido del ticket con fuente monoespaciada -->
</div>
```

```css
/* ticket-print.component.css */
@media print {
  @page {
    size: 75mm auto;
    margin: 2mm 3mm;
  }
  body {
    font-family: 'Courier New', monospace;
    font-size: 10pt;
    width: 75mm;
  }
}
```

#### **✅ VENTAJAS**
1. ✅ **Simplicidad**: Implementación rápida, solo HTML/CSS
2. ✅ **Sin dependencias externas**: No requiere librerías adicionales
3. ✅ **Compatible con cualquier navegador**: Chrome, Edge, Firefox
4. ✅ **Fácil de depurar**: Se puede visualizar en el navegador antes de imprimir
5. ✅ **Reutilizable**: Puede servir también para vista previa en pantalla
6. ✅ **Flexible**: Fácil ajustar formato cambiando CSS

#### **❌ DESVENTAJAS**
1. ❌ **Requiere configuración manual**: El usuario debe:
   - Seleccionar la impresora EPSON en el diálogo
   - Configurar papel de 76mm (primera vez)
   - Desactivar encabezados/pies del navegador
2. ❌ **Control limitado**: No se pueden usar comandos ESC/POS nativos (corte de papel, negritas nativas, códigos de barras)
3. ❌ **Depende del driver de Windows**: El sistema operativo debe tener instalado el driver de la impresora
4. ❌ **Márgenes del navegador**: Pueden añadir espacios no deseados (aunque se pueden minimizar con CSS)
5. ❌ **No hay impresión automática directa**: Siempre aparece el diálogo de impresión

#### **🖨️ COMPATIBILIDAD CON EPSON TM-U220D**
- ✅ Funciona si la impresora está instalada como impresora genérica de Windows
- ⚠️ El formato de papel debe configurarse manualmente (76mm continuo)
- ⚠️ No hay control de corte de papel automático (el usuario debe cortar manualmente)

---

### **OPCIÓN 2: jsPDF (Generar PDF tipo ticket + Imprimir)**

#### **Descripción**
- Usar `jsPDF` (ya disponible en el proyecto para reportes) para generar un PDF de tipo ticket
- Dimensiones: 75mm de ancho × alto dinámico
- Fuente monoespaciada (Courier)
- Abrir el PDF en nueva pestaña y dejar que el usuario imprima desde el visor

#### **Implementación Técnica**
```typescript
// billing.component.ts
import { jsPDF } from 'jspdf';

printTicketPdf(invoiceId: number): void {
  // Cargar datos de la factura
  this.invoiceService.getById(invoiceId).subscribe(invoice => {
    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: [75, 297] // 75mm ancho, altura dinámica (se ajusta)
    });
    
    doc.setFont('courier');
    doc.setFontSize(8);
    
    // Encabezado empresa
    doc.text('EMPRESA XYZ S.A.', 37.5, 10, { align: 'center' });
    doc.text('RUC: 1234567890001', 37.5, 15, { align: 'center' });
    
    // Datos de factura
    doc.text(`FACTURA: ${invoice.invoiceNumber}`, 5, 25);
    doc.text(`FECHA: ${formatDate(invoice.issueDate)}`, 5, 30);
    
    // Detalle de productos
    let yPos = 40;
    invoice.invoiceDetails.forEach(item => {
      doc.text(`${item.quantity} x ${item.description}`, 5, yPos);
      doc.text(`$${item.subtotal.toFixed(2)}`, 70, yPos, { align: 'right' });
      yPos += 5;
    });
    
    // Totales
    doc.text(`TOTAL: $${invoice.totalAmount.toFixed(2)}`, 70, yPos + 10, { align: 'right' });
    
    // Leyenda legal
    doc.setFontSize(6);
    doc.text('DOCUMENTO NO VALIDO COMO', 37.5, yPos + 20, { align: 'center' });
    doc.text('COMPROBANTE DE VENTA AUTORIZADO', 37.5, yPos + 25, { align: 'center' });
    
    // Guardar y abrir
    const pdfBlob = doc.output('blob');
    const url = URL.createObjectURL(pdfBlob);
    window.open(url, '_blank');
  });
}
```

#### **✅ VENTAJAS**
1. ✅ **Control total del formato**: Coordenadas exactas (X, Y) para posicionar texto
2. ✅ **Formato consistente**: El PDF se verá igual en cualquier dispositivo
3. ✅ **Archivable**: El PDF se puede guardar como respaldo
4. ✅ **Ya disponible**: `jsPDF` ya está en el proyecto (usado en reportes)
5. ✅ **No depende de CSS del navegador**: El formato está fijo en el PDF

#### **❌ DESVENTAJAS**
1. ❌ **Más complejo de desarrollar**: Requiere calcular coordenadas manualmente para cada elemento
2. ❌ **Limitado a texto simple**: No tiene soporte avanzado para tablas complejas (aunque se puede simular con coordenadas)
3. ❌ **Mismo problema de impresión**: El usuario todavía debe seleccionar la impresora en el diálogo
4. ❌ **Tamaño del PDF**: Si la factura tiene muchos productos, el PDF será largo (no es problema, pero hay que manejarlo)
5. ❌ **No hay comandos ESC/POS**: Igual que HTML, no puede controlar funciones nativas de la impresora

#### **🖨️ COMPATIBILIDAD CON EPSON TM-U220D**
- ✅ Funciona igual que Opción 1 (requiere driver de Windows)
- ⚠️ Formato de papel debe configurarse manualmente
- ⚠️ Sin control de corte de papel

---

### **OPCIÓN 3: ESC/POS con Servicio Intermedio (Node.js o .NET)**

#### **Descripción**
- Backend genera comandos **ESC/POS** (formato de impresión nativo de impresoras térmicas EPSON)
- Un servicio local (Node.js o .NET) en la máquina del cliente recibe los comandos vía HTTP/WebSocket
- El servicio envía los comandos directamente al puerto USB/Puerto Serie de la impresora
- El frontend solo envía la solicitud de impresión

#### **Arquitectura**
```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Angular   │  HTTP   │   Servicio   │  USB/   │   Impresora │
│  Frontend   │────────>│   Intermedio │  Serial │   EPSON     │
│             │ Request │  (Node/.NET) │────────>│   TM-U220D  │
└─────────────┘         └──────────────┘         └─────────────┘
```

#### **Implementación Técnica (Ejemplo con Node.js)**

**Frontend (Angular)**:
```typescript
// billing.component.ts
printTicketEscPos(invoiceId: number): void {
  this.invoiceService.getById(invoiceId).subscribe(invoice => {
    // Enviar solicitud al servicio local
    this.http.post('http://localhost:3000/print-ticket', {
      invoice: invoice,
      printer: 'EPSON_TM-U220D' // Nombre de la impresora configurada
    }).subscribe({
      next: () => this.snackBar.open('✅ Tirilla enviada a impresora', 'OK'),
      error: () => this.snackBar.open('❌ Error al imprimir', 'OK')
    });
  });
}
```

**Servicio Local (Node.js)**:
```javascript
// print-service.js (corre en localhost:3000)
const express = require('express');
const escpos = require('escpos');
const app = express();

app.post('/print-ticket', (req, res) => {
  const { invoice } = req.body;
  
  // Conectar a impresora (USB o Serial)
  const device = new escpos.USB();
  const printer = new escpos.Printer(device);
  
  device.open((error) => {
    printer
      .font('a')
      .align('ct')
      .text('EMPRESA XYZ S.A.')
      .text('RUC: 1234567890001')
      .text('--------------------------------')
      .align('lt')
      .text(`FACTURA: ${invoice.invoiceNumber}`)
      .text(`FECHA: ${invoice.issueDate}`)
      // ... más contenido
      .text('DOCUMENTO NO VALIDO COMO')
      .text('COMPROBANTE DE VENTA AUTORIZADO')
      .cut() // ← Corte automático de papel
      .close();
  });
});
```

#### **✅ VENTAJAS**
1. ✅ **Control total**: Comandos nativos ESC/POS (corte de papel, negritas, códigos de barras, QR)
2. ✅ **Impresión automática directa**: No aparece diálogo del navegador
3. ✅ **Mejor calidad de impresión**: Usa las capacidades completas de la impresora
4. ✅ **Corte de papel automático**: `printer.cut()` corta el papel al finalizar
5. ✅ **Soporte de códigos de barras/QR**: Puede imprimir QR de clave de acceso nativamente
6. ✅ **Formato optimizado**: Diseñado específicamente para impresoras térmicas

#### **❌ DESVENTAJAS**
1. ❌ **Complejidad de instalación**: Requiere instalar y mantener un servicio en cada PC
2. ❌ **Requisitos de infraestructura**: Cada punto de venta debe tener:
   - Node.js o .NET Runtime instalado
   - El servicio corriendo como servicio de Windows
   - Permisos para acceder al puerto USB/Serial
3. ❌ **Mantenimiento**: Si hay actualizaciones, hay que actualizar el servicio en todos los clientes
4. ❌ **Desarrollo adicional**: Requiere desarrollar y mantener código fuera de la aplicación Angular
5. ❌ **Soporte técnico**: Más complejidad al diagnosticar problemas de impresión
6. ❌ **Seguridad**: El servicio local debe estar configurado para aceptar solo conexiones del localhost

#### **🖨️ COMPATIBILIDAD CON EPSON TM-U220D**
- ✅ **ÓPTIMA**: Usa comandos nativos de la impresora
- ✅ Corte de papel automático
- ✅ Códigos de barras/QR nativos
- ✅ Negritas y formato avanzado

---

## 🏆 RECOMENDACIÓN FINAL

### **Para el contexto actual (Angular Web App):**

#### **OPCIÓN RECOMENDADA: Opción 1 (HTML + window.print())**

**Razones**:
1. ✅ **Implementación rápida**: Se puede desarrollar en 1-2 días
2. ✅ **Sin infraestructura adicional**: No requiere instalar nada en los clientes
3. ✅ **Funciona "out of the box"**: Solo necesita que la impresora esté instalada en Windows
4. ✅ **Mantenimiento mínimo**: Todo el código está en la aplicación Angular
5. ✅ **Suficiente para el caso de uso**: Una tirilla informativa no requiere funciones avanzadas

**Contras a considerar**:
- El usuario debe configurar la impresora la primera vez (formato de papel 76mm)
- No hay corte automático de papel (manual)

#### **OPCIÓN FUTURA (Si se requiere impresión avanzada):**

**Opción 3 (ESC/POS)** puede implementarse más adelante si:
- Se requiere corte automático de papel
- Se necesita imprimir códigos QR de clave de acceso
- Se quiere eliminar el diálogo de impresión del navegador
- Hay presupuesto para desarrollar y mantener el servicio local

---

## 📋 PLAN DE IMPLEMENTACIÓN (Opción 1 - Recomendada)

### **Fase 1: Componente de Impresión**

1. **Crear componente `TicketPrintComponent`**
   - Ruta: `/ticket/:invoiceId`
   - Cargar datos de factura desde `invoiceService.getById(invoiceId)`
   - Renderizar HTML con formato de ticket (76mm, fuente monoespaciada)

2. **CSS para impresión**
   - `@page { size: 75mm auto; margin: 2mm 3mm; }`
   - Fuente: `Courier New` o `Consolas`
   - Ancho fijo: `75mm`

3. **Lógica de auto-impresión**
   - En `ngAfterViewInit`, ejecutar `window.print()`
   - Opcional: Cerrar ventana después de imprimir

### **Fase 2: Integración en BillingComponent**

1. **Botón "Imprimir Tirilla"**
   - Aparece después de `signAndSend()` exitoso
   - Guardar `invoiceId` en variable de componente
   - Llamar a `printTicket(invoiceId)`

2. **Manejo de estados**
   - Solo mostrar botón si `status === 'AUTHORIZED'`
   - Deshabilitar botón mientras se procesa la factura

### **Fase 3: Formato del Ticket**

**Layout propuesto (42 caracteres de ancho)**:

```
        EMPRESA XYZ S.A.
        RUC: 1234567890001
    Calle Principal 123, Quito
        Tel: 02-1234567
----------------------------------------
FACTURA: 001-001-000001234
FECHA: 18/12/2025  14:32
CLAVE: 14102025011234567890...
----------------------------------------
CLIENTE: JUAN PÉREZ
ID: 0102030405
DIR: Av. Ejemplo 456
----------------------------------------
CANT DESCRIPCION           P.UNIT  TOTAL
 1   PRODUCTO A             100.00  100.00
 2   PRODUCTO B              50.00  100.00
----------------------------------------
SUBTOTAL 12%:                   200.00
IVA 12%:                          24.00
TOTAL:                           224.00
----------------------------------------
FORMA DE PAGO:
EFECTIVO:                       250.00
VUELTO:                          26.00
----------------------------------------
    DOCUMENTO NO VALIDO COMO
  COMPROBANTE DE VENTA AUTORIZADO
  
  Consulte su factura electrónica
  en el portal del SRI
----------------------------------------
```

---

## ❓ PREGUNTAS PENDIENTES

1. **¿El backend asigna número de factura al guardar borrador?**
   - Si SÍ → Se puede imprimir tirilla de borrador
   - Si NO → Solo imprimir después de autorizar

2. **¿Cuántos puntos de venta usarán esta funcionalidad?**
   - Si son muchos → Opción 1 (HTML) es más fácil de mantener
   - Si son pocos → Opción 3 (ESC/POS) puede ser viable

3. **¿Se requiere corte automático de papel?**
   - Si SÍ → Opción 3 (ESC/POS) es necesaria
   - Si NO → Opción 1 (HTML) es suficiente

4. **¿Se necesita imprimir QR de clave de acceso?**
   - Si SÍ → Opción 3 (ESC/POS) o usar librería de QR en PDF
   - Si NO → Opción 1 (HTML) es suficiente

---

## 📝 CONCLUSIÓN

**Para empezar**: Implementar **Opción 1 (HTML + window.print())** porque:
- ✅ Es la solución más rápida y simple
- ✅ No requiere cambios en infraestructura
- ✅ Funciona con la impresora EPSON TM-U220D si está instalada en Windows
- ✅ Cubre el caso de uso (tirilla informativa)

**Para el futuro**: Si se requiere más funcionalidad, migrar a **Opción 3 (ESC/POS)** con servicio intermedio.

**Sobre el número de factura**: ✅ **SÍ se debe tener el número** (se genera al guardar), por lo tanto la tirilla solo puede imprimirse **después de guardar/autorizar** la factura.


