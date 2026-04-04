# 🔐 Configuración de Seguridad para Endpoint de Borradores

**Microservicio:** `ebill-billing`  
**Fecha:** 18 de Octubre 2025  
**Problema:** Endpoint `GET /api/invoice/draft/{id}` retorna HTTP 401 incluso con JWT válido

---

## 🚨 Problema Actual

El endpoint `GET /api/invoice/draft/{id}` está implementado pero **retorna HTTP 401 Unauthorized** cuando el frontend lo llama con un JWT válido.

**Evidencia:**

- Frontend envía JWT correctamente en el header `Authorization: Bearer {token}`
- Otros endpoints del mismo microservicio funcionan correctamente (`/api/invoice/drafts`, `/api/invoice`, etc.)
- Solo el endpoint específico de obtener un borrador por ID falla con 401

---

## ✅ Solución Requerida

Verificar y actualizar la configuración de Spring Security para permitir el acceso al endpoint.

---

## 📝 Archivo a Verificar/Modificar

**Archivo:** `SecurityConfig.java` (o el equivalente en tu proyecto)  
**Ubicación típica:** `src/main/java/com/wallparisoft/ebillbilling/config/SecurityConfig.java`

---

## 🔧 Cambio Requerido

### Opción 1: Si usas `antMatchers()`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .authorizeRequests()
                // === ENDPOINTS PÚBLICOS (sin autenticación) ===
                .antMatchers("/api/login").permitAll()
                .antMatchers("/api/health").permitAll()

                // === ENDPOINTS DE BORRADORES (requieren autenticación) ===
                .antMatchers(HttpMethod.GET, "/api/invoice/drafts").authenticated()
                .antMatchers(HttpMethod.GET, "/api/invoice/draft/**").authenticated()  // ⬅️ AGREGAR ESTA LÍNEA
                .antMatchers(HttpMethod.POST, "/api/invoice/draft").authenticated()
                .antMatchers(HttpMethod.POST, "/api/invoice/draft/*/process").authenticated()
                .antMatchers(HttpMethod.DELETE, "/api/invoice/draft/**").authenticated()

                // === OTROS ENDPOINTS DE FACTURACIÓN ===
                .antMatchers("/api/invoice/**").authenticated()

                // Cualquier otra petición requiere autenticación
                .anyRequest().authenticated()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
```

### Opción 2: Si usas `authorizeHttpRequests()` (Spring Security 5.5+)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // === ENDPOINTS PÚBLICOS ===
                .requestMatchers("/api/login").permitAll()
                .requestMatchers("/api/health").permitAll()

                // === ENDPOINTS DE BORRADORES ===
                .requestMatchers(HttpMethod.GET, "/api/invoice/drafts").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/invoice/draft/**").authenticated()  // ⬅️ AGREGAR ESTA LÍNEA
                .requestMatchers(HttpMethod.POST, "/api/invoice/draft").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/invoice/draft/*/process").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/invoice/draft/**").authenticated()

                // === OTROS ENDPOINTS ===
                .requestMatchers("/api/invoice/**").authenticated()

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## 🔍 Verificación Alternativa

Si la configuración ya está correcta, verifica también:

### 1. **JwtAuthenticationFilter**

Asegúrate de que el filtro JWT esté procesando correctamente el token:

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain)
        throws ServletException, IOException {

    String token = extractJwtFromRequest(request);

    if (token != null && jwtProvider.validateToken(token)) {
        String username = jwtProvider.getUsernameFromToken(token);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ⬇️ LOG PARA DEBUG
        logger.debug("Usuario autenticado: {} para endpoint: {}",
                     username, request.getRequestURI());
    } else {
        // ⬇️ LOG PARA DEBUG
        logger.warn("Token inválido o ausente para endpoint: {}", request.getRequestURI());
    }

    filterChain.doFilter(request, response);
}
```

### 2. **Método extractJwtFromRequest()**

Verifica que esté extrayendo correctamente el Bearer token:

