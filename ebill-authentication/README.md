# 🔐 ebill-authentication

Microservicio de autenticación y autorización para el sistema de facturación electrónica.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14.17-blue.svg)](https://www.postgresql.org/)

---

## 📋 Tabla de contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Inicio rápido](#-inicio-rápido)
- [Configuración](#-configuración)
- [Arquitectura](#-arquitectura)
- [API Endpoints](#-api-endpoints)
- [Desarrollo](#-desarrollo)
- [Testing](#-testing)
- [Contribución](#-contribución)

---

## 📖 Descripción

`ebill-authentication` es el microservicio encargado de:

- ✅ Autenticación de usuarios mediante JWT
- ✅ Gestión de roles y permisos (RBAC)
- ✅ Menús dinámicos por rol
- ✅ Recuperación de contraseñas
- ✅ Gestión de usuarios y compañías
- ✅ Control de sesiones de puntos de emisión
- ✅ Multi-tenancy (usuarios por compañía)

---

## ✨ Características

### 🔒 Seguridad
- Spring Security con JWT
- Encriptación de contraseñas con BCrypt
- Tokens de recuperación con expiración
- Control de sesiones con Redis

### 👥 Gestión de usuarios
- CRUD completo de usuarios
- Asignación de múltiples roles por compañía
- Usuarios multi-compañía
- Establecimientos por usuario

### 🎯 Autorización
- Sistema de menús jerárquicos
- Permisos basados en roles
- Control de acceso granular

### 📧 Notificaciones
- Envío de emails para recuperación de contraseña
- Templates HTML personalizables

---

## 🛠 Tecnologías

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 17 LTS | Lenguaje de programación |
| Spring Boot | 3.4.5 | Framework principal |
| Spring Security | 6.4.5 | Autenticación y autorización |
| PostgreSQL | 14.17 | Base de datos |
| Redis | Latest | Caché y sesiones |
| Flyway | 11.8.0 | Migraciones de BD |
| MapStruct | 1.6.3 | Mapeo de DTOs |
| Lombok | Latest | Reducción de boilerplate |
| OpenFeign | 4.2.1 | Cliente REST |
| Maven | 3.x | Gestión de dependencias |

---

## 🚀 Inicio rápido

### Requisitos previos

```bash
# Java 17
java -version  # openjdk version "17.0.16"

# Maven
mvn -version   # Apache Maven 3.x

# PostgreSQL
psql --version # PostgreSQL 14.x

# Redis (opcional)
redis-server --version
```

### Instalación

#### 1️⃣ Clonar el repositorio

```bash
git clone http://85.239.237.88:81/dev-billing/ebill-authentication.git
cd ebill-authentication
```

#### 2️⃣ Configurar base de datos

```bash
# Crear base de datos
createdb ebill-dev-auth

# O usar Docker
docker-compose up -d postgres
```

#### 3️⃣ Compilar

```bash
mvn clean compile -DskipTests
```

#### 4️⃣ Ejecutar

```bash
# Con Maven
mvn spring-boot:run -Dspring-boot.run.profiles=localhost

# O con JAR
mvn clean package -DskipTests
java -jar target/ebill-authentication-1.0.0.jar --spring.profiles.active=localhost
```

#### 5️⃣ Verificar

```bash
curl http://localhost:8280/actuator/health
```

---

## ⚙️ Configuración

### Profiles disponibles

- **`localhost`**: Desarrollo local
- **`default`**: Producción

### Variables de entorno

Puedes sobrescribir la configuración con variables de entorno:

```bash
# Base de datos
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=ebill-dev-auth
export DB_USER=postgres
export DB_PASSWORD=postgres

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Email
export MAIL_HOST=mail.privateemail.com
export MAIL_PORT=465
export MAIL_USERNAME=notificacion@wallparisoft.com
export MAIL_PASSWORD=tu_password

# Microservicios
export CUSTOMER_SERVICE_URL=http://localhost:8180
```

### Archivo de configuración

Edita `src/main/resources/application-localhost.properties`:

```properties
server.port=8280
spring.datasource.url=jdbc:postgresql://localhost:5433/ebill-dev-auth
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## 🏗 Arquitectura

```
ebill-authentication/
├── controller/      # REST endpoints
├── service/         # Lógica de negocio
├── repository/      # Acceso a datos
├── entity/          # Entidades JPA
├── dto/             # Data Transfer Objects
├── mapper/          # MapStruct mappers
├── exception/       # Manejo de errores
├── config/          # Configuración
└── util/            # Utilidades
```

### Modelo de datos

```
User 1--N UserCompany N--1 Company
UserCompany 1--N UserCompanyRole N--1 Role
UserCompany 1--N UserCompanyEstablishment
User 1--N UserInvoicePointSession
Role N--N Menu (via MenuRole)
```

---

## 🌐 API Endpoints

### Autenticación

```http
POST   /api/login              # Login
DELETE /api/login/logout       # Logout
```

### Usuarios

```http
GET    /api/user/pageable              # Listar usuarios (paginado)
GET    /api/user/{id}                  # Obtener usuario
GET    /api/user/company/{idCompany}   # Usuarios por compañía
POST   /api/user                       # Crear usuario
PATCH  /api/user/{id}                  # Actualizar usuario
DELETE /api/user/{id}                  # Desactivar usuario
GET    /api/user/exist/userName/{user} # Verificar si existe
POST   /api/user/restore/{token}       # Restaurar contraseña
```

### Menús

```http
GET    /api/menu/{userName}    # Obtener menús del usuario
```

### Puntos de emisión

```http
POST   /api/invoice-points/session           # Asignar punto
DELETE /api/invoice-points/session           # Liberar punto
GET    /api/invoice-points/session/active    # Listar activos
GET    /api/invoice-points/session/available # Puntos disponibles
```

### Documentación completa

Ver: [API Documentation](./docs/API.md) *(próximamente)*

---

## 💻 Desarrollo

### Configuración de IDE

Para **Cursor IDE / VS Code**, ver: [CURSOR_IDE_SETUP.md](./CURSOR_IDE_SETUP.md)

Para **IntelliJ IDEA**:
1. Importar como proyecto Maven
2. Configurar JDK 17
3. Habilitar annotation processing (Lombok + MapStruct)
4. Instalar plugins: Lombok, Spring Boot

### Comandos útiles

```bash
# Compilar
mvn clean compile

# Tests
mvn test

# Empaquetar
mvn clean package -DskipTests

# Formatear código
mvn fmt:format

# Verificar formato
mvn fmt:check

# Ver dependencias
mvn dependency:tree
```

### Estructura de Git

```bash
main          # Producción
develop       # Desarrollo
feature/*     # Nuevas funcionalidades
hotfix/*      # Correcciones urgentes
```

---

## 🧪 Testing

```bash
# Ejecutar todos los tests
mvn test

# Tests de integración
mvn verify

# Coverage report
mvn jacoco:report
```

*(Nota: Actualmente no hay tests implementados - TODO)*

---

## 🐳 Docker

### Levantar servicios locales

```bash
# PostgreSQL + Redis
docker-compose up -d

# Ver logs
docker-compose logs -f

# Detener
docker-compose down
```

---

## 📦 CI/CD

El proyecto usa **GitLab CI/CD**. Ver [`.gitlab-ci.yml`](./.gitlab-ci.yml)

Pipeline stages:
1. Build
2. Test
3. Package
4. Deploy

---

## 🤝 Contribución

### Proceso

1. Crear rama desde `develop`
2. Implementar cambios
3. Ejecutar tests y formateo
4. Crear Merge Request
5. Code review
6. Merge a `develop`

### Convenciones

- **Commits**: Conventional Commits
  - `feat:` Nueva funcionalidad
  - `fix:` Corrección de bug
  - `refactor:` Refactorización
  - `docs:` Documentación
  - `test:` Tests

- **Código**: Google Java Style Guide

---

## 📝 Roadmap

- [ ] Implementar tests unitarios
- [ ] Implementar tests de integración
- [ ] Agregar Swagger/OpenAPI
- [ ] Implementar rate limiting
- [ ] Agregar circuit breaker (Resilience4j)
- [ ] Implementar caché con Redis
- [ ] Mejorar manejo de excepciones
- [ ] Agregar métricas (Actuator + Prometheus)
- [ ] Dockerizar aplicación
- [ ] Implementar refresh tokens

---

## 👥 Autores

- **Equipo Wallparisoft** - Desarrollo y mantenimiento

---

## 📄 Licencia

Propietario - © 2025 Wallparisoft. Todos los derechos reservados.

---

## 🆘 Soporte

Para problemas o preguntas:

- 📧 Email: soporte@wallparisoft.com
- 💬 GitLab Issues: [Crear issue](http://85.239.237.88:81/dev-billing/ebill-authentication/-/issues)

---

## 🔗 Enlaces relacionados

- [ebill-customer](http://85.239.237.88:81/dev-billing/ebill-customer) - Microservicio de clientes
- [ebill-utils](http://85.239.237.88:81/dev-billing/ebill-utils) - Utilidades compartidas
- [ebill-auth-util](http://85.239.237.88:81/dev-billing/ebill-auth-util) - Utilidades de autenticación

---

**Versión:** 1.0.0  
**Última actualización:** Octubre 2025
