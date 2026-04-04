# 🐛 Solución de Problemas para Ejecutar desde Cursor IDE

## ✅ Problema Resuelto

**Problema:** La aplicación no se ejecutaba desde Cursor IDE con Java 17

**Causa:** El archivo `.vscode/launch.json` no tenía `javaExec` configurado en todas las configuraciones de lanzamiento.

**Solución:** Se agregó `javaExec` apuntando a Java 17 Corretto en todas las configuraciones.

---

## 📋 Pasos para Ejecutar desde Cursor

### Opción 1: Usar el Panel de Debug/Run

1. **Abrir el panel de Run and Debug:**

   - Presiona `Cmd + Shift + D` (macOS)
   - O ve a la barra lateral izquierda y haz clic en el ícono de ▶️ con un bug

2. **Seleccionar la configuración:**

   - `Spring Boot - EBillBillingApplication (localhost)` ← **RECOMENDADO**
   - `Spring Boot - EBillBillingApplication (default)`
   - `Debug Spring Boot - EBillBillingApplication (localhost)` (con depurador en puerto 5005)

3. **Iniciar la aplicación:**
   - Haz clic en el botón verde ▶️
   - O presiona `F5`

### Opción 2: Usar el Command Palette

1. Presiona `Cmd + Shift + P` (macOS)
2. Escribe: `Java: Run Java`
3. Selecciona `EBillBillingApplication`

### Opción 3: Desde el Archivo Principal

1. Abre `EBillBillingApplication.java`
2. Haz clic en `Run` o `Debug` que aparece sobre el método `main()`

---

## 🔍 Verificación de Configuración

### ✅ Java 17 está correctamente configurado en:

```json
// .vscode/launch.json
"javaExec": "/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home/bin/java"
```

```json
// .vscode/settings.json
"java.jdt.ls.java.home": "/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"
```

### ✅ El proyecto compila sin errores:

```bash
mvn clean compile -DskipTests
# Result: BUILD SUCCESS
```

---

## 🎯 Configuraciones Disponibles

### 1. Spring Boot - EBillBillingApplication (localhost)

- **Java:** 17 (Corretto)
- **Perfil:** `localhost`
- **Puerto:** 8480
- **Base de datos:** PostgreSQL en puerto 5433
- **Uso:** Desarrollo local con perfil localhost

### 2. Spring Boot - EBillBillingApplication (default)

- **Java:** 17 (Corretto)
- **Perfil:** `default`
- **Puerto:** Según configuración default
- **Uso:** Testing con perfil por defecto

### 3. Debug Spring Boot - EBillBillingApplication (localhost)

- **Java:** 17 (Corretto)
- **Perfil:** `localhost`
- **Puerto App:** 8480
- **Puerto Debug:** 5005
- **Uso:** Desarrollo con breakpoints y debugging

---

## 🛠️ Troubleshooting

### Si la aplicación no inicia desde Cursor:

#### 1. Verificar extensiones de Java instaladas

```bash
# Extensiones requeridas (ver .vscode/extensions.json):
- vscjava.vscode-java-pack
- vscjava.vscode-spring-boot-dashboard
- redhat.java
```

#### 2. Recargar la ventana de Cursor

```bash
Cmd + Shift + P → "Developer: Reload Window"
```

#### 3. Limpiar caché de Java

```bash
Cmd + Shift + P → "Java: Clean Java Language Server Workspace"
```

#### 4. Verificar que PostgreSQL esté corriendo

```bash
lsof -i :5433 | grep LISTEN
# Debería mostrar proceso escuchando en puerto 5433
```

#### 5. Verificar compilación

```bash
mvn clean compile -DskipTests
# Debería terminar con BUILD SUCCESS
```

#### 6. Verificar que no haya otro proceso en el puerto 8480

```bash
lsof -i :8480 | grep LISTEN
# No debería mostrar ningún proceso
```

---

## 📝 Logs y Debugging

### Ver logs en la terminal integrada

- Cuando ejecutas desde Cursor, los logs aparecen en el panel de **Terminal** integrado
- Si ves `Started EBillBillingApplication in X seconds` → ✅ Todo OK

### Conectar debugger remoto

Si usas la configuración de Debug:

1. La aplicación inicia con puerto de debug 5005
2. Puedes poner breakpoints en tu código
3. El debugger se conecta automáticamente

---

## 🎉 Señales de Éxito

Cuando la aplicación inicia correctamente, verás:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.5)

Starting EBillBillingApplication using Java 17.0.16 with PID xxxxx
The following 1 profile is active: "localhost"
Database: jdbc:postgresql://localhost:5433/ebill-dev-bill (PostgreSQL 14.17)
Successfully validated 3 migrations
Schema "public" is up to date. No migration necessary.
Tomcat started on port 8480 (http) with context path '/'
Started EBillBillingApplication in 4.xxx seconds
```

✅ **La aplicación está lista en:** `http://localhost:8480`

---

## 🚦 Comandos Rápidos

```bash
# Ver si la app está corriendo
lsof -i :8480

# Ver logs del PostgreSQL (si usas Docker)
docker logs ebill-postgres

# Detener la aplicación
# Desde la terminal integrada de Cursor: Ctrl+C

# O matar el proceso
kill $(lsof -t -i:8480)
```

---

## 💡 Tips

1. **Siempre usar la configuración "localhost"** para desarrollo
2. **El perfil localhost** usa `application-localhost.properties`
3. **PostgreSQL debe estar corriendo** antes de iniciar la app
4. **Java 17** es la versión correcta (no Java 23)
5. **Puerto 8480** debe estar libre

---

## 📚 Archivos de Configuración Importantes

```
.vscode/
├── launch.json        → Configuraciones de ejecución
├── settings.json      → Configuración de Java y Maven
├── tasks.json         → Tareas de Maven
└── extensions.json    → Extensiones recomendadas

src/main/resources/
├── application.properties           → Configuración base
└── application-localhost.properties → Configuración local

.env                   → Variables de entorno (opcional)
```

---

**Última actualización:** 12 de Octubre 2025  
**Estado:** ✅ Configuración validada y funcionando
