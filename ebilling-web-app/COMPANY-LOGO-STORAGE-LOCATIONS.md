# 📁 Ubicaciones Recomendadas para Almacenar Logos de Compañías

## 📋 Problema Actual

Actualmente, los logos se están guardando en:

```
/home/deploy/companies/{idCompany}/logo.{extension}
```

**Problema:** El usuario `deploy` es un usuario de sistema usado para desplegar la aplicación, no es adecuado para almacenar datos persistentes de la aplicación.

---

## 🔍 Análisis del Código Actual

En `CompanyServiceImpl.java` línea 60:

```java
@Value("${company.logo.base-path:${user.home}/companies}")
String logoBasePath;
```

Esto significa que:

- Si existe la propiedad `company.logo.base-path` en `application.properties` o `application.yml`, se usa esa.
- Si NO existe, usa `${user.home}/companies` como valor por defecto.
- `user.home` apunta al directorio home del usuario que ejecuta la aplicación (en este caso `/home/deploy`).

---

## ✅ Opciones Recomendadas para Producción

### **Opción 1: `/opt/ebilling/companies` ⭐ RECOMENDADA**

**Ventajas:**

- ✅ Ruta estándar en Linux para software instalado (`/opt`)
- ✅ Separada de usuarios del sistema
- ✅ Fácil de hacer backup
- ✅ Independiente del usuario que ejecuta la aplicación
- ✅ Convención Linux estándar

**Estructura:**

```
/opt/ebilling/companies/
  ├── 1/
  │   └── logo.jpg
  ├── 2/
  │   └── logo.png
  └── 3/
      └── logo.png
```

**Configuración en `application.properties`:**

```properties
company.logo.base-path=/opt/ebilling/companies
```

**Permisos necesarios:**

```bash
sudo mkdir -p /opt/ebilling/companies
sudo chown -R deploy:deploy /opt/ebilling/companies
sudo chmod -R 755 /opt/ebilling/companies
```

---

### **Opción 2: `/var/lib/ebilling/companies`**

**Ventajas:**

- ✅ Ruta estándar para datos de estado de aplicaciones (`/var/lib`)
- ✅ Separada de código y configuración
- ✅ Ideal para datos que persisten entre actualizaciones
- ✅ Convención Linux estándar

**Estructura:**

```
/var/lib/ebilling/companies/
  ├── 1/
  │   └── logo.jpg
  └── 2/
      └── logo.png
```

**Configuración en `application.properties`:**

```properties
company.logo.base-path=/var/lib/ebilling/companies
```

**Permisos necesarios:**

```bash
sudo mkdir -p /var/lib/ebilling/companies
sudo chown -R deploy:deploy /var/lib/ebilling/companies
sudo chmod -R 755 /var/lib/ebilling/companies
```

---

### **Opción 3: `/srv/ebilling/companies`**

**Ventajas:**

- ✅ Ruta estándar para datos de servicio (`/srv`)
- ✅ Convención Linux para datos específicos del sitio
- ✅ Separada de usuarios y software del sistema

**Estructura:**

```
/srv/ebilling/companies/
  ├── 1/
  │   └── logo.jpg
  └── 2/
      └── logo.png
```

**Configuración en `application.properties`:**

```properties
company.logo.base-path=/srv/ebilling/companies
```

**Permisos necesarios:**

```bash
sudo mkdir -p /srv/ebilling/companies
sudo chown -R deploy:deploy /srv/ebilling/companies
sudo chmod -R 755 /srv/ebilling/companies
```

---

### **Opción 4: `/usr/local/ebilling/companies`**

**Ventajas:**

- ✅ Ruta para software instalado localmente (`/usr/local`)
- ✅ Estándar en sistemas Linux
- ✅ No se modifica durante actualizaciones del sistema

**Estructura:**

```
/usr/local/ebilling/companies/
  ├── 1/
  │   └── logo.jpg
  └── 2/
      └── logo.png
```

**Configuración en `application.properties`:**

```properties
company.logo.base-path=/usr/local/ebilling/companies
```

**Permisos necesarios:**

```bash
sudo mkdir -p /usr/local/ebilling/companies
sudo chown -R deploy:deploy /usr/local/ebilling/companies
sudo chmod -R 755 /usr/local/ebilling/companies
```

