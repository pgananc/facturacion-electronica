# 🧾 eBill Billing - Microservicio de Facturación Electrónica

Microservicio para la gestión y emisión de comprobantes electrónicos (facturas) integrado con el SRI (Servicio de Rentas Internas de Ecuador).

## 📝 Descripción

**eBill Billing** es un microservicio desarrollado en Spring Boot que permite:

- ✅ Generar facturas electrónicas en formato XML
- ✅ Firmar digitalmente los comprobantes con certificados XADES
- ✅ Enviar comprobantes al SRI para validación y autorización
- ✅ Consultar estado de autorización de comprobantes
- ✅ Almacenar y gestionar facturas en base de datos PostgreSQL
- ✅ Procesar tareas programadas para verificación de autorizaciones
- ✅ Integración con otros microservicios mediante OpenFeign

## 🏗️ Arquitectura

### Stack Tecnológico

| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Java | 17 | Lenguaje de programación |
| Spring Boot | 3.4.5 | Framework principal |
| Spring Data JPA | 3.4.5 | Persistencia de datos |
| PostgreSQL | 15+ | Base de datos |
| Flyway | 11.8.0 | Migraciones de BD |
| Lombok | Latest | Reducción de boilerplate |
| OpenFeign | Latest | Cliente HTTP declarativo |
| MITyCLib | 1.1.7 | Firma digital XADES |
| JasperReports | 6.20.0 | Generación de PDFs (RIDE) |

### Arquitectura de Microservicios

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────┐
│                 │      │                  │      │             │
│  eBill-Customer │◄─────┤  eBill-Billing   │─────►│     SRI     │
│                 │      │  (Este servicio) │      │   Web API   │
└─────────────────┘      └──────────────────┘      └─────────────┘
                                 │
                                 ▼
                         ┌───────────────┐
                         │  Digital      │
                         │  Signature    │
                         │  Service      │
                         └───────────────┘
```

## 🚀 Inicio Rápido

### Pre-requisitos

- ☕ Java 17 o superior
- 📦 Maven 3.6+
- 🐳 Docker y Docker Compose (recomendado)
- 🗄️ PostgreSQL 15+ (si no usas Docker)

### Instalación Rápida

```bash
# 1. Clonar el repositorio
git clone <repository-url>
cd ebill-billing

# 2. Copiar configuración de entorno
cp .env.example .env

# 3. Levantar base de datos con Docker
docker-compose up -d postgres

# 4. Compilar el proyecto
mvn clean install -DskipTests

# 5. Ejecutar la aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

La aplicación estará disponible en: `http://localhost:8480`

## 🔧 Configuración para Desarrollo

### Configuración con Cursor IDE / VSCode

Este proyecto está completamente configurado para trabajar con **Cursor IDE** o **VSCode**:

1. **Abrir el proyecto en Cursor/VSCode**
   ```bash
   cd ebill-billing
   cursor . # o code .
   ```

2. **Instalar extensiones recomendadas**
   - El IDE te sugerirá instalar las extensiones necesarias
   - O instálalas desde `.vscode/extensions.json`

3. **Ejecutar desde el IDE**
   - Presiona `F5` o ve a Run & Debug
   - Selecciona: **"Spring Boot - EBillBillingApplication (localhost)"**
   - ¡Listo! 🎉

### Configuraciones Disponibles

El proyecto incluye:

- ✅ `.vscode/launch.json` - Configuraciones de ejecución y debug
- ✅ `.vscode/settings.json` - Configuración del workspace Java 17
- ✅ `.vscode/tasks.json` - Tareas de Maven y Docker
- ✅ `.vscode/extensions.json` - Extensiones recomendadas
- ✅ `docker-compose.yml` - Servicios de desarrollo (PostgreSQL + PgAdmin)
- ✅ `.env.example` - Variables de entorno de ejemplo

### Perfiles de Spring

| Perfil | Descripción | Base de Datos |
|--------|-------------|---------------|
| `default` | Producción/Dev remoto | PostgreSQL remoto (85.239.237.88:5432) |
| `localhost` | Desarrollo local | PostgreSQL local (localhost:5433) |

## 📊 Base de Datos

### Estructura Principal

```sql
-- Tablas principales
- invoice              -- Facturas
- invoice_detail       -- Detalles de facturas
- payment_method       -- Métodos de pago
- authorization_sri    -- Autorizaciones del SRI
```

### Migraciones Flyway

Las migraciones se ejecutan automáticamente al iniciar:

```
src/main/resources/flyway/
├── V2025050300__initial.sql           # Estructura inicial
├── V2025050301__default_data.sql      # Datos por defecto
└── V2025061801__new_authorization_sri.sql  # Nueva tabla autorización
```

### Acceso a la Base de Datos

```bash
# Con Docker Compose (PgAdmin)
docker-compose up -d
# Acceder a: http://localhost:5050
# Email: admin@ebill.com | Password: admin

# Con psql
psql -h localhost -p 5433 -U postgres -d ebill-dev-bill
```

## 🛠️ Comandos Útiles

### Maven

```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Generar JAR
mvn clean package

# Formatear código (Google Style)
mvn fmt:format

# Verificar formato
mvn fmt:check
```

### Docker

```bash
# Iniciar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f postgres

# Detener servicios
docker-compose down

# Limpiar volúmenes
docker-compose down -v
```

### Tareas desde VSCode/Cursor

Presiona `Cmd+Shift+P` (Mac) o `Ctrl+Shift+P` (Windows/Linux) y busca:

