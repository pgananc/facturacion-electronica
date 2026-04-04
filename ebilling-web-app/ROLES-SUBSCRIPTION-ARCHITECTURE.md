# 🔐 Arquitectura de Permisos: Doble Capa (Roles + Suscripción)

**Fecha:** 13 de Diciembre, 2025  
**Contexto:** Implementación de pricing Opción A con control de features por rol + suscripción

---

## 📊 **ROLES ACTUALES EN EL SISTEMA**

### **✅ Roles EXISTENTES en Base de Datos:**

| ID Role | Nombre Técnico | Nombre Display | Descripción                          | Alcance                          |
| ------- | -------------- | -------------- | ------------------------------------ | -------------------------------- |
| **1**   | `super_admin`  | Super Admin    | Owner del proyecto WallpariSoft      | **GLOBAL** (todas las compañías) |
| **2**   | `member`       | Miembro        | Usuario operativo (cajero, vendedor) | **PUNTO DE EMISIÓN**             |
| **3**   | `admin`        | Administrador  | Dueño/Admin de la compañía           | **COMPAÑÍA** (su company)        |

### **🆕 Rol Propuesto (opcional - a evaluar):**

| ID Role | Nombre Técnico | Nombre Display | Descripción              | Alcance                                    |
| ------- | -------------- | -------------- | ------------------------ | ------------------------------------------ |
| **4**   | `supervisor`   | Supervisor     | Gerente/Jefe de sucursal | **ESTABLECIMIENTO** (una o más sucursales) |

**Nota:** El rol `supervisor` es **opcional**. Empresas pequeñas pueden funcionar solo con `admin` y `member`. Empresas medianas/grandes con múltiples sucursales se beneficiarían de este rol intermedio.

---

## 🎯 **ARQUITECTURA DE DOBLE CAPA**

### **Capa 1: ROLES (Permisos funcionales)**

Define **QUÉ puede hacer** el usuario dentro de la aplicación.

```
┌─────────────────────────────────────────────────────────────┐
│                   CAPA 1: ROLES                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  super_admin → TODAS las funciones (sin límites)           │
│  admin       → Gestión de empresa (según suscripción)      │
│  supervisor  → Operaciones de sucursal                      │
│  cajero      → Emisión de documentos                        │
│  vendedor    → Solo facturas (limitado)                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### **Capa 2: SUSCRIPCIÓN (Límites de uso)**

Define **CUÁNTO puede usar** y **QUÉ features avanzadas** tiene disponibles.

```
┌─────────────────────────────────────────────────────────────┐
│                CAPA 2: SUSCRIPCIÓN                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  GRATIS       → 50 docs, 1 usuario, features básicas       │
│  BÁSICO       → 500 docs, 2 usuarios, + Dashboard          │
│  PROFESIONAL  → 1,500 docs, 5 usuarios, + Reportes        │
│  EMPRESARIAL  → 5,000 docs, 10 usuarios, + API            │
│  ENTERPRISE   → ∞ docs, ∞ usuarios, TODO                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### **Validación Combinada:**

```typescript
// Pseudo-código de validación
function canAccessFeature(user: User, feature: Feature): boolean {
  // 1. Super Admin siempre tiene acceso (bypass)
  if (user.roles.includes("super_admin")) {
    return true;
  }

  // 2. Verificar permiso de rol
  const hasRolePermission = user.roles.some((role) => ROLE_PERMISSIONS[role].includes(feature));

  // 3. Verificar que el plan incluye la feature
  const hasSubscriptionAccess = SUBSCRIPTION_FEATURES[user.company.plan].includes(feature);

  // 4. Debe cumplir AMBAS condiciones
  return hasRolePermission && hasSubscriptionAccess;
}
```

**Ejemplo:**

```
Usuario: Juan Pérez
Rol: admin
Suscripción: BÁSICO

Pregunta: ¿Puede ver Dashboard de Ventas?
1. ¿Su rol (admin) tiene permiso? → ✅ SÍ
2. ¿Su plan (BÁSICO) incluye Dashboard? → ✅ SÍ
3. RESULTADO: ✅ PERMITIDO
```

```
Usuario: María López
Rol: admin
Suscripción: GRATIS

Pregunta: ¿Puede ver Dashboard de Ventas?
1. ¿Su rol (admin) tiene permiso? → ✅ SÍ
2. ¿Su plan (GRATIS) incluye Dashboard? → ❌ NO
3. RESULTADO: ❌ BLOQUEADO (mostrar upgrade prompt)
```

---

## 📋 **MATRIZ DE PERMISOS POR ROL**

### **ROL 1: SUPER_ADMIN** (WallpariSoft Owner)

**Alcance:** Global (todas las compañías)  
**Restricciones:** Ninguna (bypass de suscripción)

| Módulo                       | Feature                                  | Permiso              |
| ---------------------------- | ---------------------------------------- | -------------------- |
| **Administración Global**    |                                          |                      |
|                              | Ver todas las compañías                  | ✅                   |
|                              | Crear/editar/eliminar compañías          | ✅                   |
|                              | Gestionar suscripciones                  | ✅                   |
|                              | Ver dashboard global                     | ✅                   |
| **Sucursales Global**        |                                          |                      |
|                              | Ver todas las sucursales (multi-company) | ✅                   |
|                              | Crear/editar/eliminar sucursales         | ✅                   |
| **Puntos de Emisión Global** |                                          |                      |
|                              | Ver todos los puntos (multi-company)     | ✅                   |
|                              | Crear/editar/eliminar puntos             | ✅                   |
| **Usuarios**                 |                                          |                      |
|                              | Ver todos los usuarios (multi-company)   | ✅                   |
|                              | Crear/editar/eliminar usuarios           | ✅                   |
|                              | Asignar roles                            | ✅ (todos los roles) |
| **Facturación**              |                                          |                      |
|                              | Ver todas las facturas (multi-company)   | ✅                   |
|                              | Emisión de facturas                      | ✅                   |
|                              | Notas de crédito                         | ✅                   |
|                              | Borradores                               | ✅                   |
| **Reportes**                 |                                          |                      |
|                              | Ver todos los reportes (multi-company)   | ✅                   |
|                              | Exportar datos                           | ✅                   |
| **Configuración**            |                                          |                      |
|                              | Configurar sistema                       | ✅                   |
|                              | Ver logs de auditoría                    | ✅                   |

**🔑 Características clave:**

- ✅ Bypass de validación de suscripción
- ✅ Acceso a panel de administración (`/admin/*`)
- ✅ Puede gestionar cualquier compañía
- ✅ Puede asignar cualquier rol a cualquier usuario
- ✅ Protegido por `AdminGuard`

---

### **ROL 3: ADMIN** (Administrador de Compañía)

