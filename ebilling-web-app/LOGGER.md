# 📝 Guía de Logging - eBilling Web App

> **Sistema de logging profesional - Igual que SLF4J en Spring Boot**

---

## 🎯 **¿QUÉ ES EL LOGGER SERVICE?**

Es un servicio centralizado para manejar logs en Angular, similar a **SLF4J/Logback** en Spring Boot.

### **Características:**

- ✅ Niveles de log (DEBUG, INFO, WARN, ERROR)
- ✅ DEBUG solo visible en desarrollo
- ✅ Logs con colores en consola
- ✅ Timestamps automáticos
- ✅ Formato estructurado
- ✅ Fácil de usar

---

## 🚀 **CÓMO USARLO**

### **Paso 1: Inyectar el servicio**

```typescript
import { LoggerService } from "./_service/logger/logger.service";

export class LoginComponent {
  constructor(private logger: LoggerService) {}
}
```

### **Paso 2: Usar los métodos de log**

```typescript
// DEBUG - Solo en desarrollo
this.logger.debug("Usuario intentando login", { userName: this.userName });

// INFO - Información general
this.logger.info("Usuario autenticado correctamente", user);

// WARN - Advertencias
this.logger.warn("Token próximo a expirar");

// ERROR - Errores
this.logger.error("Error al llamar API", error);
```

---

## 📊 **NIVELES DE LOG**

### **Comparación con Spring Boot:**

| Spring Boot      | Angular Logger        | Cuándo usar                       |
| ---------------- | --------------------- | --------------------------------- |
| `logger.debug()` | `this.logger.debug()` | Debugging, desarrollo             |
| `logger.info()`  | `this.logger.info()`  | Info general, eventos importantes |
| `logger.warn()`  | `this.logger.warn()`  | Advertencias, deprecated          |
| `logger.error()` | `this.logger.error()` | Errores, excepciones              |

### **1. DEBUG** 🟢

```typescript
// Solo visible en DESARROLLO
this.logger.debug("Iniciando proceso de login");
this.logger.debug("Datos recibidos:", data);
this.logger.debug("Token decodificado:", token);
```

**Output:**

```
[DEBUG] 15:30:45 - Iniciando proceso de login
```

### **2. INFO** 🔵

```typescript
// Visible en desarrollo y producción
this.logger.info("Usuario logueado exitosamente");
this.logger.info("Compañía seleccionada:", company.name);
```

**Output:**

```
[INFO] 15:30:46 - Usuario logueado exitosamente
```

### **3. WARN** 🟡

```typescript
// Advertencias importantes
this.logger.warn("API lenta, más de 3 segundos");
this.logger.warn("Método deprecated, usar nuevo endpoint");
```

**Output:**

```
[WARN] 15:30:47 - API lenta, más de 3 segundos
```

### **4. ERROR** 🔴

```typescript
// Errores y excepciones
this.logger.error("Error al llamar API", error);
this.logger.error("Login falló", error, { username });
```

**Output:**

```
[ERROR] 15:30:48 - Error al llamar API {...}
```

---

## 🎨 **MÉTODOS ESPECIALES**

### **1. Log de Tabla**

```typescript
// Mostrar datos en formato tabla
this.logger.table(users, "Lista de usuarios");
```

**Output:**

```
[TABLE] 15:30:49 - Lista de usuarios
┌─────────┬──────────┬────────────────────┐
│ (index) │   name   │       email        │
├─────────┼──────────┼────────────────────┤
│    0    │ 'Juan'   │ 'juan@email.com'   │
│    1    │ 'María'  │ 'maria@email.com'  │
└─────────┴──────────┴────────────────────┘
```

### **2. Agrupar Logs**

```typescript
this.logger.group("Proceso de Autenticación");
this.logger.debug("Paso 1: Validar credenciales");
this.logger.debug("Paso 2: Generar token");
this.logger.debug("Paso 3: Cargar menús");
this.logger.groupEnd();
```

**Output:**

```
▼ [GROUP] 15:30:50 - Proceso de Autenticación
    [DEBUG] 15:30:50 - Paso 1: Validar credenciales
    [DEBUG] 15:30:50 - Paso 2: Generar token
    [DEBUG] 15:30:50 - Paso 3: Cargar menús
```

---

## 💡 **EJEMPLOS PRÁCTICOS**

### **Login Component**

```typescript
export class LoginComponent {
  constructor(private logger: LoggerService, private loginService: LoginService) {}

  login() {
    this.logger.debug("Iniciando login", { userName: this.userName });

    this.loginService.login(this.userName, this.password).subscribe({
      next: (response) => {
        this.logger.info("Login exitoso", { userName: this.userName });
        this.logger.debug("Token recibido:", response.token);
      },
      error: (err) => {
        this.logger.error("Login falló", err);
      },
    });
  }
}
```

### **HTTP Interceptor**

```typescript
export class ServerErrorsInterceptor {
  constructor(private logger: LoggerService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    this.logger.debug("HTTP Request:", req.url);

    return next.handle(req).pipe(
      tap((event) => {
        if (event instanceof HttpResponse) {
          this.logger.debug("HTTP Response:", event.status);
        }
      }),
      catchError((err) => {
        this.logger.error("HTTP Error", err);
        return throwError(() => err);
      })
    );
  }
}
```

### **Service con múltiples operaciones**

