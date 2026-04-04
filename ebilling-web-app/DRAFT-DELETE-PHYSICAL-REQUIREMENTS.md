# 🗑️ Requerimientos: Eliminación Física de Borradores

**Fecha:** 2025-11-08  
**Módulo:** Backend - Invoice API  
**Endpoint:** `DELETE /api/invoice/draft/{idInvoice}`

---

## 📋 Contexto

Los **borradores de factura** son registros temporales que:
- **NO tienen** secuencial asignado
- **NO tienen** firma digital
- **NO han sido** enviados al SRI
- **NO tienen** autorización del SRI (`authorizationSri = NULL`)

Por lo tanto, estos registros **DEBEN ser eliminados físicamente** de la base de datos cuando el usuario lo solicite, ya que:
- No tienen valor contable/tributario
- No afectan reportes al SRI
- No tienen implicaciones legales
- Son solo "trabajo en progreso" del usuario

---

## 🎯 Requerimiento

### Endpoint a implementar o verificar:

```http
DELETE /api/invoice/draft/{idInvoice}
```

### Comportamiento esperado:

1. **Validar que la factura sea borrador:**
   ```sql
   SELECT authorizationSri FROM invoice WHERE id_invoice = ?
   ```
   - Si `authorizationSri IS NOT NULL` → **ERROR 400**: "No se puede eliminar una factura autorizada"
   - Si `authorizationSri IS NULL` → **Continuar con eliminación**

2. **Eliminación física en cascada:**
   ```sql
   -- 1. Eliminar detalles de factura
   DELETE FROM invoice_detail WHERE id_invoice = ?;
   
   -- 2. Eliminar pagos asociados
   DELETE FROM payment WHERE id_invoice = ?;
   
   -- 3. Eliminar la factura
   DELETE FROM invoice WHERE id_invoice = ?;
   ```

3. **Respuesta exitosa:**
   ```json
   {
     "message": "Borrador eliminado exitosamente",
     "idInvoice": 123
   }
   ```

4. **Respuestas de error:**
   - **404 Not Found:** Si el `idInvoice` no existe
   - **400 Bad Request:** Si la factura tiene `authorizationSri` (ya fue procesada)
   - **500 Internal Server Error:** Si falla la eliminación en DB

---

## 🔐 Validaciones de seguridad

```java
@DeleteMapping("/draft/{idInvoice}")
public ResponseEntity<?> deleteDraft(@PathVariable Long idInvoice) {
    // 1. Verificar que la factura existe
    Invoice invoice = invoiceRepository.findById(idInvoice)
        .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));
    
    // 2. Verificar que sea borrador (authorizationSri = NULL)
    if (invoice.getAuthorizationSri() != null) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("No se puede eliminar una factura autorizada"));
    }
    
    // 3. Verificar que pertenece al usuario/empresa actual (opcional, según tu lógica de seguridad)
    // validateOwnership(invoice, currentUser);
    
    // 4. Eliminar físicamente (cascada)
    invoiceRepository.delete(invoice);
    
    return ResponseEntity.ok(new DeleteDraftResponse("Borrador eliminado exitosamente", idInvoice));
}
```

---

## 📊 Consideraciones de base de datos

### Opción 1: Cascada en JPA (Recomendado)
```java
@Entity
@Table(name = "invoice")
public class Invoice {
    // ...
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceDetail> details;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;
}
```

### Opción 2: Cascada en base de datos
```sql
ALTER TABLE invoice_detail
ADD CONSTRAINT fk_invoice_detail_invoice
FOREIGN KEY (id_invoice) REFERENCES invoice(id_invoice)
ON DELETE CASCADE;

ALTER TABLE payment
ADD CONSTRAINT fk_payment_invoice
FOREIGN KEY (id_invoice) REFERENCES invoice(id_invoice)
ON DELETE CASCADE;
```

---

## ✅ Checklist para el Backend

- [ ] Endpoint `DELETE /api/invoice/draft/{idInvoice}` implementado
- [ ] Validación: la factura debe existir (404 si no existe)
- [ ] Validación: `authorizationSri` debe ser `NULL` (400 si no es borrador)
- [ ] Eliminación física de `invoice_detail` (cascada)
- [ ] Eliminación física de `payment` (cascada)
- [ ] Eliminación física de `invoice`
- [ ] Manejo de errores con mensajes claros
- [ ] Log de auditoría (opcional): "Usuario X eliminó borrador Y"
- [ ] Pruebas unitarias del endpoint
- [ ] Pruebas de integración con DB

---

## 🧪 Pruebas recomendadas

### Test 1: Eliminar borrador exitosamente
```http
DELETE /api/invoice/draft/123
Authorization: Bearer {token}

Expected Response: 200 OK
{
  "message": "Borrador eliminado exitosamente",
  "idInvoice": 123
}
```

### Test 2: Intentar eliminar factura autorizada
```http
DELETE /api/invoice/draft/456
Authorization: Bearer {token}

Expected Response: 400 Bad Request
{
  "error": "No se puede eliminar una factura autorizada"
}
```

### Test 3: Intentar eliminar factura inexistente
```http
DELETE /api/invoice/draft/99999
Authorization: Bearer {token}

Expected Response: 404 Not Found
{
  "error": "Factura no encontrada"
}
```

---

## 📞 Contacto

Si el endpoint no está implementado o tiene comportamiento diferente, por favor:
1. Implementar según este documento
2. Confirmar que la eliminación es **FÍSICA** (no lógica)
3. Confirmar que la cascada funciona correctamente
4. Notificar cuando esté listo para pruebas

---

## 🚨 IMPORTANTE

**¿Por qué eliminación física y no lógica?**

- ✅ **Borradores:** Son temporales, sin valor legal → **Eliminación física**
- ❌ **Facturas autorizadas:** Tienen valor legal/tributario → **NUNCA eliminar** (ni física ni lógica)

Los borradores son como "archivos no guardados" o "drafts de email". No tienen consecuencias legales ni contables, por lo que es seguro eliminarlos físicamente.

