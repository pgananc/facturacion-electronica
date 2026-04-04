# 🔍 Revisión: Condiciones de Visualización en el Ticket

## ⚠️ PROBLEMA IDENTIFICADO

Los siguientes campos se están mostrando aunque tengan valor 0:

- ✅ Subtotales por IVA (15%, 5%, 0%)
- ✅ DESCUENTO
- ✅ ICE
- ✅ Propina 10%

**Deben ocultarse si el valor es 0 o menor.**

---

## 🔍 ANÁLISIS DEL CÓDIGO ACTUAL

### **1. Métodos Helper en TypeScript**

#### **`getSubtotalByIva(ivaPercent: number): number`**

```typescript
getSubtotalByIva(ivaPercent: number): number {
  const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
  return item?.subtotal || 0;  // ⚠️ Retorna 0 si no encuentra
}
```

**Problema potencial:**

- Si no hay productos con ese porcentaje de IVA, retorna `0`
- La condición `*ngIf="getSubtotalByIva(15) > 0"` debería funcionar
- **PERO**: Si el cálculo resulta en `0.00` (número decimal), puede haber problemas de comparación

#### **`getIvaByPercent(ivaPercent: number): number`**

```typescript
getIvaByPercent(ivaPercent: number): number {
  const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
  return item?.ivaValue || 0;  // ⚠️ Retorna 0 si no encuentra
}
```

**Mismo problema potencial.**

---

### **2. Condiciones en el Template HTML**

#### **Subtotales por IVA:**

```html
<!-- SUBTOTAL 15% -->
<div class="total-row" *ngIf="getSubtotalByIva(15) > 0">
  <span class="total-label">SUBTOTAL 15%:</span>
  <span class="total-value">${{ getSubtotalByIva(15).toFixed(2) }}</span>
</div>
```

**Análisis:**

- ✅ La condición `> 0` debería funcionar
- ⚠️ **PERO**: Si `getSubtotalByIva(15)` retorna `0.00` (string después de `.toFixed()`), la comparación puede fallar
- ⚠️ **O**: Si el valor es exactamente `0` pero se muestra como `$0.00`

#### **DESCUENTO:**

```html
<div class="total-row" *ngIf="totalDescuento > 0">
  <span class="total-label">DESCUENTO:</span>
  <span class="total-value">-${{ totalDescuento.toFixed(2) }}</span>
</div>
```

**Análisis:**

- ✅ La condición `> 0` debería funcionar
- ⚠️ **PERO**: Si `totalDescuento` es `0` o `0.00`, debería ocultarse
- ⚠️ **O**: Si el valor viene como `null` o `undefined` y se convierte a `0`

#### **ICE:**

```html
<div class="total-row" *ngIf="totalICE > 0">
  <span class="total-label">ICE:</span>
  <span class="total-value">${{ totalICE.toFixed(2) }}</span>
</div>
```

**Mismo análisis.**

#### **Propina:**

```html
<div class="total-row" *ngIf="invoice.tip > 0">
  <span class="total-label">Propina 10%:</span>
  <span class="total-value">${{ invoice.tip.toFixed(2) }}</span>
</div>
```

**Mismo análisis.**

---

## 🐛 POSIBLES CAUSAS DEL PROBLEMA

### **Causa 1: Valores Decimales Muy Pequeños**

Si el cálculo resulta en valores como `0.0000001` (por errores de precisión de punto flotante), la condición `> 0` sería `true` pero el valor mostrado sería `$0.00`.

**Solución:** Usar comparación con tolerancia o redondear antes de comparar.

### **Causa 2: Valores Null/Undefined Convertidos a 0**

Si `invoice.tip` es `null` o `undefined`, y se compara directamente, puede causar problemas.

**Solución:** Usar operador de coalescencia nula y verificar explícitamente.

### **Causa 3: Comparación con Strings**

Si el valor se convierte a string antes de la comparación, `"0.00" > 0` podría comportarse diferente.

**Solución:** Asegurar que la comparación sea numérica.

### **Causa 4: Lógica en `calculateSubtotalsByIva()`**

Si el método agrega items con valor 0 al array `subtotalsByIva`, entonces `getSubtotalByIva()` siempre retornará un valor (aunque sea 0).

**Solución:** Ya está implementado correctamente (solo agrega si `subtotalRounded > 0`), pero verificar.

---

## ✅ SOLUCIONES PROPUESTAS

### **Solución 1: Mejorar Métodos Helper (Recomendada)**

Modificar los métodos para que retornen `null` o `undefined` cuando no hay valor, y usar comparación más estricta:

