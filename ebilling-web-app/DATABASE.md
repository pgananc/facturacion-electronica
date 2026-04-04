# 🗄️ Guía de Base de Datos en Cursor IDE

> **Para desarrolladores Backend acostumbrados a IntelliJ/DataGrip**

---

## 🎯 **LA REALIDAD:**

❌ **Cursor/VS Code NO tiene visor de DB nativo** como IntelliJ Ultimate  
✅ **PERO hay extensiones que lo hacen igual o mejor**

---

## 🚀 **OPCIÓN 1: SQLTools (Recomendada dentro de Cursor)** ⭐

### **¿Qué hace?**

- ✅ Conectar a múltiples bases de datos
- ✅ Ejecutar queries con autocompletado
- ✅ Ver tablas, vistas, schemas
- ✅ Exportar resultados a CSV/JSON
- ✅ Formatear SQL automáticamente
- ✅ Historial de queries

### **Instalación:**

#### **Paso 1: Instalar extensiones**

```
1. Abre Cursor
2. Presiona: Cmd+Shift+X (Extensions)
3. Busca e instala:
   - "SQLTools" (mtxr.sqltools)
   - "SQLTools PostgreSQL/MySQL" (el driver que uses)
```

O simplemente presiona **Cmd+Shift+P** y escribe:

```
Extensions: Show Recommended Extensions
```

Verás las extensiones que agregué a `extensions.json` ✅

#### **Paso 2: Configurar conexión**

Ya creé un archivo de ejemplo: `.vscode/sqltools.json`

**Edítalo con tus credenciales:**

```json
{
  "connections": [
    {
      "name": "eBilling - PostgreSQL",
      "driver": "PostgreSQL",
      "server": "localhost",
      "port": 5432,
      "database": "ebilling_db",
      "username": "tu_usuario",    ← CAMBIAR
      "password": "tu_password",   ← CAMBIAR
      "connectionTimeout": 30
    }
  ]
}
```

#### **Paso 3: Conectar**

1. Click en el ícono de **SQLTools** en la barra lateral izquierda (🗄️)
2. Verás tus conexiones
3. Click derecho → **Connect**
4. ¡Listo! Ya puedes ver tablas, ejecutar queries, etc.

### **Uso diario:**

```sql
-- Ejecutar query: Cmd+E Cmd+E
SELECT * FROM users WHERE status = true;

-- Ver resultados en formato tabla
-- Exportar a CSV/JSON
-- Autocompletado de tablas y columnas
```

---

## 🏆 **OPCIÓN 2: DBeaver (Standalone - Mejor opción)** ⭐⭐⭐

**Esta es MI RECOMENDACIÓN si vienes de IntelliJ/DataGrip**

### **¿Por qué DBeaver?**

- ✅ **Gratuito** (no como DataGrip que es de pago)
- ✅ **Igual de poderoso** que DataGrip
- ✅ **Soporta TODO:** PostgreSQL, MySQL, MongoDB, Oracle, SQL Server, etc.
- ✅ **Interfaz idéntica** a herramientas JetBrains
- ✅ **ER Diagrams automáticos**
- ✅ **Importar/Exportar datos**
- ✅ **SQL Formatter, Autocomplete, Debug**

### **Instalación:**

#### **macOS:**

```bash
brew install --cask dbeaver-community
```

#### **Descarga directa:**

https://dbeaver.io/download/

### **Ventajas sobre extensiones de Cursor:**

- 🚀 Más rápido con DBs grandes
- 🎨 Mejor UI/UX
- 📊 ER Diagrams visuales
- 🔧 Más features avanzadas
- 💾 Mejor para backups/restore

---

## 🛠️ **OPCIÓN 3: Otras herramientas standalone**

### **TablePlus** (Recomendada para Mac)

- 💰 Paga pero muy bonita
- ⚡ Muy rápida
- 🎨 UI nativa de macOS
- https://tableplus.com/

### **pgAdmin** (Para PostgreSQL)

- ✅ Oficial de PostgreSQL
- ✅ Gratuita
- ⚠️ UI un poco anticuada
- https://www.pgadmin.org/

### **MySQL Workbench** (Para MySQL)

- ✅ Oficial de MySQL
- ✅ Gratuita
- https://www.mysql.com/products/workbench/

---

## 🔄 **COMPARACIÓN: IntelliJ vs Cursor**

