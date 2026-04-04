# 🧾 Integración Completa del Módulo de Facturación

**Fecha:** 14 de Octubre 2025  
**Estado:** ✅ INTEGRACIÓN COMPLETADA

---

## 📋 Resumen de Cambios

### 1. ✅ **Frontend mejorado con cálculos automáticos**

- IVA global modificable (por defecto 15%)
- Descuento e ICE como porcentajes (0-100%)
- Propina 10% opcional con valor visible
- Totales calculados en tiempo real
- Validaciones completas antes de enviar

### 2. ✅ **Integración con Backend (puerto 8480)**

- Endpoints configurados en `proxy.conf.json`
- Métodos `signAndSend()` y `saveDraft()` completamente implementados
- Payload estructurado con toda la información necesaria
- Manejo robusto de errores

### 3. ✅ **Validaciones implementadas**

- Formulario de cabecera completo
- Información del comprador válida
- Al menos un producto agregado
- Al menos una forma de pago
- Valores numéricos correctos

---

## 🔧 Configuración

### **Proxy actualizado** (`proxy.conf.json`)

```json
{
  "/api/purchase-proof": {
    "target": "http://localhost:8480",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  },
  "/api/invoice": {
    "target": "http://localhost:8480",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

### **Environment** (`environment.ts`)

```typescript
export const environment = {
  production: false,
  HOST_EBILLING: "http://localhost:8480",
  // ...
};
```

---

## 📤 Estructura del Payload Enviado

### **Al hacer clic en "Firmar y enviar":**

```json
{
  "companyId": 1,
  "establishmentId": 1,
  "invoicePointId": 1,
  "header": {
    "branchOffice": 1,
    "legalName": "Empresa XYZ S.A.",
    "issuedDate": "2025-10-14T15:30:00.000Z",
    "emissionPoint": 1,
    "dispatchGuide": "",
    "commercialNegotiable": false,
    "tip10": true
  },
  "buyer": {
    "identification": "1234567890",
    "idType": 2,
    "businessName": "Cliente ABC",
    "address": "Calle 123",
    "phone": "099999999",
    "email": "cliente@example.com"
  },
  "items": [
    {
      "code": 1,
      "quantity": 1,
      "description": "MACBOOK PRO M4",
      "unitPrice": 3000.0,
      "discount": 10.0,
      "totalValue": 2700.0,
      "ice": 5.0
    }
  ],
  "payments": [
    {
      "paymentType": "Efectivo",
      "value": 5000.0,
      "term": null,
      "time": null
    }
  ],
  "totals": {
    "subtotal": 3000.0,
    "totalDiscount": 300.0,
    "subtotalWithDiscount": 2700.0,
    "ivaPercentage": 15,
    "iva": 405.0,
    "ice": 150.0,
    "tip": 270.0,
    "total": 3525.0
  },
  "draft": false,
  "metadata": {
    "createdBy": "admin",
    "createdAt": "2025-10-14T15:30:00.000Z",
    "globalIvaPercentage": 15,
    "hasTip": true
  }
}
```

---

## 🚀 Endpoints del Backend

### **1. Firmar y Enviar al SRI**

```
POST http://localhost:8480/api/purchase-proof/sign-and-send
Content-Type: application/json
Authorization: Bearer {token}
```

**Response esperado:**

```json
{
  "code": 1,
  "message": "Factura firmada y enviada correctamente",
  "data": {
    "invoiceNumber": "001-001-000001234",
    "authorizationNumber": "1234567890123456789012345678901234567890123456789",
    "authorizationDate": "2025-10-14T15:30:00.000Z",
    "xml": "...",
    "pdf": "..."
  }
}
```

### **2. Guardar como Borrador**

```
POST http://localhost:8480/api/purchase-proof/draft
Content-Type: application/json
Authorization: Bearer {token}
```

**Response esperado:**

```json
{
  "code": 1,
  "message": "Borrador guardado exitosamente",
  "data": {
    "id": 123,
    "invoiceNumber": "DRAFT-001",
    "status": "DRAFT"
  }
}
```

---

## 🧪 Cómo Probar la Integración

### **Paso 1: Asegúrate de que el backend esté corriendo**

```bash
# En el directorio del backend (ebill-billing)
lsof -i :8480

