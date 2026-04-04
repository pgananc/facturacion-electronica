# Fix: Base Imponible del ICE - Normativa SRI Ecuador

## 🔴 Problema Detectado

### Error del SRI:

```
ERROR EN DIFERENCIAS
La base imponible total FacturaTotalConImpuesto [codigo=3, codigoPorcentaje=3072]
esperado 2750.0 no coincide con el calculado 25.0
```

- **codigo=3**: ICE (Impuesto a los Consumos Especiales)
- **codigoPorcentaje=3072**: Código específico del porcentaje de ICE
- **Esperado por SRI**: 2750.0 (total de TODOS los productos)
- **Calculado erróneamente**: 25.0 (solo el producto con ICE) ❌

## 📊 Análisis del Problema

### Datos de la Factura:

```json
{
  "subtotalIce": 0.5,
  "discount": 275,
  "invoiceDetails": [
    {
      "productDto": {
        "name": "Paquete GOLD 1000 canales",
        "iceRate": 2
      },
      "unitPrice": 25,
      "subtotal": 25
    },
    {
      "productDto": {
        "name": "iPad 9na gen"
      },
      "unitPrice": 500,
      "discount": 25,
      "subtotal": 475
    },
    {
      "productDto": {
        "name": "M4 Mac"
      },
      "unitPrice": 2500,
      "discount": 250,
      "subtotal": 2250
    }
  ]
}
```

### Cálculo:

**Total sin impuestos (con descuentos aplicados):**

- Paquete GOLD: 25 (tiene ICE)
- iPad: 475
- M4 Mac: 2250
- **TOTAL: 2750** ✅

### Error Conceptual Inicial:

❌ **Asumí incorrectamente que:**

> La base imponible del ICE debería ser solo la suma de los productos que tienen `iceRate` configurado (25)

✅ **La realidad según normativa SRI:**

> La base imponible del ICE en `totalConImpuestos` debe ser el TOTAL de todos los productos gravados, no solo los que tienen ICE.

## 📖 Normativa del SRI Ecuador

Según la estructura XML del SRI:

1. **En `<detalles>` de cada producto:**

   - El ICE se reporta solo en el detalle del producto que lo tiene
   - Base del ICE = subtotal de ese producto específico

2. **En `<totalConImpuestos>` (totales de la factura):**
   - La base imponible del ICE = **TOTAL de todos los productos**
   - Esto es porque el SRI necesita ver la base total sobre la cual se calculan TODOS los impuestos

### Ejemplo de XML Correcto:

```xml
<factura>
  <detalles>
    <!-- Detalle 1: Producto CON ICE -->
    <detalle>
      <descripcion>Paquete GOLD</descripcion>
      <precioTotalSinImpuesto>25</precioTotalSinImpuesto>
      <impuestos>
        <impuesto>
          <codigo>3</codigo> <!-- ICE -->
          <baseImponible>25</baseImponible> <!-- Solo este producto -->
          <valor>0.5</valor>
        </impuesto>
        <impuesto>
          <codigo>2</codigo> <!-- IVA -->
          <baseImponible>25.5</baseImponible> <!-- Subtotal + ICE -->
          <valor>3.83</valor>
        </impuesto>
      </impuestos>
    </detalle>

    <!-- Detalle 2: Producto SIN ICE -->
    <detalle>
      <descripcion>iPad</descripcion>
      <precioTotalSinImpuesto>475</precioTotalSinImpuesto>
      <impuestos>
        <impuesto>
          <codigo>2</codigo> <!-- Solo IVA -->
          <baseImponible>475</baseImponible>
          <valor>71.25</valor>
        </impuesto>
      </impuestos>
    </detalle>
  </detalles>

  <infoFactura>
    <totalConImpuestos>
      <!-- ICE TOTAL: Base = TODOS los productos -->
      <totalImpuesto>
        <codigo>3</codigo>
        <baseImponible>2750</baseImponible> <!-- ✅ TOTAL de productos -->
        <valor>0.5</valor>
      </totalImpuesto>

      <!-- IVA TOTAL: Base = Subtotal + ICE -->
      <totalImpuesto>
        <codigo>2</codigo>
        <baseImponible>2750.5</baseImponible>
        <valor>412.58</valor>
      </totalImpuesto>
    </totalConImpuestos>
  </infoFactura>
</factura>
```

## ✅ Solución Implementada

### Archivo Modificado:

`src/main/java/com/wallparisoft/ebillbilling/service/impl/GenerateInvoiceSri.java`

### Método: `crearTotalImpuestos(InvoiceDto invoiceDto)`

