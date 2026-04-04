# Configuración de Credenciales GitLab Maven

## Problema

Al compilar el proyecto, Maven no puede descargar las dependencias privadas de GitLab porque falta la autenticación:

```
authentication failed for https://gitlab.com/api/v4/projects/9/packages/maven
status: 401 Unauthorized
```

## Solución: Configurar Credenciales en settings.xml

### Opción 1: Configuración Global (Recomendada)

Crear o editar el archivo `~/.m2/settings.xml` (en Windows: `C:\Users\<TuUsuario>\.m2\settings.xml`)

**Ubicación del archivo:**

- **Windows**: `C:\Users\<TuUsuario>\.m2\settings.xml`
- **Linux/Mac**: `~/.m2/settings.xml`

**Contenido del archivo:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <!-- ebill-utils repository -->
    <server>
      <id>gitlab-ebill-utils</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>YOUR_GITLAB_TOKEN</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>

    <!-- ebill-auth-util repository -->
    <server>
      <id>gitlab-ebill-auth-util</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>YOUR_GITLAB_TOKEN</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>

    <!-- Si el error menciona gitlab.com (proyecto 9), agregar también: -->
    <server>
      <id>gitlab-maven</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>YOUR_GITLAB_TOKEN</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>

</settings>
```

### Opción 2: Usar el archivo de ejemplo del proyecto

El proyecto incluye un archivo de ejemplo: `.m2-settings.xml.example`

**Pasos:**

1. Copiar el archivo de ejemplo:

   ```bash
   # Windows (PowerShell)
   Copy-Item .m2-settings.xml.example $env:USERPROFILE\.m2\settings.xml

   # Linux/Mac
   cp .m2-settings.xml.example ~/.m2/settings.xml
   ```

2. Editar el archivo y reemplazar `YOUR_GITLAB_TOKEN` con tu token real.

### Opción 3: Configuración en el pom.xml (No recomendada)

Si prefieres configurar las credenciales directamente en el `pom.xml` (menos seguro):

**Archivo**: `pom.xml`

Agregar dentro de `<project>`:

```xml
<distributionManagement>
  <repository>
    <id>gitlab-ebill-utils</id>
    <url>http://gitlab.wallparisoft.com/api/v4/projects/6/packages/maven</url>
  </repository>
  <repository>
    <id>gitlab-ebill-auth-util</id>
    <url>http://gitlab.wallparisoft.com/api/v4/projects/10/packages/maven</url>
  </repository>
</distributionManagement>
```

Y luego configurar las credenciales en `settings.xml` como en la Opción 1.

## Obtener un Token de GitLab

### Método 1: Personal Access Token

1. Acceder a GitLab: `https://85.239.237.88` (o la URL de tu instancia)
2. Ir a: **Usuario (esquina superior derecha) > Preferences > Access Tokens**
3. Crear un nuevo token con:
   - **Name**: `Maven Access Token` (o el nombre que prefieras)
   - **Scopes**: Seleccionar `api` y `read_repository`
   - **Expiration date**: (opcional) Fecha de expiración
4. Hacer clic en **Create personal access token**
5. **Copiar el token inmediatamente** (solo se muestra una vez)

### Método 2: Project Access Token (Alternativa)

1. Ir al proyecto en GitLab
2. **Settings > Access Tokens**
3. Crear token con permisos `read_api` y `read_repository`
4. Copiar el token

## Verificar la Configuración

### Verificar que el archivo existe:

**Windows:**

```powershell
Test-Path $env:USERPROFILE\.m2\settings.xml
```

**Linux/Mac:**

```bash
ls -la ~/.m2/settings.xml
```

### Probar la compilación:

```bash
mvn clean compile -DskipTests
```

Si la configuración es correcta, Maven debería poder descargar las dependencias.

## Notas Importantes

1. **Seguridad**:

   - ❌ **NO** agregues `settings.xml` al control de versiones (ya está en `.gitignore`)
   - ❌ **NO** compartas tu token personal
   - ✅ El archivo `settings.xml` es personal y debe estar solo en tu máquina

2. **IDs de Servidor**:

   - Los `<id>` en `settings.xml` deben coincidir exactamente con los `<id>` en `pom.xml`
   - En el `pom.xml` están definidos como:
     - `gitlab-ebill-utils`
     - `gitlab-ebill-auth-util`

3. **URLs de Repositorio**:

   - El `pom.xml` usa: `http://gitlab.wallparisoft.com/api/v4/projects/`
   - Si el error menciona `https://gitlab.com`, puede ser que haya una redirección o configuración diferente
   - Verificar la URL correcta con tu administrador de GitLab

4. **Alternativa con Usuario/Contraseña**:
   Si el token no funciona, puedes usar autenticación básica:

   ```xml
   <server>
     <id>gitlab-ebill-utils</id>
     <username>tu_usuario</username>
     <password>tu_contraseña</password>
   </server>
   ```

   ⚠️ **No recomendado** por seguridad.

## Solución de Problemas

### Error: "401 Unauthorized"

**Causas posibles:**

- Token inválido o expirado
- Token sin los permisos correctos (necesita `api` y `read_repository`)
- ID del servidor no coincide con el del `pom.xml`
- URL del repositorio incorrecta

**Solución:**

1. Verificar que el token tenga los scopes correctos
2. Verificar que los IDs coincidan exactamente
3. Regenerar el token si es necesario

### Error: "Could not transfer artifact"

**Causas posibles:**

- Problemas de red/firewall
- URL del repositorio incorrecta
- Repositorio no existe o no tienes acceso

**Solución:**

1. Verificar conectividad: `ping gitlab.wallparisoft.com`
2. Verificar acceso al proyecto en GitLab
3. Contactar al administrador si el proyecto no existe

### Error: "settings.xml not found"

**Solución:**

- Crear el directorio si no existe: `mkdir ~/.m2` (Linux/Mac) o `mkdir $env:USERPROFILE\.m2` (Windows)
- Crear el archivo `settings.xml` en ese directorio

## Ejemplo Completo de settings.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <!-- ebill-utils repository -->
    <server>
      <id>gitlab-ebill-utils</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>glpat-xxxxxxxxxxxxxxxxxxxx</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>

    <!-- ebill-auth-util repository -->
    <server>
      <id>gitlab-ebill-auth-util</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>glpat-xxxxxxxxxxxxxxxxxxxx</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>

  <!-- Opcional: Configurar mirror para Maven Central -->
  <mirrors>
    <mirror>
      <id>maven-central</id>
      <name>Maven Central Mirror</name>
      <url>https://repo.maven.apache.org/maven2</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>

</settings>
```

## Referencias

- Archivo de ejemplo del proyecto: `.m2-settings.xml.example`
- Documentación Maven Settings: https://maven.apache.org/settings.html
- Documentación GitLab Package Registry: https://docs.gitlab.com/ee/user/packages/maven_repository/

