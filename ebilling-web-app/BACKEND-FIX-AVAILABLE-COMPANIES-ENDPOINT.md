# 🔧 Fix: Endpoint `/api/subscriptions/available-companies` - Error 500

**Fecha:** 21 de Diciembre, 2025  
**Microservicio:** `ebill-billing` (Puerto 8480)  
**Problema:** El endpoint devuelve error 500 y no incluye compañías "sueltas" (sin registro en `company_subscription`)

---

## 🔴 **PROBLEMA ACTUAL**

1. **Error 500** al llamar al endpoint
2. **No incluye compañías sin registro** en `company_subscription` (compañías "sueltas")
3. Solo muestra compañías que tienen registro pero con `status != 'ACTIVE'`

---

## ✅ **SOLUCIÓN REQUERIDA**

### **1. Incluir TODAS las compañías sin suscripción activa:**

El endpoint debe devolver:
- ✅ Compañías **sin registro** en `company_subscription` (nuevas, nunca tuvieron suscripción)
- ✅ Compañías con suscripción **EXPIRED**, **CANCELLED**, **SUSPENDED**
- ✅ Compañías con suscripción **vencida** (`expires_at < NOW()`)

### **2. Validar compañías "sueltas":**

Si una compañía existe en `ebill-customers.company` pero NO tiene registro en `ebill-billing.company_subscription`, debe aparecer en la lista.

---

## 🔧 **IMPLEMENTACIÓN**

### **Opción A: Llamada HTTP entre Microservicios (Recomendado)**

```java
@Service
public class SubscriptionService {
    
    @Autowired
    private CompanyClientService companyClientService; // Cliente HTTP para ebill-customers
    
    @Autowired
    private CompanySubscriptionRepository subscriptionRepository;
    
    public List<CompanyWithoutSubscriptionDTO> getCompaniesWithoutSubscription() {
        // 1. Obtener TODAS las compañías activas de ebill-customers
        List<CompanyDTO> allCompanies = companyClientService.findAllActiveCompanies();
        
        // 2. Obtener IDs de compañías con suscripción activa (de ebill-billing)
        Set<Long> companiesWithActiveSubscription = subscriptionRepository
            .findCompanyIdsWithActiveSubscription();
        
        // 3. Filtrar y mapear
        return allCompanies.stream()
            .filter(company -> !companiesWithActiveSubscription.contains(company.getIdCompany()))
            .map(company -> new CompanyWithoutSubscriptionDTO(
                company.getIdCompany(),
                company.getName(), // o businessName según el DTO
                company.getIdentification() // o ruc según el DTO
            ))
            .sorted(Comparator.comparing(CompanyWithoutSubscriptionDTO::getBusinessName))
            .collect(Collectors.toList());
    }
    
    // Método helper en el Repository
    @Query("SELECT DISTINCT cs.idCompany FROM CompanySubscription cs " +
           "WHERE cs.subscriptionStatus = 'ACTIVE' " +
           "AND cs.expiresAt > CURRENT_DATE")
    Set<Long> findCompanyIdsWithActiveSubscription();
}
```

### **Opción B: Query Cross-Database (Si están en la misma BD)**

```java
@Query(value = 
    "SELECT c.id_company AS idCompany, " +
    "       c.name AS businessName, " +
    "       c.identification AS ruc " +
    "FROM ebill_customers.company c " +
    "LEFT JOIN ebill_billing.company_subscription cs " +
    "    ON c.id_company = cs.id_company " +
    "    AND cs.subscription_status = 'ACTIVE' " +
    "    AND cs.expires_at > NOW() " +
    "WHERE cs.id_company_subscription IS NULL " +
    "  AND c.status = true " +
    "ORDER BY c.name ASC",
    nativeQuery = true)
List<CompanyWithoutSubscriptionDTO> findCompaniesWithoutActiveSubscription();
```

---

## 🧪 **PRUEBAS**

### **Caso 1: Compañía Nueva (Sin Registro)**
```
Compañía ID=3 en ebill-customers.company
NO tiene registro en ebill-billing.company_subscription
✅ Debe aparecer en la lista
```

### **Caso 2: Compañía con Suscripción EXPIRED**
```
Compañía ID=5 tiene registro en company_subscription
subscription_status = 'EXPIRED'
✅ Debe aparecer en la lista
```

### **Caso 3: Compañía con Suscripción ACTIVA**
```
Compañía ID=1 tiene registro en company_subscription
subscription_status = 'ACTIVE' y expires_at > NOW()
❌ NO debe aparecer en la lista
```

---

## 📋 **CHECKLIST DE IMPLEMENTACIÓN**

- [ ] Identificar cómo se comunica `ebill-billing` con `ebill-customers` (HTTP client, Feign, etc.)
- [ ] Crear método en `CompanyClientService` para obtener todas las compañías activas
- [ ] Crear método en `CompanySubscriptionRepository` para obtener IDs con suscripción activa
- [ ] Implementar lógica de filtrado en el servicio
- [ ] Manejar errores gracefully (si falla la llamada a `ebill-customers`)
- [ ] Agregar logs para debugging
- [ ] Probar con compañía nueva (sin registro)
- [ ] Probar con compañía con suscripción EXPIRED
- [ ] Verificar que compañías con suscripción ACTIVA NO aparecen
- [ ] Verificar que el error 500 se resuelve

---

## 🔍 **DEBUGGING**

### **Logs a Agregar:**

```java
log.info("Total compañías activas en ebill-customers: {}", allCompanies.size());
log.info("Compañías con suscripción activa: {}", companiesWithActiveSubscription.size());
log.info("Compañías sin suscripción activa (resultado): {}", result.size());
```

### **Verificar en Base de Datos:**

```sql
-- Ver compañías sin suscripción
SELECT c.id_company, c.name, c.identification
FROM company c
LEFT JOIN company_subscription cs
    ON c.id_company = cs.id_company
    AND cs.subscription_status = 'ACTIVE'
    AND cs.expires_at > NOW()
WHERE cs.id_company_subscription IS NULL
  AND c.status = true;
```

---

## ✅ **RESULTADO ESPERADO**

Después del fix:
- ✅ El endpoint devuelve 200 OK (no más error 500)
- ✅ Incluye compañías nuevas sin registro en `company_subscription`
- ✅ Incluye compañías con suscripción inactiva/vencida
- ✅ Excluye compañías con suscripción activa
- ✅ La compañía "suelta" que mencionaste aparece en la lista

