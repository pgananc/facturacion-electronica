# 🚀 Configuración de Cursor IDE para ebill-authentication

Este documento te guiará para configurar **Cursor IDE** (o VS Code) para ejecutar el microservicio `ebill-authentication` localmente.

## 📋 Pre-requisitos

1. **Java 17** instalado (Amazon Corretto 17.0.16 recomendado)
2. **Maven** instalado
3. **PostgreSQL** corriendo en `localhost:5433`
4. **Redis** (opcional, para sesiones distribuidas)
5. Extensiones de Cursor IDE/VS Code instaladas:
   - Extension Pack for Java (Microsoft)
   - Spring Boot Extension Pack (VMware)
   - Lombok Annotations Support

---

## ⚙️ Configuración paso a paso

### 1️⃣ Crear archivo `.vscode/settings.json`

Crea el directorio `.vscode` en la raíz del proyecto y agrega:

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "/Users/TU_USUARIO/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home",
      "default": true
    }
  ],
  "java.jdt.ls.java.home": "/Users/TU_USUARIO/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home",
  "maven.terminal.useJavaHome": true,
  "maven.executable.path": "mvn",
  "spring-boot.ls.java.home": "/Users/TU_USUARIO/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"
}
```

> **⚠️ IMPORTANTE:** Reemplaza `/Users/TU_USUARIO/...` con tu ruta real de Java 17.

**Para encontrar tu Java 17:**
```bash
/usr/libexec/java_home -V
```

---

### 2️⃣ Crear archivo `.vscode/launch.json`

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "🚀 Run ebill-authentication (localhost)",
      "request": "launch",
      "mainClass": "com.wallparisoft.authentication.EBillAuthenticationApplication",
      "projectName": "ebill-authentication",
      "args": "--spring.profiles.active=localhost",
      "console": "integratedTerminal"
    },
    {
      "type": "java",
      "name": "🐛 Debug ebill-authentication (localhost)",
      "request": "launch",
      "mainClass": "com.wallparisoft.authentication.EBillAuthenticationApplication",
      "projectName": "ebill-authentication",
      "args": "--spring.profiles.active=localhost",
      "vmArgs": "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005",
      "console": "integratedTerminal"
    }
  ]
}
```

> **⚠️ MUY IMPORTANTE:** El argumento `--spring.profiles.active=localhost` es OBLIGATORIO.
> 
> Sin este argumento, la aplicación intentará conectarse al servidor de producción (85.239.237.88) en lugar de tu localhost.

---

### 3️⃣ Crear archivo `.vscode/tasks.json` (opcional)

Para tareas de Maven:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Maven: Compile",
      "type": "shell",
      "command": "mvn clean compile -DskipTests",
      "group": {
        "kind": "build",
        "isDefault": true
      }
    },
    {
      "label": "Maven: Package",
      "type": "shell",
      "command": "mvn clean package -DskipTests",
      "group": "build"
    },
    {
      "label": "Maven: Test",
      "type": "shell",
      "command": "mvn test",
      "group": "test"
    }
  ]
}
```

---

## ▶️ Cómo ejecutar el microservicio

### **Opción 1: Desde Cursor IDE (Recomendado)**

1. Abre el archivo `EBillAuthenticationApplication.java`
2. Busca el ícono ▶️ (Run) en la clase principal
3. Click derecho → **Run 'EBillAuthenticationApplication'**
4. O usa el atajo: `Ctrl+F5` (sin debug) o `F5` (con debug)

### **Opción 2: Desde el panel de Debug**

1. Ve a la pestaña de **Run and Debug** (Ctrl+Shift+D)
2. Selecciona "🚀 Run ebill-authentication (localhost)"
3. Click en el botón verde ▶️

### **Opción 3: Desde terminal integrada**

```bash
# Asegurar Java 17
export JAVA_HOME=/Users/TU_USUARIO/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home

# Ejecutar con Maven
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

---

## 🧪 Verificar que funciona

Una vez iniciado, deberías ver:

```
2025-10-12T16:25:03.262-05:00  INFO 98059 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2025-10-12T16:25:03.550-05:00  INFO 98059 --- [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://localhost:5433/ebill-dev-auth (PostgreSQL 14.17)
```

**Prueba el endpoint:**
```bash
curl http://localhost:8280/actuator/health
```

---

## 🐛 Solución de problemas

### Error: "No qualifying bean of type 'RoleMapper'"
✅ **Solucionado** - El `ComponentScan` ya fue corregido.

### Error: "Port 8280 already in use"
```bash
# Detener proceso usando el puerto
lsof -ti:8280 | xargs kill -9
```

### Error: "Connection refused" a PostgreSQL
Verifica que PostgreSQL esté corriendo:
```bash
# En macOS con Homebrew
brew services list | grep postgresql

# Iniciar PostgreSQL
brew services start postgresql@14
```

### Error: MapStruct no genera implementaciones
```bash
# Limpiar y recompilar
mvn clean compile -DskipTests
```

---

## 🔧 Comandos útiles

```bash
# Compilar sin tests
mvn clean compile -DskipTests

# Empaquetar JAR
mvn clean package -DskipTests

# Ejecutar tests
mvn test

# Ver procesos Java corriendo
jps -l

# Detener microservicio
pkill -f "EBillAuthenticationApplication"

# Ver puerto 8280
lsof -ti:8280
```

---

## 📚 Estructura del proyecto

```
ebill-authentication/
├── src/main/java/
│   └── com/wallparisoft/authentication/
│       ├── EBillAuthenticationApplication.java  # ← Main class
│       ├── controller/                          # REST endpoints
│       ├── service/                             # Business logic
│       ├── repository/                          # Data access
│       ├── entity/                              # JPA entities
│       ├── dto/                                 # Data Transfer Objects
│       └── mapper/                              # MapStruct mappers
└── src/main/resources/
    ├── application.properties                    # Default config
    └── application-localhost.properties          # Local config
```

---

## 🌐 Endpoints principales

- **Health:** `http://localhost:8280/actuator/health`
- **Login:** `POST http://localhost:8280/api/login`
- **Users:** `http://localhost:8280/api/user/`
- **Menus:** `http://localhost:8280/api/menu/{userName}`

---

## 📝 Notas importantes

1. **NO subir `.vscode/` al repo** - Está en `.gitignore`
2. Las configuraciones son **personales** de cada desarrollador
3. Usar siempre **Java 17** (no 21, ni 23)
4. El profile `localhost` usa PostgreSQL en puerto `5433`
5. Spring Security está activo - algunos endpoints requieren autenticación

---

## 🤝 Soporte

Si tienes problemas, contacta al equipo de desarrollo o revisa los logs en:
- Terminal integrada de Cursor IDE
- Logs del sistema: `target/logs/`

---

**¡Listo! 🎉 Ahora puedes desarrollar cómodamente desde Cursor IDE**

