# 🔗 GUÍA DE INTEGRACIÓN FRONTEND-BACKEND

**Fecha:** 14 de Diciembre, 2025  
**Estado:** ✅ Backend Implementado - Listo para Integración

---

## 🎯 **Objetivo**

Verificar que la integración completa entre el frontend Angular y los microservicios backend (authentication + billing) funcione correctamente.

---

## 📋 **Checklist de Verificación**

### **Backend (Verificar que esté corriendo)**

#### **ebill-authentication (Puerto 8280)**

- [ ] Servicio corriendo en `http://localhost:8280`
- [ ] Base de datos con tabla `feature_permission` creada
- [ ] 15 features insertadas en la tabla
- [ ] Endpoint `POST /api/permission/validate-feature` funcionando
- [ ] RestTemplate configurado para llamar a billing

#### **ebill-billing (Puerto 8480)**

- [ ] Servicio corriendo en `http://localhost:8480`
- [ ] Tablas creadas: `subscription_plan`, `company_subscription`, `subscription_usage_history`
- [ ] 5 planes insertados (FREE, BASIC, PRO, BUSINESS, ENTERPRISE)
- [ ] Empresas migradas a plan FREE
- [ ] Endpoint `GET /api/subscription/{id}/current` funcionando
- [ ] Endpoint `GET /api/subscription/{id}/usage` funcionando
- [ ] Endpoint `POST /api/subscription/{id}/can-emit` funcionando
- [ ] Triggers en `invoice` y `credit_note` creados

#### **Frontend (Ya implementado)**

- [x] PermissionService creado
- [x] HasPermissionDirective creada
- [x] PermissionGuard creado
- [x] Rutas protegidas configuradas
- [x] Interfaces TypeScript creadas

---

## 🧪 **Pruebas Manuales**

### **1. Verificar que los servicios backend estén corriendo**

```bash
# Verificar authentication (8280)
curl -X GET http://localhost:8280/actuator/health

# Verificar billing (8480)
curl -X GET http://localhost:8480/actuator/health
```

**Resultado esperado:** `{"status":"UP"}`

---

### **2. Probar endpoint de validación de features**

```bash
# Test 1: Verificar acceso al dashboard (BASIC+)
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "featureCode": "dashboard.view",
    "idCompany": 1,
    "idUser": 1
  }'
```

**Resultado esperado:**

```json
{
  "allowed": true,
  "message": "Acceso permitido"
}
```

**Test 2: Verificar acceso a reportes avanzados (PRO+)**

```bash
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "featureCode": "reports.advanced",
    "idCompany": 1,
    "idUser": 1
  }'
```

**Resultado esperado (si es plan FREE/BASIC):**

```json
{
  "allowed": false,
  "reason": "NO_SUBSCRIPTION",
  "message": "Su plan actual no incluye esta funcionalidad. Actualice su suscripción."
}
```

---

### **3. Probar endpoint de suscripción actual**

```bash
# Obtener suscripción de la compañía 1
curl -X GET http://localhost:8480/api/subscription/1/current \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Resultado esperado:**

```json
{
  "idSubscription": 1,
  "idCompany": 1,
  "idPlan": 1,
  "plan": {
    "idPlan": 1,
    "code": "FREE",
    "name": "Plan Gratuito",
    "priceAnnual": 0,
    "documentLimitYearly": 50,
    "hasDashboard": false,
    "hasAdvancedReports": false,
    "hasDrafts": false,
    ...
  },
  "status": "ACTIVE",
  "startDate": "2025-01-01T00:00:00",
  "expiresAt": "2026-01-01T00:00:00",
  "currentYearUsage": 10,
  "percentUsed": 20,
  "documentsRemaining": 40
}
```

---

### **4. Probar endpoint de verificación de emisión**

```bash
# Verificar si puede emitir una factura
curl -X POST http://localhost:8480/api/subscription/1/can-emit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "documentType": "INVOICE"
  }'
