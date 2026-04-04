# 🖥️ Configuración de Rutas de Logos para Múltiples Sistemas Operativos

## 📋 Problema

Los desarrolladores trabajan en diferentes sistemas operativos:

- **Windows**: `C:\Users\Usuario\companies`
- **Mac**: `/Users/usuario/companies`
- **Linux (Producción)**: `/opt/ebilling/companies`

Necesitamos una solución que permita:

1. ✅ Desarrollo local en Windows/Mac con rutas compatibles
2. ✅ Producción en Linux con ruta estándar
3. ✅ Configuración fácil sin modificar código
4. ✅ Sin conflictos entre desarrolladores

---

## 🔍 Solución: Perfiles de Spring Boot

Spring Boot permite usar diferentes archivos de configuración según el perfil activo:

```
application.properties          # Configuración base
application-localhost.properties # Desarrollo local (Windows/Mac)
application-dev.properties       # Desarrollo (Linux)
application-prod.properties      # Producción (Linux)
```

---

## ✅ Opción 1: Usar Perfiles de Spring Boot (RECOMENDADA) ⭐

### **Ventajas:**

- ✅ Separación clara entre ambientes
- ✅ Cada desarrollador puede tener su configuración
- ✅ Fácil de mantener
- ✅ Compatible con diferentes OS

### **Estructura de Archivos:**

```
src/main/resources/
├── application.properties           # Base (valores por defecto)
├── application-localhost.properties # Windows/Mac desarrollo
├── application-dev.properties       # Linux desarrollo
└── application-prod.properties      # Linux producción
```

---

## 📝 Configuración por Ambiente

### **1. `application.properties` (Base)**

```properties
# Configuración base - valores por defecto
# Si no se especifica perfil, usa esta configuración

# Ruta por defecto (home del usuario)
# Windows: C:\Users\Usuario\companies
# Mac: /Users/usuario/companies
# Linux: /home/usuario/companies
company.logo.base-path=${user.home}/companies
```

---

### **2. `application-localhost.properties` (Windows/Mac Desarrollo)**

**Para Windows:**

```properties
# Perfil: localhost
# Activar con: --spring.profiles.active=localhost
# o en IDE: VM options: -Dspring.profiles.active=localhost

# Windows - Ruta absoluta
company.logo.base-path=C:/uploads/ebilling/companies

# O ruta relativa al proyecto
# company.logo.base-path=./uploads/companies

# O en el directorio de usuario
# company.logo.base-path=${user.home}/Documents/ebilling/companies
```

**Para Mac:**

```properties
# Perfil: localhost
# Mac - Ruta absoluta
company.logo.base-path=/Users/${user.name}/uploads/ebilling/companies

# O ruta relativa al proyecto
# company.logo.base-path=./uploads/companies

# O en el directorio de usuario
# company.logo.base-path=${user.home}/Documents/ebilling/companies
```

**Ruta Relativa (Funciona en ambos):**

```properties
# Perfil: localhost
# Ruta relativa al directorio de trabajo del proyecto
company.logo.base-path=./uploads/companies
```

---

### **3. `application-dev.properties` (Linux Desarrollo)**

```properties
# Perfil: dev
# Para servidores de desarrollo Linux

company.logo.base-path=/home/dev/ebilling/companies

# O si el usuario de desarrollo es diferente
# company.logo.base-path=${user.home}/ebilling/companies
```

---

### **4. `application-prod.properties` (Linux Producción)**

```properties
# Perfil: prod
# Para servidor de producción Linux

company.logo.base-path=/opt/ebilling/companies
```

---

## 🚀 Cómo Usar los Perfiles

### **Opción A: En el IDE (IntelliJ IDEA / Eclipse)**

**IntelliJ IDEA:**

1. Ir a `Run` → `Edit Configurations`
2. Seleccionar tu configuración de Spring Boot
3. En `VM options` o `Program arguments`:
   ```
   --spring.profiles.active=localhost
   ```

**Eclipse / Spring Tool Suite:**

1. Botón derecho en el proyecto → `Run As` → `Run Configurations`
2. Seleccionar `Spring Boot App`
3. En `Arguments` → `Program arguments`:
   ```
   --spring.profiles.active=localhost
   ```

---

### **Opción B: Desde la Línea de Comandos**

```bash
# Desarrollo local (Windows/Mac)
java -jar -Dspring.profiles.active=localhost ebill-customer.jar

# Desarrollo Linux
java -jar -Dspring.profiles.active=dev ebill-customer.jar

# Producción Linux
java -jar -Dspring.profiles.active=prod ebill-customer.jar
```

---

### **Opción C: Variables de Entorno**

