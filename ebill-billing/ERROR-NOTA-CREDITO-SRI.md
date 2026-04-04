# Error SRI: "No se ha encontrado un esquema para el tipo de comprobante"

## Error Recibido

```
<ns2:respuestaSolicitud xmlns:ns2="http://ec.gob.sri.ws.recepcion">
<estado>DEVUELTA</estado>
<comprobantes>
<comprobante>
<claveAcceso>N/A</claveAcceso>
<mensajes>
<mensaje>
<identificador>35</identificador>
<mensaje>ARCHIVO NO CUMPLE ESTRUCTURA XML</mensaje>
<informacionAdicional>Se encontró el siguiente error en la estructura del comprobante: No se ha encontrado un esquema para el tipo de comprobante TipoComprobante(=codigo: 3, codigoTipoDocumentoAdm: 4, nombre: Notas de Crédito, estado: ACT) en la version 2.1.0.</informacionAdicional>
<tipo>ERROR</tipo>
</mensaje>
</mensajes>
</comprobante>
</comprobantes>
</ns2:respuestaSolicitud>
```

## Análisis del Error

El error indica que:

1. El SRI está leyendo `codigo: 3` en lugar de `04` (código correcto para Nota de Crédito)
2. El `codigoTipoDocumentoAdm: 4` es correcto (corresponde a Nota de Crédito)
3. El SRI no encuentra un esquema para Nota de Crédito en la versión 2.1.0

## Posibles Causas

### 1. El SRI está leyendo el código desde la clave de acceso

El SRI podría estar interpretando el código del tipo de comprobante desde la clave de acceso en lugar del campo `<codDoc>` del XML. Si la clave de acceso tiene un código incorrecto, el SRI podría rechazar el XML.

**Solución:** Verificar que la clave de acceso se genere correctamente con el código "04" para Nota de Crédito.

### 2. Falta namespace o estructura específica en el XML

El SRI podría estar esperando un namespace específico o una estructura diferente en el XML. Comparar el XML generado para Nota de Crédito con el XML de Factura que funciona correctamente.

**Solución:** Verificar que el XML generado tenga la misma estructura que el XML de Factura, pero con los elementos específicos de Nota de Crédito.

### 3. El SRI no reconoce la estructura del XML para Nota de Crédito en la versión 2.1.0

El error menciona que no se encuentra un esquema para Nota de Crédito en la versión 2.1.0. Esto podría indicar que:

- El SRI espera una versión diferente del esquema
- Falta algún elemento requerido en el XML
- El namespace o la estructura del XML no coincide con lo que el SRI espera

**Solución:** Verificar la documentación del SRI para Nota de Crédito versión 2.1.0 y comparar con el XML generado.

## Verificaciones Necesarias

1. **Verificar el valor de `codDoc` en el XML generado:**

   - Debe ser `04` para Nota de Crédito
   - Se establece en `GenerateCreditNoteSri.crearInfoTributaria()` línea 350

2. **Verificar la clave de acceso:**

   - Debe generarse con el código "04" para Nota de Crédito
   - Se genera en `GenerateCreditNoteSri.generateAccessKey()` línea 413-425
   - Usa `TipoComprobanteEnum.NOTA_DE_CREDITO` que debería ser "04"

3. **Verificar la estructura del XML:**

   - Comparar el XML generado para Nota de Crédito con el XML de Factura
   - Verificar que todos los elementos requeridos estén presentes
   - Verificar que el namespace sea correcto (si aplica)

4. **Verificar la versión del XML:**
   - El XML debe tener `version="2.1.0"` en el elemento raíz
   - Se establece en `GenerateCreditNoteSri.createCreditNoteSri()` línea 48

## Próximos Pasos

1. Agregar logs para verificar qué valores se están estableciendo en el XML
2. Generar un XML de ejemplo y compararlo con la documentación del SRI
3. Verificar si hay algún XSD disponible para Nota de Crédito versión 2.1.0
4. Comparar el XML generado con un XML de Nota de Crédito que haya sido aceptado por el SRI

## Referencias

- Código de Nota de Crédito según SRI: `04`
- Versión del esquema: `2.1.0`
- Archivo de generación: `GenerateCreditNoteSri.java`
- Modelo XML: `NotaCredito.java`


