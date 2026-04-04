# Solución: Agregar Procesamiento de Notas de Crédito en Tarea Programada

## Resumen

Se implementará el procesamiento de notas de crédito en la tarea programada `executeTask`, similar a como se procesan las facturas en `executeSendInvoiceToSri`.

## Cambios Requeridos

### 1. Agregar método en `ICreditNoteRepo`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/repository/ICreditNoteRepo.java`

```java
@Query(
    "SELECT cn FROM CreditNote cn WHERE "
        + "cn.status = true AND "
        + "((cn.authorizationSri.receptionStatusSri IS NULL OR cn.authorizationSri.receptionStatusSri <> :receptionStatus) "
        + "OR (cn.authorizationSri.authorizationStatusSri IS NULL OR cn.authorizationSri.authorizationStatusSri <> :authorizationStatus))")
List<CreditNote> findCreditNotesExcludingSpecificSriStatuses(
    @Param("receptionStatus") String receptionStatus,
    @Param("authorizationStatus") String authorizationStatus);
```

### 2. Implementar `executeSendCreditNoteToSri` en `CreditNoteServiceImpl`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/CreditNoteServiceImpl.java`

Reemplazar el método actual (líneas 238-242) con la implementación completa:

```java
@Override
public void executeSendCreditNoteToSri() {
  // Generar token para las tareas programadas
  TokenInformation tokenInformation = new TokenInformation();
  tokenInformation.setUserName("SCHEDULED_TASK");
  tokenInformation.setRoles(List.of("SCHEDULED_TASK"));
  tokenInformation.setCompanyEstablishments(new ArrayList<>());

  // Generar token con expiración de 60 minutos
  TokenSession tokenSession;
  try {
    tokenSession = authService.generateTokenForCron(tokenInformation, 60);
    log.debug("Generated token for scheduled task execution");
  } catch (Exception e) {
    log.error("Failed to generate token for scheduled task: {}", e.getMessage(), e);
    return;
  }

  // Establecer el token en el contexto de seguridad
  SecurityContext previousContext = SecurityContextHolder.getContext();
  SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
  Authentication authentication =
      new UsernamePasswordAuthenticationToken(
          "SCHEDULED_TASK",
          tokenSession.getToken(),
          List.of(new SimpleGrantedAuthority("ROLE_SCHEDULED_TASK")));
  securityContext.setAuthentication(authentication);
  SecurityContextHolder.setContext(securityContext);

  try {
    List<CreditNote> creditNotes =
        this.repo.findCreditNotesExcludingSpecificSriStatuses("RECIBIDA", "AUTORIZADO");
    log.info("Found {} credit notes pending to send to SRI", creditNotes.size());

    // ===== OPTIMIZACIÓN: Pre-cargar datos únicos para evitar consultas duplicadas =====

    // 1. Extraer IDs únicos de companies, clients, issuance points y products
    Set<Long> uniqueCompanyIds =
        creditNotes.stream()
            .map(CreditNote::getIdCompany)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Set<Long> uniqueClientIds =
        creditNotes.stream()
            .map(CreditNote::getIdClient)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Set<Long> uniqueIssuancePointIds =
        creditNotes.stream()
            .map(CreditNote::getIdIssuancePoint)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Set<Long> uniqueProductIds =
        creditNotes.stream()
            .filter(cn -> Objects.nonNull(cn.getCreditNoteDetails()))
            .flatMap(cn -> cn.getCreditNoteDetails().stream())
            .map(com.wallparisoft.ebillbilling.entity.CreditNoteDetail::getIdProduct)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    log.info(
        "Found {} unique companies, {} unique clients, {} unique issuance points, and {} unique products to process",
        uniqueCompanyIds.size(),
        uniqueClientIds.size(),
        uniqueIssuancePointIds.size(),
        uniqueProductIds.size());

    // 2. Pre-cargar firmas digitales (una consulta por company)
    Map<Long, DigitalSignatureDto> digitalSignatureCache = new HashMap<>();
    for (Long companyId : uniqueCompanyIds) {
      try {
        DigitalSignatureResponse response =
            digitalSignatureClient.getDigitalSignatureByCompanyId(companyId);
        if (Objects.nonNull(response) && Objects.nonNull(response.getDigitalSignatureDto())) {
          digitalSignatureCache.put(companyId, response.getDigitalSignatureDto());
          log.debug("Cached digital signature for company {}", companyId);
        } else {
          log.warn("Digital signature not found for company {}", companyId);
        }
      } catch (Exception e) {
        log.error(
            "Error loading digital signature for company {}: {}", companyId, e.getMessage());
      }
    }

    // 3. Pre-cargar clientes (una consulta por client)
    Map<Long, ClientDto> clientCache = new HashMap<>();
    for (Long clientId : uniqueClientIds) {
      try {
        ClientDtoResponse response = customerClient.getClientById(clientId);
        if (Objects.nonNull(response)
            && !CollectionUtils.isEmpty(response.getClientDtos())
            && Objects.nonNull(response.getClientDtos().get(0))) {
          clientCache.put(clientId, response.getClientDtos().get(0));
          log.debug("Cached client data for client {}", clientId);
        } else {
          log.warn("Client not found for clientId {}", clientId);
        }
      } catch (Exception e) {
        log.error("Error loading client {}: {}", clientId, e.getMessage());
      }
    }

    // 4. Pre-cargar establecimientos con puntos de emisión (una consulta por issuance point)
    Map<Long, EstablishmentDto> establishmentCache = new HashMap<>();
    for (Long idIssuancePoint : uniqueIssuancePointIds) {
      try {
        EstablishmentDto establishmentDto =
            customerClient.getEstablishmentWithIssuancePoint(idIssuancePoint);
        if (Objects.nonNull(establishmentDto)) {
          establishmentCache.put(idIssuancePoint, establishmentDto);
          log.debug("Cached establishment data for issuance point {}", idIssuancePoint);
        } else {
          log.warn("Establishment not found for issuance point {}", idIssuancePoint);
        }
      } catch (Exception e) {
        log.error(
            "Error loading establishment for issuance point {}: {}",
            idIssuancePoint,
            e.getMessage());
      }
    }

    // 5. Pre-cargar compañías (una consulta por company)
    Map<Long, CompanyDto> companyCache = new HashMap<>();
    for (Long companyId : uniqueCompanyIds) {
      try {
        CompanyDto companyDto = customerClient.getCompanyById(companyId);
        if (Objects.nonNull(companyDto)) {
          companyCache.put(companyId, companyDto);
          log.debug("Cached company data for company {}", companyId);
        } else {
          log.warn("Company not found for companyId {}", companyId);
        }
      } catch (Exception e) {
        log.error("Error loading company {}: {}", companyId, e.getMessage());
      }
    }

    // 6. Pre-cargar productos (una consulta por product)
    Map<Long, ProductDto> productCache = new HashMap<>();
    for (Long productId : uniqueProductIds) {
      try {
        com.wallparisoft.ebillbilling.response.ProductResponse productResponse =
            productClient.getProductById(productId);
        if (Objects.nonNull(productResponse)
            && Objects.nonNull(productResponse.getProductDto())) {
          // Mapear ProductDto del servicio de productos al ProductDto de ebill-billing
          ProductDto productDto = mapProductDtoFromService(productResponse.getProductDto());
          if (Objects.nonNull(productDto)) {
            productCache.put(productId, productDto);
            log.debug("Cached product data for product {}", productId);
          } else {
            log.warn("Failed to map product data for productId {}", productId);
          }
        } else {
          log.warn("Product not found for productId {}", productId);
        }
      } catch (Exception e) {
        log.error("Error loading product {}: {}", productId, e.getMessage());
      }
    }

    // 7. Procesar notas de crédito usando el cache
    creditNotes.forEach(
        creditNote -> {
          // Inicializar retryCount si es null
          if (Objects.isNull(creditNote.getRetryCount())) {
            creditNote.setRetryCount(0);
          }

          // Verificar si ya se agotaron los reintentos
          if (creditNote.getRetryCount() >= 5) {
            log.warn(
                "Credit note {} has exceeded maximum retries ({}), marking as inactive",
                creditNote.getIdCreditNote(),
                creditNote.getRetryCount());
            deactivateCreditNoteAndDetails(creditNote);
            return;
          }

          try {
            // Usar cache en lugar de consultar cada vez
            DigitalSignatureDto digitalSignatureDto =
                digitalSignatureCache.get(creditNote.getIdCompany());

            if (Objects.isNull(digitalSignatureDto)) {
              log.warn(
                  "Digital signature not found in cache for company {} - Credit Note {}",
                  creditNote.getIdCompany(),
                  creditNote.getIdCreditNote());
              handleCreditNoteFailure(creditNote, "Digital signature not found");
              return;
            }

            CreditNoteDto creditNoteDtoForSri = serviceMapper.toCreditNoteDto(creditNote);

            // Usar cache para compañía si está disponible
            CompanyDto companyDtoFromCache = companyCache.get(creditNote.getIdCompany());
            if (Objects.nonNull(companyDtoFromCache)) {
              creditNoteDtoForSri.setCompanyDto(companyDtoFromCache);
              log.debug("Assigned company to credit note {} from cache", creditNote.getIdCreditNote());
            } else {
              log.warn(
                  "Company not found in cache for company {} - Credit Note {}",
                  creditNote.getIdCompany(),
                  creditNote.getIdCreditNote());
              handleCreditNoteFailure(creditNote, "Company not found");
              return;
            }

            // Usar cache para cliente si está disponible
            ClientDto clientDtoFromCache = clientCache.get(creditNote.getIdClient());
            if (Objects.nonNull(clientDtoFromCache)) {
              creditNoteDtoForSri.setClientDto(clientDtoFromCache);
            }

            // Usar cache para establecimiento si está disponible
            if (Objects.nonNull(creditNote.getIdIssuancePoint())) {
              EstablishmentDto establishmentDto =
                  establishmentCache.get(creditNote.getIdIssuancePoint());
              if (Objects.nonNull(establishmentDto)) {
                creditNoteDtoForSri.setEstablishment(establishmentDto);
                // Actualizar el sequential en el issuancePoint si está disponible en la nota de crédito
                if (Objects.nonNull(creditNote.getSequential())
                    && Objects.nonNull(establishmentDto.getIssuancePoint())) {
                  String formattedSequential = String.format("%09d", creditNote.getSequential());
                  establishmentDto.getIssuancePoint().setSequentialInvoice(formattedSequential);
                }
                log.debug(
                    "Assigned establishment to credit note {} from cache", creditNote.getIdCreditNote());
              } else {
                log.warn(
                    "Establishment not found in cache for issuance point {} - Credit Note {}",
                    creditNote.getIdIssuancePoint(),
                    creditNote.getIdCreditNote());
                handleCreditNoteFailure(creditNote, "Establishment not found");
                return;
              }
            } else {
              log.warn(
                  "Credit note {} does not have idIssuancePoint - cannot load establishment",
                  creditNote.getIdCreditNote());
              handleCreditNoteFailure(creditNote, "Credit note does not have issuance point");
              return;
            }

            // Asignar ProductDto a cada CreditNoteDetailDto desde el cache
            if (Objects.nonNull(creditNoteDtoForSri.getCreditNoteDetails())) {
              for (InvoiceDetailDto detailDto : creditNoteDtoForSri.getCreditNoteDetails()) {
                if (Objects.nonNull(detailDto.getIdProduct())) {
                  ProductDto productDto = productCache.get(detailDto.getIdProduct());
                  if (Objects.nonNull(productDto)) {
                    detailDto.setProductDto(productDto);
                    log.debug(
                        "Assigned product {} to credit note detail {} from cache",
                        detailDto.getIdProduct(),
                        detailDto.getIdInvoiceDetail());
                  } else {
                    log.warn(
                        "Product not found in cache for productId {} - Credit Note {} - Detail {}",
                        detailDto.getIdProduct(),
                        creditNote.getIdCreditNote(),
                        detailDto.getIdInvoiceDetail());
                    handleCreditNoteFailure(creditNote, "Product not found for detail");
                    return;
                  }
                } else {
                  log.warn(
                      "Credit note detail {} does not have idProduct - Credit Note {}",
                      detailDto.getIdInvoiceDetail(),
                      creditNote.getIdCreditNote());
                  handleCreditNoteFailure(creditNote, "Credit note detail does not have product");
                  return;
                }
              }
            }

            // Reutilizar AuthorizationSri existente si existe, para evitar crear registros duplicados
            AuthorizationSri existingAuthorizationSri = creditNote.getAuthorizationSri();
            AuthorizationSri authorizationSri =
                sendCreditNoteService.sendCreditNoteSri(
                    creditNoteDtoForSri, digitalSignatureDto, existingAuthorizationSri);
            creditNote.setAuthorizationSri(authorizationSri);
            // Actualizar número de nota de crédito desde el XML de autorización si está AUTORIZADO
            setCreditNoteNumberFromAuthorizationXml(creditNote, authorizationSri);

            // Validar si la autorización fue exitosa antes de resetear el contador
            boolean isAuthorized =
                Objects.nonNull(authorizationSri)
                    && StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
                    && "AUTORIZADO"
                        .equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri());
            boolean isReceived =
                Objects.nonNull(authorizationSri)
                    && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
                    && "RECIBIDA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri());

            if (isAuthorized && isReceived) {
              // Si fue exitoso, resetear el contador de reintentos y guardar
              repo.save(creditNote);
              // Notificar y enviar email si está autorizado
              notifyAuthorizedCreditNote(creditNoteDtoForSri, creditNote, authorizationSri);
              log.info("Successfully processed credit note {}", creditNote.getIdCreditNote());
            } else {
              // Si no fue exitoso, incrementar el contador de reintentos
              String failureReason =
                  buildFailureReason(authorizationSri, isAuthorized, isReceived);
              handleCreditNoteFailure(creditNote, failureReason);
            }
          } catch (feign.FeignException.Unauthorized e) {
            log.error(
                "Unauthorized access to digital signature service for credit note {} (retry {}/5) - Scheduled tasks need authentication configuration",
                creditNote.getIdCreditNote(),
                creditNote.getRetryCount());
            handleCreditNoteFailure(creditNote, "Unauthorized access to digital signature service");
          } catch (ResourceAccessException | feign.RetryableException e) {
            log.error(
                "Connection error to digital signature service for credit note {} (retry {}/5): {}",
                creditNote.getIdCreditNote(),
                creditNote.getRetryCount(),
                e.getMessage());
            handleCreditNoteFailure(creditNote, "Connection error: " + e.getMessage());
          } catch (CertificateException
              | SAXException
              | IOException
              | JAXBException
              | InterruptedException e) {
            log.error(
                "Process error for credit note {} (retry {}/5): {}",
                creditNote.getIdCreditNote(),
                creditNote.getRetryCount(),
                e.getMessage(),
                e);
            handleCreditNoteFailure(creditNote, "Process error: " + e.getMessage());
          } catch (Exception e) {
            log.error(
                "Unexpected error processing credit note {} (retry {}/5): {}",
                creditNote.getIdCreditNote(),
                creditNote.getRetryCount(),
                e.getMessage(),
                e);
            handleCreditNoteFailure(creditNote, "Unexpected error: " + e.getMessage());
          }
        });
  } finally {
    // Restaurar el contexto de seguridad anterior
    SecurityContextHolder.setContext(previousContext);
    log.debug("Restored previous security context after scheduled task execution");
  }
}

/**
 * Maneja el fallo de una nota de crédito incrementando el contador de reintentos y marcándola como
 * inactiva si se agotan los reintentos
 */
private void handleCreditNoteFailure(CreditNote creditNote, String reason) {
  creditNote.setRetryCount(creditNote.getRetryCount() + 1);

  if (creditNote.getRetryCount() >= 5) {
    log.error(
        "Credit note {} has exceeded maximum retries ({}) - Marking as inactive. Reason: {}",
        creditNote.getIdCreditNote(),
        creditNote.getRetryCount(),
        reason);
    deactivateCreditNoteAndDetails(creditNote);
  } else {
    log.warn(
        "Credit note {} retry {}/5. Reason: {}",
        creditNote.getIdCreditNote(),
        creditNote.getRetryCount(),
        reason);
    repo.save(creditNote);
  }
}

/**
 * Construye el mensaje de razón de fallo basado en el estado de la autorización SRI.
 *
 * @param authorizationSri La autorización SRI con los estados
 * @param isAuthorized Si el estado de autorización es "AUTORIZADO"
 * @param isReceived Si el estado de recepción es "RECIBIDA"
 * @return Mensaje descriptivo del fallo
 */
private String buildFailureReason(
    AuthorizationSri authorizationSri, boolean isAuthorized, boolean isReceived) {
  if (Objects.isNull(authorizationSri)) {
    return "AuthorizationSri is null";
  }

  String authStatus =
      StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
          ? authorizationSri.getAuthorizationStatusSri()
          : "null";
  String receptionStatus =
      StringUtils.hasText(authorizationSri.getReceptionStatusSri())
          ? authorizationSri.getReceptionStatusSri()
          : "null";

  if (!isReceived) {
    return String.format(
        "Reception status is not RECIBIDA. Status: %s. Error: %s",
        receptionStatus,
        StringUtils.hasText(authorizationSri.getReceptionError())
            ? authorizationSri.getReceptionError()
            : "No error message");
  }

  if (!isAuthorized) {
    return String.format(
        "Authorization status is not AUTORIZADO. Status: %s. Error: %s",
        authStatus,
        StringUtils.hasText(authorizationSri.getAuthorizationError())
            ? authorizationSri.getAuthorizationError()
            : "No error message");
  }

  return "Unknown failure reason";
}

/** Marca la nota de crédito y todos sus detalles como inactivos (status = false) */
private void deactivateCreditNoteAndDetails(CreditNote creditNote) {
  creditNote.setStatus(false);

  // Marcar todos los detalles como inactivos
  if (Objects.nonNull(creditNote.getCreditNoteDetails())) {
    creditNote.getCreditNoteDetails().forEach(detail -> detail.setStatus(false));
  }

  repo.save(creditNote);
  log.info(
      "Credit note {} and its {} details marked as inactive",
      creditNote.getIdCreditNote(),
      Objects.nonNull(creditNote.getCreditNoteDetails())
          ? creditNote.getCreditNoteDetails().size()
          : 0);
}
```

