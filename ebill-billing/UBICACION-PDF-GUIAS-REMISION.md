# 📄 Ubicación de PDFs de Guías de Remisión

## 📍 Ruta de Almacenamiento

Los PDFs de las guías de remisión se guardan en la siguiente estructura de directorios:

### **Ruta Base Configurada**

```
/opt/ebilling/guides
```

Esta ruta se configura en la tabla `params` con el código:
- **Parámetro:** `GUIDE_STORAGE_BASE_DIR`
- **Valor por defecto:** `/opt/ebilling/guides`

### **Estructura de Directorios**

Los PDFs se organizan por cliente, año, mes y día:

```
/opt/ebilling/guides/
  └── {identificacion_cliente}/
      └── {año}/
          └── {mes}/
              └── {día}/
                  ├── {clave_acceso}.xml    (XML autorizado del SRI)
                  └── GR-{clave_acceso}.pdf (PDF generado)
```

### **Ejemplo Real**

Para una guía con:
- **Cliente:** 0987654321
- **Fecha de autorización:** 28 de enero de 2025
- **Clave de acceso:** 28012025123456789012345678901234567890123456789

La ruta completa sería:

```
/opt/ebilling/guides/
  └── 0987654321/
      └── 2025/
          └── 01/
              └── 28/
                  ├── 28012025123456789012345678901234567890123456789.xml
                  └── GR-28012025123456789012345678901234567890123456789.pdf
```

## 🔍 Cómo Consultar la Ruta desde la Base de Datos

### **1. Consultar el parámetro de configuración:**

```sql
SELECT code_param, value_param, description_param
FROM params
WHERE code_param = 'GUIDE_STORAGE_BASE_DIR';
```

### **2. Consultar la ruta de almacenamiento de una guía específica:**

```sql
SELECT 
    g.id_guide,
    g.guide_number,
    a.authorization_storage_path,
    a.access_key,
    a.authorization_date
FROM guide g
JOIN authorization_sri a ON g.id_authorization_sri = a.id_authorization_sri
WHERE g.id_guide = {id_guide};
```

El campo `authorization_storage_path` contiene la ruta del directorio donde se guardaron los archivos.

## 📥 Descargar PDF desde la API

### **Endpoint para descargar PDF:**

```
GET /api/guides/{id}/pdf
```

**Ejemplo:**
```bash
curl -X GET http://localhost:8480/api/guides/1/pdf \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  --output guia-remision.pdf
```

Este endpoint:
1. Busca la guía por ID
2. Obtiene la ruta de almacenamiento desde `authorization_storage_path`
3. Lee el archivo PDF desde el sistema de archivos
4. Retorna el PDF como respuesta HTTP con `Content-Type: application/pdf`

## 🔧 Configuración

### **Cambiar la ruta de almacenamiento:**

```sql
UPDATE params
SET value_param = '/nueva/ruta/guides'
WHERE code_param = 'GUIDE_STORAGE_BASE_DIR';
```

**⚠️ Nota:** Después de cambiar la ruta, las nuevas guías se guardarán en la nueva ubicación. Las guías anteriores permanecerán en la ruta original.

## 📋 Resumen

| Elemento | Valor |
|----------|-------|
| **Ruta Base** | `/opt/ebilling/guides` |
| **Parámetro BD** | `GUIDE_STORAGE_BASE_DIR` |
| **Formato PDF** | `GR-{clave_acceso}.pdf` |
| **Formato XML** | `{clave_acceso}.xml` |
| **Estructura** | `{cliente}/{año}/{mes}/{día}/` |
| **Endpoint API** | `GET /api/guides/{id}/pdf` |

## 🚀 Flujo de Generación

1. **Guía creada** → Se envía al SRI
2. **SRI autoriza** → Se guarda XML en `{base_dir}/{cliente}/{año}/{mes}/{día}/{clave_acceso}.xml`
3. **PDF generado** → Se guarda en `{base_dir}/{cliente}/{año}/{mes}/{día}/GR-{clave_acceso}.pdf`
4. **Email enviado** → Se adjunta el PDF al correo del cliente

## 🔐 Permisos Requeridos

El directorio debe tener permisos de escritura para el usuario que ejecuta la aplicación:

```bash
sudo mkdir -p /opt/ebilling/guides
sudo chown -R {usuario_app}:{grupo_app} /opt/ebilling/guides
sudo chmod -R 755 /opt/ebilling/guides
```




