package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.proforma.ProformaConvertToInvoiceResponseDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaCreateRequestDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaListResponseDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProformaService {

  ProformaDto create(ProformaCreateRequestDto request);

  ProformaDto update(Long idProforma, ProformaUpdateRequestDto request);

  ProformaDto getById(Long idProforma, Long idCompany);

  Page<ProformaListResponseDto> search(
      Long idCompany,
      java.time.LocalDate issueDateFrom,
      java.time.LocalDate issueDateTo,
      String status,
      String identification,
      Pageable pageable);

  void delete(Long idProforma, Long idCompany);

  ProformaConvertToInvoiceResponseDto convertToInvoice(Long idProforma, Long idCompany);

  /**
   * Genera el PDF de una proforma específica para la compañía indicada.
   *
   * @param idProforma identificador de la proforma
   * @param idCompany identificador de la compañía propietaria
   * @return contenido del PDF en un arreglo de bytes
   */
  byte[] generatePdf(Long idProforma, Long idCompany);
}
