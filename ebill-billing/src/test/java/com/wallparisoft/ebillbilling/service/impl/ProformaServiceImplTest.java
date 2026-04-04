package com.wallparisoft.ebillbilling.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaCreateRequestDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaDetailRequestDto;
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
import com.wallparisoft.ebillbilling.service.ProductClient;
import com.wallparisoft.ebillbilling.service.ProformaPdfService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProformaServiceImplTest {

  @Mock IProformaRepo proformaRepo;
  @Mock IProformaDetailRepo proformaDetailRepo;
  @Mock CustomerClient customerClient;
  @Mock IInvoiceService invoiceService;
  @Mock ProductClient productClient;
  @Mock ProformaPdfService proformaPdfService;

  ProformaServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ProformaServiceImpl(
            proformaRepo,
            proformaDetailRepo,
            customerClient,
            invoiceService,
            productClient,
            proformaPdfService);
  }

  @Test
  void create_requiresAtLeastOneDetail() {
    ProformaCreateRequestDto request = validCreateRequest();
    request.setDetails(List.of());

    assertThrows(BadRequestException.class, () -> service.create(request));
    verify(proformaRepo, never()).save(any());
  }

  @Test
  void create_success() {
    ProformaCreateRequestDto request = validCreateRequest();
    when(customerClient.getCompanyById(1L))
        .thenReturn(new com.wallparisoft.ebillbilling.dto.CompanyDto());
    when(customerClient.getEstablishmentWithIssuancePoint(1L))
        .thenReturn(EstablishmentDto.builder().idEstablishment(1L).build());
    when(proformaRepo.findNextSequentialForCompany(1L)).thenReturn(1L);
    when(proformaRepo.save(any(Proforma.class)))
        .thenAnswer(
            inv -> {
              Proforma p = inv.getArgument(0);
              p.setIdProforma(100L);
              return p;
            });

    var result = service.create(request);

    assertNotNull(result);
    assertEquals("PRO-000001", result.getProformaNumber());
    assertEquals(Proforma.Status.DRAFT.name(), result.getStatus());
    verify(proformaRepo).save(any(Proforma.class));
  }

  @Test
  void update_onlyDraftAllowed() {
    Proforma proforma = savedProforma(Proforma.Status.SENT);
    when(proformaRepo.findByIdWithDetails(100L)).thenReturn(Optional.of(proforma));

    ProformaUpdateRequestDto request = validUpdateRequest();
    assertThrows(BadRequestException.class, () -> service.update(100L, request));
    verify(proformaRepo, never()).save(any());
  }

  @Test
  void update_success() {
    Proforma proforma = savedProforma(Proforma.Status.DRAFT);
    when(proformaRepo.findByIdWithDetails(100L)).thenReturn(Optional.of(proforma));
    when(customerClient.getCompanyById(1L)).thenReturn(CompanyDto.builder().idCompany(1L).build());
    when(customerClient.getEstablishmentWithIssuancePoint(1L))
        .thenReturn(EstablishmentDto.builder().idEstablishment(1L).build());
    when(proformaRepo.save(any(Proforma.class))).thenAnswer(inv -> inv.getArgument(0));

    ProformaUpdateRequestDto request = validUpdateRequest();
    var result = service.update(100L, request);

    assertNotNull(result);
    verify(proformaRepo).save(any(Proforma.class));
  }

  @Test
  void getById_notFound() {
    when(proformaRepo.findByIdWithDetails(999L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.getById(999L, 1L));
  }

  @Test
  void getById_wrongCompany() {
    Proforma p = savedProforma(Proforma.Status.DRAFT);
    p.setIdCompany(1L);
    when(proformaRepo.findByIdWithDetails(100L)).thenReturn(Optional.of(p));
    assertThrows(NotFoundException.class, () -> service.getById(100L, 2L));
  }

  @Test
  void delete_onlyDraftAllowed() {
    Proforma proforma = savedProforma(Proforma.Status.SENT);
    when(proformaRepo.findById(100L)).thenReturn(Optional.of(proforma));
    assertThrows(BadRequestException.class, () -> service.delete(100L, 1L));
    verify(proformaRepo, never()).delete(any());
  }

  @Test
  void delete_success() {
    Proforma proforma = savedProforma(Proforma.Status.DRAFT);
    when(proformaRepo.findById(100L)).thenReturn(Optional.of(proforma));
    service.delete(100L, 1L);
    verify(proformaRepo).delete(proforma);
  }

  @Test
  void convertToInvoice_alreadyConverted() {
    Proforma proforma = savedProforma(Proforma.Status.CONVERTED);
    proforma.setIdInvoice(50L);
    when(proformaRepo.findByIdWithDetails(100L)).thenReturn(Optional.of(proforma));

    assertThrows(ConflictException.class, () -> service.convertToInvoice(100L, 1L));
    verify(invoiceService, never()).saveInvoiceDraft(any());
  }

  @Test
  void convertToInvoice_success() {
    Proforma proforma = savedProforma(Proforma.Status.DRAFT);
    proforma.setIdEstablishment(1L);
    proforma.setIdIssuancePoint(1L);
    proforma.setIdClient(1L);
    proforma.setIdInvoice(null);
    when(proformaRepo.findByIdWithDetails(100L)).thenReturn(Optional.of(proforma));
    when(proformaRepo.save(any(Proforma.class))).thenAnswer(inv -> inv.getArgument(0));
    when(customerClient.getCompanyById(1L)).thenReturn(CompanyDto.builder().idCompany(1L).build());
    when(customerClient.getEstablishmentWithIssuancePoint(1L))
        .thenReturn(EstablishmentDto.builder().idEstablishment(1L).build());
    when(customerClient.getClientById(1L))
        .thenReturn(
            ClientDtoResponse.builder()
                .clientDtos(List.of(ClientDto.builder().idClient(1L).build()))
                .build());
    InvoiceResponse invoiceResponse = new InvoiceResponse();
    invoiceResponse.setIdInvoice(200L);
    invoiceResponse.setInvoiceNumber("001-001-000001");
    when(invoiceService.saveInvoiceDraft(any())).thenReturn(invoiceResponse);

    var result = service.convertToInvoice(100L, 1L);

    assertNotNull(result);
    assertEquals(200L, result.getIdInvoice());
    assertEquals("001-001-000001", result.getInvoiceNumber());
    verify(invoiceService).saveInvoiceDraft(any());
    verify(proformaRepo).save(any(Proforma.class));
  }

  @Test
  void search_returnsPage() {
    Proforma p = savedProforma(Proforma.Status.DRAFT);
    p.setDetails(List.of()); // empty so service usa IProformaDetailRepo para cargar detalles
    when(proformaRepo.findByFilters(eq(1L), eq(null), eq(null), eq(null), eq(null), any()))
        .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
    ProformaDetail detail = new ProformaDetail();
    detail.setSubtotal(BigDecimal.TEN);
    detail.setIvaPercentage(BigDecimal.ZERO);
    detail.setIcePercentage(BigDecimal.ZERO);
    when(proformaDetailRepo.findByProformaIdProformaOrderByLineOrder(100L))
        .thenReturn(List.of(detail));

    Page<?> page = service.search(1L, null, null, null, null, PageRequest.of(0, 20));

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    verify(proformaDetailRepo).findByProformaIdProformaOrderByLineOrder(100L);
  }

  @Test
  void generatePdf_success() {
    Proforma proforma = savedProforma(Proforma.Status.DRAFT);
    proforma.setIdCompany(1L);
    when(proformaRepo.findByIdWithDetails(100L)).thenReturn(Optional.of(proforma));
    when(proformaPdfService.generatePdf(proforma)).thenReturn(new byte[] {1, 2, 3});

    byte[] result = service.generatePdf(100L, 1L);

    assertNotNull(result);
    assertEquals(3, result.length);
    verify(proformaRepo).findByIdWithDetails(100L);
    verify(proformaPdfService).generatePdf(proforma);
  }

  @Test
  void generatePdf_wrongCompany_throwsNotFound() {
    Proforma proforma = savedProforma(Proforma.Status.DRAFT);
    proforma.setIdCompany(1L);
    when(proformaRepo.findByIdWithDetails(100L)).thenReturn(Optional.of(proforma));

    assertThrows(NotFoundException.class, () -> service.generatePdf(100L, 2L));
    verify(proformaPdfService, never()).generatePdf(any());
  }

  private ProformaCreateRequestDto validCreateRequest() {
    ProformaDetailRequestDto line =
        ProformaDetailRequestDto.builder()
            .idProduct(10L)
            .quantity(BigDecimal.ONE)
            .unitPrice(BigDecimal.valueOf(100))
            .discount(BigDecimal.ZERO)
            .ivaPercentage(BigDecimal.ZERO)
            .icePercentage(BigDecimal.ZERO)
            .lineOrder(1)
            .build();
    return ProformaCreateRequestDto.builder()
        .idCompany(1L)
        .idEstablishment(1L)
        .idIssuancePoint(1L)
        .idClient(1L)
        .issueDate(LocalDate.now())
        .details(List.of(line))
        .build();
  }

  private ProformaUpdateRequestDto validUpdateRequest() {
    ProformaDetailRequestDto line =
        ProformaDetailRequestDto.builder()
            .idProduct(10L)
            .quantity(BigDecimal.valueOf(2))
            .unitPrice(BigDecimal.valueOf(50))
            .discount(BigDecimal.ZERO)
            .ivaPercentage(BigDecimal.ZERO)
            .icePercentage(BigDecimal.ZERO)
            .lineOrder(1)
            .build();
    return ProformaUpdateRequestDto.builder()
        .idCompany(1L)
        .idEstablishment(1L)
        .idIssuancePoint(1L)
        .issueDate(LocalDate.now())
        .details(List.of(line))
        .build();
  }

  private Proforma savedProforma(Proforma.Status status) {
    Proforma p = new Proforma();
    p.setIdProforma(100L);
    p.setIdCompany(1L);
    p.setProformaNumber("PRO-000001");
    p.setStatus(status);
    ProformaDetail d = new ProformaDetail();
    d.setSubtotal(BigDecimal.TEN);
    d.setQuantity(BigDecimal.ONE);
    d.setUnitPrice(BigDecimal.TEN);
    d.setDiscount(BigDecimal.ZERO);
    p.setDetails(new java.util.ArrayList<>(List.of(d)));
    return p;
  }
}
