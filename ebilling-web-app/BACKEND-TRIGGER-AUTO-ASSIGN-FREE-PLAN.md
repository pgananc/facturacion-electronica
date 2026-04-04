# 🔄 Asignación Automática de Plan FREE al Crear Compañía

**Fecha:** 21 de Diciembre, 2025  
**Microservicios:** `ebill-customers` (crea compañía) → `ebill-billing` (asigna suscripción)  
**Objetivo:** Asignar automáticamente el plan FREE cuando se crea una nueva compañía

---

## 📋 **CONTEXTO**

Cuando se crea una nueva compañía en el sistema, **NO tiene suscripción** por defecto. Esto significa que:

- ❌ No puede emitir documentos
- ❌ No aparece en la lista de suscripciones
- ⚠️ Requiere acción manual del Super Admin para asignar una suscripción

**⚠️ PROBLEMA ARQUITECTÓNICO:**

- **Tabla `company`:** Está en `ebill-customers` (BD: `ebill_customers`)
- **Tabla `subscription_plan`:** Está en `ebill-billing` (BD: `ebill_billing`)
- **Tabla `company_subscription`:** Está en `ebill-billing` (BD: `ebill_billing`)

**❌ NO se puede usar un trigger SQL** porque las tablas están en **bases de datos diferentes**.

**✅ SOLUCIÓN:** Implementar la lógica a nivel de **aplicación Java** con comunicación HTTP entre microservicios.

---

## 🎯 **REQUISITOS**

### **Comportamiento Esperado:**

1. ✅ Al crear una compañía → Se asigna automáticamente plan FREE
2. ✅ La suscripción queda con `status = 'ACTIVE'`
3. ✅ `start_date = CURRENT_DATE` (o fecha proporcionada)
4. ✅ `duration_months = valor por defecto configurable` (por defecto: 12 meses)
5. ✅ `expires_at = start_date + duration_months` (calculado automáticamente)
6. ✅ `payment_method = 'MANUAL'`
7. ✅ `notes = 'Plan FREE asignado automáticamente al crear la compañía'`

### **Configuración del Período FREE:**

- ⚙️ **Período configurable** igual que las otras suscripciones (desde UI o properties)
- 📅 **Valor por defecto:** 12 meses - puede cambiarse según necesidades del negocio
- 🔧 **Formato:** Meses (igual que las otras suscripciones para consistencia)
- 💡 **Después de auto-asignación:** El Super Admin puede modificar/extender desde la UI usando los diálogos existentes
- ✅ **Consistencia:** Usa el mismo formato que las suscripciones manuales (`durationMonths`)

### **Casos Especiales:**

- ⚠️ Si el plan FREE no existe → El trigger debe fallar silenciosamente o loggear un warning
- ⚠️ Si ya existe una suscripción (caso raro) → No duplicar

---

## 🔧 **IMPLEMENTACIÓN JAVA (Comunicación HTTP entre Microservicios)**

### **Arquitectura:**

```
┌─────────────────────┐         HTTP          ┌─────────────────────┐
│  ebill-customers    │ ────────────────────> │  ebill-billing      │
│  (Puerto 8180)      │   POST /api/subsc...  │  (Puerto 8480)      │
│                     │                        │                     │
│  1. Crear company   │                        │  2. Asignar FREE    │
│  2. Llamar HTTP     │                        │  3. Crear registro  │
│     a ebill-billing │                        │     en company_     │
│                     │                        │     subscription   │
└─────────────────────┘                        └─────────────────────┘
```

### **Paso 1: Crear Endpoint en `ebill-billing`**

