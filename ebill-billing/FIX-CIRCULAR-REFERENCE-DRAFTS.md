# Fix: Referencias Circulares en GET /api/invoice/drafts

## 🐛 Problema

Al intentar llamar al endpoint `GET /api/invoice/drafts`, se producía un error de serialización JSON:

```
Could not write JSON: Document nesting depth (1001) exceeds the maximum allowed (1000, from `StreamWriteConstraints.getMaxNestingDepth()`)
```

### Causa Raíz

El endpoint retornaba directamente las entidades JPA `Invoice`, que contienen relaciones bidireccionales:

- `Invoice` → `List<InvoiceDetail>` (OneToMany)
- `InvoiceDetail` → `Invoice` (ManyToOne)

Al serializar con Jackson, se producía un ciclo infinito:

```
Invoice → InvoiceDetails → Invoice → InvoiceDetails → ... (infinito)
```

## ✅ Solución

Se implementó el patrón **DTO (Data Transfer Object)** para evitar las referencias circulares.

### Cambios Realizados:

1. **Creado `InvoiceDraftResponseDto.java`**

   - DTO específico para la respuesta de borradores
   - Contiene solo los datos necesarios sin relaciones profundas
   - Campos: idInvoice, idCompany, idClient, fechas, números, montos, retryCount, paymentMethodName, status

2. **Actualizado `IInvoiceService.java`**

   - Cambiado tipo de retorno: `List<Invoice>` → `List<InvoiceDraftResponseDto>`

3. **Actualizado `InvoiceServiceImpl.java`**

   - Método `getDraftInvoices()` ahora mapea entidades a DTOs
   - Agregado método privado `mapToDraftResponseDto()` para conversión
   - Usa `paymentMethod.getPaymentMethodType()` en lugar de campo inexistente

4. **Actualizado `InvoiceController.java`**

   - Endpoint retorna `ResponseEntity<List<InvoiceDraftResponseDto>>`
   - Import del nuevo DTO

5. **Actualizada documentación `DRAFT-INVOICE-ENDPOINTS.md`**
   - Reflejados los cambios en el tipo de respuesta
   - Agregado ejemplo de respuesta JSON actualizado
   - Documentadas las ventajas del uso de DTOs

## 📊 Resultado

✅ El endpoint `GET /api/invoice/drafts` ahora funciona correctamente
✅ Sin referencias circulares
✅ Serialización JSON exitosa
✅ Performance mejorado (solo datos necesarios)
✅ Desacoplamiento entre capa de persistencia y API

## 🎯 Lecciones Aprendidas

1. **Nunca retornar entidades JPA directamente** en endpoints REST
2. **Usar DTOs** para controlar exactamente qué datos se exponen
3. **Evitar relaciones bidireccionales** en respuestas JSON
4. **Mapear entidades a DTOs** en la capa de servicio

## 🔍 Testing

Puedes probar el endpoint corregido con:

```bash
curl -X GET http://localhost:8080/api/invoice/drafts
```

Respuesta esperada:

```json
[
  {
    "idInvoice": 123,
    "idCompany": 1,
    "idClient": 100,
    "issueDate": "2025-10-22T10:00:00",
    "creationDate": "2025-10-22T10:00:00",
    "invoiceNumber": "001-001-123456789",
    "environment": "PRODUCCION",
    "additionalInformation": "Información adicional",
    "totalAmount": 150.0,
    "tip": 0.0,
    "subtotalZero": 0.0,
    "subtotalIva": 150.0,
    "subtotalIce": 0.0,
    "discount": 0.0,
    "retryCount": 0,
    "paymentMethodName": "EFECTIVO",
    "status": true
  }
]
```

---

**Fecha:** Octubre 2025  
**Estado:** ✅ Resuelto  
**Impacto:** Crítico → Solucionado
