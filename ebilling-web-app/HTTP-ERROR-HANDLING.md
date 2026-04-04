# 🔧 Manejo de Errores HTTP - Guía Completa

## 📋 Índice

- [Descripción General](#descripción-general)
- [Comportamiento del Interceptor](#comportamiento-del-interceptor)
- [Uso Básico](#uso-básico)
- [Manejo Personalizado de Errores](#manejo-personalizado-de-errores)
- [Ejemplos Prácticos](#ejemplos-prácticos)
- [Mejores Prácticas](#mejores-prácticas)

---

## 📖 Descripción General

El interceptor `ServerErrorsInterceptor` maneja automáticamente los errores HTTP en toda la aplicación, mostrando mensajes al usuario y redirigiendo cuando es necesario.

### **Características:**

- ✅ Manejo automático de errores HTTP (400, 401, 404, 500)
- ✅ Mensajes de error automáticos con snackbar
- ✅ Reintentos automáticos (configurables)
- ✅ Logging estructurado de errores
- ✅ Permite manejo personalizado por componente
- ✅ Propagación de errores para catch local

---

## 🔄 Comportamiento del Interceptor

### **Códigos de Estado HTTP:**

| Código  | Acción   | Navegación                     | Mensaje                  |
| ------- | -------- | ------------------------------ | ------------------------ |
| **400** | Snackbar | ❌ No                          | "Error de validación"    |
| **401** | Snackbar | ✅ `/login`                    | "Sesión expirada"        |
| **404** | Snackbar | ❌ No                          | "Recurso no encontrado"  |
| **500** | Snackbar | ❌ No                          | "Error del servidor"     |
| **0**   | Snackbar | ⚠️ `/login` (si no hay sesión) | "Servidor no disponible" |

---

## 🎯 Uso Básico

### **1. Comportamiento por defecto (Automático)**

```typescript
export class UserComponent {
  constructor(private http: HttpClient) {}

  deleteUser(id: number) {
    // El interceptor maneja automáticamente cualquier error
    this.http.delete(`/api/users/${id}`).subscribe({
      next: () => console.log("Usuario eliminado"),
      // NO es necesario manejar errores aquí
    });
  }
}
```

**Resultado:**

- ✅ Si hay error 404 → Snackbar "Recurso no encontrado"
- ✅ Si hay error 500 → Snackbar "Error del servidor"
- ✅ Todos los errores se logean automáticamente

---

## 🎨 Manejo Personalizado de Errores

### **2. Saltar el Interceptor Completamente**

Usa esto cuando quieras manejar el error sin mostrar snackbar automático.

```typescript
import { HttpSkipInterceptor } from "../_shared/http-skip-interceptor";

export class ClientComponent {
  constructor(private http: HttpClient, private snackBar: MatSnackBar) {}

  findClient(identification: string) {
    const headers = HttpSkipInterceptor.skipAll();

    this.http.get(`/api/clients/${identification}`, { headers }).subscribe({
      next: (client) => {
        console.log("Cliente encontrado:", client);
      },
      error: (err: HttpErrorResponse) => {
        // TÚ manejas el error sin snackbar automático
        if (err.status === 404) {
          this.snackBar.open("Cliente no encontrado. ¿Desea crearlo?", "Crear", {
            duration: 5000,
          });
        } else if (err.status === 400) {
          this.snackBar.open("Identificación inválida", "OK", {
            duration: 3000,
          });
        }
      },
    });
  }
}
```

**Resultado:**

- ❌ NO muestra snackbar automático del interceptor
- ✅ El componente tiene control total del error
- ✅ El error sigue logueándose

---

## 💡 Ejemplos Prácticos

### **Ejemplo 1: Buscar Producto (Manejo Silencioso)**

```typescript
export class ProductSearchComponent {
  constructor(private http: HttpClient, private logger: LoggerService) {}

  searchProduct(code: string) {
    const headers = HttpSkipInterceptor.skipAll();

    this.http.get(`/api/products/${code}`, { headers }).subscribe({
      next: (product) => {
        this.selectedProduct = product;
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          // Manejo silencioso - no mostrar error al usuario
          this.logger.debug("Producto no encontrado:", code);
          this.selectedProduct = null;
        }
      },
    });
  }
}
```

---

### **Ejemplo 2: Login (Manejo Personalizado)**

```typescript
export class LoginComponent {
  constructor(private http: HttpClient, private snackBar: MatSnackBar, private router: Router) {}

  login(username: string, password: string) {
    const headers = HttpSkipInterceptor.skipAll();

    this.http.post("/api/auth/login", { username, password }, { headers }).subscribe({
      next: (response) => {
        sessionStorage.setItem("token", response.token);
        this.router.navigate(["/welcome"]);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 401) {
          this.snackBar.open("Usuario o contraseña incorrectos", "Reintentar", { duration: 5000 });
        } else {
          this.snackBar.open("Error al iniciar sesión", "OK", { duration: 3000 });
        }
      },
    });
  }
}
```

---

### **Ejemplo 3: Guardar con Validación (Manejo Mixto)**

```typescript
export class InvoiceComponent {
  constructor(private http: HttpClient, private snackBar: MatSnackBar, private logger: LoggerService) {}

  saveInvoice(invoice: Invoice) {
    // Para 404: manejo personalizado
    // Para otros errores: dejar que el interceptor maneje
    const headers = HttpSkipInterceptor.skipAll();

    this.http.post("/api/invoices", invoice, { headers }).subscribe({
      next: (response) => {
        this.snackBar.open("Factura guardada correctamente", "OK", {
          duration: 3000,
        });
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 400) {
          // Manejo específico de validación
          const errors = err.error.errors;
          this.showValidationErrors(errors);
        } else if (err.status === 404) {
          this.snackBar.open("Cliente no encontrado", "OK", {
            duration: 3000,
          });
        } else {
          // Para otros errores, mostrar mensaje genérico
          this.snackBar.open("Error al guardar factura", "OK", {
            duration: 3000,
          });
          this.logger.error("Error al guardar factura", err);
        }
      },
    });
  }
}
```

---

### **Ejemplo 4: Carga de Datos (Automático)**

```typescript
export class CompanyListComponent implements OnInit {
  constructor(private http: HttpClient) {}

  ngOnInit() {
    // Sin headers especiales = comportamiento automático
    this.http.get("/api/companies").subscribe({
      next: (companies) => {
        this.companies = companies;
      },
      // NO manejar errores aquí - el interceptor lo hace automáticamente
    });
  }
}
```

---

## ✅ Mejores Prácticas

### **Cuándo usar el interceptor automático:**

1. ✅ Operaciones de lectura generales (listados, búsquedas)
2. ✅ Operaciones donde el error genérico es suficiente
3. ✅ Cuando quieres redirigir a login automáticamente en 401

### **Cuándo saltar el interceptor:**

1. ✅ Login/Authentication (manejo personalizado de credenciales)
2. ✅ Búsquedas donde 404 es esperado y no es error
3. ✅ Validaciones donde quieres mostrar errores específicos
4. ✅ Operaciones donde el mensaje genérico no es apropiado

### **Convención de Código:**

```typescript
// ❌ MAL: Duplicar manejo de errores
this.http.get(url).subscribe({
  error: (err) => {
    // El interceptor ya mostró un snackbar
    this.snackBar.open("Error", "OK"); // Duplicado!
  },
});

// ✅ BIEN: Saltar interceptor si vas a manejar el error
const headers = HttpSkipInterceptor.skipAll();
this.http.get(url, { headers }).subscribe({
  error: (err) => {
    this.snackBar.open("Mensaje personalizado", "OK");
  },
});

// ✅ MEJOR: Dejar que el interceptor maneje si no necesitas lógica especial
this.http.get(url).subscribe({
  next: (data) => this.processData(data),
  // Sin error handler - el interceptor lo maneja
});
```

---

## 🔍 Debugging

### **Ver errores en consola:**

```typescript
// Los errores siempre se loguean, incluso con skip interceptor
this.logger.error("Error personalizado", err);
```

### **Verificar si el interceptor fue saltado:**

```typescript
const headers = HttpSkipInterceptor.skipAll();
console.log(HttpSkipInterceptor.hasSkipHeader(headers)); // true
```

---

## 📊 Comparación con Spring Boot

| Spring Boot                  | Angular (Interceptor)     |
| ---------------------------- | ------------------------- |
| `@ControllerAdvice`          | `HttpInterceptor`         |
| `@ExceptionHandler`          | `validateError()`         |
| `ResponseEntity.status(404)` | `throwError(() => err)`   |
| `try-catch` en controller    | `catchError` en subscribe |

---

## 🎓 Resumen

1. **Por defecto**: El interceptor maneja automáticamente todos los errores
2. **Personalizado**: Usa `HttpSkipInterceptor.skipAll()` para control total
3. **Mixto**: Salta el interceptor solo para casos específicos
4. **Logging**: Todos los errores se loguean automáticamente

**Recuerda:** El interceptor está diseñado para hacerte la vida más fácil. Solo sáltalo cuando realmente necesites manejo personalizado. 🚀
