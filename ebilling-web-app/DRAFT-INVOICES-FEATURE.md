# ✅ Funcionalidad de Borradores de Facturas

**Fecha:** 18 de Octubre 2025  
**Estado:** ✅ FRONTEND COMPLETADO

---

## 📋 Resumen

Se ha completado la implementación frontend de la funcionalidad de **Borradores de Facturas**, que permite:

1. **Listar borradores** guardados
2. **Procesar borradores** (firmar digitalmente y enviar al SRI)
3. **Editar borradores** (cargar en el formulario de facturación para continuar editando)
4. **Eliminar borradores**

---

## 🎨 Interfaz de Usuario

### Página: `/billing/drafts` (Listado de Borradores)

**Columnas de la tabla:**

- **# Factura**: ID interno del borrador
- **Cliente**: Razón social o nombre del comprador
- **RUC/Cédula**: Identificación del comprador
- **Fecha Creación**: Fecha en que se guardó el borrador
- **Total**: Monto total de la factura
- **Acciones**:
  - **▶ Procesar** (play_arrow): Genera secuencial, firma digitalmente y envía al SRI
  - **✏️ Editar** (edit): Carga el borrador en el formulario de facturación
  - **🗑️ Eliminar** (delete): Elimina el borrador permanentemente

**Características:**

- Paginación (5, 10, 25, 50 registros por página)
- Ordenamiento por columnas
- Mensaje cuando no hay borradores
- Loading spinner durante operaciones

---

## 🔧 Servicios Implementados

### `DraftInvoiceService` (`src/app/_service/invoice/draft-invoice.service.ts`)

#### Métodos:

1. **`saveDraft(invoiceDto: InvoiceDto): Observable<any>`**

   - Guarda una factura como borrador sin validar firma digital ni enviar al SRI
   - **Endpoint:** `POST /api/invoice/draft`

2. **`getDrafts(): Observable<InvoiceDto[]>`**

   - Obtiene todas las facturas en estado borrador
   - **Endpoint:** `GET /api/invoice/drafts`
   - **Criterio:** `authorizationSri = NULL`

3. **`getDraftById(idInvoice: number): Observable<InvoiceDto>`** ✨ **NUEVO**

   - Obtiene un borrador específico por su ID (para edición)
   - **Endpoint:** `GET /api/invoice/draft/{idInvoice}`

4. **`processDraft(idInvoice: number): Observable<any>`**

   - Procesa un borrador: genera secuencial, firma digitalmente y envía al SRI
   - **Endpoint:** `POST /api/invoice/draft/{idInvoice}/process`

5. **`deleteDraft(idInvoice: number): Observable<any>`**
   - Elimina un borrador
   - **Endpoint:** `DELETE /api/invoice/draft/{idInvoice}`

---

## 📦 Modelos Actualizados

### `InvoiceDto` (`src/app/_model/invoice/invoice-dto.ts`)

Se agregaron campos opcionales para facilitar la carga de borradores:

```typescript
export interface InvoiceDto {
  // ... campos existentes ...

  // ✨ NUEVOS: Campos adicionales del cliente
  buyerIdentification?: string;
  buyerIdType?: number;
  buyerName?: string;
  buyerAddress?: string;
  buyerPhone?: string;
  buyerMail?: string;

  // ✨ NUEVOS: Alias para facilitar compatibilidad
  details?: InvoiceDetailDto[]; // Alias para invoiceDetails
  payments?: PaymentDto[]; // Array de formas de pago
  authorizationSri?: string; // Autorización del SRI (null = borrador)
  idClient?: number; // ID del cliente en la BD
}
```

### `InvoiceDetailDto` (extendido)

```typescript
export interface InvoiceDetailDto {
  // ... campos existentes ...

  // ✨ NUEVOS: Campos adicionales
  code?: string; // Código del producto
  description?: string; // Descripción del producto
  ivaPercentage?: number; // Porcentaje de IVA
}
```

### `PaymentDto` ✨ **NUEVO**

