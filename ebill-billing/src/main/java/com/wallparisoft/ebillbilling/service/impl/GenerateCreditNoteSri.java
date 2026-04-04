package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.config.SriConfiguration;
import com.wallparisoft.ebillbilling.constants.SriTaxConstants;
import com.wallparisoft.ebillbilling.dto.ContactDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.enums.ContactTypeEnum;
import com.wallparisoft.ebillbilling.enums.EstablishmentType;
import com.wallparisoft.ebillbilling.enums.TipoComprobanteCode;
import com.wallparisoft.integration.sri.enums.AmbienteEnum;
import com.wallparisoft.integration.sri.enums.ObligadoContabilidadEnum;
import com.wallparisoft.integration.sri.enums.TipoEmisionEnum;
import com.wallparisoft.integration.sri.enums.TipoIdentificacionCompradorEnum;
import com.wallparisoft.integration.sri.model.CampoAdicional;
import com.wallparisoft.integration.sri.model.DetalleNotaCredito;
import com.wallparisoft.integration.sri.model.ImpuestoNotaCredito;
import com.wallparisoft.integration.sri.model.InfoNotaCredito;
import com.wallparisoft.integration.sri.model.InfoTributaria;
import com.wallparisoft.integration.sri.model.NotaCredito;
import com.wallparisoft.integration.sri.model.TotalImpuesto;
import com.wallparisoft.integration.sri.util.UtilsMath;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
public class GenerateCreditNoteSri {

  private final SriConfiguration sriConfiguration;

  public NotaCredito createCreditNoteSri(
      CreditNoteDto creditNoteDto, AuthorizationSri existingAuthorizationSri) {
    NotaCredito notaCredito = new NotaCredito();
    notaCredito.setVersion("1.1.0");
    notaCredito.setId("comprobante");

    // Obtener el ICE del DTO
    BigDecimal totalIce =
        creditNoteDto.getSubtotalIce() != null ? creditNoteDto.getSubtotalIce() : BigDecimal.ZERO;

    notaCredito.setInfoTributaria(crearInfoTributaria(creditNoteDto, existingAuthorizationSri));
    notaCredito.setInfoNotaCredito(crearInfoNotaCredito(creditNoteDto));
    notaCredito.setDetalles(crearDetalles(creditNoteDto.getCreditNoteDetails(), totalIce));

    // Agregar información adicional con la observación si existe
    if (StringUtils.hasText(creditNoteDto.getObservation())) {
      List<CampoAdicional> infoAdicional = new ArrayList<>();
      CampoAdicional campoObservacion = new CampoAdicional();
      campoObservacion.setNombre("NOTA");
      campoObservacion.setValue(creditNoteDto.getObservation());
      infoAdicional.add(campoObservacion);
      notaCredito.setInfoAdicional(infoAdicional);
    }

    return notaCredito;
  }

