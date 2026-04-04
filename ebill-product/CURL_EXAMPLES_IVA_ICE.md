# 🚀 Ejemplos CURL - Separación IVA e ICE

## 📋 TaxType Endpoints Mejorados

### 1. Obtener TODOS los TaxTypes activos

```bash
curl --location 'http://localhost:8380/api/tax-type?status=true' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

**Uso:** Cache global, obtiene IVA + ICE en una sola llamada

---

### 2. Obtener solo IVAs activos

```bash
curl --location 'http://localhost:8380/api/tax-type?status=true&type=IVA' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

**Uso:** Poblar combo específico de IVA (5 opciones)

---

### 3. Obtener solo ICEs activos

```bash
curl --location 'http://localhost:8380/api/tax-type?status=true&type=ICE' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

**Uso:** Poblar combo específico de ICE (30+ opciones)

---

## 🛍️ Product con IVA e ICE

### Respuesta esperada:

```json
{
  "code": 200,
  "status": "OK",
  "productDto": {
    "idProduct": 1,
    "idCompany": 1,
    "mainCode": "001",
    "name": "MACBOOK PRO",
    "description": "M4, 512 SSD, 16GB Ram, Año 2024",
    "unitPrice": 2500.0,
    "productType": 1,
    "ice": false,
    "tourismIva": false,
    "status": true,
    "ivaTaxType": {
      "idTaxType": 5,
      "type": "IVA",
      "name": "15%",
      "value": 15.0,
      "status": true
    },
    "iceTaxType": null
  }
}
```

### Ejemplo con ICE:

```json
{
  "idProduct": 2,
  "name": "Whisky Importado",
  "ivaTaxType": {
    "idTaxType": 5,
    "type": "IVA",
    "name": "15%",
    "value": 15.0
  },
  "iceTaxType": {
    "idTaxType": 10,
    "type": "ICE",
    "name": "3031 ICE-BEBIDAS ALCOHÓLICAS",
    "value": 0.0
  },
  "ice": true,
  "tourismIva": false
}
```

---

## 🎯 Estrategias Frontend

### Estrategia 1: Cache + Filtro Local (Recomendado para formularios completos)

```javascript
// Una sola llamada
const allTaxTypes = await getTaxTypes({ status: true });

// Filtrado local
const ivaOptions = allTaxTypes.filter((t) => t.type === "IVA");
const iceOptions = allTaxTypes.filter((t) => t.type === "ICE");

// Usar en combos
<Select options={ivaOptions} name="ivaTaxType" />;
{
  formik.values.ice && <Select options={iceOptions} name="iceTaxType" />;
}
```

### Estrategia 2: Queries Específicos (Recomendado para combos individuales)

```javascript
// Solo cuando se necesita
const ivaOptions = await getTaxTypes({ status: true, type: "IVA" });

// ICE solo si el flag está activo
const iceOptions = formik.values.ice
  ? await getTaxTypes({ status: true, type: "ICE" })
  : [];
```

---

## 📊 Campos del Producto

| Campo        | Tipo       | Descripción                                        |
| ------------ | ---------- | -------------------------------------------------- |
| `ivaTaxType` | TaxTypeDto | **Obligatorio** - IVA del producto                 |
| `iceTaxType` | TaxTypeDto | **Opcional** - ICE del producto (nullable)         |
| `ice`        | Boolean    | **Flag UI** - true=muestra combo ICE, false=oculta |
| `tourismIva` | Boolean    | **Flag especial** - IVA reducido en feriados       |

---

## 🗄️ Cambios en Base de Datos

### Antes:

```sql
product.id_tax_type → FK a tax_type (IVA o ICE)
product.has_ice → Boolean indicador
```

### Después:

```sql
product.id_iva_tax_type → FK a tax_type WHERE type='IVA' (obligatorio)
product.id_ice_tax_type → FK a tax_type WHERE type='ICE' (opcional)
product.has_ice → Boolean flag UI (mantiene funcionalidad)
```

---

## 🔧 Migración Flyway

Archivo: `V2025111900__separate_iva_ice_tax_types.sql`

- ✅ Renombra columna: `id_tax_type` → `id_iva_tax_type`
- ✅ Agrega columna: `id_ice_tax_type`
- ✅ Constraints e índices configurados
- ✅ Comentarios documentados en DB

---

## ✨ Performance

Todas las queries optimizadas con LEFT JOIN FETCH:

```sql
SELECT DISTINCT p
FROM Product p
LEFT JOIN FETCH p.ivaTaxType
LEFT JOIN FETCH p.iceTaxType
WHERE ...
```

**No hay problema N+1** ✅

---

## 🎨 Ejemplo Formik (Frontend)

```javascript
const validationSchema = Yup.object({
  ivaTaxType: Yup.object().required("IVA es obligatorio"),
  iceTaxType: Yup.object().when("ice", {
    is: true,
    then: Yup.object().nullable(),
    otherwise: Yup.object().nullable(),
  }),
  ice: Yup.boolean(),
  tourismIva: Yup.boolean(),
});

const initialValues = {
  name: "",
  ivaTaxType: null,
  iceTaxType: null,
  ice: false,
  tourismIva: false,
};
```

---

¡Todo listo para usar! 🚀