```typescript
export interface PaymentDto {
  idPayment?: number;
  idPaymentMethod?: number;
  paymentMethod?: string; // Nombre de la forma de pago
  paymentType?: string; // Tipo de forma de pago
  paymentMethodCode?: string; // Código de la forma de pago
  total?: number; // Monto de esta forma de pago
  value?: number; // Alias para total
  term?: number;
  timeUnit?: string;
  time?: string; // Alias para timeUnit
}
```

---

## 🔄 Flujo de Edición de Borrador

### 1. Usuario hace clic en "Editar" (✏️) en la tabla de borradores

```typescript
editDraft(draft: InvoiceDto): void {
  this.router.navigate(['/billing'], {
    state: { draftId: draft.idInvoice }
  });
}
```

### 2. `BillingComponent.ngOnInit()` detecta el `draftId` en el router state

```typescript
ngOnInit() {
  // ... código existente ...

  const navigation = this.router.getCurrentNavigation();
  const draftId = navigation?.extras?.state?.['draftId'] as number | undefined;

  if (draftId) {
    this.loadDraftForEditing(draftId);
  }
}
```

### 3. `loadDraftForEditing()` carga el borrador desde el backend

```typescript
loadDraftForEditing(draftId: number): void {
  this.draftInvoiceService.getDraftById(draftId).subscribe({
    next: (draft: InvoiceDto) => {
      // 1. Cargar datos del comprador en buyerForm
      this.buyerForm.patchValue({ ... });

      // 2. Cargar detalles de productos en detailsForm (FormArray)
      draft.details?.forEach(detail => {
        this.items.push(this.fb.group({ ... }));
        this.invoiceDetailDataSource.data.push({ ... });
      });

      // 3. Cargar formas de pago en paymentsForm (FormArray)
      draft.payments?.forEach(payment => {
        this.paymentItems.push(this.fb.group({ ... }));
        this.paymentDataSource.data.push({ ... });
      });

      // 4. Recalcular totales
      this.calculateTotals();

      // 5. Notificar al usuario
      this.snackBar.open('📝 Borrador cargado...', 'OK');
    },
    error: (err) => {
      this.snackBar.open('❌ Error al cargar borrador', 'Cerrar');
      this.router.navigate(['/billing/drafts']);
    }
  });
}
```

---

## 🔄 Flujo de Procesamiento de Borrador

### 1. Usuario hace clic en "Procesar" (▶) en la tabla de borradores

```typescript
processDraft(draft: InvoiceDto): void {
  // Confirmar con el usuario
  const confirmProcess = confirm(
    `¿Está seguro que desea procesar esta factura?\n\n` +
    `Cliente: ${draft.buyerName}\n` +
    `Total: $${draft.totalAmount?.toFixed(2)}\n\n` +
    `Se generará el secuencial, se firmará y se enviará al SRI.`
  );

  if (!confirmProcess) return;

  // Llamar al backend
  this.draftInvoiceService.processDraft(draft.idInvoice!).subscribe({
    next: (response) => {
      this.snackBar.open('✅ Factura procesada exitosamente', 'OK');
      this.loadDrafts(); // Recargar lista
    },
    error: (err) => {
      this.snackBar.open('❌ Error al procesar borrador', 'Cerrar');
    }
  });
}
```

---

## 🛠️ Endpoints de Backend Requeridos

### ✅ Implementados (según BACKEND-INTEGRATION-FINAL.md):

1. **`POST /api/invoice/draft`** - Guardar borrador ✅
2. **`GET /api/invoice/drafts`** - Listar borradores ✅
3. **`POST /api/invoice/draft/{idInvoice}/process`** - Procesar borrador ✅
4. **`DELETE /api/invoice/draft/{idInvoice}`** - Eliminar borrador ✅

### ❓ Por Confirmar:

