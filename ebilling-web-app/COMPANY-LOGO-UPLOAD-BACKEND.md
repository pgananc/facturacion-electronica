# 📸 Implementación de Carga de Logo de Compañía - Backend

## 📋 Resumen

Este documento describe la implementación del backend para subir y gestionar logos de compañías en el proyecto `ebill-customer`.

---

## 🎯 Endpoints Requeridos

### 1. **Subir Logo de Compañía**

- **Método:** `POST`
- **Ruta:** `/api/company/{idCompany}/logo`
- **Parámetros:**
  - `idCompany` (Path Variable): ID de la compañía
  - `file` (MultipartFile): Archivo de imagen del logo

### 2. **Obtener Ruta del Logo**

- **Método:** `GET`
- **Ruta:** `/api/company/{idCompany}/logo`
- **Parámetros:**
  - `idCompany` (Path Variable): ID de la compañía
- **Respuesta:** `{ "logoPath": "/home/companies/1/logo.jpg" }`

### 3. **Obtener Imagen del Logo (para mostrar en el frontend)**

- **Método:** `GET`
- **Ruta:** `/api/company/{idCompany}/logo/image`
- **Parámetros:**
  - `idCompany` (Path Variable): ID de la compañía
- **Respuesta:** Archivo de imagen (Content-Type: image/jpeg, image/png, etc.)
- **Uso:** Esta URL se puede usar directamente en el atributo `src` de una etiqueta `<img>`

### 4. **Eliminar Logo de Compañía** (ebill-customer)

- **Método:** `DELETE`
- **Ruta:** `/api/company/{idCompany}/logo`
- **Parámetros:**
  - `idCompany` (Path Variable): ID de la compañía
- **Implementación:** `ICompanyService.deleteLogo(Long idCompany)` → `CompanyServiceImpl`: borra el archivo en disco (si existe), pone `logoPath` en `null` en BD; con `@Transactional` y `@CacheEvict`. `CompanyController`: `DELETE /{idCompany}/logo`.
- **Respuesta exitosa (200 OK):** `{ "message": "Logo eliminado exitosamente" }`
- **Si la compañía no existe (500):** `{ "error": "Error al eliminar el logo: Company not found for this id :: {id}" }`
- **Si la compañía existe pero no tiene logo:** 200 con el mismo mensaje (idempotente).

**cURL:**
```bash
curl -X DELETE "http://localhost:8080/api/company/1/logo"
```

---

## 📁 Estructura de Archivos en el Servidor

Los logos se guardarán en la siguiente estructura:

```
/home/companies/
  ├── 1/
  │   └── logo.jpg
  ├── 2/
  │   └── logo.png
  └── 3/
      └── logo.png
```

**Ruta completa:** `/home/companies/{idCompany}/logo.{extension}`

---

## 💻 Implementación del Backend

### 1. Controlador REST

**Archivo:** `src/main/java/com/wallparisoft/ebill/customer/controller/CompanyController.java`