```typescript
/**
 * Obtiene el subtotal para un % de IVA específico
 * Retorna null si no existe o es 0
 */
getSubtotalByIva(ivaPercent: number): number | null {
  const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
  if (!item || item.subtotal <= 0) {
    return null;
  }
  return item.subtotal;
}

/**
 * Obtiene el IVA para un % específico
 * Retorna null si no existe o es 0
 */
getIvaByPercent(ivaPercent: number): number | null {
  const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
  if (!item || item.ivaValue <= 0) {
    return null;
  }
  return item.ivaValue;
}
```

**Y en el template:**

```html
<!-- SUBTOTAL 15% -->
<div class="total-row" *ngIf="getSubtotalByIva(15) !== null && getSubtotalByIva(15)! > 0">
  <span class="total-label">SUBTOTAL 15%:</span>
  <span class="total-value">${{ getSubtotalByIva(15)!.toFixed(2) }}</span>
</div>
```

**Problema:** Se llama dos veces al método (una vez en `*ngIf` y otra en el template).

---

### **Solución 2: Usar Comparación con Tolerancia (Más Robusta)**

Crear métodos que verifiquen si el valor es "significativamente mayor que 0":

```typescript
/**
 * Verifica si un valor es mayor que 0 (con tolerancia para errores de precisión)
 */
private isGreaterThanZero(value: number | null | undefined): boolean {
  if (value === null || value === undefined) {
    return false;
  }
  // Tolerancia de 0.01 para errores de precisión de punto flotante
  return Math.round(value * 100) / 100 > 0;
}

/**
 * Obtiene el subtotal para un % de IVA específico
 */
getSubtotalByIva(ivaPercent: number): number {
  const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
  return item?.subtotal || 0;
}

/**
 * Verifica si hay subtotal para un % de IVA específico
 */
hasSubtotalByIva(ivaPercent: number): boolean {
  return this.isGreaterThanZero(this.getSubtotalByIva(ivaPercent));
}

/**
 * Verifica si hay IVA para un % específico
 */
hasIvaByPercent(ivaPercent: number): boolean {
  return this.isGreaterThanZero(this.getIvaByPercent(ivaPercent));
}
```

**Y en el template:**

```html
<!-- SUBTOTAL 15% -->
<div class="total-row" *ngIf="hasSubtotalByIva(15)">
  <span class="total-label">SUBTOTAL 15%:</span>
  <span class="total-value">${{ getSubtotalByIva(15).toFixed(2) }}</span>
</div>
```

---

### **Solución 3: Usar Comparación Estricta con Coalescencia (Más Simple)**

Asegurar que las comparaciones sean numéricas y manejen null/undefined:

```typescript
// En el componente, asegurar que los valores sean números válidos
get totalDescuento(): number {
  return this.totalDescuento || 0;
}

get totalICE(): number {
  return this.totalICE || 0;
}

get hasTip(): boolean {
  return (this.invoice?.tip || 0) > 0;
}
```

**Y en el template usar comparación estricta:**

```html
<!-- DESCUENTO -->
<div class="total-row" *ngIf="(totalDescuento || 0) > 0">
  <span class="total-label">DESCUENTO:</span>
  <span class="total-value">-${{ totalDescuento.toFixed(2) }}</span>
</div>

<!-- ICE -->
<div class="total-row" *ngIf="(totalICE || 0) > 0">
  <span class="total-label">ICE:</span>
  <span class="total-value">${{ totalICE.toFixed(2) }}</span>
</div>

<!-- Propina 10% -->
<div class="total-row" *ngIf="hasTip">
  <span class="total-label">Propina 10%:</span>
  <span class="total-value">${{ invoice.tip.toFixed(2) }}</span>
</div>
```

---

## 🎯 SOLUCIÓN RECOMENDADA (Combinada)

### **1. Mejorar Métodos Helper con Verificaciones**