```typescript
export class ProductService {
  constructor(private http: HttpClient, private logger: LoggerService) {}

  save(product: Product) {
    this.logger.group("Guardando producto");
    this.logger.debug("Producto a guardar:", product);

    return this.http.post(this.url, product).pipe(
      tap((response) => {
        this.logger.info("Producto guardado exitosamente", response);
        this.logger.groupEnd();
      }),
      catchError((error) => {
        this.logger.error("Error al guardar producto", error);
        this.logger.groupEnd();
        return throwError(() => error);
      })
    );
  }

  findAll() {
    this.logger.debug("Buscando todos los productos");
    return this.http.get<Product[]>(this.url).pipe(
      tap((products) => {
        this.logger.info(`${products.length} productos encontrados`);
        this.logger.table(products, "Productos");
      })
    );
  }
}
```

---

## ⚙️ **CONFIGURACIÓN**

### **Cambiar nivel de log en runtime:**

```typescript
// En la consola del navegador (F12):
window["logger"].setLogLevel(0); // DEBUG
window["logger"].setLogLevel(1); // INFO
window["logger"].setLogLevel(2); // WARN
window["logger"].setLogLevel(3); // ERROR
window["logger"].setLogLevel(4); // NONE
```

### **Niveles por ambiente:**

```typescript
// environment.ts (desarrollo)
export const environment = {
  production: false,
  logLevel: "DEBUG", // Todos los logs
};

// environment.prod.ts (producción)
export const environment = {
  production: true,
  logLevel: "INFO", // Solo INFO, WARN, ERROR
};
```

---

## 🔄 **MIGRAR console.log A LOGGER**

### **ANTES:**

```typescript
console.log("Usuario:", user);
console.log(data);
console.error(err);
console.warn("Deprecated");
```

### **DESPUÉS:**

```typescript
this.logger.debug("Usuario:", user);
this.logger.debug("Data recibida:", data);
this.logger.error("Error en operación", err);
this.logger.warn("Método deprecated");
```

---

## 📋 **CHECKLIST DE MIGRACIÓN**

Para cada componente/servicio:

- [ ] Importar `LoggerService`
- [ ] Inyectar en constructor
- [ ] Buscar `console.log` → `logger.debug`
- [ ] Buscar `console.info` → `logger.info`
- [ ] Buscar `console.warn` → `logger.warn`
- [ ] Buscar `console.error` → `logger.error`
- [ ] Agregar contexto a los logs
- [ ] Probar en desarrollo
- [ ] Verificar que debug no aparezca en producción

---

## 🎯 **MEJORES PRÁCTICAS**

### **✅ HACER:**

```typescript
// 1. Logs con contexto
this.logger.debug("Login iniciado", { userName, timestamp: Date.now() });

// 2. Logs en operaciones importantes
this.logger.info("Compañía seleccionada", company);

// 3. Logs de errores completos
this.logger.error("Error al guardar", error, { productId, userId });

// 4. Agrupar operaciones complejas
this.logger.group("Proceso de facturación");
// ... múltiples pasos
this.logger.groupEnd();

// 5. Usar tabla para arrays
this.logger.table(invoices, "Facturas pendientes");
```

### **❌ NO HACER:**

```typescript
// 1. Logs sin contexto
this.logger.debug("Error"); // ¿Qué error?

// 2. Logs de información sensible
this.logger.debug("Password:", password); // ❌ NUNCA

// 3. Logs excesivos en loops
for (let i = 0; i < 10000; i++) {
  this.logger.debug("Iteración", i); // ❌ Contamina la consola
}

// 4. INFO para debugging
this.logger.info("Variable x vale:", x); // Usar debug

// 5. Múltiples logs cuando uno basta
this.logger.debug("user:", user);
this.logger.debug("token:", token);
// Mejor: this.logger.debug('Auth data:', { user, token });
```

---

## 🔍 **DEBUGGING CON LOGGER**

### **Ver flujo completo:**

```typescript
ngOnInit() {
  this.logger.group('Inicialización de componente');
  this.logger.debug('1. Obteniendo parámetros de ruta');
  this.logger.debug('2. Cargando datos iniciales');
  this.logger.debug('3. Configurando formularios');
  this.logger.groupEnd();
}
```

### **Rastrear valores:**

```typescript
this.product$
  .pipe(
    tap((p) => this.logger.debug("Product changed:", p)),
    map((p) => p.price),
    tap((price) => this.logger.debug("Price extracted:", price))
  )
  .subscribe();
```

---

## 📚 **COMPARACIÓN: Spring Boot vs Angular**

```java
// Spring Boot
@Service
@Slf4j
public class UserService {
    public User login(String username, String password) {
        log.debug("Login attempt for: {}", username);
        try {
            User user = authenticate(username, password);
            log.info("User logged in: {}", username);
            return user;
        } catch (Exception e) {
            log.error("Login failed", e);
            throw e;
        }
    }
}
```

```typescript
// Angular
@Injectable()
export class UserService {
  constructor(private logger: LoggerService) {}

  login(username: string, password: string) {
    this.logger.debug("Login attempt for:", username);
    return this.http.post(url, { username, password }).pipe(
      tap((user) => this.logger.info("User logged in:", username)),
      catchError((err) => {
        this.logger.error("Login failed", err);
        return throwError(() => err);
      })
    );
  }
}
```

---

## 🎉 **RESULTADO**

Con este sistema tienes:

- ✅ Logs profesionales como en Spring Boot
- ✅ Debug solo en desarrollo
- ✅ Fácil de mantener y leer
- ✅ Preparado para producción
- ✅ Mejor debugging experience

---

**¿Dudas?** Consulta con el equipo o revisa los ejemplos en el código 💪
