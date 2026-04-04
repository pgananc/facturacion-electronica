# Propuesta de Configuración de Rate Limiting en Base de Datos

## Resumen Ejecutivo

Esta propuesta define los parámetros que deben configurarse en la tabla `params` para implementar el rate limiting según los requisitos especificados en la tabla de endpoints.

## Estructura de Parámetros

Cada endpoint requiere configuraciones separadas para diferentes dimensiones de rate limiting:
- **Por Email**: Limita solicitudes por dirección de correo electrónico
- **Por IP**: Limita solicitudes por dirección IP del cliente
- **Por Cliente**: Limita solicitudes por identificador de cliente (cuando aplique)

## Parámetros Propuestos

### 1. `/api/mail/send` (POST) - Reset Password

**Abuso previsto:** Abuso de envío de emails

#### Parámetros por Email:
- **Código:** `MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS`
- **Valor:** `10`
- **Descripción:** Máximo número de solicitudes de envío de email de reset password permitidas por email en la ventana de tiempo

- **Código:** `MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES`
- **Valor:** `60`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por email del endpoint de envío de mail

#### Parámetros por IP:
- **Código:** `MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS`
- **Valor:** `10`
- **Descripción:** Máximo número de solicitudes de envío de email de reset password permitidas por IP en la ventana de tiempo

- **Código:** `MAIL_SEND_RATE_LIMIT_IP_WINDOW_MINUTES`
- **Valor:** `60`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por IP del endpoint de envío de mail

---

### 2. `/api/token/client` (POST) - Consulta Facturas

**Abuso previsto:** Abuso de generación de tokens

#### Parámetros por Email:
- **Código:** `TOKEN_CLIENT_RATE_LIMIT_EMAIL_MAX_REQUESTS`
- **Valor:** `5`
- **Descripción:** Máximo número de solicitudes de generación de token de cliente permitidas por email en la ventana de tiempo

- **Código:** `TOKEN_CLIENT_RATE_LIMIT_EMAIL_WINDOW_MINUTES`
- **Valor:** `60`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por email del endpoint de generación de token de cliente

#### Parámetros por IP:
- **Código:** `TOKEN_CLIENT_RATE_LIMIT_IP_MAX_REQUESTS`
- **Valor:** `20`
- **Descripción:** Máximo número de solicitudes de generación de token de cliente permitidas por IP en la ventana de tiempo

- **Código:** `TOKEN_CLIENT_RATE_LIMIT_IP_WINDOW_MINUTES`
- **Valor:** `60`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por IP del endpoint de generación de token de cliente

---

### 3. `/api/token/validate-client/generate` (POST) - Validación Token

**Abuso previsto:** Fuerza bruta de tokens

#### Parámetros por IP:
- **Código:** `TOKEN_VALIDATE_RATE_LIMIT_IP_MAX_ATTEMPTS`
- **Valor:** `10`
- **Descripción:** Máximo número de intentos de validación de token permitidos por IP en la ventana de tiempo

- **Código:** `TOKEN_VALIDATE_RATE_LIMIT_IP_WINDOW_MINUTES`
- **Valor:** `15`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por IP del endpoint de validación de token

#### Parámetros por Cliente:
- **Código:** `TOKEN_VALIDATE_RATE_LIMIT_CLIENT_MAX_ATTEMPTS`
- **Valor:** `20`
- **Descripción:** Máximo número de intentos de validación de token permitidos por cliente en la ventana de tiempo

- **Código:** `TOKEN_VALIDATE_RATE_LIMIT_CLIENT_WINDOW_MINUTES`
- **Valor:** `60`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por cliente del endpoint de validación de token

---

### 4. `/api/token/resend-client` (POST) - Reenvío de Token

**Abuso previsto:** Abuso de reenvío

#### Parámetros por Email:
- **Código:** `TOKEN_RESEND_RATE_LIMIT_EMAIL_MAX_REQUESTS`
- **Valor:** `3`
- **Descripción:** Máximo número de reenvíos de token de cliente permitidos por email en la ventana de tiempo

- **Código:** `TOKEN_RESEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES`
- **Valor:** `60`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por email del endpoint de reenvío de token

#### Parámetros por IP:
- **Código:** `TOKEN_RESEND_RATE_LIMIT_IP_MAX_REQUESTS`
- **Valor:** `10`
- **Descripción:** Máximo número de reenvíos de token de cliente permitidos por IP en la ventana de tiempo

- **Código:** `TOKEN_RESEND_RATE_LIMIT_IP_WINDOW_MINUTES`
- **Valor:** `60`
- **Descripción:** Ventana de tiempo en minutos para el rate limiting por IP del endpoint de reenvío de token

---

## Script SQL de Migración Propuesto

