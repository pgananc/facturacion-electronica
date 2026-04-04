# 🔧 Configuración del Menú - ACTUALIZADO CON CONVENCIÓN REST

## ✅ URLs ACTUALIZADAS - CONVENCIÓN REST APLICADA

El backend ya aplicó la convención REST estándar (recursos en plural).  
El frontend ha sido actualizado para coincidir con las nuevas URLs.

---

## 📋 URLs Finales (Backend y Frontend sincronizados)

### **✅ URLs ya configuradas en el backend:**

| Menú                                | URL               | Estado                                          |
| ----------------------------------- | ----------------- | ----------------------------------------------- |
| **Gestionar compañía**              | `/companies`      | ✅ Actualizado (antes `/company`)               |
| **Gestionar Sucursal**              | `/establishments` | ✅ Actualizado (antes `/company/establishment`) |
| **Gestionar Punto de Emisión**      | `/invoice-points` | ✅ Actualizado (antes `/company/invoice-point`) |
| **Gestionar clientes**              | `/clients`        | ✅ Actualizado (antes `/client`)                |
| **Gestionar productos**             | `/products`       | ✅ Actualizado (antes `/product`)               |
| **Gestionar usuario**               | `/users`          | ✅ Actualizado (antes `/user`)                  |
| **Gestionar usuarios por compañía** | `/company-users`  | ✅ Actualizado (antes `/company/company-user`)  |
| **Facturas**                        | `/invoices`       | ✅ Actualizado (antes `/billing`)               |
| **Borradores**                      | `/drafts`         | ✅ Correcto                                     |

---

## 🎯 Convención REST Aplicada

### **Regla 1: Recursos en PLURAL**

```
✅ /companies       (colección de compañías)
✅ /establishments  (colección de sucursales)
✅ /invoice-points  (colección de puntos de emisión)
✅ /clients         (colección de clientes)
✅ /products        (colección de productos)
✅ /users           (colección de usuarios)
✅ /invoices        (colección de facturas)
✅ /drafts          (colección de borradores)
```

### **Regla 2: Sub-recursos como recursos independientes**

```
❌ ANTES (anidados):
   /company/establishment
   /company/invoice-point
   /company/company-user

✅ AHORA (independientes):
   /establishments
   /invoice-points
   /company-users
```

### **Regla 3: kebab-case para nombres compuestos**

```
✅ /invoice-points   (correcto)
✅ /company-users    (correcto)
❌ /invoicePoints    (camelCase - incorrecto)
❌ /invoice_points   (snake_case - incorrecto)
```

---

## ✅ Estado de Implementación

### **Backend** ✅ COMPLETADO

- Scripts SQL ejecutados
- Tabla `menu` actualizada con nuevas URLs

### **Frontend** ✅ COMPLETADO

- `app-routing.module.ts` actualizado
- Módulos independientes creados:
  - `EstablishmentModule` → `/establishments`
  - `InvoicePointModule` → `/invoice-points`
- Lazy loading configurado correctamente

---

## 🔗 Mapeo Completo Frontend-Backend

| Módulo Frontend     | Ruta Principal    | Menú Backend                    | URL Backend       |
| ------------------- | ----------------- | ------------------------------- | ----------------- |
| CustomerModule      | `/clients`        | Gestionar clientes              | `/clients`        |
| ProductModule       | `/products`       | Gestionar productos             | `/products`       |
| UserModule          | `/users`          | Gestionar usuario               | `/users`          |
| CompanyModule       | `/companies`      | Gestionar compañía              | `/companies`      |
| EstablishmentModule | `/establishments` | Gestionar Sucursal              | `/establishments` |
| InvoicePointModule  | `/invoice-points` | Gestionar Punto de Emisión      | `/invoice-points` |
| CompanyModule       | `/company-users`  | Gestionar usuarios por compañía | `/company-users`  |
| BillingModule       | `/invoices`       | Facturas                        | `/invoices`       |
| BillingModule       | `/drafts`         | Borradores                      | `/drafts`         |

---

## 🧪 Testing - Verificación Final

### **1. Cerrar sesión y volver a iniciar**

```bash
# Esto recargará el menú desde el backend con las nuevas URLs
```

### **2. Verificar navegación**

Cada menú debe navegar correctamente a:

- ✅ `/companies` → Gestionar compañía
- ✅ `/establishments` → Gestionar Sucursal
- ✅ `/invoice-points` → Gestionar Punto de Emisión
- ✅ `/clients` → Gestionar clientes
- ✅ `/products` → Gestionar productos
- ✅ `/users` → Gestionar usuario
- ✅ `/company-users` → Gestionar usuarios por compañía
- ✅ `/invoices` → Facturas
- ✅ `/drafts` → Borradores

### **3. Verificar que NO hay errores 404**

Si aparece "página no existe", significa que hay una discrepancia entre backend y frontend.

---

## 📊 Estructura del Menú Final

```
📁 Cliente
   └─ 👥 Gestionar clientes (/clients)

📁 Producto
   └─ 📦 Gestionar productos (/products)

📁 Compañía
   ├─ 📄 Gestionar compañía (/companies)
   ├─ 🏢 Gestionar Sucursal (/establishments)
   ├─ 📍 Gestionar Punto de Emisión (/invoice-points)
   └─ 👥 Gestionar usuarios por compañía (/company-users)

📁 Usuario
   └─ 👤 Gestionar usuario (/users)

📁 Facturación Electrónica
   ├─ 📄 Facturas (/invoices)
   └─ 📋 Borradores (/drafts)
```

---

## ⚠️ IMPORTANTE

### ✅ URLs CORRECTAS (USAR):