**Alcance:** Su compañía  
**Restricciones:** Según plan de suscripción

| Módulo                | Feature                      | GRATIS           | BÁSICO             | PRO                    | EMPRESARIAL            | ENTERPRISE             |
| --------------------- | ---------------------------- | ---------------- | ------------------ | ---------------------- | ---------------------- | ---------------------- |
| **Mi Compañía**       |                              |                  |                    |                        |                        |                        |
|                       | Ver datos de compañía        | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Editar datos de compañía     | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ver suscripción actual       | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Upgrade de plan              | ✅               | ✅                 | ✅                     | ✅                     | ❌ (ya máximo)         |
| **Sucursales**        |                              |                  |                    |                        |                        |                        |
|                       | Ver sucursales propias       | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Crear/editar sucursales      | ✅ (1 max)       | ✅ (1 max)         | ✅ (2 max)             | ✅ (5 max)             | ✅ (∞)                 |
|                       | Eliminar sucursales          | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
| **Puntos de Emisión** |                              |                  |                    |                        |                        |                        |
|                       | Ver puntos propios           | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Crear/editar puntos          | ✅ (1 max)       | ✅ (1 max)         | ✅ (2 max)             | ✅ (5 max)             | ✅ (∞)                 |
|                       | Eliminar puntos              | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
| **Usuarios**          |                              |                  |                    |                        |                        |                        |
|                       | Ver usuarios de mi empresa   | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Crear usuarios               | ✅ (1 max)       | ✅ (2 max)         | ✅ (5 max)             | ✅ (10 max)            | ✅ (∞)                 |
|                       | Editar/eliminar usuarios     | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Asignar roles                | ✅ (solo member) | ✅ (admin, member) | ✅ (todos menos super) | ✅ (todos menos super) | ✅ (todos menos super) |
| **Clientes**          |                              |                  |                    |                        |                        |                        |
|                       | CRUD clientes                | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Importar clientes (CSV)      | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
| **Productos**         |                              |                  |                    |                        |                        |                        |
|                       | CRUD productos               | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Importar productos (CSV)     | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Gestión de inventario        | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
| **Facturación**       |                              |                  |                    |                        |                        |                        |
|                       | Emitir facturas              | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Notas de crédito             | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Notas de débito              | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Retenciones                  | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Guías de remisión            | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Borradores                   | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Límite docs/año              | 50               | 500                | 1,500                  | 5,000                  | ∞                      |
| **Dashboard**         |                              |                  |                    |                        |                        |                        |
|                       | Ver Welcome Dashboard        | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | KPIs en tiempo real          | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Gráfico de tendencia         | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Alertas inteligentes         | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
| **Reportes**          |                              |                  |                    |                        |                        |                        |
|                       | Reportes básicos (PDF/Excel) | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ventas por período           | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ventas por cliente           | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ventas por producto          | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Dashboard de ventas          | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Comparación de sucursales    | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Reportes personalizados      | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | Reportes programados (email) | ❌               | ❌                 | ❌                     | ❌                     | ✅                     |
| **Integraciones**     |                              |                  |                    |                        |                        |                        |
|                       | API REST (lectura)           | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | API REST (escritura)         | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | Webhooks                     | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | Integración ERP              | ❌               | ❌                 | ❌                     | ❌                     | ✅                     |

**🔑 Características clave:**

- ✅ Control total de su compañía
- ✅ Puede crear y gestionar usuarios (según plan)
- ✅ Puede crear sucursales y puntos (según plan)
- ⚠️ Sujeto a límites de suscripción
- ⚠️ No puede acceder a `/admin/*` (solo super_admin)

---

### **ROL 2: MEMBER** (Miembro/Cajero/Vendedor)

**Alcance:** Su punto de emisión asignado  
**Restricciones:** Operaciones básicas de facturación

| Módulo                | Feature                    | Permiso                                         |
| --------------------- | -------------------------- | ----------------------------------------------- |
| **Punto de Emisión**  |                            |                                                 |
|                       | Ver su punto asignado      | ✅                                              |
|                       | Cambiar punto              | ❌ (solo admin/super_admin)                     |
| **Clientes**          |                            |                                                 |
|                       | Ver clientes               | ✅                                              |
|                       | Crear clientes             | ✅                                              |
|                       | Editar clientes            | ⚠️ (solo datos básicos: nombre, mail, teléfono) |
|                       | Eliminar clientes          | ❌                                              |
| **Productos**         |                            |                                                 |
|                       | Ver productos              | ✅                                              |
|                       | Buscar productos           | ✅                                              |
|                       | Crear/editar/eliminar      | ❌ (solo admin)                                 |
| **Facturación**       |                            |                                                 |
|                       | Emitir facturas            | ✅                                              |
|                       | Notas de crédito           | ✅                                              |
|                       | Notas de débito            | ✅                                              |
|                       | Retenciones                | ✅                                              |
|                       | Guías de remisión          | ✅ (según plan)                                 |
|                       | Borradores                 | ✅ (según plan)                                 |
|                       | Ver historial              | ✅ (solo sus docs)                              |
|                       | Editar/eliminar docs       | ❌ (solo borradores propios)                    |
| **Dashboard**         |                            |                                                 |
|                       | Ver Welcome Dashboard      | ⚠️ (solo sus métricas, según plan)              |
|                       | KPIs                       | ⚠️ (solo sus ventas)                            |
| **Reportes**          |                            |                                                 |
|                       | Ver reporte de sus ventas  | ✅                                              |
|                       | Reportes de otros usuarios | ❌                                              |
|                       | Dashboard de ventas        | ❌ (solo admin)                                 |
|                       | Exportar datos             | ⚠️ (solo sus ventas)                            |
| **Usuarios**          |                            |                                                 |
|                       | Ver otros usuarios         | ❌                                              |
|                       | Crear/editar usuarios      | ❌                                              |
| **Sucursales/Puntos** |                            |                                                 |
|                       | Ver sucursales             | ⚠️ (solo la suya)                               |
|                       | Crear/editar               | ❌                                              |

**🔑 Características clave:**

- ✅ Operaciones de caja/ventas básicas
- ✅ Emisión de documentos electrónicos
- ✅ Gestión básica de clientes
- ⚠️ No puede ver ventas de otros usuarios
- ⚠️ No puede gestionar usuarios/sucursales
- ⚠️ No puede modificar productos
- ⚠️ Perfecto para: cajeros, vendedores, personal operativo

---

### **ROL 4: SUPERVISOR** (Opcional - Gerente de Sucursal)

**Alcance:** Su establecimiento (una o varias sucursales asignadas)  
**Restricciones:** Según plan + permisos intermedios

**📝 NOTA:** Este rol es **opcional** y se crearía solo si el cliente lo requiere (empresas con múltiples sucursales).

