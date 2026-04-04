package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteListResponseDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteQueryDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.CreditNote;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.entity.InvoiceDetail;
import com.wallparisoft.ebillbilling.mapper.CreditNoteServiceMapper;
import com.wallparisoft.ebillbilling.repository.ICreditNoteDetailRepo;
import com.wallparisoft.ebillbilling.repository.ICreditNoteReasonRepo;
import com.wallparisoft.ebillbilling.repository.ICreditNoteRepo;
import com.wallparisoft.ebillbilling.repository.IInvoiceRepo;
import com.wallparisoft.ebillbilling.response.CreditNoteResponse;
import com.wallparisoft.ebillbilling.response.DigitalSignatureResponse;
import com.wallparisoft.ebillbilling.service.CreditNoteDeliveryService;
import com.wallparisoft.ebillbilling.service.CreditNoteEmailService;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.DigitalSignatureClient;
import com.wallparisoft.ebillbilling.service.ICreditNoteService;
import com.wallparisoft.ebillbilling.service.ISendCreditNoteService;
import com.wallparisoft.ebillbilling.service.ISubscriptionService;
import com.wallparisoft.ebillbilling.service.ProductClient;
import feign.RetryableException;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.xml.sax.SAXException;

@RequiredArgsConstructor
@Service
@Slf4j
public class CreditNoteServiceImpl implements ICreditNoteService {

  private final ICreditNoteRepo repo;
  private final ICreditNoteDetailRepo creditNoteDetailRepo;
  private final ICreditNoteReasonRepo creditNoteReasonRepo;
  private final IInvoiceRepo invoiceRepo;
  private final ISendCreditNoteService sendCreditNoteService;
  private final CreditNoteServiceMapper serviceMapper;
  private final DigitalSignatureClient digitalSignatureClient;
  private final CustomerClient customerClient;
  private final ProductClient productClient;
  private final CreditNoteDeliveryService creditNoteDeliveryService;
  private final CreditNoteEmailService creditNoteEmailService;
  private final IAuthService authService;
  private final ISubscriptionService subscriptionService;

  @Override
  public CreditNote register(CreditNote obj) {
    return repo.save(obj);
  }

  @Override
  public CreditNote modify(CreditNote obj) {
    return repo.save(obj);
  }

  @Override
  public List<CreditNote> list() {
    return repo.findAll();
  }