  private List<DetalleNotaCredito> crearDetalles(
      List<InvoiceDetailDto> creditNoteDetailDto, BigDecimal totalIce) {
    List<DetalleNotaCredito> detalles = new ArrayList<DetalleNotaCredito>();

    // El ICE se agrega como impuesto al primer detalle
    boolean iceAgregado = false;

    for (InvoiceDetailDto detail : creditNoteDetailDto) {
      // Validar que el producto existe
      if (detail.getProductDto() == null) {
        log.error(
            "❌ [NC] ProductDto es null para el detalle. No se puede generar la nota de crédito sin información del producto.");
        throw new RuntimeException(
            "Error interno del servidor: No se pudo cargar la información del producto. El servicio de productos puede no estar disponible.");
      }

      DetalleNotaCredito detalle = new DetalleNotaCredito();
      detalle.setCodigoInterno(detail.getProductDto().getMainCode());
      // codigoAdicional solo se establece si tiene un valor válido (no "0", vacío o null)
      // Si es "0", vacío o null, no se incluirá en el XML
      String codigoAuxiliar = detail.getProductDto().getAuxiliarCode();
      if (codigoAuxiliar != null
          && !codigoAuxiliar.trim().isEmpty()
          && !"0".equals(codigoAuxiliar.trim())) {
        detalle.setCodigoAdicional(codigoAuxiliar);
      }
      detalle.setDescripcion(detail.getProductDto().getName());
      // En notas de crédito, las cantidades pueden ser negativas
      detalle.setCantidad(String.valueOf(detail.getQuantity()));
      detalle.setPrecioUnitario(String.valueOf(detail.getUnitPrice()));

      // El precioTotalSinImpuesto es SOLO el subtotal (cantidad × precio - descuento)
      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
      detalle.setPrecioTotalSinImpuesto(String.valueOf(subtotal));
      detalle.setDescuento(String.valueOf(detail.getDiscount() != null ? detail.getDiscount() : 0));
      detalles.add(detalle);

      List<ImpuestoNotaCredito> impuestos = new ArrayList<ImpuestoNotaCredito>();

      // Variable para saber si este detalle tiene ICE
      boolean tieneIce = false;

      // 1. Agregar ICE como primer impuesto (si existe y es el primer detalle)
      if (!iceAgregado && totalIce != null && totalIce.compareTo(BigDecimal.ZERO) > 0) {
        ImpuestoNotaCredito impuestoIce = new ImpuestoNotaCredito();
        impuestoIce.setCodigo(SriTaxConstants.CODE_ICE);
        impuestoIce.setCodigoPorcentaje(SriTaxConstants.DEFAULT_ICE_PERCENTAGE_CODE);

        // Obtener la tarifa real del ICE desde el producto
        BigDecimal iceTarifaReal =
            detail.getProductDto().getIceRate() != null
                ? detail.getProductDto().getIceRate()
                : new BigDecimal(SriTaxConstants.DEFAULT_ICE_RATE);

        // Establecer tarifa primero (orden requerido: codigo, codigoPorcentaje, tarifa,
        // baseImponible, valor)
        impuestoIce.setTarifa(String.valueOf(iceTarifaReal));
        // Formatear baseImponible sin decimales innecesarios
        String baseImponibleIceFormateada = formatBaseImponible(subtotal);
        impuestoIce.setBaseImponible(baseImponibleIceFormateada);
        // Establecer valor
        impuestoIce.setValor(String.valueOf(totalIce));
        impuestos.add(impuestoIce);
        iceAgregado = true;
        tieneIce = true;
      }

      // 2. Agregar IVA (la base imponible del IVA es subtotal + ICE si este detalle tiene ICE)
      ImpuestoNotaCredito impuestoIva = new ImpuestoNotaCredito();

      // Mapear taxCode: si es "IVA" o similar, usar código numérico "2"
      String taxCode = detail.getProductDto().getTaxCode();
      if (taxCode != null && (taxCode.equalsIgnoreCase("IVA") || taxCode.equalsIgnoreCase("2"))) {
        impuestoIva.setCodigo(SriTaxConstants.CODE_IVA); // "2"
      } else {
        impuestoIva.setCodigo(taxCode != null ? taxCode : SriTaxConstants.CODE_IVA);
      }

      BigDecimal rate =
          detail.getProductDto().getRate() != null
              ? detail.getProductDto().getRate()
              : BigDecimal.ZERO;

      // Mapear porcentaje a código SRI (ej: 15% -> "4", 12% -> "2")
      String codigoPorcentajeSRI = mapIvaRateToCodigoPorcentaje(rate);
      impuestoIva.setCodigoPorcentaje(codigoPorcentajeSRI);

      // Establecer tarifa primero (orden requerido: codigo, codigoPorcentaje, tarifa,
      // baseImponible, valor)
      String tarifaFormateada = formatTarifa(rate);
      impuestoIva.setTarifa(tarifaFormateada);

      // La base imponible del IVA = subtotal + ICE (si este detalle tiene ICE)
      BigDecimal baseImponibleIva = subtotal;
      if (tieneIce) {
        baseImponibleIva = subtotal.add(totalIce);
      }

      // Formatear baseImponible sin decimales innecesarios
      String baseImponibleFormateada = formatBaseImponible(baseImponibleIva);
      impuestoIva.setBaseImponible(baseImponibleFormateada);

      // Calcular y establecer el valor del IVA
      impuestoIva.setValor(String.valueOf(UtilsMath.iva(baseImponibleIva, rate)));

      impuestos.add(impuestoIva);

      detalle.setImpuestos(impuestos);
    }

    return detalles;
  }

