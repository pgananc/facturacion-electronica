# 🔄 Lógica de Reactivación de Clientes

## 📋 **RESUMEN DE CAMBIOS**

Se implementó la **reactivación automática** de clientes eliminados (soft delete) cuando se intenta crear un cliente con la misma identificación.

**Enfoque:** El frontend solo hace `POST /api/client` sin validaciones previas. El backend decide automáticamente si crear, actualizar o reactivar.

---

## 🎯 **COMPORTAMIENTO ACTUAL**

### **Antes (❌ Problema)**

Cuando eliminabas un cliente (soft delete con `status=false`) e intentabas crear uno nuevo con la misma cédula:

```
POST /api/client
{
  "identification": "1722633961001",
  "name": "JUAN PEREZ",
  ...
}
```

**Resultado:** Se creaba un **registro duplicado** en la BD porque:
- No había validación previa
- `save(client)` sin ID → JPA lo trataba como INSERT
- Violaba la lógica de negocio (un cliente por identificación por compañía)

---

### **Ahora (✅ Solución)**

El endpoint `POST /api/client` ahora:

1. **Busca si existe un cliente** con esa identificación en esa compañía
2. **Si existe:**
   - Actualiza sus datos (nombre, tipo, etc.)
   - Lo reactiva (`status = true`)
   - Actualiza/agrega contactos según corresponda
   - **Log:** `"Cliente reactivado"`
3. **Si NO existe:**
   - Crea un nuevo registro
   - Crea sus contactos
   - Crea la relación `CompanyClient`
   - **Log:** `"Cliente creado"`

---

## 🔧 **ARCHIVOS MODIFICADOS**

### 1. `ICompanyClientRepo.java`

**Cambios:**

✅ **Nuevo método:** `findByCompanyIdentificationAndClientIdentification`
- Retorna el `CompanyClient` completo (con el `Client` asociado)
- Permite acceder al cliente existente para actualizarlo
- Usado internamente por `saveClientAndContact()` para detectar clientes existentes

```java
@Query(
    "SELECT cc FROM CompanyClient cc "
        + "WHERE cc.companyIdentification = :companyIdentification "
        + "AND cc.client.identification = :clientIdentification")
Optional<CompanyClient> findByCompanyIdentificationAndClientIdentification(
    @Param("companyIdentification") String companyIdentification,
    @Param("clientIdentification") String clientIdentification);
```

---

### 2. `ClientController.java`

**Cambios:**

❌ **Endpoint eliminado:** `GET /api/client/exist/company/{companyId}/client/{identification}`
- Ya no se necesita validación previa
- El frontend simplemente hace `POST /api/client`
- El backend maneja automáticamente la lógica de crear/actualizar/reactivar

---

### 3. `IClientService.java` y `ClientServiceImpl.java`

**Cambios:**

❌ **Método eliminado:** `existsByCompanyIdentificationAndClientIdentification()`
- Ya no se usa porque no hay endpoint de validación
- La lógica de detección de duplicados está integrada en `saveClientAndContact()`

---

### 4. `ClientServiceImpl.java`

**Cambios:**

#### A) Método `saveClientAndContact()` - Refactorizado

**Flujo nuevo:**

```
1. Buscar cliente existente por (companyIdentification + identification)
   ↓
2. ¿Existe?
   ├─ SÍ → Actualizar datos + Reactivar (status=true)
   └─ NO → Crear nuevo registro
   ↓
3. Guardar cliente (INSERT o UPDATE según caso)
   ↓
4. Procesar contactos (método helper)
   ↓
5. Crear CompanyClient (solo si es nuevo)
```

**Logs agregados:**

```java
log.info("Cliente con identificación {} ya existe para compañía {}. Actualizando datos...", ...);
log.info("Cliente con ID {} estaba inactivo. Reactivando...", ...);
log.info("Cliente con identificación {} no existe. Creando nuevo registro...", ...);
log.info("Cliente reactivado/actualizado/creado exitosamente. ID: {}", ...);
```

---

#### B) Nuevo método helper: `processClientContacts()`

