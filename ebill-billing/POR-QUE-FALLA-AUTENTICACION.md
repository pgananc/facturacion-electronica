# ¿Por qué falla si el ID 6 está correcto en el pom.xml?

## El Problema Real

El error **NO es por el ID del proyecto** (el 6 está correcto). El problema es que **falta la autenticación**.

### Error que estás viendo:

```
authentication failed for https://gitlab.com/api/v4/projects/9/packages/maven
status: 401 Unauthorized
```

### Lo que tienes en pom.xml (CORRECTO):

```xml
<repository>
    <id>gitlab-ebill-utils</id>
    <url>http://85.239.237.88/api/v4/projects/6/packages/maven</url>
</repository>
```

## ¿Por qué falla entonces?

### 1. Falta el archivo `settings.xml` con credenciales

Maven necesita **dos cosas** para acceder a repositorios privados:

1. ✅ **La URL del repositorio** (ya la tienes en `pom.xml`)
2. ❌ **Las credenciales de autenticación** (falta en `~/.m2/settings.xml`)

### 2. El error muestra una URL diferente

El error muestra:

- `https://gitlab.com/api/v4/projects/9/packages/maven`

Pero tu `pom.xml` tiene:

- `http://85.239.237.88/api/v4/projects/6/packages/maven`

**Posibles causas:**

1. **Redirección HTTP → HTTPS**: GitLab puede estar redirigiendo `http://` a `https://`
2. **Dependencia transitiva**: Alguna dependencia puede estar intentando acceder a otro repositorio
3. **Cache de Maven**: Maven puede estar usando una URL cacheada
4. **Configuración de proxy/DNS**: Puede haber una redirección a nivel de red

### 3. El ID del servidor debe coincidir

En `settings.xml`, el `<id>` del servidor **DEBE coincidir exactamente** con el `<id>` del repositorio en `pom.xml`:

**pom.xml:**

```xml
<repository>
    <id>gitlab-ebill-utils</id>  ← Este ID
    <url>http://85.239.237.88/api/v4/projects/6/packages/maven</url>
</repository>
```

**settings.xml (debe tener el mismo ID):**

```xml
<server>
    <id>gitlab-ebill-utils</id>  ← Debe ser EXACTAMENTE igual
    <configuration>
        <httpHeaders>
            <property>
                <name>Private-Token</name>
                <value>TU_TOKEN_AQUI</value>
            </property>
        </httpHeaders>
    </configuration>
</server>
```

## Solución Paso a Paso

### Paso 1: Verificar si existe `settings.xml`

**Windows:**

```powershell
Test-Path $env:USERPROFILE\.m2\settings.xml
```

**Linux/Mac:**

```bash
ls -la ~/.m2/settings.xml
```

Si no existe, necesitas crearlo.

### Paso 2: Crear `settings.xml` con las credenciales

**Ubicación:**

- Windows: `C:\Users\<TuUsuario>\.m2\settings.xml`
- Linux/Mac: `~/.m2/settings.xml`

**Contenido:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <!-- IMPORTANTE: El ID debe coincidir con el del pom.xml -->
    <server>
      <id>gitlab-ebill-utils</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>TU_TOKEN_DE_GITLAB_AQUI</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>

    <server>
      <id>gitlab-ebill-auth-util</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>TU_TOKEN_DE_GITLAB_AQUI</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>

</settings>
```

### Paso 3: Obtener el Token de GitLab

1. Ir a: `http://85.239.237.88` (o `http://gitlab.wallparisoft.com`)
2. **Usuario** (esquina superior derecha) > **Preferences** > **Access Tokens**
3. Crear token con scopes: `api` y `read_repository`
4. Copiar el token y reemplazar `TU_TOKEN_DE_GITLAB_AQUI` en `settings.xml`

### Paso 4: Limpiar cache de Maven (opcional pero recomendado)

```bash
mvn dependency:purge-local-repository
# O simplemente eliminar la carpeta .m2/repository si quieres empezar desde cero
```

### Paso 5: Intentar compilar de nuevo

```bash
mvn clean compile -DskipTests
```

## Verificación: ¿Por qué el error muestra proyecto 9?

Si el error muestra `projects/9` pero tu `pom.xml` tiene `projects/6`, puede ser:

### Opción 1: Dependencia transitiva

Alguna dependencia puede estar intentando acceder a otro repositorio. Para verificar:

```bash
mvn dependency:tree
```

Esto mostrará todas las dependencias y sus repositorios.

### Opción 2: Redirección de GitLab

GitLab puede estar redirigiendo o transformando la URL. Para verificar:

1. Intentar acceder directamente a la URL:

   ```bash
   curl -I http://85.239.237.88/api/v4/projects/6/packages/maven
   ```

2. Ver si hay redirecciones:
   ```bash
   curl -L -v http://85.239.237.88/api/v4/projects/6/packages/maven
   ```

### Opción 3: Configuración de proxy o DNS

Si hay un proxy o configuración de DNS que redirige `gitlab.wallparisoft.com` a `gitlab.com`, puede causar este problema.

## Resumen: ¿Por qué falla?

| Aspecto                 | Estado       | Explicación                                        |
| ----------------------- | ------------ | -------------------------------------------------- |
| **ID del proyecto (6)** | ✅ Correcto  | El ID 6 está bien en el `pom.xml`                  |
| **URL del repositorio** | ✅ Correcto  | La URL está bien configurada                       |
| **Autenticación**       | ❌ **FALTA** | No hay `settings.xml` con token                    |
| **ID del servidor**     | ❌ **FALTA** | No hay configuración de servidor en `settings.xml` |

## La Solución en una Línea

**El problema NO es el ID 6. El problema es que falta crear `~/.m2/settings.xml` con tu token de GitLab.**

Una vez que crees el `settings.xml` con las credenciales correctas, Maven podrá autenticarse y descargar las dependencias, independientemente de si la URL se redirige o no.

## Verificación Final

Después de configurar `settings.xml`, verifica que funciona:

```bash
# Limpiar cache
mvn clean

# Compilar (esto debería descargar las dependencias)
mvn compile -DskipTests

# Si funciona, verás algo como:
# Downloading from gitlab-ebill-utils: http://85.239.237.88/api/v4/projects/6/packages/maven/...
# Downloaded from gitlab-ebill-utils: http://85.239.237.88/api/v4/projects/6/packages/maven/...
```

Si aún falla después de configurar `settings.xml`, entonces el problema puede ser:

- Token inválido o sin permisos
- URL incorrecta (aunque parece correcta)
- Problemas de red/firewall
- El proyecto 9 puede ser una dependencia transitiva diferente