```java
// SubscriptionController.java en ebill-billing

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Asigna automáticamente el plan FREE a una compañía recién creada
     * Endpoint: POST /api/subscriptions/auto-assign-free/{idCompany}
     *
     * Acepta parámetros opcionales para personalizar el período (igual que asignación manual)
     * Si no se proporcionan, usa valores por defecto configurables
     *
     * @param idCompany ID de la compañía recién creada
     * @param request (Opcional) Parámetros de personalización (startDate, durationMonths)
     * @return ResponseEntity con status 201 si se asignó, 200 si ya tenía suscripción
     */
    @PostMapping("/auto-assign-free/{idCompany}")
    @PreAuthorize("hasRole('SUPER_ADMIN')") // O sin autenticación si es interno
    public ResponseEntity<Void> autoAssignFreePlan(
            @PathVariable Long idCompany,
            @RequestBody(required = false) AutoAssignFreePlanRequest request) {
        try {
            // Verificar si ya tiene suscripción activa
            boolean hasActiveSubscription = subscriptionService
                .hasActiveSubscription(idCompany);

            if (hasActiveSubscription) {
                log.info("Compañía {} ya tiene suscripción activa. No se asignará FREE.", idCompany);
                return ResponseEntity.ok().build();
            }

            // Usar valores del request o valores por defecto
            LocalDate startDate = (request != null && request.getStartDate() != null)
                ? request.getStartDate()
                : LocalDate.now();

            int durationMonths = (request != null && request.getDurationMonths() != null)
                ? request.getDurationMonths()
                : subscriptionService.getDefaultFreePlanDurationMonths();

            // Asignar plan FREE con período configurable
            subscriptionService.assignFreePlan(idCompany, startDate, durationMonths);

            log.info("Plan FREE asignado automáticamente a la compañía {} - Duración: {} meses, Expira: {}",
                idCompany, durationMonths, startDate.plusMonths(durationMonths));
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (Exception e) {
            log.error("Error al asignar plan FREE automáticamente a compañía {}", idCompany, e);
            // No lanzar excepción - solo loggear para no bloquear creación de compañía
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DTO para personalizar la asignación automática del plan FREE
     */
    public static class AutoAssignFreePlanRequest {
        private LocalDate startDate;
        private Integer durationMonths;

        // Getters y setters
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public Integer getDurationMonths() { return durationMonths; }
        public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
    }
}
```

### **Paso 2: Servicio en `ebill-billing`**

```java
// SubscriptionService.java en ebill-billing

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private CompanySubscriptionRepository subscriptionRepository;

    // ⚙️ Duración por defecto del plan FREE (en meses) - igual que otras suscripciones
    @Value("${subscription.free-plan.default-duration-months:12}")
    private int defaultFreePlanDurationMonths;

    /**
     * Obtiene la duración por defecto del plan FREE en meses
     */
    public int getDefaultFreePlanDurationMonths() {
        return defaultFreePlanDurationMonths;
    }

    /**
     * Asigna el plan FREE a una compañía con período configurable
     * Usa el mismo formato que las suscripciones manuales (durationMonths)
     *
     * @param idCompany ID de la compañía
     * @param startDate Fecha de inicio (por defecto: hoy)
     * @param durationMonths Duración en meses (por defecto: valor configurado)
     */
    public void assignFreePlan(Long idCompany, LocalDate startDate, int durationMonths) {
        // 1. Buscar plan FREE
        SubscriptionPlan freePlan = planRepository.findByCode("FREE")
            .orElseThrow(() -> new RuntimeException("Plan FREE no encontrado"));

        // 2. Verificar que no tenga suscripción activa
        if (subscriptionRepository.existsByCompanyIdAndStatusActive(idCompany)) {
            throw new IllegalStateException("La compañía ya tiene suscripción activa");
        }

        // 3. Calcular fecha de expiración (igual que suscripciones manuales)
        LocalDate expiresAt = startDate.plusMonths(durationMonths);

        // 4. Crear suscripción FREE (igual formato que suscripciones manuales)
        CompanySubscription subscription = new CompanySubscription();
        subscription.setIdCompany(idCompany);
        subscription.setIdSubscriptionPlan(freePlan.getIdSubscriptionPlan());
        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(startDate);
        subscription.setExpiresAt(expiresAt);
        subscription.setCurrentYearUsage(0);
        subscription.setPaymentMethod("MANUAL");
        subscription.setNotes(String.format(
            "Plan FREE asignado automáticamente al crear la compañía (duración: %d meses)",
            durationMonths
        ));
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());

        subscriptionRepository.save(subscription);

        log.info("Plan FREE asignado a compañía {} - Inicio: {}, Duración: {} meses, Expira: {}",
            idCompany, startDate, durationMonths, expiresAt);
    }

    /**
     * Método sobrecargado para usar valores por defecto
     */
    public void assignFreePlan(Long idCompany) {
        assignFreePlan(idCompany, LocalDate.now(), defaultFreePlanDurationMonths);
    }

    /**
     * Verifica si una compañía tiene suscripción activa
     */
    public boolean hasActiveSubscription(Long idCompany) {
        return subscriptionRepository.existsByCompanyIdAndStatusActive(idCompany);
    }
}
```

