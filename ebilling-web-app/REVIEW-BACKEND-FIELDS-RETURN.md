# 🔍 Revisión: Campos `regime` y `specialTaxpayerResolution` en el Backend

## 📋 Análisis Realizado

Se revisó el backend Java (`ebill-customer`) para verificar si está devolviendo los campos `regime` y `specialTaxpayerResolution` correctamente.

---

## ✅ Resultado: El Backend YA tiene los Campos

### **1. Entidad `Company` (Entity)**

**Archivo:** `src/main/java/com/wallparisoft/ebill/customer/entity/Company.java`

```java
@Column(name = "special_taxpayer_resolution")
private String specialTaxpayerResolution;  // ✅ Línea 38-39

@Column(name = "regime", length = 50)
private String regime;  // ✅ Línea 53-54
```

**Estado:** ✅ Campos presentes en la entidad

---

### **2. DTO `CompanyDto`**

**Archivo:** `src/main/java/com/wallparisoft/ebill/customer/dto/CompanyDto.java`

```java
String specialTaxpayerResolution;  // ✅ Línea 25
String regime;  // ✅ Línea 32
```

**Estado:** ✅ Campos presentes en el DTO

---

### **3. DTO `CompanyDetailsDto`**

**Archivo:** `src/main/java/com/wallparisoft/ebill/customer/dto/CompanyDetailsDto.java`

```java
public class CompanyDetailsDto extends CompanyDto {
  // Hereda automáticamente los campos de CompanyDto:
  // - regime
  // - specialTaxpayerResolution
}
```

**Estado:** ✅ Campos heredados de `CompanyDto`

---

### **4. Mapper `CompanyMapper`**

**Archivo:** `src/main/java/com/wallparisoft/ebill/customer/mapper/CompanyMapper.java`

```java
@Mapper(componentModel = "spring")
public interface CompanyMapper {
  @Named("convertCompanyToCompanyDetailsDto")
  @Mapping(target = "contacts", ignore = true)
  @Mapping(target = "companiesIds", ignore = true)
  @Mapping(target = "isSuperAdmin", ignore = true)
  CompanyDetailsDto convertCompanyToCompanyDetailsDto(Company company);
}
```

**Análisis:**

- ✅ MapStruct mapea **automáticamente** campos con el mismo nombre
- ✅ Los campos `regime` y `specialTaxpayerResolution` tienen el **mismo nombre** en:
  - Entidad `Company` (entity)
  - DTO `CompanyDto`
- ✅ **NO hay ningún `@Mapping(target = "regime", ignore = true)`** que los ignore
- ✅ **Conclusión:** Los campos **SÍ se mapean automáticamente**

**Estado:** ✅ Mapper configurado correctamente

---

## 🔍 Migración de Base de Datos

**Archivo:** `V2025060303__add_regime_column.sql`

```sql
-- Agregar columna regime para almacenar el régimen del contribuyente
ALTER TABLE public.company
ADD COLUMN regime varchar(50);

COMMENT ON COLUMN public.company.regime IS
'Régimen del contribuyente: GENERAL, RIMPE_POPULAR, RIMPE_STARTUP, SPECIAL';
```

**Estado:** ✅ Migración existe (columna `regime`)

**Nota:** La columna `special_taxpayer_resolution` ya existía previamente.

---

## 📝 Frontend - Modelos Actualizados

### **1. Modelo `Company` (`company-establishment.ts`)**

**Estado:** ✅ Actualizado con los campos:

```typescript
export interface Company {
  // ... otros campos ...
  regime?: string; // ✅ Agregado
  specialTaxpayerResolution?: string; // ✅ Agregado
}
```

### **2. Modelo `CompanyFilter` (`company-filter.ts`)**

**Estado:** ✅ Ya tenía los campos:

```typescript
export interface CompanyFilter {
  // ... otros campos ...
  regime?: string; // ✅ Presente
  specialTaxpayerResolution?: string; // ✅ Presente
}
```

---

## 📤 Frontend - Payload de Factura

