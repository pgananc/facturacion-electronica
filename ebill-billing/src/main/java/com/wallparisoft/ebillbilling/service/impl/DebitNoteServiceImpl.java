package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import com.wallparisoft.ebillbilling.config.SriConfiguration;
import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteListResponseDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteQueryDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.DebitNote;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.mapper.DebitNoteServiceMapper;
import com.wallparisoft.ebillbilling.repository.IDebitNoteDetailRepo;
import com.wallparisoft.ebillbilling.repository.IDebitNoteRepo;
import com.wallparisoft.ebillbilling.repository.IInvoiceRepo;
import com.wallparisoft.ebillbilling.response.DebitNoteResponse;
import com.wallparisoft.ebillbilling.response.DigitalSignatureResponse;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.DebitNoteDeliveryService;
import com.wallparisoft.ebillbilling.service.DebitNoteEmailService;
import com.wallparisoft.ebillbilling.service.DigitalSignatureClient;
import com.wallparisoft.ebillbilling.service.IDebitNoteService;
import com.wallparisoft.ebillbilling.service.ISendDebitNoteService;
import com.wallparisoft.ebillbilling.service.ProductClient;
import com.wallparisoft.integration.sri.util.UtilsMath;
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
public class DebitNoteServiceImpl implements IDebitNoteService {

  private final IDebitNoteRepo repo;
  private final IDebitNoteDetailRepo debitNoteDetailRepo;
  private final IInvoiceRepo invoiceRepo;
  private final ISendDebitNoteService sendDebitNoteService;
  private final DebitNoteServiceMapper serviceMapper;
  private final DigitalSignatureClient digitalSignatureClient;
  private final CustomerClient customerClient;
  private final ProductClient productClient;
  private final DebitNoteDeliveryService debitNoteDeliveryService;
  private final DebitNoteEmailService debitNoteEmailService;
  private final IAuthService authService;
  private final SriConfiguration sriConfiguration;

  @Override
  public DebitNote register(DebitNote obj) {
    return repo.save(obj);
  }

  @Override
  public DebitNote modify(DebitNote obj) {
    return repo.save(obj);
  }

  @Override
  public List<DebitNote> list() {
    return repo.findAll();
  }

