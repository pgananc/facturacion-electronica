# 🧪 Test Rápido del Filtrado de Menús

## ✅ Cómo Verificar en 3 Pasos

### Paso 1: Abrir DevTools

```
Cmd+Option+I (Mac) o F12 (Windows/Linux)
```

### Paso 2: Ir a la pestaña Console

### Paso 3: Buscar estos logs

**Deben aparecer:**

```
📋 [MenuFilter] Menús recibidos del backend: X grupos
📋 [MenuFilter] Estructura: [ ... ]
✅ [MenuFilter] Menús después de filtrar: Y grupos
✅ [MenuFilter] Resultado: [ ... ]
```

---

## 🎯 ¿Qué Significan los Números?

### Ejemplo Usuario FREE:

```
📋 Menús recibidos del backend: 8 grupos
✅ Menús después de filtrar: 5 grupos  👈 Se filtraron 3 grupos
```

**Grupos filtrados (ocultos):**

- ❌ Notas de Crédito (requiere BASIC+)
- ❌ Usuarios (requiere BASIC+)
- ❌ Reportes Avanzados (requiere PRO+)

---

### Ejemplo Usuario BASIC:

```
📋 Menús recibidos del backend: 8 grupos
✅ Menús después de filtrar: 7 grupos  👈 Se filtró 1 grupo
```

**Grupo filtrado:**

- ❌ Reportes Avanzados (requiere PRO+)

---

### Ejemplo Usuario PRO:

```
📋 Menús recibidos del backend: 8 grupos
✅ Menús después de filtrar: 8 grupos  👈 No se filtró nada
```

**Ningún grupo filtrado (tiene acceso a TODO)**

---

## 🐛 Si No Ves los Logs

### Opción 1: Recargar la Página

```
Cmd+R (Mac) o Ctrl+R (Windows/Linux)
```

### Opción 2: Hacer Logout/Login

```
1. Click en menú usuario (arriba derecha)
2. Click en "Cerrar sesión"
3. Login nuevamente
```

---

## 🔍 Ver Estructura Detallada de los Menús

**En Console, ejecuta:**

```javascript
// Ver los menús actuales
console.table(
  this.menus.map((m) => ({
    id: m.id,
    nombre: m.name,
    submenus: m.subMenus?.length || 0,
  }))
);
```

---

## ✅ Checklist Rápido

- [ ] Los logs `[MenuFilter]` aparecen
- [ ] El número de grupos después de filtrar es menor que el original (para FREE/BASIC)
- [ ] En el menú visual NO aparecen opciones premium
- [ ] Puedes acceder solo a las rutas permitidas

---

## 📊 Comparación Visual

### ANTES (sin filtrar):

```
📋 Menú (8 grupos)
  ├─ Facturación
  ├─ Notas de Crédito  ❌ (no debería ver con FREE)
  ├─ Clientes
  ├─ Usuarios  ❌ (no debería ver con FREE)
  ├─ Reportes
  │   ├─ Ventas por período
  │   └─ Dashboard Avanzado  ❌ (no debería ver con FREE/BASIC)
  └─ Configuración
```

### DESPUÉS (con filtro FREE):

```
✅ Menú (5 grupos)
  ├─ Facturación
  ├─ Clientes
  ├─ Reportes
  │   └─ Ventas por período
  └─ Configuración
```

---

## 🚀 Si Todo Funciona

Deberías ver algo como esto en el menú lateral:

**Plan FREE:**

- ✅ Facturación → Nueva Factura
- ✅ Clientes
- ✅ Productos
- ✅ Reportes → Ventas por período, Pendientes
- ✅ Configuración

**NO deberías ver:**

- ❌ Notas de Crédito
- ❌ Usuarios
- ❌ Establecimientos
- ❌ Dashboard Avanzado

---

## 💡 Tip: Verificar tu Plan

**En Console:**

```javascript
// Ver token
const token = decodeToken();
console.log("Usuario:", token.userName);
console.log("Roles:", token.roles);

// Ver suscripción
const permissionService = // obtener del injector
  permissionService.getSubscription().subscribe((sub) => {
    console.log("Plan:", sub?.plan?.code);
    console.log("Estado:", sub?.status);
  });
```