**Archivo:** `src/app/pages/billing/billing.component.ts`

**Método:** `buildInvoicePayload()`

**Estado:** ✅ Actualizado para incluir los campos:

```typescript
companyDto: {
  // ... otros campos ...
  regime: this.companyData?.regime || null, // ✅ Agregado
  specialTaxpayerResolution: this.companyData?.specialTaxpayerResolution || null, // ✅ Agregado
}
```

---

## ✅ Conclusión

### **El backend SÍ está devolviendo los campos correctamente:**

1. ✅ **Base de datos:** Las columnas existen (`regime` y `special_taxpayer_resolution`)
2. ✅ **Entidad Java:** Los campos están definidos en `Company`
3. ✅ **DTO Java:** Los campos están en `CompanyDto` y se heredan en `CompanyDetailsDto`
4. ✅ **Mapper:** MapStruct mapea automáticamente los campos (mismo nombre = mapeo automático)
5. ✅ **Frontend:** Los modelos están actualizados
6. ✅ **Payload:** El payload de la factura incluye los campos

---

## 🔍 Verificación Adicional Recomendada

### **Para confirmar que el backend está devolviendo los campos:**

1. **Revisar logs del backend** al consultar una compañía:

   ```bash
   # Buscar en los logs del backend Spring Boot
   # cuando se consulta GET /api/company/companies/{ids}
   ```

2. **Probar endpoint directamente:**

   ```bash
   curl -H "Authorization: Bearer {token}" \
        http://localhost:8180/api/company/companies/1
   ```

   **Verificar que la respuesta JSON incluya:**

   ```json
   {
     "companyDetailsDtos": [
       {
         "idCompany": 1,
         "name": "...",
         "regime": "GENERAL", // ← Verificar que existe
         "specialTaxpayerResolution": null // ← Verificar que existe
       }
     ]
   }
   ```

3. **Verificar en el navegador:**
   - Abrir DevTools → Network
   - Hacer una petición que cargue compañías
   - Ver la respuesta del endpoint `/api/company/companies/{ids}`
   - Verificar que `regime` y `specialTaxpayerResolution` estén en el JSON

---

## 🎯 Próximos Pasos si los Campos NO Aparecen

Si después de verificar, los campos **NO aparecen** en la respuesta del backend:

### **1. Verificar que la migración se ejecutó:**

```sql
-- Conectarse a la base de datos PostgreSQL
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'company'
  AND column_name IN ('regime', 'special_taxpayer_resolution');
```

**Debe mostrar ambas columnas.**

---

### **2. Verificar que el mapper está generado correctamente:**

El mapper de MapStruct genera código en tiempo de compilación. Verificar:

```bash
# En el proyecto backend (ebill-customer)
# Buscar el código generado por MapStruct:
find target/generated-sources -name "*CompanyMapperImpl*"

# O en IntelliJ IDEA:
# Build → Rebuild Project
```

---

### **3. Si los campos existen pero están null:**

Verificar que al guardar/actualizar una compañía desde el frontend, los campos se están enviando correctamente:

1. **Revisar el payload en Network tab** cuando se guarda una compañía
2. **Verificar que el backend está recibiendo** `regime` y `specialTaxpayerResolution`
3. **Verificar que el backend está guardando** en la base de datos

---

## 📊 Resumen de Cambios en Frontend

### **Modelos actualizados:**

1. ✅ `CompanyFilter` - Tiene `regime` y `specialTaxpayerResolution`
2. ✅ `Company` (company-establishment.ts) - Tiene `regime` y `specialTaxpayerResolution`

### **Componentes actualizados:**

1. ✅ `update-company.component.ts` - Envía los campos al guardar/actualizar
2. ✅ `billing.component.ts` - Incluye los campos en el payload de la factura

### **UI actualizada:**

1. ✅ Formulario de crear/editar compañía - Campos agregados
2. ✅ Tabla de compañías - Columna "Contribuyente" muestra el régimen

---

**Fecha de revisión:** 2025-01-20  
**Estado:** ✅ Backend configurado correctamente, frontend actualizado