```

**Resultado esperado (si tiene cuota disponible):**

```json
{
  "allowed": true,
  "currentUsage": 10,
  "limit": 50,
  "remaining": 40,
  "message": "Puede emitir documento. Quedan 40 de 50 documentos disponibles."
}
```

**Resultado esperado (si alcanzó el límite):**

```json
{
  "allowed": false,
  "currentUsage": 50,
  "limit": 50,
  "remaining": 0,
  "message": "Ha alcanzado el límite de documentos de su plan. Actualice su suscripción."
}
```

---

### **5. Probar uso de documentos**

```bash
# Obtener estadísticas de uso
curl -X GET http://localhost:8480/api/subscription/1/usage \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Resultado esperado:**

```json
{
  "currentMonth": {
    "month": "2025-12",
    "invoices": 5,
    "creditNotes": 2,
    "total": 7
  },
  "currentYear": {
    "year": 2025,
    "total": 10,
    "limit": 50,
    "percentUsed": 20,
    "remaining": 40
  },
  "lastMonths": [
    {
      "month": "2025-11",
      "invoices": 3,
      "creditNotes": 0,
      "total": 3
    }
  ]
}
```

---

## 🌐 **Pruebas en el Frontend**

### **1. Iniciar el frontend**

```bash
cd ebilling-web-app
ng serve
```

Abrir navegador en `http://localhost:4200`

---

### **2. Verificar consola del navegador**

Al hacer login, deberías ver en la consola:

```
🔔 Toast [INFO]: Sesión iniciada correctamente
📊 Cargando suscripción...
✅ Suscripción cargada: Plan FREE
```

Si hay error:

```
❌ Error al cargar suscripción: [detalles del error]
🔄 Usando datos en caché de localStorage
```

---

### **3. Probar Rutas Protegidas**

#### **Test 1: Dashboard (/welcome)**

**Con plan FREE (sin dashboard):**

1. Navegar a `/welcome`
2. **Resultado esperado:** Toast aparece: "🔒 Esta funcionalidad requiere un plan superior"
3. Redirige a `/login` o bloquea acceso

**Con plan BASIC+:**

1. Navegar a `/welcome`
2. **Resultado esperado:** Dashboard se muestra correctamente

---

#### **Test 2: Reportes Avanzados (/reports/sales-dashboard)**

**Con plan FREE/BASIC:**

1. Navegar a `/reports/sales-dashboard`
2. **Resultado esperado:** Toast: "🔒 Esta funcionalidad requiere un plan superior"
3. Redirige a `/welcome`

**Con plan PRO+:**

1. Navegar a `/reports/sales-dashboard`
2. **Resultado esperado:** Reporte se muestra correctamente

---

#### **Test 3: Borradores (/invoices/drafts)**

**Con plan FREE:**

1. Navegar a `/invoices/drafts`
2. **Resultado esperado:** Toast: "🔒 Esta funcionalidad requiere un plan superior"
3. Redirige a `/welcome`

**Con plan BASIC+:**

1. Navegar a `/invoices/drafts`
2. **Resultado esperado:** Lista de borradores se muestra

---

### **4. Probar Directiva \*hasPermission**

#### **Test en Templates**

Si en cualquier componente tienes:

```html
<button *hasPermission="'reports.advanced'">📊 Reportes Avanzados</button>
```

**Con plan FREE/BASIC:** Botón NO se muestra  
**Con plan PRO+:** Botón SÍ se muestra

---

### **5. Probar PermissionService en Componentes**

Abre la consola del navegador y ejecuta:

```javascript
// Obtener el servicio (en Angular DevTools)
const permissionService = ng.probe($0).injector.get("PermissionService");

// Verificar suscripción
permissionService.getSubscription().subscribe((sub) => {
  console.log("Plan:", sub?.plan.name);
  console.log("Documentos restantes:", sub?.documentsRemaining);
});

// Verificar acceso
permissionService.canAccess("dashboard.view").subscribe((allowed) => {
  console.log("Puede acceder al dashboard:", allowed);
});

// Verificar si puede emitir
permissionService.canEmitDocument("INVOICE").subscribe((allowed) => {
  console.log("Puede emitir factura:", allowed);
});
```

---

## 🐛 **Troubleshooting**