  public InfoNotaCredito crearInfoNotaCredito(CreditNoteDto creditNoteDto) {
    InfoNotaCredito infoNotaCredito = new InfoNotaCredito();
    // Fecha de emisión de la nota de crédito
    infoNotaCredito.setFechaEmision((new SimpleDateFormat("dd/MM/yyyy")).format(new Date()));

    // Dirección del establecimiento actual (donde se emite la nota de crédito)
    infoNotaCredito.setDirEstablecimiento(
        creditNoteDto.getEstablishment().getAddress() != null
            ? creditNoteDto.getEstablishment().getAddress()
            : "S/N");

    infoNotaCredito.setObligadoContabilidad(ObligadoContabilidadEnum.NO);

    TipoIdentificacionCompradorEnum tipoMapeado =
        mapIdTypeToTipoIdentificacion(creditNoteDto.getClientDto().getIdType());
    infoNotaCredito.setTipoIdentificacionComprador(tipoMapeado);
    infoNotaCredito.setRazonSocialComprador(creditNoteDto.getClientDto().getName());
    infoNotaCredito.setIdentificacionComprador(creditNoteDto.getClientDto().getIdentification());

    // Campos específicos de Nota de Crédito
    infoNotaCredito.setCodDocModificado(
        creditNoteDto.getDocumentTypeModified() != null
            ? creditNoteDto.getDocumentTypeModified()
            : "01"); // Por defecto Factura

    // Formato: estab-ptoEmi-secuencial (15 caracteres)
    String numDocModificado = creditNoteDto.getOriginalInvoiceNumber();
    log.info("🔍 [NC] numDocModificado (originalInvoiceNumber): {}", numDocModificado);
    if (numDocModificado == null || numDocModificado.trim().isEmpty()) {
      log.error("❌ [NC] numDocModificado es NULL o vacío - Esto causará error en SRI");
      throw new IllegalArgumentException(
          "El número de documento modificado (originalInvoiceNumber) es obligatorio para el SRI");
    }
    infoNotaCredito.setNumDocModificado(numDocModificado);

    // Fecha de emisión del documento original (formato dd/MM/yyyy) - OBLIGATORIO para SRI
    log.info("🔍 [NC] originalIssueDate: {}", creditNoteDto.getOriginalIssueDate());
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    if (creditNoteDto.getOriginalIssueDate() != null) {
      // LocalDate necesita convertirse a LocalDateTime primero
      Date fechaOriginal =
          java.util.Date.from(
              creditNoteDto
                  .getOriginalIssueDate()
                  .atStartOfDay()
                  .atZone(ZoneId.systemDefault())
                  .toInstant());
      String fechaFormateada = sdf.format(fechaOriginal);
      log.info("🔍 [NC] fechaEmisionDocSustento: {}", fechaFormateada);
      infoNotaCredito.setFechaEmisionDocSustento(fechaFormateada);
    } else {
      log.error("❌ [NC] originalIssueDate es NULL - Esto causará error en SRI");
      throw new IllegalArgumentException(
          "La fecha de emisión del documento sustento (originalIssueDate) es obligatoria para el SRI");
    }

    // Calcular totales
    BigDecimal totalSinImpuestos =
        creditNoteDto.getCreditNoteDetails().stream()
            .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalIce =
        creditNoteDto.getSubtotalIce() != null ? creditNoteDto.getSubtotalIce() : BigDecimal.ZERO;

    // Calcular el IVA usando la tarifa vigente en la fecha del documento original
    // IMPORTANTE: La tarifa de IVA corresponde a la fecha de emisión del documento de sustento
    BigDecimal totalIva = BigDecimal.ZERO;
    if (creditNoteDto.getOriginalIssueDate() != null) {
      for (InvoiceDetailDto detail : creditNoteDto.getCreditNoteDetails()) {
        // Validar que el producto existe
        if (detail.getProductDto() == null) {
          log.error(
              "❌ [NC] ProductDto es null para el detalle en crearInfoNotaCredito. No se puede calcular el IVA sin información del producto.");
          throw new RuntimeException(
              "Error interno del servidor: No se pudo cargar la información del producto. El servicio de productos puede no estar disponible.");
        }

        BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
        BigDecimal rate =
            detail.getProductDto().getRate() != null
                ? detail.getProductDto().getRate()
                : BigDecimal.ZERO;

        // Si este producto tiene ICE, calcular el ICE de este producto específico y sumarlo a la
        // base
        BigDecimal baseIva = subtotal;
        if (detail.getProductDto().getIceRate() != null
            && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0) {
          BigDecimal iceDeEsteProducto =
              UtilsMath.iva(subtotal, detail.getProductDto().getIceRate());
          baseIva = subtotal.add(iceDeEsteProducto);
        }

        // Calcular el IVA de este producto usando la tarifa del documento original
        BigDecimal ivaDeEsteProducto = UtilsMath.iva(baseIva, rate);
        totalIva = totalIva.add(ivaDeEsteProducto);
      }
    }

    // Total de impuestos = IVA + ICE
    BigDecimal totalImpuestos = totalIva.add(totalIce);

    // Valor de modificación = totalSinImpuestos + impuestos
    BigDecimal valorModificacion = totalSinImpuestos.add(totalImpuestos);

    infoNotaCredito.setTotalSinImpuestos(String.valueOf(totalSinImpuestos));
    infoNotaCredito.setValorModificacion(String.valueOf(valorModificacion));
    infoNotaCredito.setTotalConImpuestos(crearTotalImpuestos(creditNoteDto));
    infoNotaCredito.setMoneda(com.wallparisoft.integration.sri.enums.MonedaEnum.DOLAR);
    infoNotaCredito.setMotivo(creditNoteDto.getReason());

    return infoNotaCredito;
  }

