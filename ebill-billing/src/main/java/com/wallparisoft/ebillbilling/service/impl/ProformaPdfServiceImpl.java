package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.ContactDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.entity.Proforma;
import com.wallparisoft.ebillbilling.entity.ProformaDetail;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.ProformaPdfService;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProformaPdfServiceImpl implements ProformaPdfService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final CustomerClient customerClient;

  @Override
  public byte[] generatePdf(Proforma proforma) {
    try (InputStream jrxml =
        new ClassPathResource("jasper/proforma_template.jrxml").getInputStream()) {
      JasperReport jasperReport = JasperCompileManager.compileReport(jrxml);
      Map<String, Object> parameters = buildParameters(proforma);
      JRBeanCollectionDataSource dataSource =
          new JRBeanCollectionDataSource(buildDetailRows(proforma));
      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
        return baos.toByteArray();
      }
    } catch (Exception ex) {
      log.error("Error al generar el PDF de la proforma {}", proforma.getIdProforma(), ex);
      throw new IllegalStateException("No se pudo generar el PDF de la proforma", ex);
    }
  }

  private Map<String, Object> buildParameters(Proforma proforma) {
    Map<String, Object> params = new HashMap<>();

    CompanyDto company = null;
    EstablishmentDto establishment = null;
    try {
      company = customerClient.getCompanyById(proforma.getIdCompany());
    } catch (Exception e) {
      log.warn(
          "No se pudo cargar la compañía {} para la proforma {}",
          proforma.getIdCompany(),
          proforma.getIdProforma(),
          e);
    }

    try {
      establishment =
          customerClient.getEstablishmentWithIssuancePoint(proforma.getIdIssuancePoint());
    } catch (Exception e) {
      log.warn(
          "No se pudo cargar el establecimiento/punto de emisión {} para la proforma {}",
          proforma.getIdIssuancePoint(),
          proforma.getIdProforma(),
          e);
    }

    ClientDto client = null;
    if (proforma.getIdClient() != null) {
      try {
        ClientDtoResponse resp = customerClient.getClientById(proforma.getIdClient());
        if (resp != null && !CollectionUtils.isEmpty(resp.getClientDtos())) {
          client = resp.getClientDtos().get(0);
        }
      } catch (Exception e) {
        log.warn(
            "No se pudo cargar el cliente {} para la proforma {}",
            proforma.getIdClient(),
            proforma.getIdProforma(),
            e);
      }
    }

    String logoPath = company != null ? company.getLogoPath() : null;
    params.put("logoUrl", loadLogoUrl(logoPath));

    params.put("companyName", company != null ? company.getName() : "");
    params.put("companyRuc", company != null ? company.getIdentification() : "");
    params.put("companyAddress", company != null ? company.getPrincipal() : "");
    params.put("companyTradeName", company != null ? company.getNameCommercial() : "");

    String headOfficeAddress = null;
    String branchOfficeAddress = null;
    if (establishment != null) {
      if ("SUCURSAL".equalsIgnoreCase(establishment.getType())) {
        headOfficeAddress =
            StringUtils.hasText(establishment.getParentAddress())
                ? establishment.getParentAddress()
                : establishment.getAddress();
        branchOfficeAddress = establishment.getAddress();
      } else {
        headOfficeAddress = establishment.getAddress();
        branchOfficeAddress = establishment.getAddress();
      }
    }
    if (!StringUtils.hasText(headOfficeAddress) && company != null) {
      headOfficeAddress = company.getPrincipal();
    }
    if (!StringUtils.hasText(branchOfficeAddress)) {
      branchOfficeAddress = headOfficeAddress;
    }

    params.put("companyHeadOfficeAddress", headOfficeAddress != null ? headOfficeAddress : "");
    params.put(
        "companyBranchOfficeAddress", branchOfficeAddress != null ? branchOfficeAddress : "");

    params.put("proformaNumber", proforma.getProformaNumber());
    params.put(
        "issueDate",
        proforma.getIssueDate() != null ? DATE_FORMAT.format(proforma.getIssueDate()) : "");
    params.put(
        "validUntil",
        proforma.getValidUntil() != null ? DATE_FORMAT.format(proforma.getValidUntil()) : "");

    params.put("clientName", client != null ? client.getName() : "");
    params.put("clientIdentification", client != null ? client.getIdentification() : "");
    params.put("clientAddress", buildClientAddress(client));

    params.put("additionalInformation", Objects.toString(proforma.getAdditionalInformation(), ""));

    Totals totals = calculateTotals(proforma);
    params.put("subtotalZero", formatCurrency(totals.subtotalZero));
    params.put("subtotalIva", formatCurrency(totals.subtotalIva));
    params.put("subtotalIce", formatCurrency(totals.subtotalIce));
    params.put("totalDiscount", formatCurrency(totals.totalDiscount));
    params.put("totalAmount", formatCurrency(totals.totalAmount));

    return params;
  }

  private List<DetailRow> buildDetailRows(Proforma proforma) {
    return proforma.getDetails().stream()
        .sorted((a, b) -> Integer.compare(a.getLineOrder(), b.getLineOrder()))
        .map(
            d ->
                new DetailRow(
                    d.getProductCode(),
                    d.getDescription(),
                    formatQuantity(d.getQuantity()),
                    formatCurrency(d.getUnitPrice()),
                    formatPercentage(d.getDiscount()),
                    formatCurrency(d.getSubtotal())))
        .toList();
  }

  private String formatCurrency(BigDecimal value) {
    BigDecimal safe = value != null ? value : BigDecimal.ZERO;
    return String.format(Locale.US, "%,.2f", safe);
  }

  private String formatQuantity(BigDecimal value) {
    if (value == null) {
      return "0";
    }
    BigDecimal normalized = value.stripTrailingZeros();
    return normalized.toPlainString();
  }

  private String formatPercentage(BigDecimal value) {
    if (value == null) {
      return "0";
    }
    BigDecimal normalized = value.stripTrailingZeros();
    return normalized.toPlainString();
  }

  private Totals calculateTotals(Proforma proforma) {
    BigDecimal totalAmount = BigDecimal.ZERO;
    BigDecimal totalDiscount = BigDecimal.ZERO;
    BigDecimal subtotalZero = BigDecimal.ZERO;
    BigDecimal subtotalIva = BigDecimal.ZERO;
    BigDecimal subtotalIce = BigDecimal.ZERO;

    if (proforma.getDetails() != null) {
      for (ProformaDetail d : proforma.getDetails()) {
        totalAmount = totalAmount.add(d.getSubtotal());
        totalDiscount =
            totalDiscount.add(d.getDiscount() != null ? d.getDiscount() : BigDecimal.ZERO);

        if (d.getIvaPercentage() != null && d.getIvaPercentage().compareTo(BigDecimal.ZERO) > 0) {
          subtotalIva = subtotalIva.add(d.getSubtotal());
        } else if (d.getIcePercentage() != null
            && d.getIcePercentage().compareTo(BigDecimal.ZERO) > 0) {
          subtotalIce = subtotalIce.add(d.getSubtotal());
        } else {
          subtotalZero = subtotalZero.add(d.getSubtotal());
        }
      }
    }

    return new Totals(totalAmount, totalDiscount, subtotalZero, subtotalIva, subtotalIce);
  }

  private URL loadLogoUrl(String logoPath) {
    try {
      if (StringUtils.hasText(logoPath)) {
        try {
          return new URL(logoPath);
        } catch (Exception e) {
          try {
            Path logoFilePath = Path.of(logoPath);
            if (Files.exists(logoFilePath)) {
              return logoFilePath.toUri().toURL();
            }
          } catch (Exception ex) {
            log.warn("No se pudo cargar el logo desde la ruta: {}", logoPath, ex);
          }
        }
      }
      return new ClassPathResource("jasper/no_tiene_logo.png").getURL();
    } catch (Exception e) {
      log.error("Error crítico: No se pudo cargar el logo por defecto", e);
      try {
        return getClass().getClassLoader().getResource("jasper/no_tiene_logo.png").toURI().toURL();
      } catch (Exception ex) {
        log.error("Error fatal: No se puede cargar ningún logo", ex);
        throw new RuntimeException("No se pudo cargar el logo por defecto", ex);
      }
    }
  }

  private String buildClientAddress(ClientDto client) {
    if (client == null || CollectionUtils.isEmpty(client.getContacts())) {
      return "";
    }

    return client.getContacts().stream()
        .filter(
            c -> {
              if (c == null || c.getContactType() == null || Boolean.FALSE.equals(c.getStatus())) {
                return false;
              }
              String type = c.getContactType().toUpperCase();
              // Aceptar más variantes para dirección (similar a factura)
              return type.contains("DIR")
                  || type.contains("ADDRESS")
                  || "DIRECCION".equals(type)
                  || "DIRECCIÓN".equals(type);
            })
        .map(ContactDto::getValue)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("");
  }

  public static class DetailRow {
    private final String productCode;
    private final String description;
    private final String quantity;
    private final String unitPrice;
    private final String discount;
    private final String subtotal;

    DetailRow(
        String productCode,
        String description,
        String quantity,
        String unitPrice,
        String discount,
        String subtotal) {
      this.productCode = productCode;
      this.description = description;
      this.quantity = quantity;
      this.unitPrice = unitPrice;
      this.discount = discount;
      this.subtotal = subtotal;
    }

    public String getProductCode() {
      return productCode;
    }

    public String getDescription() {
      return description;
    }

    public String getQuantity() {
      return quantity;
    }

    public String getUnitPrice() {
      return unitPrice;
    }

    public String getDiscount() {
      return discount;
    }

    public String getSubtotal() {
      return subtotal;
    }
  }

  private static class Totals {
    private final BigDecimal totalAmount;
    private final BigDecimal totalDiscount;
    private final BigDecimal subtotalZero;
    private final BigDecimal subtotalIva;
    private final BigDecimal subtotalIce;

    Totals(
        BigDecimal totalAmount,
        BigDecimal totalDiscount,
        BigDecimal subtotalZero,
        BigDecimal subtotalIva,
        BigDecimal subtotalIce) {
      this.totalAmount = totalAmount;
      this.totalDiscount = totalDiscount;
      this.subtotalZero = subtotalZero;
      this.subtotalIva = subtotalIva;
      this.subtotalIce = subtotalIce;
    }
  }
}
