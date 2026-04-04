# 🔐 Sistema de Permisos Granulares - Documentación Completa

## 📋 Resumen Ejecutivo

El sistema de permisos granular permite gestionar el acceso a funcionalidades del sistema mediante **dos capas independientes**:

1. **Permisos de Roles**: Define qué roles pueden acceder a cada funcionalidad
2. **Permisos de Planes**: Define qué planes de suscripción incluyen cada funcionalidad

**Un usuario tiene acceso solo si AMBAS condiciones se cumplen:**

- ✅ Su rol tiene permiso para la funcionalidad
- ✅ Su plan incluye la funcionalidad

---

## 🎯 Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────┐
│              SISTEMA DE PERMISOS GRANULAR               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────┐    ┌─────────────────────────┐
│  PERMISOS DE ROLES      │    │  PERMISOS DE PLANES     │
│  (¿Qué roles pueden     │    │  (¿Qué planes incluyen  │
│   acceder a qué?)       │    │   qué funcionalidades?) │
├─────────────────────────┤    ├─────────────────────────┤
│ • Super Admin: ✓       │    │ • Gratuito: ✓           │
│ • Admin: ✓             │    │ • Básico: ✓             │
│ • Member: ☐            │    │ • Profesional: ☐        │
│                         │    │ • Empresarial: ☐        │
└─────────────────────────┘    └─────────────────────────┘
         │                                │
         └────────────┬───────────────────┘
                      │
         ┌────────────▼────────────┐
         │   VALIDACIÓN FINAL      │
         │  (Backend valida ambos)  │
         │                          │
         │  Usuario tiene acceso si:│
         │  1. Su ROL tiene permiso │
         │  2. Su PLAN incluye la   │
         │     funcionalidad        │
         └──────────────────────────┘
```

---

## 📍 Ubicación en el Sistema

### Rutas

- **Gestión de Permisos**: `/admin/role-permissions`
  - **Tab "Permisos de Roles"**: Gestiona qué roles pueden acceder a cada funcionalidad
  - **Tab "Permisos de Planes"**: Gestiona qué planes incluyen cada funcionalidad

### Acceso

- **Rol requerido**: `super_admin`
- **Feature Code**: `ROLE_PERMISSION_MANAGEMENT`

---

## 🔄 Flujo de Funcionamiento

### 1. Obtención de Funcionalidades

Las funcionalidades se obtienen **directamente del menú del backend**:

```
Backend envía menú completo
    ↓
Frontend extrae funcionalidades:
  - Solo menús con URL válida
  - Solo menús con featureCode
    ↓
Agrupa por módulo:
  - Facturación
  - Notas de Crédito
  - Clientes
  - etc.
    ↓
Ordena alfabéticamente:
  - Dentro de cada módulo
  - Los módulos entre sí
```

### 2. Gestión de Permisos de Roles

**Ubicación**: Tab "Permisos de Roles" en `/admin/role-permissions`

**Funcionalidad**:

- Muestra todas las funcionalidades del menú agrupadas por módulo
- Permite marcar/desmarcar qué roles pueden acceder a cada funcionalidad
- Guarda cambios en lote

**Ejemplo**:

```
FUNCIONALIDAD          | SUPER ADMIN | ADMIN | MEMBER
─────────────────────────────────────────────────────
Crear Facturas         |     ✓       |   ✓   |   ✓
Gestión de Usuarios    |     ✓       |   ✓   |   ☐
Gestión de Suscripciones |   ✓       |   ☐   |   ☐
```

### 3. Gestión de Permisos de Planes

**Ubicación**: Tab "Permisos de Planes" en `/admin/role-permissions`

**Funcionalidad**:

- Muestra todas las funcionalidades del menú agrupadas por módulo
- Permite marcar/desmarcar qué planes incluyen cada funcionalidad
- Guarda cambios en lote

**Ejemplo**:

```
FUNCIONALIDAD          | GRATUITO | BÁSICO | PROFESIONAL | EMPRESARIAL
───────────────────────────────────────────────────────────────────────
Crear Facturas         |    ✓     |   ✓    |      ✓      |      ✓
Reportes Avanzados     |    ☐     |   ☐    |      ☐      |      ✓
Gestión de Usuarios    |    ☐     |   ✓    |      ✓      |      ✓
```

### 4. Validación en Tiempo Real

Cuando un usuario intenta acceder a una funcionalidad:

```
Usuario intenta acceder a "Crear Facturas"
    ↓
Backend valida:
  1. ¿El rol del usuario tiene permiso? → Verifica tabla de permisos de roles
  2. ¿El plan del usuario incluye la funcionalidad? → Verifica tabla de permisos de planes
    ↓