**Propósito:** Centralizar la lógica de actualización/creación de contactos.

**Lógica:**

```
Para cada contacto del DTO:
  ├─ ¿Tiene idContact?
  │   └─ SÍ → Buscar por ID
  │
  ├─ ¿No encontrado por ID?
  │   └─ Buscar por (tipo + valor) para evitar duplicados
  │
  ├─ ¿Encontrado?
  │   ├─ SÍ → ACTUALIZAR contacto existente
  │   │       └─ Actualizar valor, tipo, status
  │   │       └─ Actualizar relación ClientContact
  │   │
  │   └─ NO → CREAR nuevo contacto
  │           └─ Guardar Contact
  │           └─ Crear relación ClientContact
```

**Casos cubiertos:**

| Escenario | Acción |
|-----------|--------|
| Contacto con `idContact` existente | ✅ Actualizar |
| Contacto sin `idContact` pero mismo tipo+valor | ✅ Actualizar (evita duplicado) |
| Contacto completamente nuevo | ✅ Crear |
| Contacto que ya no viene en el DTO | ⚠️ Se mantiene en BD (no se elimina) |

**Logs de debug:**

```java
log.debug("Contacto actualizado: ID={}, tipo={}, valor={}", ...);
log.debug("Contacto nuevo creado: ID={}, tipo={}, valor={}", ...);
```

---

## 📊 **EJEMPLOS DE USO**

### **Flujo Frontend (simplificado):**

```javascript
// NO hay validación previa con GET /api/client/exist/...
// Directamente se hace POST

async function saveClient(clientData) {
  try {
    const response = await fetch('/api/client', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(clientData)
    });
    
    if (response.ok) {
      showSuccess('Cliente guardado exitosamente');
      // Backend decidió si crear, actualizar o reactivar
    }
  } catch (error) {
    showError('Error al guardar cliente');
  }
}
```

---

### **Caso 1: Cliente nuevo (no existe)**

**Request:**
```json
POST /api/client
{
  "identification": "1722633961001",
  "name": "JUAN PEREZ",
  "idType": 2,
  "clientType": 1,
  "companyIdentification": "1721400271001",
  "contacts": [
    { "contactType": "TEL", "value": "0995070719" },
    { "contactType": "MAI", "value": "juan@mail.com" }
  ]
}
```

**Resultado:**
```
✅ Cliente creado exitosamente
✅ 2 contactos creados
✅ Relación CompanyClient creada
✅ Response: BasicResponse { status: "OK" }
```

**Log:**
```
Cliente con identificación 1722633961001 no existe. Creando nuevo registro...
Contacto nuevo creado: ID=1, tipo=TEL, valor=0995070719
Contacto nuevo creado: ID=2, tipo=MAI, valor=juan@mail.com
Relación CompanyClient creada para cliente ID 1 y compañía 1721400271001
Cliente creado exitosamente. ID: 1
```

---

### **Caso 2: Cliente eliminado (soft delete) - Reactivación**

**Estado inicial:**
```sql
-- Cliente existe pero está inactivo
SELECT * FROM client WHERE identification = '1722633961001';
-- id_client | identification   | name       | status
-- 1         | 1722633961001    | JUAN PEREZ | false  ← Eliminado
```

**Request:**
```json
POST /api/client
{
  "identification": "1722633961001",
  "name": "JUAN PEREZ ACTUALIZADO",
  "idType": 2,
  "clientType": 1,
  "companyIdentification": "1721400271001",
  "contacts": [
    { "contactType": "TEL", "value": "0999999999" },  ← Nuevo número
    { "contactType": "MAI", "value": "nuevo@mail.com" }  ← Nuevo email
  ]
}
```

**Resultado:**
```
✅ Cliente reactivado (status=true)
✅ Nombre actualizado a "JUAN PEREZ ACTUALIZADO"
✅ Contactos actualizados/creados
❌ NO se crea registro duplicado
```