### **Error: Backend no responde**

**Síntoma:** Toast rojo: "Error al verificar permisos"

**Solución:**

1. Verificar que los servicios backend estén corriendo
2. Verificar CORS en backend (debe permitir `http://localhost:4200`)
3. Verificar URL en `environment.ts`:
   ```typescript
   HOST_AUTH: 'http://localhost:8280',
   HOST_EBILLING: 'http://localhost:8480',
   ```

---

### **Error: 401 Unauthorized**

**Síntoma:** Requests fallan con 401

**Solución:**

1. Verificar que el token JWT esté en localStorage
2. Verificar que el token no esté expirado
3. Verificar que el interceptor esté agregando el header Authorization

---

### **Error: Suscripción siempre es null**

**Síntoma:** `this.permissionService.getSubscription()` retorna null

**Solución:**

1. Verificar en Network tab que el request a `/api/subscription/{id}/current` se haga
2. Si falla, verificar que la compañía tenga una suscripción en la BD
3. Verificar que el `idCompany` en el token JWT sea correcto

---

### **Error: Directiva \*hasPermission no funciona**

**Síntoma:** Elementos se muestran/ocultan incorrectamente

**Solución:**

1. Verificar que `HasPermissionDirective` esté en `SharedModule.exports`
2. Verificar que el módulo que usa la directiva importe `SharedModule`
3. Verificar sintaxis: `*hasPermission="'feature.code'"` (con comillas simples internas)

---

### **Error: Guard no redirige**

**Síntoma:** Usuario puede acceder a rutas sin permiso

**Solución:**

1. Verificar que `PermissionGuard` esté en el array `canActivate` de la ruta
2. Verificar que `data: { permission: 'feature.code' }` esté en la configuración de la ruta
3. Verificar orden de guards: `[SessionValidationGuard, PermissionGuard]`

---

## 📊 **Casos de Prueba Completos**

### **Escenario 1: Usuario con plan FREE**

| Acción                               | Resultado Esperado              |
| ------------------------------------ | ------------------------------- |
| Login                                | ✅ Exitoso                      |
| Navegar a `/welcome`                 | ❌ Bloqueado (toast + redirect) |
| Navegar a `/reports/sales-dashboard` | ❌ Bloqueado                    |
| Navegar a `/invoices/drafts`         | ❌ Bloqueado                    |
| Crear 50 facturas                    | ✅ Permitido                    |
| Crear factura #51                    | ❌ Bloqueado (límite alcanzado) |

---

### **Escenario 2: Usuario con plan BASIC**

| Acción                               | Resultado Esperado              |
| ------------------------------------ | ------------------------------- |
| Login                                | ✅ Exitoso                      |
| Navegar a `/welcome`                 | ✅ Permitido (dashboard básico) |
| Navegar a `/reports/sales-dashboard` | ❌ Bloqueado (requiere PRO)     |
| Navegar a `/invoices/drafts`         | ✅ Permitido                    |
| Crear 500 facturas                   | ✅ Permitido                    |
| Crear factura #501                   | ❌ Bloqueado                    |

---

### **Escenario 3: Usuario con plan PRO**

| Acción                                 | Resultado Esperado |
| -------------------------------------- | ------------------ |
| Login                                  | ✅ Exitoso         |
| Navegar a `/welcome`                   | ✅ Permitido       |
| Navegar a `/reports/sales-dashboard`   | ✅ Permitido       |
| Navegar a `/reports/branch-comparison` | ✅ Permitido       |
| Navegar a `/invoices/drafts`           | ✅ Permitido       |
| Crear 5000 facturas                    | ✅ Permitido       |
| Crear factura #5001                    | ❌ Bloqueado       |

---

### **Escenario 4: Super Admin**

| Acción                    | Resultado Esperado            |
| ------------------------- | ----------------------------- |
| Login                     | ✅ Exitoso                    |
| Navegar a cualquier ruta  | ✅ SIEMPRE permitido (bypass) |
| Crear facturas ilimitadas | ✅ SIEMPRE permitido          |

---

## 🚀 **Script de Prueba Automatizado**