```java
package com.wallparisoft.ebill.customer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.wallparisoft.ebill.customer.service.CompanyService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    /**
     * Sube el logo de una compañía
     * POST /api/company/{idCompany}/logo
     */
    @PostMapping("/{idCompany}/logo")
    public ResponseEntity<?> uploadLogo(
            @PathVariable Long idCompany,
            @RequestParam("file") MultipartFile file) {
        try {
            String logoPath = companyService.uploadLogo(idCompany, file);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logo subido exitosamente");
            response.put("logoPath", logoPath);
            return ResponseEntity.ok().body(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al subir el logo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene la ruta del logo de una compañía
     * GET /api/company/{idCompany}/logo
     */
    @GetMapping("/{idCompany}/logo")
    public ResponseEntity<?> getLogo(@PathVariable Long idCompany) {
        try {
            String logoPath = companyService.getLogoPath(idCompany);
            if (logoPath != null && new java.io.File(logoPath).exists()) {
                Map<String, String> response = new HashMap<>();
                response.put("logoPath", logoPath);
                return ResponseEntity.ok().body(response);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al obtener el logo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene la imagen del logo de una compañía (para mostrar en el frontend)
     * GET /api/company/{idCompany}/logo/image
     */
    @GetMapping("/{idCompany}/logo/image")
    public ResponseEntity<?> getLogoImage(@PathVariable Long idCompany) {
        try {
            String logoPath = companyService.getLogoPath(idCompany);
            if (logoPath != null) {
                java.io.File logoFile = new java.io.File(logoPath);
                if (logoFile.exists()) {
                    // Determinar el tipo de contenido basado en la extensión
                    String contentType = "image/jpeg"; // Por defecto
                    String fileName = logoFile.getName().toLowerCase();
                    if (fileName.endsWith(".png")) {
                        contentType = "image/png";
                    } else if (fileName.endsWith(".gif")) {
                        contentType = "image/gif";
                    } else if (fileName.endsWith(".webp")) {
                        contentType = "image/webp";
                    }

                    // Leer el archivo y retornarlo como respuesta
                    byte[] imageBytes = java.nio.file.Files.readAllBytes(logoFile.toPath());
                    return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .body(imageBytes);
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

### 2. Servicio

**Archivo:** `src/main/java/com/wallparisoft/ebill/customer/service/CompanyService.java`

```java
package com.wallparisoft.ebill.customer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.repository.CompanyRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    // Ruta base donde se guardan los logos
    private static final String LOGO_BASE_PATH = "/home/companies";

    /**
     * Sube el logo de una compañía
     * Guarda el archivo en: /home/companies/{idCompany}/logo.{extension}
     *
     * @param idCompany ID de la compañía
     * @param file Archivo de imagen
     * @return Ruta completa del archivo guardado
     * @throws IOException Si hay error al guardar el archivo
     * @throws IllegalArgumentException Si la compañía no existe o el archivo es inválido
     */
    public String uploadLogo(Long idCompany, MultipartFile file) throws IOException {
        // Validar que la compañía existe
        Company company = companyRepository.findById(idCompany)
            .orElseThrow(() -> new IllegalArgumentException("Compañía no encontrada: " + idCompany));

        // Validar que el archivo no esté vacío
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        // Validar tipo de archivo (solo imágenes)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        // Validar tamaño del archivo (máximo 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("El archivo no debe exceder 5MB");
        }

        // Obtener extensión del archivo
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            // Si no tiene extensión, usar la extensión basada en el content type
            if (contentType != null) {
                if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
                    extension = ".jpg";
                } else if (contentType.equals("image/png")) {
                    extension = ".png";
                } else if (contentType.equals("image/gif")) {
                    extension = ".gif";
                } else if (contentType.equals("image/webp")) {
                    extension = ".webp";
                } else {
                    extension = ".jpg"; // Por defecto
                }
            } else {
                extension = ".jpg";
            }
        }

        // Crear directorio si no existe: /home/companies/{idCompany}
        String companyDir = LOGO_BASE_PATH + "/" + idCompany;
        Path companyPath = Paths.get(companyDir);
        if (!Files.exists(companyPath)) {
            Files.createDirectories(companyPath);
        }

        // Eliminar logo anterior si existe (buscar cualquier archivo que empiece con "logo.")
        File dir = companyPath.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] existingLogos = dir.listFiles((d, name) -> name.startsWith("logo."));
            if (existingLogos != null) {
                for (File existingLogo : existingLogos) {
                    existingLogo.delete();
                }
            }
        }

        // Nombre del archivo: logo.{extension}
        String fileName = "logo" + extension;
        Path filePath = companyPath.resolve(fileName);

        // Guardar el archivo
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Retornar la ruta absoluta del archivo guardado
        return filePath.toString(); // Retorna: /home/companies/{idCompany}/logo.{extension}
    }

    /**
     * Obtiene la ruta del logo de una compañía
     *
     * @param idCompany ID de la compañía
     * @return Ruta completa del logo o null si no existe
     */
    public String getLogoPath(Long idCompany) {
        String companyDir = LOGO_BASE_PATH + "/" + idCompany;
        Path companyPath = Paths.get(companyDir);

        // Buscar archivo logo con cualquier extensión
        File dir = companyPath.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.startsWith("logo."));
            if (files != null && files.length > 0) {
                return files[0].getAbsolutePath();
            }
        }

        return null;
    }
}
```

### 3. Configuración de Multipart (si es necesario)

**Archivo:** `src/main/resources/application.properties`

```properties
# Configuración para subida de archivos
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

---

## 🔒 Permisos del Sistema de Archivos

**IMPORTANTE:** Asegúrate de que el directorio `/home/companies` tenga los permisos correctos:

```bash
# Crear el directorio si no existe
sudo mkdir -p /home/companies

# Dar permisos al usuario que ejecuta la aplicación Spring Boot
sudo chown -R usuario:grupo /home/companies
sudo chmod -R 755 /home/companies
```

Reemplaza `usuario` y `grupo` con el usuario y grupo que ejecuta tu aplicación Spring Boot.

---

## 📝 Notas Importantes

1. **Ruta del servidor:** Los logos se guardan en `/home/companies/{idCompany}/logo.{extension}` en el servidor Linux.

2. **Validaciones:**

   - Solo se aceptan archivos de imagen (image/\*)
   - Tamaño máximo: 5MB
   - El archivo anterior se reemplaza si existe

3. **Seguridad:**

   - Validar que la compañía existe antes de subir el logo
   - Validar tipo y tamaño del archivo
   - Usar rutas absolutas para evitar problemas de seguridad

4. **Frontend:** El frontend ya está implementado y espera estos endpoints.

---

## 🧪 Pruebas

### Subir Logo

```bash
curl -X POST http://localhost:8180/api/company/1/logo \
  -F "file=@/ruta/al/logo.jpg"
```

### Obtener Ruta del Logo

```bash
curl http://localhost:8180/api/company/1/logo
```

---

## ✅ Checklist de Implementación

- [ ] Crear el método `uploadLogo()` en `CompanyService`
- [ ] Crear el método `getLogoPath()` en `CompanyService`
- [ ] Agregar endpoints en `CompanyController`
- [ ] Configurar permisos del directorio `/home/companies`
- [ ] Probar subida de logo
- [ ] Probar obtención de ruta del logo
- [ ] Validar que se elimina el logo anterior al subir uno nuevo

---

**Fecha de creación:** 2025-01-20  
**Última actualización:** 2025-01-20
