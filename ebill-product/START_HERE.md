# 🎯 START HERE - eBill Product

```
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║     ███████╗██████╗ ██╗██╗     ██╗                         ║
║     ██╔════╝██╔══██╗██║██║     ██║                         ║
║     █████╗  ██████╔╝██║██║     ██║                         ║
║     ██╔══╝  ██╔══██╗██║██║     ██║                         ║
║     ███████╗██████╔╝██║███████╗███████╗                    ║
║     ╚══════╝╚═════╝ ╚═╝╚══════╝╚══════╝                    ║
║                                                              ║
║              Product Microservice v1.0.0                     ║
║                                                              ║
║         🚀 Listo para Desarrollo con Cursor IDE             ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

## ⚡ Inicio Rápido - 3 Pasos

### 1️⃣ Verificar Setup
```bash
./check-setup.sh
```

### 2️⃣ Iniciar Base de Datos
```bash
docker-compose up -d
```

### 3️⃣ Ejecutar Aplicación

**Opción A: Con Script**
```bash
./run.sh localhost
```

**Opción B: Con Makefile**
```bash
make quick-start
```

**Opción C: Con Cursor IDE**
```
Presiona F5 → Selecciona "🚀 Run ebill-product (localhost)"
```

---

## 📚 Documentación

| Archivo | Descripción |
|---------|-------------|
| 📖 [**QUICKSTART.md**](QUICKSTART.md) | ⚡ **EMPIEZA AQUÍ** - Guía ultra-rápida (5 min) |
| 📘 [**README.md**](README.md) | Documentación completa del proyecto |
| 🛠️ [**DEVELOPMENT.md**](DEVELOPMENT.md) | Guía de desarrollo y mejores prácticas |
| ✅ [**SETUP_COMPLETE.md**](SETUP_COMPLETE.md) | Resumen de toda la configuración |
| 🌐 [**api-examples.http**](api-examples.http) | Ejemplos de peticiones a la API |

---

## 🎮 Comandos Rápidos

### Con Makefile (Recomendado)
```bash
make help            # Ver todos los comandos
make dev-setup       # Setup inicial completo
make quick-start     # Docker + Ejecución
make run             # Solo ejecutar
make test            # Ejecutar tests
make docker-up       # Iniciar PostgreSQL
make docker-logs     # Ver logs de DB
```

### Con Scripts
```bash
./check-setup.sh     # Verificar configuración
./run.sh localhost   # Ejecutar con profile localhost
```

### Con Maven
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

---

## 🔧 Configuración

**Puerto de la Aplicación:** `8380`  
**URL Base:** `http://localhost:8380`

**Base de Datos PostgreSQL (Docker):**
- Host: `localhost`
- Puerto: `5433`
- Database: `ebill-dev-prod`
- Usuario: `postgres`
- Password: `postgres`

---

## 🎯 Cursor IDE

### Atajos de Teclado
- `F5` - Run/Debug
- `Shift + F5` - Stop
- `Ctrl/Cmd + Shift + P` - Command Palette
- `Ctrl/Cmd + Shift + B` - Build

### Configuraciones de Ejecución Disponibles
- 🚀 **Run ebill-product (localhost)** - Ejecución normal
- 🐛 **Debug ebill-product (localhost)** - Con debugger
- 🔧 **Run with Maven (localhost)** - Compilar y ejecutar

### Tareas de Maven Disponibles
- `maven: clean install`
- `maven: spring-boot:run (localhost)`
- `maven: test`
- Y más... (Ver tasks.json)

---

## 🧪 Testing

### Ejecutar Tests
```bash
mvn test
# o
make test
```

### Probar API
Usa el archivo `api-examples.http` con la extensión REST Client

---

## 📊 Estructura del Proyecto

```
ebill-product/
├── 📁 .vscode/              # Configuración de Cursor IDE
│   ├── launch.json          # Configuraciones de ejecución
│   ├── settings.json        # Settings del workspace
│   ├── tasks.json           # Tareas de Maven
│   └── extensions.json      # Extensiones recomendadas
│
├── 📁 src/
│   ├── 📁 main/java/        # Código fuente
│   │   └── com/wallparisoft/ebill/product/
│   │       ├── controller/  # REST Controllers
│   │       ├── service/     # Business Logic
│   │       ├── repository/  # Data Access
│   │       ├── entity/      # JPA Entities
│   │       └── dto/         # Data Transfer Objects
│   │
│   └── 📁 resources/
│       ├── application.properties
│       ├── application-localhost.properties
│       └── flyway/          # Database Migrations
│
├── 📄 README.md             # Documentación principal
├── 📄 QUICKSTART.md         # Inicio rápido
├── 📄 DEVELOPMENT.md        # Guía de desarrollo
├── 📄 SETUP_COMPLETE.md     # Resumen de configuración
├── 📄 api-examples.http     # Ejemplos de API
│
├── 🐳 docker-compose.yml    # PostgreSQL
├── 🔧 Makefile              # Comandos simplificados
├── 🚀 run.sh                # Script de ejecución
├── ✅ check-setup.sh        # Verificador de setup
├── 📦 pom.xml               # Maven dependencies
└── 🎨 .cursorrules          # Reglas para Cursor AI
```

---

## 🐛 Troubleshooting Rápido

### Puerto 8380 ocupado
```bash
lsof -ti:8380 | xargs kill -9
```

### PostgreSQL no conecta
```bash
docker-compose down
docker-compose up -d
```

### Errores de compilación
```bash
mvn clean install -U
```

### Ver todos los problemas
```bash
./check-setup.sh
```

---

## 🎓 Flujo de Trabajo Recomendado

### Primera Vez
```bash
1. ./check-setup.sh
2. make dev-setup
3. Abrir en Cursor IDE
4. Presionar F5
```

### Día a Día
```bash
1. docker-compose up -d
2. Presionar F5 en Cursor IDE
3. Desarrollar y hacer commits
4. docker-compose down (cuando termines)
```

---

## 🌟 Características

- ✅ Spring Boot 3.4.5
- ✅ Java 17
- ✅ PostgreSQL con Docker
- ✅ Flyway Migrations
- ✅ MapStruct + Lombok
- ✅ Spring Security
- ✅ Hot Reload
- ✅ Debug integrado
- ✅ Formateo automático
- ✅ Documentación completa

---

## 🚀 ¡Empieza Ahora!

### La forma MÁS RÁPIDA:
```bash
make quick-start
```

### O desde Cursor IDE:
**Presiona F5 y elige "🚀 Run ebill-product (localhost)"**

---

## 📞 Ayuda

Si tienes problemas:
1. ✅ Ejecuta `./check-setup.sh`
2. 📖 Lee [QUICKSTART.md](QUICKSTART.md)
3. 📘 Revisa [README.md - Troubleshooting](README.md#-troubleshooting)
4. 💬 Contacta al equipo de desarrollo

---

## 🎯 Próximos Pasos

1. ✅ Ejecutar el proyecto
2. 📚 Leer [DEVELOPMENT.md](DEVELOPMENT.md)
3. 🧪 Probar los endpoints con [api-examples.http](api-examples.http)
4. 💻 Empezar a desarrollar

---

```
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║                   ¡LISTO PARA DESARROLLAR!                   ║
║                                                              ║
║                      Happy Coding! 💻✨                      ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

*Configurado con ❤️ para Cursor IDE*

