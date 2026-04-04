package com.wallparisoft.ebillbilling.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import com.wallparisoft.ebillbilling.config.SriConfiguration;
import com.wallparisoft.ebillbilling.dto.CarrierDto;
import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.dto.GuideDetailDto;
import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.dto.GuideListResponseDto;
import com.wallparisoft.ebillbilling.dto.GuideSearchByCompanyDto;
import com.wallparisoft.ebillbilling.dto.GuideSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Carrier;
import com.wallparisoft.ebillbilling.entity.Guide;
import com.wallparisoft.ebillbilling.entity.GuideAdditionalInformation;
import com.wallparisoft.ebillbilling.mapper.GuideServiceMapper;
import com.wallparisoft.ebillbilling.repository.ICarrierRepo;
import com.wallparisoft.ebillbilling.repository.IGuideRepo;
import com.wallparisoft.ebillbilling.response.DigitalSignatureResponse;
import com.wallparisoft.ebillbilling.response.GuideResponse;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.DigitalSignatureClient;
import com.wallparisoft.ebillbilling.service.GuideDeliveryService;
import com.wallparisoft.ebillbilling.service.IGuideService;
import com.wallparisoft.ebillbilling.service.ISendGuideService;
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
public class GuideServiceImpl implements IGuideService {

  private final IGuideRepo repo;
  private final ICarrierRepo carrierRepo;
  private final GuideServiceMapper serviceMapper;
  private final GuideDeliveryService guideDeliveryService;
  private final CustomerClient customerClient;
  private final SriConfiguration sriConfiguration;
  private final ISendGuideService sendGuideService;
  private final DigitalSignatureClient digitalSignatureClient;
  private final ProductClient productClient;
  private final IAuthService authService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public Guide register(Guide obj) {
    return repo.save(obj);
  }

  @Override
  public Guide modify(Guide obj) {
    return repo.save(obj);
  }

  @Override
  public List<Guide> list() {
    return repo.findAll();
  }

  @Override
  public List<GuideListResponseDto> listWithClientData() {
    List<Guide> allGuides = repo.findAll();

    List<Guide> guides =
        allGuides.stream()
            .filter(
                g -> {
                  if (g.getAuthorizationSri() == null) {
                    return true;
                  }
                  String authStatus = g.getAuthorizationSri().getAuthorizationStatusSri();
                  return "AUTORIZADO".equalsIgnoreCase(authStatus);
                })
            .collect(Collectors.toList());

    Set<Long> uniqueClientIds =
        guides.stream()
            .map(Guide::getIdClient)
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

    Set<Long> carrierIds =
        guides.stream()
            .map(Guide::getIdCarrier)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, CarrierDto> carrierCache = new HashMap<>();
    for (Long idCarrier : carrierIds) {
      carrierRepo
          .findById(idCarrier)
          .ifPresent(
              carrier ->
                  carrierCache.put(
                      idCarrier,
                      CarrierDto.builder()
                          .idCarrier(carrier.getIdCarrier())
                          .name(carrier.getName())
                          .identification(carrier.getIdentification())
                          .build()));
    }

    return guides.stream()
        .map(
            g -> {
              ClientDto clientDto = clientCache.get(g.getIdClient());
              CarrierDto carrierDto = carrierCache.get(g.getIdCarrier());
              String guideStatus = calculateGuideStatus(g);

              return GuideListResponseDto.builder()
                  .idGuide(g.getIdGuide())
                  .idCompany(g.getIdCompany())
                  .idClient(g.getIdClient())
                  .idEstablishment(g.getIdEstablishment())
                  .idIssuancePoint(g.getIdIssuancePoint())
                  .sequential(g.getSequential())
                  .retryCount(g.getRetryCount())
                  .issueDate(g.getIssueDate())
                  .guideNumber(g.getGuideNumber())
                  .environment(g.getEnvironment())
                  .additionalInformation(g.getAdditionalInformation())
                  .emailSent(g.getEmailSent())
                  .emailSentDate(g.getEmailSentDate())
                  .startDate(g.getStartDate())
                  .endDate(g.getEndDate())
                  .transportType(g.getTransportType())
                  .transportPlate(g.getTransportPlate())
                  .idCarrier(g.getIdCarrier())
                  .originAddress(g.getOriginAddress())
                  .destinationAddress(g.getDestinationAddress())
                  .reasonForTransfer(g.getReasonForTransfer())
                  .route(g.getRoute())
                  .creationDate(g.getCreationDate())
                  .updateDate(g.getUpdateDate())
                  .status(g.getStatus())
                  .buyerName(clientDto != null ? clientDto.getName() : null)
                  .buyerIdentification(clientDto != null ? clientDto.getIdentification() : null)
                  .carrierName(carrierDto != null ? carrierDto.getName() : null)
                  .carrierIdentification(carrierDto != null ? carrierDto.getIdentification() : null)
                  .guideStatus(guideStatus)
                  .build();
            })
        .collect(Collectors.toList());
  }

  private String calculateGuideStatus(Guide g) {
    if (g.getAuthorizationSri() == null) {
      return "BORRADOR";
    }
    String authStatus = g.getAuthorizationSri().getAuthorizationStatusSri();
    if ("AUTORIZADO".equalsIgnoreCase(authStatus)) {
      return "AUTORIZADO";
    }
    String receptionStatus = g.getAuthorizationSri().getReceptionStatusSri();
    if ("RECIBIDA".equalsIgnoreCase(receptionStatus)
        && !"AUTORIZADO".equalsIgnoreCase(authStatus)) {
      return "EN_PROCESO";
    }
    return "DEVUELTA";
  }

  @Override
  public Guide getById(Integer id) {
    return repo.findById(Long.valueOf(id)).orElse(null);
  }

  @Override
  public boolean delete(Integer id) {
    Guide guide = repo.findById(Long.valueOf(id)).orElse(null);
    if (guide != null) {
      if (guide.getAuthorizationSri() == null) {
        repo.delete(guide);
        return true;
      } else {
        throw new IllegalStateException(
            "No se puede eliminar una guía de remisión autorizada. Solo se pueden eliminar borradores.");
      }
    }
    return false;
  }

  @Override
  public GuideResponse saveGuide(GuideDto guideDto) {
    validateGuideDto(guideDto);

    GuideResponse response =
        GuideResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.name())
            .build();