  public List<TotalImpuesto> crearTotalImpuestos(CreditNoteDto creditNoteDto) {
    List<TotalImpuesto> totalImpuestos = new ArrayList<TotalImpuesto>();

    // Calcular el total sin impuestos (suma de subtotales ya con descuentos aplicados)
    BigDecimal totalSinImpuestos =
        creditNoteDto.getCreditNoteDetails().stream()
            .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Agregar ICE si existe
    BigDecimal totalIce =
        creditNoteDto.getSubtotalIce() != null ? creditNoteDto.getSubtotalIce() : BigDecimal.ZERO;

    // 1. Agregar TotalImpuesto para el ICE (si existe)
    if (totalIce.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal baseImponibleIce =
          creditNoteDto.getCreditNoteDetails().stream()
              .filter(
                  detail ->
                      detail.getProductDto() != null
                          && detail.getProductDto().getIceRate() != null
                          && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0)
              .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal iceTarifaReal =
          creditNoteDto.getCreditNoteDetails().stream()
              .filter(
                  detail ->
                      detail.getProductDto() != null
                          && detail.getProductDto().getIceRate() != null
                          && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0)
              .map(detail -> detail.getProductDto().getIceRate())
              .findFirst()
              .orElse(new BigDecimal(SriTaxConstants.DEFAULT_ICE_RATE));

      TotalImpuesto totalImpuestoIce = new TotalImpuesto();
      totalImpuestoIce.setCodigo(SriTaxConstants.CODE_ICE);
      totalImpuestoIce.setCodigoPorcentaje(SriTaxConstants.DEFAULT_ICE_PERCENTAGE_CODE);
      // Nota: En notas de crédito, el tag 'tarifa' no se requiere en totalConImpuestos
      // totalImpuestoIce.setTarifa(String.valueOf(iceTarifaReal));
      String baseImponibleIceFormateada = formatBaseImponible(baseImponibleIce);
      totalImpuestoIce.setBaseImponible(baseImponibleIceFormateada);
      totalImpuestoIce.setValor(String.valueOf(totalIce));
      totalImpuestos.add(totalImpuestoIce);
    }

    // 2. Agregar TotalImpuesto para el IVA agrupando por tarifa
    Map<BigDecimal, BigDecimal> ivasPorTarifa = new HashMap<>();

    for (InvoiceDetailDto detail : creditNoteDto.getCreditNoteDetails()) {
      // Validar que el producto existe
      if (detail.getProductDto() == null) {
        log.error(
            "❌ [NC] ProductDto es null para el detalle en crearTotalImpuestos. No se puede calcular los totales sin información del producto.");
        throw new RuntimeException(
            "Error interno del servidor: No se pudo cargar la información del producto. El servicio de productos puede no estar disponible.");
      }

      BigDecimal rate =
          detail.getProductDto().getRate() != null
              ? detail.getProductDto().getRate()
              : BigDecimal.ZERO;

      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;

      // Si este producto tiene ICE, calcular el ICE de este producto específico y sumarlo a la base
      BigDecimal baseIva = subtotal;
      if (detail.getProductDto().getIceRate() != null
          && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal iceDeEsteProducto = UtilsMath.iva(subtotal, detail.getProductDto().getIceRate());
        baseIva = subtotal.add(iceDeEsteProducto);
      }

      // Acumular base imponible por tarifa
      ivasPorTarifa.merge(rate, baseIva, BigDecimal::add);
    }

    // Crear un TotalImpuesto por cada tarifa de IVA
    for (Map.Entry<BigDecimal, BigDecimal> entry : ivasPorTarifa.entrySet()) {
      BigDecimal rate = entry.getKey();
      BigDecimal baseImponible = entry.getValue();
      BigDecimal valorIva = UtilsMath.iva(baseImponible, rate);

      TotalImpuesto totalImpuestoIva = new TotalImpuesto();
      totalImpuestoIva.setCodigo(SriTaxConstants.CODE_IVA);

      String codigoPorcentajeSRI = mapIvaRateToCodigoPorcentaje(rate);
      totalImpuestoIva.setCodigoPorcentaje(codigoPorcentajeSRI);

      // Nota: En notas de crédito, el tag 'tarifa' no se requiere en totalConImpuestos
      // String tarifaFormateada = formatTarifa(rate);
      // totalImpuestoIva.setTarifa(tarifaFormateada);

      String baseImponibleFormateada = formatBaseImponible(baseImponible);
      totalImpuestoIva.setBaseImponible(baseImponibleFormateada);

      totalImpuestoIva.setValor(String.valueOf(valorIva));
      totalImpuestos.add(totalImpuestoIva);
    }

    return totalImpuestos;
  }

