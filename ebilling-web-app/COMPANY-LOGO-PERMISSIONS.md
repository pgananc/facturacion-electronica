# 🔒 Permisos para el Directorio de Logos `/opt/ebilling/companies`

## 📋 Resumen

El directorio `/opt/ebilling/companies` necesita permisos específicos para que la aplicación Spring Boot pueda:

- ✅ Crear subdirectorios por ID de compañía (ej: `/opt/ebilling/companies/1/`)
- ✅ Escribir archivos de logos (ej: `logo.jpg`)
- ✅ Leer archivos de logos para servir al frontend
- ✅ Eliminar logos antiguos al subir uno nuevo

---

## 👤 Propietario y Grupo Recomendado

### **Propietario:**

- Usuario que ejecuta la aplicación Spring Boot (normalmente `deploy`)

### **Grupo:**

- Mismo grupo del usuario (normalmente `deploy`)
- O un grupo específico como `ebilling` o `www-data` si hay un servidor web que también necesita acceso

---

## 🔐 Permisos Recomendados

### **Para el Directorio Principal:**

```
Propietario (deploy):  rwx (lectura, escritura, ejecución)
Grupo:                 r-x (lectura, ejecución)
Otros:                 r-x (lectura, ejecución)
```

**En formato numérico:** `755`

### **Para los Subdirectorios y Archivos:**

Los subdirectorios (ej: `/opt/ebilling/companies/1/`) heredarán los permisos del directorio padre al crearse.

---

## 📝 Comandos para Configurar Permisos

### **Paso 1: Crear el Directorio**

```bash
sudo mkdir -p /opt/ebilling/companies
```

**Explicación:**

- `-p`: Crea el directorio y todos los directorios padre necesarios (si no existen)
- Necesitas `sudo` porque `/opt` requiere permisos de administrador

---

### **Paso 2: Cambiar Propietario y Grupo**

```bash
sudo chown -R deploy:deploy /opt/ebilling/companies
```

**Explicación:**

- `chown`: Cambia el propietario y grupo
- `-R`: Recursivo (aplica a todos los archivos y subdirectorios)
- `deploy:deploy`: Usuario `deploy` y grupo `deploy`
- **Ajusta** `deploy:deploy` según tu usuario y grupo real

**Verificar:**

```bash
ls -la /opt/ebilling/
# Debe mostrar: drwxr-xr-x deploy deploy companies
```

---

### **Paso 3: Configurar Permisos del Directorio**

#### **Opción A: Permisos Estándar (755) - RECOMENDADA**

```bash
sudo chmod -R 755 /opt/ebilling/companies
```

**Permisos resultantes:**

```
Propietario:  rwx (7) - lectura, escritura, ejecución
Grupo:        r-x (5) - lectura, ejecución
Otros:        r-x (5) - lectura, ejecución
```

**Esto significa:**

- ✅ El usuario `deploy` puede leer, escribir y ejecutar (entrar al directorio)
- ✅ El grupo puede leer y ejecutar
- ✅ Otros usuarios pueden leer y ejecutar

---

#### **Opción B: Permisos Más Restrictivos (750) - Más Seguro**

```bash
sudo chmod -R 750 /opt/ebilling/companies
```

**Permisos resultantes:**

```
Propietario:  rwx (7) - lectura, escritura, ejecución
Grupo:        r-x (5) - lectura, ejecución
Otros:        --- (0) - sin permisos
```

**Ventaja:**

- ✅ Más seguro: otros usuarios no pueden ver los logos
- ✅ Útil si los logos contienen información sensible

---

#### **Opción C: Permisos Más Permisivos (775) - Si Hay Servidor Web**

```bash
sudo chmod -R 775 /opt/ebilling/companies
```

**Permisos resultantes:**

```
Propietario:  rwx (7) - lectura, escritura, ejecución
Grupo:        rwx (7) - lectura, escritura, ejecución
Otros:        r-x (5) - lectura, ejecución
```

**Cuándo usar:**

- ✅ Si hay un servidor web (Nginx, Apache) que necesita servir las imágenes
- ✅ Si el servidor web está en el mismo grupo `deploy`

**Nota:** Normalmente el servidor web lee desde la aplicación Spring Boot, no directamente del sistema de archivos, así que esto no suele ser necesario.

---

## 🔧 Comandos Completos (Script Completo)

### **Script Recomendado para Producción:**

```bash
#!/bin/bash

# Variables (ajusta según tu entorno)
USER_APP="deploy"
GROUP_APP="deploy"
LOGO_PATH="/opt/ebilling/companies"

# Crear directorio
sudo mkdir -p $LOGO_PATH

# Cambiar propietario y grupo
sudo chown -R $USER_APP:$GROUP_APP $LOGO_PATH

# Configurar permisos (750 = más seguro, 755 = estándar)
sudo chmod -R 755 $LOGO_PATH

# Verificar configuración
echo "Verificando permisos..."
ls -la /opt/ebilling/
ls -la $LOGO_PATH

echo "✅ Configuración completada"
```

---

## 🔍 Verificar Permisos

### **Ver Propietario y Permisos:**

```bash
ls -la /opt/ebilling/companies
```

**Salida esperada:**

```
drwxr-xr-x 2 deploy deploy 4096 Jan 20 12:00 companies
```

**Explicación de la salida:**

- `d`: Es un directorio
- `rwxr-xr-x`: Permisos (755)
  - `rwx`: Propietario (lectura, escritura, ejecución)
  - `r-x`: Grupo (lectura, ejecución)
  - `r-x`: Otros (lectura, ejecución)
- `deploy`: Propietario
- `deploy`: Grupo
- `4096`: Tamaño
- `Jan 20 12:00`: Fecha de modificación
- `companies`: Nombre del directorio