    Long idIssuancePoint = extractIssuancePointId(guideDto);
    log.debug("idIssuancePoint extraído: {}", idIssuancePoint);

    if (Objects.nonNull(idIssuancePoint)) {
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;

      guideDto.setIdIssuancePoint(idIssuancePoint);
      guideDto.setSequential(nextSequential);

      Guide guide = serviceMapper.toGuide(guideDto);
      guide.setSequential(nextSequential);
      guide.setIdIssuancePoint(idIssuancePoint);
      guide.setRetryCount(0);
      guide.setIssueDate(LocalDateTime.now());

      // Guardar información adicional si existe
      parseAndSetAdditionalInformation(guide, guideDto.getAdditionalInformation());

      // Asignar environment desde configuración del backend
      String environmentName = sriConfiguration.getEnvironmentName();
      guide.setEnvironment(environmentName);
      guideDto.setEnvironment(environmentName);
      log.debug("📝 [GR] Environment asignado desde configuración: {}", environmentName);

      String formattedSequential = String.format("%09d", nextSequential);
      if (guideDto.getEstablishment() != null
          && guideDto.getEstablishment().getIssuancePoint() != null) {
        String estab =
            guideDto.getEstablishment().getCode() != null
                ? guideDto.getEstablishment().getCode()
                : "001";
        String ptoEmi =
            guideDto.getEstablishment().getIssuancePoint().getCode() != null
                ? guideDto.getEstablishment().getIssuancePoint().getCode()
                : "001";
        String guideNumber = String.format("%s-%s-%s", estab, ptoEmi, formattedSequential);
        guide.setGuideNumber(guideNumber);
        guideDto.setGuideNumber(guideNumber);
        log.info("📝 [GR] guideNumber generado: {}", guideNumber);
      }

      try {
        DigitalSignatureResponse digitalSignatureResponse =
            digitalSignatureClient.getDigitalSignatureByCompanyId(
                guideDto.getCompanyDto().getIdCompany());
        if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
          log.error("There is no digital signature configuration");
          response = serviceMapper.toGuideResponse(register(guide));
          response.setCode(HttpStatus.BAD_REQUEST.value());
          response.setStatus(HttpStatus.BAD_REQUEST.name());
          response.setMessage(
              "La firma digital caduca y/o no esta configurada comuníquese con el administrador, la guía se registro en borrador.");
          return response;
        }

        log.info("🚀 Enviando Guía de Remisión al SRI...");
        AuthorizationSri authorizationSri =
            sendGuideService.sendGuideSri(
                guideDto, digitalSignatureResponse.getDigitalSignatureDto(), null);

        log.info(
            "✅ Respuesta SRI recibida: receptionStatus={}, authorizationStatus={}, accessKey={}, authNumber={}",
            authorizationSri != null ? authorizationSri.getReceptionStatusSri() : "null",
            authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "null",
            authorizationSri != null ? authorizationSri.getAccessKey() : "null",
            authorizationSri != null ? authorizationSri.getAuthorizationNumber() : "null");

        // Validar si la respuesta del SRI fue exitosa antes de asignar authorizationSri y
        // guideNumber
        boolean isAuthorized =
            Objects.nonNull(authorizationSri)
                && StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
                && "AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri());
        boolean isReceived =
            Objects.nonNull(authorizationSri)
                && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
                && "RECIBIDA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri());

        // Solo asignar authorizationSri y guideNumber si está AUTORIZADO
        // Si está DEVUELTA (excepto código 43 que se maneja como RECIBIDA), no se guardan estos
        // campos
        if (isAuthorized && isReceived) {
          guide.setAuthorizationSri(authorizationSri);
          setGuideNumberFromAuthorizationXml(guide, authorizationSri);
          Guide savedGuide = register(guide);
          response = serviceMapper.toGuideResponse(savedGuide);
          response.setCode(HttpStatus.OK.value());
          response.setStatus(HttpStatus.OK.name());
          notifyAuthorizedGuide(guideDto, savedGuide, authorizationSri);
        } else {
          // Si hay error DEVUELTA o no está autorizado, guardar como borrador sin authorizationSri
          // ni guideNumber
          // Limpiar guideNumber si ya se había establecido
          guide.setGuideNumber(null);
          guide.setAuthorizationSri(null);
          Guide savedGuide = register(guide);
          response = serviceMapper.toGuideResponse(savedGuide);
          response.setCode(HttpStatus.BAD_REQUEST.value());
          response.setStatus(HttpStatus.BAD_REQUEST.name());
          String errorMessage =
              authorizationSri != null
                      && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
                      && "DEVUELTA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri())
                  ? "El SRI devolvió la guía con errores. La guía se registró como borrador para corrección."
                  : "Hubo problemas al procesar la guía con el SRI. La guía se registró como borrador.";
          response.setMessage(errorMessage);
          log.warn(
              "⚠️ [GR] Guía devuelta o no autorizada. Se guardó como borrador sin guideNumber ni authorizationSri. ReceptionStatus: {}, AuthorizationStatus: {}",
              authorizationSri != null ? authorizationSri.getReceptionStatusSri() : "null",
              authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "null");
        }

      } catch (ResourceAccessException e) {
        response = serviceMapper.toGuideResponse(register(guide));
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        response.setMessage(
            "Error al obtener la firma digital, comuníquese con el administrador, la guía se registro en borrador.");
      } catch (CertificateException
          | SAXException
          | IOException
          | JAXBException
          | InterruptedException e) {
        response = serviceMapper.toGuideResponse(register(guide));
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        response.setMessage(
            "Hubo problemas para conectarse con el SRI, la guía se registro en borrador.");
      }

    } else {
      Guide guide = serviceMapper.toGuide(guideDto);
      guide.setRetryCount(0);
      // Guardar información adicional si existe
      parseAndSetAdditionalInformation(guide, guideDto.getAdditionalInformation());
      Guide savedGuide = register(guide);
      response = serviceMapper.toGuideResponse(savedGuide);
      response.setCode(HttpStatus.OK.value());
      response.setStatus(HttpStatus.OK.name());
      response.setMessage("Guía de remisión guardada como borrador (sin punto de emisión)");
    }

    return response;
  }

  @Override
  public GuideResponse updateGuide(Long id, GuideDto guideDto) {
    Guide existingGuide = repo.findById(id).orElse(null);
    if (existingGuide == null) {
      throw new IllegalArgumentException("Guía de remisión no encontrada con ID: " + id);
    }

    if (existingGuide.getAuthorizationSri() != null) {
      throw new IllegalStateException(
          "No se puede actualizar una guía de remisión autorizada. Solo se pueden actualizar borradores.");
    }

    // Preservar campos críticos del guide existente si no están presentes en el DTO
    if (guideDto.getIdCarrier() == null
        && (guideDto.getCarrierDto() == null || guideDto.getCarrierDto().getIdCarrier() == null)) {
      guideDto.setIdCarrier(existingGuide.getIdCarrier());
      log.debug("Preservando idCarrier existente: {}", existingGuide.getIdCarrier());
    }

    if (guideDto.getIdClient() == null
        && (guideDto.getClientDto() == null || guideDto.getClientDto().getIdClient() == null)) {
      guideDto.setIdClient(existingGuide.getIdClient());
      log.debug("Preservando idClient existente: {}", existingGuide.getIdClient());
    }

    validateGuideDto(guideDto);

    Guide guide = serviceMapper.toGuide(guideDto);
    guide.setIdGuide(id);
    guide.setRetryCount(existingGuide.getRetryCount());
    guide.setCreationDate(existingGuide.getCreationDate());

    // Guardar información adicional si existe
    parseAndSetAdditionalInformation(guide, guideDto.getAdditionalInformation());

    Guide savedGuide = modify(guide);
    GuideResponse response = serviceMapper.toGuideResponse(savedGuide);
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public GuideDto getGuideById(Long id) {
    Guide guide = repo.findById(id).orElse(null);
    if (guide == null) {
      return null;
    }

    GuideDto guideDto = serviceMapper.toGuideDto(guide);

    // Enriquecer con datos completos de compañía
    if (guide.getIdCompany() != null) {
      try {
        CompanyDto companyDto = customerClient.getCompanyById(guide.getIdCompany());
        if (companyDto != null) {
          guideDto.setCompanyDto(companyDto);
        }
      } catch (Exception e) {
        log.warn("Error loading company {}: {}", guide.getIdCompany(), e.getMessage());
      }
    }

    // Enriquecer con datos completos de cliente
    if (guide.getIdClient() != null) {
      try {
        ClientDtoResponse clientResponse = customerClient.getClientById(guide.getIdClient());
        if (clientResponse != null && !CollectionUtils.isEmpty(clientResponse.getClientDtos())) {
          guideDto.setClientDto(clientResponse.getClientDtos().get(0));
        }
      } catch (Exception e) {
        log.warn("Error loading client {}: {}", guide.getIdClient(), e.getMessage());
      }
    }

    // Enriquecer con datos completos de transportista
    if (guide.getIdCarrier() != null) {
      try {
        carrierRepo
            .findById(guide.getIdCarrier())
            .ifPresent(
                carrier -> {
                  CarrierDto carrierDto =
                      CarrierDto.builder()
                          .idCarrier(carrier.getIdCarrier())
                          .idCompany(carrier.getIdCompany())
                          .identificationType(carrier.getIdentificationType())
                          .identification(carrier.getIdentification())
                          .name(carrier.getName())
                          .address(carrier.getAddress())
                          .phone(carrier.getPhone())
                          .email(carrier.getEmail())
                          .licenseNumber(carrier.getLicenseNumber())
                          .vehicleType(carrier.getVehicleType())
                          .status(carrier.getStatus())
                          .build();
                  guideDto.setCarrierDto(carrierDto);
                });
      } catch (Exception e) {
        log.warn("Error loading carrier {}: {}", guide.getIdCarrier(), e.getMessage());
      }
    }

    // Enriquecer con datos completos de establecimiento
    if (guide.getIdIssuancePoint() != null) {
      try {
        EstablishmentDto establishmentDto =
            customerClient.getEstablishmentWithIssuancePoint(guide.getIdIssuancePoint());
        if (establishmentDto != null) {
          guideDto.setEstablishment(establishmentDto);
        }
      } catch (Exception e) {
        log.warn(
            "Error loading establishment for issuancePoint {}: {}",
            guide.getIdIssuancePoint(),
            e.getMessage());
      }
    }

    return guideDto;
  }

  @Override
  public Page<Guide> searchGuides(
      String guideNumber, Long idClient, Long idCarrier, Pageable pageable) {
    // Implementación básica - puede mejorarse con consultas más complejas
    if (StringUtils.hasText(guideNumber)) {
      return repo.findByGuideNumber(guideNumber)
          .map(
              guide -> {
                List<Guide> guideList = List.of(guide);
                return new org.springframework.data.domain.PageImpl<>(
                    guideList, pageable, guideList.size());
              })
          .orElse(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));
    }
    return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
  }

  @Override
  public GuideResponse saveGuideDraft(GuideDto guideDto) {
    validateGuideDto(guideDto);

    Guide guide = serviceMapper.toGuide(guideDto);
    guide.setRetryCount(0);
    guide.setIssueDate(LocalDateTime.now());

    Guide savedGuide = register(guide);
    GuideResponse response = serviceMapper.toGuideResponse(savedGuide);
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    response.setMessage("Guía de remisión guardada como borrador");
    return response;
  }

  @Override
  public GuideResponse sendGuideToSri(Long idGuide, String password) {
    Guide guide = repo.findById(idGuide).orElse(null);
    if (guide == null) {
      throw new IllegalArgumentException("Guía de remisión no encontrada con ID: " + idGuide);
    }

    if (guide.getAuthorizationSri() != null) {
      throw new IllegalStateException(
          "Esta guía de remisión ya fue enviada al SRI. No se puede enviar nuevamente.");
    }

    GuideDto guideDto = serviceMapper.toGuideDto(guide);
    if (guideDto == null) {
      throw new IllegalStateException("No se pudo convertir la guía a DTO");
    }

    // Asegurar que el secuencial esté asignado
    if (guide.getSequential() == null && guide.getIdIssuancePoint() != null) {
      Long maxSequential = repo.findMaxSequentialByIssuancePoint(guide.getIdIssuancePoint());
      Long nextSequential = (Objects.nonNull(maxSequential) ? maxSequential : 0L) + 1;
      guide.setSequential(nextSequential);
      guideDto.setSequential(nextSequential);
      log.info("📝 [GR] Secuencial asignado: {}", nextSequential);
    }

    // Enriquecer el DTO con datos completos del establecimiento (address, type, etc.)
    if (guide.getIdIssuancePoint() != null) {
      try {
        log.debug(
            "Cargando establecimiento completo para idIssuancePoint: {}",
            guide.getIdIssuancePoint());
        EstablishmentDto establishmentDto =
            customerClient.getEstablishmentWithIssuancePoint(guide.getIdIssuancePoint());
        if (establishmentDto != null) {
          guideDto.setEstablishment(establishmentDto);
          log.debug(
              "Establecimiento cargado: code={}, type={}, address={}, issuancePoint.code={}",
              establishmentDto.getCode(),
              establishmentDto.getType(),
              establishmentDto.getAddress(),
              establishmentDto.getIssuancePoint() != null
                  ? establishmentDto.getIssuancePoint().getCode()
                  : "null");
        } else {
          log.warn(
              "No se encontró el establecimiento para el punto de emisión: {}",
              guide.getIdIssuancePoint());
        }
      } catch (Exception e) {
        log.error(
            "Error loading establishment for idIssuancePoint {}: {}",
            guide.getIdIssuancePoint(),
            e.getMessage());
      }
    }

    // Actualizar el secuencial en el issuancePoint
    if (guideDto.getEstablishment() != null
        && guideDto.getEstablishment().getIssuancePoint() != null
        && guideDto.getSequential() != null) {
      String formattedSequential = String.format("%09d", guideDto.getSequential());
      guideDto.getEstablishment().getIssuancePoint().setSequentialInvoice(formattedSequential);
      log.debug("📝 [GR] Secuencial actualizado en establishment: {}", formattedSequential);
    }

    try {
      DigitalSignatureResponse digitalSignatureResponse =
          digitalSignatureClient.getDigitalSignatureByCompanyId(guide.getIdCompany());
      if (Objects.isNull(digitalSignatureResponse.getDigitalSignatureDto())) {
        GuideResponse errorResponse =
            GuideResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .status(HttpStatus.BAD_REQUEST.name())
                .message("La firma digital no está configurada")
                .build();
        return errorResponse;
      }

      // Actualizar password si se proporciona
      if (StringUtils.hasText(password)) {
        digitalSignatureResponse.getDigitalSignatureDto().setPassword(password);
      }

      log.info("🚀 Enviando Guía de Remisión {} al SRI...", idGuide);
      AuthorizationSri authorizationSri =
          sendGuideService.sendGuideSri(
              guideDto, digitalSignatureResponse.getDigitalSignatureDto(), null);

      // Validar si la respuesta del SRI fue exitosa antes de asignar authorizationSri y guideNumber
      boolean isAuthorized =
          Objects.nonNull(authorizationSri)
              && StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
              && "AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri());
      boolean isReceived =
          Objects.nonNull(authorizationSri)
              && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
              && "RECIBIDA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri());

      // Solo asignar authorizationSri y guideNumber si está AUTORIZADO
      // Si está DEVUELTA (excepto código 43 que se maneja como RECIBIDA), no se guardan estos
      // campos
      if (isAuthorized && isReceived) {
        guide.setAuthorizationSri(authorizationSri);
        setGuideNumberFromAuthorizationXml(guide, authorizationSri);
        Guide savedGuide = modify(guide);
        GuideResponse response = serviceMapper.toGuideResponse(savedGuide);
        response.setCode(HttpStatus.OK.value());
        response.setStatus(HttpStatus.OK.name());
        notifyAuthorizedGuide(guideDto, savedGuide, authorizationSri);
        return response;
      } else {
        // Si hay error DEVUELTA o no está autorizado, guardar como borrador sin authorizationSri ni
        // guideNumber
        // Limpiar guideNumber si ya se había establecido
        guide.setGuideNumber(null);
        guide.setAuthorizationSri(null);
        Guide savedGuide = modify(guide);
        GuideResponse response = serviceMapper.toGuideResponse(savedGuide);
        response.setCode(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.BAD_REQUEST.name());
        String errorMessage =
            authorizationSri != null
                    && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
                    && "DEVUELTA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri())
                ? "El SRI devolvió la guía con errores. La guía se registró como borrador para corrección."
                : "Hubo problemas al procesar la guía con el SRI. La guía se registró como borrador.";
        response.setMessage(errorMessage);
        log.warn(
            "⚠️ [GR] Guía devuelta o no autorizada. Se guardó como borrador sin guideNumber ni authorizationSri. ReceptionStatus: {}, AuthorizationStatus: {}",
            authorizationSri != null ? authorizationSri.getReceptionStatusSri() : "null",
            authorizationSri != null ? authorizationSri.getAuthorizationStatusSri() : "null");
        return response;
      }

    } catch (Exception e) {
      log.error("Error enviando guía al SRI: {}", e.getMessage(), e);
      GuideResponse errorResponse =
          GuideResponse.builder()
              .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
              .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
              .message("Error al enviar guía al SRI: " + e.getMessage())
              .build();
      return errorResponse;
    }
  }

  @Override
  public Path getPdfFilePath(Long idGuide) {
    Guide guide = repo.findById(idGuide).orElse(null);
    if (guide == null || guide.getAuthorizationSri() == null) {
      return null;
    }

    String storagePath = guide.getAuthorizationSri().getAuthorizationStoragePath();
    if (!StringUtils.hasText(storagePath)) {
      return null;
    }

    // Usar el número de guía (formato: 001-001-000000025) en lugar de la clave de acceso
    String guideNumber = guide.getGuideNumber();
    if (!StringUtils.hasText(guideNumber)) {
      return null;
    }

    Path pdfPath = Paths.get(storagePath).resolve(guideNumber + ".pdf");
    if (Files.exists(pdfPath)) {
      return pdfPath;
    }

    return null;
  }

  @Override
  public Path getXmlFilePath(Long idGuide) {
    Guide guide = repo.findById(idGuide).orElse(null);
    if (guide == null || guide.getAuthorizationSri() == null) {
      return null;
    }

    String storagePath = guide.getAuthorizationSri().getAuthorizationStoragePath();
    if (!StringUtils.hasText(storagePath)) {
      return null;
    }

    String accessKey = guide.getAuthorizationSri().getAccessKey();
    if (!StringUtils.hasText(accessKey)) {
      return null;
    }

    Path xmlPath = Paths.get(storagePath).resolve(accessKey + ".xml");
    if (Files.exists(xmlPath)) {
      return xmlPath;
    }

    return null;
  }

  @Override
  public boolean sendGuideByEmail(Long idGuide, String email) {
    // TODO: Implementar cuando esté completo el flujo de delivery
    Guide guide = repo.findById(idGuide).orElse(null);
    if (guide == null || guide.getAuthorizationSri() == null) {
      return false;
    }

    // El delivery service se encarga de enviar el email
    // Por ahora, retornar false indicando que no está implementado
    log.warn("Envío de guía por email pendiente de implementación completa");
    return false;
  }

  private void validateGuideDto(GuideDto guideDto) {
    if (guideDto == null) {
      throw new IllegalArgumentException("GuideDto no puede ser null");
    }

    // Validar companyDto
    if (guideDto.getCompanyDto() == null) {
      throw new IllegalArgumentException("Los datos de la empresa (companyDto) son obligatorios");
    }

    if (guideDto.getCompanyDto().getIdCompany() == null) {
      throw new IllegalArgumentException("El idCompany es obligatorio");
    }

    // Validar clientDto
    if (guideDto.getClientDto() == null) {
      throw new IllegalArgumentException("Los datos del cliente (clientDto) son obligatorios");
    }

    if (guideDto.getClientDto().getIdClient() == null) {
      throw new IllegalArgumentException("El idClient es obligatorio");
    }

    // Validar carrierDto o idCarrier
    if (guideDto.getCarrierDto() == null && guideDto.getIdCarrier() == null) {
      throw new IllegalArgumentException(
          "Se debe especificar un transportista (carrierDto o idCarrier)");
    }

    // Validar establishment
    if (guideDto.getEstablishment() == null) {
      throw new IllegalArgumentException("El establecimiento (establishment) es obligatorio");
    }

    if (guideDto.getEstablishment().getIdEstablishment() == null) {
      throw new IllegalArgumentException("El idEstablishment es obligatorio");
    }

    if (guideDto.getEstablishment().getIssuancePoint() == null) {
      throw new IllegalArgumentException("El punto de emisión (issuancePoint) es obligatorio");
    }

    if (guideDto.getEstablishment().getIssuancePoint().getIdIssuancePoint() == null) {
      throw new IllegalArgumentException("El idIssuancePoint es obligatorio");
    }

    // Validar fechas
    if (guideDto.getIssueDate() == null) {
      throw new IllegalArgumentException("La fecha de emisión (issueDate) es obligatoria");
    }

    if (guideDto.getStartDate() == null) {
      throw new IllegalArgumentException(
          "La fecha de inicio de traslado (startDate) es obligatoria");
    }

    if (guideDto.getEndDate() == null) {
      throw new IllegalArgumentException("La fecha de fin de traslado (endDate) es obligatoria");
    }

    // Validar que endDate >= startDate
    if (guideDto.getEndDate().isBefore(guideDto.getStartDate())) {
      throw new IllegalArgumentException(
          "La fecha de fin de traslado debe ser mayor o igual a la fecha de inicio");
    }

    // Validar direcciones
    if (!StringUtils.hasText(guideDto.getOriginAddress())) {
      throw new IllegalArgumentException("La dirección de origen es obligatoria");
    }

    if (guideDto.getOriginAddress().length() > 300) {
      throw new IllegalArgumentException("La dirección de origen no puede exceder 300 caracteres");
    }

    if (!StringUtils.hasText(guideDto.getDestinationAddress())) {
      throw new IllegalArgumentException("La dirección de destino es obligatoria");
    }

    if (guideDto.getDestinationAddress().length() > 300) {
      throw new IllegalArgumentException("La dirección de destino no puede exceder 300 caracteres");
    }

    // Validar guideDetails
    if (guideDto.getGuideDetails() == null || guideDto.getGuideDetails().isEmpty()) {
      throw new IllegalArgumentException(
          "La guía de remisión debe tener al menos un detalle (producto)");
    }

    // Validar cada detalle
    for (int i = 0; i < guideDto.getGuideDetails().size(); i++) {
      var detail = guideDto.getGuideDetails().get(i);
      if (detail.getIdProduct() == null) {
        throw new IllegalArgumentException(
            String.format("El idProduct es obligatorio en el detalle %d", i + 1));
      }
      if (detail.getQuantity() == null || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            String.format(
                "La cantidad debe ser mayor a cero en el detalle %d (producto: %d)",
                i + 1, detail.getIdProduct()));
      }
    }
  }

  private Long extractIssuancePointId(GuideDto guideDto) {
    if (guideDto.getEstablishment() != null
        && guideDto.getEstablishment().getIssuancePoint() != null) {
      return guideDto.getEstablishment().getIssuancePoint().getIdIssuancePoint();
    }
    return guideDto.getIdIssuancePoint();
  }

  private void setGuideNumberFromAuthorizationXml(Guide guide, AuthorizationSri authorizationSri) {
    if (authorizationSri == null
        || !"AUTORIZADO".equals(authorizationSri.getAuthorizationStatusSri())
        || !StringUtils.hasText(authorizationSri.getAuthorizationXml())) {
      return;
    }

    try {
      if (guide.getSequential() != null && guide.getIdIssuancePoint() != null) {
        try {
          EstablishmentDto establishmentDto =
              customerClient.getEstablishmentWithIssuancePoint(guide.getIdIssuancePoint());
          if (establishmentDto != null
              && establishmentDto.getIssuancePoint() != null
              && StringUtils.hasText(establishmentDto.getCode())
              && StringUtils.hasText(establishmentDto.getIssuancePoint().getCode())) {
            String guideNumber =
                String.format(
                    "%s-%s-%09d",
                    establishmentDto.getCode(),
                    establishmentDto.getIssuancePoint().getCode(),
                    guide.getSequential());
            guide.setGuideNumber(guideNumber);
          }
        } catch (Exception e) {
          log.warn("Error loading establishment for guide number: {}", e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Error extracting guide number from authorization XML: {}", e.getMessage());
    }
  }

  private void notifyAuthorizedGuide(
      GuideDto guideDto, Guide savedGuide, AuthorizationSri authorizationSri) {

    if (Boolean.TRUE.equals(savedGuide.getEmailSent())) {
      log.info(
          "Email already sent for guide {} on {}. Skipping notification.",
          savedGuide.getIdGuide(),
          savedGuide.getEmailSentDate());
      return;
    }

    if (Objects.isNull(authorizationSri)
        || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
        || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
      return;
    }

    if (!StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      log.warn(
          "Authorization XML path is missing for guide {}. Skipping email notification.",
          savedGuide.getIdGuide());
      return;
    }

    ClientDto clientDtoWithContacts = null;
    try {
      if (guideDto.getClientDto() != null && guideDto.getClientDto().getIdClient() != null) {
        clientDtoWithContacts =
            customerClient
                .getClientById(guideDto.getClientDto().getIdClient())
                .getClientDtos()
                .get(0);
        guideDto.setClientDto(clientDtoWithContacts);
      }
    } catch (Exception e) {
      log.warn("Error loading client for guide notification: {}", e.getMessage());
    }

    // Cargar CompanyDto completo con logo, regime, etc.
    try {
      if (guideDto.getCompanyDto() != null && guideDto.getCompanyDto().getIdCompany() != null) {
        CompanyDto companyDtoComplete =
            customerClient.getCompanyById(guideDto.getCompanyDto().getIdCompany());
        if (companyDtoComplete != null) {
          guideDto.setCompanyDto(companyDtoComplete);
          log.debug(
              "Loaded company data for guide notification, companyId: {}, logoPath: {}",
              companyDtoComplete.getIdCompany(),
              companyDtoComplete.getLogoPath());
        }
      }
    } catch (Exception e) {
      log.warn("Error loading company for guide notification: {}", e.getMessage());
    }

    guideDeliveryService.deliverAuthorizedGuide(guideDto, savedGuide, authorizationSri);

    // El delivery service se encarga de marcar emailSent
  }

  @Override
  public Page<GuideSearchResponseDto> searchGuidesByCompany(
      GuideSearchByCompanyDto searchDto, Pageable pageable) {
    log.info(
        "🔍 [GR] Buscando guías de remisión - idCompany: {}, guideNumber: {}, page: {}, size: {}",
        searchDto.getIdCompany(),
        searchDto.getGuideNumber(),
        pageable.getPageNumber(),
        pageable.getPageSize());

    if (searchDto.getIdCompany() == null) {
      throw new IllegalArgumentException("idCompany es obligatorio");
    }

    Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Guide> guidesPage =
        repo.findGuidesByCompanyAndFilters(
            searchDto.getIdCompany(), searchDto.getGuideNumber(), unsortedPageable);

    // Pre-cargar datos de clientes (destinatarios)
    Set<Long> uniqueClientIds =
        guidesPage.getContent().stream()
            .map(Guide::getIdClient)
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

    // Pre-cargar datos de transportistas (carriers)
    Set<Long> uniqueCarrierIds =
        guidesPage.getContent().stream()
            .map(Guide::getIdCarrier)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, Carrier> carrierCache = new HashMap<>();
    for (Long idCarrier : uniqueCarrierIds) {
      carrierRepo.findById(idCarrier).ifPresent(carrier -> carrierCache.put(idCarrier, carrier));
    }

    // Mapear a DTO
    List<GuideSearchResponseDto> responseDtos =
        guidesPage.getContent().stream()
            .map(
                guide -> {
                  ClientDto clientDto = clientCache.get(guide.getIdClient());
                  Carrier carrier = carrierCache.get(guide.getIdCarrier());

                  String authorizationStatus = null;
                  if (guide.getAuthorizationSri() != null) {
                    authorizationStatus = guide.getAuthorizationSri().getAuthorizationStatusSri();
                  }

                  return GuideSearchResponseDto.builder()
                      .idGuide(guide.getIdGuide())
                      .guideNumber(guide.getGuideNumber())
                      .createDate(guide.getCreationDate())
                      .destinatarioNombre(clientDto != null ? clientDto.getName() : null)
                      .carrierIdentification(carrier != null ? carrier.getIdentification() : null)
                      .carrierName(carrier != null ? carrier.getName() : null)
                      .authorizationStatus(authorizationStatus)
                      .build();
                })
            .collect(Collectors.toList());

    return new PageImpl<>(responseDtos, pageable, guidesPage.getTotalElements());
  }

  @Override
  public void executeSendGuideToSri() {
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
      List<Guide> guides =
          this.repo.findGuidesExcludingSpecificSriStatuses("RECIBIDA", "AUTORIZADO");
      log.info("Found {} guides pending to send to SRI", guides.size());

      // ===== OPTIMIZACIÓN: Pre-cargar datos únicos para evitar consultas duplicadas =====
      // 1. Extraer IDs únicos de companies, clients, issuance points, carriers y products
      Set<Long> uniqueCompanyIds =
          guides.stream()
              .map(Guide::getIdCompany)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      Set<Long> uniqueClientIds =
          guides.stream()
              .map(Guide::getIdClient)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      Set<Long> uniqueIssuancePointIds =
          guides.stream()
              .map(Guide::getIdIssuancePoint)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      Set<Long> uniqueCarrierIds =
          guides.stream()
              .map(Guide::getIdCarrier)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      Set<Long> uniqueProductIds =
          guides.stream()
              .filter(g -> Objects.nonNull(g.getGuideDetails()))
              .flatMap(g -> g.getGuideDetails().stream())
              .map(com.wallparisoft.ebillbilling.entity.GuideDetail::getIdProduct)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      log.info(
          "Found {} unique companies, {} unique clients, {} unique issuance points, {} unique carriers, and {} unique products to process",
          uniqueCompanyIds.size(),
          uniqueClientIds.size(),
          uniqueIssuancePointIds.size(),
          uniqueCarrierIds.size(),
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

      // 6. Pre-cargar transportistas (una consulta por carrier)
      Map<Long, CarrierDto> carrierCache = new HashMap<>();
      for (Long carrierId : uniqueCarrierIds) {
        try {
          carrierRepo
              .findById(carrierId)
              .ifPresent(
                  carrier -> {
                    CarrierDto carrierDto =
                        CarrierDto.builder()
                            .idCarrier(carrier.getIdCarrier())
                            .idCompany(carrier.getIdCompany())
                            .identificationType(carrier.getIdentificationType())
                            .identification(carrier.getIdentification())
                            .name(carrier.getName())
                            .address(carrier.getAddress())
                            .phone(carrier.getPhone())
                            .email(carrier.getEmail())
                            .licenseNumber(carrier.getLicenseNumber())
                            .vehicleType(carrier.getVehicleType())
                            .status(carrier.getStatus())
                            .build();
                    carrierCache.put(carrierId, carrierDto);
                    log.debug("Cached carrier data for carrier {}", carrierId);
                  });
        } catch (Exception e) {
          log.error("Error loading carrier {}: {}", carrierId, e.getMessage());
        }
      }

      // 7. Pre-cargar productos (una consulta por product)
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

      // 8. Procesar guías usando el cache
      guides.forEach(
          guide -> {
            // Inicializar retryCount si es null
            if (Objects.isNull(guide.getRetryCount())) {
              guide.setRetryCount(0);
            }

            // Verificar si ya se agotaron los reintentos
            if (guide.getRetryCount() >= 5) {
              log.warn(
                  "Guide {} has exceeded maximum retries ({}), marking as inactive",
                  guide.getIdGuide(),
                  guide.getRetryCount());
              deactivateGuideAndDetails(guide);
              return;
            }

            try {
              // Usar cache en lugar de consultar cada vez
              com.wallparisoft.ebillbilling.dto.DigitalSignatureDto digitalSignatureDto =
                  digitalSignatureCache.get(guide.getIdCompany());

              if (Objects.isNull(digitalSignatureDto)) {
                log.warn(
                    "Digital signature not found in cache for company {} - Guide {}",
                    guide.getIdCompany(),
                    guide.getIdGuide());
                handleGuideFailure(guide, "Digital signature not found");
                return;
              }

              GuideDto guideDtoForSri = serviceMapper.toGuideDto(guide);

              // Usar cache para compañía si está disponible
              CompanyDto companyDtoFromCache = companyCache.get(guide.getIdCompany());
              if (Objects.nonNull(companyDtoFromCache)) {
                guideDtoForSri.setCompanyDto(companyDtoFromCache);
                log.debug("Assigned company to guide {} from cache", guide.getIdGuide());
              } else {
                log.warn(
                    "Company not found in cache for company {} - Guide {}",
                    guide.getIdCompany(),
                    guide.getIdGuide());
                handleGuideFailure(guide, "Company not found");
                return;
              }

              // Usar cache para cliente si está disponible
              ClientDto clientDtoFromCache = clientCache.get(guide.getIdClient());
              if (Objects.nonNull(clientDtoFromCache)) {
                guideDtoForSri.setClientDto(clientDtoFromCache);
              }

              // Usar cache para transportista si está disponible
              CarrierDto carrierDtoFromCache = carrierCache.get(guide.getIdCarrier());
              if (Objects.nonNull(carrierDtoFromCache)) {
                guideDtoForSri.setCarrierDto(carrierDtoFromCache);
              }

              // Usar cache para establecimiento si está disponible
              if (Objects.nonNull(guide.getIdIssuancePoint())) {
                EstablishmentDto establishmentDto =
                    establishmentCache.get(guide.getIdIssuancePoint());
                if (Objects.nonNull(establishmentDto)) {
                  guideDtoForSri.setEstablishment(establishmentDto);
                  // Actualizar el sequential en el issuancePoint si está disponible en la guía
                  if (Objects.nonNull(guide.getSequential())
                      && Objects.nonNull(establishmentDto.getIssuancePoint())) {
                    String formattedSequential = String.format("%09d", guide.getSequential());
                    establishmentDto.getIssuancePoint().setSequentialInvoice(formattedSequential);
                  }
                  log.debug("Assigned establishment to guide {} from cache", guide.getIdGuide());
                } else {
                  log.warn(
                      "Establishment not found in cache for issuance point {} - Guide {}",
                      guide.getIdIssuancePoint(),
                      guide.getIdGuide());
                  handleGuideFailure(guide, "Establishment not found");
                  return;
                }
              } else {
                log.warn(
                    "Guide {} does not have idIssuancePoint - cannot load establishment",
                    guide.getIdGuide());
                handleGuideFailure(guide, "Guide does not have issuance point");
                return;
              }

              // Asignar ProductDto a cada GuideDetailDto desde el cache
              if (Objects.nonNull(guideDtoForSri.getGuideDetails())) {
                for (GuideDetailDto detailDto : guideDtoForSri.getGuideDetails()) {
                  if (Objects.nonNull(detailDto.getIdProduct())) {
                    ProductDto productDto = productCache.get(detailDto.getIdProduct());
                    if (Objects.nonNull(productDto)) {
                      detailDto.setProductDto(productDto);
                      log.debug(
                          "Assigned product {} to guide detail {} from cache",
                          detailDto.getIdProduct(),
                          detailDto.getIdGuideDetail());
                    } else {
                      log.warn(
                          "Product not found in cache for productId {} - Guide {} - Detail {}",
                          detailDto.getIdProduct(),
                          guide.getIdGuide(),
                          detailDto.getIdGuideDetail());
                      handleGuideFailure(guide, "Product not found for detail");
                      return;
                    }
                  } else {
                    log.warn(
                        "Guide detail {} does not have idProduct - Guide {}",
                        detailDto.getIdGuideDetail(),
                        guide.getIdGuide());
                    handleGuideFailure(guide, "Guide detail does not have product");
                    return;
                  }
                }
              }

              // Reutilizar AuthorizationSri existente si existe, para evitar crear registros
              // duplicados
              AuthorizationSri existingAuthorizationSri = guide.getAuthorizationSri();
              AuthorizationSri authorizationSri =
                  sendGuideService.sendGuideSri(
                      guideDtoForSri, digitalSignatureDto, existingAuthorizationSri);
              guide.setAuthorizationSri(authorizationSri);
              // Actualizar número de guía desde el XML de autorización si está AUTORIZADO
              setGuideNumberFromAuthorizationXml(guide, authorizationSri);

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
                repo.save(guide);
                // Notificar y enviar email si está autorizado
                notifyAuthorizedGuide(guideDtoForSri, guide, authorizationSri);
                log.info("Successfully processed guide {}", guide.getIdGuide());
              } else {
                // Si no fue exitoso, incrementar el contador de reintentos
                String failureReason =
                    buildFailureReason(authorizationSri, isAuthorized, isReceived);
                handleGuideFailure(guide, failureReason);
              }
            } catch (feign.FeignException.Unauthorized e) {
              log.error(
                  "Unauthorized access to digital signature service for guide {} (retry {}/5) - Scheduled tasks need authentication configuration",
                  guide.getIdGuide(),
                  guide.getRetryCount());
              handleGuideFailure(guide, "Unauthorized access to digital signature service");
            } catch (ResourceAccessException | RetryableException e) {
              log.error(
                  "Connection error to digital signature service for guide {} (retry {}/5): {}",
                  guide.getIdGuide(),
                  guide.getRetryCount(),
                  e.getMessage());
              handleGuideFailure(guide, "Connection error: " + e.getMessage());
            } catch (CertificateException
                | SAXException
                | IOException
                | JAXBException
                | InterruptedException e) {
              log.error(
                  "Process error for guide {} (retry {}/5): {}",
                  guide.getIdGuide(),
                  guide.getRetryCount(),
                  e.getMessage(),
                  e);
              handleGuideFailure(guide, "Process error: " + e.getMessage());
            } catch (Exception e) {
              log.error(
                  "Unexpected error processing guide {} (retry {}/5): {}",
                  guide.getIdGuide(),
                  guide.getRetryCount(),
                  e.getMessage(),
                  e);
              handleGuideFailure(guide, "Unexpected error: " + e.getMessage());
            }
          });
    } finally {
      // Restaurar el contexto de seguridad anterior
      SecurityContextHolder.setContext(previousContext);
      log.debug("Restored previous security context after scheduled task execution");
    }
  }

  /**
   * Maneja el fallo de una guía de remisión incrementando el contador de reintentos y marcándola
   * como inactiva si se agotan los reintentos
   */
  private void handleGuideFailure(Guide guide, String reason) {
    guide.setRetryCount(guide.getRetryCount() + 1);

    if (guide.getRetryCount() >= 5) {
      log.error(
          "Guide {} has exceeded maximum retries ({}) - Marking as inactive. Reason: {}",
          guide.getIdGuide(),
          guide.getRetryCount(),
          reason);
      deactivateGuideAndDetails(guide);
    } else {
      log.warn(
          "Guide {} retry {}/5. Reason: {}", guide.getIdGuide(), guide.getRetryCount(), reason);
      repo.save(guide);
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

  /** Marca la guía de remisión y todos sus detalles como inactivos (status = false) */
  private void deactivateGuideAndDetails(Guide guide) {
    guide.setStatus(false);

    // Marcar todos los detalles como inactivos
    if (Objects.nonNull(guide.getGuideDetails())) {
      guide.getGuideDetails().forEach(detail -> detail.setStatus(false));
    }

    repo.save(guide);
    log.info(
        "Guide {} and its {} details marked as inactive",
        guide.getIdGuide(),
        Objects.nonNull(guide.getGuideDetails()) ? guide.getGuideDetails().size() : 0);
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

      ProductDto.ProductDtoBuilder builder = ProductDto.builder();

      // Mapear campos básicos
      if (productMap.containsKey("idProduct")) {
        Object idProduct = productMap.get("idProduct");
        if (idProduct instanceof Number) {
          builder.idProduct(((Number) idProduct).longValue());
        }
      }

      if (productMap.containsKey("mainCode")) {
        builder.mainCode((String) productMap.get("mainCode"));
      }

      if (productMap.containsKey("name")) {
        builder.name((String) productMap.get("name"));
      }

      if (productMap.containsKey("rate")) {
        Object rate = productMap.get("rate");
        if (rate instanceof Number) {
          builder.rate(BigDecimal.valueOf(((Number) rate).doubleValue()));
        } else if (rate instanceof String) {
          builder.rate(new BigDecimal((String) rate));
        }
      }

      if (productMap.containsKey("iceRate")) {
        Object iceRate = productMap.get("iceRate");
        if (iceRate instanceof Number) {
          builder.iceRate(BigDecimal.valueOf(((Number) iceRate).doubleValue()));
        } else if (iceRate instanceof String) {
          builder.iceRate(new BigDecimal((String) iceRate));
        }
      }

      if (productMap.containsKey("status")) {
        Object status = productMap.get("status");
        if (status instanceof Boolean) {
          builder.status((Boolean) status);
        } else if (status instanceof String) {
          builder.status(Boolean.parseBoolean((String) status));
        }
      }

      return builder.build();
    } catch (Exception e) {
      log.error("Error mapping product DTO from service: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * Parsea la información adicional desde un string JSON y la asigna a la guía.
   *
   * <p>El formato esperado es un array JSON: [{"name":"campo1","description":"valor1"},...]
   *
   * @param guide La guía a la que se asignará la información adicional
   * @param additionalInformationJson String JSON con la información adicional
   */
  private void parseAndSetAdditionalInformation(Guide guide, String additionalInformationJson) {
    if (!StringUtils.hasText(additionalInformationJson)) {
      return;
    }

    try {
      // Intentar parsear como array JSON
      List<Map<String, String>> additionalInfoList =
          objectMapper.readValue(
              additionalInformationJson, new TypeReference<List<Map<String, String>>>() {});

      List<GuideAdditionalInformation> additionalInfoEntities = new ArrayList<>();

      for (Map<String, String> item : additionalInfoList) {
        String name = item.get("name");
        String description = item.get("description");

        if (StringUtils.hasText(name) && StringUtils.hasText(description)) {
          GuideAdditionalInformation additionalInfo =
              GuideAdditionalInformation.builder()
                  .guide(guide)
                  .name(name)
                  .description(description)
                  .status(true)
                  .build();
          additionalInfoEntities.add(additionalInfo);
          log.debug(
              "✅ [GR] Información adicional preparada para guardar: nombre={}, valor={}",
              name,
              description);
        } else {
          log.warn(
              "⚠️ [GR] Campo adicional ignorado por falta de 'name' o 'description': {}", item);
        }
      }

      if (!additionalInfoEntities.isEmpty()) {
        guide.setAdditionalInformationList(additionalInfoEntities);
        log.info(
            "📝 [GR] {} campo(s) adicional(es) preparado(s) para guardar",
            additionalInfoEntities.size());
      }
    } catch (Exception e) {
      log.warn(
          "⚠️ [GR] Error al parsear información adicional como JSON: {}. Se guardará como texto plano.",
          e.getMessage());
      // Si no se puede parsear, se guarda el texto original en el campo additional_information
      // que ya está en la entidad Guide
    }
  }
}