# Deberías ver algo como:
# java    62786 usuario  200u  IPv6 0x123456  0t0  TCP *:8480 (LISTEN)
```

### **Paso 2: Reinicia el servidor de desarrollo del frontend**

⚠️ **IMPORTANTE:** Debes reiniciar para que tome el nuevo `proxy.conf.json`

```bash
# 1. Detener el servidor actual (Ctrl+C en la terminal)

# 2. Reiniciar con:
npm start
```

### **Paso 3: Completa una factura de prueba**

1. **Navega a:** `http://localhost:4200/billing`
2. **Completa:**
   - ✅ Establecimiento y Punto de emisión (auto-cargados)
   - ✅ Fecha de emisión
   - ✅ Nombre comercial
3. **Agrega un comprador:**
   - ✅ Identificación y tipo
   - ✅ Razón social
   - ✅ Email
4. **Agrega productos:**
   - ✅ Buscar en listado
   - ✅ Seleccionar uno o varios
   - ✅ Ajustar descuentos/ICE si es necesario
5. **Agrega forma de pago:**
   - ✅ Click en "Añadir forma de pago"
   - ✅ Seleccionar tipo y valor

### **Paso 4: Prueba los botones**

#### **Opción A: Guardar Borrador**

```
Click en: "Guardar sin firmar"

✅ Validación mínima (info básica + productos)
📤 POST a: /api/purchase-proof/draft
📋 Payload con: draft: true
```

#### **Opción B: Firmar y Enviar**

```
Click en: "Firmar y enviar"

✅ Validación completa (cabecera + comprador + productos + pagos)
📤 POST a: /api/purchase-proof/sign-and-send
📋 Payload completo con totales
🔐 Firma digital aplicada
📨 Envío al SRI
```

---

## 🐛 Debugging

### **Ver logs del frontend**

```javascript
// En la consola del navegador (F12), busca:
[DEBUG] Enviando factura para firmar y enviar
[DEBUG] Factura enviada exitosamente

// O si hay error:
[ERROR] Error al firmar/enviar factura
```

### **Ver logs del backend**

```bash
# En la terminal donde está corriendo el backend, verás:
2025-10-14 15:30:00 INFO  c.w.e.controller.InvoiceController - Recibida solicitud de factura
2025-10-14 15:30:05 INFO  c.w.e.service.InvoiceService - Firmando factura...
2025-10-14 15:30:10 INFO  c.w.e.service.SRIService - Enviando al SRI...
```

### **Prueba directa con cURL**

```bash
# Obtén tu token del sessionStorage (F12 > Application > Session Storage > token)
TOKEN="tu_token_aqui"

# Prueba el endpoint
curl -X POST http://localhost:8480/api/purchase-proof/draft \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "companyId": 1,
    "establishmentId": 1,
    "invoicePointId": 1,
    "header": {},
    "buyer": {},
    "items": [],
    "payments": [],
    "draft": true
  }'
```

---

## 📊 Validaciones Implementadas

### **En `signAndSend()`:**

| Validación                      | Mensaje                                     |
| ------------------------------- | ------------------------------------------- |
| Formulario cabecera incompleto  | "Complete la información de la cabecera..." |
| Formulario comprador incompleto | "Complete la información del comprador"     |
| Sin productos                   | "Agregue al menos un producto..."           |
| Sin formas de pago              | "Agregue al menos una forma de pago"        |

### **En `saveDraft()`:**

