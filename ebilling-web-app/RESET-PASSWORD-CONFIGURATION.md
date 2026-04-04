# 🔐 Configuración de Reset de Contraseña

> **⚠️ IMPORTANTE:** Este documento es SOLO para el flujo de **Reset de Contraseña** de usuarios del sistema.
>
> Para la **Consulta de Facturas** (invoice-consultation), existe un flujo diferente que usa `/token-validation` (sin parámetros en la URL).

## 📋 URL que debe usar el Backend en el Email

El backend **DEBE** enviar la siguiente URL en el correo de recuperación de contraseña:

### **Desarrollo:**

```
http://localhost:4200/reset-password/{token}
```

### **Producción:**

```
https://tu-dominio.com/reset-password/{token}
```

---

## 🎯 Ejemplo de Email

```html
<html>
  <body>
    <h2>Recuperación de Contraseña</h2>
    <p>Has solicitado restablecer tu contraseña.</p>
    <p>Por favor, haz clic en el siguiente enlace para continuar:</p>
    <a href="http://localhost:4200/reset-password/abc123xyz456"> Restablecer Contraseña </a>
    <p>Este enlace expirará en 24 horas.</p>
  </body>
</html>
```

---

## ⚙️ Configuración del Backend

### **Java (Spring Boot) - Ejemplo:**

```java
@Service
public class EmailService {

    @Value("${frontend.url}")
    private String frontendUrl; // http://localhost:4200 (dev) o https://tu-dominio.com (prod)

    public void sendPasswordResetEmail(String email, String token) {
        String resetUrl = frontendUrl + "/reset-password/" + token;

        String htmlContent = String.format(
            "<html><body>" +
            "<h2>Recuperación de Contraseña</h2>" +
            "<p>Has solicitado restablecer tu contraseña.</p>" +
            "<p>Por favor, haz clic en el siguiente enlace para continuar:</p>" +
            "<a href='%s'>Restablecer Contraseña</a>" +
            "<p>Este enlace expirará en 24 horas.</p>" +
            "</body></html>",
            resetUrl
        );

        sendEmail(email, "Recuperación de Contraseña", htmlContent);
    }
}
```

### **application.properties (Desarrollo):**

```properties
frontend.url=http://localhost:4200
```

### **application-prod.properties (Producción):**

```properties
frontend.url=https://tu-dominio.com
```

---

## 🔄 Rutas Alternativas Soportadas

Por compatibilidad, el frontend también acepta:

```
/token-validation/{token}  →  Redirige automáticamente a  →  /reset-password/{token}
```

**⚠️ IMPORTANTE:** Esta es solo una redirección temporal. **La URL correcta y recomendada es `/reset-password/{token}`**.

---

## 📝 Flujo Completo

1. **Usuario olvida contraseña** → Ingresa su email en `/reset-password`
2. **Frontend llama al backend** → `POST /api/mail/send` con el email
3. **Backend genera token** → Token único con expiración (ej: 24 horas)
4. **Backend envía email** → Con el link `{frontendUrl}/reset-password/{token}`
5. **Usuario hace click** → Navega a `/reset-password/{token}`
6. **Frontend valida token** → `GET /api/token/restore-password/validate/{token}`
7. **Si es válido** → Muestra formulario de nueva contraseña
8. **Usuario ingresa nueva contraseña** → `POST /api/user/reset-password` con token y nueva contraseña
9. **Backend actualiza contraseña** → Marca el token como usado
10. **Redirige a login** → Usuario puede iniciar sesión con la nueva contraseña

---

## 🧪 Para Probar

### **1. Probar envío de email:**

```bash
curl -X POST http://localhost:8280/api/mail/send \
  -H "Content-Type: application/json" \
  -d '{"email": "usuario@example.com"}'
```

### **2. Verificar el email recibido:**

- Debe contener un link como: `http://localhost:4200/reset-password/abc123xyz456`

### **3. Click en el link:**

- Debe abrir la página de cambio de contraseña
- **NO** debe mostrar "Página no existe"
- **NO** debe mostrar menú ni toolbar

### **4. Validar token:**

```bash
curl -X GET http://localhost:8280/api/token/restore-password/validate/abc123xyz456
```

### **5. Resetear contraseña:**

```bash
curl -X POST http://localhost:8280/api/user/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token": "abc123xyz456", "newPassword": "NuevaPassword123"}'
```

---

## ✅ Checklist de Configuración

- [ ] Backend envía URL correcta: `{frontendUrl}/reset-password/{token}`
- [ ] Variable `frontend.url` configurada en `application.properties`
- [ ] Token tiene expiración (recomendado: 24 horas)
- [ ] Email HTML con link clickeable
- [ ] Endpoint `/api/token/restore-password/validate/{token}` es público (sin JWT)
- [ ] Endpoint `/api/mail/send` es público (sin JWT)
- [ ] Endpoint `/api/user/reset-password` es público (sin JWT)

---

## 🐛 Troubleshooting

### **Problema: "Página no existe" al hacer click en el link**

**Causa:** El backend está usando una URL incorrecta en el email.

**Solución:**

1. Verificar qué URL se está enviando en el email
2. Actualizar la configuración del backend para usar `/reset-password/{token}`
3. Si necesitas usar otra URL, agregar una redirección en `app-routing.module.ts`

---

### **Problema: "La petición no es válida o ha expirado"**

**Causa:** El token es inválido, ya fue usado, o expiró.

**Solución:**

1. Verificar que el token en la URL sea correcto
2. Verificar que el token no haya expirado (backend debe validar fecha)
3. Solicitar un nuevo token desde `/reset-password`

---

### **Problema: Error 401 al validar token**

**Causa:** El endpoint `/api/token/restore-password/validate` requiere JWT.

**Solución:**

1. Verificar que el endpoint esté en `disallowedRoutes` del `JwtModule` (frontend)
2. Verificar que el endpoint sea público en el backend (sin `@PreAuthorize`)

---

## 📞 Contacto

Si tienes dudas sobre la configuración, consulta:

- Documentación del backend
- `src/app/app-routing.module.ts` (rutas del frontend)
- `src/app/app.module.ts` (configuración JWT)
