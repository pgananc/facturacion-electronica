# 🔧 Menú - Troubleshooting y Verificación

## ✅ Verificar que el Filtrado Funcione

### Paso 1: Abrir DevTools → Console

Cuando cargues la página principal después del login, debes ver:

```javascript
// Logs que DEBEN aparecer:
[PermissionService] Validando feature: REPORT_ADVANCED
[PermissionService] Validando feature: CREDIT_NOTE_CREATE
[PermissionService] Validando feature: USER_MANAGEMENT
// ... más validaciones ...

Menús filtrados por permisos: X grupos  // ✅ Este es el log clave
```

---

### Paso 2: Verificar Requests en Network Tab

**Busca múltiples requests a:**

```
POST http://localhost:8280/api/permission/validate-feature
```

**Debe hacer 1 request por cada item del menú que tenga feature code.**

---

## 🐛 Problema: El menú NO se filtra

### Síntoma

Ves TODOS los items del menú sin importar tu plan.

### Causas Posibles

#### 1️⃣ El log "Menús filtrados" NO aparece

**Solución:** Verificar que el servicio se esté ejecutando.

**En `app.component.ts`, línea ~121:**

```typescript
this.menuService.menuChange
  .pipe(
    switchMap((data) => {
      // ✅ Agregar log aquí
      console.log("🔍 [DEBUG] Menús recibidos del backend:", data);
      return this.menuFilterService.filterMenus(data);
    }),
    takeUntil(this.destroy$)
  )
  .subscribe((filteredMenus) => {
    console.log("🔍 [DEBUG] Menús después de filtrar:", filteredMenus);
    this.menus = filteredMenus;
    // ...
  });
```

---

#### 2️⃣ Las URLs del menú no coinciden con el mapeo

**Problema:** El backend devuelve URLs diferentes a las que tenemos en el mapeo.

**Verificar:**

```typescript
// En console, ejecuta:
console.log("Menús:", menus);

// Revisa las URLs de cada item:
menus.forEach((menu) => {
  console.log("Grupo:", menu.name);
  menu.subMenus?.forEach((sub) => {
    console.log("  -", sub.name, "→", sub.url);
  });
});
```

**Si las URLs son diferentes, actualizar el mapeo:**

```typescript
// En menu-filter.service.ts
private readonly urlToFeatureMap: { [key: string]: string } = {
  // Agregar las URLs exactas que devuelve el backend
  '/la-url-real-del-backend': 'FEATURE_CODE',
};
```

---

#### 3️⃣ Backend no responde o devuelve error

**Verificar en Network:**

- Status debe ser `200 OK`
- Response debe ser: `{ "allowed": true/false, ... }`

**Si devuelve 400/404:**

```typescript
// El filtro asume que NO tiene permiso
// Esto puede ocultar items válidos
```

---

## 🔍 Debug Detallado

### Agregar Logs Temporales

**En `menu-filter.service.ts`:**

```typescript
// Línea ~74 (método filterMenu)
private filterMenu(menu: Menu): Observable<Menu | null> {
  console.log('🔍 Filtrando menú:', menu.name, 'URL:', menu.url);

  if (menu.subMenus && menu.subMenus.length > 0) {
    return this.filterSubMenus(menu);
  }

  const featureCode = this.getFeatureCodeFromUrl(menu.url);
  console.log('🔍 Feature code para', menu.name, ':', featureCode);

  if (!featureCode) {
    console.log('✅ Sin feature code, permitiendo:', menu.name);
    return of(menu);
  }

  return this.permissionService.canAccess(featureCode).pipe(
    map((allowed) => {
      console.log(
        allowed ? '✅' : '❌',
        menu.name,
        '- Feature:',
        featureCode,
        '- Permitido:',
        allowed
      );
      return allowed ? menu : null;
    })
  );
}
```

---

## 📋 Checklist de Verificación

### En Console

- [ ] Aparece el log: `Menús filtrados por permisos: X grupos`
- [ ] Aparecen múltiples: `[PermissionService] Validando feature: ...`
- [ ] Se muestran ✅/❌ para cada item del menú

### En Network

- [ ] Hay múltiples `POST /api/permission/validate-feature`
- [ ] Todas devuelven 200 OK
- [ ] Responses tienen `{ "allowed": true/false, ... }`

### En el Menú Visual

- [ ] Items sin permiso NO aparecen
- [ ] Items con permiso SÍ aparecen
- [ ] Grupos vacíos (sin submenús válidos) NO aparecen

---

## 🎯 Testing Manual

### Test 1: Usuario con Plan FREE

**Ejecutar en console:**

```javascript
// Ver plan actual
const token = decodeToken();
console.log("Token:", token);

// Ver menús después de filtrar
console.log("Menús visibles:", this.menus);
```

**Debe mostrar:**

- ✅ Facturas
- ✅ Clientes
- ✅ Productos
- ❌ NO Notas de Crédito
- ❌ NO Usuarios
- ❌ NO Reportes Avanzados

---

### Test 2: Forzar Permiso

**En console, probar:**

```javascript
// Inyectar el servicio manualmente
const permissionService = app.injector.get(PermissionService);

// Verificar un feature
permissionService.canAccess("REPORT_ADVANCED").subscribe((allowed) => {
  console.log("¿Puede acceder a REPORT_ADVANCED?", allowed);
});

permissionService.canAccess("INVOICE_CREATE").subscribe((allowed) => {
  console.log("¿Puede acceder a INVOICE_CREATE?", allowed);
});
```

---

## 🛠️ Soluciones Rápidas

### Solución 1: Deshabilitar Filtrado Temporalmente

**En `app.component.ts`:**

```typescript
// TEMPORAL: Sin filtrar
this.menuService.menuChange.pipe(takeUntil(this.destroy$)).subscribe((data) => {
  this.menus = data; // Sin filtrar
  console.warn("⚠️ TEMPORAL: Menús SIN FILTRAR");
});
```

**Esto muestra TODO para verificar que el backend funcione.**

---

### Solución 2: Filtrado Solo por URLs Conocidas

**En `menu-filter.service.ts`:**

```typescript
// Lista de URLs que SIEMPRE deben ocultarse
private readonly blockedUrls = [
  '/reports/sales-dashboard',
  '/reports/branch-comparison',
  '/credit-notes',
  '/users',
  '/establishments',
];

private filterMenu(menu: Menu): Observable<Menu | null> {
  // Bloquear si está en la lista
  if (this.blockedUrls.includes(menu.url)) {
    console.log('❌ Bloqueando por URL:', menu.url);
    return of(null);
  }

  // Permitir todo lo demás
  return of(menu);
}
```

**Esto NO valida con el backend, solo filtra por URL.**

---

## ✅ Verificación Final

Una vez que funcione, deberías ver:

**Usuario FREE:**

```
Menús filtrados por permisos: 4 grupos
✅ Facturación
✅ Clientes
✅ Reportes Básicos
✅ Configuración
```

**Usuario PRO:**

```
Menús filtrados por permisos: 6 grupos
✅ Facturación
✅ Notas de Crédito
✅ Clientes
✅ Usuarios
✅ Reportes (todos)
✅ Configuración
```

---

## 📞 Si Aún No Funciona

1. Pega los logs de Console aquí
2. Pega un screenshot del Network tab
3. Pega la estructura del menú que devuelve el backend

```javascript
// Para obtener la estructura:
console.log(JSON.stringify(menus, null, 2));
```