5. **`GET /api/invoice/draft/{idInvoice}`** - Obtener borrador por ID ❓

   **Request:** `GET /api/invoice/draft/123`

   **Response esperada:**

   ```json
   {
     "idInvoice": 123,
     "idCompany": 1,
     "buyerIdentification": "1234567890001",
     "buyerIdType": 1,
     "buyerName": "Cliente ABC S.A.",
     "buyerAddress": "Av. Principal 123",
     "buyerPhone": "0999999999",
     "buyerMail": "cliente@example.com",
     "idClient": 5,
     "totalAmount": 3525.0,
     "discount": 300.0,
     "tip": 270.0,
     "subtotalZero": 3000.0,
     "subtotalIva": 405.0,
     "subtotalIce": 150.0,
     "subtotalDiscount": 300.0,
     "status": "DRAFT",
     "authorizationSri": null,
     "details": [
       {
         "idInvoiceDetail": 456,
         "idProduct": 1,
         "code": "MB-001",
         "description": "MACBOOK PRO M4 512GB",
         "unitPrice": 3000.0,
         "discount": 300.0,
         "subtotal": 2700.0,
         "quantity": 1,
         "ivaPercentage": 15
       }
     ],
     "payments": [
       {
         "idPayment": 789,
         "idPaymentMethod": 1,
         "paymentMethod": "Efectivo",
         "paymentType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
         "paymentMethodCode": "01",
         "total": 3525.0,
         "term": null,
         "timeUnit": null
       }
     ],
     "establishment": {
       "idEstablishment": 1,
       "code": "001",
       "address": "Av. Principal"
     },
     "issueDate": "2025-10-14T15:30:00.000Z",
     "additionalInformation": "Empresa XYZ S.A."
   }
   ```

   **Notas:**

   - El response debe incluir `details` (o `invoiceDetails`) con los detalles de productos
   - El response debe incluir `payments` con el array de formas de pago
   - Los campos `buyerXXX` deben venir poblados para facilitar la carga del formulario
   - Si el endpoint ya existe pero retorna una estructura diferente, informar para ajustar el frontend

---

## ✅ Completado en Frontend

- [x] Servicio `DraftInvoiceService` con todos los métodos
- [x] Componente `DraftInvoicesComponent` (lista de borradores)
- [x] Método `processDraft()` con confirmación y manejo de errores
- [x] Método `editDraft()` con navegación por router state
- [x] Método `loadDraftForEditing()` en `BillingComponent`
- [x] Actualización de modelos `InvoiceDto`, `InvoiceDetailDto`, `PaymentDto`
- [x] Tabla con paginación, ordenamiento y tooltips
- [x] Botones de acción (Procesar, Editar, Eliminar)
- [x] Manejo de estados de carga (isLoading, spinner)
- [x] Notificaciones al usuario (snackbar)

---

## 🔍 Pruebas Pendientes

### Flujo de Edición:

1. Crear un borrador desde `/billing` (click en "Guardar borrador")
2. Ir a `/billing/drafts`
3. Click en "Editar" (✏️)
4. Verificar que el formulario de facturación se cargue con:
   - Datos del cliente
   - Productos en la tabla
   - Formas de pago
   - Totales recalculados
5. Modificar algo y volver a guardar o procesar

### Flujo de Procesamiento:

1. Ir a `/billing/drafts`
2. Click en "Procesar" (▶)
3. Confirmar en el diálogo
4. Verificar que:
   - Se genera el secuencial
   - Se firma digitalmente
   - Se envía al SRI
   - El borrador desaparece de la lista (ya no es borrador)

### Flujo de Eliminación:

1. Ir a `/billing/drafts`
2. Click en "Eliminar" (🗑️)
3. Confirmar en el diálogo
4. Verificar que el borrador se elimina de la lista

---

## 📝 Notas Técnicas

### Detección de cambios (ChangeDetection)