### **Paso 3: Modificar Servicio en `ebill-customers`**

```java
// CompanyService.java en ebill-customers

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RestTemplate restTemplate; // O FeignClient, WebClient, etc.

    @Value("${ebill.billing.url:http://localhost:8480}")
    private String billingServiceUrl;

    /**
     * Crea una nueva compañía y asigna automáticamente plan FREE
     */
    public Company createCompany(CompanyRequest request) {
        // 1. Crear la compañía
        Company company = new Company();
        // ... mapear campos del request ...
        company = companyRepository.save(company);

        // 2. Llamar a ebill-billing para asignar FREE con valores por defecto
        try {
            String url = billingServiceUrl + "/api/subscriptions/auto-assign-free/" + company.getIdCompany();

            // Opción A: Sin parámetros (usa valores por defecto)
            restTemplate.postForEntity(url, null, Void.class);

            // Opción B: Con parámetros personalizados (si se necesita)
            // AutoAssignFreePlanRequest request = new AutoAssignFreePlanRequest();
            // request.setStartDate(LocalDate.now());
            // request.setDurationMonths(12);
            // restTemplate.postForEntity(url, request, Void.class);

            // Opción C: Asíncrono (no bloquea, mejor para UX)
            // CompletableFuture.runAsync(() -> {
            //     restTemplate.postForEntity(url, null, Void.class);
            // });

            log.info("Plan FREE asignado automáticamente a compañía {} con duración por defecto",
                company.getIdCompany());

        } catch (Exception e) {
            // ⚠️ IMPORTANTE: No fallar la creación de compañía si falla la asignación
            log.error("Error al asignar plan FREE automáticamente a compañía {}. " +
                     "La compañía fue creada pero sin suscripción. " +
                     "Debe asignarse manualmente desde la UI.", company.getIdCompany(), e);
            // La compañía se creó exitosamente, solo falta la suscripción
            // El Super Admin puede asignarla manualmente después
        }

        return company;
    }
}
```

### **Paso 4: Configurar RestTemplate o FeignClient**

```java
// Config.java en ebill-customers

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

**O con FeignClient (más elegante):**

```java
// BillingServiceClient.java en ebill-customers

@FeignClient(name = "ebill-billing", url = "${ebill.billing.url:http://localhost:8480}")
public interface BillingServiceClient {

    @PostMapping("/api/subscriptions/auto-assign-free/{idCompany}")
    ResponseEntity<Void> autoAssignFreePlan(@PathVariable("idCompany") Long idCompany);
}
```

### **Paso 5: Configurar Período por Defecto del Plan FREE**

**Configuración en application.properties:**

```properties
# application.properties en ebill-billing

# Duración por defecto del plan FREE (en meses) - igual formato que otras suscripciones
subscription.free-plan.default-duration-months=12

# Ejemplos de configuración:
# subscription.free-plan.default-duration-months=1    # 1 mes (prueba corta)
# subscription.free-plan.default-duration-months=3    # 3 meses (trimestre)
# subscription.free-plan.default-duration-months=6    # 6 meses (semestre)
# subscription.free-plan.default-duration-months=12   # 12 meses (1 año) - RECOMENDADO
# subscription.free-plan.default-duration-months=24   # 24 meses (2 años)
```

**Nota Importante:**

- ✅ **Formato consistente:** Usa `durationMonths` igual que las suscripciones manuales
- ✅ **Modificable después:** El Super Admin puede extender/modificar desde la UI usando los diálogos existentes
- ✅ **Valores por defecto:** Solo se usan si no se proporcionan parámetros en el request

---

## ✅ **VERIFICACIÓN**

### **1. Probar Creación de Compañía:**

```bash
# Crear compañía vía API
curl -X POST http://localhost:8180/api/company \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "identification": "9999999999001",
    "name": "Compañía Test FREE",
    ...
  }'