| Validación        | Mensaje                                              |
| ----------------- | ---------------------------------------------------- |
| Sin info básica   | "Complete al menos la información básica..."         |
| Sin productos     | "...y agregue productos"                             |
| Error del backend | Muestra el mensaje específico del error del servidor |

---

## 🔄 Flujo Completo

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Usuario completa el formulario de factura               │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Click en "Firmar y enviar" o "Guardar sin firmar"       │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Validaciones del frontend                                │
│    ✓ Formularios completos                                  │
│    ✓ Productos agregados                                    │
│    ✓ Formas de pago definidas                               │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Construcción del payload                                 │
│    - companyId, establishmentId, invoicePointId             │
│    - header, buyer, items, payments                         │
│    - totals (calculados automáticamente)                    │
│    - metadata (usuario, fecha, IVA%, propina)              │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Envío al backend (puerto 8480)                           │
│    POST /api/purchase-proof/sign-and-send                   │
│    POST /api/purchase-proof/draft                           │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Procesamiento en el backend                              │
│    - Validación de datos                                    │
│    - Firma digital (solo sign-and-send)                     │
│    - Envío al SRI (solo sign-and-send)                      │
│    - Guardado en base de datos                              │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. Response al frontend                                     │
│    ✅ Success: "Factura enviada correctamente"              │
│    ❌ Error: Mensaje específico del problema                │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Checklist de Integración

- [x] Environment configurado con `HOST_EBILLING: 8480`
- [x] Proxy configurado para `/api/purchase-proof` y `/api/invoice`
- [x] Service `InvoiceService` con métodos `signAndSend()` y `saveDraft()`
- [x] Método `validateInvoiceData()` implementado
- [x] Método `buildInvoicePayload()` implementado con toda la información
- [x] Validaciones completas antes de enviar
- [x] Manejo de errores robusto con mensajes específicos
- [x] Logs de debug para troubleshooting
- [x] Payload incluye todos los totales calculados
- [x] Payload incluye metadata (usuario, fecha, configuración)
- [x] Conversión de fechas a ISO 8601
- [x] Conversión de valores string a number donde sea necesario

---

## 🎯 Próximos Pasos Opcionales

### **1. Agregar indicador de carga (loading)**

```typescript
signAndSend(): void {
  this.isLoading = true; // Mostrar spinner

  this.purchaseProofService.signAndSend(invoicePayload).subscribe({
    next: (response) => {
      this.isLoading = false;
      // ...
    },
    error: (err) => {
      this.isLoading = false;
      // ...
    }
  });
}
```

### **2. Limpiar formulario después de enviar**

```typescript
private resetForm(): void {
  this.invoiceForm.reset();
  this.buyerForm.reset();
  this.detailsForm.reset();
  this.invoiceDetailDataSource.data = [];
  this.paymentsDataSource.data = [];
  this.calculateTotals();
}
```

### **3. Redirigir a página de éxito**

```typescript
next: (response) => {
  this.snackBar.open("✅ Factura enviada correctamente", "OK", {
    duration: 3000,
  });

  // Redirigir después de 2 segundos
  setTimeout(() => {
    this.router.navigate(["/invoices", response.data.invoiceNumber]);
  }, 2000);
};
```

### **4. Descargar XML/PDF**

```typescript
downloadInvoice(format: 'xml' | 'pdf'): void {
  // Si el backend devuelve base64 en el response
  const blob = this.base64ToBlob(response.data[format],
    format === 'xml' ? 'application/xml' : 'application/pdf');

  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `factura-${invoiceNumber}.${format}`;
  a.click();
}
```

---

## 📞 Contacto y Soporte

- **Frontend:** Angular 18.2.3 en puerto 4200
- **Backend:** Spring Boot 3.4.5 en puerto 8480
- **Database:** PostgreSQL 14.17 en puerto 5433

**¿Necesitas ayuda?** Revisa los logs en:

- Browser Console (F12)
- Terminal del backend
- Network tab en DevTools

---

**¡Integración completada exitosamente!** 🎉