  @Override
  public List<DebitNoteListResponseDto> listWithClientData() {
    List<DebitNote> allDebitNotes = repo.findAll();

    List<DebitNote> debitNotes =
        allDebitNotes.stream()
            .filter(
                dn -> {
                  if (dn.getAuthorizationSri() == null) {
                    return true;
                  }
                  String authStatus = dn.getAuthorizationSri().getAuthorizationStatusSri();
                  return "AUTORIZADO".equalsIgnoreCase(authStatus);
                })
            .collect(Collectors.toList());

    Set<Long> uniqueClientIds =
        debitNotes.stream()
            .map(DebitNote::getIdClient)
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

    Set<Long> invoiceIds =
        debitNotes.stream()
            .map(DebitNote::getOriginalInvoiceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, Invoice> invoiceCache = new HashMap<>();
    for (Long invoiceId : invoiceIds) {
      invoiceRepo.findById(invoiceId).ifPresent(inv -> invoiceCache.put(invoiceId, inv));
    }

    return debitNotes.stream()
        .map(
            dn -> {
              ClientDto clientDto = clientCache.get(dn.getIdClient());
              String debitNoteStatus = calculateDebitNoteStatus(dn);
              String originalInvoiceNumber = dn.getOriginalInvoiceNumber();

              if (originalInvoiceNumber == null && dn.getOriginalInvoiceId() != null) {
                Invoice originalInvoice = invoiceCache.get(dn.getOriginalInvoiceId());
                if (originalInvoice != null) {
                  originalInvoiceNumber = originalInvoice.getInvoiceNumber();
                }
              }

              java.math.BigDecimal totalAmount = dn.getTotalAmount();
              if (totalAmount == null) {
                if (dn.getValorTotal() != null) {
                  totalAmount = dn.getValorTotal();
                } else if (dn.getDebitNoteDetails() != null
                    && !dn.getDebitNoteDetails().isEmpty()) {
                  java.math.BigDecimal subtotal =
                      dn.getDebitNoteDetails().stream()
                          .map(
                              d ->
                                  d.getSubtotal() != null
                                      ? d.getSubtotal()
                                      : java.math.BigDecimal.ZERO)
                          .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                  java.math.BigDecimal iva =
                      dn.getTotalIva() != null ? dn.getTotalIva() : java.math.BigDecimal.ZERO;
                  totalAmount = subtotal.add(iva);
                }
              }

              return DebitNoteListResponseDto.builder()
                  .idDebitNote(dn.getIdDebitNote())
                  .idCompany(dn.getIdCompany())
                  .idClient(dn.getIdClient())
                  .idIssuancePoint(dn.getIdIssuancePoint())
                  .sequential(dn.getSequential())
                  .retryCount(dn.getRetryCount())
                  .issueDate(dn.getIssueDate())
                  .debitNoteNumber(dn.getDebitNoteNumber())
                  .environment(dn.getEnvironment())
                  .additionalInformation(dn.getAdditionalInformation())
                  .totalAmount(totalAmount)
                  .subtotalZero(dn.getSubtotalZero())
                  .subtotalIva(dn.getSubtotalIva())
                  .subtotalIce(dn.getSubtotalIce())
                  .discount(dn.getDiscount())
                  .totalIva(dn.getTotalIva())
                  .totalIce(dn.getTotalIce())
                  .valorModificacion(dn.getValorModificacion())
                  .valorTotal(dn.getValorTotal())
                  .observation(dn.getObservation())
                  .emailSent(dn.getEmailSent())
                  .emailSentDate(dn.getEmailSentDate())
                  .originalInvoiceId(dn.getOriginalInvoiceId())
                  .originalAccessKey(dn.getOriginalAccessKey())
                  .originalInvoiceNumber(originalInvoiceNumber)
                  .originalIssueDate(dn.getOriginalIssueDate())
                  .documentTypeModified(dn.getDocumentTypeModified())
                  // No incluir entidades completas para evitar ciclos de serialización
                  .authorizationSri(null)
                  .debitNoteDetails(null)
                  .creationDate(dn.getCreationDate())
                  .updateDate(dn.getUpdateDate())
                  .status(dn.getStatus())
                  .buyerName(clientDto != null ? clientDto.getName() : null)
                  .buyerIdentification(clientDto != null ? clientDto.getIdentification() : null)
                  .debitNoteStatus(debitNoteStatus)
                  .build();
            })
        .collect(Collectors.toList());
  }

  private String calculateDebitNoteStatus(DebitNote dn) {
    if (dn.getAuthorizationSri() == null) {
      return "BORRADOR";
    }
    String authStatus = dn.getAuthorizationSri().getAuthorizationStatusSri();
    if ("AUTORIZADO".equalsIgnoreCase(authStatus)) {
      return "AUTORIZADO";
    }
    String receptionStatus = dn.getAuthorizationSri().getReceptionStatusSri();
    if ("RECIBIDA".equalsIgnoreCase(receptionStatus)
        && !"AUTORIZADO".equalsIgnoreCase(authStatus)) {
      return "EN_PROCESO";
    }
    return "DEVUELTA";
  }

  @Override
  public DebitNote getById(Integer id) {
    return repo.findById(Long.valueOf(id)).orElse(null);
  }

  @Override
  public boolean delete(Integer id) {
    DebitNote debitNote = repo.findById(Long.valueOf(id)).orElse(null);
    if (debitNote != null) {
      if (debitNote.getAuthorizationSri() == null) {
        repo.delete(debitNote);
        return true;
      } else {
        throw new IllegalStateException(
            "No se puede eliminar una nota de débito autorizada. Solo se pueden eliminar borradores.");
      }
    }
    return false;
  }

  @Override
  public DebitNoteResponse saveDebitNote(DebitNoteDto debitNoteDto) {
    validateDebitNoteDto(debitNoteDto);

    DebitNoteResponse response =
        DebitNoteResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    Long idIssuancePoint = extractIssuancePointId(debitNoteDto);
    log.debug("idIssuancePoint extraído: {}", idIssuancePoint);

    if (Objects.nonNull(idIssuancePoint)) {
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;

      debitNoteDto.setIdIssuancePoint(idIssuancePoint);
      debitNoteDto.setSequential(nextSequential);

      enrichDebitNoteDto(debitNoteDto);

      DebitNote debitNote = serviceMapper.toDebitNote(debitNoteDto);
      debitNote.setSequential(nextSequential);
      debitNote.setIdIssuancePoint(idIssuancePoint);
      debitNote.setRetryCount(0);
      // Usar el issueDate del DTO si está presente, de lo contrario usar la fecha/hora actual
      debitNote.setIssueDate(
          debitNoteDto.getIssueDate() != null
              ? debitNoteDto.getIssueDate().atStartOfDay()
              : LocalDateTime.now());

      // Asignar environment desde configuración del backend (NO del frontend)
      String environmentName = sriConfiguration.getEnvironmentName();
      debitNote.setEnvironment(environmentName);
      debitNoteDto.setEnvironment(environmentName);
      log.debug("📝 [ND] Environment asignado desde configuración: {}", environmentName);

      String formattedSequential = String.format("%09d", nextSequential);
      if (debitNoteDto.getEstablishment() != null
          && debitNoteDto.getEstablishment().getIssuancePoint() != null) {
        debitNoteDto
            .getEstablishment()
            .getIssuancePoint()
            .setSequentialInvoice(formattedSequential);

        String estab =
            debitNoteDto.getEstablishment().getCode() != null
                ? debitNoteDto.getEstablishment().getCode()
                : "001";
        String ptoEmi =
            debitNoteDto.getEstablishment().getIssuancePoint().getCode() != null
                ? debitNoteDto.getEstablishment().getIssuancePoint().getCode()
                : "001";
        String debitNoteNumber = String.format("%s-%s-%s", estab, ptoEmi, formattedSequential);
        debitNote.setDebitNoteNumber(debitNoteNumber);
        debitNoteDto.setDebitNoteNumber(debitNoteNumber);
        log.info("📝 [ND] debitNoteNumber generado: {}", debitNoteNumber);
      }

      if (debitNoteDto.getOriginalInvoiceNumber() != null) {
        debitNote.setOriginalInvoiceNumber(debitNoteDto.getOriginalInvoiceNumber());
      }
      if (debitNoteDto.getOriginalIssueDate() != null) {
        debitNote.setOriginalIssueDate(debitNoteDto.getOriginalIssueDate().atStartOfDay());
      }
      if (debitNoteDto.getOriginalAccessKey() != null) {
        debitNote.setOriginalAccessKey(debitNoteDto.getOriginalAccessKey());
      }

      // Calcular todos los totales automáticamente desde debitNoteDetails (fuente de verdad)
      Map<String, BigDecimal> calculatedTotals = calculateAllTotalsFromDetails(debitNoteDto);

      // Validar que el totalAmount enviado coincida con el calculado (tolerancia 0.01)
      if (debitNoteDto.getTotalAmount() != null
          && debitNoteDto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal diferencia =
            debitNoteDto.getTotalAmount().subtract(calculatedTotals.get("totalAmount")).abs();
        if (diferencia.compareTo(new BigDecimal("0.01")) > 0) {
          log.warn(
              "⚠️ [ND] El totalAmount enviado ({}) no coincide con el calculado ({}). Se usará el calculado.",
              debitNoteDto.getTotalAmount(),
              calculatedTotals.get("totalAmount"));
        }
      }

      // Siempre usar los totales calculados (fuente de verdad)
      debitNote.setTotalAmount(calculatedTotals.get("totalAmount"));
      debitNote.setValorTotal(calculatedTotals.get("valorTotal"));
      debitNote.setValorModificacion(calculatedTotals.get("valorModificacion"));
      debitNote.setSubtotalZero(calculatedTotals.get("subtotalZero"));
      debitNote.setSubtotalIva(calculatedTotals.get("subtotalIva"));
      debitNote.setSubtotalIce(calculatedTotals.get("subtotalIce"));
      debitNote.setTotalIva(calculatedTotals.get("totalIva"));
      debitNote.setTotalIce(calculatedTotals.get("totalIce"));

      // Actualizar el DTO con los valores calculados
      debitNoteDto.setTotalAmount(calculatedTotals.get("totalAmount"));
      debitNoteDto.setValorTotal(calculatedTotals.get("valorTotal"));
      debitNoteDto.setValorModificacion(calculatedTotals.get("valorModificacion"));
      debitNoteDto.setSubtotalZero(calculatedTotals.get("subtotalZero"));
      debitNoteDto.setSubtotalIva(calculatedTotals.get("subtotalIva"));
      debitNoteDto.setSubtotalIce(calculatedTotals.get("subtotalIce"));
      debitNoteDto.setTotalIva(calculatedTotals.get("totalIva"));
      debitNoteDto.setTotalIce(calculatedTotals.get("totalIce"));

      try {
        DigitalSignatureResponse digitalSignatureResponse =
            digitalSignatureClient.getDigitalSignatureByCompanyId(
                debitNoteDto.getCompanyDto().getIdCompany());
        if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
          log.error("There is no digital signature configuration");
          response = serviceMapper.toDebitNoteResponse(register(debitNote));
          response.setCode(HttpStatus.BAD_REQUEST.value());
          response.setStatus(HttpStatus.BAD_REQUEST.name());
          response.setMessage(
              "La firma digital caduca y/o no esta configurada comuníquese con el administrador, la nota de débito se registro en borrador.");
          return response;
        }

        log.info("🚀 Enviando Nota de Débito al SRI...");
        AuthorizationSri authorizationSri =
            sendDebitNoteService.sendDebitNoteSri(
                debitNoteDto, digitalSignatureResponse.getDigitalSignatureDto(), null);
        debitNote.setAuthorizationSri(authorizationSri);

        log.info(
            "✅ Respuesta SRI recibida: receptionStatus={}, authorizationStatus={}, accessKey={}, authNumber={}",
            authorizationSri != null ? authorizationSri.getReceptionStatusSri() : "null",
            authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "null",
            authorizationSri != null ? authorizationSri.getAccessKey() : "null",
            authorizationSri != null ? authorizationSri.getAuthorizationNumber() : "null");

        // Si el SRI devolvió un error (DEVUELTA o NO AUTORIZADO), limpiar el debitNoteNumber
        // para que no quede un número "asignado" cuando en realidad falló el envío
        // Nota: El sequential NO se limpia porque ya se usó en la clave de acceso
        boolean debeLimpiarNumero = false;
        if (authorizationSri != null) {
          String receptionStatus = authorizationSri.getReceptionStatusSri();
          String authorizationStatus = authorizationSri.getAuthorizationStatusSri();

          // Limpiar si está DEVUELTA (y no es por clave registrada)
          if ("DEVUELTA".equalsIgnoreCase(receptionStatus)
              && (authorizationSri.getReceptionXml() == null
                  || (!authorizationSri.getReceptionXml().contains("CLAVE ACCESO REGISTRADA")
                      && !authorizationSri
                          .getReceptionXml()
                          .contains("<identificador>43</identificador>")))) {
            debeLimpiarNumero = true;
          } // Limpiar si está NO AUTORIZADO
          else if ("NO AUTORIZADO".equalsIgnoreCase(authorizationStatus)) {
            debeLimpiarNumero = true;
          }
        }

        if (debeLimpiarNumero) {
          log.warn(
              "⚠️ [ND] El SRI devolvió un error (receptionStatus={}, authorizationStatus={}). Limpiando debitNoteNumber para evitar asignación incorrecta.",
              authorizationSri != null ? authorizationSri.getReceptionStatusSri() : "null",
              authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "null");
          debitNote.setDebitNoteNumber(null);
          debitNoteDto.setDebitNoteNumber(null);
        } else {
          // Solo establecer el número desde el XML si la respuesta fue exitosa
          setDebitNoteNumberFromAuthorizationXml(debitNote, authorizationSri);
        }

        DebitNote savedDebitNote = register(debitNote);
        response = serviceMapper.toDebitNoteResponse(savedDebitNote);
        response.setCode(HttpStatus.OK.value());
        response.setStatus(HttpStatus.OK.name());

        // Solo notificar si fue autorizada exitosamente
        if (authorizationSri != null
            && ("RECIBIDA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri())
                || "AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri()))) {
          notifyAuthorizedDebitNote(debitNoteDto, savedDebitNote, authorizationSri);
        }

      } catch (ResourceAccessException e) {
        response = serviceMapper.toDebitNoteResponse(register(debitNote));
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        response.setMessage(
            "Error al obtener la firma digital, comuníquese con el administrador, la nota de débito se registro en borrador.");
      } catch (CertificateException
          | SAXException
          | IOException
          | JAXBException
          | InterruptedException e) {
        response = serviceMapper.toDebitNoteResponse(register(debitNote));
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        response.setMessage(
            "Hubo problemas para conectarse con el SRI, la nota de débito se registro en borrador.");
      }
    } else {
      try {
        enrichDebitNoteDto(debitNoteDto);
      } catch (Exception e) {
        log.warn("Error enriqueciendo ND sin punto de emisión: {}", e.getMessage());
      }
      DebitNote debitNote = serviceMapper.toDebitNote(debitNoteDto);
      debitNote.setRetryCount(0);
      DebitNote savedDebitNote = register(debitNote);
      response = serviceMapper.toDebitNoteResponse(savedDebitNote);
      response.setCode(HttpStatus.OK.value());
      response.setStatus(HttpStatus.OK.name());
      response.setMessage("Nota de débito guardada como borrador (sin punto de emisión)");
    }

    return response;
  }

  private void validateDebitNoteDto(DebitNoteDto debitNoteDto) {
    if (debitNoteDto == null) {
      throw new IllegalArgumentException("DebitNoteDto no puede ser null");
    }

    if (debitNoteDto.getOriginalInvoiceId() == null && debitNoteDto.getIdInvoice() != null) {
      debitNoteDto.setOriginalInvoiceId(debitNoteDto.getIdInvoice());
    }

    if (debitNoteDto.getOriginalInvoiceId() == null
        && !StringUtils.hasText(debitNoteDto.getOriginalAccessKey())) {
      throw new IllegalArgumentException(
          "Se debe especificar el ID de la factura original (idInvoice/originalInvoiceId) o su clave de acceso");
    }

    if (debitNoteDto.getDebitNoteDetails() == null
        || debitNoteDto.getDebitNoteDetails().isEmpty()) {
      if (debitNoteDto.getDetails() != null && !debitNoteDto.getDetails().isEmpty()) {
        debitNoteDto.setDebitNoteDetails(debitNoteDto.getDetails());
      } else {
        throw new IllegalArgumentException(
            "La nota de débito debe tener al menos un detalle (producto)");
      }
    }

    // Validar que cada detalle tenga producto y subtotal válidos (requerido por SRI para generar
    // motivos)
    for (int i = 0; i < debitNoteDto.getDebitNoteDetails().size(); i++) {
      var detail = debitNoteDto.getDebitNoteDetails().get(i);
      if (detail.getProductDto() == null) {
        throw new IllegalArgumentException(
            String.format("El detalle %d debe tener un producto asociado", i + 1));
      }
      if (!StringUtils.hasText(detail.getProductDto().getName())
          && !StringUtils.hasText(detail.getProductDto().getDescription())) {
        throw new IllegalArgumentException(
            String.format("El producto del detalle %d debe tener nombre o descripción", i + 1));
      }
      if (detail.getSubtotal() == null || detail.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            String.format("El detalle %d debe tener un subtotal mayor a cero", i + 1));
      }
    }

    if (debitNoteDto.getOriginalInvoiceId() != null) {
      validateAgainstOriginalInvoice(debitNoteDto);
    }

    // Validar que el total = motivos + impuestos
    validateTotalEqualsMotivosPlusImpuestos(debitNoteDto);
  }