```bash
# Windows (PowerShell)
$env:SPRING_PROFILES_ACTIVE="localhost"

# Windows (CMD)
set SPRING_PROFILES_ACTIVE=localhost

# Mac/Linux
export SPRING_PROFILES_ACTIVE=localhost
```

---

### **Opción D: En application.properties (por defecto)**

```properties
# En application.properties
spring.profiles.active=localhost
```

---

## 💻 Ejemplos Prácticos por Sistema Operativo

### **Windows 10/11**

**`application-localhost.properties`:**

```properties
# Opción 1: Ruta absoluta en disco C
company.logo.base-path=C:/uploads/ebilling/companies

# Opción 2: Ruta relativa al proyecto
company.logo.base-path=./uploads/companies

# Opción 3: En Documents del usuario
company.logo.base-path=${user.home}/Documents/ebilling/companies
```

**Crear directorio manualmente:**

```cmd
# En CMD o PowerShell
mkdir C:\uploads\ebilling\companies
```

---

### **macOS**

**`application-localhost.properties`:**

```properties
# Opción 1: Ruta absoluta en usuario
company.logo.base-path=/Users/${user.name}/uploads/ebilling/companies

# Opción 2: Ruta relativa al proyecto
company.logo.base-path=./uploads/companies

# Opción 3: En Documents del usuario
company.logo.base-path=${user.home}/Documents/ebilling/companies
```

**Crear directorio manualmente:**

```bash
mkdir -p ~/uploads/ebilling/companies
```

---

### **Linux (Producción)**

**`application-prod.properties`:**

```properties
company.logo.base-path=/opt/ebilling/companies
```

**Crear directorio y permisos:**

```bash
sudo mkdir -p /opt/ebilling/companies
sudo chown -R deploy:deploy /opt/ebilling/companies
sudo chmod -R 755 /opt/ebilling/companies
```

---

## 🔧 Solución Híbrida: Rutas Relativas + Variables de Entorno

### **Ventaja:**

- ✅ Funciona igual en todos los sistemas operativos
- ✅ No requiere cambiar configuración entre OS
- ✅ Fácil de probar

### **Configuración:**

**`application-localhost.properties`:**

```properties
# Usar variable de entorno o ruta por defecto
# Puedes sobrescribir desde fuera:
# Windows: set LOGO_PATH=C:\uploads\companies
# Mac/Linux: export LOGO_PATH=/Users/usuario/uploads/companies

company.logo.base-path=${LOGO_PATH:./uploads/companies}
```

**Usar:**

```bash
# Windows
set LOGO_PATH=C:\uploads\companies
java -jar -Dspring.profiles.active=localhost app.jar

# Mac/Linux
export LOGO_PATH=/Users/usuario/uploads/companies
java -jar -Dspring.profiles.active=localhost app.jar

# O sin variable (usa ./uploads/companies)
java -jar -Dspring.profiles.active=localhost app.jar
```

---

## 📊 Tabla Comparativa de Opciones

| Opción                                           | Windows | Mac | Linux Prod | Facilidad  | Recomendación    |
| ------------------------------------------------ | ------- | --- | ---------- | ---------- | ---------------- |
| **Ruta relativa** `./uploads/companies`          | ✅      | ✅  | ✅         | ⭐⭐⭐⭐⭐ | ⭐ **MÁS FÁCIL** |
| **Variable de entorno** `${LOGO_PATH:./uploads}` | ✅      | ✅  | ✅         | ⭐⭐⭐⭐   | ✅ Muy flexible  |
| **Perfiles separados**                           | ✅      | ✅  | ✅         | ⭐⭐⭐     | ✅ Estándar      |
| **Ruta absoluta fija**                           | ❌      | ❌  | ✅         | ⭐⭐       | ❌ No portable   |

---

## ✅ Recomendación Final

### **Para Desarrollo (Windows/Mac):**

**Usar ruta relativa en `application-localhost.properties`:**

```properties
# application-localhost.properties
company.logo.base-path=./uploads/companies
```

**Ventajas:**

- ✅ Funciona igual en Windows, Mac y Linux
- ✅ No requiere crear directorios manualmente
- ✅ Cada desarrollador lo tiene en su proyecto
- ✅ Fácil de hacer commit (opcional) o agregar a `.gitignore`

---

### **Para Producción (Linux):**

**Usar ruta absoluta en `application-prod.properties`:**

```properties
# application-prod.properties
company.logo.base-path=/opt/ebilling/companies
```

**Ventajas:**

- ✅ Ruta estándar de Linux
- ✅ Separada del proyecto
- ✅ Fácil de hacer backup
- ✅ No se pierde al actualizar código

---

## 📝 Configuración Completa Recomendada

### **1. `application.properties` (Base)**

