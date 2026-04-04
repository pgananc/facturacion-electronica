# 📜 Scripts de desarrollo

Scripts útiles para facilitar el desarrollo local de `ebill-authentication`.

---

## 📋 Scripts disponibles

### 🚀 `start-local.sh`
Inicia el microservicio en modo local.

**Uso:**
```bash
./scripts/start-local.sh
```

**Funcionalidad:**
- ✅ Verifica que Java 17 esté instalado
- ✅ Verifica que PostgreSQL esté corriendo
- ✅ Compila el proyecto
- ✅ Ejecuta con perfil `localhost`

---

### 🛑 `stop-service.sh`
Detiene el microservicio si está corriendo.

**Uso:**
```bash
./scripts/stop-service.sh
```

**Funcionalidad:**
- ✅ Busca proceso en puerto 8280
- ✅ Detiene el proceso de forma segura
- ✅ Libera el puerto para nueva ejecución

**Ideal para:** Antes de ejecutar desde Cursor IDE o reiniciar el servicio

---

### 🔧 `setup-dev.sh`
Configura todo el entorno de desarrollo desde cero.

**Uso:**
```bash
./scripts/setup-dev.sh
```

**Funcionalidad:**
- ✅ Verifica herramientas necesarias
- ✅ Levanta servicios Docker (PostgreSQL + Redis)
- ✅ Compila el proyecto
- ✅ Ejecuta migraciones Flyway
- ✅ Muestra resumen de servicios

**Ideal para:** Primera vez o después de clonar el repo

---

### 🧹 `cleanup.sh`
Limpia el entorno de desarrollo.

**Uso:**
```bash
./scripts/cleanup.sh
```

**Funcionalidad:**
- ✅ Detiene servicios Docker
- ✅ Limpia directorio `target/`
- ✅ Elimina archivos de log

---

### 🏥 `check-health.sh`
Verifica el estado del microservicio y sus dependencias.

**Uso:**
```bash
./scripts/check-health.sh
```

**Funcionalidad:**
- ✅ Verifica PostgreSQL en puerto 5433
- ✅ Verifica Redis en puerto 6379
- ✅ Verifica microservicio en puerto 8280
- ✅ Verifica proceso Java corriendo
- ✅ Muestra URLs útiles

---

## 🔄 Flujo de trabajo típico

### Primera vez / Setup inicial
```bash
# 1. Configurar entorno
./scripts/setup-dev.sh

# 2. Iniciar microservicio
./scripts/start-local.sh
```

### Desarrollo diario
```bash
# Verificar estado
./scripts/check-health.sh

# Iniciar si no está corriendo
./scripts/start-local.sh

# Detener antes de usar Cursor IDE (F5)
./scripts/stop-service.sh
```

### Limpiar y reiniciar
```bash
# Limpiar todo
./scripts/cleanup.sh

# Reconfigurar
./scripts/setup-dev.sh
```

---

## 🛠 Personalización

Puedes modificar estos scripts según tus necesidades:

- **Puerto de PostgreSQL:** Edita `5433` en los scripts
- **Puerto del microservicio:** Edita `8280` en los scripts
- **Perfil de Spring:** Cambia `localhost` por otro perfil

---

## ⚙️ Variables de entorno

Los scripts respetan las siguientes variables de entorno si están definidas:

```bash
export JAVA_HOME=/path/to/java17
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=ebill-dev-auth
```

---

## 🐛 Troubleshooting

### Script no ejecutable
```bash
chmod +x scripts/*.sh
```

### PostgreSQL no inicia
```bash
docker-compose up -d postgres
docker-compose logs postgres
```

### Puerto ya en uso
```bash
# Detener proceso en puerto 8280
lsof -ti:8280 | xargs kill -9
```

---

## 📝 Notas

- Estos scripts están diseñados para **macOS** y **Linux**
- En **Windows**, usar Git Bash o WSL2
- Los scripts usan colores ANSI para mejor legibilidad

