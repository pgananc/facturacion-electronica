# 📋 Flujo de Carga de Menús - Explicación Completa

## 🔄 Diagrama de Flujo

```
1. WelcomeComponent.ngOnInit()
   │
   ├─> Si hay idCompany e idEstablishment en localStorage
   │   │
   │   ├─> Cargar roles de la compañía (UserRoleService.getRolesByCompany)
   │   │   └─> Esperar a que se carguen los roles
   │   │
   │   └─> loadMenuAndSetCompany()
   │       │
   │       └─> MenuService.findMenuByUser(userName)
   │           │
   │           └─> GET /api/menu/{userName}
   │               │
   │               └─> Backend retorna todos los menús del usuario
   │
   └─> Si NO hay idCompany o idEstablishment
       │
       └─> loadDataCompanies()
           │
           └─> Usuario selecciona compañía y establecimiento
               │
               └─> openDialog().afterClosed()
                   │
                   └─> Cargar roles de la compañía seleccionada
                       │
                       └─> loadMenuAndSetCompany()
                           │
                           └─> MenuService.findMenuByUser(userName)

2. MenuService.findMenuByUser() retorna Observable<Menu[]>
   │
   └─> En el subscribe:
       │
       └─> menuService.menuChange.next(menu)  ← Emite los menús sin filtrar

3. AppComponent.ngOnInit() (se ejecuta al iniciar la app)
   │
   └─> Suscripción a menuService.menuChange
       │
       └─> Cuando se emiten menús:
           │
           └─> MenuFilterService.filterMenus(menus)
               │
               ├─> Verificar si usuario es super_admin
               │   │
               │   ├─> Si es super_admin → Retornar todos los menús (sin filtrar)
               │   │
               │   └─> Si NO es super_admin:
               │       │
               │       ├─> Verificar si hay suscripción activa
               │       │
               │       ├─> Si hay suscripción:
               │       │   │
               │       │   └─> Filtrar cada menú según permisos
               │       │       │
               │       │       └─> PermissionService.canAccess(featureCode)
               │       │           │
               │       │           └─> GET /api/permission/validate-feature
               │       │               │
               │       │               └─> Retorna si el usuario tiene permiso
               │       │
               │       └─> Si NO hay suscripción:
               │           │
               │           └─> Mostrar todos los menús temporalmente
               │
               └─> Retornar menús filtrados

4. AppComponent recibe menús filtrados
   │
   └─> this.menus = filteredMenus
       │
       └─> Se renderizan en el sidenav
```

## 📝 Explicación Detallada

### 1. **Inicio del Flujo - WelcomeComponent**

**Ubicación:** `src/app/pages/welcome/welcome.component.ts`

**Método:** `ngOnInit()`

**Proceso:**
- Al iniciar, verifica si hay `idCompany` e `idEstablishment` en `localStorage`
- Si existen:
  1. Carga los roles de la compañía usando `UserRoleService.getRolesByCompany()`
  2. Espera a que los roles se carguen
  3. Llama a `loadMenuAndSetCompany()`
- Si no existen:
  1. Carga las compañías disponibles
  2. Muestra un diálogo para seleccionar compañía y establecimiento
  3. Cuando el usuario selecciona, carga los roles y luego llama a `loadMenuAndSetCompany()`

### 2. **Carga de Menús desde el Backend**

**Método:** `loadMenuAndSetCompany()`

**Proceso:**
```typescript
// 1. Obtiene el userName del token
const token = decodeToken();
const userName = token.userName;

// 2. Llama al servicio para obtener menús
this.menuService.findMenuByUser(userName)
  .subscribe({
    next: (menu) => {
      // 3. Emite los menús a través del BehaviorSubject
      this.menuService.menuChange.next(menu);
      this.menuService.isLoginChange.next(true);
    }
  });
```

**Endpoint del Backend:**
- `GET /api/menu/{userName}`
- Retorna todos los menús asignados al usuario (sin filtrar por permisos)

### 3. **MenuService - BehaviorSubject**

**Ubicación:** `src/app/_service/auth/menu.service.ts`

**Propiedad:**
```typescript
menuChange = new BehaviorSubject<Menu[]>([]);
```

**Comportamiento:**
- Es un `BehaviorSubject`, lo que significa que:
  - Mantiene el último valor emitido
  - Los nuevos suscriptores reciben inmediatamente el último valor
  - Permite múltiples suscripciones

**Flujo:**
1. `WelcomeComponent` emite menús: `menuService.menuChange.next(menu)`
2. `AppComponent` está suscrito y recibe los menús automáticamente

### 4. **Filtrado de Menús - AppComponent**

**Ubicación:** `src/app/app.component.ts`

**Método:** `ngOnInit()`

