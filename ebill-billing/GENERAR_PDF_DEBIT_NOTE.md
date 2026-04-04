# 📄 Generar PDF de Nota de Débito para Validación

Este documento explica cómo generar un PDF de prueba usando la plantilla `debit_note_template.jrxml`.

## ✅ Estado

- ✅ Plantilla creada: `src/main/resources/jasper/debit_note_template.jrxml`
- ✅ Servicio implementado: `DebitNotePdfServiceImpl`
- ✅ Test creado: `DebitNotePdfServiceImplTest`
- ✅ XML de ejemplo: `src/test/resources/debit_note_autorizada_ejemplo.xml`

## 🚀 Formas de Generar el PDF

### Opción 1: Ejecutar el Test (Recomendado)

```bash
mvn test -Dtest=DebitNotePdfServiceImplTest#shouldGeneratePdfFromAuthorizationXml
```

El PDF se generará en un directorio temporal. La ubicación se mostrará en la consola.

### Opción 2: Ejecutar desde el IDE

1. Abre `src/test/java/com/wallparisoft/ebillbilling/service/DebitNotePdfServiceImplTest.java`
2. Ejecuta el método `shouldGeneratePdfFromAuthorizationXml()`
3. El PDF se generará y la ruta se mostrará en la consola

### Opción 3: Ejecutar la Clase de Validación

1. Abre `src/test/java/com/wallparisoft/ebillbilling/service/GenerateDebitNotePdfValidation.java`
2. Ejecuta el método `main()` desde tu IDE
3. El PDF se generará en `target/debit-note-pdf-validation/001-001-000000123.pdf`

## 📋 Contenido del PDF de Prueba

El PDF generado incluirá:

- **Información de la empresa:**
  - Razón Social: EMPRESA DE PRUEBA S.A.
  - RUC: 1790000000001
  - Dirección Matriz: AV. AMAZONAS Y COREA, QUITO

- **Información de la nota de débito:**
  - Número: 001-001-000000123
  - Fecha de emisión: 19/12/2025
  - Documento modificado: 001-001-000000500
  - Fecha documento sustento: 10/12/2025

- **Motivos:**
  - Intereses por mora en factura 500: $100.00
  - Recargos adicionales no contemplados: $50.00

- **Totales:**
  - Subtotal Sin Impuestos: $150.00
  - IVA 15%: $22.50
  - Valor Total: $172.50

- **Forma de pago:**
  - SIN UTILIZACION DEL SISTEMA FINANCIERO: $172.50

## ✅ Validaciones a Realizar

Al revisar el PDF generado, verifica:

1. ✅ **Encabezado:**
   - Logo de la empresa (o logo por defecto)
   - Título "NOTA DE DÉBITO" visible
   - Número de nota de débito correcto
   - Información tributaria completa

2. ✅ **Información del cliente:**
   - Razón Social: JUAN PEREZ CLIENTE
   - Identificación: 1712345678001
   - Fecha de emisión correcta

3. ✅ **Documento modificado:**
   - Documento que se modifica: 001-001-000000500
   - Fecha emisión documento sustento: 10/12/2025

4. ✅ **Sección de motivos:**
   - Tabla con columnas "Razón" y "Valor"
   - Dos motivos listados correctamente
   - Valores formateados con separadores de miles

5. ✅ **Totales:**
   - Subtotal Sin Impuestos: $150.00
   - IVA: $22.50
   - Valor Total: $172.50

6. ✅ **Formas de pago:**
   - Lista de formas de pago correctamente mostrada

7. ✅ **Información adicional:**
   - Email y teléfono del cliente

8. ✅ **Código de barras:**
   - Clave de acceso generada como código de barras

9. ✅ **Pie de página:**
   - Información de copyright
   - Mensaje de correo automático

## 🔧 Solución de Problemas

### Error: "No se pudo leer el XML de autorización"
- Verifica que el archivo `src/test/resources/debit_note_autorizada_ejemplo.xml` existe
- Verifica que el XML está bien formado

### Error: "Error al compilar/llenar el reporte Jasper"
- Verifica que la plantilla `debit_note_template.jrxml` existe en `src/main/resources/jasper/`
- Verifica que todos los parámetros requeridos están siendo pasados

### PDF generado pero vacío
- Verifica que el XML contiene todos los campos necesarios
- Revisa los logs para ver si hay errores de compilación de la plantilla

## 📝 Notas

- El PDF se genera usando JasperReports
- La plantilla está basada en la estructura de facturas pero adaptada para notas de débito
- Los motivos se muestran en lugar de detalles de productos
- El campo `valorTotal` se usa en lugar de `importeTotal`