Ambos componentes usan `ChangeDetectionStrategy.OnPush` para optimizar el rendimiento:

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})
```

Por lo tanto, se llama explícitamente `this.cdRef.markForCheck()` después de:

- Cargar datos del backend
- Modificar `MatTableDataSource`
- Actualizar variables que afectan la vista

### Navegación con estado (Router State)

La navegación de "Editar" pasa el `draftId` como estado del router (no como query param):

```typescript
this.router.navigate(["/billing"], {
  state: { draftId: draft.idInvoice },
});
```

Esto se lee en `ngOnInit()` usando `getCurrentNavigation()`:

```typescript
const navigation = this.router.getCurrentNavigation();
const draftId = navigation?.extras?.state?.["draftId"];
```

**Importante:** El estado del router solo está disponible durante la navegación inicial. Si el usuario refresca la página (`F5`), se perderá el `draftId`.

### FormArray para productos y pagos

Tanto los productos como las formas de pago se manejan con `FormArray` para permitir múltiples items:

```typescript
get items(): FormArray {
  return this.detailsForm.get('items') as FormArray;
}

get paymentItems(): FormArray {
  return this.paymentsForm.get('items') as FormArray;
}
```

Al cargar un borrador, se limpia el `FormArray` existente y se agregan los items del borrador:

```typescript
while (this.items.length) {
  this.items.removeAt(0);
}

draft.details?.forEach(detail => {
  this.items.push(this.fb.group({ ... }));
});
```

---

## 🚨 Posibles Issues

### 1. Endpoint `GET /api/invoice/draft/{idInvoice}` no existe

**Síntoma:** Error 404 al hacer clic en "Editar"

**Solución Backend:** Crear endpoint en el controlador de facturas:

```java
@GetMapping("/draft/{idInvoice}")
public ResponseEntity<InvoiceDto> getDraftById(@PathVariable Long idInvoice) {
    // Buscar factura por ID
    // Verificar que authorizationSri == null (es borrador)
    // Mapear a InvoiceDto incluyendo:
    //   - details (productos)
    //   - payments (formas de pago)
    //   - campos buyerXXX del cliente
    // Retornar DTO
}
```

### 2. Estructura de Response diferente

**Síntoma:** Frontend no carga correctamente los datos del borrador

**Solución:** Ajustar el mapping en `loadDraftForEditing()` o solicitar cambios en el backend para que retorne los campos esperados.

### 3. Múltiples formas de pago no se cargan

**Síntoma:** Solo se carga la primera forma de pago

**Solución:** Verificar que el backend retorne el array completo de `payments` en el response de `GET /draft/{idInvoice}`.

---

## ✅ Checklist de Validación

### Frontend:

- [x] Tabla de borradores con datos mockeados
- [x] Paginación y ordenamiento funcional
- [x] Botones de acción con iconos correctos
- [x] Tooltips descriptivos
- [x] Loading spinner
- [x] Manejo de errores con snackbar
- [x] Navegación a `/billing` con estado
- [x] Carga de borrador en formulario

### Backend (Pendiente):

- [ ] Endpoint `GET /api/invoice/drafts` retorna lista correcta ✅
- [ ] Endpoint `POST /api/invoice/draft/{id}/process` procesa correctamente ✅
- [ ] Endpoint `DELETE /api/invoice/draft/{id}` elimina correctamente ✅
- [ ] Endpoint `GET /api/invoice/draft/{id}` retorna borrador completo ❓
- [ ] Response incluye array `details` con productos
- [ ] Response incluye array `payments` con formas de pago
- [ ] Response incluye campos `buyerXXX` del cliente

---

## 📞 Siguiente Paso

**Pregunta para el backend:**

> ¿Ya existe el endpoint `GET /api/invoice/draft/{idInvoice}` para obtener un borrador específico?
>
> Si existe, ¿cuál es la estructura exacta del response? Necesito confirmar que incluye:
>
> - Array `details` (o `invoiceDetails`) con los productos
> - Array `payments` con las formas de pago
> - Campos `buyerIdentification`, `buyerName`, `buyerAddress`, etc. del cliente
>
> Si no existe, necesito que se implemente con la estructura descrita en este documento (sección "Endpoints de Backend Requeridos").

---

**Fin del documento**
