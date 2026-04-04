# 🔍 Debug: Error 401 al guardar factura

## ✅ Lo que sabemos hasta ahora:

### 1. Token JWT existe en sessionStorage

```javascript
{
  exists: true,
  length: 308,
  preview: 'eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJzdXBlcl9hZG1pb...'
}
```

### 2. Configuración de JwtModule

```typescript
JwtModule.forRoot({
  config: {
    tokenGetter: tokenGetter,
    allowedDomains: [
      "localhost:8180", // ✅ Customers
      "localhost:8280", // ✅ Auth
      "localhost:8380", // ✅ Products
      "localhost:8480", // ✅ E-Billing
    ],
    disallowedRoutes: ["http://localhost:8280/api/mail/send"],
  },
});
```

### 3. InvoiceService está simplificado

```typescript
register(invoice: InvoiceDto): Observable<any> {
  return this.http.post(`${this.baseUrl}/register`, invoice);
  // JwtModule debería agregar el header automáticamente
}
```

## ❌ Problema: Backend devuelve 401

Posibles causas:

1. **JwtModule no está agregando el header para localhost:8480**
2. **Backend de E-Billing usa un JWT secret diferente**
3. **Backend de E-Billing requiere configuración adicional**

---

## 🔍 Pasos de diagnóstico:

### Paso 1: Verificar si el header Authorization se está enviando

**Instrucciones:**

1. Abre Chrome DevTools (F12)
2. Ve a la pestaña **Network**
3. Busca la petición a `localhost:8480/api/invoice/register`
4. Click en la petición
5. Ve a la pestaña **Headers**
6. Busca en **Request Headers**: `Authorization: Bearer ...`

**Resultado esperado:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6...
```

**Si NO está presente:**

- JwtModule no está funcionando para este dominio
- Solución: Agregar el header manualmente

**Si SÍ está presente:**

- El problema está en el backend de E-Billing
- Verificar la configuración del JWT secret en el backend

---

### Paso 2: Comparar con peticiones exitosas a otros microservicios

**Peticiones que funcionan:**

- `localhost:8180/api/company/...` (Customers) ✅
- `localhost:8280/api/...` (Auth) ✅
- `localhost:8380/api/product/...` (Products) ✅

**Comparar:**

1. Headers de petición exitosa a Products
2. Headers de petición fallida a E-Billing
3. ¿Hay diferencias?

---

### Paso 3: Verificar configuración del backend

**En el microservicio E-Billing, verificar:**

```java
// application.properties o application.yml
jwt.secret=TU_SECRET_KEY_AQUI
jwt.expiration=3600000

// Security Config
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // ¿Está configurado para aceptar JWT?
    // ¿Usa el mismo secret que el servicio Auth?
}
```

**El JWT secret DEBE ser el mismo en TODOS los microservicios:**

- Auth Microservice: `jwt.secret=XXXX`
- E-Billing Microservice: `jwt.secret=XXXX` ← **DEBE SER IGUAL**

---

## 🚀 Solución temporal (si JwtModule no funciona)

Si JwtModule no está agregando el header, podemos hacerlo manualmente:

```typescript
// invoice.service.ts
private getAuthHeaders(): HttpHeaders {
  const token = sessionStorage.getItem(environment.TOKEN);
  return new HttpHeaders({
    'Content-Type': 'application/json',
    'Authorization': token ? `Bearer ${token}` : '',
  });
}

register(invoice: InvoiceDto): Observable<any> {
  return this.http.post(`${this.baseUrl}/register`, invoice, {
    headers: this.getAuthHeaders()
  });
}
```

---

## 📋 Próximos pasos:

1. **Revisar Network tab** y confirmar si el header Authorization se está enviando
2. **Compartir screenshot o copiar los Request Headers** de la petición fallida
3. **Verificar configuración del backend E-Billing** (jwt.secret)
4. **Si JwtModule no funciona**, aplicar solución temporal
