package com.wallparisoft.ebillbilling.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.entity.Proforma;
import com.wallparisoft.ebillbilling.entity.ProformaDetail;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProformaPdfServiceImplTest {

  @Mock CustomerClient customerClient;

  @InjectMocks ProformaPdfServiceImpl proformaPdfService;

  @Test
  void shouldGeneratePdfForValidProforma() {
    Proforma proforma = buildProforma();

    CompanyDto companyDto =
        CompanyDto.builder()
            .idCompany(1L)
            .name("Empresa XYZ")
            .identification("0999999999")
            .principal("Av. Siempre Viva 123")
            .build();
    when(customerClient.getCompanyById(1L)).thenReturn(companyDto);

    ClientDto clientDto =
        ClientDto.builder().idClient(1L).name("Juan Perez").identification("0102030405").build();
    ClientDtoResponse clientResp =
        ClientDtoResponse.builder().clientDtos(List.of(clientDto)).build();
    when(customerClient.getClientById(1L)).thenReturn(clientResp);

    byte[] pdf = proformaPdfService.generatePdf(proforma);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0, "El PDF generado no puede estar vacío");
  }

  private Proforma buildProforma() {
    Proforma p = new Proforma();
    p.setIdProforma(8L);
    p.setIdCompany(1L);
    p.setIdClient(1L);
    p.setProformaNumber("PRO-000008");
    p.setIssueDate(LocalDate.now());
    p.setValidUntil(LocalDate.now().plusDays(5));
    p.setStatus(Proforma.Status.DRAFT);

    ProformaDetail d = new ProformaDetail();
    d.setProductCode("P-001");
    d.setDescription("Producto de prueba");
    d.setQuantity(BigDecimal.ONE);
    d.setUnitPrice(BigDecimal.valueOf(100));
    d.setDiscount(BigDecimal.ZERO);
    d.setSubtotal(BigDecimal.valueOf(100));
    d.setLineOrder(1);

    p.setDetails(new java.util.ArrayList<>(List.of(d)));
    return p;
  }
}