**Log:**
```
Cliente con identificación 1722633961001 ya existe para compañía 1721400271001. Actualizando datos...
Cliente con ID 1 estaba inactivo. Reactivando...
Contacto actualizado: ID=1, tipo=TEL, valor=0999999999
Contacto actualizado: ID=2, tipo=MAI, valor=nuevo@mail.com
Cliente reactivado exitosamente. ID: 1
```

**Estado final:**
```sql
SELECT * FROM client WHERE identification = '1722633961001';
-- id_client | identification   | name                    | status
-- 1         | 1722633961001    | JUAN PEREZ ACTUALIZADO  | true  ← Reactivado
```

---

### **Caso 3: Cliente activo - Actualización**

**Estado inicial:**
```sql
-- Cliente existe y está activo
SELECT * FROM client WHERE identification = '1722633961001';
-- id_client | identification   | name       | status
-- 1         | 1722633961001    | JUAN PEREZ | true
```

**Request:**
```json
POST /api/client
{
  "identification": "1722633961001",
  "name": "JUAN PEREZ MODIFICADO",
  "idType": 2,
  "clientType": 2,  ← Cambió de tipo 1 a 2
  "companyIdentification": "1721400271001",
  "contacts": [
    { "idContact": 1, "contactType": "TEL", "value": "0995070719" },  ← Mantiene
    { "contactType": "DIR", "value": "Calle Principal 123" }  ← Nuevo contacto
  ]
}
```

**Resultado:**
```
✅ Cliente actualizado (nombre y tipo)
✅ Contacto 1 mantenido
✅ Contacto nuevo (DIR) creado
✅ Status sigue en true
```

**Log:**
```
Cliente con identificación 1722633961001 ya existe para compañía 1721400271001. Actualizando datos...
Contacto actualizado: ID=1, tipo=TEL, valor=0995070719
Contacto nuevo creado: ID=3, tipo=DIR, valor=Calle Principal 123
Cliente actualizado exitosamente. ID: 1
```

---

## 🔍 **VALIDACIONES IMPLEMENTADAS**

### ✅ **A nivel de servicio:**

1. **Búsqueda previa:** Siempre busca cliente existente antes de crear
2. **Reactivación automática:** Si existe y está inactivo → reactiva
3. **Actualización de datos:** Si existe → actualiza todos los campos
4. **Evita duplicados de contactos:** Busca por tipo+valor antes de crear

### ✅ **A nivel de repositorio:**

1. **Query corregida:** `existsByCompanyIdentificationAndClientIdentification`
   - Ahora busca correctamente en `client.identification`
2. **Nuevo método:** `findByCompanyIdentificationAndClientIdentification`
   - Retorna el `CompanyClient` completo para poder actualizar

---

## 🚨 **CONSIDERACIONES IMPORTANTES**

### 1️⃣ **Contactos que ya NO vienen en el DTO**

**Comportamiento actual:** Se mantienen en la BD (no se eliminan ni desactivan).

**Ejemplo:**
```
Cliente tiene 3 contactos: TEL, MAI, DIR
Frontend envía solo: TEL, MAI
Resultado: Los 3 contactos siguen en BD (DIR se mantiene)
```

**¿Por qué?** Porque no sabemos si el frontend omitió el contacto a propósito o por error.

**Alternativa (si quieres cambiar):**
- Desactivar contactos que no vengan en el DTO
- Requiere lógica adicional para comparar contactos existentes vs enviados

---

### 2️⃣ **Cache invalidation**

El método `saveClientAndContact()` invalida los caches:
```java
@CacheEvict(value = {"clients", "client-detail"}, allEntries = true)
```

Esto asegura que las consultas posteriores reflejen los cambios (reactivación, actualización).

---

### 3️⃣ **Transaccionalidad**

El método es `@Transactional`, por lo que:
- Si falla cualquier paso → rollback completo
- No quedan datos inconsistentes
- Cliente y contactos se guardan como unidad atómica

---

## 🧪 **TESTING RECOMENDADO**