- **Tasks: Run Task**
  - Maven: Clean
  - Maven: Compile
  - Maven: Package
  - Maven: Install
  - Maven: Test
  - Maven: Format Code
  - Maven: Spring Boot Run (localhost)
  - Docker: Start PostgreSQL
  - Docker: Stop PostgreSQL

## 📡 API Endpoints

### Facturas (Invoices)

```http
# Listar todas las facturas
GET /api/invoice

# Crear nueva factura
POST /api/invoice
Content-Type: application/json

{
  "idClient": 1,
  "idCompany": 1,
  "issueDate": "2025-10-12T10:00:00",
  "subtotalIva": 100.00,
  "subtotalZero": 0.00,
  "totalAmount": 112.00,
  "idPaymentMethod": 1,
  "details": [
    {
      "productCode": "PROD-001",
      "description": "Producto de prueba",
      "quantity": 1,
      "unitPrice": 100.00,
      "totalPrice": 100.00
    }
  ]
}
```

## 🔐 Firma Digital

El proyecto utiliza **MITyCLib** para la firma digital XADES de los comprobantes electrónicos:

- Librerías incluidas en `lib/`
- Certificados digitales en formato `.p12`
- Cumple con estándares del SRI de Ecuador

## ⚙️ Tareas Programadas (Cron)

El servicio ejecuta tareas programadas para:

- Verificar estado de autorizaciones pendientes
- Consultar respuestas del SRI
- Actualizar estados de comprobantes

Configuración en `application-localhost.properties`:

```properties
cron.task.enabled=true
cron.task.expression=0 */2 * * * *  # Cada 2 minutos
```

## 🐛 Debug y Troubleshooting

### Ejecutar en Modo Debug

1. **Desde Cursor/VSCode:**
   - Selecciona: **"Debug Spring Boot - EBillBillingApplication (localhost)"**
   - Presiona `F5`
   - Puerto de debug: `5005`

2. **Desde línea de comandos:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005"
   ```

### Problemas Comunes

Ver [SETUP.md](SETUP.md) para una guía detallada de solución de problemas.

## 📁 Estructura del Proyecto

```
ebill-billing/
├── .vscode/                    # Configuración de IDE
│   ├── launch.json            # Configuraciones de ejecución
│   ├── settings.json          # Settings del workspace
│   ├── tasks.json             # Tareas de Maven
│   └── extensions.json        # Extensiones recomendadas
├── lib/                        # Librerías locales (firma digital)
├── src/
│   ├── main/
│   │   ├── java/com/wallparisoft/ebillbilling/
│   │   │   ├── config/        # Configuración
│   │   │   ├── controller/    # REST Controllers
│   │   │   ├── entity/        # Entidades JPA
│   │   │   ├── repository/    # Repositorios
│   │   │   ├── service/       # Lógica de negocio
│   │   │   ├── mapper/        # Mappers
│   │   │   ├── response/      # DTOs de respuesta
│   │   │   └── util/          # Utilidades
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-localhost.properties
│   │       ├── flyway/        # Migraciones de BD
│   │       └── META-INF/      # WSDLs del SRI
│   └── test/                  # Tests
├── docker-compose.yml         # Servicios de desarrollo
├── .env.example              # Variables de entorno
├── pom.xml                   # Configuración Maven
├── README.md                 # Este archivo
└── SETUP.md                  # Guía detallada de setup
```

## 🧪 Testing

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar un test específico
mvn test -Dtest=InvoiceServiceTest

# Ejecutar tests con perfil localhost
mvn test -Dspring.profiles.active=localhost

# Coverage (si está configurado)
mvn clean verify
```

## 📦 Construcción para Producción

```bash
# Generar JAR ejecutable
mvn clean package -DskipTests

# El JAR se generará en:
target/ebill-billing-1.0.0.jar

# Ejecutar JAR
java -jar target/ebill-billing-1.0.0.jar --spring.profiles.active=production
```

## 🔗 Dependencias Externas

Este microservicio depende de:

### 1. Digital Signature Service
- **URL (localhost):** `http://localhost:8180`
- **Propósito:** Firma digital de comprobantes XML

### 2. eBill Customer Service
- **URL (localhost):** `http://localhost:8180`
- **Propósito:** Gestión de clientes y empresas

### 3. SRI Web Services
- **Recepción:** [WSDL](src/main/resources/META-INF/wsdl/RecepcionComprobantes.wsdl)
- **Autorización:** [WSDL](src/main/resources/META-INF/wsdl/AutorizacionComprobantes.wsdl)

## 🤝 Contribución

1. Fork el proyecto
2. Crea tu rama de feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### Estándares de Código

- **Formato:** Google Java Style Guide
- **Auto-format:** Se ejecuta automáticamente con `mvn validate`
- **Lombok:** Usa anotaciones para reducir boilerplate
- **Clean Code:** Sigue principios SOLID

## 📄 Licencia

Copyright © 2025 Wallparisoft. Todos los derechos reservados.

## 👥 Equipo

Desarrollado por el equipo de Wallparisoft.

## 📞 Soporte

Para soporte técnico o consultas:
- 📧 Email: soporte@wallparisoft.com
- 🌐 Website: https://wallparisoft.com

---

## 🎯 Quick Links

- 📖 **[Guía Detallada de Setup](SETUP.md)** - Instrucciones completas de configuración
- 🐳 **[Docker Compose](docker-compose.yml)** - Servicios de desarrollo
- ⚙️ **[Configuración VSCode](.vscode/)** - Launch configs y settings
- 🔧 **[Propiedades de Aplicación](src/main/resources/)** - Configuración de Spring

---

**¡Feliz codificación! 🚀**