| Módulo                | Feature                               | Permiso                                   |
| --------------------- | ------------------------------------- | ----------------------------------------- |
| **Mi Sucursal**       |                                       |                                           |
|                       | Ver datos de sucursal(es) asignada(s) | ✅                                        |
|                       | Editar datos                          | ⚠️ (limitado: nombre, teléfono, horarios) |
| **Puntos de Emisión** |                                       |                                           |
|                       | Ver puntos de su(s) sucursal(es)      | ✅                                        |
|                       | Crear/editar puntos                   | ✅ (según plan)                           |
|                       | Eliminar puntos                       | ❌ (solo admin)                           |
| **Usuarios**          |                                       |                                           |
|                       | Ver usuarios de su sucursal           | ✅                                        |
|                       | Crear members                         | ✅ (según plan)                           |
|                       | Editar/eliminar members               | ⚠️ (solo de su sucursal)                  |
|                       | Asignar roles                         | ⚠️ (solo member)                          |
| **Clientes**          |                                       |                                           |
|                       | CRUD clientes                         | ✅                                        |
| **Productos**         |                                       |                                           |
|                       | Ver productos                         | ✅                                        |
|                       | Crear/editar productos                | ✅                                        |
|                       | Eliminar productos                    | ❌ (solo admin)                           |
| **Facturación**       |                                       |                                           |
|                       | Emitir facturas                       | ✅                                        |
|                       | Notas de crédito                      | ✅                                        |
|                       | Retenciones                           | ✅                                        |
|                       | Borradores                            | ✅ (según plan)                           |
|                       | Ver historial                         | ✅ (toda su sucursal)                     |
| **Reportes**          |                                       |                                           |
|                       | Ver reportes de su sucursal           | ✅                                        |
|                       | Exportar datos                        | ✅                                        |
|                       | Dashboard de ventas                   | ✅ (solo su sucursal, según plan)         |
|                       | Reportes de otras sucursales          | ❌                                        |

**🔑 Características clave:**

- ✅ Gestión operativa de sucursal
- ✅ Puede supervisar equipo de members
- ✅ Reportes y métricas de su sucursal
- ⚠️ No puede ver datos de otras sucursales
- ⚠️ No puede modificar configuración global
- ⚠️ Perfecto para: gerentes de tienda, jefes de sucursal

---

**Alcance:** Global (todas las compañías)  
**Restricciones:** Ninguna (bypass de suscripción)

| Módulo                       | Feature                                  | Permiso              |
| ---------------------------- | ---------------------------------------- | -------------------- |
| **Administración Global**    |                                          |                      |
|                              | Ver todas las compañías                  | ✅                   |
|                              | Crear/editar/eliminar compañías          | ✅                   |
|                              | Gestionar suscripciones                  | ✅                   |
|                              | Ver dashboard global                     | ✅                   |
| **Sucursales Global**        |                                          |                      |
|                              | Ver todas las sucursales (multi-company) | ✅                   |
|                              | Crear/editar/eliminar sucursales         | ✅                   |
| **Puntos de Emisión Global** |                                          |                      |
|                              | Ver todos los puntos (multi-company)     | ✅                   |
|                              | Crear/editar/eliminar puntos             | ✅                   |
| **Usuarios**                 |                                          |                      |
|                              | Ver todos los usuarios (multi-company)   | ✅                   |
|                              | Crear/editar/eliminar usuarios           | ✅                   |
|                              | Asignar roles                            | ✅ (todos los roles) |
| **Facturación**              |                                          |                      |
|                              | Ver todas las facturas (multi-company)   | ✅                   |
|                              | Emisión de facturas                      | ✅                   |
|                              | Notas de crédito                         | ✅                   |
|                              | Borradores                               | ✅                   |
| **Reportes**                 |                                          |                      |
|                              | Ver todos los reportes (multi-company)   | ✅                   |
|                              | Exportar datos                           | ✅                   |
| **Configuración**            |                                          |                      |
|                              | Configurar sistema                       | ✅                   |
|                              | Ver logs de auditoría                    | ✅                   |

**🔑 Características clave:**

- ✅ Bypass de validación de suscripción
- ✅ Acceso a panel de administración (`/admin/*`)
- ✅ Puede gestionar cualquier compañía
- ✅ Puede asignar cualquier rol a cualquier usuario
- ✅ Protegido por `AdminGuard`

---

### **ROL 1: ADMIN** (Administrador de Compañía)

**Alcance:** Su compañía  
**Restricciones:** Según plan de suscripción

| Módulo                | Feature                      | GRATIS           | BÁSICO             | PRO                    | EMPRESARIAL            | ENTERPRISE             |
| --------------------- | ---------------------------- | ---------------- | ------------------ | ---------------------- | ---------------------- | ---------------------- |
| **Mi Compañía**       |                              |                  |                    |                        |                        |                        |
|                       | Ver datos de compañía        | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Editar datos de compañía     | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ver suscripción actual       | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Upgrade de plan              | ✅               | ✅                 | ✅                     | ✅                     | ❌ (ya máximo)         |
| **Sucursales**        |                              |                  |                    |                        |                        |                        |
|                       | Ver sucursales propias       | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Crear/editar sucursales      | ✅ (1 max)       | ✅ (1 max)         | ✅ (2 max)             | ✅ (5 max)             | ✅ (∞)                 |
|                       | Eliminar sucursales          | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
| **Puntos de Emisión** |                              |                  |                    |                        |                        |                        |
|                       | Ver puntos propios           | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Crear/editar puntos          | ✅ (1 max)       | ✅ (1 max)         | ✅ (2 max)             | ✅ (5 max)             | ✅ (∞)                 |
|                       | Eliminar puntos              | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
| **Usuarios**          |                              |                  |                    |                        |                        |                        |
|                       | Ver usuarios de mi empresa   | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Crear usuarios               | ✅ (1 max)       | ✅ (2 max)         | ✅ (5 max)             | ✅ (10 max)            | ✅ (∞)                 |
|                       | Editar/eliminar usuarios     | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Asignar roles                | ⚠️ (solo cajero) | ✅ (admin, cajero) | ✅ (todos menos super) | ✅ (todos menos super) | ✅ (todos menos super) |
| **Clientes**          |                              |                  |                    |                        |                        |                        |
|                       | CRUD clientes                | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Importar clientes (CSV)      | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
| **Productos**         |                              |                  |                    |                        |                        |                        |
|                       | CRUD productos               | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Importar productos (CSV)     | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Gestión de inventario        | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
| **Facturación**       |                              |                  |                    |                        |                        |                        |
|                       | Emitir facturas              | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Notas de crédito             | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Notas de débito              | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Retenciones                  | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Guías de remisión            | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Borradores                   | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Límite docs/año              | 50               | 500                | 1,500                  | 5,000                  | ∞                      |
| **Dashboard**         |                              |                  |                    |                        |                        |                        |
|                       | Ver Welcome Dashboard        | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | KPIs en tiempo real          | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Gráfico de tendencia         | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Alertas inteligentes         | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
| **Reportes**          |                              |                  |                    |                        |                        |                        |
|                       | Reportes básicos (PDF/Excel) | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ventas por período           | ✅               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ventas por cliente           | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Ventas por producto          | ❌               | ✅                 | ✅                     | ✅                     | ✅                     |
|                       | Dashboard de ventas          | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Comparación de sucursales    | ❌               | ❌                 | ✅                     | ✅                     | ✅                     |
|                       | Reportes personalizados      | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | Reportes programados (email) | ❌               | ❌                 | ❌                     | ❌                     | ✅                     |
| **Integraciones**     |                              |                  |                    |                        |                        |                        |
|                       | API REST (lectura)           | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | API REST (escritura)         | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | Webhooks                     | ❌               | ❌                 | ❌                     | ✅                     | ✅                     |
|                       | Integración ERP              | ❌               | ❌                 | ❌                     | ❌                     | ✅                     |

