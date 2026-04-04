package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.config.SriConfiguration;
import com.wallparisoft.ebillbilling.constants.SriTaxConstants;
import com.wallparisoft.ebillbilling.dto.ContactDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.PaymentDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.enums.ContactTypeEnum;
import com.wallparisoft.ebillbilling.enums.EstablishmentType;
import com.wallparisoft.ebillbilling.enums.TipoComprobanteCode;
import com.wallparisoft.integration.sri.enums.AmbienteEnum;
import com.wallparisoft.integration.sri.enums.FormaPagoEnum;
import com.wallparisoft.integration.sri.enums.ObligadoContabilidadEnum;
import com.wallparisoft.integration.sri.enums.TipoEmisionEnum;
import com.wallparisoft.integration.sri.enums.TipoIdentificacionCompradorEnum;
import com.wallparisoft.integration.sri.model.CampoAdicional;
import com.wallparisoft.integration.sri.model.ImpuestoNotaDebito;
import com.wallparisoft.integration.sri.model.InfoNotaDebito;
import com.wallparisoft.integration.sri.model.InfoTributaria;
import com.wallparisoft.integration.sri.model.MotivoNotaDebito;
import com.wallparisoft.integration.sri.model.NotaDebito;
import com.wallparisoft.integration.sri.model.Pago;
import com.wallparisoft.integration.sri.util.UtilsMath;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateDebitNoteSri {

  private final SriConfiguration sriConfiguration;

  public NotaDebito createDebitNoteSri(
      DebitNoteDto debitNoteDto, AuthorizationSri existingAuthorizationSri) {
    NotaDebito notaDebito = new NotaDebito();
    notaDebito.setVersion("1.0.0");
    notaDebito.setId("comprobante");

    notaDebito.setInfoTributaria(crearInfoTributaria(debitNoteDto, existingAuthorizationSri));
    notaDebito.setInfoNotaDebito(crearInfoNotaDebito(debitNoteDto));
    notaDebito.setMotivos(crearMotivos(debitNoteDto));

    // Agregar información adicional con la observación si existe
    if (StringUtils.hasText(debitNoteDto.getObservation())) {
      List<CampoAdicional> infoAdicional = new ArrayList<>();
      CampoAdicional campoObservacion = new CampoAdicional();
      campoObservacion.setNombre("Observacion");
      campoObservacion.setValue(debitNoteDto.getObservation());
      infoAdicional.add(campoObservacion);
      notaDebito.setInfoAdicional(infoAdicional);
    }

    return notaDebito;
  }

  public InfoNotaDebito crearInfoNotaDebito(DebitNoteDto debitNoteDto) {
    InfoNotaDebito infoNotaDebito = new InfoNotaDebito();
    // Fecha de emisión de la nota de débito (usar issueDate del DTO si está presente)
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    if (debitNoteDto.getIssueDate() != null) {
      Date fechaEmision =
          java.util.Date.from(
              debitNoteDto
                  .getIssueDate()
                  .atStartOfDay()
                  .atZone(ZoneId.systemDefault())
                  .toInstant());
      infoNotaDebito.setFechaEmision(sdf.format(fechaEmision));
    } else {
      infoNotaDebito.setFechaEmision(sdf.format(new Date()));
    }

    // Dirección del establecimiento actual
    infoNotaDebito.setDirEstablecimiento(
        debitNoteDto.getEstablishment().getAddress() != null
            ? debitNoteDto.getEstablishment().getAddress()
            : "S/N");

    // Obligado a llevar contabilidad
    infoNotaDebito.setObligadoContabilidad(ObligadoContabilidadEnum.SI);

    TipoIdentificacionCompradorEnum tipoMapeado =
        mapIdTypeToTipoIdentificacion(debitNoteDto.getClientDto().getIdType());
    infoNotaDebito.setTipoIdentificacionComprador(tipoMapeado);
    infoNotaDebito.setRazonSocialComprador(debitNoteDto.getClientDto().getName());
    infoNotaDebito.setIdentificacionComprador(debitNoteDto.getClientDto().getIdentification());

    // Campos específicos de Nota de Débito
    infoNotaDebito.setCodDocModificado(
        debitNoteDto.getDocumentTypeModified() != null
            ? debitNoteDto.getDocumentTypeModified()
            : "01"); // Por defecto Factura

    String numDocModificado = debitNoteDto.getOriginalInvoiceNumber();
    log.info("🔍 [ND] numDocModificado (originalInvoiceNumber): {}", numDocModificado);
    if (numDocModificado == null || numDocModificado.trim().isEmpty()) {
      log.error("❌ [ND] numDocModificado es NULL o vacío - Esto causará error en SRI");
      throw new IllegalArgumentException(
          "El número de documento modificado (originalInvoiceNumber) es obligatorio para el SRI");
    }
    infoNotaDebito.setNumDocModificado(numDocModificado);

    // Fecha de emisión del documento original (formato dd/MM/yyyy) - OBLIGATORIO para SRI
    log.info("🔍 [ND] originalIssueDate: {}", debitNoteDto.getOriginalIssueDate());
    if (debitNoteDto.getOriginalIssueDate() != null) {
      // Validar que la fecha del documento sustento no sea mayor a la fecha actual
      java.time.LocalDate fechaActual = java.time.LocalDate.now();
      if (debitNoteDto.getOriginalIssueDate().isAfter(fechaActual)) {
        log.error(
            "❌ [ND] La fecha de emisión del documento sustento ({}) no puede ser mayor a la fecha actual ({})",
            debitNoteDto.getOriginalIssueDate(),
            fechaActual);
        throw new IllegalArgumentException(
            "La fecha de emisión del documento sustento no puede ser mayor a la fecha actual");
      }

      Date fechaOriginal =
          java.util.Date.from(
              debitNoteDto
                  .getOriginalIssueDate()
                  .atStartOfDay()
                  .atZone(ZoneId.systemDefault())
                  .toInstant());
      String fechaFormateada = sdf.format(fechaOriginal);
      log.info("🔍 [ND] fechaEmisionDocSustento: {}", fechaFormateada);
      infoNotaDebito.setFechaEmisionDocSustento(fechaFormateada);
    } else {
      log.error("❌ [ND] originalIssueDate es NULL - Esto causará error en SRI");
      throw new IllegalArgumentException(
          "La fecha de emisión del documento sustento (originalIssueDate) es obligatoria para el SRI");
    }

    // Calcular totales
    BigDecimal totalSinImpuestos = calcularTotalSinImpuestos(debitNoteDto);
    BigDecimal totalImpuestos = calcularTotalImpuestos(debitNoteDto);
    BigDecimal valorTotal = totalSinImpuestos.add(totalImpuestos);

    // Validar que coincida con el totalAmount (si viene del DTO)
    // Si no coincide, usar el valorTotal calculado (fuente de verdad)
    if (debitNoteDto.getTotalAmount() != null) {
      BigDecimal diferencia = debitNoteDto.getTotalAmount().subtract(valorTotal).abs();
      if (diferencia.compareTo(new BigDecimal("0.01")) > 0) {
        log.warn(
            "⚠️ [ND] El totalAmount (%.2f) no coincide con valorTotal calculado (%.2f). Se usará el valorTotal calculado.",
            debitNoteDto.getTotalAmount(), valorTotal);
        // Actualizar el DTO con el valorTotal calculado
        debitNoteDto.setTotalAmount(valorTotal);
        debitNoteDto.setValorTotal(valorTotal);
      }
    } else {
      // Si no viene totalAmount, establecerlo con el valorTotal calculado
      debitNoteDto.setTotalAmount(valorTotal);
      debitNoteDto.setValorTotal(valorTotal);
    }

    infoNotaDebito.setTotalSinImpuestos(formatDecimal(totalSinImpuestos));
    // El método crearImpuestos siempre retorna al menos un impuesto (con valor 0 si no hay
    // impuestos reales)
    infoNotaDebito.setImpuestos(crearImpuestos(debitNoteDto));
    infoNotaDebito.setValorTotal(formatDecimal(valorTotal));
    infoNotaDebito.setPagos(crearPagos(debitNoteDto));

    return infoNotaDebito;
  }

  private BigDecimal calcularTotalSinImpuestos(DebitNoteDto debitNoteDto) {
    // Calcular desde los detalles (productos) - estos son la fuente de verdad
    if (debitNoteDto.getDebitNoteDetails() != null
        && !debitNoteDto.getDebitNoteDetails().isEmpty()) {
      return debitNoteDto.getDebitNoteDetails().stream()
          .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    return BigDecimal.ZERO;
  }

  private BigDecimal calcularTotalImpuestos(DebitNoteDto debitNoteDto) {
    BigDecimal totalIva = calcularTotalIva(debitNoteDto);
    BigDecimal totalIce =
        debitNoteDto.getTotalIce() != null ? debitNoteDto.getTotalIce() : BigDecimal.ZERO;
    return totalIva.add(totalIce);
  }

  private BigDecimal calcularTotalIva(DebitNoteDto debitNoteDto) {
    BigDecimal totalIva = BigDecimal.ZERO;

    if (debitNoteDto.getDebitNoteDetails() != null) {
      for (InvoiceDetailDto detail : debitNoteDto.getDebitNoteDetails()) {
        BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
        // Obtener rate del producto, o del detalle (ivaPercentage), o usar 0
        BigDecimal rate = BigDecimal.ZERO;
        if (detail.getProductDto() != null && detail.getProductDto().getRate() != null) {
          rate = detail.getProductDto().getRate();
        } else if (detail.getIvaPercentage() != null) {
          rate =
              detail
                  .getIvaPercentage()
                  .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        }

        // Si tiene ICE, agregarlo a la base
        BigDecimal baseIva = subtotal;
        if (detail.getProductDto() != null
            && detail.getProductDto().getIceRate() != null
            && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0) {
          BigDecimal iceDeEsteProducto =
              UtilsMath.iva(subtotal, detail.getProductDto().getIceRate());
          baseIva = subtotal.add(iceDeEsteProducto);
        }

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
      }
    }

    return totalIva;
  }

  public List<ImpuestoNotaDebito> crearImpuestos(DebitNoteDto debitNoteDto) {
    List<ImpuestoNotaDebito> impuestos = new ArrayList<>();

    // Calcular IVA
    BigDecimal totalIva = calcularTotalIva(debitNoteDto);
    if (totalIva != null && totalIva.compareTo(BigDecimal.ZERO) > 0) {
      // Calcular base imponible total (suma de subtotales + ICE si existe)
      BigDecimal baseImponible = BigDecimal.ZERO;
      BigDecimal rate = BigDecimal.ZERO;

      if (debitNoteDto.getDebitNoteDetails() != null
          && !debitNoteDto.getDebitNoteDetails().isEmpty()) {
        // Calcular base imponible y obtener rate del primer detalle con IVA
        for (InvoiceDetailDto detail : debitNoteDto.getDebitNoteDetails()) {
          BigDecimal subtotal =
              detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;

          // Obtener rate del producto, o del detalle (ivaPercentage), o usar 0
          BigDecimal detailRate = BigDecimal.ZERO;
          if (detail.getProductDto() != null && detail.getProductDto().getRate() != null) {
            detailRate = detail.getProductDto().getRate();
          } else if (detail.getIvaPercentage() != null) {
            detailRate =
                detail
                    .getIvaPercentage()
                    .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
          }

          // Si este detalle tiene IVA, usar su rate para el impuesto
          if (detailRate.compareTo(BigDecimal.ZERO) > 0 && rate.compareTo(BigDecimal.ZERO) == 0) {
            rate = detailRate;
          }

          // Si tiene ICE, agregarlo a la base
          if (detail.getProductDto() != null
              && detail.getProductDto().getIceRate() != null
              && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal iceDeEsteProducto =
                UtilsMath.iva(subtotal, detail.getProductDto().getIceRate());
            baseImponible = baseImponible.add(subtotal.add(iceDeEsteProducto));
          } else {
            baseImponible = baseImponible.add(subtotal);
          }
        }
      }

      ImpuestoNotaDebito impuestoIva = new ImpuestoNotaDebito();
      impuestoIva.setCodigo("2"); // IVA
      impuestoIva.setCodigoPorcentaje(mapIvaRateToCodigoPorcentaje(rate));
      impuestoIva.setBaseImponible(formatBaseImponible(baseImponible));
      impuestoIva.setTarifa(formatTarifa(rate)); // OBLIGATORIO en ND
      // Usar el totalIva calculado correctamente
      impuestoIva.setValor(formatDecimal(totalIva));
      impuestos.add(impuestoIva);
    }

    // Calcular ICE si existe
    BigDecimal totalIce =
        debitNoteDto.getTotalIce() != null ? debitNoteDto.getTotalIce() : BigDecimal.ZERO;
    if (totalIce != null && totalIce.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal baseImponibleIce = BigDecimal.ZERO;
      BigDecimal iceRate = new BigDecimal(SriTaxConstants.DEFAULT_ICE_RATE);

      if (debitNoteDto.getDebitNoteDetails() != null) {
        for (InvoiceDetailDto detail : debitNoteDto.getDebitNoteDetails()) {
          if (detail.getProductDto() != null
              && detail.getProductDto().getIceRate() != null
              && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal subtotal =
                detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
            baseImponibleIce = baseImponibleIce.add(subtotal);
            iceRate = detail.getProductDto().getIceRate();
          }
        }
      }

      ImpuestoNotaDebito impuestoIce = new ImpuestoNotaDebito();
      impuestoIce.setCodigo("3"); // ICE
      impuestoIce.setCodigoPorcentaje(SriTaxConstants.DEFAULT_ICE_PERCENTAGE_CODE);
      impuestoIce.setBaseImponible(formatBaseImponible(baseImponibleIce));
      impuestoIce.setTarifa(formatTarifa(iceRate)); // OBLIGATORIO
      impuestoIce.setValor(formatDecimal(totalIce));
      impuestos.add(impuestoIce);
    }

    // El SRI requiere que el elemento impuestos esté presente y contenga al menos un impuesto
    // Si no hay impuestos reales, crear un impuesto IVA con valor 0
    if (impuestos.isEmpty()) {
      BigDecimal totalSinImpuestos = calcularTotalSinImpuestos(debitNoteDto);
      ImpuestoNotaDebito impuestoCero = new ImpuestoNotaDebito();
      impuestoCero.setCodigo("2"); // IVA
      impuestoCero.setCodigoPorcentaje("0"); // Sin IVA
      impuestoCero.setBaseImponible(formatBaseImponible(totalSinImpuestos));
      impuestoCero.setTarifa("0"); // Tarifa 0%
      impuestoCero.setValor("0"); // Valor 0
      impuestos.add(impuestoCero);
    }

    return impuestos;
  }

  public List<MotivoNotaDebito> crearMotivos(DebitNoteDto debitNoteDto) {
    List<MotivoNotaDebito> motivos = new ArrayList<>();

    // Validar que siempre haya detalles (productos) - requerido por SRI
    if (debitNoteDto.getDebitNoteDetails() == null
        || debitNoteDto.getDebitNoteDetails().isEmpty()) {
      throw new IllegalArgumentException(
          "La nota de débito debe tener al menos un detalle (producto) para generar los motivos");
    }

    // Crear un motivo por cada detalle (producto)
    for (InvoiceDetailDto detail : debitNoteDto.getDebitNoteDetails()) {
      if (detail.getProductDto() == null) {
        throw new IllegalArgumentException("Cada detalle debe tener un producto asociado");
      }

      // Usar el nombre del producto como razón, o la descripción si el nombre está vacío
      String razon =
          StringUtils.hasText(detail.getProductDto().getName())
              ? detail.getProductDto().getName()
              : (StringUtils.hasText(detail.getProductDto().getDescription())
                  ? detail.getProductDto().getDescription()
                  : "Producto sin descripción");

      // Validar que el subtotal sea válido
      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
      if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
            String.format("El subtotal del producto '%s' debe ser mayor a cero", razon));
      }

      MotivoNotaDebito motivo = new MotivoNotaDebito();
      motivo.setRazon(razon);
      motivo.setValor(formatDecimal(subtotal));
      motivos.add(motivo);
    }

    return motivos;
  }

  public List<Pago> crearPagos(DebitNoteDto debitNoteDto) {
    List<Pago> pagos = new ArrayList<>();

    // Calcular el valorTotal correcto (totalSinImpuestos + impuestos)
    BigDecimal totalSinImpuestos = calcularTotalSinImpuestos(debitNoteDto);
    BigDecimal totalImpuestos = calcularTotalImpuestos(debitNoteDto);
    BigDecimal valorTotal = totalSinImpuestos.add(totalImpuestos);

    if (debitNoteDto.getPaymentMethods() != null && !debitNoteDto.getPaymentMethods().isEmpty()) {
      for (PaymentDto paymentDto : debitNoteDto.getPaymentMethods()) {
        Pago pago = new Pago();

        // Mapear código de forma de pago
        String formaPagoCode = paymentDto.getPaymentMethodCode();
        if (StringUtils.hasText(formaPagoCode)) {
          try {
            pago.setFormaPago(FormaPagoEnum.valueOf(mapPaymentCodeToEnum(formaPagoCode)));
          } catch (IllegalArgumentException e) {
            log.warn("Código de pago {} no reconocido, usando 01 por defecto", formaPagoCode);
            pago.setFormaPago(FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO);
          }
        } else {
          pago.setFormaPago(FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO);
        }

        // Usar el valorTotal calculado en lugar del totalAmount del DTO
        BigDecimal totalPago =
            paymentDto.getTotal() != null
                ? paymentDto.getTotal()
                : (paymentDto.getValue() != null ? paymentDto.getValue() : valorTotal);
        // Validar que el total del pago no exceda el valorTotal
        if (totalPago.compareTo(valorTotal) > 0) {
          log.warn(
              "⚠️ [ND] El total del pago (%.2f) excede el valorTotal (%.2f). Se ajustará al valorTotal.",
              totalPago, valorTotal);
          totalPago = valorTotal;
        }
        pago.setTotal(formatDecimal(totalPago));

        if (paymentDto.getTerm() != null) {
          pago.setPlazo(paymentDto.getTerm().toString());
        }
        if (paymentDto.getTimeUnit() != null || paymentDto.getTime() != null) {
          pago.setUnidadTiempo(
              paymentDto.getTimeUnit() != null ? paymentDto.getTimeUnit() : paymentDto.getTime());
        }

        pagos.add(pago);
      }
    } else {
      // Fallback: crear un pago por defecto usando el valorTotal calculado
      Pago pago = new Pago();
      pago.setFormaPago(FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO);
      pago.setTotal(formatDecimal(valorTotal));
      pagos.add(pago);
    }

    return pagos;
  }

  private String mapPaymentCodeToEnum(String code) {
    return switch (code) {
      case "01" -> "SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO";
      case "15" -> "COMPENSACION_DE_DEUDAS";
      case "16" -> "TARJETA_DE_DEBITO";
      case "17" -> "DINERO_ELECTRONICO";
      case "18" -> "TARJETA_PREPAGO";
      case "19" -> "TARJETA_DE_CREDITO";
      case "20" -> "OTROS_CON_UTILIZACION_DEL_SISTEMA_FINANCIERO";
      case "21" -> "ENDOSO_DE_TITULOS";
      default -> "SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO";
    };
  }

  public InfoTributaria crearInfoTributaria(
      DebitNoteDto debitNoteDto, AuthorizationSri existingAuthorizationSri) {
    InfoTributaria infoTributaria = new InfoTributaria();

    infoTributaria.setAmbiente(
        sriConfiguration.isPruebas() ? AmbienteEnum.PRUEBAS : AmbienteEnum.PRODUCCION);
    infoTributaria.setTipoEmision(TipoEmisionEnum.NORMAL);
    infoTributaria.setRazonSocial(debitNoteDto.getCompanyDto().getName());
    infoTributaria.setNombreComercial(debitNoteDto.getCompanyDto().getNameCommercial());
    infoTributaria.setRuc(debitNoteDto.getCompanyDto().getIdentification());

    // Código de documento: "05" para Nota de Débito
    String codDoc = TipoComprobanteCode.NOTA_DEBITO.getCode();
    infoTributaria.setCodDoc(codDoc);
    log.info("🔍 [Nota de Débito] codDoc establecido: {}", codDoc);
    infoTributaria.setEstab(debitNoteDto.getEstablishment().getCode());
    infoTributaria.setPtoEmi(debitNoteDto.getEstablishment().getIssuancePoint().getCode());
    infoTributaria.setSecuencial(
        debitNoteDto.getEstablishment().getIssuancePoint().getSequentialInvoice());

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
    String establishmentTypeStr = debitNoteDto.getEstablishment().getType();
    EstablishmentType establishmentType = EstablishmentType.fromCode(establishmentTypeStr);

    if (establishmentType == EstablishmentType.MATRIZ) {
      direccionMatriz = debitNoteDto.getEstablishment().getAddress();
    } else if (establishmentType == EstablishmentType.SUCURSAL) {
      direccionMatriz = debitNoteDto.getEstablishment().getParentAddress();
      if (direccionMatriz == null || direccionMatriz.trim().isEmpty()) {
        direccionMatriz =
            getContact(ContactTypeEnum.ADD.name(), debitNoteDto.getCompanyDto().getContacts());
      }
    } else {
      direccionMatriz =
          getContact(ContactTypeEnum.ADD.name(), debitNoteDto.getCompanyDto().getContacts());
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
            com.wallparisoft.integration.sri.enums.TipoComprobanteEnum.NOTA_DE_DEBITO,
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

    // Mapear a codigoPorcentaje (código SRI para porcentaje de IVA)
    return switch (percentage) {
      case 0 -> "0";
      case 5 -> "5";
      case 13 -> "10";
      case 15 -> "4";
      case 12 -> {
        log.warn("⚠️ IVA 12% está OBSOLETO. Usa 15% o 13% según corresponda.");
        yield "2";
      }
      case 14 -> {
        log.warn("⚠️ IVA 14% está OBSOLETO. Usa 15% o 13% según corresponda.");
        yield "3";
      }
      default -> {
        log.error("❌ IVA {}% NO ES RECONOCIDO.", percentage);
        if (percentage <= 5) {
          yield "0";
        } else if (percentage <= 13) {
          yield "10";
        } else {
          yield "4";
        }
      }
    };
  }

  private String formatTarifa(BigDecimal rate) {
    if (rate == null) {
      return "0";
    }
    // Detectar si el rate viene como decimal (0.15) o como porcentaje (15.0)
    // Si el rate es >= 1, asumimos que viene como porcentaje (15.0)
    // Si el rate es < 1, asumimos que viene como decimal (0.15)
    int percentage;
    if (rate.compareTo(BigDecimal.ONE) >= 0) {
      // Ya viene como porcentaje (ej: 15.0)
      percentage = rate.intValue();
    } else {
      // Viene como decimal (ej: 0.15), convertir a porcentaje
      percentage = rate.multiply(new BigDecimal("100")).intValue();
    }
    // Retornar como entero sin decimales (el SRI espera "15" no "15.00")
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

  /**
   * Formatea un BigDecimal a String usando punto como separador decimal. Asegura que siempre se use
   * formato US (punto) independientemente del locale del sistema.
   *
   * @param value el valor BigDecimal a formatear
   * @return String con el valor formateado usando punto como separador decimal
   */
  private String formatDecimal(BigDecimal value) {
    if (value == null) {
      return "0";
    }
    // Usar toPlainString() para evitar problemas de locale (siempre usa punto)
    return value.stripTrailingZeros().toPlainString();
  }
}