# Verificar que se creó la suscripción en ebill-billing
curl -X GET http://localhost:8480/api/subscriptions/company/3 \
  -H "Authorization: Bearer TOKEN"
```

### **2. Verificar Logs:**

- ✅ Logs en `ebill-customers`: "Plan FREE asignado automáticamente..."
- ✅ Logs en `ebill-billing`: "Plan FREE asignado automáticamente..."
- ⚠️ Si falla: Logs de error pero compañía creada

---

## 🔍 **CASOS DE PRUEBA**

### **Caso 1: Crear Compañía Normal**

```
✅ Resultado esperado:
   - Compañía creada en ebill-customers
   - Suscripción FREE creada en ebill-billing
   - Status 201 en ambos servicios
```

### **Caso 2: ebill-billing No Disponible**

```
⚠️ Resultado esperado:
   - Compañía creada exitosamente
   - Error loggeado pero no bloquea creación
   - Super Admin puede asignar suscripción manualmente después
```

### **Caso 3: Plan FREE No Existe**

```
⚠️ Resultado esperado:
   - Compañía creada exitosamente
   - Error 500 en ebill-billing loggeado
   - Super Admin debe asignar suscripción manualmente
```

### **Caso 4: Compañía Ya Tiene Suscripción (caso raro)**

```
✅ Resultado esperado:
   - Compañía creada exitosamente
   - ebill-billing detecta suscripción existente
   - No se duplica, retorna 200 OK
