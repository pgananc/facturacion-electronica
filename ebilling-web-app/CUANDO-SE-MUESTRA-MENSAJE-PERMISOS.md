# 🚫 Cuándo se Muestra el Mensaje "No tienes permisos para acceder a esta sección"

## 📋 Ubicación del Código

**Archivo:** `src/app/_guard/permission.guard.ts`  
**Línea:** 148

## 🔍 Casos en que se Muestra el Mensaje

El mensaje **"🚫 No tienes permisos para acceder a esta sección."** se muestra cuando:

### 1. **Usuario intenta acceder a una ruta protegida sin permiso**

**Condiciones:**
- ✅ El usuario está autenticado (tiene token válido)
- ✅ La ruta está protegida con `PermissionGuard`
- ✅ La ruta tiene definido `data: { permission: 'FEATURE_CODE' }`
- ❌ El usuario **NO tiene permiso** para esa feature

**Flujo:**
```
1. Usuario intenta navegar a una ruta protegida
   ↓
2. PermissionGuard.canActivate() se ejecuta
   ↓
3. Verifica si es super_admin → NO
   ↓
4. Verifica si hay suscripción activa → SÍ
   ↓
5. Llama a PermissionService.canAccessWithDetails(featureCode)
   ↓
6. Backend valida: ROL + PLAN
   ↓
7. Backend responde: { allowed: false, reason: 'NO_ROLE' }
   ↓
8. PermissionGuard muestra el toast de error
   ↓
9. Redirige a /welcome
```

### 2. **Motivos Específicos que Activan este Mensaje**

El mensaje se muestra cuando el backend retorna uno de estos `reason`:

#### **A. `NO_ROLE`**
- El rol del usuario **NO tiene permiso** para esta feature
- Ejemplo: Usuario con rol `member` intenta acceder a `/users` (requiere rol `admin`)

#### **B. `INSUFFICIENT_PERMISSIONS`**
- El usuario tiene permisos insuficientes (similar a `NO_ROLE`)

#### **C. `default` (cualquier otro motivo no especificado)**
- Cualquier otro motivo de denegación que no tenga un mensaje específico

### 3. **Motivos que NO Activan este Mensaje**

Estos motivos muestran mensajes diferentes:

| Motivo | Mensaje Mostrado |
|--------|------------------|
| `PLAN_DOES_NOT_INCLUDE_FEATURE` | 🔒 Esta funcionalidad requiere un plan superior. Actualiza tu suscripción. |
| `NO_SUBSCRIPTION` | 🔒 Esta funcionalidad requiere un plan superior. Actualiza tu suscripción. |
| `NO_ACTIVE_SUBSCRIPTION` | ❌ Tu suscripción ha expirado. Renueva tu plan. |
| `EXPIRED` | ❌ Tu suscripción ha expirado. Renueva tu plan. |
| `LIMIT_EXCEEDED` | ⚠️ Has alcanzado el límite de tu plan. Actualiza tu suscripción. |

## 📝 Ejemplos Prácticos

### Ejemplo 1: Usuario "member" intenta acceder a `/users`

```typescript
// Ruta protegida en app-routing.module.ts
{
  path: 'users',
  canActivate: [SessionValidationGuard, PermissionGuard],
  data: { permission: 'USER_MANAGEMENT' },
  loadChildren: () => import('./pages/user/user.module')
}

// Flujo:
1. Usuario con rol "member" hace clic en "Usuarios" en el menú
2. Angular intenta navegar a /users
3. PermissionGuard verifica permiso 'USER_MANAGEMENT'
4. Backend valida:
   - Rol: member
   - Feature: USER_MANAGEMENT
   - ¿member tiene permiso? → NO
5. Backend responde: { allowed: false, reason: 'NO_ROLE' }
6. PermissionGuard muestra: "🚫 No tienes permisos para acceder a esta sección."
7. Redirige a /welcome
```

### Ejemplo 2: Usuario "member" intenta acceder a `/companies`

```typescript
// Ruta protegida
{
  path: 'companies',
  canActivate: [SessionValidationGuard, PermissionGuard],
  data: { permission: 'COMPANY_SETTINGS' },
  loadChildren: () => import('./pages/company/company.module')
}

// Flujo:
1. Usuario con rol "member" intenta acceder a /companies
2. PermissionGuard verifica permiso 'COMPANY_SETTINGS'
3. Backend valida:
   - Rol: member
   - Feature: COMPANY_SETTINGS
   - ¿member tiene permiso? → NO (solo admin)
4. Backend responde: { allowed: false, reason: 'NO_ROLE' }
5. PermissionGuard muestra: "🚫 No tienes permisos para acceder a esta sección."
6. Redirige a /welcome
```

