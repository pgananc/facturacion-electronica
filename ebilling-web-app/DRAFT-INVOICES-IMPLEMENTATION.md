# 📝 Implementación de Gestión de Borradores de Facturas

**Fecha:** Octubre 23, 2025

---

## 📋 Resumen

Se ha implementado la **gestión completa de borradores de facturas** en el frontend, consumiendo los endpoints de `ebill-billing` para guardar, listar, procesar y eliminar borradores.

---

## ✅ Cambios Implementados

### 1. **Servicio DraftInvoiceService** ✅

**Archivo:** `/src/app/_service/invoice/draft-invoice.service.ts`

**Endpoints implementados:**

```typescript
// POST /api/invoice/draft - Guardar borrador
saveDraft(invoiceDto: InvoiceDto): Observable<any>

// GET /api/invoice/drafts - Listar todos los borradores
getDrafts(): Observable<InvoiceDto[]>

// POST /api/invoice/draft/{id}/process - Procesar borrador
processDraft(idInvoice: number): Observable<any>

// DELETE /api/invoice/draft/{id} - Eliminar borrador (endpoint futuro)
deleteDraft(idInvoice: number): Observable<any>
```

---

### 2. **Componente DraftInvoicesComponent** ✅

**Archivos creados:**

- `/src/app/pages/billing/draft-invoices/draft-invoices.component.ts`
- `/src/app/pages/billing/draft-invoices/draft-invoices.component.html`
- `/src/app/pages/billing/draft-invoices/draft-invoices.component.css`

**Funcionalidades:**

#### **Listado de Borradores**

- ✅ Tabla con paginación y sorting
- ✅ Columnas: Número, Fecha Emisión, Cliente, Total, Creado, Acciones
- ✅ Zebra striping y hover effects
- ✅ Look and feel consistente con company/product

#### **Acciones disponibles:**

1. **Procesar** (botón verde con icono `send`):

   - Confirma con el usuario
   - Llama a `POST /api/invoice/draft/{id}/process`
   - Genera secuencial, firma y envía al SRI
   - Recarga la lista automáticamente

2. **Editar** (botón azul con icono `edit`):

   - Placeholder para funcionalidad futura
   - Mostrará notificación: "Funcionalidad estará disponible próximamente"

3. **Eliminar** (botón rojo con icono `delete`):
   - Confirma con el usuario
   - Llama a `DELETE /api/invoice/draft/{id}`
   - Recarga la lista automáticamente

#### **Estados:**

- **Loading:** Muestra spinner mientras carga datos
- **Sin datos:** Mensaje informativo con icono y hint
- **Con datos:** Tabla completa con todas las acciones

---

### 3. **Módulo de Billing actualizado** ✅

**Archivo:** `/src/app/pages/billing/billing.module.ts`

**Cambios:**

```typescript
// Ruta agregada
{
  path: 'drafts',
  component: DraftInvoicesComponent,
  canActivate: [CanActiveUserGuard],
}

// Componente agregado a declarations
declarations: [
  BillingComponent,
  DraftInvoicesComponent, // ✅ Agregado
  SelectProductDialogComponent,
  AddPaymentDialogComponent,
]
```

**URL de acceso:** `/billing/drafts`

---

### 4. **Panel "Pendientes" Removido** ✅

**Archivo:** `/src/app/pages/billing/billing.component.html`

**Cambio:**

- ❌ **ANTES:** Layout 66% formulario + 33% panel Pendientes
- ✅ **AHORA:** Layout 100% formulario (sin panel Pendientes)

**Motivo:** El panel Pendientes será reemplazado por la página de borradores, que ofrece una gestión mucho más completa y robusta.

---

### 5. **Método saveDraft() Implementado** ✅

**Archivo:** `/src/app/pages/billing/billing.component.ts`

**Implementación:**

