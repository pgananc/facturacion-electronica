package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaConvertToInvoiceResponseDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaCreateRequestDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaDetailDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaDetailRequestDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaListResponseDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaUpdateRequestDto;
import com.wallparisoft.ebillbilling.entity.Proforma;
import com.wallparisoft.ebillbilling.entity.ProformaDetail;
import com.wallparisoft.ebillbilling.exception.BadRequestException;
import com.wallparisoft.ebillbilling.exception.ConflictException;
import com.wallparisoft.ebillbilling.exception.NotFoundException;
import com.wallparisoft.ebillbilling.repository.IProformaDetailRepo;
import com.wallparisoft.ebillbilling.repository.IProformaRepo;
import com.wallparisoft.ebillbilling.response.InvoiceResponse;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.IInvoiceService;
import com.wallparisoft.ebillbilling.service.IProformaService;
import com.wallparisoft.ebillbilling.service.ProductClient;
import com.wallparisoft.ebillbilling.service.ProformaPdfService;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProformaServiceImpl implements IProformaService {

  private final IProformaRepo proformaRepo;
  private final IProformaDetailRepo proformaDetailRepo;
  private final CustomerClient customerClient;
  private final IInvoiceService invoiceService;
  private final ProductClient productClient;
  private final ProformaPdfService proformaPdfService;

  private static final int PROFORMA_NUMBER_PAD = 6;

  @Override
  @Transactional
  public ProformaDto create(ProformaCreateRequestDto request) {
    validateCompanyEstablishmentIssuancePoint(
        request.getIdCompany(), request.getIdEstablishment(), request.getIdIssuancePoint());
    if (request.getDetails() == null || request.getDetails().isEmpty()) {
      throw new BadRequestException("At least one detail line is required");
    }
    validateDetails(request.getDetails());
    if (request.getValidUntil() != null
        && request.getIssueDate() != null
        && request.getValidUntil().isBefore(request.getIssueDate())) {
      throw new BadRequestException("validUntil must be >= issueDate");
    }

    String proformaNumber = generateProformaNumber(request.getIdCompany());
    LocalDateTime now = LocalDateTime.now();

    Proforma proforma =
        Proforma.builder()
            .idCompany(request.getIdCompany())
            .idEstablishment(request.getIdEstablishment())
            .idIssuancePoint(request.getIdIssuancePoint())
            .idClient(request.getIdClient())
            .proformaNumber(proformaNumber)
            .issueDate(request.getIssueDate())
            .validUntil(request.getValidUntil())
            .status(Proforma.Status.DRAFT)
            .additionalInformation(request.getAdditionalInformation())
            .createdAt(now)
            .updatedAt(now)
            .build();

    List<ProformaDetail> details = buildDetails(proforma, request.getDetails());
    proforma.getDetails().addAll(details);

    proforma = proformaRepo.save(proforma);
    log.info(
        "Proforma created: id={}, number={}",
        proforma.getIdProforma(),
        proforma.getProformaNumber());
    return toDto(proforma);
  }

  @Override
  @Transactional
  public ProformaDto update(Long idProforma, ProformaUpdateRequestDto request) {
    Proforma proforma =
        proformaRepo
            .findByIdWithDetails(idProforma)
            .orElseThrow(() -> new NotFoundException("Proforma not found: " + idProforma));
    if (proforma.getStatus() != Proforma.Status.DRAFT) {
      throw new BadRequestException("Only proformas with status DRAFT can be updated");
    }
    if (!Objects.equals(proforma.getIdCompany(), request.getIdCompany())) {
      throw new BadRequestException("idCompany does not match proforma");
    }
    validateCompanyEstablishmentIssuancePoint(
        request.getIdCompany(), request.getIdEstablishment(), request.getIdIssuancePoint());
    if (request.getDetails() == null || request.getDetails().isEmpty()) {
      throw new BadRequestException("At least one detail line is required");
    }
    validateDetails(request.getDetails());
    if (request.getValidUntil() != null
        && request.getIssueDate() != null
        && request.getValidUntil().isBefore(request.getIssueDate())) {
      throw new BadRequestException("validUntil must be >= issueDate");
    }

    proforma.setIdEstablishment(request.getIdEstablishment());
    proforma.setIdIssuancePoint(request.getIdIssuancePoint());
    proforma.setIdClient(request.getIdClient());
    proforma.setIssueDate(request.getIssueDate());
    proforma.setValidUntil(request.getValidUntil());
    proforma.setAdditionalInformation(request.getAdditionalInformation());
    proforma.setUpdatedAt(LocalDateTime.now());

    proforma.getDetails().clear();
    proforma.getDetails().addAll(buildDetails(proforma, request.getDetails()));

    proforma = proformaRepo.save(proforma);
    log.info("Proforma updated: id={}", idProforma);
    return toDto(proforma);
  }

  @Override
  public ProformaDto getById(Long idProforma, Long idCompany) {
    Proforma proforma =
        proformaRepo
            .findByIdWithDetails(idProforma)
            .orElseThrow(() -> new NotFoundException("Proforma not found: " + idProforma));
    if (!Objects.equals(proforma.getIdCompany(), idCompany)) {
      throw new NotFoundException("Proforma not found: " + idProforma);
    }
    return toDto(proforma);
  }

  @Override
  public Page<ProformaListResponseDto> search(
      Long idCompany,
      LocalDate issueDateFrom,
      LocalDate issueDateTo,
      String status,
      String identification,
      Pageable pageable) {
    Proforma.Status statusEnum = null;
    if (status != null && !status.isBlank()) {
      try {
        statusEnum = Proforma.Status.valueOf(status.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid status: " + status);
      }
    }
    // Buscar cliente por nombre o identificación si se proporciona
    Long idClient = null;
    if (StringUtils.hasText(identification)) {
      try {
        ClientDtoResponse clientResponse = null;
        if (StringUtils.hasText(identification)) {
          clientResponse = customerClient.getClientByIdentification(identification);
        }
        if (clientResponse != null
            && !CollectionUtils.isEmpty(clientResponse.getClientDtos())
            && Objects.nonNull(clientResponse.getClientDtos().get(0))) {
          idClient = clientResponse.getClientDtos().get(0).getIdClient();
        } else {
          idClient = 0l;
        }
      } catch (Exception e) {
        log.warn("Error buscando cliente por identificación: {}", e.getMessage());
      }
    }
    Page<Proforma> page =
        proformaRepo.findByFilters(
            idCompany, issueDateFrom, issueDateTo, statusEnum, idClient, pageable);
    return page.map(this::toListDto);
  }

  @Override
  @Transactional
  public void delete(Long idProforma, Long idCompany) {
    Proforma proforma =
        proformaRepo
            .findById(idProforma)
            .orElseThrow(() -> new NotFoundException("Proforma not found: " + idProforma));
    if (!Objects.equals(proforma.getIdCompany(), idCompany)) {
      throw new NotFoundException("Proforma not found: " + idProforma);
    }
    if (proforma.getStatus() != Proforma.Status.DRAFT) {
      throw new BadRequestException("Only proformas with status DRAFT can be deleted");
    }
    proformaRepo.delete(proforma);
    log.info("Proforma deleted: id={}", idProforma);
  }

  @Override
  @Transactional
  public ProformaConvertToInvoiceResponseDto convertToInvoice(Long idProforma, Long idCompany) {
    Proforma proforma =
        proformaRepo
            .findByIdWithDetails(idProforma)
            .orElseThrow(() -> new NotFoundException("Proforma not found: " + idProforma));
    if (!Objects.equals(proforma.getIdCompany(), idCompany)) {
      throw new NotFoundException("Proforma not found: " + idProforma);
    }
    if (proforma.getIdInvoice() != null) {
      throw new ConflictException("Proforma already converted to invoice");
    }
    if (proforma.getStatus() != Proforma.Status.DRAFT
        && proforma.getStatus() != Proforma.Status.SENT) {
      throw new BadRequestException("Only proformas with status DRAFT or SENT can be converted");
    }

    InvoiceDto invoiceDto = buildInvoiceDtoFromProforma(proforma);
    InvoiceResponse response = invoiceService.saveInvoiceDraft(invoiceDto);
    Long idInvoice = response.getIdInvoice();
    String invoiceNumber = response.getInvoiceNumber();

    proforma.setStatus(Proforma.Status.CONVERTED);
    proforma.setIdInvoice(idInvoice);
    proforma.setUpdatedAt(LocalDateTime.now());
    proformaRepo.save(proforma);

    log.info("Proforma {} converted to invoice {}", idProforma, idInvoice);
    return ProformaConvertToInvoiceResponseDto.builder()
        .idInvoice(idInvoice)
        .invoiceNumber(invoiceNumber)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] generatePdf(Long idProforma, Long idCompany) {
    Proforma proforma =
        proformaRepo
            .findByIdWithDetails(idProforma)
            .orElseThrow(() -> new NotFoundException("Proforma not found: " + idProforma));
    if (!Objects.equals(proforma.getIdCompany(), idCompany)) {
      throw new NotFoundException("Proforma not found: " + idProforma);
    }
    return proformaPdfService.generatePdf(proforma);
  }

  private void validateCompanyEstablishmentIssuancePoint(
      Long idCompany, Long idEstablishment, Long idIssuancePoint) {
    try {
      customerClient.getCompanyById(idCompany);
    } catch (FeignException.NotFound e) {
      throw new BadRequestException("Company not found: " + idCompany);
    } catch (FeignException e) {
      log.warn("Error validating company {}: {}", idCompany, e.getMessage());
      throw new BadRequestException("Company not found or unavailable");
    }
    try {
      EstablishmentDto est = customerClient.getEstablishmentWithIssuancePoint(idIssuancePoint);
      if (est == null || !Objects.equals(est.getIdEstablishment(), idEstablishment)) {
        throw new BadRequestException("Establishment or issuance point not found");
      }
    } catch (FeignException e) {
      log.warn("Error validating establishment/issuance point: {}", e.getMessage());
      throw new BadRequestException("Establishment or issuance point not found or unavailable");
    }
  }

  private void validateDetails(List<ProformaDetailRequestDto> details) {
    for (ProformaDetailRequestDto d : details) {
      if (d.getQuantity() == null || d.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException("Each line must have quantity > 0");
      }
      if (d.getUnitPrice() == null || d.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Each line must have unitPrice >= 0");
      }
      if (d.getDiscount() == null || d.getDiscount().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Each line must have discount >= 0");
      }
      if (d.getLineOrder() == null) {
        throw new BadRequestException("Each line must have lineOrder");
      }
    }
  }

  private String generateProformaNumber(Long idCompany) {
    Long next = proformaRepo.findNextSequentialForCompany(idCompany);
    return "PRO-" + String.format("%0" + PROFORMA_NUMBER_PAD + "d", next);
  }

  private List<ProformaDetail> buildDetails(
      Proforma proforma, List<ProformaDetailRequestDto> requestDetails) {
    List<ProformaDetail> list = new ArrayList<>();
    for (ProformaDetailRequestDto d : requestDetails) {
      BigDecimal totalBruto = d.getQuantity().multiply(d.getUnitPrice());
      BigDecimal porcentajeDescuento = d.getDiscount() != null ? d.getDiscount() : BigDecimal.ZERO;

      // Fórmula: Total - (Total * (Porcentaje / 100))
      BigDecimal valorDescuento =
          totalBruto
              .multiply(porcentajeDescuento)
              .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
      BigDecimal subtotal = totalBruto.subtract(valorDescuento);
      subtotal = subtotal.setScale(4, RoundingMode.HALF_UP);
      ProformaDetail detail =
          ProformaDetail.builder()
              .proforma(proforma)
              .idProduct(d.getIdProduct())
              .productCode(d.getProductCode())
              .description(d.getDescription())
              .quantity(d.getQuantity())
              .unitPrice(d.getUnitPrice())
              .discount(d.getDiscount() != null ? d.getDiscount() : BigDecimal.ZERO)
              .ivaPercentage(d.getIvaPercentage() != null ? d.getIvaPercentage() : BigDecimal.ZERO)
              .icePercentage(d.getIcePercentage() != null ? d.getIcePercentage() : BigDecimal.ZERO)
              .subtotal(subtotal)
              .lineOrder(d.getLineOrder())
              .build();
      list.add(detail);
    }
    return list;
  }

  private ProformaDto toDto(Proforma p) {
    List<ProformaDetailDto> detailDtos =
        p.getDetails() == null
            ? Collections.emptyList()
            : p.getDetails().stream()
                .sorted((a, b) -> Integer.compare(a.getLineOrder(), b.getLineOrder()))
                .map(this::toDetailDto)
                .toList();

    BigDecimal totalAmount = BigDecimal.ZERO;
    BigDecimal totalDiscount = BigDecimal.ZERO;
    BigDecimal subtotalZero = BigDecimal.ZERO;
    BigDecimal subtotalIva = BigDecimal.ZERO;
    BigDecimal subtotalIce = BigDecimal.ZERO;
    if (p.getDetails() != null) {
      for (ProformaDetail d : p.getDetails()) {
        totalAmount = totalAmount.add(d.getSubtotal());
        totalDiscount = totalDiscount.add(d.getDiscount());
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

    ClientDto clientDto = null;
    if (p.getIdClient() != null) {
      try {
        ClientDtoResponse resp = customerClient.getClientById(p.getIdClient());
        if (resp != null && !CollectionUtils.isEmpty(resp.getClientDtos())) {
          clientDto = resp.getClientDtos().get(0);
        }
      } catch (Exception e) {
        log.debug("Could not load client {} for proforma: {}", p.getIdClient(), e.getMessage());
      }
    }

    return ProformaDto.builder()
        .idProforma(p.getIdProforma())
        .idCompany(p.getIdCompany())
        .idEstablishment(p.getIdEstablishment())
        .idIssuancePoint(p.getIdIssuancePoint())
        .idClient(p.getIdClient())
        .proformaNumber(p.getProformaNumber())
        .issueDate(p.getIssueDate())
        .validUntil(p.getValidUntil())
        .status(p.getStatus() != null ? p.getStatus().name() : null)
        .idInvoice(p.getIdInvoice())
        .additionalInformation(p.getAdditionalInformation())
        .createdAt(p.getCreatedAt())
        .updatedAt(p.getUpdatedAt())
        .createdBy(p.getCreatedBy())
        .clientDto(clientDto)
        .details(detailDtos)
        .subtotalZero(subtotalZero)
        .subtotalIva(subtotalIva)
        .subtotalIce(subtotalIce)
        .totalDiscount(totalDiscount)
        .totalAmount(totalAmount)
        .build();
  }

  private ProformaDetailDto toDetailDto(ProformaDetail d) {
    return ProformaDetailDto.builder()
        .idProformaDetail(d.getIdProformaDetail())
        .idProforma(d.getProforma() != null ? d.getProforma().getIdProforma() : null)
        .idProduct(d.getIdProduct())
        .productCode(d.getProductCode())
        .description(d.getDescription())
        .quantity(d.getQuantity())
        .unitPrice(d.getUnitPrice())
        .discount(d.getDiscount())
        .ivaPercentage(d.getIvaPercentage())
        .icePercentage(d.getIcePercentage())
        .subtotal(d.getSubtotal())
        .lineOrder(d.getLineOrder())
        .build();
  }

  private ProformaListResponseDto toListDto(Proforma p) {
    // Cargar los detalles (si no vienen cargados) para calcular el "valor a pagar"
    List<ProformaDetail> details =
        (p.getDetails() != null && !p.getDetails().isEmpty())
            ? p.getDetails()
            : proformaDetailRepo.findByProformaIdProformaOrderByLineOrder(p.getIdProforma());

    BigDecimal totalAmount = BigDecimal.ZERO;
    if (details != null) {
      for (ProformaDetail d : details) {
        if (d == null) {
          continue;
        }
        BigDecimal base =
            d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO; // valor sin impuestos
        BigDecimal iva =
            d.getIvaPercentage() != null && d.getIvaPercentage().compareTo(BigDecimal.ZERO) > 0
                ? base.multiply(d.getIvaPercentage()).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        BigDecimal ice =
            d.getIcePercentage() != null && d.getIcePercentage().compareTo(BigDecimal.ZERO) > 0
                ? base.multiply(d.getIcePercentage()).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        totalAmount = totalAmount.add(base).add(iva).add(ice);
      }
    }
    String clientIdentification = null;
    String clientName = null;
    if (p.getIdClient() != null) {
      try {
        ClientDtoResponse resp = customerClient.getClientById(p.getIdClient());
        if (resp != null && !CollectionUtils.isEmpty(resp.getClientDtos())) {
          ClientDto c = resp.getClientDtos().get(0);
          clientIdentification = c.getIdentification();
          clientName = c.getName();
        }
      } catch (Exception e) {
        log.debug("Could not load client for list: {}", e.getMessage());
      }
    }
    return ProformaListResponseDto.builder()
        .idProforma(p.getIdProforma())
        .proformaNumber(p.getProformaNumber())
        .issueDate(p.getIssueDate())
        .validUntil(p.getValidUntil())
        .status(p.getStatus() != null ? p.getStatus().name() : null)
        .idInvoice(p.getIdInvoice())
        .totalAmount(totalAmount)
        .idClient(p.getIdClient())
        .clientIdentification(clientIdentification)
        .clientName(clientName)
        .build();
  }

  private InvoiceDto buildInvoiceDtoFromProforma(Proforma proforma) {
    CompanyDto companyDto;
    try {
      companyDto = customerClient.getCompanyById(proforma.getIdCompany());
    } catch (Exception e) {
      throw new BadRequestException("Company not found: " + proforma.getIdCompany());
    }
    EstablishmentDto establishmentDto;
    try {
      establishmentDto =
          customerClient.getEstablishmentWithIssuancePoint(proforma.getIdIssuancePoint());
    } catch (Exception e) {
      throw new BadRequestException("Establishment/issuance point not found");
    }
    ClientDto clientDto = null;
    if (proforma.getIdClient() != null) {
      try {
        ClientDtoResponse resp = customerClient.getClientById(proforma.getIdClient());
        if (resp != null && !CollectionUtils.isEmpty(resp.getClientDtos())) {
          clientDto = resp.getClientDtos().get(0);
        }
      } catch (Exception e) {
        log.warn("Client not found for proforma conversion: {}", proforma.getIdClient());
      }
    }
    if (clientDto == null) {
      throw new BadRequestException("Client is required for conversion; proforma client not found");
    }

    List<InvoiceDetailDto> invoiceDetails = new ArrayList<>();
    for (ProformaDetail d : proforma.getDetails()) {
      ProductDto productDto = ProductDto.builder().idProduct(d.getIdProduct()).build();
      try {
        var productResponse = productClient.getProductById(d.getIdProduct());
        if (productResponse != null && productResponse.getProductDto() != null) {
          var map = productResponse.getProductDto();
          if (map.get("idProduct") != null) {
            productDto.setIdProduct(((Number) map.get("idProduct")).longValue());
          }
          if (map.get("mainCode") != null) productDto.setMainCode((String) map.get("mainCode"));
          if (map.get("name") != null) productDto.setName((String) map.get("name"));
        }
      } catch (Exception e) {
        log.warn("Product {} not found, using minimal ProductDto", d.getIdProduct());
      }
      BigDecimal totalBruto = d.getQuantity().multiply(d.getUnitPrice());
      BigDecimal porcentajeDescuento = d.getDiscount() != null ? d.getDiscount() : BigDecimal.ZERO;
      // Fórmula: Total - (Total * (Porcentaje / 100))
      BigDecimal valorDescuento =
          totalBruto
              .multiply(porcentajeDescuento)
              .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
      BigDecimal subtotal = totalBruto.subtract(valorDescuento);

      InvoiceDetailDto invDetail =
          InvoiceDetailDto.builder()
              .idProduct(d.getIdProduct())
              .productDto(productDto)
              .quantity(d.getQuantity().intValue())
              .unitPrice(d.getUnitPrice())
              .discount(d.getDiscount())
              .subtotal(subtotal)
              .build();
      invoiceDetails.add(invDetail);
    }

    LocalDate today = LocalDate.now();
    BigDecimal totalAmount =
        proforma.getDetails().stream()
            .map(ProformaDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalDiscount =
        proforma.getDetails().stream()
            .map(ProformaDetail::getDiscount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    ProductDto defaultProductDto =
        invoiceDetails.isEmpty()
            ? ProductDto.builder().idProduct(0L).build()
            : invoiceDetails.get(0).getProductDto();

    return InvoiceDto.builder()
        .idCompany(proforma.getIdCompany())
        .idClient(proforma.getIdClient())
        .idIssuancePoint(proforma.getIdIssuancePoint())
        .companyDto(companyDto)
        .clientDto(clientDto)
        .productDto(defaultProductDto)
        .establishment(establishmentDto)
        .issueDate(today)
        .additionalInformation(proforma.getAdditionalInformation())
        .invoiceDetails(invoiceDetails)
        .details(invoiceDetails)
        .totalAmount(totalAmount)
        .discount(totalDiscount)
        .subtotalZero(BigDecimal.ZERO)
        .subtotalIva(totalAmount)
        .subtotalIce(BigDecimal.ZERO)
        .paymentMethods(Collections.emptyList())
        .build();
  }
}
