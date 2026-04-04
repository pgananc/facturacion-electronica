package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteListResponseDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteQueryDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteSearchResponseDto;
import com.wallparisoft.ebillbilling.entity.DebitNote;
import com.wallparisoft.ebillbilling.response.DebitNoteResponse;
import java.nio.file.Path;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IDebitNoteService extends ICRUD<DebitNote> {

  /**
   * Lista todas las notas de débito con datos del cliente (buyerName, buyerIdentification).
   *
   * @return Lista de DebitNoteListResponseDto
   */
  List<DebitNoteListResponseDto> listWithClientData();

  DebitNoteResponse saveDebitNote(DebitNoteDto debitNoteDto);

  DebitNoteResponse updateDebitNote(Long id, DebitNoteDto debitNoteDto);

  DebitNoteDto getDebitNoteById(Long id);

  /**
   * Obtiene un borrador de nota de débito por ID. Solo retorna notas de débito que no han sido
   * autorizadas por el SRI (authorizationSri == null).
   *
   * @param idDebitNote ID de la nota de débito borrador
   * @return DTO con datos completos de la nota de débito borrador
   * @throws IllegalArgumentException si la nota de débito no existe o ya fue autorizada
   */
  DebitNoteDto getDraftDebitNoteById(Long idDebitNote);

  Page<DebitNote> searchDebitNotes(
      String debitNoteNumber, Long idClient, Long originalInvoiceId, Pageable pageable);

  List<DebitNote> getDebitNotesByOriginalInvoice(Long originalInvoiceId);

  void executeSendDebitNoteToSri();

  /**
   * Guarda una nota de débito como borrador (sin enviar al SRI).
   *
   * @param debitNoteDto DTO con los datos de la nota de débito
   * @return DebitNoteResponse con el resultado
   */
  DebitNoteResponse saveDebitNoteDraft(DebitNoteDto debitNoteDto);

  /**
   * Envía un borrador de nota de débito al SRI.
   *
   * @param idDebitNote ID de la nota de débito
   * @param password Contraseña de la firma digital
   * @return DebitNoteResponse con el resultado
   */
  DebitNoteResponse sendDebitNoteToSri(Long idDebitNote, String password);

  /**
   * Obtiene la ruta del archivo PDF de una nota de débito.
   *
   * @param idDebitNote ID de la nota de débito
   * @return Path al archivo PDF
   */
  Path getPdfFilePath(Long idDebitNote);

  /**
   * Obtiene la ruta del archivo XML autorizado de una nota de débito.
   *
   * @param idDebitNote ID de la nota de débito
   * @return Path al archivo XML
   */
  Path getXmlFilePath(Long idDebitNote);

  /**
   * Envía la nota de débito por email al cliente.
   *
   * @param idDebitNote ID de la nota de débito
   * @param email Email alternativo (opcional, null usa el del cliente)
   * @return true si se envió correctamente
   */
  boolean sendDebitNoteByEmail(Long idDebitNote, String email);

  Page<DebitNoteSearchResponseDto> queryDebitNotes(DebitNoteQueryDto queryDto, Pageable pageable);

  /**
   * Busca notas de débito por idCompany (obligatorio) y filtros opcionales con paginación. Retorna
   * un DTO optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de DebitNoteListResponseDto
   */
  Page<DebitNoteListResponseDto> searchDebitNotesByCompany(
      com.wallparisoft.ebillbilling.dto.DebitNoteSearchByCompanyDto searchDto, Pageable pageable);

  /**
   * Busca notas de débito autorizadas y activas por número de comprobante o clave de acceso.
   * Requiere idCompany obligatorio y al menos uno de los criterios de búsqueda. Retorna información
   * básica: clave de acceso, número de autorización, fecha de emisión y número de comprobante.
   *
   * @param searchDto DTO con los criterios de búsqueda
   * @return Lista de notas de débito encontradas con información básica
   * @throws IllegalArgumentException si no se proporciona idCompany o ningún criterio de búsqueda
   */
  List<com.wallparisoft.ebillbilling.dto.DebitNoteBasicInfoDto>
      searchAuthorizedActiveDebitNotesByNumberOrAccessKey(
          com.wallparisoft.ebillbilling.dto.DebitNoteSearchByNumberOrAccessKeyDto searchDto);
}
