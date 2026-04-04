# 📋 Menús Mostrados para Rol "Member" (Miembro)

## 🔍 Explicación del Filtrado

Cuando un usuario tiene el rol **"member"**, el sistema filtra los menús de la siguiente manera:

### 1. **Proceso de Filtrado**

```
1. Backend retorna TODOS los menús asignados al usuario
   ↓
2. MenuFilterService verifica:
   - ¿Es super_admin? → NO (es member)
   - ¿Tiene suscripción activa? → SÍ/NO (depende del plan)
   ↓
3. Para cada menú con featureCode:
   - Llama a PermissionService.canAccess(featureCode)
   - Backend valida: ROL + PLAN
   - Si tiene permiso → Mostrar menú
   - Si NO tiene permiso → Ocultar menú
```

### 2. **Validación de Permisos**

El backend valida **2 condiciones**:

1. **ROL:** ¿El rol "member" tiene permiso para esta feature?
2. **PLAN:** ¿El plan de suscripción incluye esta feature?

**Ambas condiciones deben cumplirse** para mostrar el menú.

### 3. **Menús por Plan de Suscripción**

#### **Plan FREE (Gratuito)**

Según la documentación, un usuario "member" con plan FREE debería tener acceso a:

| Menú | Feature Code | ¿Se Muestra? |
|------|--------------|--------------|
| **Borradores** | `DRAFT_MANAGEMENT` | ✅ SÍ (si el backend lo permite) |
| **Clientes** | `CLIENT_CREATE` | ✅ SÍ (si el backend lo permite) |
| **Nota de Crédito** | `CREDIT_NOTE_CREATE` | ✅ SÍ (si el backend lo permite) |
| **Facturación** | `INVOICE_CREATE` | ❌ NO (requiere rol admin/accountant/operator) |
| **Productos** | `PRODUCT_MANAGEMENT` | ❌ NO (requiere plan superior) |
| **Usuarios** | `USER_MANAGEMENT` | ❌ NO (requiere rol admin) |
| **Compañía** | `COMPANY_SETTINGS` | ❌ NO (requiere rol admin) |
| **Reportes Avanzados** | `REPORT_SALES_DASHBOARD` | ❌ NO (requiere plan PROFESSIONAL+) |

#### **Plan BASIC**

Un usuario "member" con plan BASIC tendría acceso a:

| Menú | Feature Code | ¿Se Muestra? |
|------|--------------|--------------|
| Todo lo de FREE | - | ✅ SÍ |
| **Nota de Crédito** | `CREDIT_NOTE_CREATE` | ✅ SÍ |
| **Usuarios** | `USER_MANAGEMENT` | ❌ NO (requiere rol admin) |
| **Compañía** | `COMPANY_SETTINGS` | ❌ NO (requiere rol admin) |
| **Reportes Avanzados** | `REPORT_SALES_DASHBOARD` | ❌ NO (requiere plan PROFESSIONAL+) |

#### **Plan PROFESSIONAL**

Un usuario "member" con plan PROFESSIONAL tendría acceso a:

| Menú | Feature Code | ¿Se Muestra? |
|------|--------------|--------------|
| Todo lo de BASIC | - | ✅ SÍ |
| **Reportes Avanzados** | `REPORT_SALES_DASHBOARD` | ✅ SÍ (si el backend permite al rol member) |
| **Comparación de Sucursales** | `REPORT_BRANCH_COMPARISON` | ✅ SÍ (si el backend permite al rol member) |
| **Usuarios** | `USER_MANAGEMENT` | ❌ NO (requiere rol admin) |
| **Compañía** | `COMPANY_SETTINGS` | ❌ NO (requiere rol admin) |

### 4. **Menús que NUNCA se Muestran a "member"**

Independientemente del plan, estos menús **NO se muestran** a usuarios con rol "member":

- ❌ **Usuarios** (`USER_MANAGEMENT`) - Requiere rol `admin`
- ❌ **Compañía** (`COMPANY_SETTINGS`) - Requiere rol `admin`
- ❌ **Administración Global** (`/admin/*`) - Requiere rol `super_admin`
- ❌ **Gestión de Permisos** - Requiere rol `super_admin`

### 5. **Cómo Verificar en Tiempo Real**

Al ejecutar la aplicación, los logs en la consola mostrarán:

```
📊 [MenuFilterService] ========== RESUMEN DE FILTRADO ==========
📊 [MenuFilterService] Rol del usuario: ['member']
📊 [MenuFilterService] Plan de suscripción: FREE
📊 [MenuFilterService] Total de menús recibidos: 7
📊 [MenuFilterService] Total de menús después de filtrar: 3
📊 [MenuFilterService] Menús mostrados:
   ✅ Borradores (/drafts) - Feature: DRAFT_MANAGEMENT
   ✅ Clientes (/clients) - Feature: CLIENT_CREATE
   ✅ Nota de Crédito (/credit-notes) - Feature: CREDIT_NOTE_CREATE
📊 [MenuFilterService] Menús ocultos:
   ❌ Facturación (/invoices) - Feature: INVOICE_CREATE
   ❌ Productos (/products) - Feature: PRODUCT_MANAGEMENT
   ❌ Usuarios (/users) - Feature: USER_MANAGEMENT
   ❌ Compañía (/companies) - Feature: COMPANY_SETTINGS
===========================================================
```

### 6. **Validación en el Backend**

El backend valida cada feature con este endpoint:

```
GET /api/permission/validate-feature?featureCode={featureCode}&idCompany={idCompany}
```

**Respuesta si tiene permiso:**
```json
{
  "allowed": true,
  "reason": "HAS_PERMISSION",
  "message": "Access granted"
}
```

**Respuesta si NO tiene permiso:**
```json
{
  "allowed": false,
  "reason": "NO_ROLE" | "PLAN_DOES_NOT_INCLUDE_FEATURE",
  "message": "Tu rol no tiene permisos para esta función"
}
```

### 7. **Notas Importantes**

1. **El backend es la fuente de verdad:** El frontend solo filtra según la respuesta del backend
2. **Los permisos pueden cambiar:** Si el backend actualiza los permisos del rol "member", los menús se actualizarán automáticamente
3. **Sin suscripción activa:** Si no hay suscripción, se muestran todos los menús temporalmente (comportamiento de fallback)
4. **Menús sin featureCode:** Los menús que no tienen `featureCode` siempre se muestran (rutas públicas)

### 8. **Ejemplo Práctico**

**Usuario:** Juan Pérez  
**Rol:** member  
**Plan:** FREE  
**Compañía:** 22

**Menús que verá:**
- ✅ Borradores (si el backend permite `DRAFT_MANAGEMENT` para member)
- ✅ Clientes (si el backend permite `CLIENT_CREATE` para member)
- ✅ Nota de Crédito (si el backend permite `CREDIT_NOTE_CREATE` para member)

**Menús que NO verá:**
- ❌ Facturación (requiere rol admin/accountant/operator)
- ❌ Productos (requiere plan superior o rol admin)
- ❌ Usuarios (requiere rol admin)
- ❌ Compañía (requiere rol admin)
- ❌ Reportes Avanzados (requiere plan PROFESSIONAL+)

---

**Para ver los menús exactos en tiempo real, revisa los logs en la consola del navegador cuando un usuario "member" inicia sesión.**




