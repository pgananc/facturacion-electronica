# 🧾 eBill Product Microservice

Microservicio para la gestión de productos en el sistema de facturación electrónica.

## 📋 Tabla de Contenidos

- [Tecnologías](#-tecnologías)
- [Requisitos Previos](#-requisitos-previos)
- [Configuración del Proyecto](#-configuración-del-proyecto)
- [Ejecución del Proyecto](#-ejecución-del-proyecto)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [API Endpoints](#-api-endpoints)
- [Base de Datos](#-base-de-datos)
- [Desarrollo con Cursor IDE](#-desarrollo-con-cursor-ide)
- [Troubleshooting](#-troubleshooting)

## 🚀 Tecnologías

- **Java**: 17
- **Spring Boot**: 3.4.5
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - Spring Boot DevTools
- **Base de Datos**: PostgreSQL 15
- **Migraciones**: Flyway
- **Mapeo de Objetos**: MapStruct 1.6.3
- **Utilidades**: Lombok 1.18.24
- **Build Tool**: Maven 3.8+
- **Contenedores**: Docker & Docker Compose

## 📦 Requisitos Previos

Antes de ejecutar el proyecto, asegúrate de tener instalado:

- ☕ **Java JDK 17** o superior
  ```bash
  java -version
  # Debe mostrar: java version "17.x.x"
  ```

- 📦 **Maven 3.8+**
  ```bash
  mvn -version
  ```

- 🐳 **Docker & Docker Compose** (para base de datos local)
  ```bash
  docker --version
  docker-compose --version
  ```

- 💻 **Cursor IDE** con extensiones de Java instaladas

## ⚙️ Configuración del Proyecto

### 1. Clonar el repositorio

```bash
git clone http://85.239.237.88:81/dev-billing/ebill-product.git
cd ebill-product
```

### 2. Configurar Base de Datos Local

El proyecto incluye un `docker-compose.yml` para levantar PostgreSQL localmente:

```bash
# Iniciar PostgreSQL
docker-compose up -d

# Verificar que está corriendo
docker-compose ps

# Ver logs
docker-compose logs -f postgres
```

**Credenciales de la Base de Datos (Localhost):**
- **Host**: localhost
- **Puerto**: 5433
- **Base de Datos**: ebill-dev-prod
- **Usuario**: postgres
- **Contraseña**: postgres

### 3. Configurar Variables de Entorno (Opcional)

Puedes crear un archivo `.env` basado en `.env.example`:

```bash
cp .env.example .env
```

### 4. Compilar el Proyecto

```bash
# Limpiar y compilar
mvn clean install

# O solo compilar
mvn compile
```

## 🏃 Ejecución del Proyecto

### Opción 1: Script de Inicio Rápido (Recomendado)

```bash
./run.sh localhost
```

Este script:
- ✅ Verifica que Java 17+ esté instalado
- ✅ Verifica que Maven esté instalado
- ✅ Comprueba la conexión a PostgreSQL
- ✅ Ofrece iniciar PostgreSQL automáticamente si no está corriendo
- ✅ Ejecuta la aplicación con el perfil correcto

### Opción 2: Maven Spring Boot Plugin

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

### Opción 3: Desde Cursor IDE

1. Abre el proyecto en Cursor IDE
2. Ve a la pestaña **Run and Debug** (Ctrl/Cmd + Shift + D)
3. Selecciona una de las configuraciones:
   - 🚀 **Run ebill-product (localhost)** - Ejecución normal
   - 🐛 **Debug ebill-product (localhost)** - Con depurador
   - 🔧 **Run with Maven (localhost)** - Compilar antes de ejecutar
4. Presiona F5 o haz clic en el botón verde de play

### Opción 4: Desde la Paleta de Comandos

1. Presiona `Cmd/Ctrl + Shift + P`
2. Escribe "Tasks: Run Task"
3. Selecciona: **maven: spring-boot:run (localhost)**

### Opción 5: Ejecutar el JAR compilado

```bash
# Compilar el proyecto
mvn clean package -DskipTests

# Ejecutar el JAR
java -jar -Dspring.profiles.active=localhost target/ebill-product-1.0.0.jar
```

## 🌐 Verificar que la Aplicación está Corriendo

La aplicación estará disponible en:

```
http://localhost:8380
```

Para verificar el estado puedes hacer una petición a alguno de los endpoints (requiere autenticación):

```bash
# Ejemplo con curl (necesitarás un token válido)
curl -X GET http://localhost:8380/api/product/company/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 📁 Estructura del Proyecto

```
ebill-product/
├── .vscode/                    # Configuración de Cursor/VS Code
│   ├── launch.json            # Configuraciones de ejecución y debug
│   ├── settings.json          # Configuración del workspace
│   ├── tasks.json             # Tareas de Maven
│   ├── extensions.json        # Extensiones recomendadas
│   └── java-formatter.xml     # Reglas de formateo de código
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/wallparisoft/ebill/product/
│   │   │       ├── config/             # Configuraciones
│   │   │       ├── controller/         # Controladores REST
│   │   │       ├── dto/                # Data Transfer Objects
│   │   │       ├── entity/             # Entidades JPA
│   │   │       ├── enumeration/        # Enumeraciones
│   │   │       ├── exception/          # Manejo de excepciones
│   │   │       ├── mapper/             # MapStruct mappers
│   │   │       ├── repository/         # Repositorios JPA
│   │   │       ├── response/           # Objetos de respuesta
│   │   │       ├── service/            # Lógica de negocio
│   │   │       └── EBillProductApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-localhost.properties
│   │       └── flyway/                 # Scripts de migración
│   └── test/
├── target/                     # Archivos compilados (ignorado en git)
├── docker-compose.yml          # PostgreSQL para desarrollo local
├── run.sh                      # Script de inicio rápido
├── pom.xml                     # Configuración de Maven
├── .env.example                # Ejemplo de variables de entorno
├── .gitignore                  # Archivos ignorados por git
└── README.md                   # Este archivo
```

## 🔌 API Endpoints

### Productos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/product/company/{idCompany}` | Obtener productos por empresa |
| GET | `/api/product/products-status/{status}` | Obtener productos por estado |
| GET | `/api/product/{idProduct}` | Obtener producto por ID |
| GET | `/api/product/exist/{mainCode}` | Verificar si existe un código principal |
| POST | `/api/product` | Crear nuevo producto |
| POST | `/api/product/pageable` | Listar productos con paginación |
| PATCH | `/api/product/{idProduct}` | Actualizar producto |
| DELETE | `/api/product/{id}` | Eliminar producto (soft delete) |

**Nota**: Todos los endpoints requieren autenticación mediante Spring Security.

## 🗄️ Base de Datos

### Migraciones Flyway

El proyecto usa Flyway para gestionar las migraciones de base de datos:

```
src/main/resources/flyway/
├── V2025050300__initial.sql           # Creación de tablas iniciales
├── V2025050301__default_data.sql      # Datos por defecto
└── V2025060100__add_new_col_tax_type_id.sql
```

### Esquema Principal

- **product**: Tabla de productos
  - id_product (PK)
  - id_company
  - main_code
  - name
  - product_type
  - unit_price
  - description
  - has_ice
  - has_tourism_iva
  - id_tax_type (FK)
  - status
  - creation_date
  - update_date

- **tax_type**: Tabla de tipos de impuestos
  - id_tax_type (PK)
  - type (IVA, ICE)
  - name
  - value
  - status
  - creation_date
  - update_date

## 💻 Desarrollo con Cursor IDE

### Extensiones Recomendadas

El proyecto incluye un archivo `.vscode/extensions.json` con las extensiones recomendadas:

- **Extension Pack for Java** - Soporte completo para Java
- **Spring Boot Extension Pack** - Herramientas para Spring Boot
- **Lombok Annotations Support** - Soporte para Lombok
- **Maven for Java** - Integración con Maven

### Atajos de Teclado Útiles

- `F5` - Iniciar depuración
- `Ctrl/Cmd + F5` - Ejecutar sin depuración
- `Shift + F5` - Detener depuración
- `Ctrl/Cmd + Shift + B` - Ejecutar tarea de build
- `Ctrl/Cmd + Shift + P` → "Maven: Execute Commands" - Ejecutar comandos Maven

### Tareas de Maven Disponibles

Desde la paleta de comandos (Ctrl/Cmd + Shift + P) → "Tasks: Run Task":

- `maven: clean install` - Limpiar e instalar
- `maven: clean package` - Empaquetar la aplicación
- `maven: compile` - Compilar el código
- `maven: test` - Ejecutar tests
- `maven: spring-boot:run (localhost)` - Ejecutar con perfil localhost
- `maven: dependency:tree` - Ver árbol de dependencias
- `maven: clean` - Limpiar el proyecto

### Debug

Para hacer debug:

1. Coloca breakpoints en el código (clic en el margen izquierdo)
2. Selecciona "🐛 Debug ebill-product (localhost)" en el panel de Run and Debug
3. Presiona F5
4. El debugger se detendrá en los breakpoints

El puerto de debug es **5005** por defecto.

## 🔧 Troubleshooting

### Error: "Port 8380 already in use"

```bash
# macOS/Linux: Encontrar y matar el proceso
lsof -ti:8380 | xargs kill -9

# Windows
netstat -ano | findstr :8380
taskkill /PID <PID> /F
```

### Error: "Connection refused" a la base de datos

1. Verifica que PostgreSQL está corriendo:
   ```bash
   docker-compose ps
   ```

2. Si no está corriendo, inícialo:
   ```bash
   docker-compose up -d
   ```

3. Verifica la conexión:
   ```bash
   nc -zv localhost 5433
   # o
   telnet localhost 5433
   ```

### Error: "Could not find or load main class"

```bash
# Limpiar y recompilar
mvn clean compile

# O reconstruir todo
mvn clean install
```

### Error: Flyway migrations

Si hay problemas con las migraciones:

```bash
# Opción 1: Recrear la base de datos
docker-compose down -v
docker-compose up -d

# Opción 2: Limpiar Flyway
mvn flyway:clean flyway:migrate
```

### Error: "Cannot resolve symbol" en IDE

1. Reimportar el proyecto Maven:
   - Ctrl/Cmd + Shift + P
   - "Java: Clean Java Language Server Workspace"
   - "Java: Reload Projects"

2. O desde terminal:
   ```bash
   mvn clean install -DskipTests
   ```

### Problemas con Lombok

Asegúrate de que:
1. La extensión de Lombok está instalada
2. Annotation processing está habilitado en settings.json
3. Recompila el proyecto: `mvn clean compile`

## 📝 Perfiles de Ejecución

El proyecto soporta diferentes perfiles:

- **localhost**: Desarrollo local (base de datos en localhost:5433)
- **dev**: Ambiente de desarrollo (servidor remoto)
- **prod**: Producción

Para cambiar el perfil, usa:

```bash
# Mediante script
./run.sh dev

# Mediante Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Mediante variable de entorno
export SPRING_PROFILES_ACTIVE=dev
java -jar target/ebill-product-1.0.0.jar
```

## 🤝 Contribuir

1. Crea una rama feature: `git checkout -b feature/nueva-funcionalidad`
2. Realiza tus cambios
3. Formatea el código: El proyecto usa Google Java Format con indentación de 2 espacios
4. Commit: `git commit -m "Descripción del cambio"`
5. Push: `git push origin feature/nueva-funcionalidad`
6. Crea un Merge Request

## 📄 Licencia

Propiedad de Wallparisoft - Todos los derechos reservados

## 👥 Autores

- Team Wallparisoft

## 📞 Soporte

Para soporte o preguntas, contacta al equipo de desarrollo.

---

**¡Listo para desarrollar! 🚀**

Para iniciar rápidamente:
```bash
docker-compose up -d && ./run.sh localhost
```