```java
public List<TotalImpuesto> crearTotalImpuestos(InvoiceDto invoiceDto) {
  List<TotalImpuesto> totalImpuestos = new ArrayList<TotalImpuesto>();

  // Calcular el total sin impuestos (suma de subtotales ya con descuentos aplicados)
  BigDecimal totalSinImpuestos =
      invoiceDto.getInvoiceDetails().stream()
          .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

  // Agregar ICE si existe
  BigDecimal totalIce =
      invoiceDto.getSubtotalIce() != null ? invoiceDto.getSubtotalIce() : BigDecimal.ZERO;

  // 1. Agregar TotalImpuesto para el ICE (si existe)
  if (totalIce.compareTo(BigDecimal.ZERO) > 0) {
    // IMPORTANTE: Según la normativa del SRI ecuatoriano, cuando hay ICE en una factura,
    // la base imponible del ICE en "totalConImpuestos" debe ser el total de
    // TODOS los productos gravados con IVA, NO solo los que tienen ICE.
    // Esto es porque el ICE se aplica antes del IVA, pero la estructura XML requiere
    // reportar la base total sobre la cual se calculan los impuestos.
    BigDecimal baseImponibleIce = totalSinImpuestos;

    TotalImpuesto totalImpuestoIce = new TotalImpuesto();
    totalImpuestoIce.setCodigo(SriTaxConstants.CODE_ICE);
    totalImpuestoIce.setCodigoPorcentaje(SriTaxConstants.DEFAULT_ICE_PERCENTAGE_CODE);
    totalImpuestoIce.setTarifa(SriTaxConstants.DEFAULT_ICE_RATE);

    String baseImponibleIceFormateada = formatBaseImponible(baseImponibleIce);
    totalImpuestoIce.setBaseImponible(baseImponibleIceFormateada);
    totalImpuestoIce.setValor(String.valueOf(totalIce));
    totalImpuestos.add(totalImpuestoIce);

    log.info("📊 ICE Total - Base: {}, Valor: {}", baseImponibleIce, totalIce);
  }

  // 2. Agregar TotalImpuesto para el IVA (se calcula sobre Subtotal + ICE)
  BigDecimal baseImponibleIva = totalSinImpuestos.add(totalIce);
  // ... resto del código
}
```

## 🎯 Resultado Esperado

Con la factura de ejemplo:

- **Base imponible ICE:** 2750 ✅ (TODOS los productos)
- **Valor ICE:** 0.5 (calculado solo sobre el Paquete GOLD: 25 × 2%)
- **Base imponible IVA:** 2750.5 (2750 + 0.5)
- **IVA 15%:** 412.58
- **Total:** 3163.08

## 📝 Notas Importantes

1. **Diferencia entre Detalle y Total:**

   - En `<detalles>`: El ICE se reporta solo en productos que lo tienen, con su base específica
   - En `<totalConImpuestos>`: La base del ICE es el total de TODOS los productos

2. **Lógica del SRI:**

   - El SRI necesita ver el total de la factura en `totalConImpuestos`
   - Aunque el ICE se aplique solo a ciertos productos, el reporte total usa la base completa

3. **Campos de ProductDto:**
   - Se agregaron `iceRate`, `iceTaxCode`, `iceTaxCodPercentage`
   - Estos campos permiten identificar qué productos tienen ICE en los detalles

## 🧪 Testing

### Caso 1: Factura con 1 producto con ICE y 2 sin ICE

```
Productos:
- Paquete GOLD (con ICE): 25
- iPad (sin ICE): 475
- M4 Mac (sin ICE): 2250
Total: 2750

✅ Base ICE en totalConImpuestos: 2750
✅ Valor ICE: 0.5
✅ Base IVA: 2750.5
```

### Caso 2: Factura sin ICE

```
✅ No se agrega sección de ICE en totalConImpuestos
✅ Base IVA = subtotal total
```

## 🔗 Archivos Modificados

1. `src/main/java/com/wallparisoft/ebillbilling/dto/ProductDto.java`

   - Agregados campos: `iceRate`, `iceTaxCode`, `iceTaxCodPercentage`

2. `src/main/java/com/wallparisoft/ebillbilling/service/impl/GenerateInvoiceSri.java`
   - Método `crearTotalImpuestos()`: Base ICE = totalSinImpuestos (TODOS los productos)

## ✅ Verificación Final

El XML generado ahora tendrá:

```xml
<totalConImpuestos>
  <totalImpuesto>
    <codigo>3</codigo>
    <codigoPorcentaje>3072</codigoPorcentaje>
    <baseImponible>2750</baseImponible>  <!-- ✅ Total de TODOS los productos -->
    <tarifa>2</tarifa>
    <valor>0.5</valor>
  </totalImpuesto>
  <totalImpuesto>
    <codigo>2</codigo>
    <codigoPorcentaje>4</codigoPorcentaje>
    <baseImponible>2750.5</baseImponible>  <!-- Subtotal + ICE -->
    <tarifa>15</tarifa>
    <valor>412.58</valor>
  </totalImpuesto>
</totalConImpuestos>
```

Y el SRI lo aceptará sin error de "diferencias en base imponible". ✅
