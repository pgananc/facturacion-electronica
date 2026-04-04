# 📋 Plan de Implementación: Guías de Remisión

**Fecha:** Plan de implementación  
**Feature:** Guías de Remisión (Guide/GuiaRemision)  
**Código SRI:** 06  
**Estado:** ⏳ Pendiente de implementación

---

## 🎯 Resumen Ejecutivo

Este plan detalla los pasos para implementar la funcionalidad completa de Guías de Remisión siguiendo la propuesta actualizada en `PROPUESTA-ACTUALIZADA-GUIA-REMISION.md`.

---

## 📦 Fase 1: Base de Datos (Flyway Migrations)

### 1.1 Crear migración para tabla `carrier` (transportistas)

**Archivo:** `V{timestamp}__create_carrier_table.sql`

**Contenido:**

- ✅ Crear secuencia `carrier_seq`
- ✅ Crear tabla `carrier` con todos los campos
- ✅ Crear constraint `uk_carrier_company_identification`
- ✅ Crear índices: `idx_carrier_company`, `idx_carrier_identification`
- ✅ Agregar comentarios SQL

**Campos:**

- `id_carrier`, `id_company`, `identification_type`, `identification`, `name`, `address`, `phone`, `email`, `license_number`, `vehicle_type`, `status`, `creation_date`, `update_date`

---

### 1.2 Crear migración para tabla `guide` (guías de remisión)

**Archivo:** `V{timestamp}__create_guide_table.sql`

**Contenido:**

- ✅ Crear secuencia `guide_seq`
- ✅ Crear tabla `guide` con todos los campos
- ✅ Crear foreign keys: `guide_authorization_sri_id_authorization_sri_fk`, `guide_carrier_fk`
- ✅ Crear índices: `idx_guide_company`, `idx_guide_client`, `idx_guide_establishment`, `idx_guide_issuance_point`, `idx_guide_issue_date`, `idx_guide_number`, `idx_guide_authorization`, `idx_guide_carrier`
- ✅ Agregar comentarios SQL

**Campos principales:**

- Campos comunes: `id_guide`, `id_company`, `id_client`, `id_establishment`, `id_issuance_point`, `sequential`, `retry_count`, `issue_date`, `guide_number`, `environment`, `additional_information`, `email_sent`, `email_sent_date`, `id_authorization_sri`
- Campos específicos: `start_date`, `end_date`, `transport_type`, `transport_plate`, `id_carrier` (NOT NULL), `origin_address`, `destination_address`, `reason_for_transfer`, `route`
- Campos BaseEntity: `status`, `creation_date`, `update_date`

---

### 1.3 Crear migración para tabla `guide_detail`

**Archivo:** `V{timestamp}__create_guide_detail_table.sql`

**Contenido:**

- ✅ Crear secuencia `guide_detail_seq`
- ✅ Crear tabla `guide_detail`
- ✅ Crear foreign key `fk_guide_detail_guide` con `ON DELETE CASCADE`
- ✅ Crear índice `idx_guide_detail_guide`
- ✅ Agregar comentarios SQL

**Campos:**

- `id_guide_detail`, `id_guide`, `id_product`, `description`, `quantity`, `unit`, `weight`, `additional_info`, `status`, `creation_date`, `update_date`

---

### 1.4 Crear migración para tabla `guide_document_reference`

**Archivo:** `V{timestamp}__create_guide_document_reference_table.sql`

**Contenido:**

- ✅ Crear secuencia `guide_document_reference_seq`
- ✅ Crear tabla `guide_document_reference`
- ✅ Crear foreign key `fk_guide_document_reference_guide` con `ON DELETE CASCADE`
- ✅ Crear índices: `idx_guide_document_reference_guide`, `idx_guide_document_reference_type`
- ✅ Agregar comentarios SQL

**Campos:**

- `id_guide_document_reference`, `id_guide`, `document_type`, `document_number`, `access_key`, `issue_date`, `status`, `creation_date`, `update_date`

---

### 1.5 Activar trigger de suscripciones para `guide`

**Archivo:** `V{timestamp}__enable_guide_subscription_trigger.sql`

**Contenido:**

- ✅ Crear trigger `trigger_guide_usage` siguiendo el mismo patrón que `trigger_invoice_usage` y `trigger_credit_note_usage`
- ✅ La función `increment_document_usage()` ya tiene la lógica para `guide` (líneas 124-125 y 194-196)
- ✅ El trigger debe ejecutarse `AFTER INSERT` cuando `id_authorization_sri IS NOT NULL`
- ✅ Usar `DROP TRIGGER IF EXISTS` antes de crear para evitar errores en re-ejecución

**SQL:**