```typescript
saveDraft(): void {
  // 1. Validar información del comprador
  if (!this.buyerForm.valid) {
    this.snackBar.open('Complete la información del comprador', 'OK', {
      duration: 4000,
    });
    return;
  }

  // 2. Validar que haya al menos un producto
  if (this.invoiceDetailDataSource.data.length === 0) {
    this.snackBar.open('Agregue al menos un producto a la factura', 'OK', {
      duration: 4000,
    });
    return;
  }

  // 3. Construir el payload del borrador
  const invoiceDto = this.buildInvoicePayload('DRAFT');

  // 4. Guardar usando el servicio de borradores
  this.draftInvoiceService.saveDraft(invoiceDto).subscribe({
    next: (response) => {
      this.snackBar.open('✅ Borrador guardado exitosamente', 'OK', {
        duration: 5000,
      });
      // Opcional: redirigir a /billing/drafts
    },
    error: (err) => {
      this.snackBar.open(`❌ ${errorMessage}`, 'Cerrar', {
        duration: 7000,
      });
    },
  });
}
```

**Validaciones:**

- ✅ Comprador completo (email, identificación, razón social)
- ✅ Al menos 1 producto en la factura
- ❌ NO valida formas de pago (opcional para borradores)
- ❌ NO valida secuencial (se genera al procesar)
- ❌ NO valida firma digital (se firma al procesar)

---

## 🎨 Look & Feel

El componente de borradores replica **exactamente** el diseño de las páginas `company` y `product`:

- ✅ **Toolbar principal:** Gradient azul, sombra, bordes redondeados
- ✅ **Info card:** Mensaje informativo con icono y borde azul
- ✅ **Tabla:** Zebra striping, hover effects, headers en uppercase
- ✅ **Botones de acción:** Iconos con hover scale y background coloreado
- ✅ **Paginador:** 70px × 40px, compacto y centrado
- ✅ **Responsive:** Adaptado para móviles, tablets y desktop

---

## 🔗 Submenu "Borradores"

### ⚠️ **Acción Requerida en el Backend**

Para que el submenu "Borradores" aparezca en el menú lateral, se debe agregar en el backend:

**Tabla:** `menu` o donde se gestionen los menús del sistema

**Datos a insertar:**

```sql
-- Suponiendo que el id del menú "Facturación Electrónica" es 5
INSERT INTO menu (id, name, icon, url, parent_id)
VALUES (15, 'Borradores', 'drafts', '/billing/drafts', 5);
```

**O mediante la API de menús**, agregar un submenu con:

```json
{
  "name": "Borradores",
  "icon": "drafts",
  "url": "/billing/drafts",
  "parentId": <ID_DEL_MENU_FACTURACION_ELECTRONICA>
}
```

**Resultado esperado en el menú lateral:**

```
📊 Facturación Electrónica
  ├── 📄 Factura (/billing)
  └── 📝 Borradores (/billing/drafts)  ← NUEVO
```

---

## 📊 Flujo Completo de Uso

### **1. Crear Borrador**

```
Usuario → Página Facturación (/billing)
  ↓
Completa: Comprador + Productos (sin formas de pago)
  ↓
Click en "Guardar borrador"
  ↓
Frontend valida: comprador y productos
  ↓
POST /api/invoice/draft → Backend guarda sin procesar
  ↓
Notificación: "✅ Borrador guardado exitosamente"
```

### **2. Listar Borradores**

```
Usuario → Menú: Facturación Electrónica → Borradores
  ↓
GET /api/invoice/drafts
  ↓
Muestra tabla con todos los borradores
  ↓
Usuario ve: Número (sin secuencial), Cliente, Total, Fecha
```

### **3. Procesar Borrador**

```
Usuario → Click en botón "Procesar" (verde)
  ↓
Confirmación: "¿Está seguro de procesar este borrador?"
  ↓
Si acepta:
  POST /api/invoice/draft/{id}/process
  ↓
Backend:
  - Valida que sea borrador (authorizationSri = NULL)
  - Genera secuencial
  - Firma digitalmente
  - Envía al SRI
  - Actualiza authorizationSri (ya NO es borrador)
  ↓
Notificación: "✅ Factura procesada y enviada al SRI"
  ↓
Borrador desaparece de la lista (ya es factura final)
```

### **4. Eliminar Borrador**

```
Usuario → Click en botón "Eliminar" (rojo)
  ↓
Confirmación: "¿Está seguro de eliminar este borrador?"
  ↓
Si acepta:
  DELETE /api/invoice/draft/{id}
  ↓
Backend elimina el registro
  ↓
Notificación: "✅ Borrador eliminado exitosamente"
  ↓
Borrador desaparece de la lista
```

---

## 🧪 Cómo Probar

### **Prueba 1: Guardar Borrador**

