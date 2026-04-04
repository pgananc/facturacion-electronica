# SOLUCIÓN AL ERROR 500 AL GUARDAR PRODUCTOS

## 🔍 **Problema Identificado:**

El `ProductMapper` de MapStruct intenta mapear automáticamente `TaxTypeDto` → `TaxType`, pero falla porque:

- Solo recibe el DTO (con el ID)
- Necesita la **entidad completa** cargada desde la base de datos

## ✅ **Solución: Modificar ProductMapper.java**

**Ubicación:** `ebill-product/src/main/java/com/wallparisoft/ebill/product/mapper/ProductMapper.java`

### Reemplazar TODO el contenido del archivo con:

```java
package com.wallparisoft.ebill.product.mapper;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.entity.TaxType;
import com.wallparisoft.ebill.product.repository.ITaxTypeRepo;
import com.wallparisoft.ebill.product.response.ProductResponse;
import java.util.List;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ProductMapper {

  @Autowired
  protected ITaxTypeRepo taxTypeRepo;

  @Mapping(target = "status", source = "status")
  @Mapping(target = "creationDate", ignore = true)
  @Mapping(target = "updateDate", ignore = true)
  @Mapping(target = "taxType", expression = "java(mapTaxType(dto))")
  public abstract Product toProduct(ProductDto dto);

  @Mapping(
      target = "idProduct",
      expression = "java(product.getIdProduct() != null ? product.getIdProduct().intValue() : 0)")
  public abstract ProductDto toProductDto(Product product);

  @Mapping(target = "productDto", source = ".")
  @Mapping(target = "productDtos", ignore = true)
  public abstract ProductResponse toProductResponse(Product product);

  public ProductResponse toProductResponse(List<Product> products) {
    List<ProductDto> dtos = products.stream().map(this::toProductDto).toList();
    return ProductResponse.builder().productDtos(dtos).build();
  }

  /**
   * Custom mapper para TaxType
   * Carga la entidad TaxType desde la BD usando el ID del DTO
   */
  protected TaxType mapTaxType(ProductDto dto) {
    if (dto.getTaxType() == null || dto.getTaxType().getIdTaxType() == null) {
      return null;
    }
    return taxTypeRepo.findById(dto.getTaxType().getIdTaxType())
        .orElseThrow(() -> new RuntimeException("TaxType not found with ID: " + dto.getTaxType().getIdTaxType()));
  }
}
```

## 🔑 **Cambios Clave:**

### 1. **`interface` → `abstract class`**

```java
// ANTES
public interface ProductMapper {

// AHORA
public abstract class ProductMapper {
```

Necesario para poder usar `@Autowired`

### 2. **Inyectar el repositorio**

```java
@Autowired
protected ITaxTypeRepo taxTypeRepo;
```

### 3. **Custom mapping para taxType**

```java
@Mapping(target = "taxType", expression = "java(mapTaxType(dto))")
public abstract Product toProduct(ProductDto dto);
```

### 4. **Método helper `mapTaxType`**

```java
protected TaxType mapTaxType(ProductDto dto) {
  if (dto.getTaxType() == null || dto.getTaxType().getIdTaxType() == null) {
    return null;
  }
  return taxTypeRepo.findById(dto.getTaxType().getIdTaxType())
      .orElseThrow(() -> new RuntimeException("TaxType not found with ID: " + dto.getTaxType().getIdTaxType()));
}
```

## 🔨 **Después de Modificar:**

1. **Recompilar el backend:**

   ```bash
   cd ebill-product
   mvn clean compile
   ```

2. **O simplemente reiniciar el servidor Spring Boot**

   - MapStruct regenerará automáticamente el `ProductMapperImpl`

3. **Probar guardar un producto nuevamente**

## 📊 **Flujo Correcto:**

```
Frontend → { taxType: { idTaxType: 5 } }
    ↓
Backend ProductDto
    ↓
ProductMapper.mapTaxType()
    ↓
taxTypeRepo.findById(5) → Entidad TaxType completa
    ↓
Product.setTaxType(entidad)
    ↓
repo.save(product) → ✅ ÉXITO
```

## ⚠️ **Importante:**

- Este cambio es **CRÍTICO** para que funcione la relación `@ManyToOne`
- Sin este cambio, MapStruct no puede mapear correctamente el objeto anidado
- El mapper necesita **cargar la entidad completa** desde la BD antes de asignarla

---

**Después de aplicar estos cambios, el error 500 debería desaparecer!** 🚀