**IMPORTANTE**: También necesitarás agregar estos imports al inicio del archivo:

```java
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.response.ClientDtoResponse;
import com.wallparisoft.ebillbilling.service.AuthService;
import com.wallparisoft.ebillbilling.service.dto.TokenInformation;
import com.wallparisoft.ebillbilling.service.dto.TokenSession;
import feign.RetryableException;
import java.util.ArrayList;
import java.util.CollectionUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
```

Y agregar la dependencia de `AuthService` en el constructor:

```java
private final AuthService authService;
```

### 3. Modificar `TaskServiceImpl` para invocar el nuevo método

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/TaskServiceImpl.java`

```java
package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.service.ICreditNoteService;
import com.wallparisoft.ebillbilling.service.IInvoiceService;
import com.wallparisoft.ebillbilling.service.TaskService;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

  @Autowired private IInvoiceService invoiceService;
  @Autowired private ICreditNoteService creditNoteService;

  @Override
  public void executeTask() {
    log.info("Tarea programada ejecutada en: " + LocalDateTime.now());
    invoiceService.executeSendInvoiceToSri();
    creditNoteService.executeSendCreditNoteToSri();
  }
}
```

## Resumen de Cambios

1. **ICreditNoteRepo**: Agregar método `findCreditNotesExcludingSpecificSriStatuses` para buscar notas de crédito pendientes
2. **CreditNoteServiceImpl**: 
   - Implementar `executeSendCreditNoteToSri` completo con optimizaciones de cache
   - Agregar métodos auxiliares: `handleCreditNoteFailure`, `buildFailureReason`, `deactivateCreditNoteAndDetails`
   - Agregar dependencia `AuthService` en el constructor
3. **TaskServiceImpl**: Agregar invocación a `creditNoteService.executeSendCreditNoteToSri()` en `executeTask`

## Comportamiento Esperado

- La tarea programada procesará tanto facturas como notas de crédito pendientes
- Se aplicará el mismo sistema de reintentos (máximo 5 intentos)
- Se desactivarán automáticamente las notas de crédito que excedan los reintentos
- Se generarán PDFs y se enviarán emails cuando las notas de crédito sean autorizadas