```sql
DROP TRIGGER IF EXISTS trigger_guide_usage ON guide;

CREATE TRIGGER trigger_guide_usage
AFTER INSERT ON guide
FOR EACH ROW
WHEN (NEW.id_authorization_sri IS NOT NULL)
EXECUTE FUNCTION increment_document_usage();
```

---

## 💻 Fase 2: Entidades JPA

### 2.1 Crear entidad `Carrier`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/entity/Carrier.java`

**Características:**

- ✅ Extender `BaseEntity`
- ✅ Anotaciones JPA: `@Entity`, `@Table(name = "carrier")`
- ✅ Secuencia: `carrier_seq`
- ✅ Campos: todos los de la tabla `carrier`
- ✅ Relación `@ManyToOne` con `Company` (si existe entidad, o solo `id_company`)
- ✅ Lombok: `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

### 2.2 Crear entidad `Guide`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/entity/Guide.java`

**Características:**

- ✅ Extender `BaseEntity`
- ✅ Anotaciones JPA: `@Entity`, `@Table(name = "guide")`
- ✅ Secuencia: `guide_seq`
- ✅ Campos: todos los de la tabla `guide`
- ✅ Relaciones:
  - `@ManyToOne` con `AuthorizationSri`
  - `@ManyToOne` con `Carrier`
  - `@OneToMany` con `GuideDetail` (cascade ALL, orphanRemoval)
  - `@OneToMany` con `GuideDocumentReference` (cascade ALL, orphanRemoval)
- ✅ Lombok: `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- ✅ `@JsonIgnoreProperties` en relaciones para evitar referencias circulares

---

### 2.3 Crear entidad `GuideDetail`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/entity/GuideDetail.java`

**Características:**

- ✅ Extender `BaseEntity`
- ✅ Anotaciones JPA: `@Entity`, `@Table(name = "guide_detail")`
- ✅ Secuencia: `guide_detail_seq`
- ✅ Campos: todos los de la tabla `guide_detail`
- ✅ Relación `@ManyToOne` con `Guide` (mappedBy)
- ✅ Lombok: `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

### 2.4 Crear entidad `GuideDocumentReference`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/entity/GuideDocumentReference.java`

**Características:**

- ✅ Extender `BaseEntity`
- ✅ Anotaciones JPA: `@Entity`, `@Table(name = "guide_document_reference")`
- ✅ Secuencia: `guide_document_reference_seq`
- ✅ Campos: todos los de la tabla `guide_document_reference`
- ✅ Relación `@ManyToOne` con `Guide` (mappedBy)
- ✅ Lombok: `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

## 📝 Fase 3: Repositorios (JPA Repositories)

### 3.1 Crear `CarrierRepository`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/repository/CarrierRepository.java`

**Métodos:**

- ✅ Extender `JpaRepository<Carrier, Long>`
- ✅ `List<Carrier> findByIdCompany(Long idCompany)`
- ✅ `Optional<Carrier> findByIdCompanyAndIdentification(Long idCompany, String identification)`
- ✅ `List<Carrier> findByIdCompanyAndStatusTrue(Long idCompany)`

---

### 3.2 Crear `GuideRepository`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/repository/GuideRepository.java`

**Métodos:**

- ✅ Extender `JpaRepository<Guide, Long>`
- ✅ `List<Guide> findByIdCompany(Long idCompany)`
- ✅ `Optional<Guide> findByGuideNumber(String guideNumber)`
- ✅ `List<Guide> findByIdCompanyAndIdClient(Long idCompany, Long idClient)`
- ✅ `Optional<Guide> findByIdIssuancePointAndSequential(Long idIssuancePoint, Long sequential)`
- ✅ `List<Guide> findByIdCompanyAndIssueDateBetween(Long idCompany, LocalDateTime start, LocalDateTime end)`

---

### 3.3 Crear `GuideDetailRepository`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/repository/GuideDetailRepository.java`

**Métodos:**

- ✅ Extender `JpaRepository<GuideDetail, Long>`
- ✅ `List<GuideDetail> findByIdGuide(Long idGuide)`

---

### 3.4 Crear `GuideDocumentReferenceRepository`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/repository/GuideDocumentReferenceRepository.java`

**Métodos:**

- ✅ Extender `JpaRepository<GuideDocumentReference, Long>`
- ✅ `List<GuideDocumentReference> findByIdGuide(Long idGuide)`
- ✅ `List<GuideDocumentReference> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber)`

---

## 🔧 Fase 4: DTOs (Data Transfer Objects)

### 4.1 Crear `CarrierDto`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/CarrierDto.java`

**Campos:**

- `idCarrier`, `idCompany`, `identificationType`, `identification`, `name`, `address`, `phone`, `email`, `licenseNumber`, `vehicleType`, `status`

---

### 4.2 Crear `GuideDto`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/GuideDto.java`

**Campos:**

