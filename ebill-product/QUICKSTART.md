# ⚡ Quick Start - ebill-product

Guía ultra-rápida para poner el proyecto en marcha.

## 🚀 Inicio Rápido (5 minutos)

### Prerrequisitos
```bash
# Verificar Java 17+
java -version

# Verificar Maven
mvn -version

# Verificar Docker
docker --version
```

### Pasos

#### 1️⃣ Levantar Base de Datos
```bash
docker-compose up -d
```

#### 2️⃣ Ejecutar la Aplicación

**Opción A: Script (Recomendado)**
```bash
./run.sh localhost
```

**Opción B: Maven**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

**Opción C: Makefile**
```bash
make quick-start
```

**Opción D: Cursor IDE**
- Presiona `F5`
- Selecciona "🚀 Run ebill-product (localhost)"

#### 3️⃣ Verificar
```bash
# La aplicación debería estar corriendo en:
# http://localhost:8380
```

## 🎯 Comandos Más Usados

```bash
# Levantar DB
docker-compose up -d

# Ver logs de DB
docker-compose logs -f

# Parar DB
docker-compose down

# Compilar proyecto
mvn clean install

# Ejecutar tests
mvn test

# Formatear código
mvn fmt:format

# Ver dependencias
mvn dependency:tree
```

## 🔧 Con Makefile

```bash
# Setup inicial (primera vez)
make dev-setup

# Inicio rápido
make quick-start

# Solo ejecutar
make run

# Compilar
make build

# Tests
make test

# Ver todos los comandos
make help
```

## 🐛 Problemas Comunes

### Puerto 8380 ocupado
```bash
# macOS/Linux
lsof -ti:8380 | xargs kill -9

# O cambiar puerto en application-localhost.properties
server.port=8381
```

### Base de datos no conecta
```bash
# Reiniciar Docker
docker-compose down
docker-compose up -d

# Esperar 5 segundos y reintentar
```

### Errores de compilación
```bash
# Limpiar y recompilar
mvn clean install -U
```

## 📝 Archivos de Configuración

- **application-localhost.properties**: Configuración local
  - DB: `localhost:5433`
  - Usuario: `postgres`
  - Password: `postgres`

## 🎨 Extensiones de Cursor IDE

Recomendadas (se instalan automáticamente):
- Extension Pack for Java
- Spring Boot Extension Pack
- Lombok Annotations Support
- Maven for Java

## 📚 Más Información

- [README.md](README.md) - Documentación completa
- [DEVELOPMENT.md](DEVELOPMENT.md) - Guía de desarrollo
- [api-examples.http](api-examples.http) - Ejemplos de API

## 🆘 Soporte

Si tienes problemas:
1. Revisa [README.md - Troubleshooting](README.md#-troubleshooting)
2. Verifica los logs: `docker-compose logs -f`
3. Contacta al equipo de desarrollo

---

**¡Listo! 🎉**

Para desarrollo diario:
```bash
# Mañana cuando vuelvas
docker-compose up -d && ./run.sh localhost
```