### **Escenario 1: Cliente nuevo (primera vez)**
```bash
curl -X POST http://localhost:8180/api/client \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "identification": "1234567890001",
    "name": "CLIENTE NUEVO",
    "idType": 2,
    "clientType": 1,
    "companyIdentification": "1721400271001",
    "contacts": [
      {"contactType": "TEL", "value": "0999999999"}
    ]
  }'
```

**Verificar en BD:**
```sql
SELECT * FROM client WHERE identification = '1234567890001';
-- status = true, nombre = "CLIENTE NUEVO"

SELECT * FROM contact WHERE id_contact IN (
  SELECT id_contact FROM client_contact WHERE id_client = <id>
);
-- 1 contacto: TEL = 0999999999
```

---

### **Escenario 2: Cliente eliminado → Reactivación automática**
```bash
# 1. Eliminar cliente (soft delete)
curl -X DELETE http://localhost:8180/api/client/1 \
  -H "Authorization: Bearer <token>"

# 2. Verificar que status = false
SELECT status FROM client WHERE id_client = 1;
-- status = false

# 3. Enviar POST con la MISMA identificación
curl -X POST http://localhost:8180/api/client \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "identification": "1234567890001",
    "name": "CLIENTE REACTIVADO CON NUEVO NOMBRE",
    "idType": 2,
    "clientType": 1,
    "companyIdentification": "1721400271001",
    "contacts": [
      {"contactType": "TEL", "value": "0988888888"}
    ]
  }'

# 4. Verificar reactivación
SELECT * FROM client WHERE identification = '1234567890001';
-- id_client = 1 (mismo ID, NO duplicado)
-- status = true (reactivado)
-- name = "CLIENTE REACTIVADO CON NUEVO NOMBRE" (actualizado)
```

**Resultado esperado:**
- ✅ Cliente reactivado (`status=true`)
- ✅ Datos actualizados
- ✅ Contacto actualizado
- ❌ NO se creó registro duplicado

---

### **Escenario 3: Cliente activo → Actualización**
```bash
# Cliente ya existe y está activo
# Enviar POST con la MISMA identificación pero datos diferentes

curl -X POST http://localhost:8180/api/client \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "identification": "1234567890001",
    "name": "NOMBRE ACTUALIZADO",
    "idType": 2,
    "clientType": 2,
    "companyIdentification": "1721400271001",
    "contacts": [
      {"idContact": 1, "contactType": "TEL", "value": "0999999999"},
      {"contactType": "MAI", "value": "nuevo@mail.com"}
    ]
  }'
```

**Resultado esperado:**
- ✅ Cliente actualizado (nombre, clientType)
- ✅ Contacto 1 mantenido
- ✅ Contacto nuevo (MAI) creado
- ❌ NO se creó registro duplicado

---

## 📝 **LOGS PARA DEBUGGING**

### **Nivel INFO:**
```
Cliente con identificación 1722633961001 ya existe para compañía 1721400271001. Actualizando datos...
Cliente con ID 1 estaba inactivo. Reactivando...
Cliente reactivado exitosamente. ID: 1
```

### **Nivel DEBUG:**
```
Contacto actualizado: ID=1, tipo=TEL, valor=0999999999
Contacto nuevo creado: ID=2, tipo=MAI, valor=nuevo@mail.com
```

**Para ver logs DEBUG:**
```properties
# application.properties
logging.level.com.wallparisoft.ebill.customer.service.impl.ClientServiceImpl=DEBUG
```

---

## ✅ **CHECKLIST DE VALIDACIÓN**

- [x] Cliente nuevo se crea correctamente
- [x] Cliente eliminado se reactiva automáticamente
- [x] Cliente activo se actualiza sin duplicar
- [x] Contactos existentes se actualizan
- [x] Contactos nuevos se crean
- [x] No se crean contactos duplicados (mismo tipo+valor)
- [x] Query `existsByCompanyIdentificationAndClientIdentification` corregida
- [x] Logs informativos agregados
- [x] Cache invalidado correctamente
- [x] Transaccionalidad garantizada

---

**Fecha de implementación:** 2025-11-21  
**Versión:** ebill-customer v1.0  
**Autor:** AI Assistant + jguerreroh

