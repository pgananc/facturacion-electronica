# 📜 Scripts de Desarrollo

Esta carpeta contiene scripts útiles para facilitar el desarrollo del proyecto eBill Billing.

## Scripts Disponibles

### 🚀 `start-dev.sh`
Inicia el entorno de desarrollo completo.

**Qué hace:**
- ✅ Verifica que Java 17 esté instalado
- ✅ Verifica que Docker esté disponible
- ✅ Crea archivo `.env` si no existe
- ✅ Inicia PostgreSQL con Docker Compose
- ✅ Compila el proyecto
- ✅ Muestra información de conexión

**Uso:**
```bash
./scripts/start-dev.sh
```

---

### ▶️ `run-app.sh`
Ejecuta la aplicación con el perfil localhost.

**Qué hace:**
- ✅ Carga variables de entorno desde `.env`
- ✅ Ejecuta la aplicación con Maven
- ✅ Usa el perfil `localhost`

**Uso:**
```bash
./scripts/run-app.sh
```

---

### 🛑 `stop-dev.sh`
Detiene el entorno de desarrollo.

**Qué hace:**
- ✅ Detiene los contenedores Docker (PostgreSQL y PgAdmin)
- ✅ No elimina los datos (volúmenes)

**Uso:**
```bash
./scripts/stop-dev.sh
```

---

### 🧹 `clean-all.sh`
Limpia completamente el proyecto.

**Qué hace:**
- ✅ Ejecuta `mvn clean`
- ✅ Detiene y elimina contenedores Docker
- ✅ Opcionalmente elimina volúmenes (datos de BD)

**Uso:**
```bash
./scripts/clean-all.sh
```

---

### 🧪 `test.sh`
Ejecuta los tests del proyecto.

**Qué hace:**
- ✅ Carga variables de entorno desde `.env`
- ✅ Ejecuta `mvn test` con perfil localhost

**Uso:**
```bash
./scripts/test.sh
```

---

## Flujo de Trabajo Típico

### Primera vez (Setup inicial)
```bash
# 1. Iniciar entorno de desarrollo
./scripts/start-dev.sh

# 2. Ejecutar la aplicación (en otra terminal)
./scripts/run-app.sh

# O ejecutar desde Cursor IDE presionando F5
```

### Desarrollo diario
```bash
# Iniciar servicios (si no están corriendo)
docker-compose up -d

# Ejecutar desde Cursor IDE presionando F5
# o
./scripts/run-app.sh
```

### Al finalizar
```bash
# Detener servicios (conserva datos)
./scripts/stop-dev.sh

# O limpiar todo (elimina datos)
./scripts/clean-all.sh
```

---

## Solución de Problemas

### Script no ejecuta
```bash
# Dar permisos de ejecución
chmod +x scripts/*.sh
```

### "Java 17 not found"
```bash
# Verificar versión de Java
java -version

# Establecer JAVA_HOME (macOS)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### "Docker not found"
```bash
# Instalar Docker Desktop para Mac
# https://docs.docker.com/desktop/install/mac-install/
```

### Puerto 5433 en uso
```bash
# Ver qué proceso usa el puerto
lsof -i :5433

# Cambiar puerto en docker-compose.yml y application-localhost.properties
```

---

## Personalización

Puedes modificar los scripts según tus necesidades. Son scripts bash estándar que puedes editar con cualquier editor de texto.

### Variables Comunes

Edita `.env` para personalizar:
```bash
SERVER_PORT=8480
DB_PORT=5433
DB_NAME=ebill-dev-bill
```

---

## Notas

- 📝 Los scripts están diseñados para macOS/Linux
- 🪟 Para Windows, usa Git Bash o WSL
- 🐳 Requieren Docker instalado
- ☕ Requieren Java 17 y Maven