| Feature                | IntelliJ Ultimate | Cursor + SQLTools | DBeaver       |
| ---------------------- | ----------------- | ----------------- | ------------- |
| **Integrado en IDE**   | ✅ Sí             | ✅ Sí             | ❌ Standalone |
| **Gratis**             | ❌ Paga           | ✅ Gratis         | ✅ Gratis     |
| **Autocompletado SQL** | ✅                | ✅                | ✅            |
| **ER Diagrams**        | ✅                | ❌                | ✅            |
| **Multiple DBs**       | ✅                | ✅                | ✅            |
| **Query History**      | ✅                | ✅                | ✅            |
| **Export/Import**      | ✅                | ⚠️ Limitado       | ✅            |
| **Performance**        | ⭐⭐⭐⭐          | ⭐⭐⭐            | ⭐⭐⭐⭐⭐    |

---

## 💡 **MI RECOMENDACIÓN PARA TI:**

Como vienes de IntelliJ y estás acostumbrado a **tener todo integrado**, te recomiendo:

### **Setup Ideal:**

1. **Para desarrollo rápido:**

   - Usa **SQLTools en Cursor** (ya está configurado)
   - Queries simples, ver datos, ejecutar scripts

2. **Para trabajo pesado de DB:**
   - Usa **DBeaver** (descargado aparte)
   - Diseño de tablas, ER diagrams, migraciones, backups

### **Ventajas de este combo:**

- ✅ No sales de Cursor para cosas simples
- ✅ Tienes DBeaver para cosas complejas
- ✅ Ambos son gratuitos
- ✅ Similar a tener IntelliJ + DataGrip

---

## 🎬 **PRUEBA RÁPIDA (SQLTools):**

### **1. Instalar extensión:**

```
Cmd+Shift+X → Buscar "SQLTools"
```

### **2. Crear conexión:**

```
Cmd+Shift+P → "SQLTools: Add New Connection"
```

### **3. Configurar:**

```
Driver: PostgreSQL (o el que uses)
Server: localhost
Port: 5432
Database: ebilling_db
Username: postgres
Password: tu_password
```

### **4. Conectar:**

```
Click en ícono SQLTools (sidebar) → Connect
```

### **5. Ejecutar query:**

```sql
-- Crear un nuevo archivo .sql
-- Escribir tu query
SELECT * FROM users LIMIT 10;

-- Ejecutar: Cmd+E Cmd+E
```

---

## 🗂️ **ESTRUCTURA RECOMENDADA:**

```
ebilling-web-app/
├── .vscode/
│   └── sqltools.json        ← Configuraciones de DB
├── sql/
│   ├── migrations/          ← Scripts de migración
│   ├── seeds/               ← Datos de prueba
│   └── queries/             ← Queries frecuentes
└── DATABASE.md              ← Esta guía
```

---

## 📝 **QUERIES ÚTILES PARA TU PROYECTO:**

### **Ver todas las tablas:**

```sql
-- PostgreSQL
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public';

-- MySQL
SHOW TABLES;
```

### **Ver estructura de una tabla:**

```sql
-- PostgreSQL
\d users

-- MySQL
DESCRIBE users;
```

### **Ver usuarios y sus compañías:**

```sql
SELECT
  u.id_user,
  u.user_name,
  u.mail,
  c.name as company_name
FROM users u
LEFT JOIN user_company uc ON u.id_user = uc.id_user
LEFT JOIN companies c ON uc.id_company = c.id_company
WHERE u.status = true;
```

---

## 🔐 **SEGURIDAD: NO SUBIR CREDENCIALES**

Ya agregué `.vscode/sqltools.json` al `.gitignore`. Pero asegúrate:

```bash
# Agregar a .gitignore:
echo ".vscode/sqltools.json" >> .gitignore
echo "**/*.db" >> .gitignore
```

---

## 🆘 **PROBLEMAS COMUNES:**

### **1. "Cannot connect to database"**

```bash
# Verifica que el servidor de DB esté corriendo:
# PostgreSQL:
brew services list | grep postgresql

# MySQL:
brew services list | grep mysql

# Iniciar:
brew services start postgresql
brew services start mysql
```

### **2. "Authentication failed"**

- Verifica usuario/contraseña en `sqltools.json`
- Verifica que el usuario tenga permisos

### **3. "Driver not found"**

- Instala el driver específico desde Extensions

---

## 🎯 **RESUMEN RÁPIDO:**

| Necesidad           | Herramienta     | Instalación            |
| ------------------- | --------------- | ---------------------- |
| **Queries rápidas** | SQLTools        | Extensión en Cursor    |
| **Todo en uno**     | DBeaver         | `brew install dbeaver` |
| **PostgreSQL only** | pgAdmin         | https://pgadmin.org    |
| **MySQL only**      | MySQL Workbench | https://mysql.com      |
| **Mac nativa**      | TablePlus       | https://tableplus.com  |

---

**¿Dudas?** Pregunta en el canal de #backend o consulta con el equipo 💪
