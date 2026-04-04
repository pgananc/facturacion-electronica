# 🔐 Seguridad y Escalabilidad del Sistema de Menús

## ❓ Preguntas Frecuentes

### 1. ¿Es seguro validar menús en el frontend?

**✅ SÍ, es seguro.** Aquí está por qué:

#### El Menú es Solo UX (Visual)

Ocultar/mostrar opciones del menú en el frontend es **solo para mejorar la experiencia de usuario**. NO es una medida de seguridad.

#### La Seguridad REAL está en el Backend

Incluso si un usuario malicioso manipula el frontend para mostrar un menú:

```javascript
// Usuario manipula el frontend para ver "Notas de Crédito"
// Intenta acceder a /credit-notes

// ❌ El PermissionGuard lo bloqueará:
@Injectable()
export class PermissionGuard implements CanActivate {
  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    // 🔒 Llama al backend para validar
    return this.http
      .post("http://localhost:8280/api/permission/validate-feature", {
        featureCode: "CREDIT_NOTE_CREATE",
        idCompany: token.idCompany,
      })
      .pipe(
        map((response) => response.allowed) // Backend decide si permite o no
      );
  }
}

// Si el backend devuelve allowed: false
// → El usuario NO puede acceder
// → Se muestra toast de error
// → Se redirige a /welcome
```

#### Capas de Seguridad

```
┌────────────────────────────────────────────┐
│ 1. MENÚ (UX)                               │
│    Oculta opciones que no puede usar      │ 👈 SOLO UX
│    ❌ NO es seguridad                      │
└────────────────────────────────────────────┘
           ↓ Usuario intenta acceder
┌────────────────────────────────────────────┐
│ 2. PERMISSION GUARD (Frontend)             │
│    Llama a /api/permission/validate-feature│ 👈 SEGURIDAD REAL
│    ✅ Backend valida rol + plan            │
└────────────────────────────────────────────┘
           ↓ Si pasa el guard
┌────────────────────────────────────────────┐
│ 3. BACKEND ENDPOINTS (API)                 │
│    Valida token JWT                        │ 👈 SEGURIDAD ADICIONAL
│    Valida permisos de nuevo                │
│    ✅ Doble verificación                   │
└────────────────────────────────────────────┘
```

**Conclusión:** El filtrado de menús en frontend es seguro porque la seguridad real está en el backend.

---

### 2. ¿Necesito hacer deploy cada vez que agrego un menú?

**Depende de la implementación:**

#### ❌ Con Implementación Actual: SÍ

**Por qué:**

```typescript
// En menu-filter.service.ts
private readonly urlToFeatureMap: { [key: string]: string } = {
  '/invoices': 'INVOICE_CREATE',     // 👈 Hardcoded
  '/clients': 'CLIENT_CREATE',
  '/credit-notes': 'CREDIT_NOTE_CREATE',
  // Si agregas un menú nuevo:
  '/nueva-feature': 'NUEVA_FEATURE', // 👈 Requiere deploy del frontend
};
```

**Pasos para agregar un menú:**

1. Agregar menú en la base de datos (backend)
2. Agregar feature code en tabla `features` (backend)
3. **Actualizar `menu-filter.service.ts` (frontend) 👈 Requiere deploy**
4. Deploy frontend

---

#### ✅ Con Backend Enviando Feature Code: NO

**Solución recomendada:**

##### Backend Incluye `featureCode` en el Menú

**1. Actualizar modelo en Backend (Java):**

```java
// Menu.java
public class Menu {
    private Long id;
    private String name;
    private String icon;
    private String url;
    private String featureCode;  // 👈 NUEVO CAMPO
    private List<Menu> subMenus;

    // getters, setters, etc.
}
```

**2. Actualizar tabla en Base de Datos:**