**Suscripción:**
```typescript
this.menuService.menuChange
  .pipe(
    switchMap((data) => {
      // Filtrar menús por permisos de suscripción
      return this.menuFilterService.filterMenus(data);
    }),
    takeUntil(this.destroy$)
  )
  .subscribe((filteredMenus) => {
    this.menus = filteredMenus;
    // Los menús se renderizan en el sidenav
  });
```

**Características:**
- Usa `switchMap` para transformar los menús sin filtrar en menús filtrados
- Cada vez que se emiten nuevos menús, se filtran automáticamente
- Los menús filtrados se asignan a `this.menus` que se usa en el template

### 5. **MenuFilterService - Lógica de Filtrado**

**Ubicación:** `src/app/_service/menu/menu-filter.service.ts`

**Método:** `filterMenus(menus: Menu[])`

**Proceso de Filtrado:**

#### Paso 1: Verificar si es super_admin
```typescript
const roles = this.userRoleService.getCurrentRolesSync();
const isSuperAdmin = roles.includes('super_admin');

if (isSuperAdmin) {
  // Retornar todos los menús sin filtrar
  return of(menus);
}
```

#### Paso 2: Verificar suscripción activa
```typescript
const subscription = this.permissionService.getSubscriptionValue();

if (!subscription || !subscription.plan?.code) {
  // Mostrar todos los menús temporalmente
  return of(menus);
}
```

#### Paso 3: Filtrar cada menú según permisos
```typescript
// Para cada menú, verificar si tiene featureCode
const featureCode = menu.featureCode || this.getFeatureCodeFromUrl(menu.url);

if (featureCode) {
  // Verificar permiso con el backend
  return this.permissionService.canAccess(featureCode)
    .pipe(
      map((allowed) => {
        if (allowed) {
          return menu; // Incluir menú
        }
        return null; // Excluir menú
      })
    );
}
```

**Endpoint de Validación:**
- `GET /api/permission/validate-feature?featureCode={featureCode}&idCompany={idCompany}`
- Retorna `true` o `false` según si el usuario tiene permiso para esa feature

### 6. **Renderizado en el UI**

**Ubicación:** `src/app/app.component.html`

**Template:**
```html
<mat-sidenav-container>
  <mat-sidenav>
    <!-- Los menús filtrados se renderizan aquí -->
    <app-menu-item 
      *ngFor="let menu of menus" 
      [menu]="menu">
    </app-menu-item>
  </mat-sidenav>
</mat-sidenav-container>
```

## 🔑 Puntos Clave

### 1. **Roles y Permisos**
- Los roles se cargan **antes** de cargar los menús
- Los roles se obtienen del backend específicamente para la compañía seleccionada
- El token contiene roles de TODAS las compañías, pero no se usan directamente

### 2. **Filtrado en Dos Niveles**
- **Nivel 1 (Backend):** El backend retorna solo los menús asignados al usuario según sus roles
- **Nivel 2 (Frontend):** El frontend filtra según permisos de suscripción (features)

### 3. **Comportamiento de super_admin**
- Los `super_admin` ven TODOS los menús sin filtrar
- No se validan permisos de suscripción para `super_admin`

### 4. **Suscripción sin Plan**
- Si el usuario no tiene suscripción activa, se muestran todos los menús temporalmente
- Esto permite que el sistema funcione incluso sin plan asignado

### 5. **Actualización Automática**
- Cuando se cambia de compañía, los roles se recargan
- Los menús se vuelven a filtrar automáticamente
- El `BehaviorSubject` asegura que todos los componentes reciban los menús actualizados

## 🐛 Problemas Comunes

### 1. **Menús no se cargan**
- Verificar que `loadMenuAndSetCompany()` se esté llamando
- Verificar que el endpoint `/api/menu/{userName}` retorne datos
- Verificar que `menuService.menuChange.next()` se esté ejecutando

### 2. **Menús se cargan pero no se filtran**
- Verificar que `AppComponent` esté suscrito a `menuService.menuChange`
- Verificar que `MenuFilterService.filterMenus()` se esté ejecutando
- Verificar logs en la consola para ver el proceso de filtrado

### 3. **Roles no se cargan antes de filtrar**
- Verificar que `UserRoleService.getRolesByCompany()` se esté llamando
- Verificar que los roles se carguen antes de llamar a `loadMenuAndSetCompany()`
- Verificar logs para ver si hay errores al cargar roles

## 📊 Logs de Debugging

Para ver el flujo completo, buscar estos logs en la consola:

1. `🔑 [Welcome] Token decodificado` - Token al iniciar
2. `🔄 [Welcome] DETERMINANDO ROLES POR COMPAÑÍA` - Carga de roles
3. `📋 [Welcome] Menús recibidos del backend` - Menús del backend
4. `📋 [MenuFilterService] Menús completos recibidos` - Menús antes de filtrar
5. `[MenuFilterService] Filtrando X menús` - Proceso de filtrado
6. `Menús filtrados por permisos: X grupos` - Menús finales