```typescript
/**
 * Verifica si un valor numérico es mayor que 0 (maneja null/undefined y errores de precisión)
 */
private isPositiveValue(value: number | null | undefined): boolean {
  if (value === null || value === undefined) {
    return false;
  }
  // Redondear a 2 decimales y comparar (evita errores de precisión)
  const rounded = Math.round(value * 100) / 100;
  return rounded > 0;
}

/**
 * Obtiene el subtotal para un % de IVA específico
 */
getSubtotalByIva(ivaPercent: number): number {
  const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
  return item?.subtotal || 0;
}

/**
 * Verifica si hay subtotal para un % de IVA específico
 */
hasSubtotalByIva(ivaPercent: number): boolean {
  return this.isPositiveValue(this.getSubtotalByIva(ivaPercent));
}

/**
 * Obtiene el IVA para un % específico
 */
getIvaByPercent(ivaPercent: number): number {
  const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
  return item?.ivaValue || 0;
}

/**
 * Verifica si hay IVA para un % específico
 */
hasIvaByPercent(ivaPercent: number): boolean {
  return this.isPositiveValue(this.getIvaByPercent(ivaPercent));
}

/**
 * Verifica si hay descuento
 */
hasDiscount(): boolean {
  return this.isPositiveValue(this.totalDescuento);
}

/**
 * Verifica si hay ICE
 */
hasICE(): boolean {
  return this.isPositiveValue(this.totalICE);
}

/**
 * Verifica si hay propina
 */
hasTip(): boolean {
  return this.isPositiveValue(this.invoice?.tip);
}
```

### **2. Actualizar Template HTML**

```html
<!-- ✅ TOTALES DETALLADOS (según imagen) -->
<div class="totals">
  <!-- SUBTOTAL 15% -->
  <div class="total-row" *ngIf="hasSubtotalByIva(15)">
    <span class="total-label">SUBTOTAL 15%:</span>
    <span class="total-value">${{ getSubtotalByIva(15).toFixed(2) }}</span>
  </div>

  <!-- SUBTOTAL 5% -->
  <div class="total-row" *ngIf="hasSubtotalByIva(5)">
    <span class="total-label">SUBTOTAL 5%:</span>
    <span class="total-value">${{ getSubtotalByIva(5).toFixed(2) }}</span>
  </div>

  <!-- SUBTOTAL 0% -->
  <div class="total-row" *ngIf="hasSubtotalByIva(0)">
    <span class="total-label">SUBTOTAL 0%:</span>
    <span class="total-value">${{ getSubtotalByIva(0).toFixed(2) }}</span>
  </div>

  <!-- SUBTOTAL SIN IMPUESTOS (SIEMPRE se muestra) -->
  <div class="total-row">
    <span class="total-label">SUBTOTAL SIN IMPUESTOS:</span>
    <span class="total-value">${{ subtotalSinImpuestos.toFixed(2) }}</span>
  </div>

  <!-- DESCUENTO -->
  <div class="total-row" *ngIf="hasDiscount()">
    <span class="total-label">DESCUENTO:</span>
    <span class="total-value">-${{ totalDescuento.toFixed(2) }}</span>
  </div>

  <!-- ICE -->
  <div class="total-row" *ngIf="hasICE()">
    <span class="total-label">ICE:</span>
    <span class="total-value">${{ totalICE.toFixed(2) }}</span>
  </div>

  <!-- IVA 15% -->
  <div class="total-row" *ngIf="hasIvaByPercent(15)">
    <span class="total-label">IVA 15%:</span>
    <span class="total-value">${{ getIvaByPercent(15).toFixed(2) }}</span>
  </div>

  <!-- IVA 5% -->
  <div class="total-row" *ngIf="hasIvaByPercent(5)">
    <span class="total-label">IVA 5%:</span>
    <span class="total-value">${{ getIvaByPercent(5).toFixed(2) }}</span>
  </div>

  <!-- IVA 0% -->
  <div class="total-row" *ngIf="hasIvaByPercent(0)">
    <span class="total-label">IVA 0%:</span>
    <span class="total-value">${{ getIvaByPercent(0).toFixed(2) }}</span>
  </div>

  <!-- Propina 10% -->
  <div class="total-row" *ngIf="hasTip()">
    <span class="total-label">Propina 10%:</span>
    <span class="total-value">${{ invoice.tip.toFixed(2) }}</span>
  </div>

  <!-- Valor a pagar (SIEMPRE se muestra) -->
  <div class="total-row total-final">
    <span class="total-label"><strong>Valor a pagar:</strong></span>
    <span class="total-value"><strong>${{ invoice.totalAmount.toFixed(2) }}</strong></span>
  </div>
</div>
```

---

## 🔍 VERIFICACIONES ADICIONALES

### **1. Verificar `calculateSubtotalsByIva()`**

Asegurar que solo agregue items con valor > 0:

```typescript
// Ya está implementado correctamente:
if (subtotalRounded > 0) {
  this.subtotalsByIva.push({
    ivaPercent: ivaPercent,
    subtotal: subtotalRounded,
    ivaValue: ivaValueRounded,
  });
}
```