```properties
# Configuración base
# Por defecto usa el home del usuario
company.logo.base-path=${user.home}/companies

# Configuración multipart
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

---

### **2. `application-localhost.properties` (Desarrollo Local)**

```properties
# Desarrollo local - Windows/Mac
# Ruta relativa al proyecto (funciona en todos los OS)
company.logo.base-path=./uploads/companies

# O si prefieres en el directorio de usuario:
# company.logo.base-path=${user.home}/Documents/ebilling/companies

# Configuración de base de datos local
spring.datasource.url=jdbc:postgresql://localhost:5433/ebill-dev-cus
```

---

### **3. `application-dev.properties` (Desarrollo Linux)**

```properties
# Desarrollo en servidor Linux
company.logo.base-path=/home/dev/ebilling/companies
```

---

### **4. `application-prod.properties` (Producción Linux)**

```properties
# Producción en servidor Linux
company.logo.base-path=/opt/ebilling/companies
```

---

## 🚀 Cómo Activar el Perfil Correcto

### **Desarrollo Local (Windows/Mac):**

**En IntelliJ IDEA:**

```
Run → Edit Configurations → VM options:
-Dspring.profiles.active=localhost
```

**Desde línea de comandos:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
# o
./mvnw spring-boot:run -Dspring-boot.run.profiles=localhost
```

**O ejecutando el JAR:**

```bash
java -jar -Dspring.profiles.active=localhost ebill-customer.jar
```

---

### **Producción (Linux):**

**En el servidor:**

```bash
java -jar -Dspring.profiles.active=prod ebill-customer.jar

# O con variable de entorno
export SPRING_PROFILES_ACTIVE=prod
java -jar ebill-customer.jar
```

**O en `application.properties` (solo en producción):**

```properties
spring.profiles.active=prod
```

---

## 📁 Estructura de Directorios Recomendada

### **En Desarrollo (Windows/Mac):**

```
proyecto-ebill-customer/
├── src/
├── uploads/              ← Ruta relativa (se crea automáticamente)
│   └── companies/
│       ├── 1/
│       │   └── logo.jpg
│       └── 2/
│           └── logo.png
├── .gitignore            ← Agregar: uploads/companies/
└── ...
```

**`.gitignore`:**

```
# Archivos de logos (no hacer commit)
uploads/companies/
```

---

### **En Producción (Linux):**

```
/opt/ebilling/
└── companies/           ← Ruta absoluta separada del proyecto
    ├── 1/
    │   └── logo.jpg
    └── 2/
        └── logo.png
```

---

## 🔄 Migración de Datos Existentes

Si ya tienes logos en `/home/deploy/companies/`:

```bash
# 1. Crear nuevo directorio en producción
sudo mkdir -p /opt/ebilling/companies
sudo chown -R deploy:deploy /opt/ebilling/companies

# 2. Copiar archivos existentes
sudo cp -r /home/deploy/companies/* /opt/ebilling/companies/

# 3. Verificar
ls -la /opt/ebilling/companies/

# 4. Actualizar application-prod.properties
# company.logo.base-path=/opt/ebilling/companies

# 5. Reiniciar aplicación con perfil prod
java -jar -Dspring.profiles.active=prod ebill-customer.jar
```

---

## ✅ Checklist para Desarrolladores

### **Setup Inicial (Windows/Mac):**

- [ ] Crear `application-localhost.properties`
- [ ] Configurar `company.logo.base-path=./uploads/companies`
- [ ] Activar perfil `localhost` en IDE
- [ ] Probar subida de logo (se crea directorio automáticamente)
- [ ] Agregar `uploads/companies/` a `.gitignore`

### **Setup Producción (Linux):**

- [ ] Crear directorio `/opt/ebilling/companies`
- [ ] Configurar permisos (`chown`, `chmod`)
- [ ] Crear `application-prod.properties`
- [ ] Configurar `company.logo.base-path=/opt/ebilling/companies`
- [ ] Ejecutar con perfil `prod`
- [ ] Probar subida de logo

---

## 🎯 Resumen de Mejores Prácticas

1. ✅ **Desarrollo:** Usar rutas relativas (`./uploads/companies`)
2. ✅ **Producción:** Usar rutas absolutas (`/opt/ebilling/companies`)
3. ✅ **Perfiles:** Usar diferentes archivos por ambiente
4. ✅ **Git:** Ignorar directorios de uploads en desarrollo
5. ✅ **Portabilidad:** Ruta relativa funciona en todos los OS
6. ✅ **Flexibilidad:** Variables de entorno para sobrescribir si es necesario

---

**Fecha de creación:** 2025-01-20  
**Última actualización:** 2025-01-20
