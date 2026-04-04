# Fix: Error 500 al actualizar Cliente

## 📋 Problema Original

El frontend estaba enviando peticiones para actualizar clientes, pero el backend respondía con error 500:

```json
{
  "idClient": 2,
  "idType": 2,
  "clientType": 1,
  "identification": "1709055675",
  "name": "Ximena Hidalgo",
  "status": true,
  "companyIdentification": "1722633961001",
  "contacts": [...]
}
```

### Causas Raíz Identificadas:

1. **Campo `idCompany` innecesario**: Existía en `ClientDto` pero NO se usaba en ninguna parte
2. **Bug en `updateClientAndContact`**: Creaba un NUEVO cliente en lugar de actualizar el existente
3. **Lógica de contactos rota**: 
   - `ContactDto` NO tenía `idContact`
   - El código intentaba buscar por ID que siempre era null
   - No se actualizaban contactos existentes correctamente

---

## ✅ Soluciones Implementadas

### 1. Eliminado campo `idCompany` de `ClientDto`

**Archivo**: `src/main/java/com/wallparisoft/ebill/customer/dto/ClientDto.java`

```java
// ANTES
Long idCompany;          // ❌ Campo innecesario
String companyIdentification;

// DESPUÉS
String companyIdentification;  // ✅ Solo este campo se usa
```

**Beneficio**: Elimina confusión y el frontend no necesita enviar este campo.

---

### 2. Agregado `idContact` a `ContactDto`

**Archivo**: `src/main/java/com/wallparisoft/ebill/customer/dto/ContactDto.java`

```java
// ANTES
@Data
public class ContactDto {
  private String contactType;
  private String value;
  private Boolean status;
}

// DESPUÉS
@Data
public class ContactDto {
  private Long idContact;     // ✅ Nuevo campo
  private String contactType;
  private String value;
  private Boolean status;
}
```

**Beneficio**: Permite identificar contactos existentes para actualizarlos (opcional, se busca por tipo y valor si no se envía).

---

### 3. Corregido método `updateClientAndContact`

**Archivo**: `src/main/java/com/wallparisoft/ebill/customer/service/impl/ClientServiceImpl.java`

#### Cambios principales:

**a) Actualización del Cliente**
```java
// ANTES ❌
Client client = clientMapper.convertClientDtoToClient(clientDto);
client.setCreationDate(clientSave.getCreationDate());
save(client);

// DESPUÉS ✅
Client clientSave = findById(idClient);
clientSave.setIdType(clientDto.getIdType());
clientSave.setIdentification(clientDto.getIdentification());
clientSave.setName(clientDto.getName());
clientSave.setClientType(clientDto.getClientType());
clientSave.setStatus(clientDto.getStatus());
save(clientSave);
```

**b) Actualización de CompanyClient**
```java
// NUEVO ✅
Optional<CompanyClient> companyClientOptional =
    companyClientRepo.findByClient_IdClient(idClient);
if (companyClientOptional.isPresent()) {
  CompanyClient companyClient = companyClientOptional.get();
  if (!companyClient.getCompanyIdentification().equals(clientDto.getCompanyIdentification())) {
    companyClient.setCompanyIdentification(clientDto.getCompanyIdentification());
    companyClientRepo.save(companyClient);
  }
}
```

**c) Lógica Inteligente de Contactos**
```java
// Obtener TODOS los contactos del cliente (no solo activos)
List<Contact> existingContacts = contactRepo.findAllClientContactsByIdClient(idClient);

// Para cada contacto del frontend
contacts.forEach(newContact -> {
  Optional<Contact> existingContact = Optional.empty();

  // 1. Buscar por ID si está presente
  if (newContact.getIdContact() != null) {
    existingContact = existingContacts.stream()
        .filter(ec -> ec.getIdContact().equals(newContact.getIdContact()))
        .findFirst();
  }

  // 2. Si no, buscar por tipo y valor
  if (!existingContact.isPresent()) {
    existingContact = existingContacts.stream()
        .filter(ec -> 
            ec.getContactType().equals(newContact.getContactType()) &&
            ec.getValue().equals(newContact.getValue()))
        .findFirst();
  }

  // 3. Actualizar o crear
  if (existingContact.isPresent()) {
    // Actualizar contacto existente
    Contact contactToUpdate = existingContact.get();
    contactToUpdate.setValue(newContact.getValue());
    contactToUpdate.setContactType(newContact.getContactType());
    contactToUpdate.setStatus(newContact.getStatus());
    contactRepo.save(contactToUpdate);
    
    // Actualizar relación ClientContact
    // ...
  } else {
    // Crear nuevo contacto
    // ...
  }
});
```

---

### 4. Nuevo método en `IContactRepo`

**Archivo**: `src/main/java/com/wallparisoft/ebill/customer/repository/IContactRepo.java`