### Ejemplo 3: Usuario "member" con plan FREE intenta acceder a reportes avanzados

```typescript
// Ruta protegida
{
  path: 'reports/sales-dashboard',
  canActivate: [SessionValidationGuard, PermissionGuard],
  data: { permission: 'REPORT_SALES_DASHBOARD' },
  component: SalesDashboardComponent
}

// Flujo:
1. Usuario con rol "member" y plan FREE intenta acceder
2. PermissionGuard verifica permiso 'REPORT_SALES_DASHBOARD'
3. Backend valida:
   - Rol: member
   - Plan: FREE
   - Feature: REPORT_SALES_DASHBOARD
   - ¿member tiene permiso? → Depende del backend
   - ¿FREE incluye feature? → NO (requiere PROFESSIONAL+)
4. Backend responde: { allowed: false, reason: 'PLAN_DOES_NOT_INCLUDE_FEATURE' }
5. PermissionGuard muestra: "🔒 Esta funcionalidad requiere un plan superior..."
6. NO muestra el mensaje "No tienes permisos..."
```

## 🔧 Código del PermissionGuard

```typescript
// Verificar permiso con detalles
return this.permissionService.canAccessWithDetails(requiredPermission).pipe(
  map((response) => {
    if (response.allowed) {
      return true; // ✅ Permitir acceso
    }

    // ❌ Denegar acceso
    const errorKey = `${requiredPermission}-${response.reason}`;

    // Solo mostrar el toast si no lo hemos mostrado recientemente (evitar spam)
    if (!this.shownErrors.has(errorKey)) {
      this.shownErrors.add(errorKey);

      // Mostrar mensaje según el motivo
      switch (response.reason) {
        case 'PLAN_DOES_NOT_INCLUDE_FEATURE':
        case 'NO_SUBSCRIPTION':
          this.toastService.warning(
            '🔒 Esta funcionalidad requiere un plan superior...'
          );
          break;

        case 'NO_ACTIVE_SUBSCRIPTION':
        case 'EXPIRED':
          this.toastService.error(
            '❌ Tu suscripción ha expirado...'
          );
          break;

        case 'LIMIT_EXCEEDED':
          this.toastService.warning(
            '⚠️ Has alcanzado el límite de tu plan...'
          );
          break;

        case 'NO_ROLE':
        case 'INSUFFICIENT_PERMISSIONS':
        default:
          // 🚫 ESTE ES EL MENSAJE QUE SE MUESTRA
          this.toastService.error(
            '🚫 No tienes permisos para acceder a esta sección.'
          );
          break;
      }
    }

    // Redirigir a welcome
    return this.router.createUrlTree(['/welcome']);
  })
);
```

## 🎯 Resumen

El mensaje **"🚫 No tienes permisos para acceder a esta sección."** se muestra cuando:

1. ✅ Usuario está autenticado
2. ✅ Intenta acceder a una ruta protegida con `PermissionGuard`
3. ✅ El backend deniega el acceso con `reason: 'NO_ROLE'` o `'INSUFFICIENT_PERMISSIONS'`
4. ❌ El rol del usuario NO tiene permiso para esa feature

**NO se muestra cuando:**
- El motivo es relacionado con el plan (`PLAN_DOES_NOT_INCLUDE_FEATURE`, `NO_SUBSCRIPTION`)
- El motivo es límite excedido (`LIMIT_EXCEEDED`)
- El motivo es suscripción expirada (`EXPIRED`, `NO_ACTIVE_SUBSCRIPTION`)
- El usuario es `super_admin` (siempre tiene acceso)

## 🔍 Cómo Verificar

Para ver cuándo se muestra este mensaje, revisa los logs en la consola:

```
[PermissionGuard] Verificando permiso: USER_MANAGEMENT
[PermissionGuard] ❌ Permiso denegado: {
  permission: 'USER_MANAGEMENT',
  reason: 'NO_ROLE',
  message: 'Tu rol no tiene permisos...'
}
```

Si ves `reason: 'NO_ROLE'` o `'INSUFFICIENT_PERMISSIONS'`, entonces se mostrará el mensaje "🚫 No tienes permisos para acceder a esta sección."




