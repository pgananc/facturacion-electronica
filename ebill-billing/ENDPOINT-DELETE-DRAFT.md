# 🗑️ Endpoint DELETE - Eliminar Borrador de Factura

**Fecha:** 6 de Diciembre 2025  
**Endpoint:** `DELETE /api/invoice/draft/{id}`  
**Propósito:** Eliminar un borrador de factura que NO ha sido autorizado por el SRI

---

## 📋 Descripción

Permite eliminar un borrador de factura (invoice) que aún no ha sido procesado ni autorizado por el SRI. Este endpoint es útil para limpiar borradores que ya no son necesarios o que fueron creados por error.

---

## 🔗 Endpoint

```
DELETE http://localhost:8480/api/invoice/draft/{id}
```

### Path Parameter

| Parámetro | Tipo | Descripción                          |
| --------- | ---- | ------------------------------------ |
| `id`      | Long | ID de la factura borrador a eliminar |

### Headers

```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

---

## ✅ Respuesta Exitosa

**HTTP Status:** `200 OK`

```json
{
  "code": 200,
  "status": "OK",
  "message": "Borrador eliminado exitosamente"
}
```

---

## ❌ Respuestas de Error

### 1. Borrador no encontrado

**HTTP Status:** `400 BAD REQUEST`

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Borrador no encontrado con ID: 999"
}
```

### 2. Factura ya autorizada (no se puede eliminar)

**HTTP Status:** `400 BAD REQUEST`

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "No se puede eliminar esta factura porque ya fue autorizada por el SRI"
}
```

### 3. Sin autenticación

**HTTP Status:** `401 UNAUTHORIZED`

```json
{
  "timestamp": "2025-12-06T18:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/invoice/draft/21"
}
```

---

## 🔐 Seguridad

- ✅ Requiere autenticación JWT (manejado por `ebill-auth-util`)
- ✅ Solo permite eliminar borradores (`authorizationSri == NULL`)
- ✅ Valida que la factura exista antes de intentar eliminarla
- ✅ Registra todas las operaciones en logs con emojis para fácil identificación

---

## 🧪 Ejemplos de Uso

### Ejemplo 1: Eliminar un borrador válido

**Request:**

```bash
curl -X DELETE http://localhost:8480/api/invoice/draft/21 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**

```json
{
  "code": 200,
  "status": "OK",
  "message": "Borrador eliminado exitosamente"
}
```

**Logs esperados:**

```
2025-12-06T18:30:00.000-05:00  INFO 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : 🗑️ Eliminando borrador con ID: 21
2025-12-06T18:30:00.050-05:00  INFO 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : ✅ Borrador 21 eliminado exitosamente
```

### Ejemplo 2: Intentar eliminar una factura ya autorizada

**Request:**

```bash
curl -X DELETE http://localhost:8480/api/invoice/draft/15 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "No se puede eliminar esta factura porque ya fue autorizada por el SRI"
}
```

**Logs esperados:**

```
2025-12-06T18:30:00.000-05:00  INFO 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : 🗑️ Eliminando borrador con ID: 15
2025-12-06T18:30:00.050-05:00 ERROR 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : ❌ No se puede eliminar la factura 15 porque ya fue autorizada por el SRI
2025-12-06T18:30:00.055-05:00 ERROR 12345 --- [nio-8480-exec-1] c.w.e.controller.InvoiceController       : ❌ Error al eliminar borrador: No se puede eliminar esta factura porque ya fue autorizada por el SRI
```

### Ejemplo 3: Intentar eliminar un borrador inexistente

**Request:**

```bash
curl -X DELETE http://localhost:8480/api/invoice/draft/999 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Borrador no encontrado con ID: 999"
}
```

**Logs esperados:**

```
2025-12-06T18:30:00.000-05:00  INFO 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : 🗑️ Eliminando borrador con ID: 999
2025-12-06T18:30:00.050-05:00 ERROR 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : ❌ Borrador con ID 999 no encontrado
2025-12-06T18:30:00.055-05:00 ERROR 12345 --- [nio-8480-exec-1] c.w.e.controller.InvoiceController       : ❌ Error al eliminar borrador: Borrador no encontrado con ID: 999
```

---

## 📝 Archivos Modificados

1. **`InvoiceController.java`**

   - Agregado método `deleteDraft(@PathVariable("id") Long idInvoice)`
   - Agregados imports: `DeleteMapping`, `HashMap`, `Map`

2. **`IInvoiceService.java`**

   - Agregada firma del método `void deleteDraft(Long idInvoice)`

3. **`InvoiceServiceImpl.java`**
   - Implementado método `deleteDraft(Long idInvoice)` con validaciones

---

## 🔍 Validaciones Implementadas

1. **Existencia del borrador:**

   - Verifica que la factura exista en la base de datos
   - Lanza `IllegalArgumentException` si no existe

2. **Estado de borrador:**

   - Verifica que `authorizationSri == NULL`
   - Lanza `IllegalArgumentException` si ya fue autorizada

3. **Eliminación en cascada:**
   - JPA elimina automáticamente los `InvoiceDetail` relacionados gracias a `CascadeType.ALL` y `orphanRemoval = true`

---

## 🎯 Casos de Uso

### Frontend - Tabla de Borradores

```typescript
async deleteDraft(draftId: number): Promise<void> {
  try {
    const response = await this.http.delete<{
      code: number;
      status: string;
      message: string;
    }>(`${this.apiUrl}/api/invoice/draft/${draftId}`).toPromise();

    if (response.code === 200) {
      this.toastr.success(response.message, 'Borrador Eliminado');
      this.loadDrafts(); // Recargar la lista
    }
  } catch (error) {
    if (error.status === 400) {
      this.toastr.error(error.error.message, 'Error');
    } else if (error.status === 401) {
      this.toastr.error('Sesión expirada', 'Error');
      this.router.navigate(['/login']);
    } else {
      this.toastr.error('Error al eliminar el borrador', 'Error');
    }
  }
}
```

---

## ⚠️ Notas Importantes

1. **No se requiere configuración adicional en `SecurityConfig`**

   - El `SecurityConfig` de `ebill-auth-util` ya maneja la autenticación para todos los endpoints bajo `/api/invoice/draft/**`

2. **Eliminación permanente**

   - La operación NO es reversible
   - Se recomienda mostrar un diálogo de confirmación en el frontend

3. **Eliminación en cascada automática**

   - Los detalles de la factura (`InvoiceDetail`) se eliminan automáticamente
   - No es necesario eliminarlos manualmente

4. **Logs con emojis**

   - 🗑️ = Inicio de eliminación
   - ✅ = Eliminación exitosa
   - ❌ = Error en eliminación

5. **Compatibilidad con frontend**
   - La respuesta incluye `code`, `status` y `message` para facilitar el manejo en el frontend
   - El código de error es `400` (no `404`) para mantener consistencia con otros endpoints

---

## ✅ Checklist de Implementación

- [x] Endpoint `DELETE /api/invoice/draft/{id}` en `InvoiceController`
- [x] Método `deleteDraft()` en `IInvoiceService`
- [x] Implementación en `InvoiceServiceImpl`
- [x] Validación de existencia del borrador
- [x] Validación de estado (no autorizado)
- [x] Manejo de errores con `try-catch`
- [x] Logs informativos con emojis
- [x] Respuesta JSON estructurada
- [x] Documentación completa

---

**Fin del documento**