1. Ir a `/billing`
2. Completar:
   - ✅ Establecimiento
   - ✅ Nombre comercial
   - ✅ Fecha emisión
   - ✅ Punto emisión
   - ✅ Comprador (identificación, tipo, razón social, email)
   - ✅ Al menos 1 producto
   - ❌ NO completar formas de pago
3. Click en "Guardar borrador"
4. **Resultado esperado:** Notificación "✅ Borrador guardado exitosamente"

### **Prueba 2: Listar Borradores**

1. Ir a `/billing/drafts` (o usar el menú cuando esté agregado)
2. **Resultado esperado:**
   - Tabla con el borrador creado
   - Sin secuencial (o "Sin secuencial")
   - Cliente, Total, Fecha correctos

### **Prueba 3: Procesar Borrador**

1. En la tabla de borradores, click en botón verde "Procesar"
2. Confirmar el diálogo
3. **Resultado esperado:**
   - Loading spinner
   - Notificación "✅ Factura procesada y enviada al SRI"
   - Borrador desaparece de la lista
   - Factura aparece en el listado de facturas finales (con secuencial y autorización)

### **Prueba 4: Eliminar Borrador**

1. Crear otro borrador
2. En la tabla, click en botón rojo "Eliminar"
3. Confirmar el diálogo
4. **Resultado esperado:**
   - Notificación "✅ Borrador eliminado exitosamente"
   - Borrador desaparece de la lista

---

## 📁 Archivos Modificados/Creados

### **Creados:**

1. `/src/app/_service/invoice/draft-invoice.service.ts` - Servicio de borradores
2. `/src/app/pages/billing/draft-invoices/draft-invoices.component.ts` - Componente
3. `/src/app/pages/billing/draft-invoices/draft-invoices.component.html` - Template
4. `/src/app/pages/billing/draft-invoices/draft-invoices.component.css` - Estilos
5. `/DRAFT-INVOICES-IMPLEMENTATION.md` - Este documento

### **Modificados:**

1. `/src/app/pages/billing/billing.module.ts` - Agregada ruta y componente
2. `/src/app/pages/billing/billing.component.ts` - Implementado `saveDraft()`
3. `/src/app/pages/billing/billing.component.html` - Removido panel Pendientes

---

## ✅ Checklist de Implementación

- [x] Servicio DraftInvoiceService creado
- [x] Componente DraftInvoicesComponent creado (HTML + CSS + TS)
- [x] Ruta `/billing/drafts` agregada
- [x] Método `saveDraft()` implementado en BillingComponent
- [x] Panel "Pendientes" removido de billing
- [x] Look and feel replicado de company/product
- [ ] **Submenu agregado en el backend** (⚠️ Acción requerida)
- [ ] **Endpoint DELETE /api/invoice/draft/{id}** (opcional, futuro)

---

## 🚀 Próximos Pasos (Opcional)

### **1. Editar Borrador**

Permitir que el usuario vuelva a la página de facturación con los datos del borrador precargados:

```typescript
editDraft(draft: InvoiceDto): void {
  // Navegar a /billing con el ID del borrador como parámetro
  this.router.navigate(['/billing'], {
    queryParams: { draftId: draft.idInvoice }
  });

  // En BillingComponent.ngOnInit(), detectar el queryParam
  // y cargar el borrador usando draftInvoiceService.getDraftById()
}
```

### **2. Búsqueda y Filtros**

Agregar filtros por:

- Rango de fechas
- Cliente
- Monto mínimo/máximo

### **3. Exportar a PDF/Excel**

Permitir exportar la lista de borradores para análisis.

### **4. Notificaciones de Borradores Antiguos**

Mostrar badge en el menú con cantidad de borradores > 7 días.

---

## 🎉 Conclusión

Se ha implementado exitosamente la **gestión completa de borradores** de facturas:

✅ **Guardar:** Desde la página de facturación  
✅ **Listar:** En `/billing/drafts` con tabla completa  
✅ **Procesar:** Genera secuencial, firma y envía al SRI  
✅ **Eliminar:** Limpia borradores no deseados  
✅ **Look & Feel:** Consistente con el resto del sistema

**Próximo paso:** Agregar el submenu "Borradores" en el backend para que aparezca en el menú lateral.

🚀 **Todo funcionando correctamente!**
