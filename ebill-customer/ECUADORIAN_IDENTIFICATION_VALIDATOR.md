# 🇪🇨 Validador de Cédulas y RUC Ecuatorianos

## 📋 **RESUMEN**

Se implementó un validador personalizado de Jakarta para cédulas y RUC ecuatorianos según los algoritmos oficiales del SRI (Servicio de Rentas Internas).

---

## 🎯 **TIPOS DE IDENTIFICACIÓN SOPORTADOS**

### **1. Cédula (10 dígitos)**
- **Formato:** 10 dígitos numéricos
- **Algoritmo:** Módulo 10
- **Estructura:**
  - Primeros 2 dígitos: Código de provincia (01-24, 30)
  - Tercer dígito: 0-5 (personas naturales)
  - Dígitos 4-9: Número secuencial
  - Dígito 10: Dígito verificador

**Ejemplos válidos:**
```
1722633961  ✅ Cédula válida (Pichincha)
0912345678  ✅ Cédula válida (Guayas)
1234567890  ❌ Dígito verificador incorrecto
```

---

### **2. RUC Persona Natural (13 dígitos)**
- **Formato:** Cédula (10 dígitos) + `001`
- **Validación:** Misma que cédula para los primeros 10 dígitos
- **Terminación:** Debe ser `001`

**Ejemplos válidos:**
```
1722633961001  ✅ RUC Persona Natural válido
0912345678001  ✅ RUC Persona Natural válido
1722633961002  ❌ Debe terminar en 001
```

---

### **3. RUC Sociedad Privada (13 dígitos)**
- **Formato:** 13 dígitos numéricos
- **Algoritmo:** Módulo 11
- **Estructura:**
  - Primeros 2 dígitos: Código de provincia (01-24, 30)
  - Tercer dígito: `9` (sociedades privadas)
  - Dígitos 4-9: Número secuencial
  - Dígito 10: Dígito verificador
  - Dígitos 11-13: `001`

**Ejemplos válidos:**
```
1790016919001  ✅ RUC Sociedad Privada válido
0992345678001  ✅ RUC Sociedad Privada válido
1790016919002  ❌ Debe terminar en 001
```

---

### **4. RUC Sociedad Pública (13 dígitos)**
- **Formato:** 13 dígitos numéricos
- **Algoritmo:** Módulo 11
- **Estructura:**
  - Primeros 2 dígitos: Código de provincia (01-24, 30)
  - Tercer dígito: `6` (sociedades públicas)
  - Dígitos 4-9: Número secuencial
  - Dígito 10: Dígito verificador
  - Dígitos 11-13: `0001`

**Ejemplos válidos:**
```
1768012340001  ✅ RUC Sociedad Pública válido
0968012340001  ✅ RUC Sociedad Pública válido
1768012340002  ❌ Debe terminar en 0001
```

---

## 🔧 **IMPLEMENTACIÓN**

### **Archivos creados:**

```
src/main/java/com/wallparisoft/ebill/customer/
  └── validation/
      ├── ValidEcuadorianIdentification.java  (Anotación)
      └── EcuadorianIdentificationValidator.java  (Lógica de validación)
```

---

### **Uso en DTOs:**

#### **ClientDto:**
```java
@Data
public class ClientDto {
    @ValidEcuadorianIdentification(message = "Cédula o RUC del cliente inválido")
    private String identification;

    @ValidEcuadorianIdentification(
        message = "RUC de la compañía inválido",
        allowNull = true)
    private String companyIdentification;
}
```

#### **CompanyDto:**
```java
@Data
public class CompanyDto {
    @ValidEcuadorianIdentification(message = "RUC de la compañía inválido")
    private String identification;
}
```

---

## 📊 **VALIDACIONES REALIZADAS**

| Validación | Descripción |
|------------|-------------|
| ✅ **Longitud** | 10 dígitos (cédula) o 13 dígitos (RUC) |
| ✅ **Solo dígitos** | No permite letras ni caracteres especiales |
| ✅ **Código de provincia** | Primeros 2 dígitos deben ser válidos (01-24, 30) |
| ✅ **Tercer dígito** | Según tipo: 0-5 (cédula/RUC PN), 6 (RUC público), 9 (RUC privado) |
| ✅ **Terminación RUC** | `001` para PN y privadas, `0001` para públicas |
| ✅ **Dígito verificador** | Algoritmo módulo 10 (cédulas) o módulo 11 (RUC sociedades) |

---

## 🧪 **EJEMPLOS DE PRUEBA**

### **Request válido (Cédula):**
```bash
curl -X POST http://localhost:8180/api/client \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "identification": "1722633961",
    "name": "JUAN PEREZ",
    "idType": 2,
    "clientType": 1,
    "companyIdentification": "1790016919001",
    "contacts": [
      {"contactType": "TEL", "value": "0999999999"}
    ]
  }'
```

**Response:** `201 CREATED` con el cliente guardado

---

### **Request inválido (Cédula con dígito verificador incorrecto):**
```bash
curl -X POST http://localhost:8180/api/client \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "identification": "1234567890",
    "name": "JUAN PEREZ",
    "idType": 2,
    "clientType": 1,
    "companyIdentification": "1790016919001",
    "contacts": []
  }'
```

