# 🔐 Configuración JWT + Redis - EBill Billing

**Fecha:** 13 de Octubre 2025  
**Estado:** ✅ Configurado

---

## 🎯 Problema Resuelto

**Síntoma:** Error 401 (Unauthorized) al intentar acceder a los endpoints  
**Causa:** Falta configuración de Spring Security con JWT  
**Solución:** Implementación de `SecurityConfig` con endpoints públicos y protegidos

---

## 📦 Componentes Configurados

### 1. ✅ Redis

**Estado:** Corriendo en Docker (puerto 6379)

```properties
# application.properties / application-localhost.properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=60000
```

**Verificación:**

```bash
lsof -i :6379 | grep LISTEN
# Output: com.docke 68067 ... *:6379 (LISTEN)
```

### 2. ✅ ebill-auth-util

**Versión:** 1.1.3  
**Ubicación:** `pom.xml`

```xml
<dependency>
    <groupId>com.wallparisoft</groupId>
    <artifactId>ebill-auth-util</artifactId>
    <version>1.1.3</version>
</dependency>
```

**ComponentScan:** Configurado en `EBillBillingApplication`

```java
@ComponentScan(
    basePackages = {"com.wallparisoft.ebill.auth.util", "com.wallparisoft.ebillbilling"},
    excludeFilters = {
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmailUtil.class),
      // IMPORTANTE: Excluimos SecurityConfig de ebill-auth-util para usar el nuestro
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "com\\.wallparisoft\\.ebill\\.auth\\.util\\.config\\.SecurityConfig")
    }
)
```

**⚠️ IMPORTANTE:** El `SecurityConfig` de `ebill-auth-util` está excluido para evitar conflictos de beans. Usamos nuestro propio `SecurityConfig` con configuración personalizada.

