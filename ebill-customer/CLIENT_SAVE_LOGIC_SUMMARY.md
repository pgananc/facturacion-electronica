# 📝 Resumen: Lógica de Guardado de Clientes

## 🎯 **COMPORTAMIENTO ACTUAL**

### **Endpoint:** `POST /api/client`

**Flujo simplificado:**

```
Frontend envía POST /api/client con datos del cliente
    ↓
Backend busca: ¿Existe cliente con esa identificación en esa compañía?
    ↓
    ├─ NO existe → CREAR nuevo cliente
    │
    ├─ SÍ existe + ACTIVO → ❌ ERROR "Cliente ya existe con número de identificación X"
    │
    └─ SÍ existe + INACTIVO → REACTIVAR + ACTUALIZAR datos
    ↓
Retorna: ClientDto completo con todos los campos actualizados
```

---

## ✅ **LO QUE SE IMPLEMENTÓ**

1. **Eliminado endpoint de validación:**
   - ❌ `GET /api/client/exist/company/{companyId}/client/{identification}`
   - Ya no se necesita validación previa

2. **Lógica automática en `POST /api/client`:**
   - ✅ Detecta si el cliente existe
   - ✅ Si existe → actualiza (no duplica)
   - ✅ Si estaba inactivo → reactiva automáticamente
   - ✅ Maneja contactos: actualiza existentes + crea nuevos

3. **Nuevo método en repositorio:**
   - ✅ `findByCompanyIdentificationAndClientIdentification()`
   - Retorna el `CompanyClient` con el `Client` asociado
   - Permite detectar y actualizar clientes existentes

---

## 📊 **CASOS DE USO**

| Escenario | Acción del Backend | Resultado |
|-----------|-------------------|-----------|
| Cliente NO existe | CREAR nuevo | Nuevo registro en BD |
| Cliente existe + ACTIVO | ❌ LANZAR ERROR | `ModelNotFoundException` con mensaje |
| Cliente existe + INACTIVO | REACTIVAR + ACTUALIZAR | Mismo ID, `status=true`, datos actualizados |

---

## 🔧 **CONTACTOS**

**Lógica de procesamiento:**

```
Para cada contacto en el DTO:
  ├─ ¿Tiene idContact?
  │   └─ Buscar por ID → Actualizar
  │
  ├─ ¿No tiene idContact pero existe con mismo tipo+valor?
  │   └─ Buscar por tipo+valor → Actualizar (evita duplicado)
  │
  └─ ¿Es completamente nuevo?
      └─ Crear nuevo contacto
```

---

## 🚀 **FRONTEND (Simplificado)**

**Antes:**
```javascript
// 1. Validar si existe
const exists = await checkIfExists(identification);
if (exists) {
  showError("Cliente ya existe");
  return; // ❌ Bloqueado
}

// 2. Crear
await createClient(clientData);
```

**Ahora:**
```javascript
// Directamente guardar (sin validación previa)
const savedClient = await saveClient(clientData);
// Backend decide automáticamente: crear, actualizar o reactivar
// savedClient contiene el DTO completo con idClient, contacts, etc.
```

---

## 📝 **EJEMPLO PRÁCTICO**

### **Caso: Cliente eliminado que se intenta crear de nuevo**

```bash
# 1. Crear cliente
POST /api/client
{
  "identification": "1722633961001",
  "name": "JUAN PEREZ",
  "companyIdentification": "1721400271001",
  "contacts": [{"contactType": "TEL", "value": "0999999999"}]
}

# Response:
{
  "idClient": 1,
  "idType": 2,
  "identification": "1722633961001",
  "name": "JUAN PEREZ",
  "clientType": 1,
  "status": true,
  "companyIdentification": "1721400271001",
  "contacts": [
    {
      "idContact": 1,
      "contactType": "TEL",
      "value": "0999999999",
      "status": true
    }
  ]
}

# 2. Eliminar cliente (soft delete)
DELETE /api/client/1
# → Cliente ID=1 ahora tiene status=false

# 3. Intentar crear con la MISMA identificación
POST /api/client
{
  "identification": "1722633961001",
  "name": "JUAN PEREZ ACTUALIZADO",
  "companyIdentification": "1721400271001",
  "contacts": [{"contactType": "TEL", "value": "0988888888"}]
}

# Response (mismo ID, datos actualizados):
{
  "idClient": 1,  ← Mismo ID (NO duplicado)
  "idType": 2,
  "identification": "1722633961001",
  "name": "JUAN PEREZ ACTUALIZADO",  ← Actualizado
  "clientType": 1,
  "status": true,  ← Reactivado
  "companyIdentification": "1721400271001",
  "contacts": [
    {
      "idContact": 1,
      "contactType": "TEL",
      "value": "0988888888",  ← Actualizado
      "status": true
    }
  ]
}
# ✅ NO se creó registro duplicado
```

---

### **Escenario 4: Cliente activo → Error de duplicado**
```bash
# 1. Cliente ya existe y está activo
SELECT * FROM client WHERE identification = '1722633961001';
-- id_client | identification   | name       | status
-- 1         | 1722633961001    | JUAN PEREZ | true  ← ACTIVO

# 2. Intentar crear con la MISMA identificación
POST /api/client
{
  "identification": "1722633961001",
  "name": "OTRO NOMBRE",
  "companyIdentification": "1721400271001",
  "contacts": []
}

# Response (ERROR 409 Conflict):
{
  "code": 409,
  "status": "Conflict",
  "message": "Cliente ya existe con número de identificación 1722633961001"
}
```

---

## 🔍 **LOGS PARA DEBUGGING**

```
# Cliente nuevo
INFO: Cliente con identificación 1722633961001 no existe. Creando nuevo registro...
INFO: Cliente creado exitosamente. ID: 1

# Cliente existente activo
INFO: Cliente con identificación 1722633961001 ya existe para compañía 1721400271001. Actualizando datos...
INFO: Cliente actualizado exitosamente. ID: 1

# Cliente existente inactivo (reactivación)
INFO: Cliente con identificación 1722633961001 ya existe para compañía 1721400271001. Actualizando datos...
INFO: Cliente con ID 1 estaba inactivo. Reactivando...
INFO: Cliente reactivado exitosamente. ID: 1
```

---

## ✅ **VENTAJAS DE ESTE ENFOQUE**

1. **Menos requests HTTP:** 1 request en lugar de 2 (GET + POST)
2. **Más simple:** Frontend no necesita lógica condicional
3. **Evita duplicados:** Backend siempre valida antes de crear
4. **Reactivación automática:** Clientes eliminados se reactivan sin intervención manual
5. **Transaccional:** Todo o nada (cliente + contactos)
6. **Sin race conditions:** Validación y guardado en una sola transacción

---

## 📂 **ARCHIVOS MODIFICADOS**

| Archivo | Cambio |
|---------|--------|
| `ICompanyClientRepo.java` | ✅ Agregado método `findByCompanyIdentificationAndClientIdentification()` |
| `ClientServiceImpl.java` | ✅ Refactorizado `saveClientAndContact()` con lógica de reactivación |
| `ClientServiceImpl.java` | ✅ Agregado método helper `processClientContacts()` |
| `ClientController.java` | ❌ Eliminado endpoint `GET /api/client/exist/...` |
| `IClientService.java` | ❌ Eliminado método `existsByCompanyIdentificationAndClientIdentification()` |

---

**Fecha:** 2025-11-21  
**Versión:** ebill-customer v1.0