---

### **Ver Permisos en Formato Numérico:**

```bash
stat -c "%a %U:%G" /opt/ebilling/companies
```

**Salida esperada:**

```
755 deploy:deploy
```

---

### **Probar Escritura:**

```bash
# Como usuario deploy
sudo -u deploy touch /opt/ebilling/companies/test.txt

# Si funciona, eliminar el archivo de prueba
sudo -u deploy rm /opt/ebilling/companies/test.txt
```

---

## 📊 Tabla de Permisos Recomendados

| Escenario               | Permisos | Comando        | Cuándo Usar                             |
| ----------------------- | -------- | -------------- | --------------------------------------- |
| **Producción Estándar** | `755`    | `chmod -R 755` | ✅ **RECOMENDADO** - Permisos estándar  |
| **Producción Segura**   | `750`    | `chmod -R 750` | Si los logos contienen info sensible    |
| **Con Servidor Web**    | `775`    | `chmod -R 775` | Si Nginx/Apache necesita acceso directo |

---

## ⚠️ Consideraciones Importantes

### **1. Directorios Padre:**

Asegúrate de que los directorios padre también tengan permisos correctos:

```bash
# Verificar /opt/ebilling
sudo ls -la /opt/

# Si no existe /opt/ebilling, crearlo
sudo mkdir -p /opt/ebilling
sudo chown deploy:deploy /opt/ebilling
sudo chmod 755 /opt/ebilling
```

---

### **2. SELinux (si está habilitado):**

En sistemas con SELinux habilitado, puede ser necesario configurar el contexto:

```bash
# Verificar estado de SELinux
getenforce

# Si está habilitado (Enforcing), configurar contexto
sudo chcon -R -t httpd_sys_content_t /opt/ebilling/companies
```

**Nota:** Esto solo es necesario si SELinux está activo y bloquea el acceso.

---

### **3. Permisos de los Archivos Creados:**

Cuando Spring Boot crea archivos nuevos, estos heredarán:

- El **usuario y grupo** del proceso que ejecuta la aplicación (deploy)
- Los **permisos** basados en la `umask` del usuario

**Verificar umask:**

```bash
sudo -u deploy umask
```

**Resultado típico:** `0022` o `0002`

- `0022`: Archivos se crean con `644` (rw-r--r--)
- `0002`: Archivos se crean con `664` (rw-rw-r--)

---

### **4. Directorios Creados Dinámicamente:**

Cuando Spring Boot crea subdirectorios como `/opt/ebilling/companies/1/`, estos se crean con:

- Propietario: `deploy` (el usuario que ejecuta la app)
- Grupo: `deploy` (el grupo del usuario)
- Permisos: Según la `umask` del usuario (normalmente `755` para directorios)

---

## 🔄 Comandos para Mantenimiento

### **Si Cambias el Usuario de la Aplicación:**

```bash
# Nuevo usuario
NEW_USER="newuser"

# Actualizar propietario
sudo chown -R $NEW_USER:$NEW_USER /opt/ebilling/companies

# Actualizar permisos
sudo chmod -R 755 /opt/ebilling/companies
```

---

### **Si Necesitas Resetear Permisos:**

```bash
# Restablecer todo
sudo chown -R deploy:deploy /opt/ebilling/companies
sudo chmod -R 755 /opt/ebilling/companies

# Para archivos específicos creados por error
sudo find /opt/ebilling/companies -type f -exec chmod 644 {} \;
sudo find /opt/ebilling/companies -type d -exec chmod 755 {} \;
```

---

### **Agregar Usuario al Grupo (si es necesario):**

```bash
# Agregar usuario al grupo deploy
sudo usermod -a -G deploy nginx

# O si quieres crear un grupo específico
sudo groupadd ebilling
sudo usermod -a -G ebilling deploy
sudo usermod -a -G ebilling nginx
sudo chgrp -R ebilling /opt/ebilling/companies
sudo chmod -R 775 /opt/ebilling/companies
```

---

## ✅ Checklist de Configuración

- [ ] Crear directorio `/opt/ebilling/companies`
- [ ] Verificar que el usuario `deploy` existe
- [ ] Cambiar propietario a `deploy:deploy`
- [ ] Configurar permisos `755` (o `750` si es más seguro)
- [ ] Verificar permisos con `ls -la`
- [ ] Probar escritura como usuario `deploy`
- [ ] Verificar que la aplicación puede crear subdirectorios
- [ ] Verificar que la aplicación puede escribir archivos
- [ ] Verificar que la aplicación puede leer archivos

---

## 📝 Resumen de Comandos Esenciales

```bash
# 1. Crear directorio
sudo mkdir -p /opt/ebilling/companies

# 2. Cambiar propietario (ajusta deploy:deploy según tu caso)
sudo chown -R deploy:deploy /opt/ebilling/companies

# 3. Configurar permisos (recomendado: 755)
sudo chmod -R 755 /opt/ebilling/companies

# 4. Verificar
ls -la /opt/ebilling/companies
```

---

## 🎯 Recomendación Final

Para producción, usa estos comandos:

```bash
# Configuración estándar y segura
sudo mkdir -p /opt/ebilling/companies
sudo chown -R deploy:deploy /opt/ebilling/companies
sudo chmod -R 755 /opt/ebilling/companies
```

**Esto dará:**

- ✅ Propietario: `deploy`
- ✅ Grupo: `deploy`
- ✅ Permisos: `755` (rwxr-xr-x)
  - Propietario: lectura, escritura, ejecución
  - Grupo: lectura, ejecución
  - Otros: lectura, ejecución

---

**Fecha de creación:** 2025-01-20  
**Última actualización:** 2025-01-20