**✅ Esto está correcto**, pero verificar que `subtotalRounded` y `ivaValueRounded` se calculen correctamente.

### **2. Verificar Asignación de Totales**

```typescript
// Asegurar que se asignen correctamente:
this.subtotalSinImpuestos = Math.round(subtotalSinImpuestos * 100) / 100;
this.totalDescuento = this.invoice.discount || totalDescuento; // ⚠️ Puede ser 0
this.totalICE = this.invoice.subtotalIce || 0; // ⚠️ Puede ser 0
```

**Verificar:** Si `this.invoice.discount` es `0`, se asignará `0`, lo cual está correcto.

---

## 📋 CHECKLIST DE CORRECCIONES

### **En `ticket-print.component.ts`:**

- [ ] Agregar método `isPositiveValue()` para verificación robusta
- [ ] Agregar métodos `hasSubtotalByIva()` para cada porcentaje
- [ ] Agregar métodos `hasIvaByPercent()` para cada porcentaje
- [ ] Agregar método `hasDiscount()`
- [ ] Agregar método `hasICE()`
- [ ] Agregar método `hasTip()`
- [ ] Verificar que `calculateSubtotalsByIva()` solo agregue valores > 0

### **En `ticket-print.component.html`:**

- [ ] Cambiar `*ngIf="getSubtotalByIva(15) > 0"` por `*ngIf="hasSubtotalByIva(15)"`
- [ ] Cambiar `*ngIf="getSubtotalByIva(5) > 0"` por `*ngIf="hasSubtotalByIva(5)"`
- [ ] Cambiar `*ngIf="getSubtotalByIva(0) > 0"` por `*ngIf="hasSubtotalByIva(0)"`
- [ ] Cambiar `*ngIf="getIvaByPercent(15) > 0"` por `*ngIf="hasIvaByPercent(15)"`
- [ ] Cambiar `*ngIf="getIvaByPercent(5) > 0"` por `*ngIf="hasIvaByPercent(5)"`
- [ ] Cambiar `*ngIf="getIvaByPercent(0) > 0"` por `*ngIf="hasIvaByPercent(0)"`
- [ ] Cambiar `*ngIf="totalDescuento > 0"` por `*ngIf="hasDiscount()"`
- [ ] Cambiar `*ngIf="totalICE > 0"` por `*ngIf="hasICE()"`
- [ ] Cambiar `*ngIf="invoice.tip > 0"` por `*ngIf="hasTip()"`

---

## 🧪 PRUEBAS ESPECÍFICAS

### **Test 1: Factura sin Descuento**

- Crear factura con `discount = 0`
- ✅ Verificar que NO se muestre "DESCUENTO:"

### **Test 2: Factura sin ICE**

- Crear factura con `subtotalIce = 0`
- ✅ Verificar que NO se muestre "ICE:"

### **Test 3: Factura sin Propina**

- Crear factura con `tip = 0` o `tip = null`
- ✅ Verificar que NO se muestre "Propina 10%:"

### **Test 4: Factura solo con IVA 0%**

- Crear factura con productos solo de IVA 0%
- ✅ Verificar que NO se muestren:
  - SUBTOTAL 15%
  - SUBTOTAL 5%
  - IVA 15%
  - IVA 5%
- ✅ Verificar que SÍ se muestren:
  - SUBTOTAL 0%
  - IVA 0% (si aplica, aunque normalmente IVA 0% = $0.00)

### **Test 5: Factura con Valores Decimales Muy Pequeños**

- Crear factura donde un cálculo resulte en `0.0001`
- ✅ Verificar que NO se muestre (debe redondearse a 0)

---

## 🎯 RESUMEN DE CAMBIOS NECESARIOS

### **Problema:**

Los campos con valor 0 se están mostrando cuando no deberían.

### **Causa Raíz:**

- Comparaciones directas pueden fallar con valores null/undefined
- Errores de precisión de punto flotante pueden resultar en valores muy pequeños
- Falta de métodos helper específicos para verificar existencia de valores

### **Solución:**

1. ✅ Crear método `isPositiveValue()` robusto
2. ✅ Crear métodos `has*()` para cada campo condicional
3. ✅ Usar estos métodos en el template en lugar de comparaciones directas
4. ✅ Asegurar que los cálculos redondeen correctamente

### **Archivos a Modificar:**

- `ticket-print.component.ts` - Agregar métodos helper
- `ticket-print.component.html` - Cambiar condiciones `*ngIf`

---

¿Quedó claro el problema y la solución? ¿Quieres que proceda con la implementación de estas correcciones?