  /**
   * Valida la nota de débito contra la factura original: - Factura debe estar AUTORIZADA - Fecha
   * emisión ND >= fecha factura - Total ND = detalles (productos) + impuestos
   */
  private void validateAgainstOriginalInvoice(DebitNoteDto debitNoteDto) {
    Long invoiceId = debitNoteDto.getOriginalInvoiceId();

    Invoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
    if (invoice == null) {
      throw new IllegalArgumentException("Factura original no encontrada con ID: " + invoiceId);
    }

    if (invoice.getAuthorizationSri() == null
        || !"AUTORIZADO"
            .equalsIgnoreCase(invoice.getAuthorizationSri().getAuthorizationStatusSri())) {
      throw new IllegalArgumentException(
          "Solo se pueden crear notas de débito para facturas autorizadas por el SRI");
    }

    // VALIDACIÓN CLAVE: Fecha ND >= Fecha Factura
    if (debitNoteDto.getIssueDate() != null && invoice.getIssueDate() != null) {
      java.time.LocalDate invoiceDate = invoice.getIssueDate().toLocalDate();
      if (debitNoteDto.getIssueDate().isBefore(invoiceDate)) {
        throw new IllegalArgumentException(
            "La fecha de la nota de débito no puede ser anterior a la fecha de la factura");
      }
    }

    log.debug("Validaciones de ND contra factura {} completadas exitosamente", invoiceId);
  }