```java
private String extractJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7); // Remueve "Bearer " prefix
    }

    return null;
}
```

### 3. **CORS Configuration**

Si usas un `CorsConfigurationSource`, asegúrate de permitir el header `Authorization`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:4201"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*")); // Permite todos los headers incluyendo Authorization
    configuration.setExposedHeaders(Arrays.asList("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 🧪 Cómo Probar

### 1. **Logs del Backend**

Agrega logs temporales en el controlador para verificar si la petición llega:

```java
@GetMapping("/draft/{id}")
public ResponseEntity<InvoiceDto> getDraftById(@PathVariable("id") Long idInvoice) {
    log.info("🔍 Petición recibida para borrador ID: {}", idInvoice);
    log.info("🔐 Usuario autenticado: {}",
             SecurityContextHolder.getContext().getAuthentication().getName());

    // ... resto del código
}
```

### 2. **Prueba con curl**

```bash
# Obtén el token JWT del frontend (desde sessionStorage)
TOKEN="tu_token_jwt_aqui"

# Haz la petición
curl -X GET http://localhost:8480/api/invoice/draft/21 \
  -H "Authorization: Bearer $TOKEN" \
  -v
```

**Resultado esperado:** HTTP 200 con JSON del borrador

### 3. **Prueba con Postman/Insomnia**

1. Método: `GET`
2. URL: `http://localhost:8480/api/invoice/draft/21`
3. Headers:
   - `Authorization`: `Bearer {tu_token_jwt}`
   - `Content-Type`: `application/json`

---

## 📊 Diagnóstico de Logs

### Si ves en los logs del backend:

**Caso 1: No aparece ningún log del controlador**

```
❌ La petición no está llegando al controlador
✅ Solución: Problema de configuración de seguridad (antMatchers/requestMatchers)
```

**Caso 2: Aparece log pero sin usuario autenticado**

```
❌ El filtro JWT no está procesando el token correctamente
✅ Solución: Revisar JwtAuthenticationFilter y extractJwtFromRequest()
```

**Caso 3: Aparece log con usuario pero retorna 401**

```
❌ Puede haber una validación adicional de permisos/roles en el endpoint
✅ Solución: Revisar @PreAuthorize o validaciones manuales en el método
```

**Caso 4: Error de CORS preflight**

```
❌ Configuración CORS no permite el header Authorization
✅ Solución: Actualizar CorsConfiguration para permitir todos los headers
```

---

## 🎯 Checklist de Verificación

- [ ] `SecurityConfig.java` incluye `/api/invoice/draft/**` en las rutas autenticadas
- [ ] `JwtAuthenticationFilter` extrae correctamente el Bearer token
- [ ] `JwtAuthenticationFilter` valida el token y establece el authentication en SecurityContext
- [ ] CORS permite el header `Authorization`
- [ ] No hay anotaciones `@PreAuthorize` restrictivas en el método `getDraftById()`
- [ ] El token JWT no está expirado (verificar claim `exp`)
- [ ] El usuario tiene al menos un rol válido en el token

---

## 🔗 Información Adicional

**Token JWT esperado del frontend:**

```
Header: Authorization
Value: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Claims esperados en el token:**

```json
{
  "sub": "usuario@example.com",
  "roles": ["ROLE_ADMIN", "ROLE_USER"],
  "companies": [1, 2],
  "exp": 1697654400,
  "iat": 1697650800
}
```

---

## 📞 Siguiente Paso

Una vez aplicados los cambios:

1. **Reiniciar el microservicio** `ebill-billing`
2. **Verificar los logs** al intentar acceder al endpoint
3. **Compartir los logs** si el problema persiste

**Si después de estos cambios sigue fallando, compartir:**

- ✅ Logs del backend al hacer la petición
- ✅ Configuración actual de `SecurityConfig.java`
- ✅ Implementación del `JwtAuthenticationFilter`

---

**Fin del documento**