Guardar como `test-integration.sh`:

```bash
#!/bin/bash

echo "🧪 PRUEBAS DE INTEGRACIÓN FRONTEND-BACKEND"
echo "=========================================="
echo ""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
AUTH_URL="http://localhost:8280"
BILLING_URL="http://localhost:8480"
TOKEN="YOUR_JWT_TOKEN_HERE"

# Test 1: Health checks
echo "1️⃣ Verificando servicios..."
if curl -s "$AUTH_URL/actuator/health" | grep -q "UP"; then
  echo -e "${GREEN}✅ Authentication service OK${NC}"
else
  echo -e "${RED}❌ Authentication service FAILED${NC}"
fi

if curl -s "$BILLING_URL/actuator/health" | grep -q "UP"; then
  echo -e "${GREEN}✅ Billing service OK${NC}"
else
  echo -e "${RED}❌ Billing service FAILED${NC}"
fi

echo ""

# Test 2: Validate feature
echo "2️⃣ Probando validación de features..."
RESPONSE=$(curl -s -X POST "$AUTH_URL/api/permission/validate-feature" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"featureCode":"dashboard.view","idCompany":1,"idUser":1}')

if echo "$RESPONSE" | grep -q "allowed"; then
  echo -e "${GREEN}✅ Validate feature endpoint OK${NC}"
  echo "   Response: $RESPONSE"
else
  echo -e "${RED}❌ Validate feature endpoint FAILED${NC}"
fi

echo ""

# Test 3: Get subscription
echo "3️⃣ Probando obtención de suscripción..."
RESPONSE=$(curl -s -X GET "$BILLING_URL/api/subscription/1/current" \
  -H "Authorization: Bearer $TOKEN")

if echo "$RESPONSE" | grep -q "plan"; then
  echo -e "${GREEN}✅ Get subscription endpoint OK${NC}"
  echo "   Plan: $(echo $RESPONSE | jq -r '.plan.name')"
else
  echo -e "${RED}❌ Get subscription endpoint FAILED${NC}"
fi

echo ""

# Test 4: Can emit document
echo "4️⃣ Probando verificación de emisión..."
RESPONSE=$(curl -s -X POST "$BILLING_URL/api/subscription/1/can-emit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"documentType":"INVOICE"}')

if echo "$RESPONSE" | grep -q "allowed"; then
  echo -e "${GREEN}✅ Can emit endpoint OK${NC}"
  echo "   Allowed: $(echo $RESPONSE | jq -r '.allowed')"
else
  echo -e "${RED}❌ Can emit endpoint FAILED${NC}"
fi

echo ""
echo "=========================================="
echo "🎉 Pruebas completadas!"
```

**Uso:**

```bash
chmod +x test-integration.sh
./test-integration.sh
```

---

## ✅ **Checklist Final de Integración**

- [ ] Servicios backend corriendo (8280, 8480)
- [ ] Base de datos con todas las tablas creadas
- [ ] Features y planes insertados
- [ ] Triggers configurados
- [ ] Frontend corriendo (4200)
- [ ] Login exitoso
- [ ] Suscripción se carga en frontend
- [ ] Rutas protegidas funcionan según plan
- [ ] Directiva \*hasPermission oculta/muestra elementos
- [ ] Límites de documentos se respetan
- [ ] Super admin tiene bypass completo
- [ ] Mensajes de error son claros
- [ ] Caché localStorage funciona como fallback

---

## 📚 **Documentación Relacionada**

- `FRONTEND-PERMISSIONS-IMPLEMENTATION.md` - Guía de uso del frontend
- `FRONTEND-IMPLEMENTATION-SUMMARY.md` - Resumen de la implementación
- `BACKEND-CHECKLIST-MICROSERVICIOS.md` - Checklist de backend
- `FASE1-IMPLEMENTACION-COMPLETA.md` - Especificación completa

---

## 🎉 **¡Integración Completa!**

Si todos los tests pasan, tu sistema de permisos basado en ROL + SUSCRIPCIÓN está funcionando correctamente end-to-end.

**🚀 ¡Listo para producción!**
