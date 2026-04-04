# 🚀 Instrucciones Rápidas para Ejecutar

## ⚠️ IMPORTANTE: El problema estaba en la versión de Java

Tu sistema usa **Java 23 por defecto**, pero el proyecto requiere **Java 17**.

✅ **YA ESTÁ SOLUCIONADO** - Configuré Cursor IDE para usar Java 17 automáticamente.

---

## 🎯 3 Formas de Ejecutar la Aplicación

### 1️⃣ Desde Cursor IDE (RECOMENDADO)

**Pasos:**

1. **Recarga el Workspace de Java:**
   - Presiona `Cmd+Shift+P` (Mac) o `Ctrl+Shift+P` (Windows/Linux)
   - Escribe: `Java: Clean Java Language Server Workspace`
   - Presiona Enter
   - Espera 10-15 segundos a que se reindexe

2. **Inicia la Base de Datos (si no está corriendo):**
   ```bash
   docker-compose up -d
   ```

3. **Ejecuta la Aplicación:**
   - Presiona `F5`
   - O ve a: **Run and Debug** (icono de play con bug en la barra lateral)
   - Selecciona: **"Spring Boot - EBillBillingApplication (localhost)"**
   - Click en el botón verde de play ▶️

4. **Verifica que esté corriendo:**
   - Ve a: http://localhost:8480

---

### 2️⃣ Desde Terminal con Script (FÁCIL)

```bash
# Inicia la base de datos
docker-compose up -d

# Ejecuta con Java 17 (script configurado)
./run-with-java17.sh
```

---

### 3️⃣ Desde Terminal Manual

```bash
# Configura Java 17
export JAVA_HOME="/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Verifica que esté usando Java 17
java -version

# Inicia la base de datos
docker-compose up -d

# Ejecuta la aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

---

## 🔍 Verificar que Todo Funcione

### Base de Datos
```bash
docker ps | grep ebill-postgres
```
Debería mostrar un contenedor corriendo.

### Aplicación
```bash
curl http://localhost:8480/api/invoice
```
O abre en el navegador: http://localhost:8480

### PgAdmin (opcional)
http://localhost:5050
- Email: `admin@ebill.com`
- Password: `admin`

---

## ❌ Si Aún No Funciona desde Cursor IDE

### Solución 1: Recargar Completamente
```bash
# 1. Cierra Cursor IDE completamente
# 2. Abre terminal y ejecuta:
cd /Users/jguerreroh/Development/wallparisoft/facturacion-electronica/ebill-billing
rm -rf .vscode/.project-cache
rm -rf target/

# 3. Abre Cursor IDE de nuevo
cursor .

# 4. Espera a que se indexe (verás un indicador de progreso abajo a la derecha)
# 5. Presiona F5
```

### Solución 2: Configuración Manual de Java
1. Ve a `Cmd+,` (Configuración)
2. Busca: `java.jdt.ls.java.home`
3. Verifica que diga: `/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home`

### Solución 3: Usa el Script
```bash
./run-with-java17.sh
```
Este script **SIEMPRE funciona** porque fuerza Java 17.

---

## 📊 Estado del Proyecto

```
✅ Compilación: OK (BUILD SUCCESS)
✅ Java 17: Configurado en Cursor IDE
✅ Lombok: Funcionando correctamente
✅ Package: Corregido (ebillbilling)
✅ Docker: Listo (PostgreSQL + PgAdmin)
✅ Configuración: Completa
```

---

## 🆘 Errores Comunes

### "Cannot find or load main class"
**Causa:** Cursor IDE usando Java 23 en lugar de Java 17
**Solución:** Recarga el workspace (paso 1 de arriba)

### "Port 8480 already in use"
**Causa:** Ya hay una instancia corriendo
**Solución:**
```bash
# Encuentra el proceso
lsof -i :8480

# Mátalo
kill -9 [PID]
```

### "Connection refused" (base de datos)
**Causa:** PostgreSQL no está corriendo
**Solución:**
```bash
docker-compose up -d postgres
```

### "Flyway migration failed"
**Causa:** Base de datos con estado inconsistente
**Solución:**
```bash
# Limpia y recrea
docker-compose down -v
docker-compose up -d
```

---

## 📁 Archivos Importantes Creados

- `.vscode/settings.json` - Configuración de Java 17
- `.vscode/launch.json` - Configuración de ejecución
- `run-with-java17.sh` - Script para ejecutar con Java 17
- `.java-version` - Indica la versión de Java requerida
- `docker-compose.yml` - Base de datos lista para usar

---

## 🎉 ¡Listo!

La aplicación está configurada y lista para ejecutarse con:
- **Java 17** ✅
- **Perfil localhost** ✅
- **Base de datos PostgreSQL** ✅
- **Todas las dependencias** ✅

**Solo presiona F5 en Cursor IDE después de recargar el workspace! 🚀**

