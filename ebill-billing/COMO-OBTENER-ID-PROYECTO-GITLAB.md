# Cómo Obtener el ID del Proyecto en GitLab

## ¿Qué es el ID del Proyecto?

El número en la URL `http://85.239.237.88/api/v4/projects/6/packages/maven` es el **ID numérico del proyecto** en GitLab. Este ID es único para cada proyecto.

En tu `pom.xml`:

- **Proyecto 6**: `ebill-utils` (repositorio de utilidades)
- **Proyecto 10**: `ebill-auth-util` (repositorio de autenticación)

## Métodos para Obtener el ID del Proyecto

### Método 1: Desde la Página del Proyecto (Más Fácil)

1. **Acceder a GitLab**: `http://85.239.237.88` (o la URL de tu instancia)
2. **Navegar al proyecto** que contiene el paquete Maven
3. **El ID aparece en la página principal del proyecto**:
   - En la parte superior, debajo del nombre del proyecto
   - Formato: `Project ID: 6` o simplemente `#6`
   - También puede aparecer en la URL: `http://85.239.237.88/group/project-name` (el ID está en el HTML)

### Método 2: Desde la URL del Proyecto

1. **Ir a la página principal del proyecto** en GitLab
2. **Ver la URL del navegador**:
   ```
   http://85.239.237.88/group-name/project-name
   ```
3. **Hacer clic en "Settings" > "General"**:
   - El ID del proyecto aparece en la sección "Project ID"
   - Ejemplo: `Project ID: 6`

### Método 3: Usando la API de GitLab

**Con token de acceso:**

```bash
# Obtener información de un proyecto por su path
curl --header "PRIVATE-TOKEN: YOUR_TOKEN" \
  "http://85.239.237.88/api/v4/projects/group%2Fproject-name"

# O listar todos tus proyectos
curl --header "PRIVATE-TOKEN: YOUR_TOKEN" \
  "http://85.239.237.88/api/v4/projects?owned=true"
```

La respuesta JSON incluirá el campo `"id"`:

```json
{
  "id": 6,
  "name": "ebill-utils",
  "path": "ebill-utils",
  "path_with_namespace": "group/ebill-utils",
  ...
}
```

### Método 4: Desde el Código Fuente del Proyecto

Si tienes acceso al repositorio del proyecto, el ID puede estar en:

1. **Archivo `.gitlab-ci.yml`** del proyecto
2. **Documentación** del proyecto
3. **README.md** del proyecto

### Método 5: Inspeccionar el HTML de la Página

1. **Abrir la página del proyecto** en GitLab
2. **Inspeccionar el código fuente** (F12 > Elements/Inspeccionar)
3. **Buscar** `data-project-id` o `project_id` en el HTML
4. El valor será el ID del proyecto

### Método 6: Desde la Página de Packages

1. **Ir al proyecto** en GitLab
2. **Navegar a**: `Packages & Registries > Package Registry`
3. **Ver la URL** de algún paquete Maven
4. La URL tendrá el formato: `.../projects/6/packages/maven/...`
5. El número `6` es el ID del proyecto

## Verificar el ID Correcto

### Para ebill-utils (Proyecto 6):

1. Ir a: `http://85.239.237.88` (o tu URL de GitLab)
2. Buscar el proyecto `ebill-utils`
3. Verificar el ID en Settings > General
4. Debe ser: `6`

### Para ebill-auth-util (Proyecto 10):

1. Ir a: `http://85.239.237.88` (o tu URL de GitLab)
2. Buscar el proyecto `ebill-auth-util`
3. Verificar el ID en Settings > General
4. Debe ser: `10`

## Si el ID es Diferente

Si los IDs en tu instancia de GitLab son diferentes (por ejemplo, 15 y 20 en lugar de 6 y 10):

1. **Obtener los IDs correctos** usando los métodos anteriores
2. **Actualizar el `pom.xml`** con los IDs correctos:

```xml
<repositories>
    <repository>
        <id>gitlab-ebill-utils</id>
        <url>http://85.239.237.88/api/v4/projects/15/packages/maven</url>
    </repository>
    <repository>
        <id>gitlab-ebill-auth-util</id>
        <url>http://85.239.237.88/api/v4/projects/20/packages/maven</url>
    </repository>
</repositories>
```

3. **Los IDs en `settings.xml` NO cambian**, solo los IDs en `pom.xml` (las URLs)

## Ejemplo Práctico

### Paso 1: Encontrar el Proyecto ebill-utils

1. Acceder a GitLab: `http://85.239.237.88`
2. Buscar proyecto: `ebill-utils`
3. Ir a: **Settings > General**
4. Ver: **Project ID: 6**

### Paso 2: Verificar la URL del Package Registry

1. En el mismo proyecto, ir a: **Packages & Registries > Package Registry**
2. Si hay paquetes Maven, la URL será algo como:
   ```
   http://85.239.237.88/group/ebill-utils/-/packages/123
   ```
3. O en la API:
   ```
   http://85.239.237.88/api/v4/projects/6/packages
   ```

### Paso 3: Confirmar en pom.xml

El `pom.xml` debe tener:

```xml
<repository>
    <id>gitlab-ebill-utils</id>
    <url>http://85.239.237.88/api/v4/projects/6/packages/maven</url>
</repository>
```

## Nota sobre el Error Actual

El error menciona:

```
https://gitlab.com/api/v4/projects/9/packages/maven
```

Esto sugiere que:

1. **Hay una redirección** de `http://gitlab.wallparisoft.com` a `https://gitlab.com`
2. **El proyecto 9** podría ser otro proyecto diferente
3. **O hay una configuración** que está usando una URL diferente

### Solución:

1. **Verificar la URL correcta** en el `pom.xml`:

   - Actualmente usa: `http://gitlab.wallparisoft.com`
   - Pero el error muestra: `https://gitlab.com`
   - Puede que necesites cambiar a: `http://85.239.237.88`

2. **Actualizar el `pom.xml`** si es necesario:

```xml
<repositories>
    <repository>
        <id>gitlab-ebill-utils</id>
        <url>http://85.239.237.88/api/v4/projects/6/packages/maven</url>
    </repository>
    <repository>
        <id>gitlab-ebill-auth-util</id>
        <url>http://85.239.237.88/api/v4/projects/10/packages/maven</url>
    </repository>
</repositories>
```

## Resumen

| Proyecto        | ID Esperado | URL en pom.xml                                           |
| --------------- | ----------- | -------------------------------------------------------- |
| ebill-utils     | 6           | `http://85.239.237.88/api/v4/projects/6/packages/maven`  |
| ebill-auth-util | 10          | `http://85.239.237.88/api/v4/projects/10/packages/maven` |

**Para obtener el ID:**

1. Ir a GitLab: `http://85.239.237.88`
2. Buscar el proyecto
3. Settings > General > Project ID
4. O usar la API de GitLab con tu token