```sql
-- ============================================================================
-- Rate Limiting Configuration for /api/mail/send
-- ============================================================================

-- Rate limiting por email
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de envío de email de reset password permitidas por email en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por email del endpoint de envío de mail', 
    true, 
    '60'
);

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de envío de email de reset password permitidas por IP en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de envío de mail', 
    true, 
    '60'
);

-- ============================================================================
-- Rate Limiting Configuration for /api/token/client
-- ============================================================================

-- Rate limiting por email
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_EMAIL_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de generación de token de cliente permitidas por email en la ventana de tiempo', 
    true, 
    '5'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_EMAIL_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por email del endpoint de generación de token de cliente', 
    true, 
    '60'
);

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_IP_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de generación de token de cliente permitidas por IP en la ventana de tiempo', 
    true, 
    '20'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de generación de token de cliente', 
    true, 
    '60'
);

-- ============================================================================
-- Rate Limiting Configuration for /api/token/validate-client/generate
-- ============================================================================

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_IP_MAX_ATTEMPTS', 
    NOW(), 
    'Máximo número de intentos de validación de token permitidos por IP en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de validación de token', 
    true, 
    '15'
);

-- Rate limiting por cliente
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_CLIENT_MAX_ATTEMPTS', 
    NOW(), 
    'Máximo número de intentos de validación de token permitidos por cliente en la ventana de tiempo', 
    true, 
    '20'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_CLIENT_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por cliente del endpoint de validación de token', 
    true, 
    '60'
);

-- ============================================================================
-- Rate Limiting Configuration for /api/token/resend-client
-- ============================================================================

-- Rate limiting por email
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_EMAIL_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de reenvíos de token de cliente permitidos por email en la ventana de tiempo', 
    true, 
    '3'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por email del endpoint de reenvío de token', 
    true, 
    '60'
);

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_IP_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de reenvíos de token de cliente permitidos por IP en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de reenvío de token', 
    true, 
    '60'
);
```

## Resumen de Parámetros

| Endpoint | Dimensión | Código Parámetro | Valor | Ventana (min) |
|----------|-----------|-----------------|-------|---------------|
| `/api/mail/send` | Email | `MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS` | 10 | 60 |
| `/api/mail/send` | IP | `MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS` | 10 | 60 |
| `/api/token/client` | Email | `TOKEN_CLIENT_RATE_LIMIT_EMAIL_MAX_REQUESTS` | 5 | 60 |
| `/api/token/client` | IP | `TOKEN_CLIENT_RATE_LIMIT_IP_MAX_REQUESTS` | 20 | 60 |
| `/api/token/validate-client/generate` | IP | `TOKEN_VALIDATE_RATE_LIMIT_IP_MAX_ATTEMPTS` | 10 | 15 |
| `/api/token/validate-client/generate` | Cliente | `TOKEN_VALIDATE_RATE_LIMIT_CLIENT_MAX_ATTEMPTS` | 20 | 60 |
| `/api/token/resend-client` | Email | `TOKEN_RESEND_RATE_LIMIT_EMAIL_MAX_REQUESTS` | 3 | 60 |
| `/api/token/resend-client` | IP | `TOKEN_RESEND_RATE_LIMIT_IP_MAX_REQUESTS` | 10 | 60 |

**Total de parámetros a crear: 16**

## Consideraciones de Implementación

### 1. Convención de Nombres
- Se utiliza el patrón: `{ENDPOINT}_{DIMENSION}_RATE_LIMIT_{TIPO}_{PROPIEDAD}`
- Ejemplo: `MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS`

### 2. Dimensiones de Rate Limiting
- **EMAIL**: Limita por dirección de correo electrónico
- **IP**: Limita por dirección IP del cliente
- **CLIENT**: Limita por identificador de cliente (client_number)

### 3. Propiedades
- **MAX_REQUESTS / MAX_ATTEMPTS**: Número máximo de solicitudes/intentos permitidos
- **WINDOW_MINUTES**: Ventana de tiempo en minutos para el rate limiting

### 4. Valores por Defecto
Todos los parámetros tienen valores por defecto según la tabla de requisitos. Estos pueden ajustarse según necesidades operativas.

### 5. Integración con Servicios
Los servicios de rate limiting deberán:
- Leer estos parámetros desde la base de datos
- Implementar lógica separada para cada dimensión (email, IP, cliente)
- Usar Redis para almacenar contadores con TTL basado en `WINDOW_MINUTES`
- Validar ambos límites (email e IP) y aplicar el más restrictivo

### 6. Compatibilidad con Implementación Actual
La implementación actual de `ClientTokenRateLimitService` solo maneja rate limiting por email/identification. Esta propuesta extiende el modelo para soportar múltiples dimensiones.

## Próximos Pasos

1. **Revisar y aprobar** esta propuesta
2. **Crear migración Flyway** con el script SQL propuesto
3. **Implementar servicios de rate limiting** que utilicen estos parámetros
4. **Agregar enums** en `ParmsAuthEnum` para los nuevos códigos de parámetros
5. **Implementar lógica de rate limiting** en los controladores correspondientes
6. **Crear servicios genéricos** de rate limiting reutilizables para cada dimensión

## Notas Adicionales

- Los valores propuestos están basados en la tabla de requisitos proporcionada
- Se recomienda monitorear estos límites en producción y ajustarlos según el comportamiento real
- Considerar implementar alertas cuando se alcancen los límites para detectar posibles ataques
- Los valores pueden ajustarse dinámicamente en la base de datos sin necesidad de redeployar la aplicación