Si AMBAS condiciones se cumplen → ✅ Acceso permitido
Si alguna falla → ❌ Acceso denegado
```

---

## 🛠️ Componentes Técnicos

### Frontend

#### Componentes

1. **`RolePermissionsContainerComponent`**

   - Contenedor principal con tabs
   - Alterna entre "Permisos de Roles" y "Permisos de Planes"

2. **`PermissionMatrixComponent`**

   - Matriz de permisos de roles
   - Muestra funcionalidades × roles
   - Permite toggle de permisos

3. **`PlanPermissionMatrixComponent`**
   - Matriz de permisos de planes
   - Muestra funcionalidades × planes
   - Permite toggle de permisos

#### Servicios

1. **`PermissionAdminService`**

   - `getAllFeatures()`: Obtiene funcionalidades del menú y enriquece con permisos de roles
   - `getAllFeaturesWithPlanPermissions()`: Obtiene funcionalidades del menú y enriquece con permisos de planes
   - `getAllRoles()`: Obtiene todos los roles disponibles
   - `getAllPlans()`: Obtiene todos los planes disponibles
   - `bulkUpdatePermissions()`: Actualiza permisos de roles en lote
   - `bulkUpdatePlanPermissions()`: Actualiza permisos de planes en lote

2. **`PermissionService`**
   - `canAccess()`: Valida si un usuario puede acceder a una funcionalidad (considera rol + plan)
   - `canAccessWithDetails()`: Valida con detalles del motivo de denegación

### Backend (Endpoints Esperados)

#### Permisos de Roles

```
GET  /api/permission/permissions/features
GET  /api/permission/permissions/role/{roleId}
PUT  /api/permission/permissions/feature/{featureCode}
PUT  /api/permission/permissions/bulk (opcional)
```

#### Permisos de Planes

```
GET  /api/permission/permissions/features/plans
GET  /api/permission/permissions/feature/{featureCode}/plans
PUT  /api/permission/permissions/feature/{featureCode}/plans
PUT  /api/permission/permissions/plans/bulk (opcional)
```

---

## 📝 Ejemplo de Uso

### Escenario: Agregar "Crear Facturas" al Plan Básico

1. **Ir a**: `/admin/role-permissions`
2. **Cambiar a tab**: "Permisos de Planes"
3. **Buscar**: "Crear Facturas" en el módulo "Facturación"
4. **Marcar checkbox**: En la columna "Básico"
5. **Guardar cambios**: Click en "Guardar Cambios"

**Resultado**: Todos los usuarios con plan "Básico" ahora pueden acceder a "Crear Facturas" (si su rol también tiene permiso).

---

## ⚠️ Consideraciones Importantes

### 1. Funcionalidades del Menú

- **Solo aparecen funcionalidades que están en el menú del backend**
- Cada menú/submenú debe tener:
  - `url` válida (no `null`)
  - `featureCode` definido

### 2. Validación Dual

- **Un usuario necesita AMBOS permisos**:
  - Su rol debe tener permiso
  - Su plan debe incluir la funcionalidad
- Si falta alguno, el acceso es denegado

### 3. Roles con Bypass

- `super_admin` tiene bypass completo
- No necesita validar permisos de plan
- Siempre tiene acceso a todo

### 4. Cambios en Tiempo Real

- Los cambios se aplican inmediatamente después de guardar
- Los usuarios afectados deberán recargar la sesión para ver los cambios

---

## 🔍 Troubleshooting

### Problema: No aparecen funcionalidades en la matriz

**Solución**:

1. Verificar que el backend esté enviando el menú completo
2. Verificar que cada menú tenga `featureCode` definido
3. Verificar que cada menú tenga `url` válida (no `null`)

### Problema: Los cambios no se guardan

**Solución**:

1. Verificar que el backend tenga los endpoints implementados
2. Verificar permisos de `super_admin`
3. Revisar la consola del navegador para errores

### Problema: Usuario no puede acceder a pesar de tener permisos

**Solución**:

1. Verificar que el rol tenga permiso (tab "Permisos de Roles")
2. Verificar que el plan incluya la funcionalidad (tab "Permisos de Planes")
3. Verificar que el usuario tenga el plan correcto asignado

---

## 📚 Referencias

- **Modelos**: `src/app/_model/permission/`
- **Servicios**: `src/app/_service/permission/`
- **Componentes**: `src/app/pages/admin/role-permissions/`
- **Guards**: `src/app/_guard/permission.guard.ts`

---

## ✅ Checklist de Implementación

### Frontend

- [x] Componente de contenedor con tabs
- [x] Componente de matriz de permisos de roles
- [x] Componente de matriz de permisos de planes
- [x] Servicio para obtener funcionalidades del menú
- [x] Servicio para gestionar permisos de roles
- [x] Servicio para gestionar permisos de planes
- [x] Agrupación por módulo
- [x] Ordenamiento alfabético
- [x] Validación dual (rol + plan)

### Backend (Pendiente)

- [ ] Endpoints para obtener permisos de roles
- [ ] Endpoints para actualizar permisos de roles
- [ ] Endpoints para obtener permisos de planes
- [ ] Endpoints para actualizar permisos de planes
- [ ] Validación dual en tiempo real
- [ ] Tabla de permisos de roles
- [ ] Tabla de permisos de planes

---

**Última actualización**: Diciembre 2024
