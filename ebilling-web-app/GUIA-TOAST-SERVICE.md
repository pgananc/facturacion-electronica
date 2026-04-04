# Guía de Uso de ToastService

## 📋 Introducción

El `ToastService` es el servicio centralizado para mostrar notificaciones toast en toda la aplicación. Reemplaza el uso directo de `MatSnackBar.open()` y `alert()` para mantener consistencia visual y de comportamiento.

## 🎯 Uso Básico

### 1. Importar el servicio

```typescript
import { ToastService } from '../../_service/notification/toast.service';
```

### 2. Inyectar en el constructor

```typescript
constructor(
  private readonly toast: ToastService
) {}
```

### 3. Usar los métodos disponibles

```typescript
// Mensaje de éxito (verde)
this.toast.success('Operación exitosa');

// Mensaje de error (rojo)
this.toast.error('Error al procesar la solicitud');

// Mensaje de advertencia (naranja)
this.toast.warning('Atención requerida');

// Mensaje informativo (azul)
this.toast.info('Información importante');
```

## 🔧 Métodos Disponibles

### `success(message: string, icon?: string)`
Muestra un toast de éxito (verde) con icono de check.

**Ejemplo:**
```typescript
this.toast.success('Factura guardada correctamente');
```

### `error(message: string, icon?: string)`
Muestra un toast de error (rojo) con icono de error.

**Ejemplo:**
```typescript
this.toast.error('Error al guardar la factura');
```

### `warning(message: string, icon?: string)`
Muestra un toast de advertencia (naranja) con icono de warning.

**Ejemplo:**
```typescript
this.toast.warning('Por favor, complete todos los campos requeridos');
```

### `info(message: string, icon?: string)`
Muestra un toast informativo (azul) con icono de info.

**Ejemplo:**
```typescript
this.toast.info('El producto se creó exitosamente');
```

### `httpError(error: any, context?: string, defaultMessage?: string)`
Muestra un mensaje de error HTTP con formato amigable. Filtra automáticamente mensajes técnicos.

**Ejemplo:**
```typescript
this.productService.save(product).subscribe({
  next: (result) => {
    this.toast.success('Producto guardado');
  },
  error: (error) => {
    this.toast.httpError(error, 'guardar el producto', 'No se pudo guardar el producto');
  }
});
```

## 📝 Ejemplos de Uso en Diferentes Escenarios

### Validación de Formulario

```typescript
if (!this.form.valid) {
  this.toast.warning('Por favor, complete todos los campos requeridos');
  return;
}
```

### Operación Exitosa

```typescript
this.productService.save(product).subscribe({
  next: () => {
    this.toast.success('Producto guardado correctamente');
    this.dialogRef.close(true);
  },
  error: (error) => {
    this.toast.error('Error al guardar el producto');
  }
});
```

### Manejo de Errores HTTP

```typescript
this.clientService.findByIdentification(identification).subscribe({
  next: (client) => {
    this.toast.success('Cliente encontrado');
  },
  error: (error) => {
    this.toast.httpError(error, 'buscar el cliente', 'No se pudo encontrar el cliente');
  }
});
```

### Reemplazo de `alert()`

**❌ Antes:**
```typescript
alert('Por favor, ingrese la contraseña del certificado.');
```

**✅ Después:**
```typescript
this.toast.warning('Por favor, ingrese la contraseña del certificado.');
```

### Reemplazo de `snackBar.open()`

**❌ Antes:**
```typescript
this.snackBar.open('Error al guardar', 'Cerrar', {
  duration: 5000,
  horizontalPosition: 'center',
  verticalPosition: 'bottom',
});
```

**✅ Después:**
```typescript
this.toast.error('Error al guardar');
```

## 🎨 Características Visuales

- **Posición:** Superior derecha (top-end)
- **Duración:** 5 segundos por defecto
- **Animación:** Slide-in desde la derecha
- **Iconos:** Automáticos según el tipo (check_circle, error, warning, info)
- **Colores:**
  - Success: Verde (#4caf50)
  - Error: Rojo (#f44336)
  - Warning: Naranja (#ff9800)
  - Info: Azul (#2196f3)

## ⚠️ Notas Importantes

1. **No usar `alert()` o `confirm()` nativos del navegador** - Siempre usar `ToastService` o `ConfirmDialogComponent` para diálogos.

2. **No usar `MatSnackBar.open()` directamente** - Usar `ToastService` para mantener consistencia.

3. **Para errores HTTP**, usar `httpError()` en lugar de `error()` para obtener mensajes más amigables.

4. **El servicio ya está disponible globalmente** - No es necesario agregarlo a los providers del módulo.

## 📚 Referencia

- **Servicio:** `src/app/_service/notification/toast.service.ts`
- **Componente de Toast:** `src/app/_shared/validation-snackbar/validation-snackbar.component.ts`
- **Ejemplo de uso:** `src/app/pages/billing/billing.component.ts`

## 🔄 Migración

Si encuentras código que usa `alert()`, `confirm()`, o `snackBar.open()` directamente, reemplázalo con `ToastService`:

1. Importa `ToastService`
2. Inyéctalo en el constructor
3. Reemplaza las llamadas según el tipo de mensaje:
   - `alert()` → `toast.warning()` o `toast.error()`
   - `snackBar.open()` → `toast.success()`, `toast.error()`, `toast.warning()`, o `toast.info()`