```java
@Query(
    value = "SELECT c FROM ClientContact cc " +
            "inner join cc.contact c " +
            "inner join cc.client cl " +
            "where cl.idClient = :idClient")
List<Contact> findAllClientContactsByIdClient(@Param("idClient") Long idClient);
```

**Beneficio**: Obtiene TODOS los contactos (activos e inactivos) para poder actualizarlos correctamente.

---

### 5. Configuración de Java 17

#### a) VSCode/Cursor Settings

**Archivo**: `.vscode/settings.json`

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home",
      "default": true
    }
  ],
  "spring-boot.ls.java.home": "/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"
}
```

#### b) Script ebill.sh

Configuración automática de Java 17 al inicio del script:

```bash
# Configurar Java 17 automáticamente
if [ -x "/usr/libexec/java_home" ]; then
    JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")
    if [ -n "$JAVA_17_HOME" ]; then
        export JAVA_HOME="$JAVA_17_HOME"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi
```

#### c) Archivos adicionales

- `.java-version`: Indica a herramientas como jEnv que usen Java 17
- `.mvn/jvm.config`: Configuración de JVM para Maven

---

## 🚀 Cómo Probar

### 1. Recargar el proyecto en IntelliJ/VSCode
```bash
# Si usas VSCode/Cursor, recarga la ventana
Cmd + Shift + P → "Developer: Reload Window"
```

### 2. Compilar el proyecto
```bash
./ebill.sh build

# O directamente con Maven
mvn clean install -DskipTests
```

### 3. Ejecutar la aplicación
```bash
./ebill.sh run

# O en IntelliJ, ejecutar EBillCustomerApplication
```

### 4. Probar actualización desde el frontend

El frontend ahora puede enviar:

**Opción 1: Sin idContact (se busca por tipo y valor)**
```json
{
  "idClient": 2,
  "idType": 2,
  "clientType": 1,
  "identification": "1709055675",
  "name": "Ximena Hidalgo",
  "status": true,
  "companyIdentification": "1722633961001",
  "contacts": [
    {
      "contactType": "TEL",
      "value": "022222222",
      "status": true
    }
  ]
}
```

**Opción 2: Con idContact (más preciso)**
```json
{
  "idClient": 2,
  "contacts": [
    {
      "idContact": 123,
      "contactType": "TEL",
      "value": "022222222",
      "status": true
    }
  ]
}
```

---

## 📊 Resultado Esperado

✅ **Status 200 OK** en lugar de 500  
✅ Cliente actualizado correctamente en la BD  
✅ Contactos actualizados sin duplicados  
✅ CompanyClient actualizado si cambió el RUC  
✅ Campos de auditoría (creation_date, update_date) preservados  

---

## 📁 Archivos Modificados

1. ✏️ `src/main/java/com/wallparisoft/ebill/customer/dto/ClientDto.java`
2. ✏️ `src/main/java/com/wallparisoft/ebill/customer/dto/ContactDto.java`
3. ✏️ `src/main/java/com/wallparisoft/ebill/customer/service/impl/ClientServiceImpl.java`
4. ✏️ `src/main/java/com/wallparisoft/ebill/customer/repository/IContactRepo.java`
5. ✏️ `.vscode/settings.json`
6. ✏️ `ebill.sh`
7. ➕ `.java-version` (nuevo)
8. ➕ `.mvn/jvm.config` (nuevo)
9. ➕ `FIX_CLIENT_UPDATE.md` (este archivo)

---

## 🔍 Logs de Depuración

Para verificar que todo funciona correctamente, revisa los logs:

```log
2025-10-15T14:39:45.094-05:00 DEBUG 19528 --- [nio-8180-exec-3] 
Event log: EventLog(
  service=com.wallparisoft.ebill.customer.controller.ClientController, 
  method=update, 
  information=ID client: 2, Object company: ClientDto(
    idClient=2, 
    idType=2, 
    identification=1709055675, 
    name=Ximena Hidalgo, 
    clientType=1, 
    status=true, 
    companyIdentification=1722633961001, 
    contacts=[...]
  )
)
```

✅ Ya NO aparece `idCompany=null`  
✅ Solo `companyIdentification` está presente

---

## 💡 Mejoras Futuras (Opcional)

1. **Agregar `idContact` en la respuesta del GET**: Para que el frontend pueda enviarlo en actualizaciones
2. **Desactivar contactos eliminados**: Si un contacto existía pero ya no viene en el request, marcarlo como `status=false`
3. **Validaciones**: Agregar `@Valid` y anotaciones como `@NotNull`, `@Size` en los DTOs
4. **Tests unitarios**: Crear tests para `updateClientAndContact`

---

## 📞 Soporte

Si encuentras algún problema:

1. Verifica Java 17: `java -version`
2. Limpia y recompila: `./ebill.sh clean && ./ebill.sh build`
3. Verifica los logs del backend para el stack trace completo
4. Revisa este documento para los cambios realizados