```sql
-- Agregar columna feature_code
ALTER TABLE menu
ADD COLUMN feature_code VARCHAR(50);

-- Actualizar menús existentes
UPDATE menu SET feature_code = 'INVOICE_CREATE' WHERE url = '/invoices';
UPDATE menu SET feature_code = 'CREDIT_NOTE_CREATE' WHERE url = '/credit-notes';
UPDATE menu SET feature_code = 'CLIENT_CREATE' WHERE url = '/clients';
UPDATE menu SET feature_code = 'USER_MANAGEMENT' WHERE url = '/users';
UPDATE menu SET feature_code = 'COMPANY_SETTINGS' WHERE url = '/companies';
UPDATE menu SET feature_code = 'ESTABLISHMENT_MANAGEMENT' WHERE url = '/establishments';
UPDATE menu SET feature_code = 'ESTABLISHMENT_MANAGEMENT' WHERE url = '/invoice-points';
UPDATE menu SET feature_code = 'REPORT_ADVANCED' WHERE url = '/reports/sales-dashboard';
UPDATE menu SET feature_code = 'REPORT_ADVANCED' WHERE url = '/reports/branch-comparison';

-- Ver resultado
SELECT id, name, url, feature_code FROM menu WHERE active = true;
```

**3. Frontend YA está listo** (actualización aplicada):

```typescript
// menu.ts
export interface Menu {
  id: number;
  name: string;
  icon: string;
  url: string;
  featureCode?: string;  // 👈 YA AGREGADO
  subMenus: Menu[];
}

// menu-filter.service.ts
private filterMenu(menu: Menu): Observable<Menu | null> {
  // 🔧 Prioriza featureCode del backend si existe
  const featureCode = menu.featureCode || this.getFeatureCodeFromUrl(menu.url);

  if (!featureCode) {
    return of(menu); // Ruta pública
  }

  // Validar con backend
  return this.permissionService.canAccess(featureCode);
}
```

**4. Response del backend con `featureCode`:**

```json
[
  {
    "id": 1,
    "name": "Facturación",
    "subMenus": [
      {
        "id": 10,
        "name": "Nueva Factura",
        "url": "/invoices",
        "featureCode": "INVOICE_CREATE" // 👈 Backend lo envía
      }
    ]
  },
  {
    "id": 2,
    "name": "Notas de Crédito",
    "subMenus": [
      {
        "id": 20,
        "name": "Nueva Nota de Crédito",
        "url": "/credit-notes",
        "featureCode": "CREDIT_NOTE_CREATE" // 👈 Backend lo envía
      }
    ]
  }
]
```

**Pasos para agregar un menú (CON esta solución):**

1. Agregar menú en la base de datos con `feature_code` ✅
2. Agregar feature code en tabla `features` (si no existe) ✅
3. ~~Actualizar frontend~~ ❌ **NO NECESARIO**
4. ~~Deploy frontend~~ ❌ **NO NECESARIO**

**¡Solo necesitas actualizar la base de datos!** 🎉

---

## 📊 Comparación de Enfoques

| Aspecto                    | Hardcoded en Frontend      | Feature Code del Backend  |
| -------------------------- | -------------------------- | ------------------------- |
| **Deploy al agregar menú** | ❌ Sí, siempre             | ✅ No necesario           |
| **Mantenimiento**          | ❌ Duplicado (DB + código) | ✅ Centralizado en DB     |
| **Flexibilidad**           | ❌ Requiere programador    | ✅ Admin puede gestionar  |
| **Consistencia**           | ❌ Puede desincronizarse   | ✅ Siempre sincronizado   |
| **Seguridad**              | ✅ Igual (backend valida)  | ✅ Igual (backend valida) |
| **Performance**            | ✅ Mismo                   | ✅ Mismo                  |
| **Complejidad backend**    | ✅ Más simple              | ⚠️ Un campo más           |
| **Complejidad frontend**   | ⚠️ Mapeo manual            | ✅ Automático             |

---

## 🎯 Recomendación

### ✅ Implementa: Backend Envía Feature Code

**Por qué:**

1. **Sin deploys innecesarios** - Agrega menús desde la DB
2. **Más escalable** - Gestión centralizada
3. **Menos errores** - No hay desincronización
4. **Más flexible** - Cambios rápidos sin programar

**Costo:**

- Un campo adicional en la tabla `menu`
- Actualizar el endpoint que devuelve menús (ya lo hace)

**Beneficio:**

- Agregar menús nuevos sin tocar código
- Sistema auto-configurable

---

## 🛠️ Implementación Paso a Paso

### Paso 1: Backend - Agregar Campo