  public InfoTributaria crearInfoTributaria(
      CreditNoteDto creditNoteDto, AuthorizationSri existingAuthorizationSri) {
    InfoTributaria infoTributaria = new InfoTributaria();

    // IMPORTANTE: El ambiente se obtiene de la configuración del servidor, NO del request
    infoTributaria.setAmbiente(
        sriConfiguration.isPruebas() ? AmbienteEnum.PRUEBAS : AmbienteEnum.PRODUCCION);
    infoTributaria.setTipoEmision(TipoEmisionEnum.NORMAL);
    infoTributaria.setRazonSocial(creditNoteDto.getCompanyDto().getName());
    infoTributaria.setNombreComercial(creditNoteDto.getCompanyDto().getNameCommercial());
    infoTributaria.setRuc(creditNoteDto.getCompanyDto().getIdentification());

    // Código de documento: "04" para Nota de Crédito
    String codDoc = TipoComprobanteCode.NOTA_CREDITO.getCode();
    infoTributaria.setCodDoc(codDoc);
    log.info("🔍 [Nota de Crédito] codDoc establecido: {}", codDoc);
    infoTributaria.setEstab(creditNoteDto.getEstablishment().getCode());
    infoTributaria.setPtoEmi(creditNoteDto.getEstablishment().getIssuancePoint().getCode());
    infoTributaria.setSecuencial(
        creditNoteDto.getEstablishment().getIssuancePoint().getSequentialInvoice());

    // Reutilizar la clave de acceso existente si existe
    if (existingAuthorizationSri != null
        && StringUtils.hasText(existingAuthorizationSri.getAccessKey())) {
      infoTributaria.setClaveAcceso(existingAuthorizationSri.getAccessKey());
      log.debug(
          "Reutilizando clave de acceso existente: {} para evitar error 'CLAVE ACCESO REGISTRADA'",
          existingAuthorizationSri.getAccessKey());
    } else {
      // Generar nueva clave de acceso
      infoTributaria.setClaveAcceso(generateAccessKey(infoTributaria));
      log.debug("Generando nueva clave de acceso");
    }

    // Setear dirección matriz según el tipo de establecimiento
    String direccionMatriz;
    String establishmentTypeStr = creditNoteDto.getEstablishment().getType();
    EstablishmentType establishmentType = EstablishmentType.fromCode(establishmentTypeStr);

    if (establishmentType == EstablishmentType.MATRIZ) {
      direccionMatriz = creditNoteDto.getEstablishment().getAddress();
    } else if (establishmentType == EstablishmentType.SUCURSAL) {
      direccionMatriz = creditNoteDto.getEstablishment().getParentAddress();
      if (direccionMatriz == null || direccionMatriz.trim().isEmpty()) {
        direccionMatriz =
            getContact(ContactTypeEnum.ADD.name(), creditNoteDto.getCompanyDto().getContacts());
      }
    } else {
      direccionMatriz =
          getContact(ContactTypeEnum.ADD.name(), creditNoteDto.getCompanyDto().getContacts());
    }

    infoTributaria.setDirMatriz(direccionMatriz != null ? direccionMatriz : "S/N");

    return infoTributaria;
  }