**🔑 Características clave:**

- ✅ Control total de su compañía
- ✅ Puede crear y gestionar usuarios (según plan)
- ✅ Puede crear sucursales y puntos (según plan)
- ⚠️ Sujeto a límites de suscripción
- ⚠️ No puede acceder a `/admin/*` (solo super_admin)

---

### **ROL 2: SUPERVISOR** (Gerente/Jefe de Sucursal)

**Alcance:** Su establecimiento  
**Restricciones:** Según plan + permisos de rol

| Módulo                | Feature                     | Permiso                           |
| --------------------- | --------------------------- | --------------------------------- |
| **Mi Sucursal**       |                             |                                   |
|                       | Ver datos de sucursal       | ✅                                |
|                       | Editar datos (limitado)     | ⚠️ (nombre, teléfono)             |
| **Puntos de Emisión** |                             |                                   |
|                       | Ver puntos de su sucursal   | ✅                                |
|                       | Crear/editar puntos         | ✅ (según plan)                   |
|                       | Eliminar puntos             | ❌ (solo admin)                   |
| **Usuarios**          |                             |                                   |
|                       | Ver usuarios de su sucursal | ✅                                |
|                       | Crear cajeros               | ✅ (según plan)                   |
|                       | Editar/eliminar cajeros     | ⚠️ (solo de su sucursal)          |
|                       | Asignar roles               | ⚠️ (solo cajero/vendedor)         |
| **Clientes**          |                             |                                   |
|                       | CRUD clientes               | ✅                                |
| **Productos**         |                             |                                   |
|                       | Ver productos               | ✅                                |
|                       | Crear/editar productos      | ✅                                |
|                       | Eliminar productos          | ❌ (solo admin)                   |
| **Facturación**       |                             |                                   |
|                       | Emitir facturas             | ✅                                |
|                       | Notas de crédito            | ✅                                |
|                       | Retenciones                 | ✅                                |
|                       | Borradores                  | ✅ (según plan)                   |
| **Reportes**          |                             |                                   |
|                       | Ver reportes de su sucursal | ✅                                |
|                       | Exportar datos              | ✅                                |
|                       | Dashboard de ventas         | ✅ (solo su sucursal, según plan) |

**🔑 Características clave:**

- ✅ Enfocado en operaciones de sucursal
- ✅ Puede gestionar su equipo de cajeros
- ⚠️ No puede ver datos de otras sucursales
- ⚠️ No puede modificar configuración global

---

### **ROL 3: CAJERO** (Usuario Operativo)

**Alcance:** Su punto de emisión  
**Restricciones:** Operaciones básicas

| Módulo               | Feature                   | Permiso                    |
| -------------------- | ------------------------- | -------------------------- |
| **Punto de Emisión** |                           |                            |
|                      | Ver su punto asignado     | ✅                         |
|                      | Cambiar punto             | ❌                         |
| **Clientes**         |                           |                            |
|                      | Ver clientes              | ✅                         |
|                      | Crear clientes            | ✅                         |
|                      | Editar clientes           | ⚠️ (solo datos básicos)    |
|                      | Eliminar clientes         | ❌                         |
| **Productos**        |                           |                            |
|                      | Ver productos             | ✅                         |
|                      | Buscar productos          | ✅                         |
|                      | Crear/editar/eliminar     | ❌                         |
| **Facturación**      |                           |                            |
|                      | Emitir facturas           | ✅                         |
|                      | Notas de crédito          | ✅                         |
|                      | Retenciones               | ✅                         |
|                      | Borradores                | ✅ (según plan)            |
|                      | Ver historial             | ✅ (solo sus docs)         |
| **Reportes**         |                           |                            |
|                      | Ver reporte de sus ventas | ✅                         |
|                      | Dashboard                 | ❌ (solo admin/supervisor) |
|                      | Reportes avanzados        | ❌                         |

**🔑 Características clave:**

- ✅ Operaciones de caja básicas
- ✅ Emisión de documentos
- ⚠️ No puede gestionar usuarios
- ⚠️ No puede ver ventas de otros cajeros
- ⚠️ No puede modificar productos

---

### **ROL 4: VENDEDOR** (Emisión Limitada)

**Alcance:** Su punto de emisión  
**Restricciones:** Solo facturas

| Módulo          | Feature                   | Permiso                |
| --------------- | ------------------------- | ---------------------- |
| **Facturación** |                           |                        |
|                 | Emitir facturas           | ✅                     |
|                 | Notas de crédito          | ❌                     |
|                 | Retenciones               | ❌                     |
|                 | Ver historial             | ✅ (solo sus facturas) |
| **Clientes**    |                           |                        |
|                 | Ver clientes              | ✅                     |
|                 | Crear clientes            | ✅ (básico)            |
| **Productos**   |                           |                        |
|                 | Ver productos             | ✅                     |
|                 | Buscar productos          | ✅                     |
| **Reportes**    |                           |                        |
|                 | Ver reporte de sus ventas | ✅                     |

**🔑 Características clave:**

- ✅ Solo emisión de facturas
- ❌ No puede hacer NC, retenciones, etc.
- ⚠️ Para vendedores externos/comisionistas

---

## 🗄️ **ESQUEMA DE BASE DE DATOS**

### **1. Tabla `role` (EXISTENTE - ya está creada ✅)**

```sql
-- Roles actuales en el sistema
-- No se requiere modificación, solo verificar que existan

SELECT * FROM public.role WHERE id_role IN (1, 2, 3);

-- Resultado esperado:
-- 1 | super_admin | Super Admin           | true
-- 2 | member      | Miembro               | true
-- 3 | admin       | Administrador         | true

-- Si deseas agregar el rol opcional SUPERVISOR (ID: 4):
INSERT INTO public.role (id_role, name, description, status) VALUES
(4, 'supervisor', 'Supervisor de Sucursal', true)
ON CONFLICT (id_role) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description;
```

