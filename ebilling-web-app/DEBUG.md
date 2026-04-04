# 🐛 Guía de Debugging - eBilling Web App

> **Para desarrolladores Backend que están trabajando en Frontend** 😄

---

## 🚀 Quick Start

### Método 1: Debug en Cursor/VS Code (Recomendado)

1. **Poner un breakpoint** en cualquier archivo `.ts`:

   - Click en el margen izquierdo del editor (número de línea)
   - Aparecerá un círculo rojo 🔴

2. **Iniciar debug**:

   - Presiona `F5` o
   - Ve a la pestaña "Run and Debug" (Ctrl/Cmd + Shift + D)
   - Selecciona "🚀 Debug Angular (Chrome)"
   - Click en el botón verde ▶️

3. **Disfruta**:
   - El navegador se abrirá automáticamente
   - El servidor Angular iniciará
   - La ejecución se detendrá en tus breakpoints
   - Puedes inspeccionar variables, call stack, etc.

---

## 📋 Configuraciones Disponibles

### 1. 🚀 Debug Angular (Chrome)

- **Uso:** Debug completo desde cero
- **Qué hace:** Inicia el servidor + abre Chrome con debug
- **Shortcut:** F5

### 2. 🔗 Attach to Chrome

- **Uso:** Si ya tienes `npm start` corriendo
- **Qué hace:** Se conecta a Chrome sin reiniciar el servidor
- **Cómo usarlo:**
  1. Corre `npm start` en terminal
  2. Abre Chrome en `http://localhost:4200`
  3. Selecciona esta configuración y presiona F5

### 3. 🦊 Debug Angular (Firefox)

- **Uso:** Si prefieres Firefox sobre Chrome
- **Qué hace:** Igual que Chrome pero con Firefox

---

## 🎯 Debugging Como Backend Dev

### Comparación con Spring Boot:

| Concepto          | Spring Boot (IntelliJ) | Angular (Cursor) |
| ----------------- | ---------------------- | ---------------- |
| **Iniciar debug** | Shift+F9               | F5               |
| **Breakpoint**    | Click en margen        | Click en margen  |
| **Step Over**     | F8                     | F10              |
| **Step Into**     | F7                     | F11              |
| **Step Out**      | Shift+F8               | Shift+F11        |
| **Resume**        | F9                     | F5               |
| **Evaluate**      | Alt+F8                 | Debug Console    |
| **Watch**         | Variables tab          | Variables tab    |

---

## 🔧 Tips y Trucos

### 1. Usar `debugger;` en el código

```typescript
login() {
  debugger; // ← La ejecución se detendrá aquí
  this.loginService.login(this.userName, this.password)
    .subscribe((response) => {
      debugger; // ← Y aquí también
      console.log(response);
    });
}
```

### 2. Console.log con estilo

```typescript
console.log("%c Response:", "color: green; font-weight: bold", response);
console.table(users); // ← Muestra arrays/objetos en tabla
console.group("Login Flow");
console.log("Step 1");
console.log("Step 2");
console.groupEnd();
```

### 3. Conditional Breakpoints

- Click derecho en el breakpoint
- "Edit Breakpoint..."
- Agrega condición: `userName === 'admin'`

### 4. Logpoints (sin detener ejecución)

- Click derecho en el margen
- "Add Logpoint..."
- Escribe: `Usuario: {userName}, Pass: {password}`

---

## 🌐 Debug en el Navegador (DevTools)

### Chrome DevTools Shortcuts:

| Acción          | Shortcut                         |
| --------------- | -------------------------------- |
| Abrir DevTools  | F12 o Cmd+Opt+I                  |
| Console         | Cmd+Opt+J                        |
| Sources (debug) | Cmd+Opt+I                        |
| Network         | Ver peticiones HTTP              |
| Application     | Ver localStorage, sessionStorage |

### Tabs más útiles:

1. **Console** - Logs, errores, ejecutar código
2. **Sources** - Debuggear código TypeScript
3. **Network** - Ver llamadas HTTP (como Postman)
4. **Application** - Ver tokens, storage
5. **Elements** - Inspeccionar HTML/CSS

---

## 🔍 Debugging de HTTP Requests

### Ver peticiones como en Postman:

1. F12 → **Network** tab
2. Filter: **Fetch/XHR**
3. Haz una acción (ej: login)
4. Click en la petición
5. Ve a **Headers**, **Payload**, **Response**

### Ejemplo:

```
POST http://localhost:8280/api/login
Request Headers:
  Content-Type: application/json
Request Payload:
  {"userName": "admin", "password": "xxx"}
Response:
  {"code": 0, "token": "eyJhbGc..."}
```

---

## 🛠️ Debugging de RxJS Observables

Los Observables son como CompletableFuture de Java, pero con más poder.

### Debug con tap():

```typescript
this.loginService
  .login(userName, password)
  .pipe(
    tap((response) => {
      debugger; // ← Detiene aquí
      console.log("Response:", response);
    })
  )
  .subscribe((response) => {
    // tu lógica
  });
```

### Debug con catchError():

```typescript
this.loginService
  .login(userName, password)
  .pipe(
    tap((response) => console.log("Success:", response)),
    catchError((error) => {
      debugger; // ← Detiene aquí en errores
      console.error("Error:", error);
      return throwError(() => error);
    })
  )
  .subscribe();
```

---

## 🐞 Problemas Comunes

### 1. "Breakpoint no se activa"

- ✅ Verifica que `sourceMaps: true` en `angular.json`
- ✅ Reinicia el servidor: `Ctrl+C` → `npm start`
- ✅ Limpia cache: `rm -rf .angular/cache`

### 2. "Variables show undefined"

- Es normal en async code
- Usa `await` o `.subscribe()` para ver valores

### 3. "Cannot connect to Chrome"

- Cierra todas las ventanas de Chrome
- Vuelve a presionar F5

---

## 📚 Recursos Adicionales

- [Angular DevTools Extension](https://chrome.google.com/webstore/detail/angular-devtools/ienfalfjdbdpebioblfackkekamfmbnh)
- [Chrome DevTools Docs](https://developer.chrome.com/docs/devtools/)
- [VS Code Debugging](https://code.visualstudio.com/docs/editor/debugging)

---

## 🎓 Comandos Útiles

```bash
# Iniciar con source maps optimizados
npm start

# Build con source maps (para debug de prod)
ng build --source-map=true

# Ver bundle size
npm run analyze

# Limpiar cache
rm -rf .angular/cache node_modules/.cache
```

---

**¿Dudas?** Pregunta en el canal de #frontend o consulta con el equipo 💪
