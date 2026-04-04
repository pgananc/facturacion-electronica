# Corrección: IVA calculado sobre (Subtotal + ICE)

## Problema Detectado

El sistema estaba calculando el IVA **solo sobre el subtotal**, sin incluir el ICE en la base imponible.

### Ejemplo del Error

**Datos de la factura:**

- Subtotal sin impuestos: $1,485.00
- ICE: $74.25
- IVA 15%: ?

**Cálculo INCORRECTO (anterior):**

```
IVA = 1485 × 0.15 = $222.75 ❌
Total = 1485 + 74.25 + 222.75 + 148.5 = $1,930.50
```

**Cálculo CORRECTO (según normativa ecuatoriana):**

```
Base imponible IVA = Subtotal + ICE
Base imponible IVA = 1485 + 74.25 = $1,559.25
IVA = 1559.25 × 0.15 = $233.89 ✅
Total = 1485 + 74.25 + 233.89 + 148.5 = $1,941.64 ✅
```

**Diferencia:** $11.14 de IVA no calculado

## Normativa Ecuatoriana

Según la normativa tributaria del SRI de Ecuador:

> **Artículo 65 LRTI**: El IVA se calcula sobre la base imponible total, que incluye:
>
> - El precio o valor de los bienes transferidos o servicios prestados
> - **Los impuestos, tasas por servicios y demás gastos legalmente imputables al precio** (incluyendo ICE)

**En resumen**: Cuando un producto tiene ICE, el IVA debe calcularse sobre **(Precio + ICE)**, no solo sobre el precio.

## Solución Implementada

### 1. Cálculo del IVA en `crearInfoFactura`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/GenerateInvoiceSri.java`

**ANTES:**

```java
// Calcular el IVA sobre cada detalle
BigDecimal totalIva = invoiceDto.getInvoiceDetails().stream().map(detail -> {
  BigDecimal subtotal = detail.getSubtotal();
  BigDecimal rate = detail.getProductDto().getRate();
  return UtilsMath.iva(subtotal, rate);
}).reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal totalIce = invoiceDto.getSubtotalIce();
BigDecimal totalImpuestos = totalIva.add(totalIce);
```

**AHORA:**

```java
// Agregar ICE si existe
BigDecimal totalIce = invoiceDto.getSubtotalIce() != null
    ? invoiceDto.getSubtotalIce()
    : BigDecimal.ZERO;

// En Ecuador, el IVA se calcula sobre (Subtotal + ICE)
BigDecimal baseImponibleIva = totalSinImpuestos.add(totalIce);

// Obtener la tasa de IVA del primer producto
BigDecimal ivaRate = invoiceDto.getInvoiceDetails().get(0)
    .getProductDto().getRate();

// Calcular el IVA sobre la base imponible (Subtotal + ICE)
BigDecimal totalIva = UtilsMath.iva(baseImponibleIva, ivaRate);

BigDecimal totalImpuestos = totalIva.add(totalIce);
```

### 2. Reporte de Impuestos al SRI en `crearTotalImpuestos`

**ANTES:**

```java
// Reportaba el IVA por cada detalle, sin considerar ICE
for (InvoiceDetailDto detail : invoiceDto.getInvoiceDetails()) {
  TotalImpuesto totalImpuesto = new TotalImpuesto();
  totalImpuesto.setBaseImponible(String.valueOf(detail.getSubtotal())); // ❌ Sin ICE
  totalImpuesto.setValor(String.valueOf(UtilsMath.iva(detail.getSubtotal(), rate)));
  totalImpuestos.add(totalImpuesto);
}
```

**AHORA:**

```java
// Calcula y reporta un solo TotalImpuesto con base = Subtotal + ICE
BigDecimal totalSinImpuestos = /* suma de todos los detalles */;
BigDecimal totalIce = invoiceDto.getSubtotalIce();
BigDecimal baseImponibleIva = totalSinImpuestos.add(totalIce);

TotalImpuesto totalImpuesto = new TotalImpuesto();
totalImpuesto.setBaseImponible(String.valueOf(baseImponibleIva)); // ✅ Incluye ICE
totalImpuesto.setValor(String.valueOf(UtilsMath.iva(baseImponibleIva, rate)));
totalImpuestos.add(totalImpuesto);
```

### 3. Logs Mejorados

**ANTES:**

```
=== TOTALES CALCULADOS ===
Total sin impuestos: 1485
IVA: 222.75          ❌
ICE: 74.25
Total impuestos: 297.00
IMPORTE TOTAL: 1930.50  ❌
```

**AHORA:**

```
=== TOTALES CALCULADOS ===
Total sin impuestos: 1485
ICE: 74.25
Base imponible IVA (Subtotal + ICE): 1559.25
IVA (0.15%): 233.89   ✅
Total impuestos (IVA + ICE): 308.14
Propina: 148.5
IMPORTE TOTAL: 1941.64  ✅
```