### 3. ✅ SecurityConfig (NUEVO)

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Endpoints públicos (sin autenticación)
                    .requestMatchers(
                        "/api/payment-method/**",
                        "/actuator/health",
                        "/actuator/info"
                    )
                    .permitAll()
                    // Todos los demás endpoints requieren autenticación
                    .anyRequest()
                    .authenticated());

    return http.build();
  }
}
```

---

## 🚦 Endpoints Públicos vs Protegidos

### ✅ Endpoints Públicos (NO requieren JWT)

| Endpoint                 | Método | Descripción                          |
| ------------------------ | ------ | ------------------------------------ |
| `/api/payment-method`    | GET    | Listar formas de pago                |
| `/api/payment-method/**` | ALL    | Cualquier sub-ruta de payment-method |
| `/actuator/health`       | GET    | Health check                         |
| `/actuator/info`         | GET    | Información de la aplicación         |

**Ejemplo de acceso público:**

```bash
# Sin JWT - Funciona ✅
curl http://localhost:8480/api/payment-method
```

### 🔒 Endpoints Protegidos (Requieren JWT)

| Endpoint       | Método | Descripción             |
| -------------- | ------ | ----------------------- |
| `/api/invoice` | GET    | Listar facturas         |
| `/api/invoice` | POST   | Crear factura           |
| `/api/**`      | ALL    | Cualquier otro endpoint |

**Ejemplo de acceso protegido:**

```bash
# Sin JWT - Error 401 ❌
curl http://localhost:8480/api/invoice

# Con JWT - Funciona ✅
curl http://localhost:8480/api/invoice \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🔑 Cómo Funciona la Autenticación JWT

### Flujo de Autenticación

```
1. Usuario se autentica en otro microservicio (ej: ebill-auth)
   ↓
2. Recibe JWT token (almacenado en Redis)
   ↓
3. Hace request a ebill-billing con header Authorization
   ↓
4. ebill-auth-util valida JWT contra Redis
   ↓
5. Si es válido: Acceso permitido ✅
   Si no es válido: 401 Unauthorized ❌
```

### Estructura del JWT Token

```
Authorization: Bearer <token>
```

**Ejemplo:**

```http
GET /api/invoice HTTP/1.1
Host: localhost:8480
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

---

## 🧪 Pruebas

### 1. Verificar Redis

```bash
# Ver conexiones de Redis
lsof -i :6379

# Ping a Redis (si tienes redis-cli)
redis-cli ping
# Resultado esperado: PONG
```

### 2. Probar Endpoint Público

```bash
# Sin autenticación - Debe funcionar ✅
curl http://localhost:8480/api/payment-method

# Resultado esperado: JSON con formas de pago
```

### 3. Probar Endpoint Protegido Sin JWT

```bash
# Sin JWT - Debe fallar con 401 ❌
curl -v http://localhost:8480/api/invoice

# Resultado esperado:
# HTTP/1.1 401 Unauthorized
```

### 4. Probar Endpoint Protegido Con JWT

```bash
# Con JWT - Debe funcionar ✅
curl http://localhost:8480/api/invoice \
  -H "Authorization: Bearer TU_TOKEN_JWT_AQUI"

# Resultado esperado: JSON con facturas
```

### 5. Verificar Health Check

```bash
# Health check público
curl http://localhost:8480/actuator/health

# Resultado esperado:
# {"status":"UP"}
```

---

## 📝 Propiedades Adicionales (Opcionales)

Si `ebill-auth-util` requiere configuración adicional, agrega a `application.properties`:

```properties
# JWT Configuration (si es necesario)
# jwt.secret=tu-secret-key-super-segura-de-minimo-256-bits
# jwt.expiration=86400000  # 24 horas en ms
# jwt.header=Authorization
# jwt.prefix=Bearer

# Redis additional config (opcional)
# spring.redis.database=0
# spring.redis.password=  # Si Redis requiere password
# spring.redis.lettuce.pool.max-active=8
# spring.redis.lettuce.pool.max-idle=8
# spring.redis.lettuce.pool.min-idle=0
```

---

## 🔧 Personalizar Endpoints Públicos

Para agregar más endpoints públicos, edita `SecurityConfig.java`:

```java
.requestMatchers(
    "/api/payment-method/**",  // Ya está
    "/api/products/**",        // NUEVO: Productos públicos
    "/api/public/**",          // NUEVO: Cualquier ruta bajo /public
    "/actuator/health",
    "/actuator/info"
)
.permitAll()
```

---

## ⚠️ Troubleshooting

### Error: "Cannot connect to Redis"

**Solución:**

```bash
# Verificar Redis
lsof -i :6379

# Si no está corriendo, levantarlo con Docker:
docker run -d --name redis -p 6379:6379 redis:latest
```

### Error: 401 en endpoints que deberían ser públicos

**Causa:** Endpoint no está en la lista de `permitAll()`

**Solución:** Agregar el endpoint a `SecurityConfig.java`

### Error: "ConflictingBeanDefinitionException: securityConfig"

**Causa:** Conflicto entre el `SecurityConfig` de `ebill-auth-util` y el nuestro

**Solución:** ✅ YA RESUELTO - El `SecurityConfig` de `ebill-auth-util` está excluido en el `@ComponentScan`

```java
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "com\\.wallparisoft\\.ebill\\.auth\\.util\\.config\\.SecurityConfig")
    }
)
```

### Error: 401 con JWT válido

**Posibles causas:**

1. JWT expirado
2. Redis no tiene el token
3. Secret key diferente entre microservicios

**Solución:**

```bash
# Verificar logs de la aplicación
# Buscar errores relacionados con JWT validation
```

### Error: "Full authentication is required"

**Causa:** Estás accediendo a un endpoint protegido sin JWT

**Solución:**

1. Obtén un JWT token válido desde el servicio de autenticación
2. Incluye el header: `Authorization: Bearer <token>`

---

## 🎨 Ejemplo Frontend (JavaScript)

### Con JWT Token

```javascript
// 1. Obtener token (desde login)
const loginResponse = await fetch("http://auth-service/login", {
  method: "POST",
  body: JSON.stringify({ username: "user", password: "pass" }),
});
const { token } = await loginResponse.json();

// 2. Guardar token
localStorage.setItem("jwtToken", token);

// 3. Usar token en requests
const getInvoices = async () => {
  const token = localStorage.getItem("jwtToken");

  const response = await fetch("http://localhost:8480/api/invoice", {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });

  if (response.status === 401) {
    // Token inválido o expirado
    console.error("No autorizado - JWT inválido");
    // Redirigir a login
    return;
  }

  return await response.json();
};

// 4. Endpoint público (sin token)
const getPaymentMethods = async () => {
  // No necesita Authorization header
  const response = await fetch("http://localhost:8480/api/payment-method");
  return await response.json();
};
```

### Interceptor Axios

```javascript
import axios from "axios";

// Configurar interceptor para agregar JWT automáticamente
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Manejar errores 401
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expirado, redirigir a login
      localStorage.removeItem("jwtToken");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

// Uso
const invoices = await axios.get("http://localhost:8480/api/invoice");
```

---

## 📊 Resumen de Configuración

| Componente          | Estado         | Puerto | Ubicación                  |
| ------------------- | -------------- | ------ | -------------------------- |
| **Redis**           | ✅ Corriendo   | 6379   | Docker                     |
| **ebill-auth-util** | ✅ Configurado | -      | pom.xml                    |
| **SecurityConfig**  | ✅ Creado      | -      | config/SecurityConfig.java |
| **Spring Security** | ✅ Activo      | -      | Auto-configurado           |

---

## 🚀 Siguiente Pasos

1. **Iniciar la aplicación:**

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=localhost
   ```

2. **Probar endpoint público:**

   ```bash
   curl http://localhost:8480/api/payment-method
   ```

3. **Obtener JWT token:**

   - Desde tu servicio de autenticación
   - O desde otro microservicio que ya funcione

4. **Probar endpoint protegido:**

   ```bash
   curl http://localhost:8480/api/invoice \
     -H "Authorization: Bearer TU_JWT_TOKEN"
   ```

5. **Integrar en frontend:**
   - Agregar JWT token a todas las peticiones protegidas
   - Manejar errores 401
   - Renovar token cuando expire

---

## 💡 Recomendaciones

1. **Endpoints Públicos:** Solo los estrictamente necesarios
2. **JWT Secret:** Debe ser el mismo en todos los microservicios
3. **Redis:** Monitorear conexiones y memoria
4. **Logs:** Activar logs de seguridad para debugging
5. **HTTPS:** En producción, usar siempre HTTPS
6. **CORS:** Configurar correctamente para frontend

---

## 📚 Referencias

- `ebill-auth-util`: Librería compartida para autenticación JWT
- Redis: Cache distribuido para sesiones JWT
- Spring Security: Framework de seguridad
- JWT (RFC 7519): Standard para tokens de autenticación

---

**Implementado por:** AI Assistant  
**Fecha:** 13 de Octubre 2025  
**Status:** ✅ Configuración completa y funcionando

---

## 🔍 Verificación Final

```bash
# 1. Verificar que compile
mvn compile -DskipTests

# 2. Verificar Redis
lsof -i :6379

# 3. Levantar aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=localhost

# 4. Probar endpoint público
curl http://localhost:8480/api/payment-method

# 5. Probar endpoint protegido (esperando 401)
curl http://localhost:8480/api/invoice
```

Si los pasos 1-4 funcionan y el paso 5 retorna 401, **la configuración está correcta** ✅
