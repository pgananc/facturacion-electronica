package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.ContactDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteQueryDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.DigitalSignatureDto;
import com.wallparisoft.ebillbilling.dto.DocumentQueryDto;
import com.wallparisoft.ebillbilling.dto.DocumentSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.dto.InvoiceAvailableForCreditNoteDto;
import com.wallparisoft.ebillbilling.dto.InvoiceBasicInfoDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDraftResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.dto.InvoiceListResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceQueryDto;
import com.wallparisoft.ebillbilling.dto.InvoiceSearchByNumberOrAccessKeyDto;
import com.wallparisoft.ebillbilling.dto.InvoiceSearchDto;
import com.wallparisoft.ebillbilling.dto.InvoiceSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.entity.InvoiceDetail;
import com.wallparisoft.ebillbilling.mapper.InvoiceServiceMapper;
import com.wallparisoft.ebillbilling.repository.ICreditNoteDetailRepo;
import com.wallparisoft.ebillbilling.repository.IInvoiceRepo;
import com.wallparisoft.ebillbilling.response.DigitalSignatureResponse;
import com.wallparisoft.ebillbilling.response.InvoiceResponse;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.DigitalSignatureClient;
import com.wallparisoft.ebillbilling.service.ICreditNoteService;
import com.wallparisoft.ebillbilling.service.IInvoiceService;
import com.wallparisoft.ebillbilling.service.ISendInvoiceService;
import com.wallparisoft.ebillbilling.service.ISubscriptionService;
import com.wallparisoft.ebillbilling.service.InvoiceDeliveryService;
import com.wallparisoft.ebillbilling.service.ProductClient;
import com.wallparisoft.integration.sri.model.Factura;
import com.wallparisoft.integration.sri.util.XmlUtil;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@RequiredArgsConstructor
@Service
@Slf4j
public class InvoiceServiceImpl implements IInvoiceService {

  private final IInvoiceRepo repo;
  private final ISendInvoiceService sendInvoiceService;
  final InvoiceServiceMapper serviceMapper;
  final DigitalSignatureClient digitalSignatureClient;
  final CustomerClient customerClient;
  final ProductClient productClient;
  private final IAuthService authService;
  private final InvoiceDeliveryService invoiceDeliveryService;
  private final ICreditNoteDetailRepo creditNoteDetailRepo;
  private final ICreditNoteService creditNoteService;
  private final ISubscriptionService subscriptionService;
  private final com.wallparisoft.ebillbilling.service.GenericRateLimitService
      genericRateLimitService;

  final @Override public Invoice register(Invoice obj) {
    return repo.save(obj);
  }

  @Override
  public Invoice modify(Invoice obj) {
    throw new UnsupportedOperationException("Use saveInvoice() or processDraftInvoice() instead");
  }

  @Override
  public List<Invoice> list() {
    return repo.findAll();
  }

  @Override
  public List<InvoiceListResponseDto> listWithClientData() {
    List<Invoice> invoices = repo.findAll();

    // Extraer IDs únicos de clientes
    Set<Long> uniqueClientIds =
        invoices.stream()
            .map(Invoice::getIdClient)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // Pre-cargar datos de clientes en un mapa (solo name e identification)
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

    // Mapear Invoice a InvoiceListResponseDto preservando todos los campos + datos del cliente
    return invoices.stream()
        .map(
            invoice -> {
              ClientDto clientDto = clientCache.get(invoice.getIdClient());

              // Calcular monto disponible para NC (solo NC autorizadas descuentan)
              BigDecimal totalCreditNotesApplied = BigDecimal.ZERO;
              if (invoice.getIdInvoice() != null) {
                try {
                  totalCreditNotesApplied =
                      creditNoteDetailRepo.findTotalCreditNotesApplied(invoice.getIdInvoice());
                  if (totalCreditNotesApplied == null) {
                    totalCreditNotesApplied = BigDecimal.ZERO;
                  }
                } catch (Exception e) {
                  log.warn(
                      "Error calculando NC aplicadas para factura {}: {}",
                      invoice.getIdInvoice(),
                      e.getMessage());
                }
              }
              BigDecimal totalAmount =
                  invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
              BigDecimal tip = invoice.getTip() != null ? invoice.getTip() : BigDecimal.ZERO;
              // availableAmount = totalAmount - tip - totalCreditNotesApplied
              // La propina NO es retornable mediante NC, solo los productos
              BigDecimal availableAmount =
                  totalAmount.subtract(tip).subtract(totalCreditNotesApplied);
              // Evitar valores negativos
              if (availableAmount.compareTo(BigDecimal.ZERO) < 0) {
                availableAmount = BigDecimal.ZERO;
              }
              log.info(
                  "📊 Factura {}: total={}, tip={}, NCApplied={}, available={}",
                  invoice.getInvoiceNumber(),
                  totalAmount,
                  tip,
                  totalCreditNotesApplied,
                  availableAmount);

              // Log para ver el cálculo de availableAmount
              log.info(
                  "📄 Invoice ID={}, number={}, totalAmount={}, ncApplied={}, availableAmount={}",
                  invoice.getIdInvoice(),
                  invoice.getInvoiceNumber(),
                  totalAmount,
                  totalCreditNotesApplied,
                  availableAmount);

              return InvoiceListResponseDto.builder()
                  .idInvoice(invoice.getIdInvoice())
                  .idCompany(invoice.getIdCompany())
                  .idClient(invoice.getIdClient())
                  .idIssuancePoint(invoice.getIdIssuancePoint())
                  .sequential(invoice.getSequential())
                  .retryCount(invoice.getRetryCount())
                  .issueDate(invoice.getIssueDate())
                  .invoiceNumber(invoice.getInvoiceNumber())
                  .environment(invoice.getEnvironment())
                  .additionalInformation(invoice.getAdditionalInformation())
                  .totalAmount(invoice.getTotalAmount())
                  .tip(invoice.getTip())
                  .subtotalZero(invoice.getSubtotalZero())
                  .subtotalIva(invoice.getSubtotalIva())
                  .subtotalIce(invoice.getSubtotalIce())
                  .discount(invoice.getDiscount())
                  .emailSent(invoice.getEmailSent())
                  .emailSentDate(invoice.getEmailSentDate())
                  .authorizationSri(invoice.getAuthorizationSri())
                  .invoiceDetails(invoice.getInvoiceDetails())
                  .invoicePayments(invoice.getInvoicePayments())
                  .creationDate(invoice.getCreationDate())
                  .updateDate(invoice.getUpdateDate())
                  .status(invoice.getStatus())
                  // Datos del cliente
                  .clientName(clientDto != null ? clientDto.getName() : null)
                  .clientIdentification(clientDto != null ? clientDto.getIdentification() : null)
                  // Monto disponible para NC
                  .availableAmount(availableAmount)
                  .build();
            })
        .collect(Collectors.toList());
  }

  @Override
  public Invoice getById(Integer id) {
    throw new UnsupportedOperationException("Use getDraftInvoiceById(Long) instead");
  }

  @Override
  public boolean delete(Integer id) {
    throw new UnsupportedOperationException(
        "Use deleteDraft(Long) instead - only draft invoices can be deleted");
  }

