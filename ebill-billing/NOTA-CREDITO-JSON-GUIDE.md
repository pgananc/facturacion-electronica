# 📋 Guía de JSON para Nota de Crédito

## Campos Estrictamente Necesarios

### ✅ Campos Obligatorios (@Nonnull)

1. **`companyDto`** - Datos de la empresa

   - `identification` (RUC)
   - `name` (Razón social)
   - `status`
   - Otros campos opcionales pero recomendados: `contacts`, `forcedToAccounting`, `regime`, etc.

2. **`clientDto`** - Datos del cliente

   - `identification` (Cédula/RUC)
   - `name`
   - `status`
   - Otros campos opcionales: `address`, `email`, `phone`, etc.

3. **`reason`** - Motivo de la nota de crédito ⚠️ **OBLIGATORIO**
   - Ejemplos: "Devolución de productos defectuosos", "Error en facturación", "Descuento no aplicado", etc.

### ✅ Campos Específicos de Nota de Crédito

4. **`originalInvoiceId`** - ID de la factura original en la base de datos

   - Tipo: `Long`
   - Ejemplo: `123`

5. **`originalInvoiceNumber`** - Número de factura original (formato: estab-ptoEmi-secuencial)

   - Tipo: `String`
   - Ejemplo: `"001-001-000000001"`

6. **`originalIssueDate`** - Fecha de emisión de la factura original

   - Tipo: `String` (ISO DateTime)
   - Ejemplo: `"2025-11-25T22:22:20.316Z"`

7. **`documentTypeModified`** - Tipo de documento modificado

   - Tipo: `String`
   - Valores: `"01"` (Factura), `"03"` (Liquidación de compra), etc.
   - Por defecto: `"01"` si no se especifica

8. **`originalAccessKey`** - Clave de acceso de la factura original (opcional si se tiene `originalInvoiceId`)
   - Tipo: `String`
   - Ejemplo: `"2511250001172263396100110010010000000011234567890"`

### ✅ Campos de Detalles y Totales

9. **`creditNoteDetails`** o **`details`** - Lista de productos/servicios a devolver/modificar

   - Tipo: `Array<InvoiceDetailDto>`
   - Cada detalle debe incluir:
     - `idProduct`
     - `productDto` (con todos los datos del producto)
     - `quantity`
     - `unitPrice`
     - `discount`
     - `subtotal`

10. **`establishment`** - Establecimiento de emisión

    - `name`
    - `code` (3 dígitos: "001")
    - `address`
    - `type` ("MATRIZ" | "SUCURSAL")
    - `issuancePoint.code` (3 dígitos)
    - `issuancePoint.idIssuancePoint`

11. **`totalAmount`** - Monto total de la nota de crédito

    - Tipo: `BigDecimal`
    - Ejemplo: `3048.08`

12. **`valorModificacion`** - Valor total de la modificación

    - Tipo: `BigDecimal`
    - Generalmente igual a `totalAmount`
    - Ejemplo: `3048.08`

13. **`subtotalIva`** - Subtotal con IVA

    - Tipo: `BigDecimal`
    - Ejemplo: `2650.5`

14. **`subtotalZero`** - Subtotal sin IVA (0% IVA)

    - Tipo: `BigDecimal`
    - Ejemplo: `0`

15. **`subtotalIce`** - Subtotal con ICE

    - Tipo: `BigDecimal`
    - Ejemplo: `0`

16. **`discount`** - Descuento total

    - Tipo: `BigDecimal`
    - Ejemplo: `0`

17. **`subtotalDiscount`** - Subtotal de descuentos
    - Tipo: `BigDecimal`
    - Ejemplo: `0`

### ✅ Campos Opcionales

18. **`issueDate`** - Fecha de emisión de la nota de crédito

    - Tipo: `String` (ISO DateTime)
    - Si no se especifica, se usa la fecha actual
    - Ejemplo: `"2025-11-25T22:30:00.000Z"`

19. **`additionalInformation`** - Información adicional

    - Tipo: `String`
    - Ejemplo: `"WALLPARISOFT"`

20. **`idClient`** - ID del cliente (opcional, se puede obtener de `clientDto.idClient`)

21. **`idCompany`** - ID de la empresa (opcional, se puede obtener de `companyDto.idCompany`)

## ❌ Campos que NO se Necesitan (a diferencia de Factura)

- **`paymentMethod`** / **`paymentMethods`** - Las notas de crédito no tienen formas de pago
- **`tip`** - No aplica en notas de crédito
- **`idPaymentMethod`** - No aplica

## 📝 Ejemplo Completo

Ver archivo `ejemplo-nota-credito.json` para un ejemplo completo basado en la factura proporcionada.

## 🔍 Validaciones Importantes

1. **`reason`** es obligatorio y no puede estar vacío
2. **`originalInvoiceId`** o **`originalAccessKey`** debe existir para referenciar la factura original
3. **`originalInvoiceNumber`** debe seguir el formato: `estab-ptoEmi-secuencial` (ej: "001-001-000000001")
4. **`documentTypeModified`** debe ser un código válido según el SRI (por defecto "01" para Factura)
5. Los totales deben ser consistentes: `totalAmount = subtotalIva + subtotalZero + subtotalIce - discount`
6. **`valorModificacion`** generalmente debe ser igual a `totalAmount`

## 🚀 Endpoint

**POST** `/api/credit-notes`

```bash
curl -X POST http://localhost:8480/api/credit-notes \
  -H "Content-Type: application/json" \
  -d @ejemplo-nota-credito.json
```

## 📌 Notas Adicionales

- La nota de crédito se genera automáticamente con un número secuencial basado en el punto de emisión
- Se genera automáticamente la clave de acceso (accessKey) de 49 dígitos
- El XML se firma digitalmente antes de enviarse al SRI
- Una vez autorizada, se genera el PDF (RIDE) y se envía por email al cliente


