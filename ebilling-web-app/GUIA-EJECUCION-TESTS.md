# 🧪 Guía de Ejecución de Tests Unitarios

## 📋 Comandos Disponibles

### 1. Ejecutar todos los tests (modo watch)

```bash
npm test
```

Este comando:

- ✅ Abre Chrome automáticamente
- ✅ Ejecuta todos los tests
- ✅ Se recarga automáticamente cuando cambias archivos
- ✅ Muestra resultados en tiempo real en el navegador
- ✅ Puerto: `http://localhost:9876`

**Uso recomendado:** Durante desarrollo, para ver resultados inmediatos.

---

### 2. Ejecutar tests una sola vez (CI/CD)

```bash
npm test -- --watch=false --browsers=ChromeHeadless
```

Este comando:

- ✅ Ejecuta todos los tests una vez
- ✅ No abre navegador (headless)
- ✅ Termina automáticamente
- ✅ Ideal para CI/CD

**Uso recomendado:** Para ejecutar en pipelines o verificar que todo pasa.

---

### 3. Ejecutar tests con cobertura

```bash
npm test -- --code-coverage --watch=false
```

Este comando:

- ✅ Ejecuta todos los tests
- ✅ Genera reporte de cobertura
- ✅ Reporte HTML en: `coverage/ebilling-web-app/index.html`
- ✅ Reporte texto en consola

**Uso recomendado:** Para ver qué porcentaje del código está cubierto por tests.

---

### 4. Ejecutar un archivo de test específico

```bash
npm test -- --include='**/permission.service.spec.ts'
```

O usando ng directamente:

```bash
ng test --include='**/permission.service.spec.ts'
```

**Uso recomendado:** Para ejecutar solo los tests de un servicio/componente específico.

---

### 5. Ejecutar tests con filtro por nombre

```bash
npm test -- --grep="should be created"
```

Este comando ejecuta solo los tests que contengan "should be created" en su nombre.

---

## 🎯 Tests Disponibles Actualmente

### ✅ Tests Implementados (5 archivos)

1. **PermissionService** (`permission.service.spec.ts`)

   - 15+ casos de prueba
   - Cobertura: suscripciones, permisos, validación de features

2. **PermissionGuard** (`permission.guard.spec.ts`)

   - 10+ casos de prueba
   - Cobertura: protección de rutas, super_admin, mensajes de error

3. **HasPermissionDirective** (`has-permission.directive.spec.ts`)

   - 7+ casos de prueba
   - Cobertura: mostrar/ocultar elementos según permisos

4. **InvoiceService** (`invoice.service.spec.ts`)

   - 12+ casos de prueba
   - Cobertura: CRUD de facturas, búsqueda, descargas

5. **ClientService** (`client.service.spec.ts`)
   - 10+ casos de prueba
   - Cobertura: CRUD de clientes, búsqueda

**Total:** ~54+ casos de prueba

---

## 📊 Ver Resultados

### En el Navegador (modo watch)

1. Ejecuta `npm test`
2. Se abre Chrome automáticamente en `http://localhost:9876`
3. Verás:
   - ✅ Tests pasando (verde)
   - ❌ Tests fallando (rojo)
   - ⚠️ Tests pendientes (amarillo)
   - Detalles de errores si hay fallos

### En la Consola

Al ejecutar tests, verás output como:

```
Chrome Headless 120.0.6099.109 (macOS 10.15.7): Executed 54 of 54 SUCCESS (0.234 secs / 0.123 secs)
TOTAL: 54 SUCCESS
```

---

## 📈 Ver Cobertura de Código

### Generar Reporte de Cobertura

```bash
npm test -- --code-coverage --watch=false
```

### Ver Reporte HTML

1. Ejecuta el comando anterior
2. Abre el archivo: `coverage/ebilling-web-app/index.html` en tu navegador
3. Verás:
   - Porcentaje de cobertura por archivo
   - Líneas cubiertas/no cubiertas
   - Funciones cubiertas/no cubiertas
   - Branches cubiertos/no cubiertos

### Ver Reporte en Consola

Al final de la ejecución verás algo como:

```
=============================== Coverage summary ===============================
Statements   : 45.23% ( 234/517 )
Branches     : 38.12% ( 89/234 )
Functions    : 52.34% ( 123/235 )
Lines        : 44.56% ( 221/496 )
================================================================================
```

---

## 🔧 Configuración Avanzada

### Cambiar Puerto de Karma

Si el puerto 9876 está ocupado, edita `karma.conf.js`:

```javascript
port: 9877, // Cambiar a otro puerto
```

### Ejecutar en Otro Navegador

Edita `karma.conf.js`:

```javascript
browsers: ['Firefox'], // o 'Chrome', 'Safari', etc.
```

### Ejecutar en Múltiples Navegadores

```javascript
browsers: ['Chrome', 'Firefox', 'Safari'],
```

---

## 🐛 Troubleshooting

### Error: Puerto 9876 ocupado

```bash
# Encontrar proceso usando el puerto
lsof -ti:9876

# Matar proceso
kill -9 $(lsof -ti:9876)

# O cambiar puerto en karma.conf.js
```

### Error: Chrome no se abre

```bash
# Ejecutar en modo headless
npm test -- --browsers=ChromeHeadless --watch=false
```

### Error: Tests fallan por dependencias

Asegúrate de que todas las dependencias estén instaladas:

```bash
npm install
```

### Error: Tests fallan por imports

Verifica que los paths en `tsconfig.spec.json` sean correctos.

---

## 📝 Ejemplos de Uso

### Ejemplo 1: Desarrollo Normal

```bash
# Terminal 1: Servidor de desarrollo
npm start

# Terminal 2: Tests en modo watch
npm test
```

### Ejemplo 2: Verificar antes de commit

```bash
# Ejecutar todos los tests una vez
npm test -- --watch=false --browsers=ChromeHeadless

# Si todos pasan, hacer commit
git add .
git commit -m "feat: agregar tests unitarios"
```

### Ejemplo 3: Ver cobertura antes de PR

```bash
# Generar reporte de cobertura
npm test -- --code-coverage --watch=false

# Abrir reporte HTML
open coverage/ebilling-web-app/index.html
```

---

## 🎯 Próximos Pasos

1. **Ejecutar tests actuales:**

   ```bash
   npm test
   ```

2. **Verificar que todos pasan:**

   - Revisar navegador en `http://localhost:9876`
   - Todos deberían estar en verde ✅

3. **Ver cobertura:**

   ```bash
   npm test -- --code-coverage --watch=false
   ```

4. **Aumentar cobertura:**
   - Agregar más tests para componentes faltantes
   - Meta: >80% de cobertura en servicios críticos

---

## 📚 Referencias

- [Angular Testing Guide](https://angular.io/guide/testing)
- [Jasmine Documentation](https://jasmine.github.io/)
- [Karma Configuration](https://karma-runner.github.io/latest/config/configuration-file.html)

---

**Última actualización:** Diciembre 2025
