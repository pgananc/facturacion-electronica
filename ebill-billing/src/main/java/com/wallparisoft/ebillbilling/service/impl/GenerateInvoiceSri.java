package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.config.SriConfiguration;
import com.wallparisoft.ebillbilling.constants.SriTaxConstants;
import com.wallparisoft.ebillbilling.dto.ContactDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.dto.PaymentDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.enums.ContactTypeEnum;
import com.wallparisoft.ebillbilling.enums.EstablishmentType;
import com.wallparisoft.ebillbilling.enums.TipoComprobanteCode;
import com.wallparisoft.integration.sri.enums.AmbienteEnum;
import com.wallparisoft.integration.sri.enums.FormaPagoEnum;
import com.wallparisoft.integration.sri.enums.MonedaEnum;
import com.wallparisoft.integration.sri.enums.ObligadoContabilidadEnum;
import com.wallparisoft.integration.sri.enums.TipoComprobanteEnum;
import com.wallparisoft.integration.sri.enums.TipoEmisionEnum;
import com.wallparisoft.integration.sri.enums.TipoIdentificacionCompradorEnum;
import com.wallparisoft.integration.sri.model.Detalle;
import com.wallparisoft.integration.sri.model.Factura;
import com.wallparisoft.integration.sri.model.Impuesto;
import com.wallparisoft.integration.sri.model.InfoFactura;
import com.wallparisoft.integration.sri.model.InfoTributaria;
import com.wallparisoft.integration.sri.model.Pago;
import com.wallparisoft.integration.sri.model.TotalImpuesto;
import com.wallparisoft.integration.sri.util.UtilsMath;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateInvoiceSri {

  private final SriConfiguration sriConfiguration;

  public Factura createInvoiceSri(InvoiceDto invoiceDto) {
    return createInvoiceSri(invoiceDto, null);
  }

  public Factura createInvoiceSri(
      InvoiceDto invoiceDto, AuthorizationSri existingAuthorizationSri) {
    Factura factura = new Factura();
    factura.setVersion("2.1.0");
    factura.setId("comprobante");

    // Obtener el ICE del DTO
    BigDecimal totalIce =
        invoiceDto.getSubtotalIce() != null ? invoiceDto.getSubtotalIce() : BigDecimal.ZERO;

    factura.setInfoTributaria(crearInfoTributaria(invoiceDto, existingAuthorizationSri));
    factura.setInfoFactura(crearInfoFactura(invoiceDto));
    factura.setDetalles(crearDetalles(invoiceDto.getInvoiceDetails(), totalIce));

    return factura;
  }

  private List<Detalle> crearDetalles(
      List<InvoiceDetailDto> invoiceDetailDto, BigDecimal totalIce) {
    List<Detalle> detalles = new ArrayList<Detalle>();

    // El ICE se agrega como impuesto al primer detalle
    boolean iceAgregado = false;

    for (InvoiceDetailDto detail : invoiceDetailDto) {
      Detalle detalle = new Detalle();
      detalle.setCodigoPrincipal(detail.getProductDto().getMainCode());
      detalle.setCodigoAuxiliar("0");
      detalle.setDescripcion(detail.getProductDto().getName());
      detalle.setCantidad(String.valueOf(detail.getQuantity()));
      detalle.setPrecioUnitario(String.valueOf(detail.getUnitPrice()));

      // El precioTotalSinImpuesto es SOLO el subtotal (cantidad × precio - descuento)
      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
      detalle.setPrecioTotalSinImpuesto(String.valueOf(subtotal));
      detalle.setDescuento(String.valueOf(detail.getDiscount() != null ? detail.getDiscount() : 0));
      detalles.add(detalle);

      List<Impuesto> impuestos = new ArrayList<Impuesto>();

      // Variable para saber si este detalle tiene ICE
      boolean tieneIce = false;

      // 1. Agregar ICE como primer impuesto (si existe y es el primer detalle)
      if (!iceAgregado && totalIce != null && totalIce.compareTo(BigDecimal.ZERO) > 0) {
        Impuesto impuestoIce = new Impuesto();
        impuestoIce.setCodigo(SriTaxConstants.CODE_ICE);
        impuestoIce.setCodigoPorcentaje(SriTaxConstants.DEFAULT_ICE_PERCENTAGE_CODE);

        // Obtener la tarifa real del ICE desde el producto
        BigDecimal iceTarifaReal =
            detail.getProductDto().getIceRate() != null
                ? detail.getProductDto().getIceRate()
                : new BigDecimal(SriTaxConstants.DEFAULT_ICE_RATE);
        impuestoIce.setTarifa(String.valueOf(iceTarifaReal));

        // Formatear baseImponible sin decimales innecesarios
        String baseImponibleIceFormateada = formatBaseImponible(subtotal);
        impuestoIce.setBaseImponible(baseImponibleIceFormateada);
        impuestoIce.setValor(String.valueOf(totalIce));
        impuestos.add(impuestoIce);
        iceAgregado = true;
        tieneIce = true;
      }

      // 2. Agregar IVA (la base imponible del IVA es subtotal + ICE si este detalle tiene ICE)
      Impuesto impuestoIva = new Impuesto();

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

      // La base imponible del IVA = subtotal + ICE (si este detalle tiene ICE)
      BigDecimal baseImponibleIva = subtotal;
      if (tieneIce) {
        baseImponibleIva = subtotal.add(totalIce);
      }

      // Formatear tarifa sin decimales innecesarios
      String tarifaFormateada = formatTarifa(rate);
      impuestoIva.setTarifa(tarifaFormateada);

      // Formatear baseImponible sin decimales innecesarios
      String baseImponibleFormateada = formatBaseImponible(baseImponibleIva);
      impuestoIva.setBaseImponible(baseImponibleFormateada);

      impuestoIva.setValor(String.valueOf(UtilsMath.iva(baseImponibleIva, rate)));
      impuestos.add(impuestoIva);

      detalle.setImpuestos(impuestos);

      // Solo agregar detalles adicionales si hay información válida
      // Por ahora, dejar vacío - se puede implementar después si se necesita
      // detalle.setDetallesAdicionales(new ArrayList<>());
    }

    return detalles;
  }

  public InfoFactura crearInfoFactura(InvoiceDto invoiceDto) {
    InfoFactura infoFactura = new InfoFactura();
    // Fix: YYYY (año de semana ISO) → yyyy (año del calendario)
    infoFactura.setFechaEmision((new SimpleDateFormat("dd/MM/yyyy")).format(new Date()));

    // Dirección del establecimiento actual (donde se emite la factura)
    // Siempre es la dirección del establecimiento, sea MATRIZ o SUCURSAL
    infoFactura.setDirEstablecimiento(
        invoiceDto.getEstablishment().getAddress() != null
            ? invoiceDto.getEstablishment().getAddress()
            : "S/N");

    // Obligado a llevar contabilidad (de la empresa)
    String forcedToAccounting =
        invoiceDto.getCompanyDto() != null
            ? invoiceDto.getCompanyDto().getForcedToAccounting()
            : null;
    ObligadoContabilidadEnum obligadoContabilidad =
        (forcedToAccounting != null && "S".equalsIgnoreCase(forcedToAccounting.trim()))
            ? ObligadoContabilidadEnum.SI
            : ObligadoContabilidadEnum.NO;
    infoFactura.setObligadoContabilidad(obligadoContabilidad);

    TipoIdentificacionCompradorEnum tipoMapeado =
        mapIdTypeToTipoIdentificacion(invoiceDto.getClientDto().getIdType());

    infoFactura.setTipoIdentificacionComprador(tipoMapeado);
    infoFactura.setRazonSocialComprador(invoiceDto.getClientDto().getName());
    infoFactura.setIdentificacionComprador(invoiceDto.getClientDto().getIdentification());

    // Extraer y setear la dirección del comprador desde los contactos
    String direccionComprador = extractBuyerAddressFromInvoiceDto(invoiceDto);
    infoFactura.setDireccionComprador(
        StringUtils.hasText(direccionComprador) ? direccionComprador : "S/N");

    // IMPORTANTE: Para el SRI, totalSinImpuestos es el total ANTES de descuentos
    // Se calcula: unitPrice × quantity (para cada producto)
    BigDecimal totalSinImpuestos =
        invoiceDto.getInvoiceDetails().stream()
            .map(
                detail -> {
                  BigDecimal unitPrice =
                      detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                  BigDecimal quantity =
                      detail.getQuantity() != null
                          ? new BigDecimal(detail.getQuantity())
                          : BigDecimal.ONE;
                  return unitPrice.multiply(quantity);
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Reportar el total de descuentos (para información al SRI)
    BigDecimal totalDescuentos =
        invoiceDto.getInvoiceDetails().stream()
            .map(detail -> detail.getDiscount() != null ? detail.getDiscount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Agregar ICE si existe en el DTO
    BigDecimal totalIce =
        invoiceDto.getSubtotalIce() != null ? invoiceDto.getSubtotalIce() : BigDecimal.ZERO;

    // El subtotal real (después de descuentos) para cálculo de IVA
    BigDecimal subtotalConDescuentos = totalSinImpuestos.subtract(totalDescuentos);

    // Calcular el IVA sumando el IVA de cada producto (ya que pueden tener diferentes tarifas)
    BigDecimal totalIva = BigDecimal.ZERO;
    for (InvoiceDetailDto detail : invoiceDto.getInvoiceDetails()) {
      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
      BigDecimal rate =
          detail.getProductDto().getRate() != null
              ? detail.getProductDto().getRate()
              : BigDecimal.ZERO;

      // Si este producto tiene ICE, calcular el ICE de este producto específico y sumarlo a la
      // base del IVA
      BigDecimal baseIva = subtotal;
      if (detail.getProductDto().getIceRate() != null
          && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0) {
        // Calcular el ICE de ESTE producto: subtotal × (iceRate/100)
        BigDecimal iceDeEsteProducto = UtilsMath.iva(subtotal, detail.getProductDto().getIceRate());
        baseIva = subtotal.add(iceDeEsteProducto);
      }

      // Calcular el IVA de este producto
      BigDecimal ivaDeEsteProducto = UtilsMath.iva(baseIva, rate);
      totalIva = totalIva.add(ivaDeEsteProducto);
    }

    // Total de impuestos = IVA + ICE
    BigDecimal totalImpuestos = totalIva.add(totalIce);

    // Calcular el importe total = subtotalConDescuentos + impuestos + propina
    BigDecimal propina = invoiceDto.getTip() != null ? invoiceDto.getTip() : BigDecimal.ZERO;
    BigDecimal importeTotal = subtotalConDescuentos.add(totalImpuestos).add(propina);

    // ✅ IMPORTANTE: El SRI espera que totalSinImpuestos sea la suma de los
    // precioTotalSinImpuesto de los detalles (CON descuentos aplicados)
    infoFactura.setTotalSinImpuestos(String.valueOf(subtotalConDescuentos));
    infoFactura.setTotalDescuento(String.valueOf(totalDescuentos));
    infoFactura.setTotalConImpuestos(crearTotalImpuestos(invoiceDto));
    infoFactura.setPropina(UtilsMath.redondearTotalS(propina)); // Reportar la propina
    infoFactura.setImporteTotal(UtilsMath.redondearTotalS(importeTotal));
    infoFactura.setMoneda(MonedaEnum.DOLAR);
    infoFactura.setPagos(crearPagos(invoiceDto)); // Usar paymentMethods del invoiceDto
    return infoFactura;
  }

  private List<Pago> crearPagos(InvoiceDto invoiceDto) {
    List<Pago> pagos = new ArrayList<Pago>();

    // Si hay paymentMethods en el invoiceDto, usarlos
    if (invoiceDto.getPaymentMethods() != null && !invoiceDto.getPaymentMethods().isEmpty()) {
      for (PaymentDto paymentDto : invoiceDto.getPaymentMethods()) {
        Pago pago = new Pago();

        // Mapear el código del método de pago al enum FormaPagoEnum
        FormaPagoEnum formaPago =
            mapPaymentMethodCodeToFormaPagoEnum(paymentDto.getPaymentMethodCode());
        pago.setFormaPago(formaPago);

        // Usar el valor del paymentDto (puede ser value o total)
        BigDecimal monto =
            paymentDto.getValue() != null ? paymentDto.getValue() : paymentDto.getTotal();
        if (monto == null) {
          monto = BigDecimal.ZERO;
        }
        pago.setTotal(UtilsMath.redondearTotalS(monto));

        // Setear plazo y unidad de tiempo si están disponibles
        if (paymentDto.getTerm() != null) {
          pago.setPlazo(String.valueOf(paymentDto.getTerm()));
        }
        if (StringUtils.hasText(paymentDto.getTimeUnit())) {
          pago.setUnidadTiempo(paymentDto.getTimeUnit());
        } else if (StringUtils.hasText(paymentDto.getTime())) {
          pago.setUnidadTiempo(paymentDto.getTime());
        }

        pagos.add(pago);
      }
    }

    return pagos;
  }

  /**
   * Mapea el código del método de pago (String) al enum FormaPagoEnum del SRI
   *
   * @param paymentMethodCode Código del método de pago (ej: "01", "19")
   * @return FormaPagoEnum correspondiente, o SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO por defecto
   */
  private FormaPagoEnum mapPaymentMethodCodeToFormaPagoEnum(String paymentMethodCode) {
    if (!StringUtils.hasText(paymentMethodCode)) {
      return FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO;
    }

    switch (paymentMethodCode) {
      case "01":
        return FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO;
      case "15":
        return FormaPagoEnum.COMPENSACION_DE_DEUDAS;
      case "16":
        return FormaPagoEnum.TARJETA_DE_DEBITO;
      case "17":
        return FormaPagoEnum.DINERO_ELECTRONICO;
      case "18":
        return FormaPagoEnum.TARJETA_PREPAGO;
      case "19":
        return FormaPagoEnum.TARJETA_DE_CREDITO;
      case "20":
        return FormaPagoEnum.OTROS_CON_UTILIZACION_DEL_SISTEMA_FINANCIERO;
      case "21":
        return FormaPagoEnum.ENDOSO_DE_TITULOS;
      default:
        log.warn(
            "Código de método de pago no reconocido: {}, usando SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO por defecto",
            paymentMethodCode);
        return FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO;
    }
  }

  public List<TotalImpuesto> crearTotalImpuestos(InvoiceDto invoiceDto) {
    List<TotalImpuesto> totalImpuestos = new ArrayList<TotalImpuesto>();

    // Calcular el total sin impuestos (suma de subtotales ya con descuentos aplicados)
    BigDecimal totalSinImpuestos =
        invoiceDto.getInvoiceDetails().stream()
            .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Agregar ICE si existe
    BigDecimal totalIce =
        invoiceDto.getSubtotalIce() != null ? invoiceDto.getSubtotalIce() : BigDecimal.ZERO;

    // 1. Agregar TotalImpuesto para el ICE (si existe)
    if (totalIce.compareTo(BigDecimal.ZERO) > 0) {
      // IMPORTANTE: La base imponible del ICE debe ser SOLO la suma de los productos
      // que tienen ICE aplicado, NO todos los productos de la factura.
      // El SRI valida que: baseImponible × (tarifa/100) = valor
      BigDecimal baseImponibleIce =
          invoiceDto.getInvoiceDetails().stream()
              .filter(
                  detail ->
                      detail.getProductDto() != null
                          && detail.getProductDto().getIceRate() != null
                          && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0)
              .map(detail -> detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      // Obtener la tarifa real del ICE desde el producto que lo tiene
      BigDecimal iceTarifaReal =
          invoiceDto.getInvoiceDetails().stream()
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
      totalImpuestoIce.setTarifa(String.valueOf(iceTarifaReal));
      // Formatear baseImponible sin decimales innecesarios
      String baseImponibleIceFormateada = formatBaseImponible(baseImponibleIce);
      totalImpuestoIce.setBaseImponible(baseImponibleIceFormateada);
      totalImpuestoIce.setValor(String.valueOf(totalIce));
      totalImpuestos.add(totalImpuestoIce);

      log.info(
          "📊 ICE Total - Base: {}, Tarifa: {}%, Valor: {}",
          baseImponibleIce, iceTarifaReal, totalIce);
    }

    // 2. Agregar TotalImpuesto para el IVA agrupando por tarifa
    // Necesitamos un TotalImpuesto por cada tarifa diferente (0%, 12%, 15%, etc.)
    Map<BigDecimal, BigDecimal> ivasPorTarifa = new HashMap<>();

    for (InvoiceDetailDto detail : invoiceDto.getInvoiceDetails()) {
      BigDecimal rate =
          detail.getProductDto().getRate() != null
              ? detail.getProductDto().getRate()
              : BigDecimal.ZERO;

      BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;

      // Si este producto tiene ICE, calcular el ICE de este producto específico y sumarlo a la base
      // del IVA
      BigDecimal baseIva = subtotal;
      if (detail.getProductDto().getIceRate() != null
          && detail.getProductDto().getIceRate().compareTo(BigDecimal.ZERO) > 0) {
        // Calcular el ICE de ESTE producto: subtotal × (iceRate/100)
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

      // Mapear porcentaje a código SRI
      String codigoPorcentajeSRI = mapIvaRateToCodigoPorcentaje(rate);
      totalImpuestoIva.setCodigoPorcentaje(codigoPorcentajeSRI);

      // Formatear tarifa y base
      String tarifaFormateada = formatTarifa(rate);
      totalImpuestoIva.setTarifa(tarifaFormateada);

      String baseImponibleFormateada = formatBaseImponible(baseImponible);
      totalImpuestoIva.setBaseImponible(baseImponibleFormateada);

      totalImpuestoIva.setValor(String.valueOf(valorIva));
      totalImpuestos.add(totalImpuestoIva);

      log.info("📊 IVA {}% - Base: {}, Valor: {}", rate, baseImponible, valorIva);
    }

    return totalImpuestos;
  }

  public InfoTributaria crearInfoTributaria(InvoiceDto invoiceDto) {
    return crearInfoTributaria(invoiceDto, null);
  }

  public InfoTributaria crearInfoTributaria(
      InvoiceDto invoiceDto, AuthorizationSri existingAuthorizationSri) {
    InfoTributaria infoTributaria = new InfoTributaria();

    // IMPORTANTE: El ambiente se obtiene de la configuración del servidor, NO del request
    // Esto previene que un usuario malicioso cambie el ambiente a producción
    infoTributaria.setAmbiente(
        sriConfiguration.isPruebas() ? AmbienteEnum.PRUEBAS : AmbienteEnum.PRODUCCION);
    infoTributaria.setTipoEmision(TipoEmisionEnum.NORMAL);
    infoTributaria.setRazonSocial(invoiceDto.getCompanyDto().getName());
    infoTributaria.setNombreComercial(invoiceDto.getCompanyDto().getNameCommercial());
    infoTributaria.setRuc(invoiceDto.getCompanyDto().getIdentification());

    // Primero setear estos valores porque la clave de acceso los necesita
    infoTributaria.setCodDoc(TipoComprobanteCode.FACTURA.getCode());
    infoTributaria.setEstab(invoiceDto.getEstablishment().getCode());
    infoTributaria.setPtoEmi(invoiceDto.getEstablishment().getIssuancePoint().getCode());
    infoTributaria.setSecuencial(
        invoiceDto.getEstablishment().getIssuancePoint().getSequentialInvoice());

    // Reutilizar la clave de acceso existente si existe, para evitar el error "CLAVE ACCESO
    // REGISTRADA"
    // en reintentos. Solo generar una nueva clave si no existe una previa.
    if (existingAuthorizationSri != null
        && StringUtils.hasText(existingAuthorizationSri.getAccessKey())) {
      infoTributaria.setClaveAcceso(existingAuthorizationSri.getAccessKey());
      log.debug(
          "Reutilizando clave de acceso existente: {} para evitar error 'CLAVE ACCESO REGISTRADA'",
          existingAuthorizationSri.getAccessKey());
    } else {
      // Generar nueva clave de acceso solo si no existe una previa
      infoTributaria.setClaveAcceso(generateAccessKey(infoTributaria));
      log.debug("Generando nueva clave de acceso");
    }

    // Setear dirección matriz según el tipo de establecimiento
    String direccionMatriz;
    String establishmentTypeStr = invoiceDto.getEstablishment().getType();
    EstablishmentType establishmentType = EstablishmentType.fromCode(establishmentTypeStr);

    if (establishmentType == EstablishmentType.MATRIZ) {
      // Si es MATRIZ, la dirección matriz es la misma del establecimiento
      direccionMatriz = invoiceDto.getEstablishment().getAddress();
    } else if (establishmentType == EstablishmentType.SUCURSAL) {
      // Si es SUCURSAL, usar la dirección del establecimiento padre
      direccionMatriz = invoiceDto.getEstablishment().getParentAddress();
      if (direccionMatriz == null || direccionMatriz.trim().isEmpty()) {
        // Fallback a contactos de la compañía si no viene parentAddress
        direccionMatriz =
            getContact(ContactTypeEnum.ADD.name(), invoiceDto.getCompanyDto().getContacts());
      }
    } else {
      // Si no especifica tipo, usar dirección de contactos (comportamiento anterior)
      direccionMatriz =
          getContact(ContactTypeEnum.ADD.name(), invoiceDto.getCompanyDto().getContacts());
    }

    infoTributaria.setDirMatriz(direccionMatriz != null ? direccionMatriz : "S/N");

    return infoTributaria;
  }

  public String getContact(String contactType, List<ContactDto> contacts) {

    Optional<ContactDto> contactOpt =
        contacts.stream().filter(c -> c.getContactType().equals(contactType)).findFirst();

    return contactOpt.map(ContactDto::getValue).orElse("S/N");
  }

  /**
   * Mapea el idType del cliente al enum TipoIdentificacionCompradorEnum del SRI Códigos SRI: 04 =
   * RUC 05 = CEDULA 06 = PASAPORTE 07 = CONSUMIDOR FINAL 08 = IDENTIFICACION DEL EXTERIOR 09 =
   * PLACA
   *
   * @param idType Tipo de identificación del cliente (1=CEDULA, 2=RUC, 3=PASAPORTE)
   * @return El enum correspondiente del SRI
   */
  private TipoIdentificacionCompradorEnum mapIdTypeToTipoIdentificacion(Integer idType) {
    log.info("🔍 [mapIdTypeToTipoIdentificacion] idType recibido: {}", idType);

    if (idType == null) {
      log.warn("⚠️ idType es null, usando CONSUMIDOR_FINAL por defecto");
      return TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
    }

    TipoIdentificacionCompradorEnum resultado =
        switch (idType) {
          case 1 -> {
            log.info("🔍 [mapIdTypeToTipoIdentificacion] idType=1 → CEDULA (código SRI: 05)");
            yield TipoIdentificacionCompradorEnum.CEDULA;
          }
          case 2 -> {
            log.info("🔍 [mapIdTypeToTipoIdentificacion] idType=2 → RUC (código SRI: 04)");
            yield TipoIdentificacionCompradorEnum.RUC;
          }
          case 3 -> {
            log.info("🔍 [mapIdTypeToTipoIdentificacion] idType=3 → PASAPORTE (código SRI: 06)");
            yield TipoIdentificacionCompradorEnum.PASAPORTE;
          }
          case 4 -> {
            log.info(
                "🔍 [mapIdTypeToTipoIdentificacion] idType=4 → CONSUMIDOR_FINAL (código SRI: 07)");
            yield TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
          }
          case 5 -> {
            log.info(
                "🔍 [mapIdTypeToTipoIdentificacion] idType=5 → IDENTIFICACION_DEL_EXTERIOR (código SRI: 08)");
            yield TipoIdentificacionCompradorEnum.IDENTIFICACION_DEL_EXTERIOR;
          }
          default -> {
            log.warn("⚠️ idType {} no reconocido, usando CONSUMIDOR_FINAL por defecto", idType);
            yield TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
          }
        };

    log.info("🔍 [mapIdTypeToTipoIdentificacion] Resultado final: {}", resultado);
    return resultado;
  }

  public String generateAccessKey(InfoTributaria infoTributaria) {
    Calendar cal = Calendar.getInstance();
    String accessKey =
        infoTributaria.generarClaveAcceso(cal.getTime(), TipoComprobanteEnum.FACTURA, "12345678");
    if (accessKey.length() == 49) {
      return accessKey;
    } else {
      return generateAccessKey(infoTributaria);
    }
  }

  /**
   * Mapea el rate de IVA (BigDecimal) al código de porcentaje según el catálogo del SRI.
   *
   * @param rate Rate de IVA (puede venir como decimal 0.15 o como porcentaje 15.0)
   * @return Código de porcentaje según CodigoPorcentajeEnum (ej: "4" para 15%, "2" para 12%)
   */
  private String mapIvaRateToCodigoPorcentaje(BigDecimal rate) {
    if (rate == null) {
      return "0"; // Por defecto 0%
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

    // Mapear según CodigoPorcentajeEnum vigentes en Ecuador (2024-2025)
    // Fuente: https://www.sri.gob.ec/impuesto-al-valor-agregado-iva
    switch (percentage) {
      case 0:
        return "0"; // ✅ IVA 0% - Exento (código SRI: 0)
      case 5:
        return "5"; // ✅ IVA 5% - Materiales de construcción (código SRI: 5, vigente desde
      // 01/04/2024)
      case 13:
        return "10"; // ✅ IVA 13% - General (código SRI: 10)
      case 15:
        return "4"; // ✅ IVA 15% - General (código SRI: 4, vigente desde 01/04/2024)

      // ❌ TARIFAS OBSOLETAS (ya no válidas desde 2024)
      case 12:
        log.warn(
            "⚠️ IVA 12% está OBSOLETO. Usa 15% o 13% según corresponda. Código SRI usado: 2 (puede ser rechazado)");
        return "2"; // IVA 12% - OBSOLETO
      case 14:
        log.warn(
            "⚠️ IVA 14% está OBSOLETO. Usa 15% o 13% según corresponda. Código SRI usado: 3 (puede ser rechazado)");
        return "3"; // IVA 14% - OBSOLETO

      default:
        log.error(
            "❌ IVA {}% NO ES RECONOCIDO. Tarifas válidas: 0%, 5% (construcción), 13%, 15%",
            percentage);
        // Usar el código más cercano como fallback
        if (percentage <= 5) {
          return "0";
        } else if (percentage <= 13) {
          return "10"; // Aproximar a 13%
        } else {
          return "4"; // Aproximar a 15%
        }
    }
  }

  /**
   * Formatea la tarifa eliminando decimales innecesarios.
   *
   * @param rate Rate de impuesto (BigDecimal, puede venir como decimal 0.15 o como porcentaje 15.0)
   * @return String formateado sin decimales innecesarios (ej: "15" en lugar de "15.0000")
   */
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
    return String.valueOf(percentage);
  }

  /**
   * Formatea la base imponible eliminando decimales innecesarios cuando el valor es entero.
   *
   * @param baseImponible Base imponible (BigDecimal)
   * @return String formateado sin decimales innecesarios (ej: "2500" en lugar de "2500.00")
   */
  private String formatBaseImponible(BigDecimal baseImponible) {
    if (baseImponible == null) {
      return "0";
    }
    // Si el valor es entero, retornar sin decimales
    if (baseImponible.scale() == 0 || baseImponible.stripTrailingZeros().scale() <= 0) {
      return baseImponible.toBigInteger().toString();
    }
    // Si tiene decimales significativos, retornar con formato apropiado
    return baseImponible.stripTrailingZeros().toPlainString();
  }

  /**
   * Extrae la dirección del comprador desde los contactos del cliente en el InvoiceDto. Busca el
   * contacto con contactType = "DIR"
   *
   * @param invoiceDto El DTO de la factura que contiene el ClientDto
   * @return La dirección del comprador o null si no se encuentra
   */
  private String extractBuyerAddressFromInvoiceDto(InvoiceDto invoiceDto) {
    if (Objects.isNull(invoiceDto)
        || Objects.isNull(invoiceDto.getClientDto())
        || Objects.isNull(invoiceDto.getClientDto().getContacts())
        || invoiceDto.getClientDto().getContacts().isEmpty()) {
      return null;
    }

    return invoiceDto.getClientDto().getContacts().stream()
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
}