  public String getContact(String contactType, List<ContactDto> contacts) {
    Optional<ContactDto> contactOpt =
        contacts.stream().filter(c -> c.getContactType().equals(contactType)).findFirst();
    return contactOpt.map(ContactDto::getValue).orElse("S/N");
  }

  private TipoIdentificacionCompradorEnum mapIdTypeToTipoIdentificacion(Integer idType) {
    if (idType == null) {
      return TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
    }

    return switch (idType) {
      case 1 -> TipoIdentificacionCompradorEnum.CEDULA;
      case 2 -> TipoIdentificacionCompradorEnum.RUC;
      case 3 -> TipoIdentificacionCompradorEnum.PASAPORTE;
      case 4 -> TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
      case 5 -> TipoIdentificacionCompradorEnum.IDENTIFICACION_DEL_EXTERIOR;
      default -> TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
    };
  }

  public String generateAccessKey(InfoTributaria infoTributaria) {
    Calendar cal = Calendar.getInstance();
    String accessKey =
        infoTributaria.generarClaveAcceso(
            cal.getTime(),
            com.wallparisoft.integration.sri.enums.TipoComprobanteEnum.NOTA_DE_CREDITO,
            "12345678");
    if (accessKey != null && accessKey.length() == 49) {
      return accessKey;
    } else {
      return generateAccessKey(infoTributaria);
    }
  }

  private String mapIvaRateToCodigoPorcentaje(BigDecimal rate) {
    if (rate == null) {
      return "0";
    }

    int percentage;
    if (rate.compareTo(BigDecimal.ONE) >= 0) {
      percentage = rate.intValue();
    } else {
      percentage = rate.multiply(new BigDecimal("100")).intValue();
    }

    switch (percentage) {
      case 0:
        return "0";
      case 5:
        return "5";
      case 13:
        return "10";
      case 15:
        return "4";
      case 12:
        log.warn("⚠️ IVA 12% está OBSOLETO. Usa 15% o 13% según corresponda.");
        return "2";
      case 14:
        log.warn("⚠️ IVA 14% está OBSOLETO. Usa 15% o 13% según corresponda.");
        return "3";
      default:
        log.error("❌ IVA {}% NO ES RECONOCIDO.", percentage);
        if (percentage <= 5) {
          return "0";
        } else if (percentage <= 13) {
          return "10";
        } else {
          return "4";
        }
    }
  }

  private String formatTarifa(BigDecimal rate) {
    if (rate == null) {
      return "0";
    }
    int percentage;
    if (rate.compareTo(BigDecimal.ONE) >= 0) {
      percentage = rate.intValue();
    } else {
      percentage = rate.multiply(new BigDecimal("100")).intValue();
    }
    return String.valueOf(percentage);
  }

  private String formatBaseImponible(BigDecimal baseImponible) {
    if (baseImponible == null) {
      return "0";
    }
    if (baseImponible.scale() == 0 || baseImponible.stripTrailingZeros().scale() <= 0) {
      return baseImponible.toBigInteger().toString();
    }
    return baseImponible.stripTrailingZeros().toPlainString();
  }
}
