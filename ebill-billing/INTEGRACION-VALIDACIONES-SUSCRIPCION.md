# 🔐 Integración de Validaciones de Suscripción con ebill-customer

## 📌 Resumen

Este documento explica cómo integrar las validaciones de límites de suscripción desde **ebill-billing** en **ebill-customer**, donde se gestionan usuarios, establecimientos y puntos de emisión.

---

## 🎯 Objetivo

Validar límites de suscripción cuando se crean:

- ✅ Usuarios
- ✅ Establecimientos
- ✅ Puntos de emisión

---

## 🔧 Implementación en ebill-customer

### 1️⃣ Crear Cliente Feign para ebill-billing

Crear archivo: `src/main/java/com/wallparisoft/ebillcustomer/service/BillingClient.java`

```java
package com.wallparisoft.ebillcustomer.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "billing-client", url = "${ebill.billing.url:http://localhost:8480}")
public interface BillingClient {

    @GetMapping("/api/subscription/{idCompany}/current")
    ResponseEntity<SubscriptionResponseDto> getCurrentSubscription(@PathVariable("idCompany") Long idCompany);
}
```

### 2️⃣ Crear DTO de Respuesta

Crear archivo: `src/main/java/com/wallparisoft/ebillcustomer/dto/SubscriptionResponseDto.java`

```java
package com.wallparisoft.ebillcustomer.dto;

import lombok.Data;

@Data
public class SubscriptionResponseDto {
    private Long idSubscription;
    private Long idCompany;
    private SubscriptionPlanDto plan;
    private String status;
    // ... otros campos según necesidad
}

@Data
class SubscriptionPlanDto {
    private String code;
    private String name;
    private Integer userLimit;
    private Integer establishmentLimit;
    private Integer issuancePointLimit;
}
```

### 3️⃣ Validar al Crear Usuario

En el servicio de usuarios (ej: `UserServiceImpl.java`):

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepo userRepo;
    private final BillingClient billingClient;

    @Override
    public User createUser(UserDto userDto) {
        Long idCompany = userDto.getIdCompany();

        // Contar usuarios actuales de la compañía
        int currentUserCount = userRepo.countByIdCompany(idCompany);

        // Obtener límites de la suscripción
        try {
            SubscriptionResponseDto subscription =
                billingClient.getCurrentSubscription(idCompany).getBody();

            Integer userLimit = subscription.getPlan().getUserLimit();

            if (currentUserCount >= userLimit) {
                throw new IllegalStateException(
                    "Ha alcanzado el límite de usuarios de su plan (" + userLimit +
                    "). Actualice su plan para agregar más usuarios."
                );
            }
        } catch (FeignException e) {
            log.warn("No se pudo validar límite de usuarios: {}", e.getMessage());
            // Decidir: ¿permitir creación o rechazar?
        }

        // Crear usuario
        return userRepo.save(mapToEntity(userDto));
    }
}
```

### 4️⃣ Validar al Crear Establecimiento

En el servicio de establecimientos (ej: `EstablishmentServiceImpl.java`):

```java
@Service
@RequiredArgsConstructor
public class EstablishmentServiceImpl implements IEstablishmentService {

    private final IEstablishmentRepo establishmentRepo;
    private final BillingClient billingClient;

    @Override
    public Establishment createEstablishment(EstablishmentDto dto) {
        Long idCompany = dto.getIdCompany();

        // Contar establecimientos actuales
        int currentCount = establishmentRepo.countByIdCompany(idCompany);

        // Validar límite
        try {
            SubscriptionResponseDto subscription =
                billingClient.getCurrentSubscription(idCompany).getBody();

            Integer limit = subscription.getPlan().getEstablishmentLimit();

            if (currentCount >= limit) {
                throw new IllegalStateException(
                    "Ha alcanzado el límite de establecimientos de su plan (" + limit +
                    "). Actualice su plan para agregar más establecimientos."
                );
            }
        } catch (FeignException e) {
            log.warn("No se pudo validar límite de establecimientos: {}", e.getMessage());
        }

        return establishmentRepo.save(mapToEntity(dto));
    }
}
```

### 5️⃣ Validar al Crear Punto de Emisión

En el servicio de puntos de emisión (ej: `IssuancePointServiceImpl.java`):

```java
@Service
@RequiredArgsConstructor
public class IssuancePointServiceImpl implements IIssuancePointService {

    private final IIssuancePointRepo issuancePointRepo;
    private final BillingClient billingClient;