  /**
   * VALIDACIÓN CLAVE: Asegurar que el total de la nota de débito sea la suma de (detalles +
   * impuestos). Los detalles (productos) se usan para generar los motivos del SRI.
   */
  private void validateTotalEqualsMotivosPlusImpuestos(DebitNoteDto debitNoteDto) {
    // Calcular total desde los detalles (productos) - estos son la fuente de verdad
    BigDecimal totalDetalles = BigDecimal.ZERO;
    if (debitNoteDto.getDebitNoteDetails() != null
        && !debitNoteDto.getDebitNoteDetails().isEmpty()) {
      totalDetalles =
          debitNoteDto.getDebitNoteDetails().stream()
              .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    BigDecimal totalImpuestos = BigDecimal.ZERO;
    BigDecimal totalIva =
        debitNoteDto.getTotalIva() != null ? debitNoteDto.getTotalIva() : BigDecimal.ZERO;
    BigDecimal totalIce =
        debitNoteDto.getTotalIce() != null ? debitNoteDto.getTotalIce() : BigDecimal.ZERO;
    totalImpuestos = totalIva.add(totalIce);

    BigDecimal sumaEsperada = totalDetalles.add(totalImpuestos);

    BigDecimal totalAmount =
        debitNoteDto.getTotalAmount() != null
            ? debitNoteDto.getTotalAmount()
            : (debitNoteDto.getValorTotal() != null
                ? debitNoteDto.getValorTotal()
                : debitNoteDto.getValorModificacion());

    if (totalAmount != null) {
      BigDecimal diferencia = totalAmount.subtract(sumaEsperada).abs();
      if (diferencia.compareTo(new BigDecimal("0.01")) > 0) {
        throw new IllegalArgumentException(
            String.format(
                "El total de la nota de débito (%.2f) debe ser igual a la suma de detalles (%.2f) + impuestos (%.2f) = %.2f",
                totalAmount, totalDetalles, totalImpuestos, sumaEsperada));
      }
    } else {
      debitNoteDto.setTotalAmount(sumaEsperada);
      debitNoteDto.setValorTotal(sumaEsperada);
      // valorModificacion = totalDetalles (suma de motivos sin impuestos)
      debitNoteDto.setValorModificacion(totalDetalles);
      log.info(
          "📝 [ND] totalAmount calculado desde detalles + impuestos: {} = {} + {}, valorModificacion: {}",
          sumaEsperada,
          totalDetalles,
          totalImpuestos,
          totalDetalles);
    }
  }

  private Long extractIssuancePointId(DebitNoteDto debitNoteDto) {
    if (debitNoteDto.getIdIssuancePoint() != null) {
      return debitNoteDto.getIdIssuancePoint();
    }

    if (debitNoteDto.getEstablishment() != null
        && debitNoteDto.getEstablishment().getIssuancePoint() != null
        && debitNoteDto.getEstablishment().getIssuancePoint().getIdIssuancePoint() != null) {
      return debitNoteDto.getEstablishment().getIssuancePoint().getIdIssuancePoint();
    }

    return null;
  }

  /**
   * Calcula todos los totales automáticamente desde debitNoteDetails. Este método es la fuente de
   * verdad para todos los cálculos de totales.
   *
   * @param debitNoteDto DTO con los detalles
   * @return Map con todos los totales calculados
   */
  private Map<String, BigDecimal> calculateAllTotalsFromDetails(DebitNoteDto debitNoteDto) {
    Map<String, BigDecimal> totals = new HashMap<>();

    List<InvoiceDetailDto> details = debitNoteDto.getDebitNoteDetails();
    if (details == null || details.isEmpty()) {
      details = debitNoteDto.getDetails();
    }
    if (details == null || details.isEmpty()) {
      totals.put("valorModificacion", BigDecimal.ZERO);
      totals.put("subtotalZero", BigDecimal.ZERO);
      totals.put("subtotalIva", BigDecimal.ZERO);
      totals.put("subtotalIce", BigDecimal.ZERO);
      totals.put("totalIva", BigDecimal.ZERO);
      totals.put("totalIce", BigDecimal.ZERO);
      totals.put("valorTotal", BigDecimal.ZERO);
      totals.put("totalAmount", BigDecimal.ZERO);
      return totals;
    }

    BigDecimal valorModificacion = BigDecimal.ZERO; // Suma de subtotales (sin impuestos)
    BigDecimal subtotalZero = BigDecimal.ZERO;
    BigDecimal subtotalIva = BigDecimal.ZERO;
    BigDecimal subtotalIce = BigDecimal.ZERO;
    BigDecimal totalIva = BigDecimal.ZERO;
    BigDecimal totalIce = BigDecimal.ZERO;

    for (InvoiceDetailDto detail : details) {
      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
      valorModificacion = valorModificacion.add(subtotal);

      // Determinar tipo de impuesto y calcular
      ProductDto productDto = detail.getProductDto();
      if (productDto != null) {
        // Obtener rate del producto, o del detalle (ivaPercentage), o usar 0
        BigDecimal rate =
            productDto.getRate() != null
                ? productDto.getRate()
                : (detail.getIvaPercentage() != null
                    ? detail
                        .getIvaPercentage()
                        .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        String taxCode = productDto.getTaxCode();

        // Calcular ICE primero si existe (afecta la base del IVA)
        BigDecimal iceDeEsteProducto = BigDecimal.ZERO;
        if (productDto.getIceRate() != null
            && productDto.getIceRate().compareTo(BigDecimal.ZERO) > 0) {
          iceDeEsteProducto = UtilsMath.iva(subtotal, productDto.getIceRate());
          totalIce = totalIce.add(iceDeEsteProducto);
          subtotalIce = subtotalIce.add(subtotal);
        }

        // Calcular IVA (base puede incluir ICE)
        BigDecimal baseIva = subtotal;
        if (iceDeEsteProducto.compareTo(BigDecimal.ZERO) > 0) {
          baseIva = subtotal.add(iceDeEsteProducto);
        }

        if (rate.compareTo(BigDecimal.ZERO) > 0 && ("2".equals(taxCode) || taxCode == null)) {
          // Calcular IVA: si rate viene como decimal (0.15), multiplicar directamente
          // Si rate viene como porcentaje (15), usar UtilsMath.iva
          BigDecimal ivaDeEsteProducto;
          if (rate.compareTo(BigDecimal.ONE) < 0) {
            // Rate viene como decimal (0.15), calcular directamente
            ivaDeEsteProducto = baseIva.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
          } else {
            // Rate viene como porcentaje (15), usar UtilsMath.iva
            ivaDeEsteProducto = UtilsMath.iva(baseIva, rate);
          }
          totalIva = totalIva.add(ivaDeEsteProducto);
          subtotalIva = subtotalIva.add(baseIva);
        } else if (rate.compareTo(BigDecimal.ZERO) == 0 || "0".equals(taxCode)) {
          subtotalZero = subtotalZero.add(subtotal);
        }
      } else {
        // Si no hay producto, asumir sin impuestos
        subtotalZero = subtotalZero.add(subtotal);
      }
    }

    BigDecimal valorTotal = valorModificacion.add(totalIva).add(totalIce);

    totals.put("valorModificacion", valorModificacion);
    totals.put("subtotalZero", subtotalZero);
    totals.put("subtotalIva", subtotalIva);
    totals.put("subtotalIce", subtotalIce);
    totals.put("totalIva", totalIva);
    totals.put("totalIce", totalIce);
    totals.put("valorTotal", valorTotal);
    totals.put("totalAmount", valorTotal); // totalAmount = valorTotal

    log.info(
        "📝 [ND] Totales calculados automáticamente: valorModificacion={}, subtotalIva={}, subtotalIce={}, totalIva={}, totalIce={}, valorTotal={}",
        valorModificacion,
        subtotalIva,
        subtotalIce,
        totalIva,
        totalIce,
        valorTotal);

    return totals;
  }

  /**
   * Calcula el total sin impuestos (suma de subtotales de los detalles). Este valor se usa para
   * valorModificacion en notas de débito.
   *
   * @deprecated Usar calculateAllTotalsFromDetails en su lugar
   */
  @Deprecated
  private BigDecimal calculateTotalSinImpuestosFromDto(DebitNoteDto debitNoteDto) {
    Map<String, BigDecimal> totals = calculateAllTotalsFromDetails(debitNoteDto);
    return totals.get("valorModificacion");
  }

  /**
   * Calcula el total amount (valorTotal).
   *
   * @deprecated Usar calculateAllTotalsFromDetails en su lugar
   */
  @Deprecated
  private BigDecimal calculateTotalAmountFromDto(DebitNoteDto debitNoteDto) {
    Map<String, BigDecimal> totals = calculateAllTotalsFromDetails(debitNoteDto);
    return totals.get("totalAmount");
  }

  private void enrichDebitNoteDto(DebitNoteDto debitNoteDto) {
    if (debitNoteDto.getCompanyDto() == null
        || debitNoteDto.getCompanyDto().getIdCompany() == null) {
      throw new IllegalArgumentException("CompanyDto es requerido");
    }

    try {
      Long idCompany = debitNoteDto.getCompanyDto().getIdCompany();
      CompanyDto companyDto = customerClient.getCompanyById(idCompany);
      if (companyDto != null) {
        debitNoteDto.setCompanyDto(companyDto);
      } else {
        throw new IllegalArgumentException("No se encontró la compañía con ID: " + idCompany);
      }
    } catch (Exception e) {
      log.error("Error loading company: {}", e.getMessage());
      throw new IllegalArgumentException("Error al cargar datos de la compañía: " + e.getMessage());
    }

    if (debitNoteDto.getClientDto() == null || debitNoteDto.getClientDto().getIdClient() == null) {
      throw new IllegalArgumentException("ClientDto es requerido");
    }

    try {
      ClientDto clientDto =
          customerClient
              .getClientById(debitNoteDto.getClientDto().getIdClient())
              .getClientDtos()
              .get(0);
      debitNoteDto.setClientDto(clientDto);
    } catch (Exception e) {
      log.warn("Error loading client: {}", e.getMessage());
    }

    Long idIssuancePoint = extractIssuancePointId(debitNoteDto);

    if (idIssuancePoint != null) {
      try {
        EstablishmentDto establishmentDto =
            customerClient.getEstablishmentWithIssuancePoint(idIssuancePoint);
        if (establishmentDto != null) {
          debitNoteDto.setEstablishment(establishmentDto);
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
          "Se requiere el punto de emisión (idIssuancePoint) para crear la nota de débito");
    }

    Long originalInvoiceId =
        debitNoteDto.getOriginalInvoiceId() != null
            ? debitNoteDto.getOriginalInvoiceId()
            : debitNoteDto.getIdInvoice();

    if (originalInvoiceId != null) {
      try {
        Invoice originalInvoice = invoiceRepo.findById(originalInvoiceId).orElse(null);
        if (originalInvoice != null) {
          if (!StringUtils.hasText(debitNoteDto.getOriginalInvoiceNumber())) {
            debitNoteDto.setOriginalInvoiceNumber(originalInvoice.getInvoiceNumber());
          }

          if (debitNoteDto.getOriginalIssueDate() == null) {
            // Si no viene en el DTO, obtenerla de la factura original
            if (originalInvoice.getIssueDate() != null) {
              debitNoteDto.setOriginalIssueDate(originalInvoice.getIssueDate().toLocalDate());
            } else if (originalInvoice.getAuthorizationSri() != null
                && StringUtils.hasText(originalInvoice.getAuthorizationSri().getAccessKey())) {
              String accessKey = originalInvoice.getAuthorizationSri().getAccessKey();
              try {
                String dateStr = accessKey.substring(0, 8);
                java.time.LocalDate extractedDate =
                    java.time.LocalDate.parse(
                        dateStr, java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
                debitNoteDto.setOriginalIssueDate(extractedDate);
              } catch (Exception e) {
                log.warn("⚠️ [ND] No se pudo extraer fecha del accessKey: {}", e.getMessage());
              }
            }
          } else {
            // Si el usuario envió originalIssueDate, validar que no sea mayor a la fecha actual
            java.time.LocalDate fechaActual = java.time.LocalDate.now();
            if (debitNoteDto.getOriginalIssueDate().isAfter(fechaActual)) {
              log.error(
                  "❌ [ND] La fecha de emisión del documento sustento ({}) no puede ser mayor a la fecha actual ({})",
                  debitNoteDto.getOriginalIssueDate(),
                  fechaActual);
              throw new IllegalArgumentException(
                  "La fecha de emisión del documento sustento no puede ser mayor a la fecha actual");
            }

            // Validar que la fecha enviada coincida con la fecha de la factura original
            // (tolerancia de 1 día por posibles diferencias de timezone)
            if (originalInvoice.getIssueDate() != null) {
              java.time.LocalDate fechaFacturaOriginal =
                  originalInvoice.getIssueDate().toLocalDate();
              long diasDiferencia =
                  java.time.temporal.ChronoUnit.DAYS.between(
                      fechaFacturaOriginal, debitNoteDto.getOriginalIssueDate());
              if (Math.abs(diasDiferencia) > 1) {
                log.warn(
                    "⚠️ [ND] La fecha de emisión del documento sustento ({}) no coincide con la fecha de la factura original ({}). Diferencia: {} días",
                    debitNoteDto.getOriginalIssueDate(),
                    fechaFacturaOriginal,
                    diasDiferencia);
                // No lanzar excepción, solo advertir, ya que puede haber casos válidos
              }
            }
          }

          if (!StringUtils.hasText(debitNoteDto.getOriginalAccessKey())
              && originalInvoice.getAuthorizationSri() != null) {
            debitNoteDto.setOriginalAccessKey(originalInvoice.getAuthorizationSri().getAccessKey());
          }

          if (debitNoteDto.getClientDto() == null
              || debitNoteDto.getClientDto().getIdClient() == null) {
            if (originalInvoice.getIdClient() != null) {
              ClientDto clientDto = new ClientDto();
              clientDto.setIdClient(originalInvoice.getIdClient());
              debitNoteDto.setClientDto(clientDto);
            }
          }
        } else {
          throw new IllegalArgumentException(
              "No se encontró la factura original con ID: " + originalInvoiceId);
        }
      } catch (IllegalArgumentException e) {
        throw e;
      } catch (Exception e) {
        log.error(
            "❌ [ND] Error cargando factura original {}: {}", originalInvoiceId, e.getMessage());
        throw new IllegalArgumentException("Error cargando factura original: " + e.getMessage());
      }
    } else {
      throw new IllegalArgumentException(
          "El ID de la factura original (originalInvoiceId o idInvoice) es obligatorio");
    }

    if (!StringUtils.hasText(debitNoteDto.getOriginalInvoiceNumber())) {
      throw new IllegalArgumentException(
          "El número de factura original (originalInvoiceNumber) no pudo ser determinado");
    }
    if (debitNoteDto.getOriginalIssueDate() == null) {
      throw new IllegalArgumentException(
          "La fecha de emisión de la factura original (originalIssueDate) no pudo ser determinada");
    }

    if (debitNoteDto.getDebitNoteDetails() != null) {
      for (InvoiceDetailDto detail : debitNoteDto.getDebitNoteDetails()) {
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

  private ProductDto mapProductDtoFromService(java.util.Map<String, Object> productDtoFromService) {
    if (Objects.isNull(productDtoFromService) || productDtoFromService.isEmpty()) {
      return null;
    }

    try {
      ProductDto productDto = new ProductDto();

      if (productDtoFromService.containsKey("idProduct")) {
        Object idProduct = productDtoFromService.get("idProduct");
        if (idProduct instanceof Integer) {
          productDto.setIdProduct(((Integer) idProduct).longValue());
        } else if (idProduct instanceof Long) {
          productDto.setIdProduct((Long) idProduct);
        }
      }

      if (productDtoFromService.containsKey("mainCode")) {
        productDto.setMainCode((String) productDtoFromService.get("mainCode"));
      }

      if (productDtoFromService.containsKey("name")) {
        productDto.setName((String) productDtoFromService.get("name"));
      }

      if (productDtoFromService.containsKey("ivaTaxType")
          && productDtoFromService.get("ivaTaxType") != null) {
        Object taxTypeObj = productDtoFromService.get("ivaTaxType");
        if (taxTypeObj instanceof java.util.Map) {
          java.util.Map<String, Object> taxTypeMap = (java.util.Map<String, Object>) taxTypeObj;

          if (taxTypeMap.containsKey("value")) {
            Object value = taxTypeMap.get("value");
            if (value instanceof Number) {
              java.math.BigDecimal rate =
                  new java.math.BigDecimal(value.toString())
                      .setScale(4, java.math.RoundingMode.HALF_UP);
              productDto.setRate(rate);
            }
          }

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

      return productDto;
    } catch (Exception e) {
      log.error("Error mapping ProductDto from service: {}", e.getMessage(), e);
      return null;
    }
  }

  private void setDebitNoteNumberFromAuthorizationXml(
      DebitNote debitNote, AuthorizationSri authorizationSri) {
    if (authorizationSri == null
        || !"AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())
        || !StringUtils.hasText(authorizationSri.getAuthorizationXml())) {
      return;
    }

    try {
      if (debitNote.getSequential() != null && debitNote.getIdIssuancePoint() != null) {
        try {
          EstablishmentDto establishmentDto =
              customerClient.getEstablishmentWithIssuancePoint(debitNote.getIdIssuancePoint());
          if (establishmentDto != null
              && establishmentDto.getIssuancePoint() != null
              && StringUtils.hasText(establishmentDto.getCode())
              && StringUtils.hasText(establishmentDto.getIssuancePoint().getCode())) {
            String debitNoteNumber =
                String.format(
                    "%s-%s-%09d",
                    establishmentDto.getCode(),
                    establishmentDto.getIssuancePoint().getCode(),
                    debitNote.getSequential());
            debitNote.setDebitNoteNumber(debitNoteNumber);
          }
        } catch (Exception e) {
          log.warn("Error loading establishment for debit note number: {}", e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Error extracting debit note number from authorization XML: {}", e.getMessage());
    }
  }

  private void notifyAuthorizedDebitNote(
      DebitNoteDto debitNoteDto, DebitNote savedDebitNote, AuthorizationSri authorizationSri) {

    if (Boolean.TRUE.equals(savedDebitNote.getEmailSent())) {
      log.info(
          "Email already sent for debit note {} on {}. Skipping notification.",
          savedDebitNote.getIdDebitNote(),
          savedDebitNote.getEmailSentDate());
      return;
    }

    if (Objects.isNull(authorizationSri)
        || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
        || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
      return;
    }

    if (!StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      log.warn(
          "Authorization XML path is missing for debit note {}. Skipping email notification.",
          savedDebitNote.getIdDebitNote());
      return;
    }

    ClientDto clientDtoWithContacts = null;
    try {
      if (debitNoteDto.getClientDto() != null
          && debitNoteDto.getClientDto().getIdClient() != null) {
        clientDtoWithContacts =
            customerClient
                .getClientById(debitNoteDto.getClientDto().getIdClient())
                .getClientDtos()
                .get(0);
        debitNoteDto.setClientDto(clientDtoWithContacts);
      }
    } catch (Exception e) {
      log.warn("Error loading client for debit note notification: {}", e.getMessage());
    }

    debitNoteDeliveryService.deliverAuthorizedDebitNote(
        debitNoteDto, savedDebitNote, authorizationSri);

    String recipientEmail = getRecipientEmail(debitNoteDto, clientDtoWithContacts);
    if (!StringUtils.hasText(recipientEmail)) {
      log.info(
          "No recipient email found for debit note {}. PDF and XML generated, but email not sent. Marking emailSent as false.",
          savedDebitNote.getIdDebitNote());
      savedDebitNote.setEmailSent(false);
      repo.save(savedDebitNote);
    }
  }

  private String getRecipientEmail(DebitNoteDto debitNoteDto, ClientDto clientDto) {
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

    if (debitNoteDto != null
        && debitNoteDto.getClientDto() != null
        && debitNoteDto.getClientDto().getContacts() != null) {
      String emailFromDebitNoteDto =
          debitNoteDto.getClientDto().getContacts().stream()
              .filter(contact -> "MAI".equalsIgnoreCase(contact.getContactType()))
              .filter(
                  contact ->
                      contact.getStatus() == null || Boolean.TRUE.equals(contact.getStatus()))
              .findFirst()
              .map(contact -> contact.getValue())
              .orElse(null);
      if (StringUtils.hasText(emailFromDebitNoteDto)) {
        return emailFromDebitNoteDto;
      }
    }

    return null;
  }

  @Override
  public DebitNoteResponse updateDebitNote(Long id, DebitNoteDto debitNoteDto) {
    DebitNote existingDebitNote = repo.findById(id).orElse(null);
    if (existingDebitNote == null) {
      throw new IllegalArgumentException("Nota de débito no encontrada con ID: " + id);
    }

    if (existingDebitNote.getAuthorizationSri() != null) {
      throw new IllegalStateException(
          "No se puede actualizar una nota de débito autorizada. Solo se pueden actualizar borradores.");
    }

    validateDebitNoteDto(debitNoteDto);
    enrichDebitNoteDto(debitNoteDto);

    // Calcular todos los totales automáticamente desde debitNoteDetails
    Map<String, BigDecimal> calculatedTotals = calculateAllTotalsFromDetails(debitNoteDto);

    // Actualizar el DTO con los valores calculados
    debitNoteDto.setTotalAmount(calculatedTotals.get("totalAmount"));
    debitNoteDto.setValorTotal(calculatedTotals.get("valorTotal"));
    debitNoteDto.setValorModificacion(calculatedTotals.get("valorModificacion"));
    debitNoteDto.setSubtotalZero(calculatedTotals.get("subtotalZero"));
    debitNoteDto.setSubtotalIva(calculatedTotals.get("subtotalIva"));
    debitNoteDto.setSubtotalIce(calculatedTotals.get("subtotalIce"));
    debitNoteDto.setTotalIva(calculatedTotals.get("totalIva"));
    debitNoteDto.setTotalIce(calculatedTotals.get("totalIce"));

    DebitNote debitNote = serviceMapper.toDebitNote(debitNoteDto);
    debitNote.setIdDebitNote(id);
    debitNote.setCreationDate(existingDebitNote.getCreationDate());

    // Asignar environment desde configuración del backend (NO del frontend)
    String environmentName = sriConfiguration.getEnvironmentName();
    debitNote.setEnvironment(environmentName);
    debitNoteDto.setEnvironment(environmentName);

    // Asegurar que los totales calculados se guarden
    debitNote.setTotalAmount(calculatedTotals.get("totalAmount"));
    debitNote.setValorTotal(calculatedTotals.get("valorTotal"));
    debitNote.setValorModificacion(calculatedTotals.get("valorModificacion"));
    debitNote.setSubtotalZero(calculatedTotals.get("subtotalZero"));
    debitNote.setSubtotalIva(calculatedTotals.get("subtotalIva"));
    debitNote.setSubtotalIce(calculatedTotals.get("subtotalIce"));
    debitNote.setTotalIva(calculatedTotals.get("totalIva"));
    debitNote.setTotalIce(calculatedTotals.get("totalIce"));

    DebitNote updatedDebitNote = register(debitNote);
    DebitNoteResponse response = serviceMapper.toDebitNoteResponse(updatedDebitNote);
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    response.setMessage("Nota de débito actualizada exitosamente");

    return response;
  }

  @Override
  public DebitNoteDto getDebitNoteById(Long id) {
    DebitNote debitNote = repo.findById(id).orElse(null);
    if (debitNote == null) {
      return null;
    }

    DebitNoteDto debitNoteDto = serviceMapper.toDebitNoteDto(debitNote);
    enrichDebitNoteDto(debitNoteDto);
    return debitNoteDto;
  }

  @Override
  public DebitNoteDto getDraftDebitNoteById(Long idDebitNote) {
    log.info("📝 [ND] Buscando borrador con ID: {}", idDebitNote);

    // 1. Buscar la nota de débito por ID
    DebitNote debitNote =
        repo.findById(idDebitNote)
            .orElseThrow(
                () -> {
                  log.error("❌ [ND] Borrador con ID {} no encontrado", idDebitNote);
                  return new IllegalArgumentException(
                      "Borrador no encontrado con ID: " + idDebitNote);
                });

    // 2. Verificar que sea un borrador (authorizationSri == null)
    if (Objects.nonNull(debitNote.getAuthorizationSri())) {
      log.warn("⚠️ [ND] La nota de débito {} ya fue procesada", idDebitNote);
      throw new IllegalArgumentException(
          "Esta nota de débito ya fue procesada y autorizada por el SRI. No es un borrador editable.");
    }

    log.info("✅ [ND] Borrador encontrado. Total: ${}", debitNote.getTotalAmount());

    // 3. Convertir a DTO usando el mapper
    DebitNoteDto debitNoteDto = serviceMapper.toDebitNoteDto(debitNote);

    // 4. Establecer status como DRAFT
    debitNoteDto.setStatus("DRAFT");

    // 5. Establecer IDs en los DTOs antes de enriquecer (necesario para enrichDebitNoteDto)
    if (debitNoteDto.getCompanyDto() != null && debitNote.getIdCompany() != null) {
      debitNoteDto.getCompanyDto().setIdCompany(debitNote.getIdCompany());
    }
    if (debitNoteDto.getClientDto() != null && debitNote.getIdClient() != null) {
      debitNoteDto.getClientDto().setIdClient(debitNote.getIdClient());
    }

    // 6. Enriquecer con datos completos (cliente, compañía, establecimiento, productos)
    enrichDebitNoteDto(debitNoteDto);

    log.info("✅ [ND] Borrador completo retornado para edición (ID: {})", idDebitNote);
    return debitNoteDto;
  }

  @Override
  public Page<DebitNote> searchDebitNotes(
      String debitNoteNumber, Long idClient, Long originalInvoiceId, Pageable pageable) {
    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return repo.findDebitNotesByCriteria(
        debitNoteNumber, idClient, null, null, null, null, unsortedPageable);
  }

  @Override
  public List<DebitNote> getDebitNotesByOriginalInvoice(Long originalInvoiceId) {
    return repo.findByOriginalInvoiceId(originalInvoiceId);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void executeSendDebitNoteToSri() {
    TokenInformation tokenInformation = new TokenInformation();
    tokenInformation.setUserName("SCHEDULED_TASK");
    tokenInformation.setRoles(List.of("SCHEDULED_TASK"));
    tokenInformation.setCompanyEstablishments(new ArrayList<>());

    TokenSession tokenSession;
    try {
      tokenSession = authService.generateTokenForCron(tokenInformation, 60);
      log.debug("Generated token for scheduled task execution");
    } catch (Exception e) {
      log.error("Failed to generate token for scheduled task: {}", e.getMessage(), e);
      return;
    }

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
      List<DebitNote> debitNotes =
          this.repo.findDebitNotesExcludingSpecificSriStatuses("RECIBIDA", "AUTORIZADO");
      log.info("Found {} debit notes pending to send to SRI", debitNotes.size());

      debitNotes.forEach(
          debitNote -> {
            if (Objects.isNull(debitNote.getRetryCount())) {
              debitNote.setRetryCount(0);
            }

            if (debitNote.getRetryCount() >= 5) {
              log.warn(
                  "Debit note {} has exceeded maximum retries ({}), marking as inactive",
                  debitNote.getIdDebitNote(),
                  debitNote.getRetryCount());
              debitNote.setStatus(false);
              repo.save(debitNote);
              return;
            }

            try {
              DigitalSignatureResponse digitalSignatureResponse =
                  digitalSignatureClient.getDigitalSignatureByCompanyId(debitNote.getIdCompany());
              if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
                log.warn(
                    "Digital signature not found for company {} - Debit Note {}",
                    debitNote.getIdCompany(),
                    debitNote.getIdDebitNote());
                debitNote.setRetryCount(debitNote.getRetryCount() + 1);
                repo.save(debitNote);
                return;
              }

              DebitNoteDto debitNoteDto = serviceMapper.toDebitNoteDto(debitNote);
              enrichDebitNoteDto(debitNoteDto);

              AuthorizationSri authorizationSri =
                  sendDebitNoteService.sendDebitNoteSri(
                      debitNoteDto,
                      digitalSignatureResponse.getDigitalSignatureDto(),
                      debitNote.getAuthorizationSri());

              debitNote.setAuthorizationSri(authorizationSri);
              repo.save(debitNote);

              if (authorizationSri != null
                  && "AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())) {
                try {
                  debitNoteDeliveryService.deliverAuthorizedDebitNote(
                      debitNoteDto, debitNote, authorizationSri);
                } catch (Exception e) {
                  log.warn("Error notificando ND autorizada: {}", e.getMessage());
                }
              }

            } catch (Exception e) {
              log.error(
                  "Error processing debit note {}: {}", debitNote.getIdDebitNote(), e.getMessage());
              debitNote.setRetryCount(debitNote.getRetryCount() + 1);
              repo.save(debitNote);
            }
          });
    } finally {
      SecurityContextHolder.setContext(previousContext);
    }
  }

  @Override
  public DebitNoteResponse saveDebitNoteDraft(DebitNoteDto debitNoteDto) {
    log.info("Guardando nota de débito como borrador");

    validateDebitNoteDto(debitNoteDto);
    enrichDebitNoteDto(debitNoteDto);

    DebitNote debitNote = serviceMapper.toDebitNote(debitNoteDto);
    DebitNoteResponse response =
        DebitNoteResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    try {
      debitNote.setSequential(null);
      debitNote.setIdIssuancePoint(debitNoteDto.getIdIssuancePoint());
      debitNote.setRetryCount(0);
      debitNote.setIssueDate(
          debitNoteDto.getIssueDate() != null
              ? debitNoteDto.getIssueDate().atStartOfDay()
              : LocalDateTime.now());
      debitNote.setAuthorizationSri(null);

      // Asignar environment desde configuración del backend (NO del frontend)
      String environmentName = sriConfiguration.getEnvironmentName();
      debitNote.setEnvironment(environmentName);
      debitNoteDto.setEnvironment(environmentName);
      log.debug("📝 [ND] Environment asignado desde configuración (borrador): {}", environmentName);

      if (debitNoteDto.getOriginalInvoiceNumber() != null) {
        debitNote.setOriginalInvoiceNumber(debitNoteDto.getOriginalInvoiceNumber());
      }
      // Calcular todos los totales automáticamente desde debitNoteDetails
      Map<String, BigDecimal> calculatedTotals = calculateAllTotalsFromDetails(debitNoteDto);

      // Siempre usar los totales calculados (fuente de verdad)
      debitNote.setTotalAmount(calculatedTotals.get("totalAmount"));
      debitNote.setValorTotal(calculatedTotals.get("valorTotal"));
      debitNote.setValorModificacion(calculatedTotals.get("valorModificacion"));
      debitNote.setSubtotalZero(calculatedTotals.get("subtotalZero"));
      debitNote.setSubtotalIva(calculatedTotals.get("subtotalIva"));
      debitNote.setSubtotalIce(calculatedTotals.get("subtotalIce"));
      debitNote.setTotalIva(calculatedTotals.get("totalIva"));
      debitNote.setTotalIce(calculatedTotals.get("totalIce"));

      // Actualizar el DTO con los valores calculados
      debitNoteDto.setTotalAmount(calculatedTotals.get("totalAmount"));
      debitNoteDto.setValorTotal(calculatedTotals.get("valorTotal"));
      debitNoteDto.setValorModificacion(calculatedTotals.get("valorModificacion"));
      debitNoteDto.setSubtotalZero(calculatedTotals.get("subtotalZero"));
      debitNoteDto.setSubtotalIva(calculatedTotals.get("subtotalIva"));
      debitNoteDto.setSubtotalIce(calculatedTotals.get("subtotalIce"));
      debitNoteDto.setTotalIva(calculatedTotals.get("totalIva"));
      debitNoteDto.setTotalIce(calculatedTotals.get("totalIce"));

      DebitNote savedDebitNote = repo.save(debitNote);

      response = serviceMapper.toDebitNoteResponse(savedDebitNote);
      response.setCode(HttpStatus.OK.value());
      response.setStatus("BORRADOR");
      response.setMessage("Nota de débito guardada como borrador");

      log.info("Borrador de ND guardado con ID: {}", savedDebitNote.getIdDebitNote());

    } catch (Exception e) {
      log.error("Error guardando borrador de ND: {}", e.getMessage(), e);
      response.setMessage("Error guardando borrador: " + e.getMessage());
    }

    return response;
  }

  @Override
  public DebitNoteResponse sendDebitNoteToSri(Long idDebitNote, String password) {
    log.info("Enviando ND borrador {} al SRI", idDebitNote);

    DebitNoteResponse response =
        DebitNoteResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    DebitNote debitNote = repo.findById(idDebitNote).orElse(null);
    if (debitNote == null) {
      response.setMessage("Nota de débito no encontrada con ID: " + idDebitNote);
      return response;
    }

    if (debitNote.getAuthorizationSri() != null) {
      response.setMessage("Esta nota de débito ya fue enviada al SRI");
      return response;
    }

    DebitNoteDto debitNoteDto = serviceMapper.toDebitNoteDto(debitNote);

    if (debitNote.getSequential() == null && debitNote.getIdIssuancePoint() != null) {
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(debitNote.getIdIssuancePoint());
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;
      debitNote.setSequential(nextSequential);
      debitNoteDto.setSequential(nextSequential);
    }

    enrichDebitNoteDto(debitNoteDto);

    try {
      DigitalSignatureResponse digitalSignatureResponse =
          digitalSignatureClient.getDigitalSignatureByCompanyId(
              debitNoteDto.getCompanyDto().getIdCompany());

      if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
        response.setMessage("No hay configuración de firma digital para esta empresa");
        return response;
      }

      AuthorizationSri authorizationSri =
          sendDebitNoteService.sendDebitNoteSri(
              debitNoteDto, digitalSignatureResponse.getDigitalSignatureDto(), null);

      debitNote.setAuthorizationSri(authorizationSri);
      DebitNote savedDebitNote = repo.save(debitNote);

      response = serviceMapper.toDebitNoteResponse(savedDebitNote);
      response.setCode(HttpStatus.OK.value());
      response.setStatus(
          authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "ENVIADO");
      response.setMessage(
          authorizationSri != null
                  && "AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())
              ? "Nota de débito autorizada exitosamente"
              : "Nota de débito enviada al SRI");

      if (authorizationSri != null
          && "AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())) {
        try {
          debitNoteDeliveryService.deliverAuthorizedDebitNote(
              debitNoteDto, savedDebitNote, authorizationSri);
        } catch (Exception e) {
          log.warn("Error notificando ND autorizada: {}", e.getMessage());
        }
      }

    } catch (Exception e) {
      log.error("Error enviando ND al SRI: {}", e.getMessage(), e);
      response.setMessage("Error enviando al SRI: " + e.getMessage());
    }

    return response;
  }

  @Override
  public Path getPdfFilePath(Long idDebitNote) {
    DebitNote debitNote =
        repo.findById(idDebitNote)
            .orElseThrow(
                () -> new IllegalArgumentException("Nota de débito no encontrada: " + idDebitNote));

    if (debitNote.getAuthorizationSri() == null) {
      throw new IllegalArgumentException("La nota de débito no tiene autorización del SRI");
    }

    String storagePath = debitNote.getAuthorizationSri().getAuthorizationStoragePath();
    if (!StringUtils.hasText(storagePath)) {
      throw new IllegalArgumentException("No se encontró la ruta de almacenamiento");
    }

    String debitNoteNumber = debitNote.getDebitNoteNumber();
    if (!StringUtils.hasText(debitNoteNumber)) {
      throw new IllegalArgumentException("La nota de débito no tiene número asignado");
    }

    Path pdfPath = Paths.get(storagePath, debitNoteNumber + ".pdf");

    if (!Files.exists(pdfPath)) {
      throw new IllegalArgumentException("El archivo PDF no existe: " + pdfPath);
    }

    return pdfPath;
  }

  @Override
  public Path getXmlFilePath(Long idDebitNote) {
    DebitNote debitNote =
        repo.findById(idDebitNote)
            .orElseThrow(
                () -> new IllegalArgumentException("Nota de débito no encontrada: " + idDebitNote));

    if (debitNote.getAuthorizationSri() == null) {
      throw new IllegalArgumentException("La nota de débito no tiene autorización del SRI");
    }

    String storagePath = debitNote.getAuthorizationSri().getAuthorizationStoragePath();
    if (!StringUtils.hasText(storagePath)) {
      throw new IllegalArgumentException("No se encontró la ruta de almacenamiento");
    }

    String accessKey = debitNote.getAuthorizationSri().getAccessKey();
    Path xmlPath = Paths.get(storagePath, accessKey + ".xml");

    if (!Files.exists(xmlPath)) {
      throw new IllegalArgumentException("El archivo XML no existe: " + xmlPath);
    }

    return xmlPath;
  }

  @Override
  public boolean sendDebitNoteByEmail(Long idDebitNote, String alternativeEmail) {
    log.info("Enviando ND {} por email", idDebitNote);

    DebitNote debitNote =
        repo.findById(idDebitNote)
            .orElseThrow(
                () -> new IllegalArgumentException("Nota de débito no encontrada: " + idDebitNote));

    if (debitNote.getAuthorizationSri() == null
        || !"AUTORIZADO".equals(debitNote.getAuthorizationSri().getAuthorizationStatusSri())) {
      throw new IllegalArgumentException(
          "Solo se pueden enviar por email notas de débito autorizadas");
    }

    String recipientEmail = alternativeEmail;
    if (!StringUtils.hasText(recipientEmail)) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(debitNote.getIdClient());
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
      Path pdfPath = getPdfFilePath(idDebitNote);
      Path xmlPath = getXmlFilePath(idDebitNote);

      byte[] pdfBytes = Files.readAllBytes(pdfPath);
      byte[] xmlBytes = Files.readAllBytes(xmlPath);

      DebitNoteDto debitNoteDto = serviceMapper.toDebitNoteDto(debitNote);
      enrichDebitNoteDto(debitNoteDto);

      debitNoteEmailService.sendAuthorizedDebitNote(
          recipientEmail,
          debitNote,
          debitNoteDto,
          debitNote.getAuthorizationSri(),
          pdfBytes,
          xmlBytes);

      debitNote.setEmailSent(true);
      debitNote.setEmailSentDate(LocalDateTime.now());
      repo.save(debitNote);

      log.info("Email de ND {} enviado a {}", idDebitNote, recipientEmail);
      return true;

    } catch (Exception e) {
      log.error("Error enviando email de ND: {}", e.getMessage(), e);
      throw new RuntimeException("Error enviando email: " + e.getMessage());
    }
  }

  @Override
  public Page<DebitNoteSearchResponseDto> queryDebitNotes(
      DebitNoteQueryDto queryDto, Pageable pageable) {
    log.info("Querying debit notes with criteria: {}, pageable: {}", queryDto, pageable);

    ClientDto clientDto = null;
    if (StringUtils.hasText(queryDto.getBuyerIdentification())) {
      try {
        ClientDtoResponse clientDtoResponse =
            customerClient.getClientByIdentification(queryDto.getBuyerIdentification());

        if (clientDtoResponse != null
            && !clientDtoResponse.getClientDtos().isEmpty()
            && Objects.nonNull(clientDtoResponse.getClientDtos().get(0))) {
          clientDto = clientDtoResponse.getClientDtos().get(0);
        }
      } catch (Exception e) {
        log.warn(
            "Error loading client by identification {}: {}",
            queryDto.getBuyerIdentification(),
            e.getMessage());
      }
    }

    Long idClient = clientDto != null ? clientDto.getIdClient() : null;

    java.time.LocalDateTime creationDateFrom = null;
    java.time.LocalDateTime creationDateTo = null;
    if (queryDto.getCreationDateFrom() != null) {
      creationDateFrom = queryDto.getCreationDateFrom().atStartOfDay();
    }
    if (queryDto.getCreationDateTo() != null) {
      creationDateTo = queryDto.getCreationDateTo().atTime(23, 59, 59);
    }

    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<DebitNote> debitNotesPage =
        repo.findDebitNotesByCriteria(
            queryDto.getDebitNoteNumber(),
            idClient,
            creationDateFrom,
            creationDateTo,
            queryDto.getReceptionStatusSri(),
            queryDto.getAuthorizationStatusSri(),
            unsortedPageable);

    List<DebitNoteSearchResponseDto> responseDtos =
        debitNotesPage.getContent().stream()
            .map(
                dn -> {
                  String originalInvoiceNumber = dn.getOriginalInvoiceNumber();
                  if (originalInvoiceNumber == null && dn.getOriginalInvoiceId() != null) {
                    Invoice originalInvoice =
                        invoiceRepo.findById(dn.getOriginalInvoiceId()).orElse(null);
                    if (originalInvoice != null) {
                      originalInvoiceNumber = originalInvoice.getInvoiceNumber();
                    }
                  }

                  String buyerName = null;
                  String buyerIdentification = null;
                  if (dn.getIdClient() != null) {
                    try {
                      ClientDtoResponse clientResponse =
                          customerClient.getClientById(dn.getIdClient());
                      if (clientResponse != null
                          && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
                        ClientDto client = clientResponse.getClientDtos().get(0);
                        buyerName = client.getName();
                        buyerIdentification = client.getIdentification();
                      }
                    } catch (Exception e) {
                      log.warn("Error loading client {}: {}", dn.getIdClient(), e.getMessage());
                    }
                  }

                  return DebitNoteSearchResponseDto.builder()
                      .idDebitNote(dn.getIdDebitNote())
                      .debitNoteNumber(dn.getDebitNoteNumber())
                      .creationDate(dn.getCreationDate())
                      .idClient(dn.getIdClient())
                      .nameClient(buyerName)
                      .totalAmount(dn.getTotalAmount())
                      .authorizationNumber(
                          dn.getAuthorizationSri() != null
                              ? dn.getAuthorizationSri().getAuthorizationNumber()
                              : null)
                      .receptionStatusSri(
                          dn.getAuthorizationSri() != null
                              ? dn.getAuthorizationSri().getReceptionStatusSri()
                              : null)
                      .authorizationStatusSri(
                          dn.getAuthorizationSri() != null
                              ? dn.getAuthorizationSri().getAuthorizationStatusSri()
                              : null)
                      .storagePath(
                          dn.getAuthorizationSri() != null
                              ? dn.getAuthorizationSri().getAuthorizationStoragePath()
                              : null)
                      .originalInvoiceId(dn.getOriginalInvoiceId())
                      .originalInvoiceNumber(originalInvoiceNumber)
                      .build();
                })
            .collect(Collectors.toList());

    return new org.springframework.data.domain.PageImpl<>(
        responseDtos, pageable, debitNotesPage.getTotalElements());
  }

  @Override
  public Page<DebitNoteListResponseDto> searchDebitNotesByCompany(
      com.wallparisoft.ebillbilling.dto.DebitNoteSearchByCompanyDto searchDto, Pageable pageable) {
    log.info(
        "🔍 [ND] Buscando notas de débito - idCompany: {}, debitNoteNumber: {}, originalInvoiceNumber: {}, page: {}, size: {}",
        searchDto.getIdCompany(),
        searchDto.getDebitNoteNumber(),
        searchDto.getOriginalInvoiceNumber(),
        pageable.getPageNumber(),
        pageable.getPageSize());

    if (searchDto.getIdCompany() == null) {
      throw new IllegalArgumentException("idCompany es obligatorio");
    }

    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<DebitNote> debitNotesPage =
        repo.findDebitNotesByCompanyAndFilters(
            searchDto.getIdCompany(),
            searchDto.getDebitNoteNumber(),
            searchDto.getOriginalInvoiceNumber(),
            unsortedPageable);

    // Pre-cargar datos de clientes
    Set<Long> uniqueClientIds =
        debitNotesPage.getContent().stream()
            .map(DebitNote::getIdClient)
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
        debitNotesPage.getContent().stream()
            .map(DebitNote::getOriginalInvoiceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, Invoice> invoiceCache = new HashMap<>();
    for (Long invoiceId : invoiceIds) {
      invoiceRepo.findById(invoiceId).ifPresent(inv -> invoiceCache.put(invoiceId, inv));
    }

    // Mapear a DTO
    List<DebitNoteListResponseDto> responseDtos =
        debitNotesPage.getContent().stream()
            .map(
                dn -> {
                  ClientDto clientDto = clientCache.get(dn.getIdClient());
                  String debitNoteStatus = calculateDebitNoteStatus(dn);
                  String originalInvoiceNumberFromCache = dn.getOriginalInvoiceNumber();

                  if (originalInvoiceNumberFromCache == null && dn.getOriginalInvoiceId() != null) {
                    Invoice originalInvoice = invoiceCache.get(dn.getOriginalInvoiceId());
                    if (originalInvoice != null) {
                      originalInvoiceNumberFromCache = originalInvoice.getInvoiceNumber();
                    }
                  }

                  BigDecimal totalAmount = dn.getTotalAmount();
                  if (totalAmount == null) {
                    if (dn.getValorTotal() != null) {
                      totalAmount = dn.getValorTotal();
                    } else if (dn.getDebitNoteDetails() != null
                        && !dn.getDebitNoteDetails().isEmpty()) {
                      BigDecimal subtotal =
                          dn.getDebitNoteDetails().stream()
                              .map(d -> d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO)
                              .reduce(BigDecimal.ZERO, BigDecimal::add);
                      BigDecimal iva =
                          dn.getTotalIva() != null ? dn.getTotalIva() : BigDecimal.ZERO;
                      totalAmount = subtotal.add(iva);
                    }
                  }

                  return DebitNoteListResponseDto.builder()
                      .idDebitNote(dn.getIdDebitNote())
                      .idCompany(dn.getIdCompany())
                      .idClient(dn.getIdClient())
                      .idIssuancePoint(dn.getIdIssuancePoint())
                      .sequential(dn.getSequential())
                      .retryCount(dn.getRetryCount())
                      .issueDate(dn.getIssueDate())
                      .debitNoteNumber(dn.getDebitNoteNumber())
                      .environment(dn.getEnvironment())
                      .additionalInformation(dn.getAdditionalInformation())
                      .totalAmount(totalAmount)
                      .subtotalZero(dn.getSubtotalZero())
                      .subtotalIva(dn.getSubtotalIva())
                      .subtotalIce(dn.getSubtotalIce())
                      .discount(dn.getDiscount())
                      .totalIva(dn.getTotalIva())
                      .totalIce(dn.getTotalIce())
                      .valorModificacion(dn.getValorModificacion())
                      .valorTotal(dn.getValorTotal())
                      .observation(dn.getObservation())
                      .emailSent(dn.getEmailSent())
                      .emailSentDate(dn.getEmailSentDate())
                      .originalInvoiceId(dn.getOriginalInvoiceId())
                      .originalAccessKey(dn.getOriginalAccessKey())
                      .originalInvoiceNumber(originalInvoiceNumberFromCache)
                      .originalIssueDate(dn.getOriginalIssueDate())
                      .documentTypeModified(dn.getDocumentTypeModified())
                      .authorizationSri(null)
                      .debitNoteDetails(null)
                      .creationDate(dn.getCreationDate())
                      .updateDate(dn.getUpdateDate())
                      .status(dn.getStatus())
                      .buyerName(clientDto != null ? clientDto.getName() : null)
                      .buyerIdentification(clientDto != null ? clientDto.getIdentification() : null)
                      .debitNoteStatus(debitNoteStatus)
                      .build();
                })
            .collect(Collectors.toList());

    log.info(
        "✅ [ND] Búsqueda completada - Total: {}, Página: {}, Tamaño: {}",
        debitNotesPage.getTotalElements(),
        debitNotesPage.getNumber(),
        debitNotesPage.getSize());

    return new org.springframework.data.domain.PageImpl<>(
        responseDtos, pageable, debitNotesPage.getTotalElements());
  }

  @Override
  public List<com.wallparisoft.ebillbilling.dto.DebitNoteBasicInfoDto>
      searchAuthorizedActiveDebitNotesByNumberOrAccessKey(
          com.wallparisoft.ebillbilling.dto.DebitNoteSearchByNumberOrAccessKeyDto searchDto) {
    log.info(
        "🔍 [ND] Buscando notas de débito autorizadas y activas - idCompany: {}, debitNoteNumber: {}, accessKey: {}",
        searchDto.getIdCompany(),
        searchDto.getDebitNoteNumber(),
        searchDto.getAccessKey());

    // Validar que se proporcione al menos un criterio de búsqueda
    if (!StringUtils.hasText(searchDto.getDebitNoteNumber())
        && !StringUtils.hasText(searchDto.getAccessKey())) {
      throw new IllegalArgumentException(
          "Se debe proporcionar al menos uno de los siguientes criterios: debitNoteNumber o accessKey");
    }

    // Buscar notas de débito autorizadas y activas
    List<DebitNote> debitNotes =
        repo.findAuthorizedActiveDebitNotesByNumberOrAccessKey(
            searchDto.getIdCompany(), searchDto.getDebitNoteNumber(), searchDto.getAccessKey());

    // Convertir a DTO con productos
    List<com.wallparisoft.ebillbilling.dto.DebitNoteBasicInfoDto> result = new ArrayList<>();
    for (DebitNote debitNote : debitNotes) {
      String accessKey = null;
      String authorizationNumber = null;
      if (debitNote.getAuthorizationSri() != null) {
        accessKey = debitNote.getAuthorizationSri().getAccessKey();
        authorizationNumber = debitNote.getAuthorizationSri().getAuthorizationNumber();
      }

      // Cargar detalles de la nota de débito (si no están cargados)
      List<com.wallparisoft.ebillbilling.entity.DebitNoteDetail> details =
          debitNote.getDebitNoteDetails();
      if (details == null || details.isEmpty()) {
        // Recargar la nota de débito con detalles
        DebitNote debitNoteWithDetails =
            repo.findById(debitNote.getIdDebitNote()).orElse(debitNote);
        details = debitNoteWithDetails.getDebitNoteDetails();
      }

      // Mapear detalles a ProductInfoDto
      List<com.wallparisoft.ebillbilling.dto.ProductInfoDto> products = new ArrayList<>();
      if (details != null && !details.isEmpty()) {
        for (com.wallparisoft.ebillbilling.entity.DebitNoteDetail detail : details) {
          ProductDto productDto = null;
          String mainCode = null;
          String auxiliarCode = null;
          String name = null;

          // Obtener datos del producto
          if (detail.getIdProduct() != null) {
            try {
              com.wallparisoft.ebillbilling.response.ProductResponse productResponse =
                  productClient.getProductById(detail.getIdProduct());
              if (productResponse != null && productResponse.getProductDto() != null) {
                productDto = mapProductDtoFromService(productResponse.getProductDto());
                if (productDto != null) {
                  mainCode = productDto.getMainCode();
                  auxiliarCode = productDto.getAuxiliarCode();
                  name =
                      productDto.getName() != null
                          ? productDto.getName()
                          : productDto.getDescription();
                }
              }
            } catch (Exception e) {
              log.warn(
                  "⚠️ No se pudo cargar el producto {} para ND: {}",
                  detail.getIdProduct(),
                  e.getMessage());
            }
          }

          // Si no se pudo obtener el producto, usar valores por defecto
          if (mainCode == null) {
            mainCode = "N/A";
          }
          if (name == null) {
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
          com.wallparisoft.ebillbilling.dto.DebitNoteBasicInfoDto.builder()
              .accessKey(accessKey)
              .authorizationNumber(authorizationNumber)
              .issueDate(debitNote.getIssueDate())
              .debitNoteNumber(debitNote.getDebitNoteNumber())
              .products(products)
              .build());
    }

    log.info(
        "✅ [ND] Búsqueda completada - Encontradas {} notas de débito autorizadas y activas",
        result.size());

    return result;
  }
}