---

### **2. Tabla `subscription_plan` (NUEVA)**

```sql
CREATE TABLE IF NOT EXISTS public.subscription_plan (
  id_plan SERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE, -- 'FREE', 'BASIC', 'PRO', 'BUSINESS', 'ENTERPRISE'
  name VARCHAR(100) NOT NULL,
  description TEXT,
  price_annual DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_monthly DECIMAL(10,2), -- Si ofrecemos mensual

  -- Límites de uso
  document_limit_yearly INT, -- NULL = ilimitado
  user_limit INT, -- NULL = ilimitado
  establishment_limit INT, -- NULL = ilimitado
  issuance_point_limit INT, -- NULL = ilimitado

  -- Features booleanas
  has_dashboard BOOLEAN DEFAULT false,
  has_advanced_reports BOOLEAN DEFAULT false,
  has_drafts BOOLEAN DEFAULT false,
  has_api_access BOOLEAN DEFAULT false,
  has_webhooks BOOLEAN DEFAULT false,
  has_erp_integration BOOLEAN DEFAULT false,
  has_inventory BOOLEAN DEFAULT false,
  has_scheduled_reports BOOLEAN DEFAULT false,
  has_csv_import BOOLEAN DEFAULT false,

  is_active BOOLEAN DEFAULT true,
  display_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Insertar planes (Opción A del research)
INSERT INTO public.subscription_plan
(code, name, description, price_annual, price_monthly,
 document_limit_yearly, user_limit, establishment_limit, issuance_point_limit,
 has_dashboard, has_advanced_reports, has_drafts, has_api_access, has_webhooks,
 has_erp_integration, has_inventory, has_scheduled_reports, has_csv_import, display_order)
VALUES
('FREE', 'Gratis', 'Plan gratuito para pruebas', 0, 0,
 50, 1, 1, 1,
 false, false, false, false, false, false, false, false, false, 0),

('BASIC', 'Básico', 'Para emprendedores y pequeños negocios', 39, 4,
 500, 2, 1, 1,
 true, false, true, false, false, false, false, false, false, 1),

('PRO', 'Profesional', 'Para PYMES en crecimiento', 79, 8,
 1500, 5, 2, 2,
 true, true, true, false, false, false, false, false, true, 2),

('BUSINESS', 'Empresarial', 'Para empresas medianas', 149, 15,
 5000, 10, 5, 5,
 true, true, true, true, true, false, true, false, true, 3),

('ENTERPRISE', 'Enterprise', 'Para corporativos y cadenas', 299, 30,
 NULL, NULL, NULL, NULL,
 true, true, true, true, true, true, true, true, true, 4);
```

---

### **3. Tabla `company_subscription` (NUEVA)**

```sql
CREATE TABLE IF NOT EXISTS public.company_subscription (
  id_subscription SERIAL PRIMARY KEY,
  id_company INT NOT NULL REFERENCES company(id_company) ON DELETE CASCADE,
  id_plan INT NOT NULL REFERENCES subscription_plan(id_plan),

  -- Fechas
  start_date DATE NOT NULL DEFAULT CURRENT_DATE,
  expires_at DATE NOT NULL,

  -- Estado
  status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED, SUSPENDED

  -- Uso actual (mes en curso)
  current_month_usage INT DEFAULT 0,
  current_year_usage INT DEFAULT 0,
  last_reset_date DATE DEFAULT CURRENT_DATE,

  -- Facturación
  payment_method VARCHAR(50), -- 'MANUAL', 'STRIPE', 'PAYPHONE', etc.
  last_payment_date DATE,
  last_payment_amount DECIMAL(10,2),

  -- Notas
  notes TEXT,

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),

  UNIQUE(id_company) -- Una compañía solo puede tener una suscripción activa
);

-- Índices
CREATE INDEX idx_company_subscription_company ON public.company_subscription(id_company);
CREATE INDEX idx_company_subscription_plan ON public.company_subscription(id_plan);
CREATE INDEX idx_company_subscription_status ON public.company_subscription(status);
CREATE INDEX idx_company_subscription_expires ON public.company_subscription(expires_at);
```

---

### **4. Tabla `subscription_usage_history` (NUEVA - auditoría)**

```sql
CREATE TABLE IF NOT EXISTS public.subscription_usage_history (
  id_history SERIAL PRIMARY KEY,
  id_company INT NOT NULL REFERENCES company(id_company) ON DELETE CASCADE,
  id_subscription INT NOT NULL REFERENCES company_subscription(id_subscription) ON DELETE CASCADE,

  month_year VARCHAR(7) NOT NULL, -- '2025-12'

  -- Contadores
  invoices_count INT DEFAULT 0,
  credit_notes_count INT DEFAULT 0,
  debit_notes_count INT DEFAULT 0,
  retentions_count INT DEFAULT 0,
  total_documents INT DEFAULT 0,

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),

  UNIQUE(id_company, month_year)
);

-- Índices
CREATE INDEX idx_subscription_usage_company ON public.subscription_usage_history(id_company);
CREATE INDEX idx_subscription_usage_month ON public.subscription_usage_history(month_year);
```

---

### **5. Tabla `feature_permission` (NUEVA - mapeo granular)**

