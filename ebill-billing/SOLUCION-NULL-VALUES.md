# Solución: Manejo de Valores Null en Cálculo de IVA

## Problema

El error ocurría cuando se intentaba calcular el IVA con valores `null`:

```
Cannot read field "scale" because "multiplicand" is null
	at java.base/java.math.BigDecimal.multiply(BigDecimal.java:1572)
	at com.wallparisoft.integration.sri.util.UtilsMath.multiplicar(UtilsMath.java:63)
	at com.wallparisoft.integration.sri.util.UtilsMath.iva(UtilsMath.java:50)
	at com.wallparisoft.ebillbilling.service.impl.GenerateInvoiceSri.crearTotalImpuestos(GenerateInvoiceSri.java:168)
```

Esto sucedía cuando:

- `detail.getSubtotal()` era `null`
- `detail.getProductDto().getRate()` era `null`
- `detail.getDiscount()` era `null`

## Solución Implementada

### 1. Agregado Import de BigDecimal

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/GenerateInvoiceSri.java`

```java
import java.math.BigDecimal;
```

### 2. Validación de Null en `crearDetalles()`

Se agregaron validaciones para evitar NullPointerException:

```java
// Ensure rate is not null, default to 0
BigDecimal rate = detail.getProductDto().getRate() != null
    ? detail.getProductDto().getRate()
    : BigDecimal.ZERO;

BigDecimal subtotal = detail.getSubtotal() != null
    ? detail.getSubtotal()
    : BigDecimal.ZERO;

// Para discount también
detalle.setDescuento(String.valueOf(detail.getDiscount() != null ? detail.getDiscount() : 0));

impuesto.setTarifa(String.valueOf(rate));
impuesto.setBaseImponible(String.valueOf(subtotal));
impuesto.setValor(String.valueOf(UtilsMath.iva(subtotal, rate)));
```

### 3. Validación de Null en `crearTotalImpuestos()`

Se aplicó la misma lógica:

```java
// Ensure rate and subtotal are not null, default to 0
BigDecimal rate = detail.getProductDto().getRate() != null
    ? detail.getProductDto().getRate()
    : BigDecimal.ZERO;

BigDecimal subtotal = detail.getSubtotal() != null
    ? detail.getSubtotal()
    : BigDecimal.ZERO;

totalImpuesto.setTarifa(String.valueOf(rate));
totalImpuesto.setBaseImponible(String.valueOf(subtotal));
totalImpuesto.setValor(String.valueOf(UtilsMath.iva(subtotal, rate)));
```

## Comportamiento

### Antes (Con Error)

```
Request:
{
  "invoiceDetails": [
    {
      "subtotal": null,
      "productDto": {
        "rate": null
      }
    }
  ]
}

Result: NullPointerException
```

### Ahora (Sin Error)

```
Request:
{
  "invoiceDetails": [
    {
      "subtotal": null,
      "productDto": {
        "rate": null
      }
    }
  ]
}

Result: Se usan valores por defecto
- rate: 0
- subtotal: 0
- discount: 0
- IVA calculado: 0
```

## Valores por Defecto

| Campo      | Tipo       | Default si null     |
| ---------- | ---------- | ------------------- |
| `rate`     | BigDecimal | BigDecimal.ZERO (0) |
| `subtotal` | BigDecimal | BigDecimal.ZERO (0) |
| `discount` | BigDecimal | 0                   |

## Recomendaciones

⚠️ **Importante**: Aunque ahora el sistema maneja valores null, es recomendable que:

1. El frontend/cliente siempre envíe valores completos en el request
2. Se agreguen validaciones a nivel de controlador/DTO para rechazar requests incompletos
3. Los valores de rate, subtotal y discount deberían ser obligatorios (@NotNull)

### Ejemplo de Validación Recomendada en InvoiceDetailDto:

```java
@NotNull(message = "El subtotal es requerido")
private BigDecimal subtotal;

@NotNull(message = "El descuento es requerido")
private BigDecimal discount;
```

### Ejemplo de Validación Recomendada en ProductDto:

```java
@NotNull(message = "La tarifa (rate) es requerida")
private BigDecimal rate;
```

## Testing

Para probar que funciona correctamente:

1. **Request válido** (todos los campos completos):

```json
{
  "invoiceDetails": [
    {
      "quantity": 2,
      "unitPrice": 100.0,
      "subtotal": 200.0,
      "discount": 10.0,
      "productDto": {
        "rate": 15
      }
    }
  ]
}
```

2. **Request con valores null** (ahora manejado sin error):

```json
{
  "invoiceDetails": [
    {
      "quantity": 2,
      "unitPrice": 100.0,
      "subtotal": null,
      "discount": null,
      "productDto": {
        "rate": null
      }
    }
  ]
}
```

Resultado: Los valores null se reemplazan por 0 automáticamente.

## Archivos Modificados

1. `src/main/java/com/wallparisoft/ebillbilling/service/impl/GenerateInvoiceSri.java`
   - Agregado import de BigDecimal
   - Validación de null en método `crearDetalles()`
   - Validación de null en método `crearTotalImpuestos()`