- Todos los campos de `Guide` más:
- `ClientDto clientDto` (consultado desde ebill-customer)
- `CarrierDto carrierDto`
- `List<GuideDetailDto> guideDetails`
- `List<GuideDocumentReferenceDto> documentReferences`
- `AuthorizationSriDto authorizationSri` (opcional)

---

### 4.3 Crear `GuideDetailDto`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/GuideDetailDto.java`

**Campos:**

- `idGuideDetail`, `idGuide`, `idProduct`, `description`, `quantity`, `unit`, `weight`, `additionalInfo`
- `ProductDto productDto` (opcional, consultado desde ebill-product)

---

### 4.4 Crear `GuideDocumentReferenceDto`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/GuideDocumentReferenceDto.java`

**Campos:**

- `idGuideDocumentReference`, `idGuide`, `documentType`, `documentNumber`, `accessKey`, `issueDate`

---

### 4.5 Crear `GuideListResponseDto`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/GuideListResponseDto.java`

**Campos:**

- Campos principales para listado: `idGuide`, `guideNumber`, `issueDate`, `startDate`, `endDate`, `clientName`, `clientIdentification`, `carrierName`, `carrierIdentification`, `originAddress`, `destinationAddress`, `status`, `authorizationStatus`

---

### 4.6 Crear `CreateGuideRequestDto`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/CreateGuideRequestDto.java`

**Campos:**

- Todos los campos necesarios para crear una guía (sin IDs generados)

---

## 🎯 Fase 5: Servicios

### 5.1 Crear `CarrierService` e implementación

**Archivo Interface:** `src/main/java/com/wallparisoft/ebillbilling/service/CarrierService.java`  
**Archivo Implementación:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/CarrierServiceImpl.java`

**Métodos:**

- ✅ `List<CarrierDto> findAllByCompany(Long idCompany)`
- ✅ `Optional<CarrierDto> findById(Long id)`
- ✅ `CarrierDto create(CarrierDto carrierDto)`
- ✅ `CarrierDto update(Long id, CarrierDto carrierDto)`
- ✅ `void delete(Long id)` (soft delete)
- ✅ `List<CarrierDto> search(Long idCompany, String query)`

---

### 5.2 Crear `GuideService` e implementación

**Archivo Interface:** `src/main/java/com/wallparisoft/ebillbilling/service/GuideService.java`  
**Archivo Implementación:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/GuideServiceImpl.java`

**Métodos:**

- ✅ `List<GuideListResponseDto> findAllByCompany(Long idCompany)`
- ✅ `Optional<GuideDto> findById(Long id)`
- ✅ `GuideDto create(CreateGuideRequestDto requestDto)`
- ✅ `GuideDto update(Long id, CreateGuideRequestDto requestDto)`
- ✅ `void delete(Long id)` (soft delete)
- ✅ `List<GuideListResponseDto> search(Long idCompany, String query)`
- ✅ `List<GuideListResponseDto> findByClient(Long idCompany, Long idClient)`
- ✅ `GuideDto generateSequential(Long idCompany, Long idEstablishment, Long idIssuancePoint)` (similar a Invoice)
- ✅ `GuideDto authorize(Long idGuide, String password)` (generar XML y enviar al SRI)

---

### 5.3 Crear `GenerateGuideSri` (generación de XML)

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/GenerateGuideSri.java`

**Funcionalidad:**

- ✅ Generar XML según esquema del SRI para guías de remisión
- ✅ Firmar XML con certificado digital
- ✅ Enviar al SRI (recepción y autorización)
- ✅ Guardar respuesta en `authorization_sri`
- ✅ Actualizar `guide` con `id_authorization_sri`

**Referencia:** Similar a `GenerateInvoiceSri` o `GenerateDebitNoteSri`

---

## 🌐 Fase 6: Controladores REST (Controllers)

### 6.1 Crear `CarrierController`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/controller/CarrierController.java`

**Endpoints:**

- ✅ `GET /api/carrier` - Listar transportistas de la empresa
- ✅ `GET /api/carrier/{id}` - Obtener transportista por ID
- ✅ `POST /api/carrier` - Crear nuevo transportista
- ✅ `PUT /api/carrier/{id}` - Actualizar transportista
- ✅ `DELETE /api/carrier/{id}` - Eliminar transportista (soft delete)
- ✅ `GET /api/carrier/search?q={query}` - Buscar transportistas

---

### 6.2 Crear `GuideController`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/controller/GuideController.java`

**Endpoints:**

- ✅ `GET /api/guide` - Listar guías de la empresa
- ✅ `GET /api/guide/{id}` - Obtener guía por ID
- ✅ `POST /api/guide` - Crear nueva guía
- ✅ `PUT /api/guide/{id}` - Actualizar guía
- ✅ `DELETE /api/guide/{id}` - Eliminar guía (soft delete)
- ✅ `GET /api/guide/search?q={query}` - Buscar guías
- ✅ `GET /api/guide/by-client/{idClient}` - Listar guías de un cliente
- ✅ `POST /api/guide/{id}/authorize` - Autorizar guía en el SRI

