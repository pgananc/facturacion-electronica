# eBill Billing - Guía de Configuración para Desarrollo

Este documento describe cómo configurar y ejecutar el microservicio **eBill Billing** en tu entorno de desarrollo local usando Cursor IDE.

## 📋 Requisitos Previos

### Software Requerido

1. **Java Development Kit (JDK) 17**
   ```bash
   # Verificar instalación
   java -version
   # Debe mostrar: openjdk version "17.x.x"
   ```

2. **Maven 3.6+**
   ```bash
   # Verificar instalación
   mvn -version
   ```

3. **Docker y Docker Compose** (para base de datos)
   ```bash
   # Verificar instalación
   docker --version
   docker-compose --version
   ```

4. **PostgreSQL 15** (alternativa a Docker)
   - Si no usas Docker, instala PostgreSQL localmente

### Extensiones de VS Code / Cursor IDE Recomendadas

Al abrir el proyecto, Cursor IDE te sugerirá instalar las extensiones recomendadas. Las principales son:

- **Extension Pack for Java** (vscjava.vscode-java-pack)
- **Spring Boot Extension Pack** (vmware.vscode-boot-dev-pack)
- **Lombok Annotations Support** (gabrielbb.vscode-lombok)
- **Maven for Java** (vscjava.vscode-maven)
- **SonarLint** (sonarsource.sonarlint-vscode)

## 🚀 Configuración Inicial

### 1. Clonar el Repositorio

```bash
cd /Users/jguerreroh/Development/wallparisoft/facturacion-electronica
git clone <repository-url> ebill-billing
cd ebill-billing
```

### 2. Configurar Variables de Entorno

```bash
# Copiar el archivo de ejemplo
cp .env.example .env

# Editar .env con tus configuraciones locales si es necesario
```

### 3. Configurar Java 17

#### En macOS con Homebrew:
```bash
# Instalar OpenJDK 17
brew install openjdk@17

# Crear enlace simbólico
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Configurar JAVA_HOME en tu ~/.zshrc o ~/.bash_profile
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Actualizar Configuración de VSCode/Cursor

Si tu JDK 17 está en una ruta diferente, actualiza `.vscode/settings.json`:

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "/ruta/a/tu/jdk-17",
      "default": true
    }
  ],
  "java.jdt.ls.java.home": "/ruta/a/tu/jdk-17"
}
```

### 4. Levantar la Base de Datos

#### Opción A: Usar Docker Compose (Recomendado)

```bash
# Iniciar PostgreSQL
docker-compose up -d postgres

# Ver logs
docker-compose logs -f postgres

# Detener
docker-compose down
```

La base de datos estará disponible en:
- **Host:** localhost
- **Puerto:** 5433
- **Database:** ebill-dev-bill
- **Usuario:** postgres
- **Password:** postgres

#### Opción B: PostgreSQL Local

Si tienes PostgreSQL instalado localmente:

```sql
-- Conectarse como superusuario
psql -U postgres

-- Crear base de datos
CREATE DATABASE "ebill-dev-bill";

-- Salir
\q
```

#### Opción C: Usar PgAdmin (Interfaz Gráfica)

```bash
# Iniciar PostgreSQL y PgAdmin
docker-compose up -d

# Acceder a PgAdmin en: http://localhost:5050
# Email: admin@ebill.com
# Password: admin
```

### 5. Compilar el Proyecto

```bash
# Limpiar y compilar
mvn clean install -DskipTests

# O usar la tarea de VSCode: Cmd+Shift+P -> "Tasks: Run Task" -> "Maven: Install"
```

## ▶️ Ejecutar la Aplicación

### Opción 1: Desde Cursor IDE (Recomendado)

1. Abrir la vista de **Run and Debug** (Cmd+Shift+D)
2. Seleccionar la configuración: **"Spring Boot - EBillBillingApplication (localhost)"**
3. Presionar **F5** o hacer clic en el botón de play

### Opción 2: Desde Terminal con Maven

```bash
# Ejecutar con perfil localhost
mvn spring-boot:run -Dspring-boot.run.profiles=localhost

# O usar la tarea de VSCode
```

### Opción 3: Ejecutar JAR Compilado

```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar -Dspring.profiles.active=localhost target/ebill-billing-1.0.0.jar
```

## 🔍 Verificar que la Aplicación Esté Funcionando

### 1. Verificar Logs

Busca en los logs:
```
Started EBillBillingApplication in X.XXX seconds
Tomcat started on port(s): 8480 (http)
```

### 2. Probar Endpoints

```bash
# Health check (si está configurado)
curl http://localhost:8480/actuator/health

# Listar facturas
curl http://localhost:8480/api/invoice

# Ver documentación de la API (si Swagger está habilitado)
open http://localhost:8480/swagger-ui.html
```

## 🐛 Debug

### Debug Remoto

La configuración **"Debug Spring Boot - EBillBillingApplication (localhost)"** habilita debug remoto en el puerto **5005**.

1. Ejecutar la configuración de debug
2. Colocar breakpoints en el código
3. Hacer peticiones a la API

