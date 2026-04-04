package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.DocumentQueryDto;
import com.wallparisoft.ebillbilling.dto.DocumentSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceAvailableForCreditNoteDto;
import com.wallparisoft.ebillbilling.dto.InvoiceBasicInfoDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDraftResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.dto.InvoiceListResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceQueryDto;
import com.wallparisoft.ebillbilling.dto.InvoiceSearchByNumberOrAccessKeyDto;
import com.wallparisoft.ebillbilling.dto.InvoiceSearchDto;
import com.wallparisoft.ebillbilling.dto.InvoiceSearchResponseDto;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.response.InvoiceResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IInvoiceService extends ICRUD<Invoice> {

  /**
   * Lista todas las facturas con los datos del cliente (name, identification).
   *
   * @return Lista de InvoiceListResponseDto con clientName y clientIdentification
   */
  List<InvoiceListResponseDto> listWithClientData();

  InvoiceResponse saveInvoice(InvoiceDto purchaseProofDto);

  void executeSendInvoiceToSri();

  Page<InvoiceSearchResponseDto> searchInvoices(InvoiceSearchDto searchDto, Pageable pageable);

  /**
   * Consulta facturas por número de factura, identificación del comprador o fecha de creación. Solo
   * retorna datos básicos de la factura sin leer archivos del disco.
   *
   * @param queryDto Criterios de búsqueda
   * @param pageable Paginación
   * @param request HttpServletRequest para extraer IP y token para rate limiting
   * @return Página de facturas encontradas
   */
  Page<InvoiceSearchResponseDto> queryInvoices(
      InvoiceQueryDto queryDto, Pageable pageable, jakarta.servlet.http.HttpServletRequest request);

  // Draft invoice methods
  InvoiceResponse saveInvoiceDraft(InvoiceDto invoiceDto);

  List<InvoiceDraftResponseDto> getDraftInvoices();

  InvoiceDto getDraftInvoiceById(Long idInvoice);

  /**
   * Obtiene una factura completa por ID (puede ser borrador o autorizada).
   *
   * @param idInvoice ID de la factura
   * @return DTO con datos completos de la factura
   * @throws IllegalArgumentException si la factura no existe
   */
  InvoiceDto getInvoiceById(Long idInvoice);

  InvoiceResponse processDraftInvoice(Long idInvoice);

  /**
   * Elimina un borrador de factura
   *
   * @param idInvoice ID del borrador a eliminar
   * @throws IllegalArgumentException si la factura ya fue autorizada o no existe
   */
  void deleteDraft(Long idInvoice);

  /**
   * Obtiene una factura con las cantidades disponibles para crear nota de crédito. Calcula las
   * cantidades ya devueltas en NC previas.
   *
   * @param idInvoice ID de la factura
   * @return DTO con datos de factura y cantidades disponibles
   * @throws IllegalArgumentException si la factura no existe o no está autorizada
   */
  InvoiceAvailableForCreditNoteDto getInvoiceAvailableForCreditNote(Long idInvoice);

  /**
   * Consulta unificada de documentos (facturas y/o notas de crédito) según el tipo especificado.
   *
   * @param queryDto Criterios de búsqueda con filtro de tipo de documento
   * @param pageable Paginación
   * @param request HttpServletRequest para extraer IP y token para rate limiting
   * @return Página de documentos encontrados (facturas y/o notas de crédito)
   */
  Page<DocumentSearchResponseDto> queryDocuments(
      DocumentQueryDto queryDto,
      Pageable pageable,
      jakarta.servlet.http.HttpServletRequest request);

  /**
   * Busca facturas por idCompany (obligatorio) y filtros opcionales para crear notas de débito.
   * Retorna facturas autorizadas con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda
   * @param pageable Paginación
   * @return Página de InvoiceForDebitNoteResponseDto
   */
  Page<com.wallparisoft.ebillbilling.dto.InvoiceForDebitNoteResponseDto> searchInvoicesForDebitNote(
      com.wallparisoft.ebillbilling.dto.InvoiceSearchForDebitNoteDto searchDto, Pageable pageable);

  /**
   * Busca facturas por idCompany (obligatorio) y filtros opcionales con paginación. Retorna un DTO
   * optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de InvoiceListResponseDto
   */
  Page<InvoiceListResponseDto> searchInvoicesByCompany(
      com.wallparisoft.ebillbilling.dto.InvoiceSearchForCreditNoteDto searchDto, Pageable pageable);

  /**
   * Busca facturas por idCompany (obligatorio), rango de fechas y filtros opcionales con
   * paginación. Retorna un DTO optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio, fechas opcionales)
   * @param pageable Paginación
   * @return Página de InvoiceListResponseDto
   */
  Page<InvoiceListResponseDto> searchInvoicesByFilters(
      com.wallparisoft.ebillbilling.dto.InvoiceSearchByCompanyAndDateDto searchDto,
      Pageable pageable);

  /**
   * Busca facturas autorizadas y activas por número de factura o clave de acceso. Requiere
   * idCompany obligatorio y al menos uno de los criterios de búsqueda. Retorna información básica:
   * clave de acceso, número de autorización, fecha de emisión y número de comprobante.
   *
   * @param searchDto DTO con los criterios de búsqueda
   * @return Lista de facturas encontradas con información básica
   * @throws IllegalArgumentException si no se proporciona idCompany o ningún criterio de búsqueda
   */
  List<InvoiceBasicInfoDto> searchAuthorizedActiveInvoicesByNumberOrAccessKey(
      InvoiceSearchByNumberOrAccessKeyDto searchDto);
}
