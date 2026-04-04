package com.wallparisoft.ebillbilling.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallparisoft.ebillbilling.config.SriConfiguration;
import com.wallparisoft.ebillbilling.dto.ContactDto;
import com.wallparisoft.ebillbilling.dto.GuideDetailDto;
import com.wallparisoft.ebillbilling.dto.GuideDocumentReferenceDto;
import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.enums.ContactTypeEnum;
import com.wallparisoft.ebillbilling.enums.EstablishmentType;
import com.wallparisoft.ebillbilling.enums.TipoComprobanteCode;
import com.wallparisoft.integration.sri.enums.AmbienteEnum;
import com.wallparisoft.integration.sri.enums.ObligadoContabilidadEnum;
import com.wallparisoft.integration.sri.enums.TipoEmisionEnum;
import com.wallparisoft.integration.sri.enums.TipoIdentificacionCompradorEnum;
import com.wallparisoft.integration.sri.model.CampoAdicional;
import com.wallparisoft.integration.sri.model.Destinatario;
import com.wallparisoft.integration.sri.model.DetalleGuiaRemision;
import com.wallparisoft.integration.sri.model.GuiaRemision;
import com.wallparisoft.integration.sri.model.InfoGuiaRemision;
import com.wallparisoft.integration.sri.model.InfoTributaria;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateGuideSri {

  private final SriConfiguration sriConfiguration;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public GuiaRemision createGuideSri(GuideDto guideDto, AuthorizationSri existingAuthorizationSri) {
    GuiaRemision guiaRemision = new GuiaRemision();
    guiaRemision.setVersion("1.0.0");
    guiaRemision.setId("comprobante");

    guiaRemision.setInfoTributaria(crearInfoTributaria(guideDto, existingAuthorizationSri));
    guiaRemision.setInfoGuiaRemision(crearInfoGuiaRemision(guideDto));
    guiaRemision.setDestinatarios(crearDestinatarios(guideDto));

    // Agregar información adicional si existe
    if (StringUtils.hasText(guideDto.getAdditionalInformation())) {
      List<CampoAdicional> infoAdicional =
          parseAdditionalInformation(guideDto.getAdditionalInformation());
      if (!infoAdicional.isEmpty()) {
        guiaRemision.setInfoAdicional(infoAdicional);
      }
    }

    return guiaRemision;
  }

  public InfoGuiaRemision crearInfoGuiaRemision(GuideDto guideDto) {
    InfoGuiaRemision infoGuiaRemision = new InfoGuiaRemision();

    // Dirección del establecimiento
    infoGuiaRemision.setDirEstablecimiento(
        guideDto.getEstablishment() != null && guideDto.getEstablishment().getAddress() != null
            ? guideDto.getEstablishment().getAddress()
            : "S/N");

    // Dirección de partida
    infoGuiaRemision.setDirPartida(guideDto.getOriginAddress());

    // Información del transportista
    if (guideDto.getCarrierDto() != null) {
      infoGuiaRemision.setRazonSocialTransportista(guideDto.getCarrierDto().getName());
      infoGuiaRemision.setRucTransportista(guideDto.getCarrierDto().getIdentification());

      // Mapear tipo de identificación del transportista
      // IMPORTANTE: Para Guía de Remisión, el transportista NO puede ser CONSUMIDOR_FINAL
      // Si tiene RUC (13 dígitos), debe ser RUC; si tiene cédula (10 dígitos), debe ser CEDULA
      TipoIdentificacionCompradorEnum tipoTransportista =
          mapIdTypeToTipoIdentificacionForCarrier(
              guideDto.getCarrierDto().getIdentificationType(),
              guideDto.getCarrierDto().getIdentification());
      infoGuiaRemision.setTipoIdentificacionTransportista(tipoTransportista);
    }

    // Obligado a llevar contabilidad (de la empresa)
    String forcedToAccounting =
        guideDto.getCompanyDto() != null ? guideDto.getCompanyDto().getForcedToAccounting() : null;
    ObligadoContabilidadEnum obligadoContabilidad =
        (forcedToAccounting != null && "S".equalsIgnoreCase(forcedToAccounting.trim()))
            ? ObligadoContabilidadEnum.SI
            : ObligadoContabilidadEnum.NO;
    infoGuiaRemision.setObligadoContabilidad(obligadoContabilidad);

    // Fechas de transporte (deben ir antes de placa según especificación SRI)
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    if (guideDto.getStartDate() != null) {
      Date fechaIni =
          java.util.Date.from(
              guideDto.getStartDate().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
      infoGuiaRemision.setFechaIniTransporte(sdf.format(fechaIni));
    }

    if (guideDto.getEndDate() != null) {
      Date fechaFin =
          java.util.Date.from(
              guideDto.getEndDate().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
      infoGuiaRemision.setFechaFinTransporte(sdf.format(fechaFin));
    }

    // Placa del vehículo (debe ir después de las fechas según especificación SRI)
    infoGuiaRemision.setPlaca(guideDto.getTransportPlate());

    // Nota: direccionDestinatario, motivoTraslado, ruta y documentos de sustento
    // deben ir en la sección destinatarios, no en infoGuiaRemision
    return infoGuiaRemision;
  }

  public List<Destinatario> crearDestinatarios(GuideDto guideDto) {
    List<Destinatario> destinatarios = new ArrayList<>();

    // Crear un destinatario principal con el cliente
    Destinatario destinatario = new Destinatario();

    if (guideDto.getClientDto() != null) {
      destinatario.setIdentificacionDestinatario(guideDto.getClientDto().getIdentification());
      destinatario.setRazonSocialDestinatario(guideDto.getClientDto().getName());
      destinatario.setDirDestinatario(guideDto.getDestinationAddress());
    }

    // Motivo del traslado
    destinatario.setMotivoTraslado(guideDto.getReasonForTransfer());

    // Ruta (opcional)
    if (StringUtils.hasText(guideDto.getRoute())) {
      destinatario.setRuta(guideDto.getRoute());
    }

    // Documentos de sustento (si existen)
    if (guideDto.getDocumentReferences() != null && !guideDto.getDocumentReferences().isEmpty()) {
      GuideDocumentReferenceDto firstRef = guideDto.getDocumentReferences().get(0);
      if (StringUtils.hasText(firstRef.getDocumentType())) {
        destinatario.setCodDocSustento(firstRef.getDocumentType());
      }
      if (StringUtils.hasText(firstRef.getDocumentNumber())) {
        destinatario.setNumDocSustento(firstRef.getDocumentNumber());
      }
      if (StringUtils.hasText(firstRef.getAccessKey())) {
        destinatario.setNumAutDocSustento(firstRef.getAccessKey());
      }
      if (firstRef.getIssueDate() != null) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date fechaDoc =
            java.util.Date.from(firstRef.getIssueDate().atZone(ZoneId.systemDefault()).toInstant());
        destinatario.setFechaEmisionDocSustento(sdf.format(fechaDoc));
      }
    }

    // Crear detalles de productos
    destinatario.setDetalles(crearDetallesGuia(guideDto));

    destinatarios.add(destinatario);

    return destinatarios;
  }

  public List<DetalleGuiaRemision> crearDetallesGuia(GuideDto guideDto) {
    List<DetalleGuiaRemision> detalles = new ArrayList<>();

    if (guideDto.getGuideDetails() == null || guideDto.getGuideDetails().isEmpty()) {
      throw new IllegalArgumentException(
          "La guía de remisión debe tener al menos un detalle (producto)");
    }

    for (GuideDetailDto detailDto : guideDto.getGuideDetails()) {
      DetalleGuiaRemision detalle = new DetalleGuiaRemision();

      // Código interno del producto
      if (detailDto.getProductDto() != null
          && StringUtils.hasText(detailDto.getProductDto().getMainCode())) {
        detalle.setCodigoInterno(detailDto.getProductDto().getMainCode());
      } else if (detailDto.getIdProduct() != null) {
        detalle.setCodigoInterno(detailDto.getIdProduct().toString());
      }

      // Código adicional (opcional)
      if (detailDto.getProductDto() != null
          && StringUtils.hasText(detailDto.getProductDto().getAuxiliarCode())) {
        detalle.setCodigoAdicional(detailDto.getProductDto().getAuxiliarCode());
      }

      // Descripción
      detalle.setDescripcion(
          StringUtils.hasText(detailDto.getDescription())
              ? detailDto.getDescription()
              : (detailDto.getProductDto() != null
                      && StringUtils.hasText(detailDto.getProductDto().getDescription())
                  ? detailDto.getProductDto().getDescription()
                  : "Producto sin descripción"));

      // Cantidad
      detalle.setCantidad(
          detailDto.getQuantity() != null ? formatDecimal(detailDto.getQuantity()) : "0");

      // Nota: Los documentos de sustento (codDocSustento, numDocSustento, etc.)
      // deben ir en la sección Destinatario, no en DetalleGuiaRemision
      detalles.add(detalle);
    }

    return detalles;
  }

  public InfoTributaria crearInfoTributaria(
      GuideDto guideDto, AuthorizationSri existingAuthorizationSri) {
    InfoTributaria infoTributaria = new InfoTributaria();

    infoTributaria.setAmbiente(
        sriConfiguration.isPruebas() ? AmbienteEnum.PRUEBAS : AmbienteEnum.PRODUCCION);
    infoTributaria.setTipoEmision(TipoEmisionEnum.NORMAL);
    infoTributaria.setRazonSocial(guideDto.getCompanyDto().getName());
    infoTributaria.setNombreComercial(guideDto.getCompanyDto().getNameCommercial());
    infoTributaria.setRuc(guideDto.getCompanyDto().getIdentification());

    // Código de documento: "06" para Guía de Remisión
    String codDoc = TipoComprobanteCode.GUIA_REMISION.getCode();
    infoTributaria.setCodDoc(codDoc);
    log.info("🔍 [Guía de Remisión] codDoc establecido: {}", codDoc);

    if (guideDto.getEstablishment() != null) {
      infoTributaria.setEstab(guideDto.getEstablishment().getCode());
      if (guideDto.getEstablishment().getIssuancePoint() != null) {
        infoTributaria.setPtoEmi(guideDto.getEstablishment().getIssuancePoint().getCode());

        // Obtener el secuencial de guideDto.getSequential() y formatearlo
        if (guideDto.getSequential() != null) {
          String formattedSequential = String.format("%09d", guideDto.getSequential());
          infoTributaria.setSecuencial(formattedSequential);
        } else {
          // Si no hay secuencial en guideDto, intentar desde establishment (fallback)
          String sequentialFromEstablishment =
              guideDto.getEstablishment().getIssuancePoint().getSequentialInvoice();
          if (StringUtils.hasText(sequentialFromEstablishment)) {
            infoTributaria.setSecuencial(sequentialFromEstablishment);
          } else {
            throw new IllegalArgumentException("Secuencial no asignado");
          }
        }
      }
    }

    // Reutilizar la clave de acceso existente si existe
    if (existingAuthorizationSri != null
        && StringUtils.hasText(existingAuthorizationSri.getAccessKey())) {
      infoTributaria.setClaveAcceso(existingAuthorizationSri.getAccessKey());
      log.debug(
          "Reutilizando clave de acceso existente: {} para evitar error 'CLAVE ACCESO REGISTRADA'",
          existingAuthorizationSri.getAccessKey());
    } else {
      infoTributaria.setClaveAcceso(generateAccessKey(infoTributaria));
      log.debug("Generando nueva clave de acceso");
    }

    // Setear dirección matriz según el tipo de establecimiento
    String direccionMatriz;
    if (guideDto.getEstablishment() != null) {
      String establishmentTypeStr = guideDto.getEstablishment().getType();
      EstablishmentType establishmentType = EstablishmentType.fromCode(establishmentTypeStr);

      if (establishmentType == EstablishmentType.MATRIZ) {
        direccionMatriz = guideDto.getEstablishment().getAddress();
      } else if (establishmentType == EstablishmentType.SUCURSAL) {
        direccionMatriz = guideDto.getEstablishment().getParentAddress();
        if (direccionMatriz == null || direccionMatriz.trim().isEmpty()) {
          direccionMatriz =
              getContact(ContactTypeEnum.ADD.name(), guideDto.getCompanyDto().getContacts());
        }
      } else {
        direccionMatriz =
            getContact(ContactTypeEnum.ADD.name(), guideDto.getCompanyDto().getContacts());
      }
    } else {
      direccionMatriz =
          getContact(ContactTypeEnum.ADD.name(), guideDto.getCompanyDto().getContacts());
    }

    infoTributaria.setDirMatriz(direccionMatriz != null ? direccionMatriz : "S/N");

    return infoTributaria;
  }

  public String getContact(String contactType, List<ContactDto> contacts) {
    if (contacts == null) {
      return "S/N";
    }
    Optional<ContactDto> contactOpt =
        contacts.stream().filter(c -> c.getContactType().equals(contactType)).findFirst();
    return contactOpt.map(ContactDto::getValue).orElse("S/N");
  }

  private TipoIdentificacionCompradorEnum mapIdTypeToTipoIdentificacion(String idType) {
    if (!StringUtils.hasText(idType)) {
      return TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
    }

    // Mapear desde string (ej: "04", "05", etc.) o desde código numérico
    return switch (idType.trim()) {
      case "04", "4" -> TipoIdentificacionCompradorEnum.RUC;
      case "05", "5" -> TipoIdentificacionCompradorEnum.CEDULA;
      case "06", "6" -> TipoIdentificacionCompradorEnum.PASAPORTE;
      case "07", "7" -> TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
      case "08", "8" -> TipoIdentificacionCompradorEnum.IDENTIFICACION_DEL_EXTERIOR;
      case "09", "9" -> TipoIdentificacionCompradorEnum.PLACA;
      default -> TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
    };
  }

  /**
   * Mapea el tipo de identificación del transportista. Para Guía de Remisión, el transportista NO
   * puede ser CONSUMIDOR_FINAL. Si no se puede determinar el tipo, se infiere desde la longitud de
   * la identificación.
   *
   * @param idType Tipo de identificación (puede ser código numérico "1", "2", etc. o código SRI
   *     "04", "05", etc.)
   * @param identification Número de identificación (para inferir el tipo si idType no es válido)
   * @return El enum correspondiente del SRI (RUC o CEDULA, nunca CONSUMIDOR_FINAL)
   */
  private TipoIdentificacionCompradorEnum mapIdTypeToTipoIdentificacionForCarrier(
      String idType, String identification) {
    // Primero intentar mapear desde idType
    TipoIdentificacionCompradorEnum tipoMapeado = mapIdTypeToTipoIdentificacion(idType);

    // Si el resultado es CONSUMIDOR_FINAL o no se pudo mapear, inferir desde la identificación
    if (tipoMapeado == TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL
        || (!StringUtils.hasText(idType) && StringUtils.hasText(identification))) {
      // Inferir tipo desde la longitud de la identificación
      String identificationClean = identification != null ? identification.trim() : "";
      if (identificationClean.length() == 13) {
        log.info(
            "🔍 [Transportista] Identificación con 13 dígitos detectada, usando RUC: {}",
            identificationClean);
        return TipoIdentificacionCompradorEnum.RUC;
      } else if (identificationClean.length() == 10) {
        log.info(
            "🔍 [Transportista] Identificación con 10 dígitos detectada, usando CEDULA: {}",
            identificationClean);
        return TipoIdentificacionCompradorEnum.CEDULA;
      } else {
        // Si no se puede inferir, usar RUC por defecto (más común para transportistas)
        log.warn(
            "⚠️ [Transportista] No se pudo determinar el tipo de identificación (idType: {}, identification: {}), usando RUC por defecto",
            idType,
            identificationClean);
        return TipoIdentificacionCompradorEnum.RUC;
      }
    }

    // Si el tipo mapeado es CONSUMIDOR_FINAL, cambiarlo a RUC (transportistas no pueden ser CF)
    if (tipoMapeado == TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL) {
      log.warn(
          "⚠️ [Transportista] Tipo CONSUMIDOR_FINAL no permitido para transportistas, usando RUC por defecto");
      return TipoIdentificacionCompradorEnum.RUC;
    }

    return tipoMapeado;
  }

  public String generateAccessKey(InfoTributaria infoTributaria) {
    Calendar cal = Calendar.getInstance();
    String accessKey =
        infoTributaria.generarClaveAcceso(
            cal.getTime(),
            com.wallparisoft.integration.sri.enums.TipoComprobanteEnum.GUIA_DE_REMISION,
            "12345678");
    if (accessKey != null && accessKey.length() == 49) {
      return accessKey;
    } else {
      return generateAccessKey(infoTributaria);
    }
  }

  /**
   * Parsea la información adicional desde un string JSON y crea una lista de CampoAdicional.
   *
   * <p>El formato esperado es un array JSON: [{"name":"campo1","description":"valor1"},...]
   *
   * @param additionalInformationJson String JSON con la información adicional
   * @return Lista de CampoAdicional para enviar al SRI
   */
  private List<CampoAdicional> parseAdditionalInformation(String additionalInformationJson) {
    List<CampoAdicional> infoAdicional = new ArrayList<>();

    if (!StringUtils.hasText(additionalInformationJson)) {
      return infoAdicional;
    }

    try {
      // Intentar parsear como array JSON
      List<Map<String, String>> additionalInfoList =
          objectMapper.readValue(
              additionalInformationJson, new TypeReference<List<Map<String, String>>>() {});

      for (Map<String, String> item : additionalInfoList) {
        String name = item.get("name");
        String description = item.get("description");

        if (StringUtils.hasText(name) && StringUtils.hasText(description)) {
          CampoAdicional campoAdicional = new CampoAdicional();
          campoAdicional.setNombre(name);
          campoAdicional.setValue(description);
          infoAdicional.add(campoAdicional);
          log.debug("✅ [GR] Campo adicional agregado: nombre={}, valor={}", name, description);
        } else {
          log.warn(
              "⚠️ [GR] Campo adicional ignorado por falta de 'name' o 'description': {}", item);
        }
      }
    } catch (Exception e) {
      log.warn(
          "⚠️ [GR] Error al parsear información adicional como JSON, enviando como campo único: {}",
          e.getMessage());
      // Fallback: enviar como un solo campo si no se puede parsear
      CampoAdicional campoAdicional = new CampoAdicional();
      campoAdicional.setNombre("InformacionAdicional");
      campoAdicional.setValue(additionalInformationJson);
      infoAdicional.add(campoAdicional);
    }

    return infoAdicional;
  }

  /**
   * Formatea un BigDecimal a String usando punto como separador decimal.
   *
   * @param value el valor BigDecimal a formatear
   * @return String con el valor formateado usando punto como separador decimal
   */
  private String formatDecimal(BigDecimal value) {
    if (value == null) {
      return "0";
    }
    return value.stripTrailingZeros().toPlainString();
  }
}