    @Override
    public IssuancePoint createIssuancePoint(IssuancePointDto dto) {
        Long idCompany = dto.getIdCompany();

        // Contar puntos de emisión actuales
        int currentCount = issuancePointRepo.countByIdCompany(idCompany);

        // Validar límite
        try {
            SubscriptionResponseDto subscription =
                billingClient.getCurrentSubscription(idCompany).getBody();

            Integer limit = subscription.getPlan().getIssuancePointLimit();

            if (currentCount >= limit) {
                throw new IllegalStateException(
                    "Ha alcanzado el límite de puntos de emisión de su plan (" + limit +
                    "). Actualice su plan para agregar más puntos de emisión."
                );
            }
        } catch (FeignException e) {
            log.warn("No se pudo validar límite de puntos de emisión: {}", e.getMessage());
        }

        return issuancePointRepo.save(mapToEntity(dto));
    }
}
```

---

## ⚙️ Configuración

Agregar en `application.yml` de **ebill-customer**:

```yaml
ebill:
  billing:
    url: http://localhost:8480 # Ajustar según ambiente
```

---

## 🔒 Manejo de Errores

### Opción 1: Rechazar creación si no se puede validar

```java
catch (FeignException e) {
    log.error("Error validando límite: {}", e.getMessage());
    throw new IllegalStateException(
        "No se puede validar la suscripción. Intente nuevamente."
    );
}
```

### Opción 2: Permitir creación si el servicio no está disponible

```java
catch (FeignException e) {
    log.warn("Servicio de billing no disponible, permitiendo creación: {}", e.getMessage());
    // Continuar con la creación
}
```

---

## 🧪 Testing

### Test de Integración

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserServiceIntegrationTest {

    @Autowired
    private IUserService userService;

    @MockBean
    private BillingClient billingClient;

    @Test
    void shouldRejectUserCreationWhenLimitReached() {
        // Given
        SubscriptionResponseDto subscription = mockSubscription(userLimit = 3);
        when(billingClient.getCurrentSubscription(anyLong()))
            .thenReturn(ResponseEntity.ok(subscription));

        // Simular que ya existen 3 usuarios
        when(userRepo.countByIdCompany(1L)).thenReturn(3);

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            userService.createUser(createUserDto());
        });
    }
}
```

---

## 📊 Flujo de Validación

```
┌─────────────────────────────────────────────────────┐
│  ebill-customer: UserServiceImpl.createUser()      │
└─────────────┬───────────────────────────────────────┘
              │
              │ 1. Contar usuarios actuales
              ↓
     ┌────────────────────────┐
     │  userRepo.count()      │
     └────────┬───────────────┘
              │
              │ 2. Consultar suscripción
              ↓
     ┌──────────────────────────────────────────┐
     │  BillingClient.getCurrentSubscription()  │
     └────────┬─────────────────────────────────┘
              │
              │ HTTP GET
              ↓
┌─────────────────────────────────────────────────────┐
│  ebill-billing: SubscriptionController              │
│  GET /api/subscription/{idCompany}/current          │
└─────────────┬───────────────────────────────────────┘
              │
              │ 3. Respuesta con límites
              ↓
     ┌──────────────────────────┐
     │  SubscriptionResponseDto │
     │  - userLimit: 10         │
     └────────┬─────────────────┘
              │
              │ 4. Validar: currentCount < userLimit
              ↓
     ┌─────────────────────────────────┐
     │  if (currentCount >= userLimit) │
     │    throw Exception              │
     │  else                           │
     │    create user                  │
     └─────────────────────────────────┘
```

---

## 🎯 Validaciones Implementadas en ebill-billing

✅ **Validación de documentos:**

- `InvoiceServiceImpl.saveInvoice()` → Valida límite de facturas
- `CreditNoteServiceImpl.saveCreditNote()` → Valida límite de notas de crédito

✅ **Endpoint de validación:**

- `POST /api/subscription/{idCompany}/can-emit` → Verifica si puede emitir documento

✅ **Métodos de validación programática:**

```java
// En cualquier servicio de ebill-customer que use BillingClient
subscriptionService.validateUserLimit(idCompany, currentUserCount);
subscriptionService.validateEstablishmentLimit(idCompany, currentEstablishmentCount);
subscriptionService.validateIssuancePointLimit(idCompany, currentIssuancePointCount);
```

---

## 📝 Notas Importantes

1. **Super Admin bypass**: Los SUPER_ADMIN no tienen restricciones de límites
2. **Manejo de errores**: Decidir política de qué hacer si billing no está disponible
3. **Caché**: Considerar cachear límites de suscripción para reducir llamadas
4. **Testing**: Probar escenarios con servicio caído, timeout, etc.

---

## 🚀 Siguiente Paso

Aplicar estas validaciones en **ebill-customer** para completar el sistema de suscripciones.
