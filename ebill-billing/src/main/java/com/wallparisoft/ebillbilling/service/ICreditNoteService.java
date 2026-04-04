package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteListResponseDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteQueryDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteSearchResponseDto;
import com.wallparisoft.ebillbilling.entity.CreditNote;
import com.wallparisoft.ebillbilling.response.CreditNoteResponse;
import java.nio.file.Path;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICreditNoteService extends ICRUD<CreditNote> {

  /**
   * Lista todas las notas de crédito con datos del cliente (buyerName, buyerIdentification).
   *
   * @return Lista de CreditNoteListResponseDto
   */
  List<CreditNoteListResponseDto> listWithClientData();

  CreditNoteResponse saveCreditNote(CreditNoteDto creditNoteDto);

  CreditNoteResponse updateCreditNote(Long id, CreditNoteDto creditNoteDto);

  CreditNoteDto getCreditNoteById(Long id);

  /**
   * Obtiene un borrador de nota de crédito por ID. Solo retorna notas de crédito que no han sido
   * autorizadas por el SRI (authorizationSri == null).
   *
   * @param idCreditNote ID de la nota de crédito borrador
   * @return DTO con datos completos de la nota de crédito borrador
   * @throws IllegalArgumentException si la nota de crédito no existe o ya fue autorizada
   */
  CreditNoteDto getDraftCreditNoteById(Long idCreditNote);

  Page<CreditNote> searchCreditNotes(
      String creditNoteNumber, Long idClient, Long originalInvoiceId, Pageable pageable);

  List<CreditNote> getCreditNotesByOriginalInvoice(Long originalInvoiceId);

  void executeSendCreditNoteToSri();

  /**
   * Guarda una nota de crédito como borrador (sin enviar al SRI).
   *
   * @param creditNoteDto DTO con los datos de la nota de crédito
   * @return CreditNoteResponse con el resultado
   */
  CreditNoteResponse saveCreditNoteDraft(CreditNoteDto creditNoteDto);

  /**
   * Envía un borrador de nota de crédito al SRI.
   *
   * @param idCreditNote ID de la nota de crédito
   * @param password Contraseña de la firma digital
   * @return CreditNoteResponse con el resultado
   */
  CreditNoteResponse sendCreditNoteToSri(Long idCreditNote, String password);

  /**
   * Obtiene la ruta del archivo PDF de una nota de crédito.
   *
   * @param idCreditNote ID de la nota de crédito
   * @return Path al archivo PDF
   */
  Path getPdfFilePath(Long idCreditNote);

  /**
   * Obtiene la ruta del archivo XML autorizado de una nota de crédito.
   *
   * @param idCreditNote ID de la nota de crédito
   * @return Path al archivo XML
   */
  Path getXmlFilePath(Long idCreditNote);

  /**
   * Envía la nota de crédito por email al cliente.
   *
   * @param idCreditNote ID de la nota de crédito
   * @param email Email alternativo (opcional, null usa el del cliente)
   * @return true si se envió correctamente
   */
  boolean sendCreditNoteByEmail(Long idCreditNote, String email);

  Page<CreditNoteSearchResponseDto> queryCreditNotes(
      CreditNoteQueryDto queryDto, Pageable pageable);

  /**
   * Busca notas de crédito por idCompany (obligatorio) y filtros opcionales con paginación. Retorna
   * un DTO optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de CreditNoteListResponseDto
   */
  Page<CreditNoteListResponseDto> searchCreditNotesByCompany(
      com.wallparisoft.ebillbilling.dto.CreditNoteSearchByCompanyDto searchDto, Pageable pageable);

  /**
   * Busca notas de crédito autorizadas y activas por número de comprobante o clave de acceso.
   * Requiere idCompany obligatorio y al menos uno de los criterios de búsqueda. Retorna información
   * básica: clave de acceso, número de autorización, fecha de emisión y número de comprobante.
   *
   * @param searchDto DTO con los criterios de búsqueda
   * @return Lista de notas de crédito encontradas con información básica
   * @throws IllegalArgumentException si no se proporciona idCompany o ningún criterio de búsqueda
   */
  List<com.wallparisoft.ebillbilling.dto.CreditNoteBasicInfoDto>
      searchAuthorizedActiveCreditNotesByNumberOrAccessKey(
          com.wallparisoft.ebillbilling.dto.CreditNoteSearchByNumberOrAccessKeyDto searchDto);
}