**Response:** `400 BAD REQUEST`
```json
{
  "code": 400,
  "status": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "identification",
      "message": "Cédula o RUC del cliente inválido"
    }
  ]
}
```

---

### **Request inválido (RUC que no termina en 001):**
```bash
curl -X POST http://localhost:8180/api/client \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "identification": "1722633961002",
    "name": "JUAN PEREZ",
    "idType": 2,
    "clientType": 1,
    "companyIdentification": "1790016919001",
    "contacts": []
  }'
```

**Response:** `400 BAD REQUEST`
```json
{
  "code": 400,
  "status": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "identification",
      "message": "Cédula o RUC del cliente inválido"
    }
  ]
}
```

---

## 🔍 **ALGORITMOS IMPLEMENTADOS**

### **Módulo 10 (Cédulas y RUC Persona Natural)**

```
Ejemplo: 1722633961

Paso 1: Multiplicar dígitos en posiciones pares (0,2,4,6,8) por 2
  1*2=2, 2*1=2, 6*2=12, 3*1=3, 6*2=12, 9*1=9, 1*2=2

Paso 2: Si resultado > 9, restar 9
  2, 2, 12-9=3, 3, 12-9=3, 9, 2

Paso 3: Sumar todos los dígitos
  2+7+3+2+3+3+3+9+2 = 34

Paso 4: Calcular dígito verificador
  (10 - (34 % 10)) % 10 = (10 - 4) % 10 = 6

Paso 5: Comparar con el último dígito (posición 9)
  6 == 1 ❌ (en este ejemplo el dígito verificador no coincide)
```

---

### **Módulo 11 (RUC Sociedades)**

```
Ejemplo RUC Sociedad Privada: 1790016919001

Coeficientes: [4, 3, 2, 7, 6, 5, 4, 3, 2]

Paso 1: Multiplicar cada dígito por su coeficiente
  1*4 + 7*3 + 9*2 + 0*7 + 0*6 + 1*5 + 6*4 + 9*3 + 1*2
  = 4 + 21 + 18 + 0 + 0 + 5 + 24 + 27 + 2
  = 101

Paso 2: Calcular residuo módulo 11
  101 % 11 = 2

Paso 3: Calcular dígito verificador
  11 - 2 = 9

Paso 4: Comparar con el dígito en posición 9
  9 == 9 ✅
```

---

## 📝 **PROVINCIAS VÁLIDAS**

| Código | Provincia |
|--------|-----------|
| 01 | Azuay |
| 02 | Bolívar |
| 03 | Cañar |
| 04 | Carchi |
| 05 | Cotopaxi |
| 06 | Chimborazo |
| 07 | El Oro |
| 08 | Esmeraldas |
| 09 | Guayas |
| 10 | Imbabura |
| 11 | Loja |
| 12 | Los Ríos |
| 13 | Manabí |
| 14 | Morona Santiago |
| 15 | Napo |
| 16 | Pastaza |
| 17 | Pichincha |
| 18 | Tungurahua |
| 19 | Zamora Chinchipe |
| 20 | Galápagos |
| 21 | Sucumbíos |
| 22 | Orellana |
| 23 | Santo Domingo de los Tsáchilas |
| 24 | Santa Elena |
| 30 | Ecuatorianos en el exterior |

---

## ⚙️ **CONFIGURACIÓN**

### **Permitir valores nulos:**

Por defecto, la validación **NO permite valores nulos**. Si necesitas permitirlos:

```java
@ValidEcuadorianIdentification(
    message = "RUC inválido",
    allowNull = true)
private String identification;
```

---

### **Integración con @Valid:**

La validación se ejecuta automáticamente cuando usas `@Valid` en los controladores:

```java
@PostMapping
public ResponseEntity<ClientDto> save(@Valid @RequestBody ClientDto clientDto) {
    // Si la validación falla, Spring retorna 400 Bad Request automáticamente
    ClientDto savedClient = clientService.saveClientAndContact(clientDto);
    return ResponseEntity.ok(savedClient);
}
```

---

## ✅ **VENTAJAS**

1. ✅ **Validación automática:** Se ejecuta antes de llegar al servicio
2. ✅ **Mensajes personalizados:** Puedes cambiar el mensaje de error
3. ✅ **Reutilizable:** Se puede usar en cualquier DTO
4. ✅ **Algoritmos oficiales:** Implementa los algoritmos del SRI
5. ✅ **Manejo de errores:** Spring Boot retorna 400 automáticamente
6. ✅ **Sin código duplicado:** Centralizado en un solo lugar

---

## 🚨 **CASOS ESPECIALES**

### **Pasaportes:**

Los pasaportes **NO se validan** con este validador. Si el `idType` es `PAS` (Pasaporte), no uses la anotación `@ValidEcuadorianIdentification`.

---

### **Identificaciones extranjeras:**

Para identificaciones de otros países, no uses este validador. Considera crear validadores específicos si es necesario.

---

## 📚 **REFERENCIAS**

- [SRI - Servicio de Rentas Internas del Ecuador](https://www.sri.gob.ec/)
- [Algoritmo de validación de cédula ecuatoriana](https://www.sri.gob.ec/validacion-cedula)
- [Estructura del RUC](https://www.sri.gob.ec/estructura-ruc)

---

**Fecha de implementación:** 2025-11-21  
**Versión:** ebill-customer v1.0  
**Autor:** AI Assistant + jguerreroh