```sql
CREATE TABLE IF NOT EXISTS public.feature_permission (
  id_permission SERIAL PRIMARY KEY,
  feature_code VARCHAR(100) NOT NULL UNIQUE,
  feature_name VARCHAR(255) NOT NULL,
  description TEXT,
  module VARCHAR(50), -- 'BILLING', 'REPORTS', 'ADMIN', etc.

  -- Roles que tienen acceso (array de IDs)
  allowed_roles INT[] DEFAULT '{}', -- {1, 2, 5} = admin, supervisor, super_admin

  -- Planes que incluyen esta feature (array de códigos)
  required_plans VARCHAR(50)[] DEFAULT '{}', -- {'BASIC', 'PRO', 'BUSINESS', 'ENTERPRISE'}

  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Insertar features principales
INSERT INTO public.feature_permission
(feature_code, feature_name, description, module, allowed_roles, required_plans)
VALUES
-- Dashboard
('dashboard.view', 'Ver Dashboard', 'Acceso al Welcome Dashboard', 'DASHBOARD',
 '{3}', '{''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- solo admin

('dashboard.kpis', 'Ver KPIs', 'Ver métricas en tiempo real', 'DASHBOARD',
 '{3}', '{''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- solo admin

-- Reportes
('reports.basic', 'Reportes Básicos', 'Ver reportes básicos PDF/Excel', 'REPORTS',
 '{2,3}', '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- member y admin

('reports.advanced', 'Reportes Avanzados', 'Dashboard de ventas, comparativas', 'REPORTS',
 '{3}', '{''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- solo admin

('reports.scheduled', 'Reportes Programados', 'Envío automático por email', 'REPORTS',
 '{3}', '{''ENTERPRISE''}'), -- solo admin

-- Facturación
('billing.invoice.create', 'Emitir Facturas', 'Crear y autorizar facturas', 'BILLING',
 '{2,3}', '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- member y admin

('billing.credit_note.create', 'Emitir NC', 'Crear notas de crédito', 'BILLING',
 '{2,3}', '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- member y admin

('billing.draft.manage', 'Gestión de Borradores', 'Guardar y gestionar borradores', 'BILLING',
 '{2,3}', '{''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- member y admin

-- Administración
('admin.company.manage', 'Gestión de Compañía', 'Editar datos de la empresa', 'ADMIN',
 '{3}', '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- solo admin

('admin.users.manage', 'Gestión de Usuarios', 'Crear/editar usuarios', 'ADMIN',
 '{3}', '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- solo admin

('admin.establishments.manage', 'Gestión de Sucursales', 'Crear/editar sucursales', 'ADMIN',
 '{3}', '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- solo admin

('admin.products.manage', 'Gestión de Productos', 'Crear/editar/eliminar productos', 'ADMIN',
 '{3}', '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'), -- solo admin

-- API
('api.read', 'API Lectura', 'Acceso a endpoints de lectura', 'API',
 '{3}', '{''BUSINESS'',''ENTERPRISE''}'), -- solo admin

('api.write', 'API Escritura', 'Acceso a endpoints de escritura', 'API',
 '{3}', '{''BUSINESS'',''ENTERPRISE''}'), -- solo admin

-- Integraciones
('integration.erp', 'Integración ERP', 'Conectar con sistemas contables', 'INTEGRATION',
 '{3}', '{''ENTERPRISE''}'); -- solo admin

-- Si decides implementar el rol SUPERVISOR (ID: 4), actualizar:
/*
UPDATE public.feature_permission SET allowed_roles = '{3,4}'
WHERE feature_code IN (
  'admin.users.manage',           -- supervisor puede crear members
  'admin.establishments.manage',  -- supervisor puede gestionar puntos de su sucursal
  'reports.advanced'              -- supervisor puede ver reportes de su sucursal
);
*/
```

---

## 🔧 **IMPLEMENTACIÓN EN CÓDIGO**

### **1. Servicio de Permisos (nuevo)**

```typescript
// src/app/_service/permission/permission.service.ts

import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, of } from "rxjs";
import { map, catchError } from "rxjs/operators";
import { environment } from "../../../environments/environment";
import { decodeToken } from "../../_functions/functions";

export interface SubscriptionPlan {
  code: string; // 'FREE', 'BASIC', 'PRO', etc.
  name: string;
  documentLimitYearly: number | null;
  userLimit: number | null;
  hasDashboard: boolean;
  hasAdvancedReports: boolean;
  hasDrafts: boolean;
  hasApiAccess: boolean;
  // ... más features
}

export interface CompanySubscription {
  idSubscription: number;
  plan: SubscriptionPlan;
  status: "ACTIVE" | "EXPIRED" | "CANCELLED" | "SUSPENDED";
  startDate: string;
  expiresAt: string;
  currentYearUsage: number;
}

export interface PermissionCheckRequest {
  featureCode: string;
  idCompany: number;
}

export interface PermissionCheckResponse {
  allowed: boolean;
  reason?: "NO_ROLE" | "NO_SUBSCRIPTION" | "LIMIT_EXCEEDED" | "EXPIRED";
  details?: string;
}

@Injectable({
  providedIn: "root",
})
export class PermissionService {
  private readonly baseUrl = `${environment.HOST_AUTH}/api/permission`;
  private readonly subscriptionUrl = `${environment.HOST_BILLING}/api/subscription`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Obtiene la suscripción actual de la compañía
   */
  getCurrentSubscription(idCompany: number): Observable<CompanySubscription> {
    return this.http.get<CompanySubscription>(`${this.subscriptionUrl}/${idCompany}/current`);
  }

  /**
   * Verifica si el usuario tiene permiso para una feature
   * Valida AMBAS capas: rol + suscripción
   */
  canAccess(featureCode: string): Observable<boolean> {
    const token = decodeToken();
    if (!token) {
      return of(false);
    }

    const request: PermissionCheckRequest = {
      featureCode,
      idCompany: token.idCompany,
    };

    return this.http.post<PermissionCheckResponse>(`${this.baseUrl}/check`, request).pipe(
      map((response) => response.allowed),
      catchError(() => of(false))
    );
  }

  /**
   * Verifica permiso y devuelve detalles (para mostrar mensajes)
   */
  canAccessWithDetails(featureCode: string): Observable<PermissionCheckResponse> {
    const token = decodeToken();
    if (!token) {
      return of({
        allowed: false,
        reason: "NO_ROLE",
        details: "Usuario no autenticado",
      });
    }

    const request: PermissionCheckRequest = {
      featureCode,
      idCompany: token.idCompany,
    };

    return this.http.post<PermissionCheckResponse>(`${this.baseUrl}/check`, request).pipe(
      catchError(() =>
        of({
          allowed: false,
          reason: "NO_ROLE",
          details: "Error al verificar permisos",
        })
      )
    );
  }

  /**
   * Verifica si el usuario es super_admin (bypass de suscripción)
   */
  isSuperAdmin(): boolean {
    const token = decodeToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes("super_admin");
  }

  /**
   * Verifica si el usuario tiene un rol específico
   */
  hasRole(roleName: string): boolean {
    const token = decodeToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes(roleName);
  }

  /**
   * Obtiene el plan actual desde localStorage (caché)
   */
  getCachedPlanCode(): string {
    return localStorage.getItem("SUBSCRIPTION_PLAN_CODE") || "FREE";
  }

  /**
   * Verifica si el plan actual incluye una feature (caché local)
   */
  planHasFeature(feature: keyof SubscriptionPlan): boolean {
    const planCode = this.getCachedPlanCode();

    // Definición local de features por plan (sincronizar con backend)
    const PLAN_FEATURES: Record<string, Partial<SubscriptionPlan>> = {
      FREE: {
        hasDashboard: false,
        hasAdvancedReports: false,
        hasDrafts: false,
        hasApiAccess: false,
      },
      BASIC: {
        hasDashboard: true,
        hasAdvancedReports: false,
        hasDrafts: true,
        hasApiAccess: false,
      },
      PRO: {
        hasDashboard: true,
        hasAdvancedReports: true,
        hasDrafts: true,
        hasApiAccess: false,
      },
      BUSINESS: {
        hasDashboard: true,
        hasAdvancedReports: true,
        hasDrafts: true,
        hasApiAccess: true,
      },
      ENTERPRISE: {
        hasDashboard: true,
        hasAdvancedReports: true,
        hasDrafts: true,
        hasApiAccess: true,
      },
    };

    return PLAN_FEATURES[planCode]?.[feature] ?? false;
  }
}
```