---

## 📊 Comparación de Opciones

| Opción                          | Ventajas                                                        | Desventajas                  | Recomendación      |
| ------------------------------- | --------------------------------------------------------------- | ---------------------------- | ------------------ |
| `/opt/ebilling/companies`       | ✅ Estándar Linux<br>✅ Separado de usuarios<br>✅ Fácil backup | Ninguna significativa        | ⭐ **RECOMENDADA** |
| `/var/lib/ebilling/companies`   | ✅ Para datos persistentes<br>✅ Estándar Linux                 | Ninguna significativa        | ✅ Buena opción    |
| `/srv/ebilling/companies`       | ✅ Para datos de servicio<br>✅ Estándar Linux                  | Ninguna significativa        | ✅ Buena opción    |
| `/usr/local/ebilling/companies` | ✅ Software local<br>✅ Estándar                                | Puede confundirse con código | ✅ Aceptable       |

---

## 🔧 Cómo Configurar

### Paso 1: Crear el directorio

```bash
# Para /opt/ebilling/companies (recomendado)
sudo mkdir -p /opt/ebilling/companies
```

### Paso 2: Configurar permisos

```bash
# Dar permisos al usuario que ejecuta la aplicación
sudo chown -R deploy:deploy /opt/ebilling/companies
sudo chmod -R 755 /opt/ebilling/companies
```

### Paso 3: Configurar en Spring Boot

**Archivo:** `src/main/resources/application.properties`

```properties
# Ruta base para almacenar logos de compañías
company.logo.base-path=/opt/ebilling/companies
```

**O en `application.yml`:**

```yaml
company:
  logo:
    base-path: /opt/ebilling/companies
```

### Paso 4: Configurar por ambiente (opcional)

**Archivo:** `src/main/resources/application-prod.properties`

```properties
# Producción
company.logo.base-path=/opt/ebilling/companies
```

**Archivo:** `src/main/resources/application-dev.properties`

```properties
# Desarrollo local
company.logo.base-path=./uploads/companies
```

---

## 🚀 Migración de Datos Existentes

Si ya tienes logos guardados en `/home/deploy/companies/`, puedes migrarlos:

```bash
# 1. Crear nuevo directorio
sudo mkdir -p /opt/ebilling/companies
sudo chown -R deploy:deploy /opt/ebilling/companies

# 2. Copiar archivos existentes
sudo cp -r /home/deploy/companies/* /opt/ebilling/companies/

# 3. Verificar que se copiaron correctamente
ls -la /opt/ebilling/companies/

# 4. (Opcional) Hacer backup del directorio anterior
sudo mv /home/deploy/companies /home/deploy/companies.backup

# 5. Actualizar configuración en application.properties
# company.logo.base-path=/opt/ebilling/companies

# 6. Reiniciar la aplicación
```

---

## 📝 Consideraciones Adicionales

### Backup

Asegúrate de incluir el directorio de logos en tu estrategia de backup:

```bash
# Ejemplo de backup
tar -czf backup-logos-$(date +%Y%m%d).tar.gz /opt/ebilling/companies/
```

### Espacio en Disco

Monitorea el espacio en disco utilizado:

```bash
# Ver tamaño del directorio
du -sh /opt/ebilling/companies/
```

### Variables de Entorno

También puedes usar variables de entorno para mayor flexibilidad:

```properties
company.logo.base-path=${LOGO_STORAGE_PATH:/opt/ebilling/companies}
```

Luego en el sistema:

```bash
export LOGO_STORAGE_PATH=/opt/ebilling/companies
```

---

## ✅ Recomendación Final

**⭐ Usar `/opt/ebilling/companies`** porque:

1. ✅ Es la convención estándar de Linux para software instalado
2. ✅ Está separada de usuarios del sistema
3. ✅ Es fácil de identificar y hacer backup
4. ✅ No depende del usuario que ejecuta la aplicación
5. ✅ Es clara y mantenible

**Configuración recomendada:**

```properties
# src/main/resources/application.properties
company.logo.base-path=/opt/ebilling/companies
```

---

**Fecha de creación:** 2025-01-20  
**Última actualización:** 2025-01-20