## Impacto

### Archivos Modificados

1. `src/main/java/com/wallparisoft/ebillbilling/service/impl/GenerateInvoiceSri.java`
   - Método `crearInfoFactura()`: Calcula IVA sobre (Subtotal + ICE)
   - Método `crearTotalImpuestos()`: Reporta base imponible correcta al SRI
   - Logs: Muestra base imponible y cálculo detallado

### Validación

**Cálculo Manual:**

```
Subtotal:           $1,485.00
ICE:                $   74.25
------------------------
Base IVA:           $1,559.25
IVA 15%:            $  233.89
Propina 10%:        $  148.50
------------------------
TOTAL A PAGAR:      $1,941.64 ✅
```

**Frontend vs Sistema:**
| Concepto | Frontend | Sistema | Estado |
|---|---|---|---|
| Subtotal | $1,485.00 | $1,485.00 | ✅ |
| ICE | $74.25 | $74.25 | ✅ |
| Base IVA | $1,559.25 | $1,559.25 | ✅ |
| IVA 15% | $233.89 | $233.89 | ✅ |
| Propina | $148.50 | $148.50 | ✅ |
| **TOTAL** | **$1,941.64** | **$1,941.64** | ✅ |

## Testing

### Request JSON de Prueba

```json
{
  "establishment": {
    "type": "MATRIZ",
    "issuancePoint": {
      "idIssuancePoint": 1
    }
  },
  "invoiceDetails": [
    {
      "quantity": 1,
      "unitPrice": 1650.0,
      "discount": 165.0,
      "subtotal": 1485.0,
      "productDto": {
        "rate": 0.15,
        "taxCode": "2",
        "taxCodPercentage": "3"
      }
    }
  ],
  "subtotalIce": 74.25,
  "tip": 148.5
}
```

### Logs Esperados

```
=== TOTALES CALCULADOS ===
Total sin impuestos: 1485
ICE: 74.25
Base imponible IVA (Subtotal + ICE): 1559.25
IVA (0.15%): 233.89
Total impuestos (IVA + ICE): 308.14
Propina: 148.5
IMPORTE TOTAL: 1941.64
========================
```

### XML Enviado al SRI

```xml
<infoFactura>
  <totalSinImpuestos>1485.00</totalSinImpuestos>
  <totalConImpuestos>
    <totalImpuesto>
      <codigo>2</codigo>
      <codigoPorcentaje>3</codigoPorcentaje>
      <tarifa>0.15</tarifa>
      <baseImponible>1559.25</baseImponible>  <!-- Subtotal + ICE ✅ -->
      <valor>233.89</valor>                     <!-- IVA correcto ✅ -->
    </totalImpuesto>
  </totalConImpuestos>
  <propina>148.50</propina>
  <importeTotal>1941.64</importeTotal>
</infoFactura>
```

## Consideraciones

### 1. Asunción de Tasa Única de IVA

El código asume que **todos los productos tienen la misma tasa de IVA** (toma la tasa del primer producto).

**Si hay productos con diferentes tasas de IVA:**

- Habría que agrupar por tasa
- Calcular base imponible por cada tasa
- Reportar múltiples `<totalImpuesto>` en el XML

**Ejemplo:**

- Producto A: IVA 15%
- Producto B: IVA 0%
- ICE total: $50

```xml
<totalConImpuestos>
  <totalImpuesto>
    <codigoPorcentaje>3</codigoPorcentaje>  <!-- IVA 15% -->
    <baseImponible>1000 + 50</baseImponible> <!-- Subtotal A + ICE -->
    <valor>157.50</valor>
  </totalImpuesto>
  <totalImpuesto>
    <codigoPorcentaje>0</codigoPorcentaje>  <!-- IVA 0% -->
    <baseImponible>500</baseImponible>
    <valor>0.00</valor>
  </totalImpuesto>
</totalConImpuestos>
```

### 2. Distribución del ICE

El ICE se recibe como un valor global en `invoiceDto.getSubtotalIce()`.

Si en el futuro se necesita ICE por producto:

- Agregar campo `ice` en `InvoiceDetailDto`
- Calcular IVA para cada detalle sobre `(subtotal + ice)`
- Agrupar por tasa de IVA

## Referencias

- **LRTI (Ley de Régimen Tributario Interno)**: Artículo 65
- **SRI - Ficha Técnica Factura Electrónica**: Versión 2.1.0
- **Ejemplo oficial**: [Portal SRI](https://www.sri.gob.ec)

## Conclusión

✅ **El sistema ahora calcula correctamente el IVA sobre (Subtotal + ICE)**  
✅ **Los totales coinciden con el frontend (diferencia de $0.00)**  
✅ **El XML enviado al SRI reporta la base imponible correcta**  
✅ **Logs detallados para debugging**

**Fecha de corrección**: 14 de Octubre 2025  
**Versión**: 1.0.0