```

---

## 📝 **CONSIDERACIONES IMPORTANTES**

### **1. Manejo de Errores:**

- ⚠️ **NO bloquear** la creación de compañía si falla la asignación de suscripción
- ✅ **Loggear** todos los errores para debugging
- ✅ **Permitir** asignación manual posterior si falla automática

### **2. Comunicación Asíncrona (Opcional pero Recomendado):**

```java
// Mejor UX: No bloquear respuesta al usuario
@Async
public void assignFreePlanAsync(Long idCompany) {
    try {
        billingServiceClient.autoAssignFreePlan(idCompany);
    } catch (Exception e) {
        log.error("Error asíncrono al asignar FREE", e);
    }
}
```

### **3. Timeout y Reintentos:**

```java
// Configurar timeout corto para no bloquear
restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
HttpComponentsClientHttpRequestFactory factory =
    (HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory();
factory.setConnectTimeout(3000); // 3 segundos
factory.setReadTimeout(5000);     // 5 segundos
```

---

## 🎯 **IMPLEMENTACIÓN RECOMENDADA**

**Flujo Completo:**

1. ✅ **ebill-customers:** Crear compañía → Llamar HTTP a `ebill-billing`
2. ✅ **ebill-billing:** Recibir llamada → Validar → Asignar FREE → Retornar 201
3. ✅ **Si falla:** Loggear error pero NO bloquear creación de compañía
4. ✅ **Super Admin:** Puede asignar manualmente si falla automática

---

## 📋 **CHECKLIST DE IMPLEMENTACIÓN**

### **En `ebill-billing` (Puerto 8480):**

- [ ] Crear endpoint: `POST /api/subscriptions/auto-assign-free/{idCompany}`
- [ ] Implementar método `assignFreePlan(Long idCompany)` en `SubscriptionService`
- [ ] **Configurar período del plan FREE** (application.properties o tabla de configuración)
- [ ] Implementar método `calculateFreePlanExpirationDate()` con lógica configurable
- [ ] Validar que plan FREE existe
- [ ] Validar que compañía no tiene suscripción activa
- [ ] Crear registro en `company_subscription` con plan FREE y fecha de expiración calculada
- [ ] Agregar logs informativos (incluyendo fecha de expiración)
- [ ] Manejar excepciones (plan no existe, compañía ya tiene suscripción, etc.)
- [ ] Documentar configuración del período en README o documentación del proyecto

### **En `ebill-customers` (Puerto 8180):**

- [ ] Configurar RestTemplate o FeignClient para comunicarse con `ebill-billing`
- [ ] Agregar propiedad de configuración: `ebill.billing.url=http://localhost:8480`
- [ ] Modificar método `createCompany()` para llamar al endpoint después de crear
- [ ] Implementar manejo de errores (NO bloquear creación si falla asignación)
- [ ] Agregar logs para debugging
- [ ] Considerar implementación asíncrona (opcional pero recomendado)

### **Pruebas:**

- [ ] Crear compañía nueva → Verificar que se asigna FREE automáticamente
- [ ] Verificar que la fecha de expiración es correcta según configuración
- [ ] Cambiar configuración del período → Crear nueva compañía → Verificar nuevo período
- [ ] Verificar que aparece en lista de suscripciones (`/api/subscriptions`)
- [ ] Verificar que puede emitir documentos
- [ ] Probar caso de error (ebill-billing caído) → Compañía se crea pero sin suscripción
- [ ] Probar asignación manual después de fallo automático
- [ ] Verificar logs en ambos microservicios (incluyendo fecha de expiración)
- [ ] Probar diferentes configuraciones (30 días, 90 días, 1 año, 10 años)

---

## ⚙️ **CONFIGURACIÓN DEL PERÍODO FREE**

### **Enfoque: Consistente con Suscripciones Manuales**

El plan FREE usa el **mismo formato** que las suscripciones manuales:

- ✅ **Unidad:** Meses (`durationMonths`)
- ✅ **Formato:** Igual que `POST /api/subscriptions` (asignación manual)
- ✅ **Modificable:** El Super Admin puede extender/modificar desde la UI después

### **Valores Recomendados por Escenario:**

| Escenario             | Duración | Configuración                              |
| --------------------- | -------- | ------------------------------------------ |
| **Prueba/Trial**      | 1 mes    | `default-duration-months=1`                |
| **Trimestre**         | 3 meses  | `default-duration-months=3`                |
| **Semestre**          | 6 meses  | `default-duration-months=6`                |
| **Año Estándar**      | 12 meses | `default-duration-months=12` (RECOMENDADO) |
| **Período Extendido** | 24 meses | `default-duration-months=24`               |

### **Ejemplo de application.properties:**

```properties
# Duración por defecto del plan FREE (en meses)
# Valor por defecto: 12 meses (1 año)
subscription.free-plan.default-duration-months=12

# Para cambiar a 1 mes de prueba:
# subscription.free-plan.default-duration-months=1

# Para cambiar a 3 meses (trimestre):
# subscription.free-plan.default-duration-months=3

# Para cambiar a 24 meses (2 años):
# subscription.free-plan.default-duration-months=24
```

### **Modificación desde la UI:**

Después de que se asigna automáticamente el plan FREE, el Super Admin puede:

1. **Extender la suscripción:**

   - Usar el diálogo "Extender Suscripción"
   - Agregar meses adicionales

2. **Cambiar el plan:**

   - Usar el diálogo "Cambiar Plan"
   - Migrar de FREE a BASIC, PROFESSIONAL, etc.

3. **Ver detalles:**
   - Ver la suscripción en la lista de suscripciones
   - Ver fecha de expiración, uso actual, etc.

### **Notas Importantes:**

- ⚠️ **Cambios en producción:** Si cambias el período por defecto, solo afecta a compañías NUEVAS
- ✅ **Compañías existentes:** Mantienen su fecha de expiración original
- ✅ **Modificación posterior:** Pueden extenderse/modificarse desde la UI sin problemas
- 🔄 **Reinicio requerido:** Cambios en `application.properties` requieren reiniciar el servicio
- 💡 **Flexibilidad:** El endpoint acepta parámetros opcionales para personalizar el período al crear