---

### **2. Directiva de Permisos (para HTML)**

```typescript
// src/app/_directives/has-permission.directive.ts

import { Directive, Input, TemplateRef, ViewContainerRef, OnInit, OnDestroy } from "@angular/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { PermissionService } from "../_service/permission/permission.service";

/**
 * Directiva para mostrar/ocultar elementos según permisos
 *
 * Uso:
 * <div *hasPermission="'dashboard.view'">
 *   <!-- Solo visible si tiene permiso -->
 * </div>
 *
 * <button *hasPermission="'billing.invoice.create'; else upgradeTpl">
 *   Crear Factura
 * </button>
 * <ng-template #upgradeTpl>
 *   <button (click)="showUpgradeDialog()">
 *     🔒 Actualizar Plan
 *   </button>
 * </ng-template>
 */
@Directive({
  selector: "[hasPermission]",
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  @Input() hasPermission!: string; // Feature code
  @Input() hasPermissionElse?: TemplateRef<any>; // Fallback template

  private destroy$ = new Subject<void>();

  constructor(private readonly templateRef: TemplateRef<any>, private readonly viewContainer: ViewContainerRef, private readonly permissionService: PermissionService) {}

  ngOnInit(): void {
    // Super admin siempre tiene acceso
    if (this.permissionService.isSuperAdmin()) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      return;
    }

    // Verificar permiso
    this.permissionService
      .canAccess(this.hasPermission)
      .pipe(takeUntil(this.destroy$))
      .subscribe((allowed) => {
        this.viewContainer.clear();

        if (allowed) {
          this.viewContainer.createEmbeddedView(this.templateRef);
        } else if (this.hasPermissionElse) {
          this.viewContainer.createEmbeddedView(this.hasPermissionElse);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

---

### **3. Guard de Permisos (para rutas)**

```typescript
// src/app/_guard/permission.guard.ts

import { Injectable } from "@angular/core";
import { CanActivate, ActivatedRouteSnapshot, Router, UrlTree } from "@angular/router";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { PermissionService } from "../_service/permission/permission.service";
import { MatSnackBar } from "@angular/material/snack-bar";

/**
 * Guard para proteger rutas según permisos (rol + suscripción)
 *
 * Uso en routing:
 * {
 *   path: 'reports/advanced',
 *   canActivate: [PermissionGuard],
 *   data: { permission: 'reports.advanced' },
 *   component: AdvancedReportsComponent
 * }
 */
@Injectable({
  providedIn: "root",
})
export class PermissionGuard implements CanActivate {
  constructor(private readonly permissionService: PermissionService, private readonly router: Router, private readonly snackBar: MatSnackBar) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> | boolean {
    const requiredPermission = route.data["permission"] as string;

    if (!requiredPermission) {
      console.warn("[PermissionGuard] No se especificó permiso requerido");
      return true;
    }

    // Super admin siempre puede acceder
    if (this.permissionService.isSuperAdmin()) {
      return true;
    }

    // Verificar permiso
    return this.permissionService.canAccessWithDetails(requiredPermission).pipe(
      map((response) => {
        if (response.allowed) {
          return true;
        }

        // Mostrar mensaje según el motivo
        switch (response.reason) {
          case "NO_SUBSCRIPTION":
            this.snackBar.open("🔒 Esta funcionalidad requiere un plan superior. Actualiza tu suscripción.", "Ver Planes", { duration: 5000 });
            return this.router.createUrlTree(["/subscription/upgrade"]);

          case "LIMIT_EXCEEDED":
            this.snackBar.open("⚠️ Has alcanzado el límite de tu plan. Actualiza tu suscripción.", "Ver Planes", { duration: 5000 });
            return this.router.createUrlTree(["/subscription/upgrade"]);

          case "EXPIRED":
            this.snackBar.open("❌ Tu suscripción ha expirado. Renueva tu plan.", "Renovar", { duration: 5000 });
            return this.router.createUrlTree(["/subscription/renew"]);

          case "NO_ROLE":
          default:
            this.snackBar.open("⛔ No tienes permisos para acceder a esta sección.", "Cerrar", { duration: 5000 });
            return this.router.createUrlTree(["/welcome"]);
        }
      })
    );
  }
}
```

---

## 📦 **ENDPOINTS BACKEND REQUERIDOS**

### **1. Verificación de Permisos**

```
POST /api/permission/check
Content-Type: application/json
Authorization: Bearer {JWT}

Request:
{
  "featureCode": "dashboard.view",
  "idCompany": 123
}

Response:
{
  "allowed": true | false,
  "reason": "NO_ROLE" | "NO_SUBSCRIPTION" | "LIMIT_EXCEEDED" | "EXPIRED" | null,
  "details": "string (opcional)"
}
```

---

### **2. Suscripción Actual**

```
GET /api/subscription/{idCompany}/current
Authorization: Bearer {JWT}

Response:
{
  "idSubscription": 1,
  "plan": {
    "code": "PRO",
    "name": "Profesional",
    "documentLimitYearly": 1500,
    "userLimit": 5,
    "hasDashboard": true,
    "hasAdvancedReports": true,
    // ... más features
  },
  "status": "ACTIVE",
  "startDate": "2025-01-01",
  "expiresAt": "2025-12-31",
  "currentYearUsage": 350
}
```

---

### **3. Uso Mensual/Anual**

```
GET /api/subscription/{idCompany}/usage
Authorization: Bearer {JWT}