---

## 🔄 Fase 7: Integraciones

### 7.1 Integración con `CustomerClient` (Feign)

**Archivo existente:** `src/main/java/com/wallparisoft/ebillbilling/service/CustomerClient.java`

**Uso:**

- ✅ Consultar datos del cliente destinatario (`id_client`) vía Feign
- ✅ Similar a como se hace en `InvoiceServiceImpl`

---

### 7.2 Integración con `ProductClient` (Feign)

**Archivo existente:** `src/main/java/com/wallparisoft/ebillbilling/service/ProductClient.java`

**Uso:**

- ✅ Consultar datos de productos en `guide_detail` (opcional, si `id_product` está presente)

---

### 7.3 Integración con secuenciales

**Funcionalidad:**

- ✅ Actualizar secuencial en `ebill-customer` cuando se autoriza una guía
- ✅ Similar a como se hace con facturas
- ✅ Usar `CustomerClient.updateSequential()`

---

## 🧪 Fase 8: Testing

### 8.1 Tests unitarios

**Archivos:**

- ✅ `CarrierServiceTest.java`
- ✅ `GuideServiceTest.java`
- ✅ `GuideRepositoryTest.java`

---

### 8.2 Tests de integración

**Archivos:**

- ✅ `CarrierControllerIntegrationTest.java`
- ✅ `GuideControllerIntegrationTest.java`
- ✅ `GenerateGuideSriIntegrationTest.java`

---

## 📚 Fase 9: Documentación

### 9.1 Documentación de API

**Archivo:** Actualizar `API-ENDPOINTS.md`

**Contenido:**

- ✅ Documentar todos los endpoints de `CarrierController`
- ✅ Documentar todos los endpoints de `GuideController`
- ✅ Ejemplos de requests/responses
- ✅ Códigos de error

---

### 9.2 Documentación técnica

**Archivo:** `GUIA-REMISION-IMPLEMENTACION.md`

**Contenido:**

- ✅ Resumen de la implementación
- ✅ Diagramas de flujo
- ✅ Ejemplos de uso
- ✅ Troubleshooting

---

## ✅ Checklist de Validación

### Base de Datos

- [ ] Migraciones Flyway ejecutadas correctamente
- [ ] Tablas creadas con todos los campos
- [ ] Foreign keys funcionando
- [ ] Índices creados
- [ ] Trigger de suscripciones activo

### Entidades

- [ ] Todas las entidades creadas
- [ ] Relaciones JPA correctas
- [ ] Secuencias funcionando
- [ ] Lombok configurado

### Servicios

- [ ] CRUD completo para Carrier
- [ ] CRUD completo para Guide
- [ ] Generación de XML funcionando
- [ ] Integración con SRI funcionando
- [ ] Integración con Feign Clients funcionando

### API

- [ ] Todos los endpoints funcionando
- [ ] Validaciones implementadas
- [ ] Manejo de errores
- [ ] Documentación Swagger/OpenAPI

### Testing

- [ ] Tests unitarios pasando
- [ ] Tests de integración pasando
- [ ] Cobertura de código adecuada

---

## 📊 Orden de Implementación Recomendado

1. **Fase 1** (Base de Datos) - Base fundamental
2. **Fase 2** (Entidades) - Modelo de datos
3. **Fase 3** (Repositorios) - Acceso a datos
4. **Fase 4** (DTOs) - Capa de transferencia
5. **Fase 5.1** (CarrierService) - Servicio de transportistas primero
6. **Fase 6.1** (CarrierController) - API de transportistas
7. **Fase 5.2** (GuideService) - Servicio de guías
8. **Fase 5.3** (GenerateGuideSri) - Generación XML
9. **Fase 6.2** (GuideController) - API de guías
10. **Fase 7** (Integraciones) - Conectar con otros servicios
11. **Fase 8** (Testing) - Asegurar calidad
12. **Fase 9** (Documentación) - Documentar todo

---

## ⚠️ Consideraciones Importantes

1. **Secuenciales:** Seguir el mismo patrón que `Invoice` para generar secuenciales por punto de emisión
2. **Reintentos:** Implementar sistema de reintentos similar a facturas (`retry_count`)
3. **Validaciones:** Validar que el transportista existe antes de crear guía
4. **SRI:** Revisar esquema XML del SRI para guías de remisión (código 06)
5. **Suscripciones:** El trigger ya está preparado, solo necesita activarse
6. **Email:** Implementar envío de emails similar a facturas (opcional)

---

**Estado:** ⏳ Listo para implementación  
**Próximo paso:** Iniciar Fase 1 (Base de Datos)