  @Override
  public InvoiceResponse saveInvoice(InvoiceDto invoiceDto) {
    // ✅ VALIDACIÓN DE SUSCRIPCIÓN: Verificar si puede emitir factura
    try {
      subscriptionService.validateCanEmit(invoiceDto.getIdCompany(), "INVOICE");
      log.info("✅ Validación de suscripción exitosa para company {}", invoiceDto.getIdCompany());
    } catch (IllegalStateException e) {
      log.error("❌ Validación de suscripción falló: {}", e.getMessage());
      return InvoiceResponse.builder()
          .code(HttpStatus.FORBIDDEN.value())
          .status(HttpStatus.FORBIDDEN.name())
          .message(e.getMessage())
          .build();
    }

    Invoice invoice = serviceMapper.toInvoice(invoiceDto);
    InvoiceResponse response =
        InvoiceResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    // Validate and assign sequential number based on issuance point
    if (Objects.nonNull(invoiceDto.getEstablishment())
        && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint())
        && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint().getIdIssuancePoint())) {

      Long idIssuancePoint = invoiceDto.getEstablishment().getIssuancePoint().getIdIssuancePoint();

      // Get the next sequential for this issuance point
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;

      // Set the sequential and issuance point ID in the invoice
      invoice.setIdIssuancePoint(idIssuancePoint);
      invoice.setSequential(nextSequential);
      invoice.setRetryCount(0); // Inicializar contador de reintentos
      invoiceDto.setIdIssuancePoint(idIssuancePoint);
      invoiceDto.setSequential(nextSequential);

      // Format sequential as a 9-digit string for the issuancePoint DTO (for SRI)
      String formattedSequential = String.format("%09d", nextSequential);
      invoiceDto.getEstablishment().getIssuancePoint().setSequentialInvoice(formattedSequential);

      log.info("Assigned sequential {} for issuance point {}", nextSequential, idIssuancePoint);
    } else {
      log.error("Issuance point ID is required to generate invoice sequential");
      response.setCode(HttpStatus.BAD_REQUEST.value());
      response.setStatus(HttpStatus.BAD_REQUEST.name());
      response.setMessage(
          "El ID del punto de emisión es requerido para generar el secuencial de la factura.");
      return response;
    }

    try {
      // Obtener ClientDto completo con contactos antes de enviar al SRI
      ClientDto clientDtoWithContacts = getClientDtoWithContacts(invoiceDto);

      DigitalSignatureResponse digitalSignatureResponse =
          digitalSignatureClient.getDigitalSignatureByCompanyId(
              invoiceDto.getCompanyDto().getIdCompany());
      if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
        log.error("There is no digital signature configuration");
        response = serviceMapper.toInvoiceResponse(register(invoice));
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        response.setMessage(
            "La firma digital caduca y/o no esta configurada comuníquese con el administrador, la factura se registro en borrador.");
        return response;
      }
      // Para borradores procesados manualmente, no hay AuthorizationSri existente
      AuthorizationSri authorizationSri =
          sendInvoiceService.sendInvoiceSri(
              invoiceDto, digitalSignatureResponse.getDigitalSignatureDto(), null);
      invoice.setAuthorizationSri(authorizationSri);
      // Actualizar invoiceNumber desde el XML de autorización si está AUTORIZADO
      setInvoiceNumberFromAuthorizationXml(invoice, authorizationSri);
      Invoice savedInvoice = register(invoice);
      response = serviceMapper.toInvoiceResponse(savedInvoice);

      response.setCode(HttpStatus.OK.value());
      response.setStatus(HttpStatus.OK.name());

      // Pasar el ClientDto ya obtenido para evitar consulta duplicada
      notifyAuthorizedInvoice(invoiceDto, savedInvoice, authorizationSri, clientDtoWithContacts);

      // Sincronizar secuencial con ebill-customer después de autorización exitosa
      if (Objects.nonNull(invoiceDto.getEstablishment())
          && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint())
          && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint().getCode())
          && Objects.nonNull(invoiceDto.getEstablishment().getIdEstablishment())) {

        try {
          syncSequentialToCustomer(
              invoiceDto.getEstablishment().getIssuancePoint().getCode(),
              invoiceDto.getEstablishment().getIdEstablishment(),
              savedInvoice.getSequential());
        } catch (Exception e) {
          // Log el error pero NO afectar la respuesta de la factura
          log.error(
              "❌❌❌ FALLO FINAL al sincronizar secuencial {} después de 3 intentos. "
                  + "Factura guardada correctamente pero secuencial DESINCRONIZADO con ebill-customer. "
                  + "Sincronización manual requerida: PATCH /api/invoice-issuance-points/{}/sequential?establishmentId={}",
              savedInvoice.getSequential(),
              invoiceDto.getEstablishment().getIssuancePoint().getCode(),
              invoiceDto.getEstablishment().getIdEstablishment());
        }
      }

    } catch (ResourceAccessException | feign.RetryableException e) {
      response = serviceMapper.toInvoiceResponse(register(invoice));
      response.setCode(HttpStatus.BAD_REQUEST.value());
      response.setStatus(HttpStatus.BAD_REQUEST.name());
      response.setMessage(
          "Error al obtener la firma digital, comuníquese con el administrador, la factura se registro en borrador.");
    } catch (CertificateException
        | SAXException
        | IOException
        | JAXBException
        | InterruptedException e) {

      response = serviceMapper.toInvoiceResponse(register(invoice));
      response.setCode(HttpStatus.BAD_REQUEST.value());
      response.setStatus(HttpStatus.BAD_REQUEST.name());
      response.setMessage(
          "Hubo problemas para conectarse con el SRI, la factura se registro en borrador.");
    }

    return response;
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void executeSendInvoiceToSri() {
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
      List<Invoice> invoices =
          this.repo.findInvoicesExcludingSpecificSriStatuses("RECIBIDA", "AUTORIZADO");
      log.info("Found {} invoices pending to send to SRI", invoices.size());

      // ===== OPTIMIZACIÓN: Pre-cargar datos únicos para evitar consultas duplicadas =====
      // 1. Extraer IDs únicos de companies, clients, issuance points y products
      Set<Long> uniqueCompanyIds =
          invoices.stream()
              .map(Invoice::getIdCompany)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      Set<Long> uniqueClientIds =
          invoices.stream()
              .map(Invoice::getIdClient)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      Set<Long> uniqueIssuancePointIds =
          invoices.stream()
              .map(Invoice::getIdIssuancePoint)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      Set<Long> uniqueProductIds =
          invoices.stream()
              .filter(inv -> Objects.nonNull(inv.getInvoiceDetails()))
              .flatMap(inv -> inv.getInvoiceDetails().stream())
              .map(com.wallparisoft.ebillbilling.entity.InvoiceDetail::getIdProduct)
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

      // 7. Procesar facturas usando el cache
      invoices.forEach(
          invoice -> {
            // Inicializar retryCount si es null
            if (Objects.isNull(invoice.getRetryCount())) {
              invoice.setRetryCount(0);
            }

            // Verificar si ya se agotaron los reintentos
            if (invoice.getRetryCount() >= 5) {
              log.warn(
                  "Invoice {} has exceeded maximum retries ({}), marking as inactive",
                  invoice.getIdInvoice(),
                  invoice.getRetryCount());
              deactivateInvoiceAndDetails(invoice);
              return;
            }

            try {
              // Usar cache en lugar de consultar cada vez
              DigitalSignatureDto digitalSignatureDto =
                  digitalSignatureCache.get(invoice.getIdCompany());

              if (Objects.isNull(digitalSignatureDto)) {
                log.warn(
                    "Digital signature not found in cache for company {} - Invoice {}",
                    invoice.getIdCompany(),
                    invoice.getIdInvoice());
                handleInvoiceFailure(invoice, "Digital signature not found");
                return;
              }

              InvoiceDto invoiceDtoForSri = serviceMapper.toInvoiceDto(invoice);

              // Usar cache para compañía si está disponible
              CompanyDto companyDtoFromCache = companyCache.get(invoice.getIdCompany());
              if (Objects.nonNull(companyDtoFromCache)) {
                invoiceDtoForSri.setCompanyDto(companyDtoFromCache);
                log.debug("Assigned company to invoice {} from cache", invoice.getIdInvoice());
              } else {
                log.warn(
                    "Company not found in cache for company {} - Invoice {}",
                    invoice.getIdCompany(),
                    invoice.getIdInvoice());
                handleInvoiceFailure(invoice, "Company not found");
                return;
              }

              // Usar cache para cliente si está disponible
              ClientDto clientDtoFromCache = clientCache.get(invoice.getIdClient());
              if (Objects.nonNull(clientDtoFromCache)) {
                invoiceDtoForSri.setClientDto(clientDtoFromCache);
              }

              // Usar cache para establecimiento si está disponible
              if (Objects.nonNull(invoice.getIdIssuancePoint())) {
                EstablishmentDto establishmentDto =
                    establishmentCache.get(invoice.getIdIssuancePoint());
                if (Objects.nonNull(establishmentDto)) {
                  invoiceDtoForSri.setEstablishment(establishmentDto);
                  // Actualizar el sequential en el issuancePoint si está disponible en la factura
                  if (Objects.nonNull(invoice.getSequential())
                      && Objects.nonNull(establishmentDto.getIssuancePoint())) {
                    String formattedSequential = String.format("%09d", invoice.getSequential());
                    establishmentDto.getIssuancePoint().setSequentialInvoice(formattedSequential);
                  }
                  log.debug(
                      "Assigned establishment to invoice {} from cache", invoice.getIdInvoice());
                } else {
                  log.warn(
                      "Establishment not found in cache for issuance point {} - Invoice {}",
                      invoice.getIdIssuancePoint(),
                      invoice.getIdInvoice());
                  handleInvoiceFailure(invoice, "Establishment not found");
                  return;
                }
              } else {
                log.warn(
                    "Invoice {} does not have idIssuancePoint - cannot load establishment",
                    invoice.getIdInvoice());
                handleInvoiceFailure(invoice, "Invoice does not have issuance point");
                return;
              }

              // Asignar ProductDto a cada InvoiceDetailDto desde el cache
              if (Objects.nonNull(invoiceDtoForSri.getInvoiceDetails())) {
                for (InvoiceDetailDto detailDto : invoiceDtoForSri.getInvoiceDetails()) {
                  if (Objects.nonNull(detailDto.getIdProduct())) {
                    ProductDto productDto = productCache.get(detailDto.getIdProduct());
                    if (Objects.nonNull(productDto)) {
                      detailDto.setProductDto(productDto);
                      log.debug(
                          "Assigned product {} to invoice detail {} from cache",
                          detailDto.getIdProduct(),
                          detailDto.getIdInvoiceDetail());
                    } else {
                      log.warn(
                          "Product not found in cache for productId {} - Invoice {} - Detail {}",
                          detailDto.getIdProduct(),
                          invoice.getIdInvoice(),
                          detailDto.getIdInvoiceDetail());
                      handleInvoiceFailure(invoice, "Product not found for detail");
                      return;
                    }
                  } else {
                    log.warn(
                        "Invoice detail {} does not have idProduct - Invoice {}",
                        detailDto.getIdInvoiceDetail(),
                        invoice.getIdInvoice());
                    handleInvoiceFailure(invoice, "Invoice detail does not have product");
                    return;
                  }
                }
              }

              // Obtener ClientDto completo con contactos (puede usar cache si ya está completo)
              ClientDto clientDtoWithContacts = getClientDtoWithContacts(invoiceDtoForSri);

              // Reutilizar AuthorizationSri existente si existe, para evitar crear registros
              // duplicados
              AuthorizationSri existingAuthorizationSri = invoice.getAuthorizationSri();
              AuthorizationSri authorizationSri =
                  sendInvoiceService.sendInvoiceSri(
                      invoiceDtoForSri, digitalSignatureDto, existingAuthorizationSri);
              invoice.setAuthorizationSri(authorizationSri);
              // Actualizar invoiceNumber desde el XML de autorización si está AUTORIZADO
              setInvoiceNumberFromAuthorizationXml(invoice, authorizationSri);

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
                repo.save(invoice);
                // Pasar el ClientDto ya obtenido para evitar consulta duplicada
                notifyAuthorizedInvoice(
                    invoiceDtoForSri, invoice, authorizationSri, clientDtoWithContacts);
                log.info("Successfully processed invoice {}", invoice.getIdInvoice());
              } else {
                // Si no fue exitoso, incrementar el contador de reintentos
                String failureReason =
                    buildFailureReason(authorizationSri, isAuthorized, isReceived);
                handleInvoiceFailure(invoice, failureReason);
              }
            } catch (feign.FeignException.Unauthorized e) {
              log.error(
                  "Unauthorized access to digital signature service for invoice {} (retry {}/5) - Scheduled tasks need authentication configuration",
                  invoice.getIdInvoice(),
                  invoice.getRetryCount());
              handleInvoiceFailure(invoice, "Unauthorized access to digital signature service");
            } catch (ResourceAccessException | feign.RetryableException e) {
              log.error(
                  "Connection error to digital signature service for invoice {} (retry {}/5): {}",
                  invoice.getIdInvoice(),
                  invoice.getRetryCount(),
                  e.getMessage());
              handleInvoiceFailure(invoice, "Connection error: " + e.getMessage());
            } catch (CertificateException
                | SAXException
                | IOException
                | JAXBException
                | InterruptedException e) {
              log.error(
                  "Process error for invoice {} (retry {}/5): {}",
                  invoice.getIdInvoice(),
                  invoice.getRetryCount(),
                  e.getMessage(),
                  e);
              handleInvoiceFailure(invoice, "Process error: " + e.getMessage());
            } catch (Exception e) {
              log.error(
                  "Unexpected error processing invoice {} (retry {}/5): {}",
                  invoice.getIdInvoice(),
                  invoice.getRetryCount(),
                  e.getMessage(),
                  e);
              handleInvoiceFailure(invoice, "Unexpected error: " + e.getMessage());
            }
          });
    } finally {
      // Restaurar el contexto de seguridad anterior
      SecurityContextHolder.setContext(previousContext);
      log.debug("Restored previous security context after scheduled task execution");
    }
  }

  /**
   * Maneja el fallo de una factura incrementando el contador de reintentos y marcándola como
   * inactiva si se agotan los reintentos
   */
  private void handleInvoiceFailure(Invoice invoice, String reason) {
    invoice.setRetryCount(invoice.getRetryCount() + 1);

    if (invoice.getRetryCount() >= 5) {
      log.error(
          "Invoice {} has exceeded maximum retries ({}) - Marking as inactive. Reason: {}",
          invoice.getIdInvoice(),
          invoice.getRetryCount(),
          reason);
      deactivateInvoiceAndDetails(invoice);
    } else {
      log.warn(
          "Invoice {} retry {}/5. Reason: {}",
          invoice.getIdInvoice(),
          invoice.getRetryCount(),
          reason);
      repo.save(invoice);
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

  /** Marca la factura y todos sus detalles como inactivos (status = false) */
  private void deactivateInvoiceAndDetails(Invoice invoice) {
    invoice.setStatus(false);

    // Marcar todos los detalles como inactivos
    if (Objects.nonNull(invoice.getInvoiceDetails())) {
      invoice.getInvoiceDetails().forEach(detail -> detail.setStatus(false));
    }

    repo.save(invoice);
    log.info(
        "Invoice {} and its {} details marked as inactive",
        invoice.getIdInvoice(),
        Objects.nonNull(invoice.getInvoiceDetails()) ? invoice.getInvoiceDetails().size() : 0);
  }

  @Override
  public Page<InvoiceSearchResponseDto> searchInvoices(
      InvoiceSearchDto searchDto, Pageable pageable) {
    log.info("Searching invoices with criteria: {}, pageable: {}", searchDto, pageable);

    Page<Invoice> invoices =
        repo.findInvoicesByCriteria(
            searchDto.getInvoiceNumber(),
            searchDto.getIdClient(),
            searchDto.getCreationDateFrom(),
            searchDto.getCreationDateTo(),
            searchDto.getReceptionStatusSri(),
            searchDto.getAuthorizationStatusSri(),
            pageable);

    return invoices.map(this::mapToSearchResponseDto);
  }

  private InvoiceSearchResponseDto mapToSearchResponseDto(Invoice invoice) {
    return InvoiceSearchResponseDto.builder()
        .idInvoice(invoice.getIdInvoice())
        .invoiceNumber(invoice.getInvoiceNumber())
        .creationDate(invoice.getCreationDate())
        .idClient(invoice.getIdClient())
        .totalAmount(invoice.getTotalAmount())
        .receptionStatusSri(
            Objects.nonNull(invoice.getAuthorizationSri())
                ? invoice.getAuthorizationSri().getReceptionStatusSri()
                : null)
        .authorizationStatusSri(
            Objects.nonNull(invoice.getAuthorizationSri())
                ? invoice.getAuthorizationSri().getAuthorizationStatusSri()
                : null)
        .storagePath(
            Objects.nonNull(invoice.getAuthorizationSri())
                ? invoice.getAuthorizationSri().getAuthorizationStoragePath()
                : null)
        .authorizationNumber(
            Objects.nonNull(invoice.getAuthorizationSri())
                ? invoice.getAuthorizationSri().getAuthorizationNumber()
                : null)
        .build();
  }

  @Override
  public Page<InvoiceSearchResponseDto> queryInvoices(
      InvoiceQueryDto queryDto,
      Pageable pageable,
      jakarta.servlet.http.HttpServletRequest request) {

    // Extraer IP del cliente
    String clientIp = extractClientIp(request);

    // Extraer token del request
    String token = extractToken(request);

    // 1. Validar rate limiting por TOKEN
    if (token != null && !token.isBlank()) {
      if (!genericRateLimitService.canMakeRequest(
          "INVOICE_QUERY",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.TOKEN,
          token)) {

        long secondsLeft =
            genericRateLimitService.getTimeUntilNextAttempt(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN,
                token);
        long minutesLeft = secondsLeft / 60;
        int currentAttempts =
            genericRateLimitService.getCurrentAttempts(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN,
                token);
        int maxAttempts =
            genericRateLimitService.getMaxRequests(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN);
        int windowMinutes =
            genericRateLimitService.getWindowMinutes(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN);

        String message =
            String.format(
                "Máximo de solicitudes alcanzado por token (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);

        throw new com.wallparisoft.ebillbilling.exception.RateLimitExceededException(
            message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
      }
    }

    // 2. Validar rate limiting por IP
    if (clientIp != null && !clientIp.isBlank()) {
      if (!genericRateLimitService.canMakeRequest(
          "INVOICE_QUERY",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
          clientIp)) {

        long secondsLeft =
            genericRateLimitService.getTimeUntilNextAttempt(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
                clientIp);
        long minutesLeft = secondsLeft / 60;
        int currentAttempts =
            genericRateLimitService.getCurrentAttempts(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
                clientIp);
        int maxAttempts =
            genericRateLimitService.getMaxRequests(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .IP);
        int windowMinutes =
            genericRateLimitService.getWindowMinutes(
                "INVOICE_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .IP);

        String message =
            String.format(
                "Máximo de solicitudes alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);

        throw new com.wallparisoft.ebillbilling.exception.RateLimitExceededException(
            message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
      }
    }

    log.info("Querying invoices with criteria: {}, pageable: {}", queryDto, pageable);
    Page<InvoiceSearchResponseDto> resultTotal = Page.empty();
    if (StringUtils.hasText(queryDto.getBuyerIdentification())) {

      try {
        ClientDtoResponse clientDtoResponse =
            customerClient.getClientByIdentification(queryDto.getBuyerIdentification());

        if (clientDtoResponse != null && !clientDtoResponse.getClientDtos().isEmpty()) {
          List<InvoiceSearchResponseDto> content = new ArrayList<>();
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
            InvoiceSearchDto searchDto =
                InvoiceSearchDto.builder()
                    .invoiceNumber(queryDto.getInvoiceNumber())
                    .idClient(clientDto != null ? clientDto.getIdClient() : null)
                    .creationDateFrom(creationDateFrom)
                    .creationDateTo(creationDateTo)
                    .receptionStatusSri(queryDto.getReceptionStatusSri())
                    .authorizationStatusSri(queryDto.getAuthorizationStatusSri())
                    .build();

            Page<InvoiceSearchResponseDto> result = searchInvoices(searchDto, pageable);

            if (clientDto != null) {
              for (InvoiceSearchResponseDto invoice : result) {
                invoice.setNameClient(clientDto.getName());
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

    // 3. Incrementar contadores SOLO si fue exitoso
    if (token != null && !token.isBlank()) {
      genericRateLimitService.incrementRequest(
          "INVOICE_QUERY",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.TOKEN,
          token);
    }
    if (clientIp != null && !clientIp.isBlank()) {
      genericRateLimitService.incrementRequest(
          "INVOICE_QUERY",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
          clientIp);
    }

    return resultTotal;
  }

  @Override
  public InvoiceResponse saveInvoiceDraft(InvoiceDto invoiceDto) {
    log.info("Saving invoice as draft for company {}", invoiceDto.getIdCompany());

    Invoice invoice = serviceMapper.toInvoice(invoiceDto);

    // FORZAR que los borradores NO tengan secuencial ni punto de emisión
    // Esto garantiza que sean borradores sin importar lo que venga en el DTO
    invoice.setSequential(null);
    invoice.setIdIssuancePoint(null);
    invoice.setRetryCount(0);
    invoice.setAuthorizationSri(null); // Indicador de borrador

    log.info(
        "Draft invoice: sequential={}, idIssuancePoint={}, authorizationSri={}",
        invoice.getSequential(),
        invoice.getIdIssuancePoint(),
        invoice.getAuthorizationSri());

    // Save invoice without sequential, without digital signature, and without
    // sending to SRI
    Invoice savedInvoice = register(invoice);

    InvoiceResponse response = serviceMapper.toInvoiceResponse(savedInvoice);
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    response.setMessage("Factura guardada como borrador exitosamente");

    log.info("✅ Draft invoice saved successfully with ID: {}", savedInvoice.getIdInvoice());

    return response;
  }

  @Override
  public List<InvoiceDraftResponseDto> getDraftInvoices() {
    log.info("Fetching all draft invoices (authorizationSri is null)");
    List<Invoice> drafts = repo.findByAuthorizationSriIsNull();
    log.info("Found {} draft invoices", drafts.size());

    // Map to DTO to avoid circular reference issues
    return drafts.stream().map(this::mapToDraftResponseDto).toList();
  }

  private InvoiceDraftResponseDto mapToDraftResponseDto(Invoice invoice) {
    InvoiceDraftResponseDto.InvoiceDraftResponseDtoBuilder builder =
        InvoiceDraftResponseDto.builder()
            .idInvoice(invoice.getIdInvoice())
            .idCompany(invoice.getIdCompany())
            .idClient(invoice.getIdClient())
            .issueDate(invoice.getIssueDate())
            .creationDate(invoice.getCreationDate())
            .invoiceNumber(invoice.getInvoiceNumber())
            .environment(invoice.getEnvironment())
            .additionalInformation(invoice.getAdditionalInformation())
            .totalAmount(invoice.getTotalAmount())
            .tip(invoice.getTip())
            .subtotalZero(invoice.getSubtotalZero())
            .subtotalIva(invoice.getSubtotalIva())
            .subtotalIce(invoice.getSubtotalIce())
            .discount(invoice.getDiscount())
            .retryCount(invoice.getRetryCount())
            .paymentMethodName(
                invoice.getInvoicePayments() != null
                        && !invoice.getInvoicePayments().isEmpty()
                        && invoice.getInvoicePayments().get(0).getPaymentMethod() != null
                    ? invoice.getInvoicePayments().get(0).getPaymentMethod().getPaymentMethodType()
                    : null)
            .status(invoice.getStatus());

    // Mapear datos del cliente desde microservicio ebill-customer
    if (Objects.nonNull(invoice.getIdClient())) {
      try {
        ClientDtoResponse response = customerClient.getClientById(invoice.getIdClient());

        // El endpoint retorna una lista de ClientDto, tomamos el primero
        if (Objects.nonNull(response) && !CollectionUtils.isEmpty(response.getClientDtos())) {
          ClientDto clientDto = response.getClientDtos().get(0);

          builder
              .buyerIdentification(clientDto.getIdentification())
              .buyerIdType(clientDto.getIdType())
              .buyerName(clientDto.getName())
              .clientDto(clientDto);

          // Nota: address, phone y mail están en ContactDto dentro de ClientDto
          // Se pueden mapear si es necesario
          log.debug(
              "✅ Cliente mapeado en lista: {} ({})",
              clientDto.getName(),
              clientDto.getIdentification());
        } else {
          log.warn("⚠️ Cliente {} no encontrado en ebill-customer", invoice.getIdClient());
        }
      } catch (Exception e) {
        log.warn(
            "⚠️ No se pudo cargar cliente {} para lista de borradores: {}",
            invoice.getIdClient(),
            e.getMessage());
        // No agregar datos del cliente si falla
      }
    }

    return builder.build();
  }

  @Override
  public InvoiceDto getDraftInvoiceById(Long idInvoice) {
    log.info("📝 Buscando borrador con ID: {}", idInvoice);

    // 1. Buscar la factura por ID
    Invoice invoice =
        repo.findById(idInvoice)
            .orElseThrow(
                () -> {
                  log.error("❌ Borrador con ID {} no encontrado", idInvoice);
                  return new IllegalArgumentException(
                      "Borrador no encontrado con ID: " + idInvoice);
                });

    // 2. Verificar que sea un borrador (authorizationSri == null)
    if (Objects.nonNull(invoice.getAuthorizationSri())) {
      log.warn("⚠️ La factura {} ya fue procesada", idInvoice);
      throw new IllegalArgumentException(
          "Esta factura ya fue procesada y autorizada por el SRI. No es un borrador editable.");
    }

    log.info("✅ Borrador encontrado. Total: ${}", invoice.getTotalAmount());

    // 3. Construir el DTO de respuesta
    InvoiceDto dto = new InvoiceDto();

    // === Datos básicos ===
    dto.setIdInvoice(invoice.getIdInvoice());
    dto.setIdCompany(invoice.getIdCompany());
    dto.setTotalAmount(invoice.getTotalAmount());
    dto.setDiscount(invoice.getDiscount());
    dto.setTip(invoice.getTip());
    dto.setSubtotalZero(invoice.getSubtotalZero());
    dto.setSubtotalIva(invoice.getSubtotalIva());
    dto.setSubtotalIce(invoice.getSubtotalIce());
    dto.setStatus("DRAFT");
    dto.setIssueDate(invoice.getIssueDate().toLocalDate());
    dto.setCreationDate(invoice.getCreationDate());
    dto.setAdditionalInformation(invoice.getAdditionalInformation());
    dto.setEnvironment(invoice.getEnvironment());

    // === Datos del cliente desde microservicio ebill-customer ===
    log.info("🔍 idClient de la factura: {}", invoice.getIdClient());

    if (Objects.nonNull(invoice.getIdClient())) {
      try {
        log.info("📞 Llamando a customerClient.getClientById({})", invoice.getIdClient());

        ClientDtoResponse response = customerClient.getClientById(invoice.getIdClient());

        log.info("✅ Respuesta del microservicio cliente: {}", response);

        // El endpoint retorna una lista de ClientDto, tomamos el primero
        if (Objects.nonNull(response) && !CollectionUtils.isEmpty(response.getClientDtos())) {
          ClientDto clientDto = response.getClientDtos().get(0);

          log.info("   - idClient: {}", clientDto.getIdClient());
          log.info("   - identification: {}", clientDto.getIdentification());
          log.info("   - name: {}", clientDto.getName());
          log.info("   - idType: {}", clientDto.getIdType());

          dto.setIdClient(clientDto.getIdClient());
          dto.setBuyerIdentification(clientDto.getIdentification());
          dto.setBuyerIdType(clientDto.getIdType());
          dto.setBuyerName(clientDto.getName());
          dto.setClientDto(clientDto);

          log.info("✅ DTO después de setear cliente:");
          log.info("   - dto.getIdClient(): {}", dto.getIdClient());
          log.info("   - dto.getBuyerIdentification(): {}", dto.getBuyerIdentification());
          log.info("   - dto.getBuyerName(): {}", dto.getBuyerName());
          log.info("   - dto.getClientDto(): {}", dto.getClientDto());
        } else {
          log.warn(
              "⚠️ Cliente {} no encontrado en ebill-customer (respuesta vacía)",
              invoice.getIdClient());

          // FALLBACK: Al menos setear el idClient aunque no tengamos los datos completos
          dto.setIdClient(invoice.getIdClient());
          dto.setBuyerName("Cliente ID: " + invoice.getIdClient() + " (no encontrado)");
          dto.setBuyerIdentification("N/A");

          ClientDto emptyClientDto = new ClientDto();
          emptyClientDto.setIdClient(invoice.getIdClient());
          dto.setClientDto(emptyClientDto);
        }

      } catch (Exception e) {
        log.error("❌ ERROR al cargar el cliente {}: {}", invoice.getIdClient(), e.getMessage(), e);
        log.error("   Tipo de excepción: {}", e.getClass().getName());
        log.error("   Stack trace completo:", e);

        // FALLBACK: Al menos setear el idClient aunque no tengamos los datos completos
        dto.setIdClient(invoice.getIdClient());
        dto.setBuyerName("Cliente ID: " + invoice.getIdClient() + " (datos no disponibles)");
        dto.setBuyerIdentification("N/A");

        ClientDto emptyClientDto = new ClientDto();
        emptyClientDto.setIdClient(invoice.getIdClient());
        dto.setClientDto(emptyClientDto);

        log.warn("⚠️ Usando datos de fallback para el cliente");
      }
    } else {
      log.warn("⚠️ La factura NO tiene idClient asignado");
      dto.setClientDto(new ClientDto());
    }

    // === Detalles de productos ===
    List<InvoiceDetailDto> detailsDto = new ArrayList<>();

    if (Objects.nonNull(invoice.getInvoiceDetails())) {
      for (InvoiceDetail detail : invoice.getInvoiceDetails()) {
        InvoiceDetailDto detailDto = new InvoiceDetailDto();

        detailDto.setIdInvoiceDetail(detail.getIdInvoiceDetail());
        detailDto.setIdProduct(detail.getIdProduct());
        detailDto.setUnitPrice(detail.getUnitPrice());
        detailDto.setDiscount(detail.getDiscount());
        detailDto.setSubtotal(detail.getSubtotal());
        detailDto.setQuantity(detail.getQuantity());

        // Cargar datos del producto desde microservicio ebill-product
        if (Objects.nonNull(detail.getIdProduct())) {
          try {
            com.wallparisoft.ebillbilling.response.ProductResponse productResponse =
                productClient.getProductById(detail.getIdProduct());
            if (Objects.isNull(productResponse)
                || Objects.isNull(productResponse.getProductDto())) {
              log.warn("Product not found for productId {}", detail.getIdProduct());
              continue;
            }
            // Mapear ProductDto del servicio de productos al ProductDto de ebill-billing
            ProductDto productDto = mapProductDtoFromService(productResponse.getProductDto());

            detailDto.setCode(productDto.getMainCode());
            detailDto.setDescription(productDto.getName());
            detailDto.setProductDto(productDto);

            // Calcular IVA percentage si existe rate
            // El rate ya viene como porcentaje (15.00)
            if (Objects.nonNull(productDto.getRate())) {
              detailDto.setIvaPercentage(productDto.getRate());
            } else {
              detailDto.setIvaPercentage(BigDecimal.ZERO);
            }
          } catch (Exception e) {
            log.warn(
                "⚠️ No se pudo cargar el producto {}: {}", detail.getIdProduct(), e.getMessage());
            // Inicializar con valores vacíos
            detailDto.setProductDto(new ProductDto());
          }
        }

        detailsDto.add(detailDto);
      }
    }

    dto.setInvoiceDetails(detailsDto);
    dto.setDetails(detailsDto); // Alias para compatibilidad
    log.info("📦 {} productos cargados", detailsDto.size());

    // Inicializar otros campos @Nonnull para evitar NPE
    if (Objects.isNull(dto.getCompanyDto())) {
      dto.setCompanyDto(new CompanyDto());
    }
    if (Objects.isNull(dto.getProductDto())) {
      dto.setProductDto(new ProductDto());
    }

    log.info("✅ Borrador completo retornado para edición (ID: {})", idInvoice);
    log.info("📤 DTO FINAL que se retorna:");
    log.info("   - idInvoice: {}", dto.getIdInvoice());
    log.info("   - idClient: {}", dto.getIdClient());
    log.info("   - buyerName: {}", dto.getBuyerName());
    log.info("   - buyerIdentification: {}", dto.getBuyerIdentification());
    log.info("   - clientDto: {}", dto.getClientDto());
    log.info(
        "   - details.size(): {}", Objects.nonNull(dto.getDetails()) ? dto.getDetails().size() : 0);
    log.info(
        "   - payments.size(): {}",
        Objects.nonNull(dto.getPaymentMethods()) ? dto.getPaymentMethods().size() : 0);
    log.info("   - totalAmount: {}", dto.getTotalAmount());

    return dto;
  }

  @Override
  public InvoiceDto getInvoiceById(Long idInvoice) {
    log.info("📝 Buscando factura con ID: {}", idInvoice);

    // 1. Buscar la factura por ID
    Invoice invoice =
        repo.findById(idInvoice)
            .orElseThrow(
                () -> {
                  log.error("❌ Factura con ID {} no encontrada", idInvoice);
                  return new IllegalArgumentException("Factura no encontrada con ID: " + idInvoice);
                });

    // 2. Determinar el status
    String status = Objects.isNull(invoice.getAuthorizationSri()) ? "DRAFT" : "AUTORIZADO";

    log.info("✅ Factura encontrada. Status: {}, Total: ${}", status, invoice.getTotalAmount());

    // 3. Convertir a DTO usando el mapper
    InvoiceDto dto = serviceMapper.toInvoiceDto(invoice);

    // 4. Establecer status correcto
    dto.setStatus(status);

    // 5. Establecer IDs en los DTOs antes de enriquecer (si es necesario)
    if (dto.getCompanyDto() != null && invoice.getIdCompany() != null) {
      dto.getCompanyDto().setIdCompany(invoice.getIdCompany());
    }
    if (dto.getClientDto() != null && invoice.getIdClient() != null) {
      dto.getClientDto().setIdClient(invoice.getIdClient());
    }

    // 6. Enriquecer con datos completos (cliente, compañía, establecimiento, productos)
    // El mapper ya debería haber mapeado los datos básicos, pero podemos enriquecer si es necesario
    if (invoice.getIdClient() != null) {
      try {
        ClientDtoResponse response = customerClient.getClientById(invoice.getIdClient());
        if (Objects.nonNull(response) && !CollectionUtils.isEmpty(response.getClientDtos())) {
          ClientDto clientDto = response.getClientDtos().get(0);
          dto.setIdClient(clientDto.getIdClient());
          dto.setBuyerIdentification(clientDto.getIdentification());
          dto.setBuyerIdType(clientDto.getIdType());
          dto.setBuyerName(clientDto.getName());
          dto.setClientDto(clientDto);
        }
      } catch (Exception e) {
        log.warn("⚠️ No se pudo cargar el cliente {}: {}", invoice.getIdClient(), e.getMessage());
      }
    }

    // Enriquecer con datos de compañía
    if (invoice.getIdCompany() != null) {
      try {
        CompanyDto companyDto = customerClient.getCompanyById(invoice.getIdCompany());
        if (companyDto != null) {
          dto.setCompanyDto(companyDto);
        }
      } catch (Exception e) {
        log.warn("⚠️ No se pudo cargar la compañía {}: {}", invoice.getIdCompany(), e.getMessage());
      }
    }

    // Enriquecer detalles con productos si no están completos
    if (dto.getInvoiceDetails() != null) {
      for (InvoiceDetailDto detail : dto.getInvoiceDetails()) {
        if (detail.getProductDto() == null && detail.getIdProduct() != null) {
          try {
            com.wallparisoft.ebillbilling.response.ProductResponse productResponse =
                productClient.getProductById(detail.getIdProduct());
            if (Objects.nonNull(productResponse)
                && Objects.nonNull(productResponse.getProductDto())) {
              ProductDto productDto = mapProductDtoFromService(productResponse.getProductDto());
              detail.setProductDto(productDto);
              if (Objects.nonNull(productDto.getMainCode())) {
                detail.setCode(productDto.getMainCode());
              }
              if (Objects.nonNull(productDto.getName())) {
                detail.setDescription(productDto.getName());
              }
              if (Objects.nonNull(productDto.getRate())) {
                detail.setIvaPercentage(productDto.getRate());
              }
            }
          } catch (Exception e) {
            log.warn(
                "⚠️ No se pudo cargar el producto {}: {}", detail.getIdProduct(), e.getMessage());
          }
        }
      }
    }

    log.info("✅ Factura completa retornada (ID: {}, Status: {})", idInvoice, status);
    return dto;
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public InvoiceResponse processDraftInvoice(Long idInvoice) {
    log.info("Processing draft invoice with ID: {}", idInvoice);

    InvoiceResponse response =
        InvoiceResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    // Find the draft invoice
    Invoice invoice =
        repo.findById(idInvoice)
            .orElseThrow(
                () -> {
                  log.error("Invoice with ID {} not found", idInvoice);
                  return new IllegalArgumentException(
                      "Factura con ID " + idInvoice + " no encontrada");
                });

    // Verify it's a draft (authorizationSri is null)
    if (invoice.getAuthorizationSri() != null) {
      log.error("Invoice {} is not a draft, it already has authorization from SRI", idInvoice);
      response.setMessage("La factura no es un borrador, ya fue procesada");
      return response;
    }

    // Convert to InvoiceDto for processing
    InvoiceDto invoiceDto = serviceMapper.toInvoiceDto(invoice);

    // Validate and assign sequential number based on issuance point
    if (Objects.nonNull(invoiceDto.getEstablishment())
        && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint())
        && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint().getIdIssuancePoint())) {

      Long idIssuancePoint = invoiceDto.getEstablishment().getIssuancePoint().getIdIssuancePoint();

      // Get the next sequential for this issuance point
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;

      // Set the sequential and issuance point ID in the invoice
      invoice.setIdIssuancePoint(idIssuancePoint);
      invoice.setSequential(nextSequential);
      invoice.setRetryCount(0);
      invoiceDto.setIdIssuancePoint(idIssuancePoint);
      invoiceDto.setSequential(nextSequential);

      // Format sequential as a 9-digit string for the issuancePoint DTO (for SRI)
      String formattedSequential = String.format("%09d", nextSequential);
      invoiceDto.getEstablishment().getIssuancePoint().setSequentialInvoice(formattedSequential);

      log.info("Assigned sequential {} for issuance point {}", nextSequential, idIssuancePoint);
    } else {
      log.error("Issuance point ID is required to generate invoice sequential");
      response.setMessage(
          "El ID del punto de emisión es requerido para generar el secuencial de la factura.");
      return response;
    }

    // Process with digital signature and send to SRI
    try {
      // Obtener ClientDto completo con contactos antes de enviar al SRI
      ClientDto clientDtoWithContacts = getClientDtoWithContacts(invoiceDto);

      DigitalSignatureResponse digitalSignatureResponse =
          digitalSignatureClient.getDigitalSignatureByCompanyId(
              invoiceDto.getCompanyDto().getIdCompany());

      if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
        log.error(
            "There is no digital signature configuration for company {}",
            invoiceDto.getCompanyDto().getIdCompany());
        response.setMessage(
            "La firma digital caduca y/o no está configurada comuníquese con el administrador.");
        return response;
      }

      // Para borradores procesados manualmente, no hay AuthorizationSri existente
      AuthorizationSri authorizationSri =
          sendInvoiceService.sendInvoiceSri(
              invoiceDto, digitalSignatureResponse.getDigitalSignatureDto(), null);

      invoice.setAuthorizationSri(authorizationSri);
      // Actualizar invoiceNumber desde el XML de autorización si está AUTORIZADO
      setInvoiceNumberFromAuthorizationXml(invoice, authorizationSri);
      Invoice processedInvoice = register(invoice);

      response = serviceMapper.toInvoiceResponse(processedInvoice);
      response.setCode(HttpStatus.OK.value());
      response.setStatus(HttpStatus.OK.name());
      response.setMessage("Borrador procesado y enviado al SRI exitosamente");

      // Pasar el ClientDto ya obtenido para evitar consulta duplicada
      notifyAuthorizedInvoice(
          invoiceDto, processedInvoice, authorizationSri, clientDtoWithContacts);

      log.info("Draft invoice {} processed successfully", idInvoice);

      // Sincronizar secuencial con ebill-customer después de autorización exitosa
      if (Objects.nonNull(invoiceDto.getEstablishment())
          && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint())
          && Objects.nonNull(invoiceDto.getEstablishment().getIssuancePoint().getCode())
          && Objects.nonNull(invoiceDto.getEstablishment().getIdEstablishment())) {

        try {
          syncSequentialToCustomer(
              invoiceDto.getEstablishment().getIssuancePoint().getCode(),
              invoiceDto.getEstablishment().getIdEstablishment(),
              processedInvoice.getSequential());
        } catch (Exception e) {
          // Log el error pero NO afectar la respuesta de la factura
          log.error(
              "❌❌❌ FALLO FINAL al sincronizar secuencial {} después de 3 intentos. "
                  + "Borrador procesado correctamente pero secuencial DESINCRONIZADO con ebill-customer. "
                  + "Sincronización manual requerida: PATCH /api/invoice-issuance-points/{}/sequential?establishmentId={}",
              processedInvoice.getSequential(),
              invoiceDto.getEstablishment().getIssuancePoint().getCode(),
              invoiceDto.getEstablishment().getIdEstablishment());
        }
      }

    } catch (ResourceAccessException | feign.RetryableException e) {
      log.error(
          "Error connecting to digital signature service for draft invoice {}: {}",
          idInvoice,
          e.getMessage());
      response.setMessage("Error al obtener la firma digital, comuníquese con el administrador.");
    } catch (CertificateException
        | SAXException
        | IOException
        | JAXBException
        | InterruptedException e) {
      log.error("Error processing draft invoice {} for SRI: {}", idInvoice, e.getMessage());
      response.setMessage("Hubo problemas para conectarse con el SRI.");
    }

    return response;
  }

  @Override
  public void deleteDraft(Long idInvoice) {
    log.info("🗑️ Eliminando borrador con ID: {}", idInvoice);

    // 1. Buscar la factura
    Invoice invoice =
        repo.findById(idInvoice)
            .orElseThrow(
                () -> {
                  log.error("❌ Borrador con ID {} no encontrado", idInvoice);
                  return new IllegalArgumentException(
                      "Borrador no encontrado con ID: " + idInvoice);
                });

    // 2. Verificar que sea un borrador (NO autorizado)
    if (Objects.nonNull(invoice.getAuthorizationSri())) {
      log.error(
          "❌ No se puede eliminar la factura {} porque ya fue autorizada por el SRI", idInvoice);
      throw new IllegalArgumentException(
          "No se puede eliminar esta factura porque ya fue autorizada por el SRI");
    }

    // 3. Eliminar el borrador (los InvoiceDetail se eliminan automáticamente por
    // cascade +
    // orphanRemoval)
    repo.delete(invoice);
    log.info("✅ Borrador {} eliminado exitosamente", idInvoice);
  }

  /**
   * Sincroniza el secuencial de la factura con ebill-customer. Este método reintenta hasta 3 veces
   * en caso de fallo temporal.
   *
   * @param issuancePointCode Código del punto de emisión (ej: "001")
   * @param establishmentId ID del establecimiento
   * @param sequential Número secuencial a sincronizar
   */
  @Retryable(
      retryFor = {feign.FeignException.class, ResourceAccessException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2))
  private void syncSequentialToCustomer(
      String issuancePointCode, Long establishmentId, Long sequential) {
    try {
      log.info(
          "🔄 Sincronizando secuencial {} para punto de emisión {}-{}",
          sequential,
          establishmentId,
          issuancePointCode);

      customerClient.updateSequential(
          issuancePointCode, establishmentId, Map.of("sequentialInvoice", sequential));

      log.info(
          "✅ Secuencial {} sincronizado exitosamente en ebill-customer para {}-{}",
          sequential,
          establishmentId,
          issuancePointCode);

    } catch (Exception e) {
      log.error(
          "❌ ERROR al sincronizar secuencial {} para {}-{}: {} (intento {}/3). "
              + "La factura se guardó correctamente pero el secuencial puede estar DESINCRONIZADO con ebill-customer.",
          sequential,
          establishmentId,
          issuancePointCode,
          e.getMessage(),
          // Se imprime el intento actual (se captura automáticamente por @Retryable)
          "N");
      throw e; // Re-lanzar para que @Retryable pueda reintentar
    }
  }

  private void notifyAuthorizedInvoice(
      InvoiceDto invoiceDto,
      Invoice savedInvoice,
      AuthorizationSri authorizationSri,
      ClientDto clientDto) {

    // Verificar si el correo ya fue enviado
    if (Boolean.TRUE.equals(savedInvoice.getEmailSent())) {
      log.info(
          "Email already sent for invoice {} on {}. Skipping notification.",
          savedInvoice.getIdInvoice(),
          savedInvoice.getEmailSentDate());
      return;
    }

    if (Objects.isNull(authorizationSri)
        || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
        || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
      return;
    }

    if (!StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      log.warn(
          "Authorization XML path is missing for invoice {}. Skipping email notification.",
          savedInvoice.getIdInvoice());
      return;
    }

    Path storageDir = null;
    try {
      storageDir = Paths.get(authorizationSri.getAuthorizationStoragePath());
    } catch (Exception ex) {
      log.warn(
          "Authorization XML path is invalid for invoice {}. Skipping email notification.",
          savedInvoice.getIdInvoice(),
          ex);
      return;
    }

    Path xmlPath = storageDir.resolve(authorizationSri.getAccessKey() + ".xml");
    if (!Files.isRegularFile(xmlPath)) {
      log.warn(
          "Authorization XML is unavailable for invoice {}. Skipping email notification.",
          savedInvoice.getIdInvoice());
      return;
    }
    // Usar el ClientDto pasado como parámetro (ya obtenido previamente)
    if (Objects.nonNull(clientDto)) {
      invoiceDto.setBuyerName(clientDto.getName());
      if (Objects.nonNull(clientDto.getContacts()) && !clientDto.getContacts().isEmpty()) {
        clientDto
            .getContacts()
            .forEach(
                c -> {
                  if (Objects.nonNull(c.getContactType()) && c.getContactType().equals("MAI")) {
                    invoiceDto.setBuyerMail(c.getValue());
                  }
                });
      }
    } else {
      // Fallback: si no se pasó el ClientDto, intentar obtenerlo (caso legacy)
      try {
        ClientDtoResponse response = customerClient.getClientById(savedInvoice.getIdClient());
        if (Objects.nonNull(response) && !CollectionUtils.isEmpty(response.getClientDtos())) {
          ClientDto fallbackClientDto = response.getClientDtos().get(0);
          invoiceDto.setBuyerName(fallbackClientDto.getName());
          if (Objects.nonNull(fallbackClientDto.getContacts())
              && !fallbackClientDto.getContacts().isEmpty()) {
            fallbackClientDto
                .getContacts()
                .forEach(
                    c -> {
                      if (Objects.nonNull(c.getContactType()) && c.getContactType().equals("MAI")) {
                        invoiceDto.setBuyerMail(c.getValue());
                      }
                    });
          }
        }
      } catch (feign.FeignException.Unauthorized e) {
        log.error(
            "Unauthorized access to digital signature service for invoice {} (retry {}/5) - Scheduled tasks need authentication configuration",
            savedInvoice.getIdInvoice(),
            savedInvoice.getRetryCount());
        handleInvoiceFailure(savedInvoice, "Unauthorized access to digital signature service");
      }
    }

    // CAMBIO: Siempre generar PDFs, independientemente del email
    // El servicio de delivery ahora maneja la generación de PDFs y el envío de email por separado
    invoiceDeliveryService.deliverAuthorizedInvoice(invoiceDto, savedInvoice, authorizationSri);

    // CAMBIO: Si no hay email, marcar como no enviado explícitamente
    String recipientEmail = resolveBuyerEmail(invoiceDto);
    if (!StringUtils.hasText(recipientEmail)) {
      log.info(
          "No recipient email found for invoice {}. PDF and XML generated, but email not sent. Marking emailSent as false.",
          savedInvoice.getIdInvoice());
      savedInvoice.setEmailSent(false);
      repo.save(savedInvoice);
    }
  }

  private String resolveBuyerEmail(InvoiceDto invoiceDto) {
    if (Objects.isNull(invoiceDto)) {
      return null;
    }

    if (StringUtils.hasText(invoiceDto.getBuyerMail())) {
      return invoiceDto.getBuyerMail();
    }

    if (Objects.nonNull(invoiceDto.getClientDto())
        && Objects.nonNull(invoiceDto.getClientDto().getContacts())) {
      return invoiceDto.getClientDto().getContacts().stream()
          .filter(Objects::nonNull)
          .filter(
              contact ->
                  Objects.isNull(contact.getStatus()) || Boolean.TRUE.equals(contact.getStatus()))
          .filter(contact -> isEmailContact(contact.getContactType()))
          .map(ContactDto::getValue)
          .filter(StringUtils::hasText)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private boolean isEmailContact(String contactType) {
    if (!StringUtils.hasText(contactType)) {
      return false;
    }
    return "EMAIL".equalsIgnoreCase(contactType) || "EMA".equalsIgnoreCase(contactType);
  }

  /**
   * Extrae el invoiceNumber del XML de autorización cuando el estado es AUTORIZADO y lo asigna a la
   * factura antes de guardarla. Formato: estab-ptoEmi-secuencial
   *
   * @param invoice La factura a la que se le asignará el invoiceNumber
   * @param authorizationSri La autorización que contiene el XML con los datos
   */
  private void setInvoiceNumberFromAuthorizationXml(
      Invoice invoice, AuthorizationSri authorizationSri) {
    if (Objects.isNull(authorizationSri)
        || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())
        || !StringUtils.hasText(authorizationSri.getAuthorizationXml())) {
      return;
    }

    try {
      // Parsear el XML de autorización
      Document document =
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(
                  new ByteArrayInputStream(
                      authorizationSri.getAuthorizationXml().getBytes(StandardCharsets.UTF_8)));

      document.getDocumentElement().normalize();

      // Extraer el XML del comprobante (está dentro de CDATA)
      String comprobanteXml = getNodeText(document, "comprobante");

      if (!StringUtils.hasText(comprobanteXml)) {
        log.warn(
            "No se encontró el XML del comprobante en la autorización para invoice {}",
            invoice.getIdInvoice());
        return;
      }

      // Parsear el XML del comprobante a objeto Factura
      Factura factura =
          XmlUtil.unmarshal(
              new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
              Factura.class);

      // Extraer estab, ptoEmi y secuencial
      String estab = factura.getInfoTributaria().getEstab();
      String ptoEmi = factura.getInfoTributaria().getPtoEmi();
      String secuencial = factura.getInfoTributaria().getSecuencial();

      if (StringUtils.hasText(estab)
          && StringUtils.hasText(ptoEmi)
          && StringUtils.hasText(secuencial)) {
        // Construir el invoiceNumber con el formato: estab-ptoEmi-secuencial
        String invoiceNumber = String.format("%s-%s-%s", estab, ptoEmi, secuencial);
        invoice.setInvoiceNumber(invoiceNumber);

        log.info(
            "✅ InvoiceNumber asignado para invoice {}: {}", invoice.getIdInvoice(), invoiceNumber);
      } else {
        log.warn(
            "No se pudieron extraer todos los campos necesarios del XML para invoice {}. estab={}, ptoEmi={}, secuencial={}",
            invoice.getIdInvoice(),
            estab,
            ptoEmi,
            secuencial);
      }

    } catch (Exception e) {
      log.error(
          "Error al extraer invoiceNumber del XML de autorización para invoice {}: {}",
          invoice.getIdInvoice(),
          e.getMessage(),
          e);
    }
  }

  /** Helper para extraer texto de un nodo XML */
  private String getNodeText(Document document, String tagName) {
    NodeList nodeList = document.getElementsByTagName(tagName);
    if (nodeList.getLength() > 0) {
      return nodeList.item(0).getTextContent();
    }
    return null;
  }

  /**
   * Obtiene el ClientDto con los contactos completos, incluyendo la dirección (DIR). Si el
   * invoiceDto ya tiene el ClientDto con contactos, lo retorna. Si no, consulta el cliente desde el
   * servicio.
   *
   * @param invoiceDto El DTO de la factura que puede o no tener el ClientDto completo
   * @return ClientDto con los contactos completos, o null si no se puede obtener
   */
  private ClientDto getClientDtoWithContacts(InvoiceDto invoiceDto) {
    // Verificar si ya tenemos el ClientDto con contactos y dirección
    if (Objects.nonNull(invoiceDto.getClientDto())
        && Objects.nonNull(invoiceDto.getClientDto().getContacts())
        && !invoiceDto.getClientDto().getContacts().isEmpty()) {

      // Verificar si ya tiene un contacto tipo DIR
      boolean hasDireccion =
          invoiceDto.getClientDto().getContacts().stream()
              .anyMatch(
                  c ->
                      Objects.nonNull(c.getContactType())
                          && "DIR".equalsIgnoreCase(c.getContactType())
                          && StringUtils.hasText(c.getValue()));

      if (hasDireccion) {
        log.debug("ClientDto ya tiene contactos con dirección, reutilizando");
        return invoiceDto.getClientDto();
      }
    }

    // Si no tiene el ClientDto completo o no tiene dirección, consultarlo
    if (Objects.nonNull(invoiceDto.getIdClient())) {
      try {
        log.info(
            "Consultando cliente {} para obtener contactos completos", invoiceDto.getIdClient());
        ClientDtoResponse response = customerClient.getClientById(invoiceDto.getIdClient());

        if (Objects.nonNull(response)
            && !CollectionUtils.isEmpty(response.getClientDtos())
            && Objects.nonNull(response.getClientDtos().get(0))) {
          ClientDto clientDto = response.getClientDtos().get(0);
          // Actualizar el invoiceDto con el ClientDto completo
          invoiceDto.setClientDto(clientDto);
          return clientDto;
        }
      } catch (Exception e) {
        log.warn(
            "Error al consultar cliente {} para obtener contactos: {}",
            invoiceDto.getIdClient(),
            e.getMessage());
      }
    }

    return invoiceDto.getClientDto(); // Retornar el que ya tiene aunque no tenga contactos
  }

  /**
   * Extrae la dirección del comprador desde los contactos del cliente. Busca el contacto con
   * contactType = "DIR"
   *
   * @param clientDto El DTO del cliente con sus contactos
   * @return La dirección del comprador o null si no se encuentra
   */
  /**
   * Mapea el ProductDto del servicio de productos (ebill-product) al ProductDto de ebill-billing.
   * El servicio de productos retorna un ProductDto con estructura diferente (incluye TaxTypeDto).
   *
   * @param productDtoFromService ProductDto del servicio de productos (viene como Map desde
   *     ProductResponse)
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

      // Mapear TaxTypeDto para extraer rate, taxCode, taxCodPercentage
      // El servicio responde con "ivaTaxType" no "taxType"
      if (productMap.containsKey("ivaTaxType") && productMap.get("ivaTaxType") != null) {
        Object taxTypeObj = productMap.get("ivaTaxType");
        if (taxTypeObj instanceof java.util.Map) {
          java.util.Map<String, Object> taxTypeMap = (java.util.Map<String, Object>) taxTypeObj;

          // Extraer rate (value en TaxTypeDto)
          // El valor viene como porcentaje (15.00) y se mantiene así porque UtilsMath.iva()
          // ya divide por 100 internamente: IVA = (baseImponible × rate) / 100
          if (taxTypeMap.containsKey("value")) {
            Object value = taxTypeMap.get("value");
            if (value instanceof Number) {
              // Mantener el valor como porcentaje (15.00), no convertir a decimal (0.15)
              java.math.BigDecimal rate =
                  new java.math.BigDecimal(value.toString())
                      .setScale(4, java.math.RoundingMode.HALF_UP);
              productDto.setRate(rate);
            }
          }

          // Extraer taxCode (type en TaxTypeDto, que es el código del impuesto)
          // Si es "IVA" o similar, mapear al código numérico "2"
          if (taxTypeMap.containsKey("type")) {
            Object typeObj = taxTypeMap.get("type");
            String type = String.valueOf(typeObj);
            if (type != null && (type.equalsIgnoreCase("IVA") || type.equalsIgnoreCase("2"))) {
              productDto.setTaxCode("2"); // Código SRI para IVA
            } else {
              productDto.setTaxCode(type);
            }
          }

          // Extraer taxCodPercentage: mapear el porcentaje al código SRI
          // No guardar "15%" sino el código "4" según CodigoPorcentajeEnum
          // El value puede venir como decimal (0.15) o como porcentaje (15.0)
          if (taxTypeMap.containsKey("value")) {
            Object value = taxTypeMap.get("value");
            if (value instanceof Number) {
              double rateValue = ((Number) value).doubleValue();
              int percentage;
              // Detectar si viene como decimal (0.15) o como porcentaje (15.0)
              if (rateValue >= 1.0) {
                // Ya viene como porcentaje (ej: 15.0)
                percentage = (int) rateValue;
              } else {
                // Viene como decimal (ej: 0.15), convertir a porcentaje
                percentage = (int) (rateValue * 100);
              }
              // Mapear porcentaje a código SRI
              String codigoPorcentaje = mapIvaPercentageToCodigoSRI(percentage);
              productDto.setTaxCodPercentage(codigoPorcentaje);
            }
          }
        }
      }

      // Campos que no existen en el servicio de productos, dejar como null o default
      productDto.setAuxiliarCode(null);
      productDto.setDiscount(0.0);

      return productDto;

    } catch (Exception e) {
      log.error("Error mapping ProductDto from service: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * Mapea el porcentaje de IVA al código de porcentaje según el catálogo del SRI.
   *
   * @param percentage Porcentaje de IVA (ej: 15, 12, 14, 0)
   * @return Código de porcentaje según CodigoPorcentajeEnum (ej: "4" para 15%, "2" para 12%)
   */
  private String mapIvaPercentageToCodigoSRI(int percentage) {
    switch (percentage) {
      case 0:
        return "0"; // IVA_0
      case 12:
        return "2"; // IVA_12
      case 14:
        return "3"; // IVA_14
      case 15:
        return "4"; // IVA_15
      default:
        // Si no coincide con ningún código conocido, usar el más cercano
        log.warn(
            "Porcentaje {}% no tiene código SRI específico, usando código por defecto", percentage);
        if (percentage == 0) {
          return "0";
        } else if (percentage <= 12) {
          return "2"; // Usar 12% como aproximación
        } else if (percentage <= 14) {
          return "3"; // Usar 14% como aproximación
        } else {
          return "4"; // Usar 15% como aproximación
        }
    }
  }

  private String extractBuyerAddress(ClientDto clientDto) {
    if (Objects.isNull(clientDto)
        || Objects.isNull(clientDto.getContacts())
        || clientDto.getContacts().isEmpty()) {
      return null;
    }

    return clientDto.getContacts().stream()
        .filter(
            c ->
                Objects.nonNull(c.getContactType())
                    && "DIR".equalsIgnoreCase(c.getContactType())
                    && StringUtils.hasText(c.getValue())
                    && (Objects.isNull(c.getStatus()) || Boolean.TRUE.equals(c.getStatus())))
        .map(ContactDto::getValue)
        .findFirst()
        .orElse(null);
  }

  @Override
  public InvoiceAvailableForCreditNoteDto getInvoiceAvailableForCreditNote(Long idInvoice) {
    log.info("Obteniendo factura {} disponible para nota de crédito", idInvoice);

    // 1. Buscar la factura
    Invoice invoice =
        repo.findById(idInvoice)
            .orElseThrow(
                () -> new IllegalArgumentException("Factura no encontrada con ID: " + idInvoice));

    // 2. Verificar que esté autorizada
    if (Objects.isNull(invoice.getAuthorizationSri())
        || !"AUTORIZADO"
            .equalsIgnoreCase(invoice.getAuthorizationSri().getAuthorizationStatusSri())) {
      throw new IllegalArgumentException(
          "Solo se pueden crear notas de crédito para facturas autorizadas por el SRI");
    }

    // 3. Obtener datos del cliente
    InvoiceAvailableForCreditNoteDto.ClientInfo clientInfo = null;
    if (Objects.nonNull(invoice.getIdClient())) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(invoice.getIdClient());
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          ClientDto clientDto = clientResponse.getClientDtos().get(0);
          String email = extractClientEmail(clientDto);
          clientInfo =
              InvoiceAvailableForCreditNoteDto.ClientInfo.builder()
                  .idClient(clientDto.getIdClient())
                  .identification(clientDto.getIdentification())
                  .name(clientDto.getName())
                  .email(email)
                  .build();
        }
      } catch (Exception e) {
        log.warn(
            "No se pudo obtener datos del cliente {}: {}", invoice.getIdClient(), e.getMessage());
      }
    }

    // 4. Obtener cantidades ya devueltas por detalle de factura
    Map<Long, java.math.BigDecimal> quantitiesReturned = new HashMap<>();
    List<Object[]> returnedData = creditNoteDetailRepo.findQuantitiesReturnedByInvoice(idInvoice);
    for (Object[] row : returnedData) {
      Long idInvoiceDetail = ((Number) row[0]).longValue();
      // El SUM() de JPA puede devolver Long, Double o BigDecimal dependiendo del driver
      java.math.BigDecimal totalReturned =
          row[1] != null
              ? new java.math.BigDecimal(((Number) row[1]).toString())
              : java.math.BigDecimal.ZERO;
      quantitiesReturned.put(idInvoiceDetail, totalReturned);
    }

    // 5. Obtener total de NC aplicadas
    java.math.BigDecimal totalCreditNotesApplied =
        creditNoteDetailRepo.findTotalCreditNotesApplied(idInvoice);

    // 6. Calcular disponible por cada detalle
    List<InvoiceAvailableForCreditNoteDto.InvoiceDetailAvailable> detailsAvailable =
        new ArrayList<>();
    if (!CollectionUtils.isEmpty(invoice.getInvoiceDetails())) {
      for (InvoiceDetail detail : invoice.getInvoiceDetails()) {
        java.math.BigDecimal quantityInvoiced = java.math.BigDecimal.valueOf(detail.getQuantity());
        java.math.BigDecimal quantityReturned =
            quantitiesReturned.getOrDefault(detail.getIdInvoiceDetail(), java.math.BigDecimal.ZERO);
        java.math.BigDecimal quantityAvailable = quantityInvoiced.subtract(quantityReturned);

        // Obtener datos del producto
        String mainCode = null;
        String description = null;
        java.math.BigDecimal ivaPercentage = java.math.BigDecimal.ZERO;
        try {
          ProductDto productDto = getProductFromService(detail.getIdProduct());
          if (productDto != null) {
            mainCode = productDto.getMainCode();
            description = productDto.getName();
            ivaPercentage =
                productDto.getRate() != null ? productDto.getRate() : java.math.BigDecimal.ZERO;
          }
        } catch (Exception e) {
          log.warn(
              "No se pudo obtener datos del producto {}: {}",
              detail.getIdProduct(),
              e.getMessage());
        }

        log.info(
            "Detalle factura {} - idProduct: {}, mainCode: {}, description: {}",
            detail.getIdInvoiceDetail(),
            detail.getIdProduct(),
            mainCode,
            description);

        detailsAvailable.add(
            InvoiceAvailableForCreditNoteDto.InvoiceDetailAvailable.builder()
                .idInvoiceDetail(detail.getIdInvoiceDetail())
                .idProduct(detail.getIdProduct())
                .mainCode(mainCode)
                .description(description)
                .quantityInvoiced(quantityInvoiced)
                .quantityReturned(quantityReturned)
                .quantityAvailable(quantityAvailable)
                .unitPrice(detail.getUnitPrice())
                .discount(detail.getDiscount())
                .subtotal(detail.getSubtotal())
                .ivaPercentage(ivaPercentage)
                .build());
      }
    }

    log.info("Response available-for-credit-note - Total detalles: {}", detailsAvailable.size());
    for (InvoiceAvailableForCreditNoteDto.InvoiceDetailAvailable det : detailsAvailable) {
      log.info(
          "Detalle response: idInvoiceDetail={}, mainCode={}, description={}",
          det.getIdInvoiceDetail(),
          det.getMainCode(),
          det.getDescription());
    }

    // 7. Calcular monto disponible
    java.math.BigDecimal totalInvoice =
        invoice.getTotalAmount() != null ? invoice.getTotalAmount() : java.math.BigDecimal.ZERO;
    java.math.BigDecimal tip =
        invoice.getTip() != null ? invoice.getTip() : java.math.BigDecimal.ZERO;
    // availableAmount = totalInvoice - tip - totalCreditNotesApplied
    // La propina NO es retornable mediante NC, solo los productos
    java.math.BigDecimal availableAmount =
        totalInvoice.subtract(tip).subtract(totalCreditNotesApplied);
    if (availableAmount.compareTo(java.math.BigDecimal.ZERO) < 0) {
      availableAmount = java.math.BigDecimal.ZERO;
    }
    log.info(
        "📊 available-for-credit-note: total={}, tip={}, NCApplied={}, available={}",
        totalInvoice,
        tip,
        totalCreditNotesApplied,
        availableAmount);

    // 8. Construir respuesta
    return InvoiceAvailableForCreditNoteDto.builder()
        .idInvoice(invoice.getIdInvoice())
        .invoiceNumber(invoice.getInvoiceNumber())
        .issueDate(invoice.getIssueDate())
        .accessKey(
            invoice.getAuthorizationSri() != null
                ? invoice.getAuthorizationSri().getAccessKey()
                : null)
        .authorizationNumber(
            invoice.getAuthorizationSri() != null
                ? invoice.getAuthorizationSri().getAuthorizationNumber()
                : null)
        .client(clientInfo)
        .totalInvoice(totalInvoice)
        .tip(tip)
        .totalCreditNotesApplied(totalCreditNotesApplied)
        .availableAmount(availableAmount)
        .details(detailsAvailable)
        .build();
  }

  private String extractClientEmail(ClientDto clientDto) {
    if (clientDto == null || CollectionUtils.isEmpty(clientDto.getContacts())) {
      return null;
    }
    return clientDto.getContacts().stream()
        .filter(
            c ->
                Objects.nonNull(c.getContactType())
                    && ("MAI".equalsIgnoreCase(c.getContactType())
                        || "EMAIL".equalsIgnoreCase(c.getContactType()))
                    && StringUtils.hasText(c.getValue()))
        .map(ContactDto::getValue)
        .findFirst()
        .orElse(null);
  }

  private ProductDto getProductFromService(Long idProduct) {
    try {
      log.info("Obteniendo producto desde servicio, idProduct: {}", idProduct);
      com.wallparisoft.ebillbilling.response.ProductResponse productResponse =
          productClient.getProductById(idProduct);
      log.info("ProductResponse recibido: {}", productResponse);

      if (Objects.nonNull(productResponse) && Objects.nonNull(productResponse.getProductDto())) {
        log.info("ProductDto del response: {}", productResponse.getProductDto());
        ProductDto mappedDto = mapProductDtoFromService(productResponse.getProductDto());
        log.info(
            "ProductDto mapeado - mainCode: {}, name: {}, rate: {}",
            mappedDto != null ? mappedDto.getMainCode() : "null",
            mappedDto != null ? mappedDto.getName() : "null",
            mappedDto != null ? mappedDto.getRate() : "null");
        return mappedDto;
      } else {
        log.warn("ProductResponse es null o no tiene productDto para idProduct: {}", idProduct);
      }
    } catch (Exception e) {
      log.error("Error al obtener producto {}: {}", idProduct, e.getMessage(), e);
    }
    return null;
  }

  @Override
  public Page<DocumentSearchResponseDto> queryDocuments(
      DocumentQueryDto queryDto,
      Pageable pageable,
      jakarta.servlet.http.HttpServletRequest request) {

    // Extraer IP del cliente
    String clientIp = extractClientIp(request);

    // Extraer token del request
    String token = extractToken(request);

    // 1. Validar rate limiting por TOKEN
    if (token != null && !token.isBlank()) {
      if (!genericRateLimitService.canMakeRequest(
          "DOCUMENTS_QUERY",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.TOKEN,
          token)) {

        long secondsLeft =
            genericRateLimitService.getTimeUntilNextAttempt(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN,
                token);
        long minutesLeft = secondsLeft / 60;
        int currentAttempts =
            genericRateLimitService.getCurrentAttempts(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN,
                token);
        int maxAttempts =
            genericRateLimitService.getMaxRequests(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN);
        int windowMinutes =
            genericRateLimitService.getWindowMinutes(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN);

        String message =
            String.format(
                "Máximo de solicitudes alcanzado por token (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);

        throw new com.wallparisoft.ebillbilling.exception.RateLimitExceededException(
            message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
      }
    }

    // 2. Validar rate limiting por IP
    if (clientIp != null && !clientIp.isBlank()) {
      if (!genericRateLimitService.canMakeRequest(
          "DOCUMENTS_QUERY",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
          clientIp)) {

        long secondsLeft =
            genericRateLimitService.getTimeUntilNextAttempt(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
                clientIp);
        long minutesLeft = secondsLeft / 60;
        int currentAttempts =
            genericRateLimitService.getCurrentAttempts(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
                clientIp);
        int maxAttempts =
            genericRateLimitService.getMaxRequests(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .IP);
        int windowMinutes =
            genericRateLimitService.getWindowMinutes(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .IP);

        String message =
            String.format(
                "Máximo de solicitudes alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);

        throw new com.wallparisoft.ebillbilling.exception.RateLimitExceededException(
            message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
      }
    }

    log.info("Querying documents with criteria: {}, pageable: {}", queryDto, pageable);

    // Validar documentType (0=ambos, 1=facturas, 2=notas de crédito)
    Integer documentType = queryDto.getDocumentType();
    if (documentType == null) {
      documentType = 0; // Por defecto, buscar ambos
    }

    // Si se busca por identificación del comprador, primero buscar el cliente
    // ClientDto clientDto = null;
    if (StringUtils.hasText(queryDto.getBuyerIdentification())) {
      try {
        ClientDtoResponse clientDtoResponse =
            customerClient.getClientByIdentification(queryDto.getBuyerIdentification());
        List<DocumentSearchResponseDto> allDocuments = new ArrayList<>();
        if (clientDtoResponse != null && !clientDtoResponse.getClientDtos().isEmpty()) {

          // Consultar facturas si documentType es 0 (ambos) o 1 (solo facturas)
          if (documentType == 0 || documentType == 1) {
            InvoiceQueryDto invoiceQueryDto =
                InvoiceQueryDto.builder()
                    .invoiceNumber(queryDto.getDocumentNumber())
                    .buyerIdentification(queryDto.getBuyerIdentification())
                    .creationDateFrom(queryDto.getCreationDateFrom())
                    .creationDateTo(queryDto.getCreationDateTo())
                    .receptionStatusSri(queryDto.getReceptionStatusSri())
                    .authorizationStatusSri(queryDto.getAuthorizationStatusSri())
                    .build();

            Page<InvoiceSearchResponseDto> invoices =
                queryInvoices(invoiceQueryDto, pageable, request);
            for (InvoiceSearchResponseDto invoice : invoices.getContent()) {
              DocumentSearchResponseDto doc =
                  DocumentSearchResponseDto.builder()
                      .documentType(1) // 1 =
                      // Factura
                      .idDocument(invoice.getIdInvoice())
                      .documentNumber(invoice.getInvoiceNumber())
                      .creationDate(invoice.getCreationDate())
                      .idClient(invoice.getIdClient())
                      .nameClient(invoice.getNameClient())
                      .totalAmount(invoice.getTotalAmount())
                      .authorizationNumber(invoice.getAuthorizationNumber())
                      .receptionStatusSri(invoice.getReceptionStatusSri())
                      .authorizationStatusSri(invoice.getAuthorizationStatusSri())
                      .storagePath(invoice.getStoragePath())
                      .originalInvoiceId(null)
                      .originalInvoiceNumber(null)
                      .reason(null)
                      .build();
              allDocuments.add(doc);
            }
          }

          // Consultar notas de crédito si documentType es 0 (ambos) o 2 (solo notas de crédito)
          if (documentType == 0 || documentType == 2) {
            CreditNoteQueryDto creditNoteQueryDto =
                CreditNoteQueryDto.builder()
                    .creditNoteNumber(queryDto.getDocumentNumber())
                    .buyerIdentification(queryDto.getBuyerIdentification())
                    .creationDateFrom(queryDto.getCreationDateFrom())
                    .creationDateTo(queryDto.getCreationDateTo())
                    .receptionStatusSri(queryDto.getReceptionStatusSri())
                    .authorizationStatusSri(queryDto.getAuthorizationStatusSri())
                    .build();

            Page<CreditNoteSearchResponseDto> creditNotes =
                creditNoteService.queryCreditNotes(creditNoteQueryDto, pageable);
            for (CreditNoteSearchResponseDto creditNote : creditNotes.getContent()) {
              DocumentSearchResponseDto doc =
                  DocumentSearchResponseDto.builder()
                      .documentType(2) // 2 =
                      // Nota
                      // de
                      // Crédito
                      .idDocument(creditNote.getIdCreditNote())
                      .documentNumber(creditNote.getCreditNoteNumber())
                      .creationDate(creditNote.getCreationDate())
                      .idClient(creditNote.getIdClient())
                      .nameClient(creditNote.getNameClient())
                      .totalAmount(creditNote.getTotalAmount())
                      .authorizationNumber(creditNote.getAuthorizationNumber())
                      .receptionStatusSri(creditNote.getReceptionStatusSri())
                      .authorizationStatusSri(creditNote.getAuthorizationStatusSri())
                      .storagePath(creditNote.getStoragePath())
                      .originalInvoiceId(creditNote.getOriginalInvoiceId())
                      .originalInvoiceNumber(creditNote.getOriginalInvoiceNumber())
                      .reason(creditNote.getReason())
                      .build();
              allDocuments.add(doc);
            }
          }

          // Ordenar por fecha de creación descendente
          allDocuments.sort(
              (a, b) -> {
                if (a.getCreationDate() == null && b.getCreationDate() == null) {
                  return 0;
                }
                if (a.getCreationDate() == null) {
                  return 1;
                }
                if (b.getCreationDate() == null) {
                  return -1;
                }
                return b.getCreationDate().compareTo(a.getCreationDate());
              });

          // Aplicar paginación manual
          int start = (int) pageable.getOffset();
          int end = Math.min((start + pageable.getPageSize()), allDocuments.size());
          List<DocumentSearchResponseDto> pagedDocuments =
              start < allDocuments.size() ? allDocuments.subList(start, end) : new ArrayList<>();

          log.info(
              "Found {} documents matching criteria (type: {})",
              allDocuments.size(),
              documentType == 0 ? "both" : (documentType == 1 ? "invoices" : "credit notes"));

          Page<DocumentSearchResponseDto> result =
              new PageImpl<>(pagedDocuments, pageable, allDocuments.size());

          // 3. Incrementar contadores SOLO si fue exitoso
          if (token != null && !token.isBlank()) {
            genericRateLimitService.incrementRequest(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN,
                token);
          }
          if (clientIp != null && !clientIp.isBlank()) {
            genericRateLimitService.incrementRequest(
                "DOCUMENTS_QUERY",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
                clientIp);
          }
          return result;
        } else {
          log.info("No client found for identification: {}", queryDto.getBuyerIdentification());
          return Page.empty(pageable);
        }
      } catch (Exception ex) {
        log.warn(
            "Error searching client by identification {}: {}",
            queryDto.getBuyerIdentification(),
            ex.getMessage());
        return Page.empty(pageable);
      }
    }
    return Page.empty(pageable);
  }

  /** Extrae la IP del cliente del request, manejando proxies. */
  private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }

  /** Extrae el token JWT del header Authorization. */
  private String extractToken(jakarta.servlet.http.HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return null;
  }

  @Override
  public Page<com.wallparisoft.ebillbilling.dto.InvoiceForDebitNoteResponseDto>
      searchInvoicesForDebitNote(
          com.wallparisoft.ebillbilling.dto.InvoiceSearchForDebitNoteDto searchDto,
          Pageable pageable) {
    log.info(
        "🔍 [INVOICE] Buscando facturas para nota de débito - idCompany: {}, numeroAutorizacion: {}, numeroFactura: {}, nombreCliente: {}, identificacionCliente: {}, page: {}, size: {}",
        searchDto.getIdCompany(),
        searchDto.getNumeroAutorizacion(),
        searchDto.getNumeroFactura(),
        searchDto.getNombreCliente(),
        searchDto.getIdentificacionCliente(),
        pageable.getPageNumber(),
        pageable.getPageSize());

    if (searchDto.getIdCompany() == null) {
      throw new IllegalArgumentException("idCompany es obligatorio");
    }

    // Buscar cliente por nombre o identificación si se proporciona
    Long idClient = null;
    if (StringUtils.hasText(searchDto.getNombreCliente())
        || StringUtils.hasText(searchDto.getIdentificacionCliente())) {
      try {
        ClientDtoResponse clientResponse = null;
        if (StringUtils.hasText(searchDto.getIdentificacionCliente())) {
          clientResponse =
              customerClient.getClientByIdentification(searchDto.getIdentificacionCliente());
        } else if (StringUtils.hasText(searchDto.getNombreCliente())) {
          // Si solo hay nombre, buscar por nombre (esto puede requerir un endpoint adicional)
          // Por ahora, usaremos identificación si está disponible
          log.warn(
              "Búsqueda por nombre de cliente no implementada completamente. Usando identificación si está disponible.");
        }

        if (clientResponse != null
            && !CollectionUtils.isEmpty(clientResponse.getClientDtos())
            && Objects.nonNull(clientResponse.getClientDtos().get(0))) {
          idClient = clientResponse.getClientDtos().get(0).getIdClient();
        }
      } catch (Exception e) {
        log.warn("Error buscando cliente por nombre/identificación: {}", e.getMessage());
      }
    }

    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Invoice> invoicesPage =
        repo.findInvoicesForDebitNote(
            searchDto.getIdCompany(),
            searchDto.getNumeroAutorizacion(),
            searchDto.getNumeroFactura(),
            idClient,
            unsortedPageable);

    // Pre-cargar datos de clientes
    Set<Long> uniqueClientIds =
        invoicesPage.getContent().stream()
            .map(Invoice::getIdClient)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, ClientDto> clientCache = new HashMap<>();
    for (Long clientId : uniqueClientIds) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(clientId);
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          clientCache.put(clientId, clientResponse.getClientDtos().get(0));
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener datos del cliente {}: {}", clientId, e.getMessage());
      }
    }

    // Mapear a DTO
    List<com.wallparisoft.ebillbilling.dto.InvoiceForDebitNoteResponseDto> responseDtos =
        invoicesPage.getContent().stream()
            .map(
                invoice -> {
                  ClientDto clientDto = clientCache.get(invoice.getIdClient());

                  // Calcular availableAmount (para notas de débito, generalmente es el totalAmount)
                  // ya que las notas de débito agregan valor, no lo restan
                  java.math.BigDecimal availableAmount = invoice.getTotalAmount();
                  if (availableAmount == null) {
                    availableAmount = java.math.BigDecimal.ZERO;
                  }

                  // Construir AuthorizationSriInfo
                  com.wallparisoft.ebillbilling.dto.InvoiceForDebitNoteResponseDto
                          .AuthorizationSriInfo
                      authInfo = null;
                  if (invoice.getAuthorizationSri() != null) {
                    authInfo =
                        com.wallparisoft.ebillbilling.dto.InvoiceForDebitNoteResponseDto
                            .AuthorizationSriInfo.builder()
                            .idAuthorizationSri(
                                invoice.getAuthorizationSri().getIdAuthorizationSri())
                            .authorizationNumber(
                                invoice.getAuthorizationSri().getAuthorizationNumber())
                            .accessKey(invoice.getAuthorizationSri().getAccessKey())
                            .authorizationStatusSri(
                                invoice.getAuthorizationSri().getAuthorizationStatusSri())
                            .authorizationDate(invoice.getAuthorizationSri().getAuthorizationDate())
                            .build();
                  }

                  return com.wallparisoft.ebillbilling.dto.InvoiceForDebitNoteResponseDto.builder()
                      .idInvoice(invoice.getIdInvoice())
                      .invoiceNumber(invoice.getInvoiceNumber())
                      .sequential(invoice.getSequential())
                      .issueDate(
                          invoice.getIssueDate() != null
                              ? invoice.getIssueDate().toLocalDate()
                              : null)
                      .creationDate(invoice.getCreationDate())
                      .totalAmount(invoice.getTotalAmount())
                      .tip(invoice.getTip())
                      .availableAmount(availableAmount)
                      .idCompany(invoice.getIdCompany())
                      .idIssuancePoint(invoice.getIdIssuancePoint())
                      .accessKey(
                          invoice.getAuthorizationSri() != null
                              ? invoice.getAuthorizationSri().getAccessKey()
                              : null)
                      .authorizationSri(authInfo)
                      .clientDto(clientDto)
                      .buyerName(clientDto != null ? clientDto.getName() : null)
                      .buyerIdentification(clientDto != null ? clientDto.getIdentification() : null)
                      .build();
                })
            .collect(Collectors.toList());

    log.info(
        "✅ [INVOICE] Búsqueda completada - Total: {}, Página: {}, Tamaño: {}",
        invoicesPage.getTotalElements(),
        invoicesPage.getNumber(),
        invoicesPage.getSize());

    return new PageImpl<>(responseDtos, pageable, invoicesPage.getTotalElements());
  }

  @Override
  public Page<InvoiceListResponseDto> searchInvoicesByCompany(
      com.wallparisoft.ebillbilling.dto.InvoiceSearchForCreditNoteDto searchDto,
      Pageable pageable) {
    log.info(
        "🔍 [INVOICE] Buscando facturas - idCompany: {}, numeroAutorizacion: {}, numeroFactura: {}, nombreCliente: {}, identificacionCliente: {}, page: {}, size: {}",
        searchDto.getIdCompany(),
        searchDto.getNumeroAutorizacion(),
        searchDto.getNumeroFactura(),
        searchDto.getNombreCliente(),
        searchDto.getIdentificacionCliente(),
        pageable.getPageNumber(),
        pageable.getPageSize());

    if (searchDto.getIdCompany() == null) {
      throw new IllegalArgumentException("idCompany es obligatorio");
    }

    // Buscar cliente por nombre o identificación si se proporciona
    Long idClient = null;
    if (StringUtils.hasText(searchDto.getNombreCliente())
        || StringUtils.hasText(searchDto.getIdentificacionCliente())) {
      try {
        ClientDtoResponse clientResponse = null;
        if (StringUtils.hasText(searchDto.getIdentificacionCliente())) {
          clientResponse =
              customerClient.getClientByIdentification(searchDto.getIdentificacionCliente());
        } else if (StringUtils.hasText(searchDto.getNombreCliente())) {
          // Si solo hay nombre, buscar por nombre (esto puede requerir un endpoint adicional)
          // Por ahora, usaremos identificación si está disponible
          log.warn(
              "Búsqueda por nombre de cliente no implementada completamente. Usando identificación si está disponible.");
        }

        if (clientResponse != null
            && !CollectionUtils.isEmpty(clientResponse.getClientDtos())
            && Objects.nonNull(clientResponse.getClientDtos().get(0))) {
          idClient = clientResponse.getClientDtos().get(0).getIdClient();
        }
      } catch (Exception e) {
        log.warn("Error buscando cliente por nombre/identificación: {}", e.getMessage());
      }
    }

    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Invoice> invoicesPage =
        repo.findInvoicesByCompanyAndFilters(
            searchDto.getIdCompany(),
            searchDto.getNumeroAutorizacion(),
            searchDto.getNumeroFactura(),
            idClient,
            unsortedPageable);

    // Pre-cargar datos de clientes
    Set<Long> uniqueClientIds =
        invoicesPage.getContent().stream()
            .map(Invoice::getIdClient)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, ClientDto> clientCache = new HashMap<>();
    for (Long clientId : uniqueClientIds) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(clientId);
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          clientCache.put(clientId, clientResponse.getClientDtos().get(0));
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener datos del cliente {}: {}", clientId, e.getMessage());
      }
    }

    // Mapear a DTO
    List<InvoiceListResponseDto> responseDtos =
        invoicesPage.getContent().stream()
            .map(
                invoice -> {
                  ClientDto clientDto = clientCache.get(invoice.getIdClient());

                  // Calcular monto disponible para NC (solo NC autorizadas descuentan)
                  BigDecimal totalCreditNotesApplied = BigDecimal.ZERO;
                  if (invoice.getIdInvoice() != null) {
                    try {
                      totalCreditNotesApplied =
                          creditNoteDetailRepo.findTotalCreditNotesApplied(invoice.getIdInvoice());
                      if (totalCreditNotesApplied == null) {
                        totalCreditNotesApplied = BigDecimal.ZERO;
                      }
                    } catch (Exception e) {
                      log.warn(
                          "Error calculando NC aplicadas para factura {}: {}",
                          invoice.getIdInvoice(),
                          e.getMessage());
                    }
                  }
                  BigDecimal totalAmount =
                      invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
                  BigDecimal tip = invoice.getTip() != null ? invoice.getTip() : BigDecimal.ZERO;
                  // availableAmount = totalAmount - tip - totalCreditNotesApplied
                  BigDecimal availableAmount =
                      totalAmount.subtract(tip).subtract(totalCreditNotesApplied);
                  if (availableAmount.compareTo(BigDecimal.ZERO) < 0) {
                    availableAmount = BigDecimal.ZERO;
                  }

                  return InvoiceListResponseDto.builder()
                      .idInvoice(invoice.getIdInvoice())
                      .idCompany(invoice.getIdCompany())
                      .idClient(invoice.getIdClient())
                      .idIssuancePoint(invoice.getIdIssuancePoint())
                      .sequential(invoice.getSequential())
                      .retryCount(invoice.getRetryCount())
                      .issueDate(invoice.getIssueDate())
                      .invoiceNumber(invoice.getInvoiceNumber())
                      .environment(invoice.getEnvironment())
                      .additionalInformation(invoice.getAdditionalInformation())
                      .totalAmount(invoice.getTotalAmount())
                      .tip(invoice.getTip())
                      .subtotalZero(invoice.getSubtotalZero())
                      .subtotalIva(invoice.getSubtotalIva())
                      .subtotalIce(invoice.getSubtotalIce())
                      .discount(invoice.getDiscount())
                      .emailSent(invoice.getEmailSent())
                      .emailSentDate(invoice.getEmailSentDate())
                      .authorizationSri(invoice.getAuthorizationSri())
                      .invoiceDetails(invoice.getInvoiceDetails())
                      .invoicePayments(null)
                      .creationDate(invoice.getCreationDate())
                      .updateDate(invoice.getUpdateDate())
                      .status(invoice.getStatus())
                      .clientName(clientDto != null ? clientDto.getName() : null)
                      .clientIdentification(
                          clientDto != null ? clientDto.getIdentification() : null)
                      .availableAmount(availableAmount)
                      .build();
                })
            .collect(Collectors.toList());

    log.info(
        "✅ [INVOICE] Búsqueda completada - Total: {}, Página: {}, Tamaño: {}",
        invoicesPage.getTotalElements(),
        invoicesPage.getNumber(),
        invoicesPage.getSize());

    return new PageImpl<>(responseDtos, pageable, invoicesPage.getTotalElements());
  }

  @Override
  public Page<InvoiceListResponseDto> searchInvoicesByFilters(
      com.wallparisoft.ebillbilling.dto.InvoiceSearchByCompanyAndDateDto searchDto,
      Pageable pageable) {
    log.info(
        "🔍 [INVOICE] Buscando facturas por compañía y fechas - idCompany: {}, creationDateFrom: {}, creationDateTo: {}, numeroAutorizacion: {}, numeroFactura: {}, nombreCliente: {}, identificacionCliente: {}, receptionStatusSri: {}, authorizationStatusSri: {}, page: {}, size: {}",
        searchDto.getIdCompany(),
        searchDto.getCreationDateFrom(),
        searchDto.getCreationDateTo(),
        searchDto.getNumeroAutorizacion(),
        searchDto.getNumeroFactura(),
        searchDto.getNombreCliente(),
        searchDto.getIdentificacionCliente(),
        searchDto.getReceptionStatusSri(),
        searchDto.getAuthorizationStatusSri(),
        pageable.getPageNumber(),
        pageable.getPageSize());

    if (searchDto.getIdCompany() == null) {
      throw new IllegalArgumentException("idCompany es obligatorio");
    }

    // Buscar cliente por nombre o identificación si se proporciona
    Long idClient = null;
    if (StringUtils.hasText(searchDto.getNombreCliente())
        || StringUtils.hasText(searchDto.getIdentificacionCliente())) {
      try {
        ClientDtoResponse clientResponse = null;
        if (StringUtils.hasText(searchDto.getIdentificacionCliente())) {
          clientResponse =
              customerClient.getClientByIdentification(searchDto.getIdentificacionCliente());
        } else if (StringUtils.hasText(searchDto.getNombreCliente())) {
          // Si solo hay nombre, buscar por nombre (esto puede requerir un endpoint adicional)
          // Por ahora, usaremos identificación si está disponible
          log.warn(
              "Búsqueda por nombre de cliente no implementada completamente. Usando identificación si está disponible.");
        }

        if (clientResponse != null
            && !CollectionUtils.isEmpty(clientResponse.getClientDtos())
            && Objects.nonNull(clientResponse.getClientDtos().get(0))) {
          idClient = clientResponse.getClientDtos().get(0).getIdClient();
        }
      } catch (Exception e) {
        log.warn("Error buscando cliente por nombre/identificación: {}", e.getMessage());
      }
    }

    // Convertir LocalDate a LocalDateTime para la búsqueda
    java.time.LocalDateTime creationDateFrom =
        searchDto.getCreationDateFrom() != null
            ? searchDto.getCreationDateFrom().atStartOfDay()
            : null;
    java.time.LocalDateTime creationDateTo =
        searchDto.getCreationDateTo() != null
            ? searchDto.getCreationDateTo().atTime(23, 59, 59)
            : null;

    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Invoice> invoicesPage =
        repo.findInvoicesByCompanyAndDateRange(
            searchDto.getIdCompany(),
            creationDateFrom,
            creationDateTo,
            searchDto.getNumeroAutorizacion(),
            searchDto.getNumeroFactura(),
            idClient,
            searchDto.getReceptionStatusSri(),
            searchDto.getAuthorizationStatusSri(),
            unsortedPageable);

    // Pre-cargar datos de clientes
    Set<Long> uniqueClientIds =
        invoicesPage.getContent().stream()
            .map(Invoice::getIdClient)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, ClientDto> clientCache = new HashMap<>();
    for (Long clientId : uniqueClientIds) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(clientId);
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          clientCache.put(clientId, clientResponse.getClientDtos().get(0));
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener datos del cliente {}: {}", clientId, e.getMessage());
      }
    }

    // Mapear a DTO
    List<InvoiceListResponseDto> responseDtos =
        invoicesPage.getContent().stream()
            .map(
                invoice -> {
                  ClientDto clientDto = clientCache.get(invoice.getIdClient());

                  // Calcular monto disponible para NC (solo NC autorizadas descuentan)
                  BigDecimal totalCreditNotesApplied = BigDecimal.ZERO;
                  if (invoice.getIdInvoice() != null) {
                    try {
                      totalCreditNotesApplied =
                          creditNoteDetailRepo.findTotalCreditNotesApplied(invoice.getIdInvoice());
                      if (totalCreditNotesApplied == null) {
                        totalCreditNotesApplied = BigDecimal.ZERO;
                      }
                    } catch (Exception e) {
                      log.warn(
                          "Error calculando NC aplicadas para factura {}: {}",
                          invoice.getIdInvoice(),
                          e.getMessage());
                    }
                  }
                  BigDecimal totalAmount =
                      invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
                  BigDecimal tip = invoice.getTip() != null ? invoice.getTip() : BigDecimal.ZERO;
                  // availableAmount = totalAmount - tip - totalCreditNotesApplied
                  BigDecimal availableAmount =
                      totalAmount.subtract(tip).subtract(totalCreditNotesApplied);
                  if (availableAmount.compareTo(BigDecimal.ZERO) < 0) {
                    availableAmount = BigDecimal.ZERO;
                  }

                  return InvoiceListResponseDto.builder()
                      .idInvoice(invoice.getIdInvoice())
                      .idCompany(invoice.getIdCompany())
                      .idClient(invoice.getIdClient())
                      .idIssuancePoint(invoice.getIdIssuancePoint())
                      .sequential(invoice.getSequential())
                      .retryCount(invoice.getRetryCount())
                      .issueDate(invoice.getIssueDate())
                      .invoiceNumber(invoice.getInvoiceNumber())
                      .environment(invoice.getEnvironment())
                      .additionalInformation(invoice.getAdditionalInformation())
                      .totalAmount(invoice.getTotalAmount())
                      .tip(invoice.getTip())
                      .subtotalZero(invoice.getSubtotalZero())
                      .subtotalIva(invoice.getSubtotalIva())
                      .subtotalIce(invoice.getSubtotalIce())
                      .discount(invoice.getDiscount())
                      .emailSent(invoice.getEmailSent())
                      .emailSentDate(invoice.getEmailSentDate())
                      .authorizationSri(invoice.getAuthorizationSri())
                      .invoiceDetails(invoice.getInvoiceDetails())
                      .invoicePayments(null)
                      .creationDate(invoice.getCreationDate())
                      .updateDate(invoice.getUpdateDate())
                      .status(invoice.getStatus())
                      .clientName(clientDto != null ? clientDto.getName() : null)
                      .clientIdentification(
                          clientDto != null ? clientDto.getIdentification() : null)
                      .availableAmount(availableAmount)
                      .build();
                })
            .collect(Collectors.toList());

    log.info(
        "✅ [INVOICE] Búsqueda por compañía y fechas completada - Total: {}, Página: {}, Tamaño: {}",
        invoicesPage.getTotalElements(),
        invoicesPage.getNumber(),
        invoicesPage.getSize());

    return new PageImpl<>(responseDtos, pageable, invoicesPage.getTotalElements());
  }

  @Override
  public List<InvoiceBasicInfoDto> searchAuthorizedActiveInvoicesByNumberOrAccessKey(
      InvoiceSearchByNumberOrAccessKeyDto searchDto) {
    log.info(
        "🔍 [INVOICE] Buscando facturas autorizadas y activas - idCompany: {}, invoiceNumber: {}, accessKey: {}",
        searchDto.getIdCompany(),
        searchDto.getInvoiceNumber(),
        searchDto.getAccessKey());

    // Validar que se proporcione al menos un criterio de búsqueda
    if (!StringUtils.hasText(searchDto.getInvoiceNumber())
        && !StringUtils.hasText(searchDto.getAccessKey())) {
      throw new IllegalArgumentException(
          "Se debe proporcionar al menos uno de los siguientes criterios: invoiceNumber o accessKey");
    }

    // Buscar facturas autorizadas y activas
    List<Invoice> invoices =
        repo.findAuthorizedActiveInvoicesByNumberOrAccessKey(
            searchDto.getIdCompany(), searchDto.getInvoiceNumber(), searchDto.getAccessKey());

    // Convertir a DTO con productos
    List<InvoiceBasicInfoDto> result = new ArrayList<>();
    for (Invoice invoice : invoices) {
      String accessKey = null;
      String authorizationNumber = null;
      if (invoice.getAuthorizationSri() != null) {
        accessKey = invoice.getAuthorizationSri().getAccessKey();
        authorizationNumber = invoice.getAuthorizationSri().getAuthorizationNumber();
      }

      // Cargar detalles de la factura (si no están cargados)
      List<InvoiceDetail> details = invoice.getInvoiceDetails();
      if (details == null || details.isEmpty()) {
        // Recargar la factura con detalles
        Invoice invoiceWithDetails = repo.findById(invoice.getIdInvoice()).orElse(invoice);
        details = invoiceWithDetails.getInvoiceDetails();
      }

      // Mapear detalles a ProductInfoDto
      List<com.wallparisoft.ebillbilling.dto.ProductInfoDto> products = new ArrayList<>();
      if (details != null && !details.isEmpty()) {
        for (InvoiceDetail detail : details) {
          ProductDto productDto = null;
          String mainCode = null;
          String auxiliarCode = null;
          String name = null;

          // Obtener datos del producto
          if (detail.getIdProduct() != null) {
            productDto = getProductFromService(detail.getIdProduct());
            if (productDto != null) {
              mainCode = productDto.getMainCode();
              auxiliarCode = productDto.getAuxiliarCode();
              name =
                  productDto.getName() != null ? productDto.getName() : productDto.getDescription();
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
          InvoiceBasicInfoDto.builder()
              .accessKey(accessKey)
              .authorizationNumber(authorizationNumber)
              .issueDate(invoice.getIssueDate())
              .invoiceNumber(invoice.getInvoiceNumber())
              .products(products)
              .build());
    }

    log.info(
        "✅ [INVOICE] Búsqueda completada - Encontradas {} facturas autorizadas y activas",
        result.size());

    return result;
  }
}