```sql
-- Base de datos
ALTER TABLE menu ADD COLUMN feature_code VARCHAR(50);

-- Actualizar menús existentes
UPDATE menu SET feature_code = 'INVOICE_CREATE' WHERE url = '/invoices';
UPDATE menu SET feature_code = 'CREDIT_NOTE_CREATE' WHERE url = '/credit-notes';
UPDATE menu SET feature_code = 'CLIENT_CREATE' WHERE url = '/clients';
UPDATE menu SET feature_code = 'USER_MANAGEMENT' WHERE url = '/users';
UPDATE menu SET feature_code = 'COMPANY_SETTINGS' WHERE url = '/companies';
UPDATE menu SET feature_code = 'ESTABLISHMENT_MANAGEMENT' WHERE url = '/establishments';
UPDATE menu SET feature_code = 'REPORT_ADVANCED' WHERE url LIKE '/reports/sales-dashboard%';
```

```java
// Menu.java
@Column(name = "feature_code")
private String featureCode;

// getter y setter
public String getFeatureCode() { return featureCode; }
public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }
```

### Paso 2: Frontend - YA ESTÁ LISTO ✅

El código ya está actualizado para soportar `featureCode` del backend:

```typescript
// menu.ts - YA tiene el campo
export interface Menu {
  featureCode?: string; // ✅
}

// menu-filter.service.ts - YA lo usa
const featureCode = menu.featureCode || this.getFeatureCodeFromUrl(menu.url); // ✅
```

### Paso 3: Testing

```bash
# Ver response del backend
curl -X GET http://localhost:8280/api/menu/0102030405 \
  -H "Authorization: Bearer <token>" \
  | jq

# Debe incluir featureCode:
# {
#   "id": 10,
#   "name": "Nueva Factura",
#   "url": "/invoices",
#   "featureCode": "INVOICE_CREATE"  👈 Debe aparecer
# }
```

### Paso 4: Agregar Nuevo Menú (Sin Deploy)

```sql
-- Agregar menú nuevo
INSERT INTO menu (name, icon, url, feature_code, parent_id, active, order_num)
VALUES ('Reportes Financieros', 'account_balance', '/reports/financial', 'REPORT_FINANCIAL', 4, true, 3);

-- Asegurar que el feature existe
INSERT INTO features (feature_code, feature_name, description, feature_module, allowed_roles, required_plans, is_active)
VALUES ('REPORT_FINANCIAL', 'Reportes Financieros', 'Acceso a reportes financieros avanzados', 'REPORTS', ARRAY[1,2], ARRAY['PROFESSIONAL', 'ENTERPRISE'], true);
```

**¡Listo! No se necesita deploy del frontend.**

---

## 🔒 Seguridad - Resumen

**Pregunta:** ¿Es seguro si alguien manipula el frontend?

**Respuesta:** SÍ, completamente seguro.

### Por qué:

```
Usuario manipula frontend
  ↓
Ve menú "Reportes Avanzados"
  ↓
Click en el menú
  ↓
PermissionGuard intercepta → Llama a backend
  ↓
Backend verifica: rol + plan
  ↓
Backend responde: { "allowed": false, "reason": "PLAN_DOES_NOT_INCLUDE_FEATURE" }
  ↓
PermissionGuard bloquea acceso
  ↓
Toast: "Requiere plan superior"
  ↓
Redirige a /welcome
```

**No puede:**

- ❌ Acceder a la ruta
- ❌ Ver los datos
- ❌ Hacer requests a los endpoints (backend valida token)

**Puede:**

- ✅ Ver el botón en el menú (pero no sirve de nada)

---

## 📝 Checklist de Migración

### Backend

- [ ] Agregar columna `feature_code` a tabla `menu`
- [ ] Actualizar modelo `Menu.java` con el campo
- [ ] Poblar `feature_code` para menús existentes
- [ ] Verificar que el endpoint devuelve el campo

### Frontend

- [x] Actualizar interface `Menu` ✅ (Ya hecho)
- [x] Actualizar `MenuFilterService` ✅ (Ya hecho)
- [ ] Testing con feature codes del backend

### Testing

- [ ] Verificar que los menús se filtran correctamente
- [ ] Agregar un menú nuevo solo desde DB
- [ ] Verificar que aparece sin deploy del frontend

---

## 🎉 Beneficios Finales

Con `featureCode` del backend:

✅ **Cero deploys** para agregar menús
✅ **Gestión desde DB** - Admin puede hacerlo
✅ **Más escalable** - Crece sin programar
✅ **Centralizado** - Todo en backend
✅ **Seguro** - Backend siempre valida

**Inversión:** 1 hora de desarrollo
**Ahorro:** Horas de deploys en el futuro
