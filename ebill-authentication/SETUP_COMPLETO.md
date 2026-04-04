# ✅ Setup Completo - ebill-authentication

## 📋 Resumen de Configuración

Este documento resume toda la configuración realizada para ejecutar `ebill-authentication` localmente desde Cursor IDE.

---

## 🎯 Estado Actual

✅ **Microservicio funcionando correctamente**
- Puerto: `8280`
- Profile: `localhost`
- Java: `17.0.16 (Amazon Corretto)`
- Base de datos: `localhost:5433`
- Redis: `localhost:6379`

---

## 📁 Archivos Creados/Configurados

### 1. `.vscode/launch.json`
Configuración para ejecutar y debuggear desde Cursor IDE.
- Configura automáticamente Java 17
- Usa profile `localhost` por defecto
- Incluye configuración de debug

### 2. `.vscode/tasks.json`
Tasks predefinidos para Maven y scripts:
- Maven Clean, Compile, Install, Test
- Start Local (usando script)
- Check Health
- Docker Compose Up/Down

### 3. `.vscode/settings.json`
Configuración del workspace:
- Java 17 como runtime por defecto
- Maven configurado para usar Java 17
- Archivos de configuración excluidos

### 4. `.vscode/extensions.json`
Extensiones recomendadas:
- Java Extension Pack
- Spring Boot Extensions
- Lombok Support
- Docker Support

### 5. `.gitignore`
Actualizado para:
- ✅ **MANTENER** `.vscode/` (útil para el equipo)
- ❌ **IGNORAR** `.cursor/` (específico de usuario)
- ❌ **IGNORAR** `.env` (credenciales)

### 6. `scripts/start-local.sh`
Script mejorado que:
- Detecta y configura Java 17 automáticamente
- Verifica PostgreSQL
- Compila el proyecto
- Ejecuta con profile `localhost`

---

## 🚀 Cómo Ejecutar

### Opción 1: Desde Cursor IDE (Recomendado)

> ⚠️ **IMPORTANTE:** Si el puerto 8280 está ocupado, ejecuta primero:
> ```bash
> ./scripts/stop-service.sh
> ```

1. **Presiona `F5`** o ve al panel "Run and Debug"
2. Selecciona: **"🚀 Run ebill-authentication (localhost)"**
3. Click en el botón ▶️

O simplemente:
- Abre `EBillAuthenticationApplication.java`
- Click derecho → **"Run Java"**

### Opción 2: Usando el Script

```bash
./scripts/start-local.sh
```

### Opción 3: Maven Directo

```bash
export JAVA_HOME=/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

---

## ⚙️ Configuración de Profiles

### `application.properties` (DEFAULT)
- **Base de datos:** `85.239.237.88:5432` (producción)
- **Customer API:** `https://85.239.237.88:444`
- **Uso:** Producción/Staging

### `application-localhost.properties`
- **Base de datos:** `localhost:5433` (local)
- **Customer API:** `http://localhost:8180`
- **Uso:** Desarrollo local

> ⚠️ **IMPORTANTE:** Siempre usa `--spring.profiles.active=localhost` para desarrollo local.

---

## 🔍 Verificación

### Check Health
```bash
./scripts/check-health.sh
```

### Verificar manualmente
```bash
# Verificar proceso
ps aux | grep EBillAuthenticationApplication

# Verificar puerto
lsof -ti:8280

# Test endpoint (esperará 401 por seguridad)
curl -v http://localhost:8280/actuator/health
```

---

## 📊 Endpoints del Microservicio

Base URL: `http://localhost:8280`

- `/login` - Autenticación
- `/users` - Gestión de usuarios
- `/menus` - Gestión de menús
- `/roles` - Gestión de roles
- `/user-companies` - Empresas por usuario
- `/invoice-point-session` - Sesiones de punto de facturación
- `/actuator/health` - Health check (protegido)

> 🔒 **Nota:** Todos los endpoints están protegidos con Spring Security.

---

## 🐳 Docker Compose

### Iniciar servicios locales
```bash
docker-compose up -d
```

Incluye:
- PostgreSQL (puerto 5433)
- Redis (puerto 6379)
- Adminer (puerto 8081)
- Redis Commander (puerto 8082)

### URLs Útiles
- **Adminer:** http://localhost:8081
- **Redis Commander:** http://localhost:8082

---

## 🔧 Troubleshooting

### Error: "No qualifying bean of type 'RoleMapper'"

**Solución:**
```bash
mvn clean install -DskipTests
```
Esto regenerará los mappers de MapStruct con las anotaciones correctas.

### Error: Java version mismatch

**Solución:**
El script y las configuraciones de IDE ya configuran Java 17 automáticamente.
Si aún tienes problemas:
```bash
export JAVA_HOME=/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### Error: PostgreSQL connection refused

**Solución:**
```bash
docker-compose up -d postgres
```

### Puerto 8280 en uso

Este error ocurre cuando intentas ejecutar el microservicio desde Cursor IDE pero ya hay una instancia corriendo desde terminal.

**Solución rápida:**
```bash
./scripts/stop-service.sh
```

**Solución manual:**
```bash
# Encontrar proceso
lsof -ti:8280

# Matar proceso
kill -9 $(lsof -ti:8280)
```

**Prevención:** Siempre detén el proceso anterior antes de ejecutar desde IDE.

---

## 📚 Documentación Adicional

- `README.md` - Documentación principal del proyecto
- `CURSOR_IDE_SETUP.md` - Guía detallada de configuración de IDE
- `CONTRIBUTING.md` - Guía de contribución
- `scripts/README.md` - Documentación de scripts

---

## ✅ Checklist de Verificación

Antes de empezar a desarrollar, verifica:

- [ ] Java 17 instalado
- [ ] PostgreSQL corriendo en puerto 5433
- [ ] Redis corriendo en puerto 6379
- [ ] Puerto 8280 libre
- [ ] Extensiones de Java instaladas en Cursor IDE
- [ ] Proyecto compilado correctamente (`mvn clean install`)

---

## 👥 Equipo

**Wallparisoft Dev Team**  
Fecha de configuración: Octubre 2025

---

## 📝 Notas Finales

- Todos los archivos de configuración de `.vscode/` están en el repositorio para facilitar el setup del equipo
- El profile `localhost` es **obligatorio** para desarrollo local
- Los scripts automatizan tareas comunes y verificaciones
- La documentación está actualizada y lista para nuevos desarrolladores

**¡Happy Coding! 🚀**

