# Solución: TransientObjectException con PaymentMethod

## Problema

El error ocurría al intentar guardar una `Invoice`:

```
org.hibernate.TransientObjectException: persistent instance references an unsaved transient instance of 'com.wallparisoft.ebillbilling.entity.PaymentMethod' (save the transient instance before flushing)
```

### Causa

El mapper `InvoiceServiceMapper` estaba creando una nueva instancia de `PaymentMethod` usando solo el ID:

```java
.paymentMethod(
    PaymentMethod.builder().idPaymentMethod(invoiceDto.getIdPaymentMethod()).build())
```

Hibernate interpretaba esto como un intento de crear una nueva entidad `PaymentMethod` en lugar de usar una referencia a una existente. Como la entidad no estaba persistida, lanzaba el error.

## Solución Implementada

### 1. Inyección del Repositorio

Se agregó el repositorio de `PaymentMethod` al mapper:

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/mapper/InvoiceServiceMapper.java`

```java
@Component
@Slf4j
@RequiredArgsConstructor  // ← Agregado para inyección de dependencias
@FieldDefaults(level = PRIVATE)
public class InvoiceServiceMapper {

  final IPaymentMethodRepo paymentMethodRepo;  // ← Agregado
```

### 2. Búsqueda de PaymentMethod en Base de Datos

Se modificó el método `toInvoice()` para buscar el `PaymentMethod` existente:

```java
public Invoice toInvoice(InvoiceDto invoiceDto) {

  // Fetch PaymentMethod from database if idPaymentMethod is provided
  PaymentMethod paymentMethod = null;
  if (invoiceDto.getIdPaymentMethod() != null) {
    paymentMethod = paymentMethodRepo.findById(invoiceDto.getIdPaymentMethod())
        .orElseThrow(() -> new RuntimeException("PaymentMethod not found with id: " + invoiceDto.getIdPaymentMethod()));
  }

  Invoice invoice =
      Invoice.builder()
          .status(true)
          .idInvoice(invoiceDto.getIdInvoice())
          .idCompany(invoiceDto.getCompanyDto().getIdCompany())
          .idClient(invoiceDto.getClientDto().getIdClient())
          .paymentMethod(paymentMethod)  // ← Usa la entidad recuperada de la BD
          // ... resto de los campos
          .build();

  // ... resto del método
}
```

## Cómo Funciona Ahora

### Antes (Con Error)

```java
// Creaba una nueva instancia "desconectada"
PaymentMethod pm = PaymentMethod.builder()
    .idPaymentMethod(1L)
    .build();

invoice.setPaymentMethod(pm);
repo.save(invoice); // ❌ Error: PaymentMethod no está persistido
```

### Ahora (Sin Error)

```java
// Busca la entidad existente en la base de datos
PaymentMethod pm = paymentMethodRepo.findById(1L)
    .orElseThrow(...);

invoice.setPaymentMethod(pm);  // pm es una entidad "managed" por Hibernate
repo.save(invoice); // ✅ Funciona correctamente
```

## Flujo Completo

1. **Request llega con `idPaymentMethod`**

   ```json
   {
     "idPaymentMethod": 1,
     "companyDto": { ... },
     "clientDto": { ... }
   }
   ```

2. **Mapper busca el PaymentMethod en la BD**

   ```java
   PaymentMethod paymentMethod = paymentMethodRepo.findById(1L)
       .orElseThrow(() -> new RuntimeException("PaymentMethod not found with id: 1"));
   ```

3. **Se construye la Invoice con el PaymentMethod existente**

   ```java
   Invoice invoice = Invoice.builder()
       .paymentMethod(paymentMethod)  // Entidad "managed"
       .build();
   ```

4. **Se guarda la Invoice sin errores**
   ```java
   repo.save(invoice);  // ✅ Funciona
   ```

## Validación

El método ahora también valida que el `PaymentMethod` exista:

```java
.orElseThrow(() -> new RuntimeException("PaymentMethod not found with id: " + invoiceDto.getIdPaymentMethod()));
```

Si el ID no existe en la base de datos, se lanza una excepción descriptiva.

## Archivos Modificados

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/mapper/InvoiceServiceMapper.java`

Cambios:

1. Agregado `@RequiredArgsConstructor` para inyección de dependencias
2. Inyectado `IPaymentMethodRepo`
3. Modificado método `toInvoice()` para buscar el `PaymentMethod` en lugar de crear uno nuevo

## Testing

Para probar:

```json
POST /api/invoices

{
  "idPaymentMethod": 1,
  "companyDto": {
    "idCompany": 1,
    "name": "Mi Empresa"
  },
  "establishment": {
    "issuancePoint": {
      "idIssuancePoint": 1,
      "code": "001"
    }
  },
  // ... resto de los datos
}
```

Asegúrate de que:

- El `idPaymentMethod` existe en la tabla `payment_method`
- Si no existe, se lanzará: `RuntimeException: PaymentMethod not found with id: X`

## Alternativa: EntityManager.getReference()

Otra solución (no implementada) sería usar `EntityManager.getReference()`:

```java
@PersistenceContext
private EntityManager entityManager;

public Invoice toInvoice(InvoiceDto invoiceDto) {
  PaymentMethod paymentMethod = null;
  if (invoiceDto.getIdPaymentMethod() != null) {
    paymentMethod = entityManager.getReference(
        PaymentMethod.class,
        invoiceDto.getIdPaymentMethod()
    );
  }
  // ...
}
```

Esta opción crea un proxy sin hacer la consulta SELECT, pero si el ID no existe, el error aparece al hacer flush en lugar de inmediatamente.

## Beneficios de la Solución Actual

✅ **Validación temprana**: Si el `PaymentMethod` no existe, falla inmediatamente
✅ **Claro y explícito**: Es fácil entender qué está haciendo el código
✅ **Mensaje de error descriptivo**: Ayuda al debugging
✅ **No requiere EntityManager**: Usa Spring Data JPA estándar