- `/companies` (plural)
- `/establishments` (plural)
- `/invoice-points` (plural)
- `/clients` (plural)
- `/products` (plural)
- `/users` (plural)
- `/invoices` (plural)
- `/drafts` (plural)
- `/company-users` (kebab-case)

### ❌ URLs ANTIGUAS (NO USAR):

- `/company` (singular) ❌
- `/company/establishment` (anidado) ❌
- `/company/invoice-point` (anidado) ❌
- `/client` (singular) ❌
- `/product` (singular) ❌
- `/user` (singular) ❌
- `/billing` (acción en lugar de recurso) ❌

---

## 🎉 Beneficios de la Implementación

✅ **Estándar REST**: Sigue las mejores prácticas internacionales  
✅ **Consistencia**: Todas las URLs siguen el mismo patrón  
✅ **Claridad**: URLs intuitivas y predecibles  
✅ **Escalabilidad**: Fácil agregar nuevos recursos  
✅ **Mantenibilidad**: Código más limpio y organizado  
✅ **SEO-friendly**: URLs descriptivas y semánticas

---

**Fecha actualización**: Noviembre 2024  
**Estado**: ✅ COMPLETADO - Backend y Frontend sincronizados

---

## 📋 SQL para Insertar en Base de Datos

### **1. Gestionar Sucursal**

```sql
INSERT INTO menu (
  id_menu,
  name,
  url,
  icon,
  id_menu_parent,
  position,
  status
) VALUES (
  <NEXT_ID>,                          -- ID secuencial siguiente
  'Gestionar Sucursal',               -- Nombre del menú
  '/company/establishments',          -- ⚠️ URL CORRECTA (PLURAL)
  'business',                         -- Icono Material
  <ID_DEL_MENU_COMPAÑIA>,            -- ID del menú padre "Compañía"
  3,                                  -- Posición (después de Gestionar Compañía)
  true                                -- Activo
);
```

### **2. Gestionar Punto de Emisión**

```sql
INSERT INTO menu (
  id_menu,
  name,
  url,
  icon,
  id_menu_parent,
  position,
  status
) VALUES (
  <NEXT_ID>,                          -- ID secuencial siguiente
  'Gestionar Punto de Emisión',      -- Nombre del menú
  '/company/issuance-points',         -- ⚠️ URL CORRECTA (PLURAL)
  'point_of_sale',                    -- Icono Material
  <ID_DEL_MENU_COMPAÑIA>,            -- ID del menú padre "Compañía"
  4,                                  -- Posición (después de Gestionar Sucursal)
  true                                -- Activo
);
```

---

## 🎯 URLs Correctas (Frontend)

| Menú                           | URL Frontend               | Componente                 |
| ------------------------------ | -------------------------- | -------------------------- |
| **Gestionar Sucursal**         | `/company/establishments`  | EstablishmentListComponent |
| **Gestionar Punto de Emisión** | `/company/issuance-points` | IssuancePointListComponent |

---

## 📝 Iconos Material Sugeridos

- **Sucursal**: `business`, `store`, `location_city`, `domain`
- **Punto de Emisión**: `point_of_sale`, `receipt_long`, `print`, `qr_code_2`

---

## 🔍 Cómo Encontrar el ID del Menú Padre

```sql
-- Encontrar el ID del menú "Compañía"
SELECT id_menu, name, url
FROM menu
WHERE name LIKE '%Compañ%' OR name LIKE '%Company%';
```

---

## 🔐 Asignación de Roles

Después de crear los menús, asignarlos a los roles apropiados:

```sql
-- Asignar "Gestionar Sucursal" a roles ADMIN y COMPANY_ADMIN
INSERT INTO role_menu (id_role, id_menu)
VALUES
  (<ID_ROLE_ADMIN>, <ID_MENU_GESTIONAR_SUCURSAL>),
  (<ID_ROLE_COMPANY_ADMIN>, <ID_MENU_GESTIONAR_SUCURSAL>);

-- Asignar "Gestionar Punto de Emisión" a roles ADMIN y COMPANY_ADMIN
INSERT INTO role_menu (id_role, id_menu)
VALUES
  (<ID_ROLE_ADMIN>, <ID_MENU_GESTIONAR_PUNTO_EMISION>),
  (<ID_ROLE_COMPANY_ADMIN>, <ID_MENU_GESTIONAR_PUNTO_EMISION>);
```

---

## ✅ Estructura del Menú Final

```
📁 Compañía
   ├─ 📄 Gestionar Compañía (/company)
   ├─ 👥 Asignar Usuario (/company/company-user)
   ├─ 🏢 Gestionar Sucursal (/company/establishments) ⭐ NUEVO
   └─ 📍 Gestionar Punto de Emisión (/company/issuance-points) ⭐ NUEVO
```

---

## 🧪 Testing

Después de insertar los menús en la base de datos:

1. **Cerrar sesión** en el frontend
2. **Iniciar sesión** nuevamente (para recargar el menú desde el backend)
3. **Verificar** que aparecen los 2 nuevos elementos en el menú "Compañía"
4. **Hacer clic** en cada uno para verificar que navegan correctamente

---

## ⚠️ IMPORTANTE

### ❌ URLs INCORRECTAS (NO USAR):

- `/company/establishment` (singular)
- `/company/invoice-issuance-point` (singular)

### ✅ URLs CORRECTAS (USAR):

- `/company/establishments` (plural)
- `/company/issuance-points` (plural)

---

## 📞 Consultas

Si tienes dudas sobre:

- Los IDs de los menús existentes
- Los IDs de roles
- La estructura de la tabla `menu`

Por favor revisa el script de base de datos o consulta con el equipo de frontend.

---

**Fecha**: Noviembre 2024  
**Estado**: Pendiente de implementación en backend