Response:
{
  "currentMonth": {
    "month": "2025-12",
    "invoices": 25,
    "creditNotes": 3,
    "total": 28
  },
  "currentYear": {
    "year": 2025,
    "total": 350,
    "limit": 1500,
    "percentUsed": 23.3
  }
}
```

---

## 🚀 **PLAN DE IMPLEMENTACIÓN**

### **Semana 1: Base de Datos + Backend**

**Día 1-2:**

- [ ] Crear tabla `subscription_plan`
- [ ] Crear tabla `company_subscription`
- [ ] Crear tabla `subscription_usage_history`
- [ ] Crear tabla `feature_permission`
- [ ] Migrar datos existentes (asignar plan FREE a todas las companies)

**Día 3-4:**

- [ ] Endpoint `POST /api/permission/check`
- [ ] Endpoint `GET /api/subscription/{id}/current`
- [ ] Endpoint `GET /api/subscription/{id}/usage`
- [ ] Trigger para incrementar `current_year_usage` al crear invoice/NC

**Día 5:**

- [ ] Testing de endpoints
- [ ] Documentación de API

---

### **Semana 2: Frontend**

**Día 1-2:**

- [ ] `PermissionService` (service)
- [ ] `HasPermissionDirective` (directive)
- [ ] `PermissionGuard` (guard)
- [ ] Testing de servicios

**Día 3-4:**

- [ ] Panel "Mi Suscripción" (nuevo componente)
  - Ver plan actual
  - Ver uso mensual/anual
  - Progress bar
  - Botón "Actualizar Plan"
- [ ] Integrar directiva en componentes existentes

**Día 5:**

- [ ] Aplicar guards a rutas según permisos
- [ ] Testing E2E

---

### **Semana 3: Alertas y Pulido**

**Día 1-2:**

- [ ] Alerta "Límite de docs alcanzado (80%)"
- [ ] Alerta "Límite de docs alcanzado (100%)"
- [ ] Alerta "Suscripción próxima a vencer (7 días)"

**Día 3-4:**

- [ ] UI de upgrade de plan
- [ ] Flow de pago (manual inicialmente)
- [ ] Confirmación de upgrade

**Día 5:**

- [ ] Testing completo
- [ ] Documentación de usuario

---

## ✅ **RESUMEN EJECUTIVO Y DECISIONES FINALES**

### **🎯 Arquitectura Confirmada:**

```
SISTEMA DE 3 ROLES BASE + 1 OPCIONAL:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. super_admin (ID: 1) → Owner WallpariSoft
2. member      (ID: 2) → Cajero/Vendedor/Operativo
3. admin       (ID: 3) → Administrador de Compañía
4. supervisor  (ID: 4) → [OPCIONAL] Gerente de Sucursal
```

---

### **💰 Planes de Suscripción (Pricing Opción A):**

| Plan            | Precio/año | Docs  | Usuarios | Dashboard | Reportes Avanz. | API |
| --------------- | ---------- | ----- | -------- | --------- | --------------- | --- |
| **GRATIS**      | $0         | 50    | 1        | ❌        | ❌              | ❌  |
| **BÁSICO**      | $39        | 500   | 2        | ✅        | ❌              | ❌  |
| **PRO**         | $79        | 1,500 | 5        | ✅        | ✅              | ❌  |
| **EMPRESARIAL** | $149       | 5,000 | 10       | ✅        | ✅              | ✅  |
| **ENTERPRISE**  | $299       | ∞     | ∞        | ✅        | ✅              | ✅  |

---

### **🔐 Validación de Permisos:**

```
┌─────────────────────────────────┐
│ 1. ¿Es super_admin?             │
│    ✅ SÍ → ACCESO TOTAL (bypass)│
│    ❌ NO → Continuar validación │
└─────────────────────────────────┘
              ↓
┌─────────────────────────────────┐
│ 2. ¿Su ROL tiene permiso?       │
│    ✅ SÍ → Continuar            │
│    ❌ NO → DENEGAR (403)        │
└─────────────────────────────────┘
              ↓
┌─────────────────────────────────┐
│ 3. ¿Su PLAN incluye feature?    │
│    ✅ SÍ → PERMITIR             │
│    ❌ NO → UPGRADE REQUIRED     │
└─────────────────────────────────┘
```

---

### **📊 Uso Típico por Rol:**

#### **SUPER_ADMIN (Owner WallpariSoft):**

- Ver todas las compañías
- Gestionar suscripciones
- Soporte técnico avanzado
- Análisis global

#### **ADMIN (Dueño de Negocio):**

- Gestionar su empresa
- Ver dashboards y reportes (según plan)
- Crear usuarios (members)
- Configurar sucursales y puntos
- Upgrade de plan

#### **MEMBER (Cajero/Vendedor):**

- Emitir facturas, NC, ND
- Buscar clientes y productos
- Ver sus propias ventas
- Reportes básicos (solo sus datos)

#### **SUPERVISOR (Opcional - Gerente):**

- Gestionar su sucursal
- Supervisar members de su sucursal
- Ver reportes de su sucursal
- Crear members para su sucursal

---

### **🚀 Implementación Sugerida:**

**FASE 1 (Semana 1-2): Base de Datos**

- ✅ Crear 4 tablas: `subscription_plan`, `company_subscription`, `subscription_usage_history`, `feature_permission`
- ✅ Insertar 5 planes (FREE → ENTERPRISE)
- ✅ Migrar companies existentes a plan FREE
- ✅ Insertar permisos base (15-20 features)

**FASE 2 (Semana 3-4): Backend**

- ✅ Endpoint `POST /api/permission/check`
- ✅ Endpoint `GET /api/subscription/{id}/current`
- ✅ Endpoint `GET /api/subscription/{id}/usage`
- ✅ Trigger para incrementar uso al emitir docs

**FASE 3 (Semana 5-6): Frontend**

- ✅ `PermissionService` (validación doble capa)
- ✅ `HasPermissionDirective` (para templates)
- ✅ `PermissionGuard` (para rutas)
- ✅ Panel "Mi Suscripción"

**FASE 4 (Semana 7): Alertas**

- ✅ Alerta "Límite de docs alcanzado (80%)"
- ✅ Alerta "Suscripción próxima a vencer"
- ✅ Alerta "Plan insuficiente para feature"

---

### **🎯 Decisión Pendiente:**

**¿Implementamos el rol SUPERVISOR (ID: 4)?**

**Opción A: NO (más simple)**

- ✅ Solo 3 roles: super_admin, member, admin
- ✅ Menos complejidad
- ✅ Suficiente para 90% de clientes
- ⚠️ Empresas grandes gestionan sucursales con admin único

**Opción B: SÍ (más escalable)**

- ✅ 4 roles: super_admin, member, admin, supervisor
- ✅ Mejor delegación en empresas medianas/grandes
- ✅ Cada sucursal tiene su gerente
- ⚠️ Más complejidad en permisos

**RECOMENDACIÓN:** Empezar con **Opción A** (3 roles). Si un cliente requiere supervisor, agregarlo después es fácil (solo insert en `role` + update en `feature_permission`).

---

## ✅ **SIGUIENTE PASO:**

**¿Qué prefieres?**

1. **"implementa fase 1"** → Comienzo con SQL scripts (tablas + inserts)
2. **"agrega supervisor"** → Confirmamos que queremos el 4to rol desde el inicio
3. **"ajusta algo"** → Dime qué modificar en la arquitectura
4. **"fase 3a primero"** → Completamos Welcome Dashboard (alertas) antes de suscripciones

**Dime cómo proceder.** 🚀