  @Override
  public List<com.wallparisoft.ebillbilling.dto.CreditNoteListResponseDto> listWithClientData() {
    List<CreditNote> allCreditNotes = repo.findAll();

    // Filtrar: solo BORRADORES (sin authorizationSri) y AUTORIZADAS
    // Las DEVUELTAS/FALLIDAS no se muestran en la lista
    List<CreditNote> creditNotes =
        allCreditNotes.stream()
            .filter(
                cn -> {
                  // BORRADOR: no tiene authorizationSri
                  if (cn.getAuthorizationSri() == null) {
                    return true;
                  }
                  // AUTORIZADA: tiene authorizationSri con estado AUTORIZADO
                  String authStatus = cn.getAuthorizationSri().getAuthorizationStatusSri();
                  return "AUTORIZADO".equalsIgnoreCase(authStatus);
                })
            .collect(Collectors.toList());

    log.info(
        "📋 [NC] Total en BD: {}, Filtradas (borradores + autorizadas): {}",
        allCreditNotes.size(),
        creditNotes.size());

    // Extraer IDs únicos de clientes
    Set<Long> uniqueClientIds =
        creditNotes.stream()
            .map(CreditNote::getIdClient)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // Pre-cargar datos de clientes en un mapa
    Map<Long, ClientDto> clientCache = new HashMap<>();
    for (Long idClient : uniqueClientIds) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(idClient);
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          clientCache.put(idClient, clientResponse.getClientDtos().get(0));
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener datos del cliente {}: {}", idClient, e.getMessage());
      }
    }

    // Pre-cargar facturas originales para enriquecer campos faltantes
    Set<Long> invoiceIds =
        creditNotes.stream()
            .map(CreditNote::getOriginalInvoiceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, Invoice> invoiceCache = new HashMap<>();
    for (Long invoiceId : invoiceIds) {
      invoiceRepo.findById(invoiceId).ifPresent(inv -> invoiceCache.put(invoiceId, inv));
    }

    // Mapear CreditNote a CreditNoteListResponseDto
    return creditNotes.stream()
        .map(
            cn -> {
              ClientDto clientDto = clientCache.get(cn.getIdClient());

              // Calcular estado de la NC
              String creditNoteStatus = calculateCreditNoteStatus(cn);

              // Enriquecer campos faltantes desde la factura original
              String originalInvoiceNumber = cn.getOriginalInvoiceNumber();
              java.math.BigDecimal totalAmount = cn.getTotalAmount();

              log.info(
                  "🔍 [NC] ID={}, originalInvoiceId={}, originalInvoiceNumber (actual)={}, totalAmount (actual)={}",
                  cn.getIdCreditNote(),
                  cn.getOriginalInvoiceId(),
                  originalInvoiceNumber,
                  totalAmount);

              // Si originalInvoiceNumber es null, obtenerlo de la factura
              if (originalInvoiceNumber == null && cn.getOriginalInvoiceId() != null) {
                Invoice originalInvoice = invoiceCache.get(cn.getOriginalInvoiceId());
                log.info(
                    "🔍 [NC] Buscando factura ID={} en cache, encontrada={}",
                    cn.getOriginalInvoiceId(),
                    originalInvoice != null);
                if (originalInvoice != null) {
                  originalInvoiceNumber = originalInvoice.getInvoiceNumber();
                  log.info("🔍 [NC] Enriquecido originalInvoiceNumber={}", originalInvoiceNumber);
                }
              }

              // Si totalAmount es null, calcular desde los detalles o valorModificacion
              if (totalAmount == null) {
                if (cn.getValorModificacion() != null) {
                  totalAmount = cn.getValorModificacion();
                } else if (cn.getCreditNoteDetails() != null
                    && !cn.getCreditNoteDetails().isEmpty()) {
                  // Sumar subtotales de los detalles + IVA
                  java.math.BigDecimal subtotal =
                      cn.getCreditNoteDetails().stream()
                          .map(
                              d ->
                                  d.getSubtotal() != null
                                      ? d.getSubtotal()
                                      : java.math.BigDecimal.ZERO)
                          .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                  java.math.BigDecimal iva =
                      cn.getTotalIva() != null ? cn.getTotalIva() : java.math.BigDecimal.ZERO;
                  totalAmount = subtotal.add(iva);
                }
              }

              return com.wallparisoft.ebillbilling.dto.CreditNoteListResponseDto.builder()
                  .idCreditNote(cn.getIdCreditNote())
                  .idCompany(cn.getIdCompany())
                  .idClient(cn.getIdClient())
                  .idIssuancePoint(cn.getIdIssuancePoint())
                  .sequential(cn.getSequential())
                  .retryCount(cn.getRetryCount())
                  .issueDate(cn.getIssueDate())
                  .creditNoteNumber(cn.getCreditNoteNumber())
                  .environment(cn.getEnvironment())
                  .additionalInformation(cn.getAdditionalInformation())
                  .totalAmount(totalAmount)
                  .subtotalZero(cn.getSubtotalZero())
                  .subtotalIva(cn.getSubtotalIva())
                  .subtotalIce(cn.getSubtotalIce())
                  .discount(cn.getDiscount())
                  .totalIva(cn.getTotalIva())
                  .totalIce(cn.getTotalIce())
                  .tip(cn.getTip())
                  .valorModificacion(cn.getValorModificacion())
                  .observation(cn.getObservation())
                  .emailSent(cn.getEmailSent())
                  .emailSentDate(cn.getEmailSentDate())
                  .originalInvoiceId(cn.getOriginalInvoiceId())
                  .originalAccessKey(cn.getOriginalAccessKey())
                  .originalInvoiceNumber(originalInvoiceNumber)
                  .originalIssueDate(cn.getOriginalIssueDate())
                  .documentTypeModified(cn.getDocumentTypeModified())
                  .reason(cn.getReason())
                  .creditNoteReason(cn.getCreditNoteReason())
                  .authorizationSri(cn.getAuthorizationSri())
                  .creditNoteDetails(cn.getCreditNoteDetails())
                  .creationDate(cn.getCreationDate())
                  .updateDate(cn.getUpdateDate())
                  .status(cn.getStatus())
                  // Datos del cliente (comprador)
                  .buyerName(clientDto != null ? clientDto.getName() : null)
                  .buyerIdentification(clientDto != null ? clientDto.getIdentification() : null)
                  // Estado calculado
                  .creditNoteStatus(creditNoteStatus)
                  .build();
            })
        .collect(Collectors.toList());
  }

  /**
   * Calcula el estado de una nota de crédito basado en su authorizationSri.
   *
   * @param cn CreditNote
   * @return BORRADOR, AUTORIZADO, EN_PROCESO, DEVUELTA
   */
  private String calculateCreditNoteStatus(CreditNote cn) {
    if (cn.getAuthorizationSri() == null) {
      return "BORRADOR";
    }
    String authStatus = cn.getAuthorizationSri().getAuthorizationStatusSri();
    if ("AUTORIZADO".equalsIgnoreCase(authStatus)) {
      return "AUTORIZADO";
    }
    String receptionStatus = cn.getAuthorizationSri().getReceptionStatusSri();
    if ("RECIBIDA".equalsIgnoreCase(receptionStatus)
        && !"AUTORIZADO".equalsIgnoreCase(authStatus)) {
      return "EN_PROCESO";
    }
    return "DEVUELTA";
  }

  @Override
  public CreditNote getById(Integer id) {
    return repo.findById(Long.valueOf(id)).orElse(null);
  }

  @Override
  public boolean delete(Integer id) {
    CreditNote creditNote = repo.findById(Long.valueOf(id)).orElse(null);
    if (creditNote != null) {
      // Solo permitir eliminar borradores (sin autorización)
      if (creditNote.getAuthorizationSri() == null) {
        repo.delete(creditNote);
        return true;
      } else {
        throw new IllegalStateException(
            "No se puede eliminar una nota de crédito autorizada. Solo se pueden eliminar borradores.");
      }
    }
    return false;
  }

  @Override
  public CreditNoteResponse saveCreditNote(CreditNoteDto creditNoteDto) {
    // ✅ VALIDACIÓN DE SUSCRIPCIÓN: Verificar si puede emitir nota de crédito
    try {
      subscriptionService.validateCanEmit(creditNoteDto.getIdCompany(), "CREDIT_NOTE");
      log.info("✅ Validación de suscripción exitosa para company {}", creditNoteDto.getIdCompany());
    } catch (IllegalStateException e) {
      log.error("❌ Validación de suscripción falló: {}", e.getMessage());
      return CreditNoteResponse.builder()
          .code(HttpStatus.FORBIDDEN.value())
          .status(HttpStatus.FORBIDDEN.name())
          .message(e.getMessage())
          .build();
    }

    // Validaciones básicas
    validateCreditNoteDto(creditNoteDto);

    CreditNoteResponse response =
        CreditNoteResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    // Extraer idIssuancePoint de cualquier ubicación del DTO
    Long idIssuancePoint = extractIssuancePointId(creditNoteDto);
    log.debug("idIssuancePoint extraído: {}", idIssuancePoint);

    // Validar y asignar número secuencial basado en punto de emisión
    if (Objects.nonNull(idIssuancePoint)) {

      // Obtener el siguiente secuencial para este punto de emisión
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;

      creditNoteDto.setIdIssuancePoint(idIssuancePoint);
      creditNoteDto.setSequential(nextSequential);

      // IMPORTANTE: Enriquecer DTO ANTES de mapear a entidad para que los detalles
      // tengan
      // productDto con mainCode, description, etc.
      enrichCreditNoteDto(creditNoteDto);

      // AHORA mapear a entidad (después del enriquecimiento)
      CreditNote creditNote = serviceMapper.toCreditNote(creditNoteDto);
      creditNote.setSequential(nextSequential);
      creditNote.setIdIssuancePoint(idIssuancePoint);
      creditNote.setRetryCount(0);
      creditNote.setIssueDate(LocalDateTime.now());

      // Setear el secuencial formateado después del enriquecimiento
      String formattedSequential = String.format("%09d", nextSequential);
      if (creditNoteDto.getEstablishment() != null
          && creditNoteDto.getEstablishment().getIssuancePoint() != null) {
        creditNoteDto
            .getEstablishment()
            .getIssuancePoint()
            .setSequentialInvoice(formattedSequential);

        // Generar creditNoteNumber en formato estab-ptoEmi-secuencial (ej:
        // 001-001-000000001)
        String estab =
            creditNoteDto.getEstablishment().getCode() != null
                ? creditNoteDto.getEstablishment().getCode()
                : "001";
        String ptoEmi =
            creditNoteDto.getEstablishment().getIssuancePoint().getCode() != null
                ? creditNoteDto.getEstablishment().getIssuancePoint().getCode()
                : "001";
        String creditNoteNumber = String.format("%s-%s-%s", estab, ptoEmi, formattedSequential);
        creditNote.setCreditNoteNumber(creditNoteNumber);
        creditNoteDto.setCreditNoteNumber(creditNoteNumber);
        log.info("📝 [NC] creditNoteNumber generado: {}", creditNoteNumber);
      }

      // Sincronizar campos enriquecidos del DTO a la entidad CreditNote
      if (creditNoteDto.getOriginalInvoiceNumber() != null) {
        creditNote.setOriginalInvoiceNumber(creditNoteDto.getOriginalInvoiceNumber());
      }
      if (creditNoteDto.getOriginalIssueDate() != null) {
        creditNote.setOriginalIssueDate(creditNoteDto.getOriginalIssueDate().atStartOfDay());
      }
      if (creditNoteDto.getOriginalAccessKey() != null) {
        creditNote.setOriginalAccessKey(creditNoteDto.getOriginalAccessKey());
      }

      // Calcular totalAmount desde el DTO enriquecido (usa productDto.getRate() para
      // IVA)
      BigDecimal calculatedTotal = calculateTotalAmountFromDto(creditNoteDto);
      if (creditNoteDto.getTotalAmount() != null
          && creditNoteDto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
        creditNote.setTotalAmount(creditNoteDto.getTotalAmount());
      } else if (calculatedTotal.compareTo(BigDecimal.ZERO) > 0) {
        creditNote.setTotalAmount(calculatedTotal);
        creditNoteDto.setTotalAmount(calculatedTotal);
        log.info("📝 [NC] totalAmount calculado desde DTO: {}", calculatedTotal);
      }

      try {
        // Obtener firma digital
        DigitalSignatureResponse digitalSignatureResponse =
            digitalSignatureClient.getDigitalSignatureByCompanyId(
                creditNoteDto.getCompanyDto().getIdCompany());
        if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
          log.error("There is no digital signature configuration");
          response = serviceMapper.toCreditNoteResponse(register(creditNote));
          response.setCode(HttpStatus.BAD_REQUEST.value());
          response.setStatus(HttpStatus.BAD_REQUEST.name());
          response.setMessage(
              "La firma digital caduca y/o no esta configurada comuníquese con el administrador, la nota de crédito se registro en borrador.");
          return response;
        }

        // Enviar al SRI
        log.info("🚀 Enviando Nota de Crédito al SRI...");
        AuthorizationSri authorizationSri =
            sendCreditNoteService.sendCreditNoteSri(
                creditNoteDto, digitalSignatureResponse.getDigitalSignatureDto(), null);

        // Log resultado del SRI
        log.info(
            "✅ Respuesta SRI recibida: receptionStatus={}, authorizationStatus={}, accessKey={}, authNumber={}",
            authorizationSri != null ? authorizationSri.getReceptionStatusSri() : "null",
            authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "null",
            authorizationSri != null ? authorizationSri.getAccessKey() : "null",
            authorizationSri != null ? authorizationSri.getAuthorizationNumber() : "null");

        // Validar si la respuesta del SRI fue exitosa antes de asignar authorizationSri
        // y
        // creditNoteNumber
        boolean isAuthorized =
            Objects.nonNull(authorizationSri)
                && StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
                && "AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri());
        boolean isReceived =
            Objects.nonNull(authorizationSri)
                && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
                && "RECIBIDA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri());

        // Solo asignar authorizationSri y creditNoteNumber si está AUTORIZADO
        // Si está DEVUELTA (excepto código 43 que se maneja como RECIBIDA), no se
        // guardan estos
        // campos
        if (isAuthorized && isReceived) {
          creditNote.setAuthorizationSri(authorizationSri);
          // Actualizar número de nota de crédito desde el XML de autorización si está
          // AUTORIZADO
          setCreditNoteNumberFromAuthorizationXml(creditNote, authorizationSri);
          CreditNote savedCreditNote = register(creditNote);
          response = serviceMapper.toCreditNoteResponse(savedCreditNote);
          response.setCode(HttpStatus.OK.value());
          response.setStatus(HttpStatus.OK.name());
          // Notificar y enviar email si está autorizado
          notifyAuthorizedCreditNote(creditNoteDto, savedCreditNote, authorizationSri);
        } else {
          // Si hay error DEVUELTA o no está autorizado, guardar como borrador sin
          // authorizationSri
          // ni creditNoteNumber
          // Limpiar creditNoteNumber si ya se había establecido
          creditNote.setCreditNoteNumber(null);
          creditNote.setAuthorizationSri(null);
          CreditNote savedCreditNote = register(creditNote);
          response = serviceMapper.toCreditNoteResponse(savedCreditNote);
          response.setCode(HttpStatus.BAD_REQUEST.value());
          response.setStatus(HttpStatus.BAD_REQUEST.name());
          String errorMessage =
              authorizationSri != null
                      && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
                      && "DEVUELTA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri())
                  ? "El SRI devolvió la nota de crédito con errores. La nota se registró como borrador para corrección."
                  : "Hubo problemas al procesar la nota de crédito con el SRI. La nota se registró como borrador.";
          response.setMessage(errorMessage);
          log.warn(
              "⚠️ [NC] Nota de crédito devuelta o no autorizada. Se guardó como borrador sin creditNoteNumber ni authorizationSri. ReceptionStatus: {}, AuthorizationStatus: {}",
              authorizationSri != null ? authorizationSri.getReceptionStatusSri() : "null",
              authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "null");
        }

      } catch (ResourceAccessException e) {
        response = serviceMapper.toCreditNoteResponse(register(creditNote));
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        response.setMessage(
            "Error al obtener la firma digital, comuníquese con el administrador, la nota de crédito se registro en borrador.");
      } catch (CertificateException
          | SAXException
          | IOException
          | JAXBException
          | InterruptedException e) {
        response = serviceMapper.toCreditNoteResponse(register(creditNote));
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        response.setMessage(
            "Hubo problemas para conectarse con el SRI, la nota de crédito se registro en borrador.");
      }
    } else {
      // Guardar como borrador si no hay punto de emisión
      // Enriquecer con lo que se pueda sin punto de emisión
      try {
        enrichCreditNoteDto(creditNoteDto);
      } catch (Exception e) {
        log.warn("Error enriqueciendo NC sin punto de emisión: {}", e.getMessage());
      }
      CreditNote creditNote = serviceMapper.toCreditNote(creditNoteDto);
      creditNote.setRetryCount(0);
      CreditNote savedCreditNote = register(creditNote);
      response = serviceMapper.toCreditNoteResponse(savedCreditNote);
      response.setCode(HttpStatus.OK.value());
      response.setStatus(HttpStatus.OK.name());
      response.setMessage("Nota de crédito guardada como borrador (sin punto de emisión)");
    }

    // Log de respuesta completa para debugging
    log.info(
        "📋 Response Nota de Crédito: code={}, status={}, message={}",
        response.getCode(),
        response.getStatus(),
        response.getMessage());
    log.info(
        "📋 NC Details: idCreditNote={}, creditNoteNumber={}, accessKey={}",
        response.getIdCreditNote(),
        response.getCreditNoteNumber(),
        response.getAccessKey());
    log.info(
        "📋 NC SRI: authorizationNumber={}, receptionStatus={}, authorizationStatus={}",
        response.getAuthorizationNumber(),
        response.getReceptionStatusSri(),
        response.getAuthorizationStatusSri());
    log.info("📋 NC Storage: storagePath={}", response.getStoragePath());

    return response;
  }

  @Override
  public CreditNoteResponse updateCreditNote(Long id, CreditNoteDto creditNoteDto) {
    CreditNote existingCreditNote = repo.findById(id).orElse(null);
    if (existingCreditNote == null) {
      throw new IllegalArgumentException("Nota de crédito no encontrada con ID: " + id);
    }

    // Solo permitir actualizar borradores
    if (existingCreditNote.getAuthorizationSri() != null) {
      throw new IllegalStateException(
          "No se puede actualizar una nota de crédito autorizada. Solo se pueden actualizar borradores.");
    }

    validateCreditNoteDto(creditNoteDto);

    // Actualizar campos
    CreditNote creditNote = serviceMapper.toCreditNote(creditNoteDto);
    creditNote.setIdCreditNote(id);
    creditNote.setCreationDate(existingCreditNote.getCreationDate());

    CreditNote updatedCreditNote = register(creditNote);
    CreditNoteResponse response = serviceMapper.toCreditNoteResponse(updatedCreditNote);
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());

    return response;
  }

  @Override
  public CreditNoteDto getCreditNoteById(Long id) {
    CreditNote creditNote = repo.findById(id).orElse(null);
    if (creditNote == null) {
      return null;
    }

    CreditNoteDto creditNoteDto = serviceMapper.toCreditNoteDto(creditNote);
    enrichCreditNoteDto(creditNoteDto);
    return creditNoteDto;
  }

  @Override
  public CreditNoteDto getDraftCreditNoteById(Long idCreditNote) {
    log.info("📝 [NC] Buscando borrador con ID: {}", idCreditNote);

    // 1. Buscar la nota de crédito por ID
    CreditNote creditNote =
        repo.findById(idCreditNote)
            .orElseThrow(
                () -> {
                  log.error("❌ [NC] Borrador con ID {} no encontrado", idCreditNote);
                  return new IllegalArgumentException(
                      "Borrador no encontrado con ID: " + idCreditNote);
                });

    // 2. Verificar que sea un borrador (authorizationSri == null)
    if (Objects.nonNull(creditNote.getAuthorizationSri())) {
      log.warn("⚠️ [NC] La nota de crédito {} ya fue procesada", idCreditNote);
      throw new IllegalArgumentException(
          "Esta nota de crédito ya fue procesada y autorizada por el SRI. No es un borrador editable.");
    }

    log.info("✅ [NC] Borrador encontrado. Total: ${}", creditNote.getTotalAmount());

    // 3. Convertir a DTO usando el mapper
    CreditNoteDto creditNoteDto = serviceMapper.toCreditNoteDto(creditNote);

    // 4. Establecer status como DRAFT
    creditNoteDto.setStatus("DRAFT");

    // 5. Establecer IDs en los DTOs antes de enriquecer (necesario para
    // enrichCreditNoteDto)
    if (creditNoteDto.getCompanyDto() != null && creditNote.getIdCompany() != null) {
      creditNoteDto.getCompanyDto().setIdCompany(creditNote.getIdCompany());
    }
    if (creditNoteDto.getClientDto() != null && creditNote.getIdClient() != null) {
      creditNoteDto.getClientDto().setIdClient(creditNote.getIdClient());
    }

    // 6. Enriquecer con datos completos (cliente, compañía, establecimiento,
    // productos)
    enrichCreditNoteDto(creditNoteDto);

    log.info("✅ [NC] Borrador completo retornado para edición (ID: {})", idCreditNote);
    return creditNoteDto;
  }

  @Override
  public Page<CreditNote> searchCreditNotes(
      String creditNoteNumber, Long idClient, Long originalInvoiceId, Pageable pageable) {
    // Use unsorted Pageable since the native query has its own ORDER BY clause
    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    // Note: originalInvoiceId is not used in current query - use other methods if
    // filtering by
    // originalInvoice is needed
    return repo.findCreditNotesByCriteria(
        creditNoteNumber, idClient, null, null, null, null, unsortedPageable);
  }

  @Override
  public List<CreditNote> getCreditNotesByOriginalInvoice(Long originalInvoiceId) {
    return repo.findByOriginalInvoiceId(originalInvoiceId);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
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

      // ===== OPTIMIZACIÓN: Pre-cargar datos únicos para evitar consultas duplicadas
      // =====
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
      Map<Long, com.wallparisoft.ebillbilling.dto.DigitalSignatureDto> digitalSignatureCache =
          new HashMap<>();
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

      // 4. Pre-cargar establecimientos con puntos de emisión (una consulta por
      // issuance point)
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
              com.wallparisoft.ebillbilling.dto.DigitalSignatureDto digitalSignatureDto =
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
                log.debug(
                    "Assigned company to credit note {} from cache", creditNote.getIdCreditNote());
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
                  // Actualizar el sequential en el issuancePoint si está disponible en la nota de
                  // crédito
                  if (Objects.nonNull(creditNote.getSequential())
                      && Objects.nonNull(establishmentDto.getIssuancePoint())) {
                    String formattedSequential = String.format("%09d", creditNote.getSequential());
                    establishmentDto.getIssuancePoint().setSequentialInvoice(formattedSequential);
                  }
                  log.debug(
                      "Assigned establishment to credit note {} from cache",
                      creditNote.getIdCreditNote());
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

              // Reutilizar AuthorizationSri existente si existe, para evitar crear registros
              // duplicados
              AuthorizationSri existingAuthorizationSri = creditNote.getAuthorizationSri();
              AuthorizationSri authorizationSri =
                  sendCreditNoteService.sendCreditNoteSri(
                      creditNoteDtoForSri, digitalSignatureDto, existingAuthorizationSri);
              creditNote.setAuthorizationSri(authorizationSri);
              // Actualizar número de nota de crédito desde el XML de autorización si está
              // AUTORIZADO
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
              handleCreditNoteFailure(
                  creditNote, "Unauthorized access to digital signature service");
            } catch (ResourceAccessException | RetryableException e) {
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

  // Métodos privados de ayuda
  private void validateCreditNoteDto(CreditNoteDto creditNoteDto) {
    if (creditNoteDto == null) {
      throw new IllegalArgumentException("CreditNoteDto no puede ser null");
    }

    // Validar que venga idReason o reason
    if (creditNoteDto.getIdReason() == null && !StringUtils.hasText(creditNoteDto.getReason())) {
      throw new IllegalArgumentException(
          "El motivo de la nota de crédito es obligatorio (idReason o reason)");
    }

    // Si viene idReason, obtener la descripción del catálogo
    if (creditNoteDto.getIdReason() != null && !StringUtils.hasText(creditNoteDto.getReason())) {
      com.wallparisoft.ebillbilling.entity.CreditNoteReason reasonEntity =
          creditNoteReasonRepo
              .findById(creditNoteDto.getIdReason())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Motivo de nota de crédito no encontrado con ID: "
                              + creditNoteDto.getIdReason()));
      creditNoteDto.setReason(reasonEntity.getDescription());
    }

    // Usar idInvoice como alias de originalInvoiceId si este último es null
    if (creditNoteDto.getOriginalInvoiceId() == null && creditNoteDto.getIdInvoice() != null) {
      creditNoteDto.setOriginalInvoiceId(creditNoteDto.getIdInvoice());
    }

    if (creditNoteDto.getOriginalInvoiceId() == null
        && !StringUtils.hasText(creditNoteDto.getOriginalAccessKey())) {
      throw new IllegalArgumentException(
          "Se debe especificar el ID de la factura original (idInvoice/originalInvoiceId) o su clave de acceso");
    }

    if (creditNoteDto.getCreditNoteDetails() == null
        || creditNoteDto.getCreditNoteDetails().isEmpty()) {
      // Verificar también el campo 'details' como alias
      if (creditNoteDto.getDetails() != null && !creditNoteDto.getDetails().isEmpty()) {
        creditNoteDto.setCreditNoteDetails(creditNoteDto.getDetails());
      } else {
        throw new IllegalArgumentException("La nota de crédito debe tener al menos un detalle");
      }
    }

    // Validaciones adicionales con la factura original
    if (creditNoteDto.getOriginalInvoiceId() != null) {
      validateAgainstOriginalInvoice(creditNoteDto);
    }
  }

  /**
   * Valida la nota de crédito contra la factura original: - Factura debe estar AUTORIZADA - Total
   * NC ≤ saldo disponible - Cantidades por item ≤ cantidad disponible - Fecha emisión NC ≥ fecha
   * factura
   */
  private void validateAgainstOriginalInvoice(CreditNoteDto creditNoteDto) {
    Long invoiceId = creditNoteDto.getOriginalInvoiceId();

    // 1. Buscar la factura
    Invoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
    if (invoice == null) {
      throw new IllegalArgumentException("Factura original no encontrada con ID: " + invoiceId);
    }

    // 2. Verificar que esté autorizada
    if (invoice.getAuthorizationSri() == null
        || !"AUTORIZADO"
            .equalsIgnoreCase(invoice.getAuthorizationSri().getAuthorizationStatusSri())) {
      throw new IllegalArgumentException(
          "Solo se pueden crear notas de crédito para facturas autorizadas por el SRI");
    }

    // 3. Validar fecha: NC debe ser igual o posterior a la factura
    if (creditNoteDto.getIssueDate() != null && invoice.getIssueDate() != null) {
      // Convertir LocalDateTime a LocalDate para comparación
      java.time.LocalDate invoiceDate = invoice.getIssueDate().toLocalDate();
      if (creditNoteDto.getIssueDate().isBefore(invoiceDate)) {
        throw new IllegalArgumentException(
            "La fecha de la nota de crédito no puede ser anterior a la fecha de la factura");
      }
    }

    // 4. Calcular saldo disponible de la factura
    java.math.BigDecimal totalCreditNotesApplied =
        creditNoteDetailRepo.findTotalCreditNotesApplied(invoiceId);
    java.math.BigDecimal totalInvoice =
        invoice.getTotalAmount() != null ? invoice.getTotalAmount() : java.math.BigDecimal.ZERO;
    java.math.BigDecimal availableAmount = totalInvoice.subtract(totalCreditNotesApplied);

    // 5. Calcular total de la nueva NC
    java.math.BigDecimal newCreditNoteTotal = creditNoteDto.getTotalAmount();
    if (newCreditNoteTotal == null) {
      // Calcular desde los detalles si no está especificado
      newCreditNoteTotal =
          creditNoteDto.getCreditNoteDetails().stream()
              .map(
                  detail -> {
                    java.math.BigDecimal subtotal =
                        detail.getSubtotal() != null
                            ? detail.getSubtotal()
                            : java.math.BigDecimal.ZERO;
                    return subtotal;
                  })
              .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    // 6. Validar que total NC ≤ saldo disponible
    if (newCreditNoteTotal.compareTo(availableAmount) > 0) {
      throw new IllegalArgumentException(
          String.format(
              "El total de la nota de crédito (%.2f) excede el saldo disponible de la factura (%.2f). "
                  + "Total factura: %.2f, NC aplicadas: %.2f",
              newCreditNoteTotal, availableAmount, totalInvoice, totalCreditNotesApplied));
    }

    // 7. Validar cantidades por item si hay referencia a detalles de factura
    Map<Long, java.math.BigDecimal> quantitiesReturned = new HashMap<>();
    List<Object[]> returnedData = creditNoteDetailRepo.findQuantitiesReturnedByInvoice(invoiceId);
    for (Object[] row : returnedData) {
      Long idInvoiceDetail = ((Number) row[0]).longValue();
      // El SUM() de JPA puede devolver Long, Double o BigDecimal dependiendo del
      // driver
      java.math.BigDecimal totalReturned =
          row[1] != null
              ? new java.math.BigDecimal(((Number) row[1]).toString())
              : java.math.BigDecimal.ZERO;
      quantitiesReturned.put(idInvoiceDetail, totalReturned);
    }

    // Crear mapa de cantidades originales por idInvoiceDetail
    Map<Long, Integer> originalQuantities = new HashMap<>();
    if (invoice.getInvoiceDetails() != null) {
      for (InvoiceDetail detail : invoice.getInvoiceDetails()) {
        originalQuantities.put(detail.getIdInvoiceDetail(), detail.getQuantity());
      }
    }

    // Validar cada detalle de la NC
    for (InvoiceDetailDto detailDto : creditNoteDto.getCreditNoteDetails()) {
      // Si tiene referencia a un detalle de factura, validar cantidad
      if (detailDto.getIdInvoiceDetail() != null) {
        Integer originalQty = originalQuantities.get(detailDto.getIdInvoiceDetail());
        if (originalQty == null) {
          throw new IllegalArgumentException(
              "Detalle de factura no encontrado: " + detailDto.getIdInvoiceDetail());
        }

        java.math.BigDecimal alreadyReturned =
            quantitiesReturned.getOrDefault(
                detailDto.getIdInvoiceDetail(), java.math.BigDecimal.ZERO);
        java.math.BigDecimal availableQty =
            java.math.BigDecimal.valueOf(originalQty).subtract(alreadyReturned);
        java.math.BigDecimal requestedQty =
            java.math.BigDecimal.valueOf(
                detailDto.getQuantity() != null ? detailDto.getQuantity() : 0);

        if (requestedQty.compareTo(availableQty) > 0) {
          throw new IllegalArgumentException(
              String.format(
                  "Cantidad solicitada (%d) para el producto excede la cantidad disponible (%.0f). "
                      + "Cantidad original: %d, ya devuelta: %.0f",
                  detailDto.getQuantity(), availableQty, originalQty, alreadyReturned));
        }
      }
    }

    log.debug("Validaciones de NC contra factura {} completadas exitosamente", invoiceId);
  }

  /**
   * Extrae el idIssuancePoint del DTO de nota de crédito. Puede venir de diferentes lugares según
   * cómo se envíe el request.
   */
  private Long extractIssuancePointId(CreditNoteDto creditNoteDto) {
    // Primero intentar desde idIssuancePoint directo
    if (creditNoteDto.getIdIssuancePoint() != null) {
      return creditNoteDto.getIdIssuancePoint();
    }

    // Luego desde establishment.issuancePoint.idIssuancePoint
    if (creditNoteDto.getEstablishment() != null
        && creditNoteDto.getEstablishment().getIssuancePoint() != null
        && creditNoteDto.getEstablishment().getIssuancePoint().getIdIssuancePoint() != null) {
      return creditNoteDto.getEstablishment().getIssuancePoint().getIdIssuancePoint();
    }

    return null;
  }

  /**
   * Calcula el totalAmount de una NC usando el DTO enriquecido. Usa la misma lógica que
   * GenerateCreditNoteSri: obtiene la tarifa de IVA del producto (productDto.getRate()).
   *
   * @param creditNoteDto DTO con detalles enriquecidos (productDto cargado)
   * @return BigDecimal con el total calculado (subtotal + IVA)
   */
  private BigDecimal calculateTotalAmountFromDto(CreditNoteDto creditNoteDto) {
    List<InvoiceDetailDto> details = creditNoteDto.getCreditNoteDetails();
    if (details == null || details.isEmpty()) {
      details = creditNoteDto.getDetails();
    }
    if (details == null || details.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal totalSinImpuestos = BigDecimal.ZERO;
    BigDecimal totalIva = BigDecimal.ZERO;

    for (InvoiceDetailDto detail : details) {
      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
      totalSinImpuestos = totalSinImpuestos.add(subtotal);

      // Obtener tarifa de IVA del producto (igual que GenerateCreditNoteSri)
      BigDecimal rate = BigDecimal.ZERO;
      if (detail.getProductDto() != null && detail.getProductDto().getRate() != null) {
        rate = detail.getProductDto().getRate();
      } else if (detail.getIvaPercentage() != null) {
        rate = detail.getIvaPercentage();
      }

      // Calcular IVA: subtotal * rate / 100
      if (rate.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal ivaDeEsteProducto =
            subtotal
                .multiply(rate)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        totalIva = totalIva.add(ivaDeEsteProducto);
      }
    }

    BigDecimal total = totalSinImpuestos.add(totalIva);
    log.info(
        "📝 [NC] totalAmount calculado: subtotal={}, iva={}, total={}",
        totalSinImpuestos,
        totalIva,
        total);
    return total;
  }

  private void enrichCreditNoteDto(CreditNoteDto creditNoteDto) {
    // Enriquecer con datos de compañía
    if (creditNoteDto.getCompanyDto() == null
        || creditNoteDto.getCompanyDto().getIdCompany() == null) {
      throw new IllegalArgumentException("CompanyDto es requerido");
    }

    // Cargar datos completos de la compañía (RUC, razón social, etc.)
    try {
      Long idCompany = creditNoteDto.getCompanyDto().getIdCompany();
      CompanyDto companyDto = customerClient.getCompanyById(idCompany);
      if (companyDto != null) {
        creditNoteDto.setCompanyDto(companyDto);
        log.debug(
            "Loaded company data for companyId {}: RUC={}",
            idCompany,
            companyDto.getIdentification());
      } else {
        throw new IllegalArgumentException("No se encontró la compañía con ID: " + idCompany);
      }
    } catch (Exception e) {
      log.error("Error loading company: {}", e.getMessage());
      throw new IllegalArgumentException("Error al cargar datos de la compañía: " + e.getMessage());
    }

    // Enriquecer con datos de cliente
    if (creditNoteDto.getClientDto() == null
        || creditNoteDto.getClientDto().getIdClient() == null) {
      throw new IllegalArgumentException("ClientDto es requerido");
    }

    try {
      ClientDto clientDto =
          customerClient
              .getClientById(creditNoteDto.getClientDto().getIdClient())
              .getClientDtos()
              .get(0);
      creditNoteDto.setClientDto(clientDto);
    } catch (Exception e) {
      log.warn("Error loading client: {}", e.getMessage());
    }

    // Enriquecer con datos de establecimiento
    Long idIssuancePoint = extractIssuancePointId(creditNoteDto);

    if (idIssuancePoint != null) {
      try {
        log.debug("Cargando establecimiento para idIssuancePoint: {}", idIssuancePoint);
        EstablishmentDto establishmentDto =
            customerClient.getEstablishmentWithIssuancePoint(idIssuancePoint);
        if (establishmentDto != null) {
          creditNoteDto.setEstablishment(establishmentDto);
          log.debug(
              "Establecimiento cargado: code={}, issuancePoint.code={}",
              establishmentDto.getCode(),
              establishmentDto.getIssuancePoint() != null
                  ? establishmentDto.getIssuancePoint().getCode()
                  : "null");
        } else {
          throw new IllegalArgumentException(
              "No se encontró el establecimiento para el punto de emisión: " + idIssuancePoint);
        }
      } catch (Exception e) {
        log.error(
            "Error loading establishment for idIssuancePoint {}: {}",
            idIssuancePoint,
            e.getMessage());
        throw new IllegalArgumentException(
            "Error al cargar datos del establecimiento: " + e.getMessage());
      }
    } else {
      throw new IllegalArgumentException(
          "Se requiere el punto de emisión (idIssuancePoint) para crear la nota de crédito");
    }

    // *** IMPORTANTE: Enriquecer con datos de la factura original ***
    // Esto es OBLIGATORIO para el SRI: numDocModificado, fechaEmisionDocSustento
    Long originalInvoiceId =
        creditNoteDto.getOriginalInvoiceId() != null
            ? creditNoteDto.getOriginalInvoiceId()
            : creditNoteDto.getIdInvoice();

    log.info(
        "🔍 [NC] Buscando factura original con ID: {} (originalInvoiceId={}, idInvoice={})",
        originalInvoiceId,
        creditNoteDto.getOriginalInvoiceId(),
        creditNoteDto.getIdInvoice());

    if (originalInvoiceId != null) {
      try {
        Invoice originalInvoice = invoiceRepo.findById(originalInvoiceId).orElse(null);
        if (originalInvoice != null) {
          log.info(
              "🔍 [NC] Factura encontrada: invoiceNumber={}, issueDate={}",
              originalInvoice.getInvoiceNumber(),
              originalInvoice.getIssueDate());

          // Setear número de factura original (REQUERIDO por SRI)
          if (!StringUtils.hasText(creditNoteDto.getOriginalInvoiceNumber())) {
            creditNoteDto.setOriginalInvoiceNumber(originalInvoice.getInvoiceNumber());
            log.info(
                "📝 [NC] Seteando originalInvoiceNumber desde factura: {}",
                originalInvoice.getInvoiceNumber());
          }

          // Setear fecha de emisión de factura original (REQUERIDO por SRI)
          if (creditNoteDto.getOriginalIssueDate() == null) {
            if (originalInvoice.getIssueDate() != null) {
              creditNoteDto.setOriginalIssueDate(originalInvoice.getIssueDate().toLocalDate());
              log.info(
                  "📝 [NC] Seteando originalIssueDate desde factura: {}",
                  originalInvoice.getIssueDate().toLocalDate());
            } else if (originalInvoice.getAuthorizationSri() != null
                && StringUtils.hasText(originalInvoice.getAuthorizationSri().getAccessKey())) {
              // Fallback: extraer fecha del accessKey (posiciones 0-7: ddMMyyyy)
              String accessKey = originalInvoice.getAuthorizationSri().getAccessKey();
              try {
                String dateStr = accessKey.substring(0, 8); // ddMMyyyy
                java.time.LocalDate extractedDate =
                    java.time.LocalDate.parse(
                        dateStr, java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
                creditNoteDto.setOriginalIssueDate(extractedDate);
                log.info(
                    "📝 [NC] Seteando originalIssueDate desde accessKey: {} -> {}",
                    dateStr,
                    extractedDate);
              } catch (Exception e) {
                log.warn("⚠️ [NC] No se pudo extraer fecha del accessKey: {}", e.getMessage());
              }
            }
          }

          // Setear clave de acceso de factura original
          if (!StringUtils.hasText(creditNoteDto.getOriginalAccessKey())
              && originalInvoice.getAuthorizationSri() != null) {
            creditNoteDto.setOriginalAccessKey(
                originalInvoice.getAuthorizationSri().getAccessKey());
            log.info(
                "📝 [NC] Seteando originalAccessKey desde factura: {}",
                originalInvoice.getAuthorizationSri().getAccessKey());
          }

          // Setear ID de cliente si no viene
          if (creditNoteDto.getClientDto() == null
              || creditNoteDto.getClientDto().getIdClient() == null) {
            if (originalInvoice.getIdClient() != null) {
              ClientDto clientDto = new ClientDto();
              clientDto.setIdClient(originalInvoice.getIdClient());
              creditNoteDto.setClientDto(clientDto);
            }
          }

          log.info(
              "✅ [NC] Datos de factura original cargados: number={}, date={}",
              creditNoteDto.getOriginalInvoiceNumber(),
              creditNoteDto.getOriginalIssueDate());
        } else {
          log.error("❌ [NC] No se encontró la factura original con ID: {}", originalInvoiceId);
          throw new IllegalArgumentException(
              "No se encontró la factura original con ID: " + originalInvoiceId);
        }
      } catch (IllegalArgumentException e) {
        throw e; // Re-lanzar para que sea manejada arriba
      } catch (Exception e) {
        log.error(
            "❌ [NC] Error cargando factura original {}: {}", originalInvoiceId, e.getMessage());
        throw new IllegalArgumentException("Error cargando factura original: " + e.getMessage());
      }
    } else {
      log.error("❌ [NC] No se proporcionó originalInvoiceId ni idInvoice");
      throw new IllegalArgumentException(
          "El ID de la factura original (originalInvoiceId o idInvoice) es obligatorio");
    }

    // Validación final: asegurar que los campos requeridos por SRI estén seteados
    if (!StringUtils.hasText(creditNoteDto.getOriginalInvoiceNumber())) {
      throw new IllegalArgumentException(
          "El número de factura original (originalInvoiceNumber) no pudo ser determinado");
    }
    if (creditNoteDto.getOriginalIssueDate() == null) {
      throw new IllegalArgumentException(
          "La fecha de emisión de la factura original (originalIssueDate) no pudo ser determinada");
    }

    // Enriquecer detalles con productos
    if (creditNoteDto.getCreditNoteDetails() != null) {
      for (InvoiceDetailDto detail : creditNoteDto.getCreditNoteDetails()) {
        if (detail.getProductDto() == null && detail.getIdProduct() != null) {
          try {
            com.wallparisoft.ebillbilling.response.ProductResponse productResponse =
                productClient.getProductById(detail.getIdProduct());
            if (productResponse != null && productResponse.getProductDto() != null) {
              ProductDto productDto = mapProductDtoFromService(productResponse.getProductDto());
              if (productDto != null) {
                detail.setProductDto(productDto);
              }
            }
          } catch (Exception e) {
            log.warn("Error loading product {}: {}", detail.getIdProduct(), e.getMessage());
          }
        }
      }
    }
  }

  private void setCreditNoteNumberFromAuthorizationXml(
      CreditNote creditNote, AuthorizationSri authorizationSri) {
    if (authorizationSri == null
        || !"AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())
        || !StringUtils.hasText(authorizationSri.getAuthorizationXml())) {
      return;
    }

    try {
      // Extraer número de nota de crédito del XML de autorización
      // Similar a setInvoiceNumberFromAuthorizationXml en InvoiceServiceImpl
      String xml = authorizationSri.getAuthorizationXml();
      // Implementar parsing del XML para extraer el número
      // Por ahora, usar el formato: estab-ptoEmi-secuencial basado en los datos de la
      // nota
      if (creditNote.getSequential() != null && creditNote.getIdIssuancePoint() != null) {
        // Obtener establecimiento para construir el número
        try {
          EstablishmentDto establishmentDto =
              customerClient.getEstablishmentWithIssuancePoint(creditNote.getIdIssuancePoint());
          if (establishmentDto != null
              && establishmentDto.getIssuancePoint() != null
              && StringUtils.hasText(establishmentDto.getCode())
              && StringUtils.hasText(establishmentDto.getIssuancePoint().getCode())) {
            String creditNoteNumber =
                String.format(
                    "%s-%s-%09d",
                    establishmentDto.getCode(),
                    establishmentDto.getIssuancePoint().getCode(),
                    creditNote.getSequential());
            creditNote.setCreditNoteNumber(creditNoteNumber);
          }
        } catch (Exception e) {
          log.warn("Error loading establishment for credit note number: {}", e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Error extracting credit note number from authorization XML: {}", e.getMessage());
    }
  }

  /**
   * Notifica y envía el email con PDF cuando una nota de crédito es autorizada por el SRI
   *
   * @param creditNoteDto DTO de la nota de crédito
   * @param savedCreditNote Nota de crédito guardada
   * @param authorizationSri Autorización del SRI
   */
  private void notifyAuthorizedCreditNote(
      CreditNoteDto creditNoteDto, CreditNote savedCreditNote, AuthorizationSri authorizationSri) {

    // Verificar si el correo ya fue enviado
    if (Boolean.TRUE.equals(savedCreditNote.getEmailSent())) {
      log.info(
          "Email already sent for credit note {} on {}. Skipping notification.",
          savedCreditNote.getIdCreditNote(),
          savedCreditNote.getEmailSentDate());
      return;
    }

    if (Objects.isNull(authorizationSri)
        || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
        || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
      return;
    }

    if (!StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      log.warn(
          "Authorization XML path is missing for credit note {}. Skipping email notification.",
          savedCreditNote.getIdCreditNote());
      return;
    }

    // Obtener ClientDto completo con contactos
    ClientDto clientDtoWithContacts = null;
    try {
      if (creditNoteDto.getClientDto() != null
          && creditNoteDto.getClientDto().getIdClient() != null) {
        clientDtoWithContacts =
            customerClient
                .getClientById(creditNoteDto.getClientDto().getIdClient())
                .getClientDtos()
                .get(0);
        creditNoteDto.setClientDto(clientDtoWithContacts);
      }
    } catch (Exception e) {
      log.warn("Error loading client for credit note notification: {}", e.getMessage());
    }

    // CAMBIO: Siempre generar PDFs, independientemente del email
    // El servicio de delivery ahora maneja la generación de PDFs y el envío de
    // email por separado
    creditNoteDeliveryService.deliverAuthorizedCreditNote(
        creditNoteDto, savedCreditNote, authorizationSri);

    // CAMBIO: Si no hay email, marcar como no enviado explícitamente
    String recipientEmail = getRecipientEmail(creditNoteDto, clientDtoWithContacts);
    if (!StringUtils.hasText(recipientEmail)) {
      log.info(
          "No recipient email found for credit note {}. PDF and XML generated, but email not sent. Marking emailSent as false.",
          savedCreditNote.getIdCreditNote());
      savedCreditNote.setEmailSent(false);
      repo.save(savedCreditNote);
    }
  }

  /**
   * Obtiene el email del cliente desde el DTO o desde los contactos
   *
   * @param creditNoteDto DTO de la nota de crédito
   * @param clientDto Cliente con contactos (opcional)
   * @return Email del cliente o null si no se encuentra
   */
  private String getRecipientEmail(CreditNoteDto creditNoteDto, ClientDto clientDto) {
    // Intentar obtener desde clientDto pasado como parámetro
    if (clientDto != null && clientDto.getContacts() != null) {
      String emailFromClientDto =
          clientDto.getContacts().stream()
              .filter(contact -> "MAI".equalsIgnoreCase(contact.getContactType()))
              .filter(
                  contact ->
                      contact.getStatus() == null || Boolean.TRUE.equals(contact.getStatus()))
              .findFirst()
              .map(contact -> contact.getValue())
              .orElse(null);
      if (StringUtils.hasText(emailFromClientDto)) {
        return emailFromClientDto;
      }
    }

    // Fallback: intentar desde creditNoteDto
    if (creditNoteDto != null
        && creditNoteDto.getClientDto() != null
        && creditNoteDto.getClientDto().getContacts() != null) {
      String emailFromCreditNoteDto =
          creditNoteDto.getClientDto().getContacts().stream()
              .filter(contact -> "MAI".equalsIgnoreCase(contact.getContactType()))
              .filter(
                  contact ->
                      contact.getStatus() == null || Boolean.TRUE.equals(contact.getStatus()))
              .findFirst()
              .map(contact -> contact.getValue())
              .orElse(null);
      if (StringUtils.hasText(emailFromCreditNoteDto)) {
        return emailFromCreditNoteDto;
      }
    }

    // Intentar obtener email usando reflexión por si el JSON tiene buyerMail aunque
    // no esté en el
    // DTO
    if (creditNoteDto != null) {
      try {
        java.lang.reflect.Method getBuyerMail = creditNoteDto.getClass().getMethod("getBuyerMail");
        Object buyerMail = getBuyerMail.invoke(creditNoteDto);
        if (buyerMail instanceof String && StringUtils.hasText((String) buyerMail)) {
          return (String) buyerMail;
        }
      } catch (Exception e) {
        // buyerMail no existe en el DTO, continuar
      }
    }

    return null;
  }

  /**
   * Maneja el fallo de una nota de crédito incrementando el contador de reintentos y marcándola
   * como inactiva si se agotan los reintentos
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

  /**
   * Mapea un Map<String, Object> (ProductDto del servicio de productos) a ProductDto de
   * ebill-billing
   *
   * @param productDtoFromService Map con los datos del producto (viene de ProductResponse)
   * @return ProductDto de ebill-billing con los campos mapeados
   */
  private ProductDto mapProductDtoFromService(java.util.Map<String, Object> productDtoFromService) {
    if (Objects.isNull(productDtoFromService) || productDtoFromService.isEmpty()) {
      return null;
    }

    try {
      java.util.Map<String, Object> productMap = productDtoFromService;

      ProductDto productDto = new ProductDto();

      // Mapear campos básicos
      if (productMap.containsKey("idProduct")) {
        Object idProduct = productMap.get("idProduct");
        if (idProduct instanceof Integer) {
          productDto.setIdProduct(((Integer) idProduct).longValue());
        } else if (idProduct instanceof Long) {
          productDto.setIdProduct((Long) idProduct);
        }
      }

      if (productMap.containsKey("idCompany")) {
        Object idCompany = productMap.get("idCompany");
        if (idCompany instanceof Long) {
          productDto.setIdCompany((Long) idCompany);
        } else if (idCompany instanceof Integer) {
          productDto.setIdCompany(((Integer) idCompany).longValue());
        }
      }

      if (productMap.containsKey("mainCode")) {
        productDto.setMainCode((String) productMap.get("mainCode"));
      }

      if (productMap.containsKey("name")) {
        productDto.setName((String) productMap.get("name"));
      }

      if (productMap.containsKey("productType")) {
        Object productType = productMap.get("productType");
        if (productType instanceof Integer) {
          productDto.setProductType((Integer) productType);
        }
      }

      if (productMap.containsKey("unitPrice")) {
        Object unitPrice = productMap.get("unitPrice");
        if (unitPrice instanceof Double) {
          productDto.setUnitPrice(((Double) unitPrice).doubleValue());
        } else if (unitPrice instanceof Number) {
          productDto.setUnitPrice(((Number) unitPrice).doubleValue());
        }
      }

      if (productMap.containsKey("description")) {
        productDto.setDescription((String) productMap.get("description"));
      }

      if (productMap.containsKey("status")) {
        Object status = productMap.get("status");
        if (status instanceof Boolean) {
          productDto.setStatus((Boolean) status);
        }
      }

      // Mapear TaxTypeDto para extraer rate, taxCode
      if (productMap.containsKey("ivaTaxType") && productMap.get("ivaTaxType") != null) {
        Object taxTypeObj = productMap.get("ivaTaxType");
        if (taxTypeObj instanceof java.util.Map) {
          java.util.Map<String, Object> taxTypeMap = (java.util.Map<String, Object>) taxTypeObj;

          // Extraer rate (value en TaxTypeDto)
          if (taxTypeMap.containsKey("value")) {
            Object value = taxTypeMap.get("value");
            if (value instanceof Number) {
              java.math.BigDecimal rate =
                  new java.math.BigDecimal(value.toString())
                      .setScale(4, java.math.RoundingMode.HALF_UP);
              productDto.setRate(rate);
            }
          }

          // Extraer taxCode
          if (taxTypeMap.containsKey("type")) {
            Object typeObj = taxTypeMap.get("type");
            String type = String.valueOf(typeObj);
            if (type != null && (type.equalsIgnoreCase("IVA") || type.equalsIgnoreCase("2"))) {
              productDto.setTaxCode("2");
            } else {
              productDto.setTaxCode(type);
            }
          }
        }
      }

      // Campos que no existen en el servicio de productos
      productDto.setAuxiliarCode(null);
      productDto.setDiscount(0.0);

      return productDto;
    } catch (Exception e) {
      log.error("Error mapping ProductDto from service: {}", e.getMessage(), e);
      return null;
    }
  }

  @Override
  public CreditNoteResponse saveCreditNoteDraft(CreditNoteDto creditNoteDto) {
    log.info("Guardando nota de crédito como borrador");

    // Validaciones básicas
    validateCreditNoteDto(creditNoteDto);

    // Enriquecer DTO con datos de factura original antes de mapear
    enrichCreditNoteDto(creditNoteDto);

    CreditNote creditNote = serviceMapper.toCreditNote(creditNoteDto);
    CreditNoteResponse response =
        CreditNoteResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    try {
      // NO asignar secuencial para borradores - se asigna al enviar al SRI
      creditNote.setSequential(null);
      creditNote.setIdIssuancePoint(creditNoteDto.getIdIssuancePoint());
      creditNote.setRetryCount(0);
      // Convertir LocalDate a LocalDateTime (inicio del día)
      creditNote.setIssueDate(
          creditNoteDto.getIssueDate() != null
              ? creditNoteDto.getIssueDate().atStartOfDay()
              : LocalDateTime.now());
      creditNote.setAuthorizationSri(null); // Sin autorización = borrador

      // Sincronizar campos enriquecidos del DTO a la entidad
      if (creditNoteDto.getOriginalInvoiceNumber() != null) {
        creditNote.setOriginalInvoiceNumber(creditNoteDto.getOriginalInvoiceNumber());
      }
      if (creditNoteDto.getTotalAmount() != null) {
        creditNote.setTotalAmount(creditNoteDto.getTotalAmount());
      }

      CreditNote savedCreditNote = repo.save(creditNote);

      response = serviceMapper.toCreditNoteResponse(savedCreditNote);
      response.setCode(HttpStatus.OK.value());
      response.setStatus("BORRADOR");
      response.setMessage("Nota de crédito guardada como borrador");

      log.info("Borrador de NC guardado con ID: {}", savedCreditNote.getIdCreditNote());

    } catch (Exception e) {
      log.error("Error guardando borrador de NC: {}", e.getMessage(), e);
      response.setMessage("Error guardando borrador: " + e.getMessage());
    }

    return response;
  }

  @Override
  public CreditNoteResponse sendCreditNoteToSri(Long idCreditNote, String password) {
    log.info("Enviando NC borrador {} al SRI", idCreditNote);

    CreditNoteResponse response =
        CreditNoteResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    // 1. Buscar la NC
    CreditNote creditNote = repo.findById(idCreditNote).orElse(null);
    if (creditNote == null) {
      response.setMessage("Nota de crédito no encontrada con ID: " + idCreditNote);
      return response;
    }

    // 2. Verificar que sea borrador
    if (creditNote.getAuthorizationSri() != null) {
      response.setMessage("Esta nota de crédito ya fue enviada al SRI");
      return response;
    }

    // 3. Convertir a DTO y enriquecer
    CreditNoteDto creditNoteDto = serviceMapper.toCreditNoteDto(creditNote);

    // Asignar secuencial si no tiene
    if (creditNote.getSequential() == null && creditNote.getIdIssuancePoint() != null) {
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(creditNote.getIdIssuancePoint());
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;
      creditNote.setSequential(nextSequential);
      creditNoteDto.setSequential(nextSequential);
    }

    enrichCreditNoteDto(creditNoteDto);

    try {
      // 4. Obtener firma digital
      DigitalSignatureResponse digitalSignatureResponse =
          digitalSignatureClient.getDigitalSignatureByCompanyId(
              creditNoteDto.getCompanyDto().getIdCompany());

      if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
        response.setMessage("No hay configuración de firma digital para esta empresa");
        return response;
      }

      // 5. Enviar al SRI
      AuthorizationSri authorizationSri =
          sendCreditNoteService.sendCreditNoteSri(
              creditNoteDto, digitalSignatureResponse.getDigitalSignatureDto(), null);

      creditNote.setAuthorizationSri(authorizationSri);
      CreditNote savedCreditNote = repo.save(creditNote);

      response = serviceMapper.toCreditNoteResponse(savedCreditNote);
      response.setCode(HttpStatus.OK.value());
      response.setStatus(
          authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "ENVIADO");
      response.setMessage(
          authorizationSri != null
                  && "AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())
              ? "Nota de crédito autorizada exitosamente"
              : "Nota de crédito enviada al SRI");

      // 6. Notificar si fue autorizada
      if (authorizationSri != null
          && "AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())) {
        try {
          creditNoteDeliveryService.deliverAuthorizedCreditNote(
              creditNoteDto, savedCreditNote, authorizationSri);
        } catch (Exception e) {
          log.warn("Error notificando NC autorizada: {}", e.getMessage());
        }
      }

    } catch (Exception e) {
      log.error("Error enviando NC al SRI: {}", e.getMessage(), e);
      response.setMessage("Error enviando al SRI: " + e.getMessage());
    }

    return response;
  }

  @Override
  public Path getPdfFilePath(Long idCreditNote) {
    CreditNote creditNote =
        repo.findById(idCreditNote)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Nota de crédito no encontrada: " + idCreditNote));

    if (creditNote.getAuthorizationSri() == null) {
      throw new IllegalArgumentException("La nota de crédito no tiene autorización del SRI");
    }

    String storagePath = creditNote.getAuthorizationSri().getAuthorizationStoragePath();
    if (!StringUtils.hasText(storagePath)) {
      throw new IllegalArgumentException("No se encontró la ruta de almacenamiento");
    }

    // El PDF se nombra con creditNoteNumber, no con accessKey
    String creditNoteNumber = creditNote.getCreditNoteNumber();
    if (!StringUtils.hasText(creditNoteNumber)) {
      throw new IllegalArgumentException("La nota de crédito no tiene número asignado");
    }

    Path pdfPath = Paths.get(storagePath, creditNoteNumber + ".pdf");

    if (!Files.exists(pdfPath)) {
      throw new IllegalArgumentException("El archivo PDF no existe: " + pdfPath);
    }

    return pdfPath;
  }

  @Override
  public Path getXmlFilePath(Long idCreditNote) {
    CreditNote creditNote =
        repo.findById(idCreditNote)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Nota de crédito no encontrada: " + idCreditNote));

    if (creditNote.getAuthorizationSri() == null) {
      throw new IllegalArgumentException("La nota de crédito no tiene autorización del SRI");
    }

    String storagePath = creditNote.getAuthorizationSri().getAuthorizationStoragePath();
    if (!StringUtils.hasText(storagePath)) {
      throw new IllegalArgumentException("No se encontró la ruta de almacenamiento");
    }

    String accessKey = creditNote.getAuthorizationSri().getAccessKey();
    Path xmlPath = Paths.get(storagePath, accessKey + ".xml");

    if (!Files.exists(xmlPath)) {
      throw new IllegalArgumentException("El archivo XML no existe: " + xmlPath);
    }

    return xmlPath;
  }

  @Override
  public boolean sendCreditNoteByEmail(Long idCreditNote, String alternativeEmail) {
    log.info("Enviando NC {} por email", idCreditNote);

    CreditNote creditNote =
        repo.findById(idCreditNote)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Nota de crédito no encontrada: " + idCreditNote));

    if (creditNote.getAuthorizationSri() == null
        || !"AUTORIZADO".equals(creditNote.getAuthorizationSri().getAuthorizationStatusSri())) {
      throw new IllegalArgumentException(
          "Solo se pueden enviar por email notas de crédito autorizadas");
    }

    // Obtener email del destinatario
    String recipientEmail = alternativeEmail;
    if (!StringUtils.hasText(recipientEmail)) {
      // Buscar email del cliente
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(creditNote.getIdClient());
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          ClientDto clientDto = clientResponse.getClientDtos().get(0);
          if (clientDto.getContacts() != null) {
            recipientEmail =
                clientDto.getContacts().stream()
                    .filter(
                        c ->
                            "MAI".equalsIgnoreCase(c.getContactType())
                                || "EMAIL".equalsIgnoreCase(c.getContactType()))
                    .map(c -> c.getValue())
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
          }
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener email del cliente: {}", e.getMessage());
      }
    }

    if (!StringUtils.hasText(recipientEmail)) {
      throw new IllegalArgumentException("No se encontró email del destinatario");
    }

    try {
      // Obtener archivos
      Path pdfPath = getPdfFilePath(idCreditNote);
      Path xmlPath = getXmlFilePath(idCreditNote);

      byte[] pdfBytes = Files.readAllBytes(pdfPath);
      byte[] xmlBytes = Files.readAllBytes(xmlPath);

      CreditNoteDto creditNoteDto = serviceMapper.toCreditNoteDto(creditNote);
      enrichCreditNoteDto(creditNoteDto);

      // Enviar email
      creditNoteEmailService.sendAuthorizedCreditNote(
          recipientEmail,
          creditNote,
          creditNoteDto,
          creditNote.getAuthorizationSri(),
          pdfBytes,
          xmlBytes);

      // Marcar como enviado
      creditNote.setEmailSent(true);
      creditNote.setEmailSentDate(LocalDateTime.now());
      repo.save(creditNote);

      log.info("Email de NC {} enviado a {}", idCreditNote, recipientEmail);
      return true;

    } catch (Exception e) {
      log.error("Error enviando email de NC: {}", e.getMessage(), e);
      throw new RuntimeException("Error enviando email: " + e.getMessage());
    }
  }

  @Override
  public Page<CreditNoteSearchResponseDto> queryCreditNotes(
      CreditNoteQueryDto queryDto, Pageable pageable) {
    log.info("Querying credit notes with criteria: {}, pageable: {}", queryDto, pageable);

    // Si se busca por identificación del comprador, primero buscar el cliente
    Page<CreditNoteSearchResponseDto> resultTotal = Page.empty();
    if (StringUtils.hasText(queryDto.getBuyerIdentification())) {
      try {
        ClientDtoResponse clientDtoResponse =
            customerClient.getClientByIdentification(queryDto.getBuyerIdentification());

        if (clientDtoResponse != null && !clientDtoResponse.getClientDtos().isEmpty()) {
          List<CreditNoteSearchResponseDto> content = new ArrayList<>();
          for (ClientDto clientDto : clientDtoResponse.getClientDtos()) {

            // Convertir LocalDate a LocalDateTime para la búsqueda
            LocalDateTime creationDateFrom =
                queryDto.getCreationDateFrom() != null
                    ? queryDto.getCreationDateFrom().atStartOfDay()
                    : null;
            LocalDateTime creationDateTo =
                queryDto.getCreationDateTo() != null
                    ? queryDto.getCreationDateTo().atTime(23, 59, 59)
                    : null;

            // Usar el método existente de búsqueda
            Pageable unsortedPageable =
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            Page<CreditNote> creditNotes =
                repo.findCreditNotesByCriteria(
                    queryDto.getCreditNoteNumber(),
                    clientDto != null ? clientDto.getIdClient() : null,
                    creationDateFrom,
                    creationDateTo,
                    queryDto.getReceptionStatusSri(),
                    queryDto.getAuthorizationStatusSri(),
                    unsortedPageable);

            // Mapear a DTO de respuesta
            Page<CreditNoteSearchResponseDto> result =
                creditNotes.map(this::mapToSearchResponseDto);

            // Si se encontró el cliente, agregar el nombre
            if (clientDto != null) {
              for (CreditNoteSearchResponseDto creditNote : result) {
                creditNote.setNameClient(clientDto.getName());
              }
            }
            content.addAll(result.getContent());
          }
          resultTotal = new PageImpl<>(content, pageable, content.size());
        } else {
          log.info("No client found for identification: {}", queryDto.getBuyerIdentification());
          // Si no se encuentra el cliente, retornar página vacía
          return Page.empty(pageable);
        }
      } catch (Exception ex) {
        log.warn(
            "Error searching client by identification {}: {}",
            queryDto.getBuyerIdentification(),
            ex.getMessage());
        // Si hay error al buscar el cliente, retornar página vacía
        return Page.empty(pageable);
      }
    }

    log.info("Found {} credit notes matching criteria", resultTotal.getTotalElements());
    return resultTotal;
  }

  private CreditNoteSearchResponseDto mapToSearchResponseDto(CreditNote creditNote) {
    return CreditNoteSearchResponseDto.builder()
        .idCreditNote(creditNote.getIdCreditNote())
        .creditNoteNumber(creditNote.getCreditNoteNumber())
        .creationDate(creditNote.getCreationDate())
        .idClient(creditNote.getIdClient())
        .totalAmount(creditNote.getTotalAmount())
        .receptionStatusSri(
            Objects.nonNull(creditNote.getAuthorizationSri())
                ? creditNote.getAuthorizationSri().getReceptionStatusSri()
                : null)
        .authorizationStatusSri(
            Objects.nonNull(creditNote.getAuthorizationSri())
                ? creditNote.getAuthorizationSri().getAuthorizationStatusSri()
                : null)
        .storagePath(
            Objects.nonNull(creditNote.getAuthorizationSri())
                ? creditNote.getAuthorizationSri().getAuthorizationStoragePath()
                : null)
        .authorizationNumber(
            Objects.nonNull(creditNote.getAuthorizationSri())
                ? creditNote.getAuthorizationSri().getAuthorizationNumber()
                : null)
        .originalInvoiceId(creditNote.getOriginalInvoiceId())
        .originalInvoiceNumber(creditNote.getOriginalInvoiceNumber())
        .reason(creditNote.getReason())
        .build();
  }

  @Override
  public Page<CreditNoteListResponseDto> searchCreditNotesByCompany(
      com.wallparisoft.ebillbilling.dto.CreditNoteSearchByCompanyDto searchDto, Pageable pageable) {
    log.info(
        "🔍 [NC] Buscando notas de crédito - idCompany: {}, creditNoteNumber: {}, originalInvoiceNumber: {}, page: {}, size: {}",
        searchDto.getIdCompany(),
        searchDto.getCreditNoteNumber(),
        searchDto.getOriginalInvoiceNumber(),
        pageable.getPageNumber(),
        pageable.getPageSize());

    if (searchDto.getIdCompany() == null) {
      throw new IllegalArgumentException("idCompany es obligatorio");
    }

    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<CreditNote> creditNotesPage =
        repo.findCreditNotesByCompanyAndFilters(
            searchDto.getIdCompany(),
            searchDto.getCreditNoteNumber(),
            searchDto.getOriginalInvoiceNumber(),
            unsortedPageable);

    // Pre-cargar datos de clientes
    Set<Long> uniqueClientIds =
        creditNotesPage.getContent().stream()
            .map(CreditNote::getIdClient)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, ClientDto> clientCache = new HashMap<>();
    for (Long idClient : uniqueClientIds) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(idClient);
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          clientCache.put(idClient, clientResponse.getClientDtos().get(0));
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener datos del cliente {}: {}", idClient, e.getMessage());
      }
    }

    // Pre-cargar facturas originales
    Set<Long> invoiceIds =
        creditNotesPage.getContent().stream()
            .map(CreditNote::getOriginalInvoiceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, Invoice> invoiceCache = new HashMap<>();
    for (Long invoiceId : invoiceIds) {
      invoiceRepo.findById(invoiceId).ifPresent(inv -> invoiceCache.put(invoiceId, inv));
    }

    // Mapear a DTO
    List<CreditNoteListResponseDto> responseDtos =
        creditNotesPage.getContent().stream()
            .map(
                cn -> {
                  ClientDto clientDto = clientCache.get(cn.getIdClient());
                  String creditNoteStatus = calculateCreditNoteStatus(cn);
                  String originalInvoiceNumber = cn.getOriginalInvoiceNumber();

                  if (originalInvoiceNumber == null && cn.getOriginalInvoiceId() != null) {
                    Invoice originalInvoice = invoiceCache.get(cn.getOriginalInvoiceId());
                    if (originalInvoice != null) {
                      originalInvoiceNumber = originalInvoice.getInvoiceNumber();
                    }
                  }

                  BigDecimal totalAmount = cn.getTotalAmount();
                  if (totalAmount == null) {
                    if (cn.getValorModificacion() != null) {
                      totalAmount = cn.getValorModificacion();
                    } else if (cn.getCreditNoteDetails() != null
                        && !cn.getCreditNoteDetails().isEmpty()) {
                      BigDecimal subtotal =
                          cn.getCreditNoteDetails().stream()
                              .map(d -> d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO)
                              .reduce(BigDecimal.ZERO, BigDecimal::add);
                      BigDecimal iva =
                          cn.getTotalIva() != null ? cn.getTotalIva() : BigDecimal.ZERO;
                      totalAmount = subtotal.add(iva);
                    }
                  }

                  return CreditNoteListResponseDto.builder()
                      .idCreditNote(cn.getIdCreditNote())
                      .idCompany(cn.getIdCompany())
                      .idClient(cn.getIdClient())
                      .idIssuancePoint(cn.getIdIssuancePoint())
                      .sequential(cn.getSequential())
                      .retryCount(cn.getRetryCount())
                      .issueDate(cn.getIssueDate())
                      .creditNoteNumber(cn.getCreditNoteNumber())
                      .environment(cn.getEnvironment())
                      .additionalInformation(cn.getAdditionalInformation())
                      .totalAmount(totalAmount)
                      .subtotalZero(cn.getSubtotalZero())
                      .subtotalIva(cn.getSubtotalIva())
                      .subtotalIce(cn.getSubtotalIce())
                      .discount(cn.getDiscount())
                      .totalIva(cn.getTotalIva())
                      .totalIce(cn.getTotalIce())
                      .tip(cn.getTip())
                      .valorModificacion(cn.getValorModificacion())
                      .observation(cn.getObservation())
                      .emailSent(cn.getEmailSent())
                      .emailSentDate(cn.getEmailSentDate())
                      .originalInvoiceId(cn.getOriginalInvoiceId())
                      .originalAccessKey(cn.getOriginalAccessKey())
                      .originalInvoiceNumber(originalInvoiceNumber)
                      .originalIssueDate(cn.getOriginalIssueDate())
                      .documentTypeModified(cn.getDocumentTypeModified())
                      .reason(cn.getReason())
                      .creditNoteReason(cn.getCreditNoteReason())
                      .authorizationSri(null)
                      .creditNoteDetails(null)
                      .creationDate(cn.getCreationDate())
                      .updateDate(cn.getUpdateDate())
                      .status(cn.getStatus())
                      .buyerName(clientDto != null ? clientDto.getName() : null)
                      .buyerIdentification(clientDto != null ? clientDto.getIdentification() : null)
                      .creditNoteStatus(creditNoteStatus)
                      .build();
                })
            .collect(Collectors.toList());

    log.info(
        "✅ [NC] Búsqueda completada - Total: {}, Página: {}, Tamaño: {}",
        creditNotesPage.getTotalElements(),
        creditNotesPage.getNumber(),
        creditNotesPage.getSize());

    return new PageImpl<>(responseDtos, pageable, creditNotesPage.getTotalElements());
  }

  @Override
  public List<com.wallparisoft.ebillbilling.dto.CreditNoteBasicInfoDto>
      searchAuthorizedActiveCreditNotesByNumberOrAccessKey(
          com.wallparisoft.ebillbilling.dto.CreditNoteSearchByNumberOrAccessKeyDto searchDto) {
    log.info(
        "🔍 [NC] Buscando notas de crédito autorizadas y activas - idCompany: {}, creditNoteNumber: {}, accessKey: {}",
        searchDto.getIdCompany(),
        searchDto.getCreditNoteNumber(),
        searchDto.getAccessKey());

    // Validar que se proporcione al menos un criterio de búsqueda
    if (!StringUtils.hasText(searchDto.getCreditNoteNumber())
        && !StringUtils.hasText(searchDto.getAccessKey())) {
      throw new IllegalArgumentException(
          "Se debe proporcionar al menos uno de los siguientes criterios: creditNoteNumber o accessKey");
    }

    // Buscar notas de crédito autorizadas y activas
    List<CreditNote> creditNotes =
        repo.findAuthorizedActiveCreditNotesByNumberOrAccessKey(
            searchDto.getIdCompany(), searchDto.getCreditNoteNumber(), searchDto.getAccessKey());

    // Convertir a DTO con productos
    List<com.wallparisoft.ebillbilling.dto.CreditNoteBasicInfoDto> result = new ArrayList<>();
    for (CreditNote creditNote : creditNotes) {
      String accessKey = null;
      String authorizationNumber = null;
      if (creditNote.getAuthorizationSri() != null) {
        accessKey = creditNote.getAuthorizationSri().getAccessKey();
        authorizationNumber = creditNote.getAuthorizationSri().getAuthorizationNumber();
      }

      // Cargar detalles de la nota de crédito (si no están cargados)
      List<com.wallparisoft.ebillbilling.entity.CreditNoteDetail> details =
          creditNote.getCreditNoteDetails();
      if (details == null || details.isEmpty()) {
        // Recargar la nota de crédito con detalles
        CreditNote creditNoteWithDetails =
            repo.findById(creditNote.getIdCreditNote()).orElse(creditNote);
        details = creditNoteWithDetails.getCreditNoteDetails();
      }

      // Mapear detalles a ProductInfoDto
      List<com.wallparisoft.ebillbilling.dto.ProductInfoDto> products = new ArrayList<>();
      if (details != null && !details.isEmpty()) {
        for (com.wallparisoft.ebillbilling.entity.CreditNoteDetail detail : details) {
          ProductDto productDto = null;
          String mainCode = detail.getMainCode();
          String auxiliarCode = detail.getAuxCode();
          String name = detail.getDescription();

          // Intentar obtener datos actualizados del producto si existe idProduct
          if (detail.getIdProduct() != null) {
            try {
              com.wallparisoft.ebillbilling.response.ProductResponse productResponse =
                  productClient.getProductById(detail.getIdProduct());
              if (productResponse != null && productResponse.getProductDto() != null) {
                productDto = mapProductDtoFromService(productResponse.getProductDto());
                if (productDto != null) {
                  // Usar datos del servicio si están disponibles
                  if (StringUtils.hasText(productDto.getMainCode())) {
                    mainCode = productDto.getMainCode();
                  }
                  if (StringUtils.hasText(productDto.getAuxiliarCode())) {
                    auxiliarCode = productDto.getAuxiliarCode();
                  }
                  if (StringUtils.hasText(productDto.getName())) {
                    name = productDto.getName();
                  } else if (StringUtils.hasText(productDto.getDescription())) {
                    name = productDto.getDescription();
                  }
                }
              }
            } catch (Exception e) {
              log.warn(
                  "⚠️ No se pudo cargar el producto {} para NC: {}",
                  detail.getIdProduct(),
                  e.getMessage());
              // Usar datos del detalle como fallback
            }
          }

          // Si no hay código o nombre, usar valores por defecto
          if (!StringUtils.hasText(mainCode)) {
            mainCode = "N/A";
          }
          if (!StringUtils.hasText(name)) {
            name = "Producto no disponible";
          }

          products.add(
              com.wallparisoft.ebillbilling.dto.ProductInfoDto.builder()
                  .idProduct(detail.getIdProduct())
                  .mainCode(mainCode)
                  .auxiliarCode(auxiliarCode)
                  .name(name)
                  .quantity(detail.getQuantity())
                  .unitPrice(detail.getUnitPrice())
                  .discount(detail.getDiscount() != null ? detail.getDiscount() : BigDecimal.ZERO)
                  .subtotal(detail.getSubtotal())
                  .build());
        }
      }

      result.add(
          com.wallparisoft.ebillbilling.dto.CreditNoteBasicInfoDto.builder()
              .accessKey(accessKey)
              .authorizationNumber(authorizationNumber)
              .issueDate(creditNote.getIssueDate())
              .creditNoteNumber(creditNote.getCreditNoteNumber())
              .products(products)
              .build());
    }

    log.info(
        "✅ [NC] Búsqueda completada - Encontradas {} notas de crédito autorizadas y activas",
        result.size());

    return result;
  }
}
