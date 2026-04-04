# 📋 Origen del Campo `environment` en Notas de Débito

## 🔍 Pregunta

**¿De dónde se toma el campo `environment` para la nota de débito?**

## ✅ Respuesta

El campo `environment` **NO se envía desde el frontend**. Se obtiene automáticamente desde la **configuración del backend**.

### 📍 Origen del Valor

1. **Configuración en `application.properties`:**
   ```properties
   # Ambiente: 1=PRUEBAS, 2=PRODUCCION
   sri.environment=1
   ```

2. **Clase de Configuración:**
   - **Clase:** `com.wallparisoft.ebillbilling.config.SriConfiguration`
   - **Método:** `getEnvironmentName()`
   - **Retorna:** `"PRUEBAS"` o `"PRODUCCION"` según el valor configurado

3. **Asignación Automática:**
   El campo se asigna automáticamente en `DebitNoteServiceImpl` cuando se crea la nota de débito:
   ```java
   // En saveDebitNote(), saveDebitNoteDraft(), updateDebitNote()
   String environmentName = sriConfiguration.getEnvironmentName();
   debitNote.setEnvironment(environmentName);
   debitNoteDto.setEnvironment(environmentName);
   ```

### 🔄 Flujo Completo

```
application.properties
    ↓
sri.environment=1 (o 2)
    ↓
SriConfiguration.getEnvironmentName()
    ↓
"PRUEBAS" o "PRODUCCION"
    ↓
DebitNoteServiceImpl (al crear/actualizar)
    ↓
debitNote.setEnvironment(environmentName)
    ↓
Guardado en BD (tabla debit_note.environment)
```

### 📝 Código Relevante

**1. Configuración (`SriConfiguration.java`):**
```java
@Value("${sri.environment:1}")
private String environment;

public String getEnvironmentName() {
  return isPruebas() ? "PRUEBAS" : "PRODUCCION";
}
```

**2. Asignación en Servicio (`DebitNoteServiceImpl.java`):**
```java
// En saveDebitNote()
String environmentName = sriConfiguration.getEnvironmentName();
debitNote.setEnvironment(environmentName);
debitNoteDto.setEnvironment(environmentName);
log.debug("📝 [ND] Environment asignado desde configuración: {}", environmentName);
```

### ⚠️ Importante

- **El frontend NO debe enviar `environment`** en el request
- **El backend siempre asigna** el valor desde `SriConfiguration`
- **Si el frontend envía `environment`**, se ignora y se sobrescribe con el valor de configuración
- **El valor se guarda en BD** para registro histórico (saber con qué ambiente se creó la nota)

### 🎯 Valores Posibles

| Valor en `application.properties` | Valor en BD | Descripción |
|-----------------------------------|-------------|-------------|
| `1` | `"PRUEBAS"` | Ambiente de pruebas del SRI |
| `2` | `"PRODUCCION"` | Ambiente de producción del SRI |

### 📊 Ubicación en Base de Datos

```sql
-- Tabla: debit_note
-- Columna: environment
-- Tipo: VARCHAR
-- Valores: "PRUEBAS" o "PRODUCCION"
```

### 🔧 Cambiar el Ambiente

Para cambiar el ambiente, editar `application.properties`:

```properties
# Para pruebas
sri.environment=1

# Para producción (⚠️ CUIDADO)
sri.environment=2
```

**⚠️ ADVERTENCIA:** Cambiar a `2` (PRODUCCION) hará que todas las notas de débito se envíen al SRI real. Solo cambiar en producción.

### ✅ Conclusión

El campo `environment` se obtiene de:
1. **Configuración:** `application.properties` → `sri.environment`
2. **Clase:** `SriConfiguration.getEnvironmentName()`
3. **Asignación:** Automática en `DebitNoteServiceImpl` al crear/actualizar

**El frontend NO necesita enviar este campo.**