### Debug de Tests

```bash
# Ejecutar tests con debug
mvn test -Dspring.profiles.active=localhost

# O desde VSCode: Click derecho en el test -> "Debug Test"
```

## 📊 Base de Datos

### Migraciones con Flyway

Las migraciones de base de datos se ejecutan automáticamente al iniciar la aplicación. Los scripts están en:

```
src/main/resources/flyway/
├── V2025050300__initial.sql
├── V2025050301__default_data.sql
└── V2025061801__new_authorization_sri.sql
```

### Conectarse a la Base de Datos

```bash
# Con psql
psql -h localhost -p 5433 -U postgres -d ebill-dev-bill

# Listar tablas
\dt

# Ver estructura de una tabla
\d invoice
```

## 🔧 Tareas Comunes de Maven

Las siguientes tareas están disponibles en VSCode (Cmd+Shift+P -> "Tasks: Run Task"):

- **Maven: Clean** - Limpiar el proyecto
- **Maven: Compile** - Compilar código fuente
- **Maven: Package** - Generar JAR
- **Maven: Install** - Instalar en repositorio local
- **Maven: Test** - Ejecutar tests
- **Maven: Format Code** - Formatear código según Google Style
- **Maven: Spring Boot Run (localhost)** - Ejecutar con perfil localhost

## 📦 Dependencias Externas

Este proyecto depende de otros microservicios:

### 1. Digital Signature Service
- **URL:** http://localhost:8180
- **Propósito:** Firma digital de documentos XML

### 2. Customer Service
- **URL:** http://localhost:8180
- **Propósito:** Gestión de clientes

⚠️ **Nota:** Asegúrate de tener estos servicios ejecutándose o actualiza las URLs en `application-localhost.properties`.

## 🔐 Firma Digital

El proyecto incluye librerías para firma digital XADES:

- MITyCLibXADES
- MITyCLibTSA
- xmlsec

Estas librerías están en la carpeta `lib/` y se cargan como dependencias del sistema.

## 🔄 Tareas Programadas (Cron)

El servicio incluye tareas programadas configurables:

```properties
# En application-localhost.properties
cron.task.enabled=true
cron.task.expression=0 */2 * * * *  # Cada 2 minutos
```

## 📝 Estructura del Proyecto

```
ebill-billing/
├── src/main/java/com/wallparisoft/ebillbilling/
│   ├── config/          # Configuración (Scheduler, CORS)
│   ├── controller/      # Controladores REST
│   ├── entity/          # Entidades JPA
│   ├── repository/      # Repositorios Spring Data
│   ├── service/         # Lógica de negocio
│   ├── mapper/          # Mappers DTO <-> Entity
│   ├── response/        # DTOs de respuesta
│   └── util/            # Utilidades
├── src/main/resources/
│   ├── application.properties
│   ├── application-localhost.properties
│   └── flyway/          # Scripts de migración
├── lib/                 # Librerías locales (firma digital)
├── .vscode/             # Configuración de Cursor/VSCode
│   ├── launch.json      # Configuraciones de ejecución
│   ├── settings.json    # Configuración del workspace
│   └── tasks.json       # Tareas de Maven
└── docker-compose.yml   # Servicios de desarrollo (PostgreSQL)
```

## ❓ Solución de Problemas

### Problema: "Cannot find Java 17"

**Solución:**
```bash
# Verificar JAVA_HOME
echo $JAVA_HOME

# Listar versiones instaladas
/usr/libexec/java_home -V

# Establecer Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Problema: "Port 5433 already in use"

**Solución:**
```bash
# Verificar qué proceso usa el puerto
lsof -i :5433

# Detener contenedor existente
docker stop ebill-postgres
docker rm ebill-postgres

# O cambiar el puerto en docker-compose.yml y application-localhost.properties
```

### Problema: "Flyway migration failed"

**Solución:**
```bash
# Conectarse a la BD y limpiar
psql -h localhost -p 5433 -U postgres -d ebill-dev-bill

# Limpiar tabla de Flyway
DROP TABLE IF EXISTS "FLYWAY" CASCADE;

# Reintentar la aplicación
```

### Problema: "Cannot resolve dependency ebill-auth-util"

**Solución:**
Este proyecto depende de librerías privadas de Wallparisoft. Necesitas:

1. Acceso al repositorio Maven privado en `https://85.239.237.88/api/v4/projects/`
2. Configurar credenciales en `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>gitlab-ebill-utils</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>YOUR_GITLAB_TOKEN</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
    <server>
      <id>gitlab-ebill-auth-util</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>YOUR_GITLAB_TOKEN</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>
</settings>
```

### Problema: "Lombok annotations not working"

**Solución:**
1. Instalar extensión "Lombok Annotations Support"
2. Reiniciar Cursor IDE
3. Recargar el proyecto Java: Cmd+Shift+P -> "Java: Clean Java Language Server Workspace"

## 📚 Recursos Adicionales

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## 🤝 Soporte

Para problemas o preguntas, contacta al equipo de desarrollo de Wallparisoft.

---

**Happy Coding! 🚀**



