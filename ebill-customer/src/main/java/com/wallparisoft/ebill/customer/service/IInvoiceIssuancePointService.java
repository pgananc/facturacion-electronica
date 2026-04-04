package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.EstablishmentWithIssuancePointDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointRequestDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointResponseDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointWithEstablishmentDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

public interface IInvoiceIssuancePointService {

  /**
   * Obtiene un punto de emisión por su Id e Id del establecimiento al que pertenece.
   *
   * @param idInvoiceIssuancePoint Id del punto de emisión
   * @param idEstablishment Id del establecimiento
   * @return DTO con la información si existe
   */
  Optional<InvoiceIssuancePointResponseDto> findByIdAndEstablishment(
      Long idInvoiceIssuancePoint, Long idEstablishment);

  /**
   * Obtiene todos los puntos de emisión de un establecimiento con paginación.
   *
   * @param idEstablishment Id del establecimiento
   * @param pageable objeto de paginación
   * @return página con DTOs
   */
  Page<InvoiceIssuancePointResponseDto> findAllByEstablishment(
      Long idEstablishment, Pageable pageable);

  /**
   * Obtiene todos los puntos de emisión activos de un establecimiento con paginación.
   *
   * @param idEstablishment Id del establecimiento
   * @param pageable objeto de paginación
   * @return página con DTOs activos
   */
  Page<InvoiceIssuancePointResponseDto> findAllActiveByEstablishment(
      Long idEstablishment, Pageable pageable);

  /**
   * Obtiene todos los puntos de emisión de un establecimiento sin paginación.
   *
   * @param idEstablishment ID del establecimiento
   * @return lista de puntos de emisión
   */
  List<InvoiceIssuancePointResponseDto> findAllByEstablishment(Long idEstablishment);

  /**
   * Obtiene todos los puntos de emisión de una compañía (todos los establecimientos) con
   * información del establecimiento.
   *
   * @param idCompany ID de la compañía
   * @return lista de puntos con información del establecimiento
   */
  List<InvoiceIssuancePointWithEstablishmentDto> findAllByCompany(Long idCompany);

  InvoiceIssuancePointResponseDto create(InvoiceIssuancePointRequestDto dto);

  InvoiceIssuancePointResponseDto update(
      Long id, Long establishmentId, InvoiceIssuancePointRequestDto dto);

  void deactivate(Long id, Long establishmentId);

  List<InvoiceIssuancePointResponseDto> findAvailableByEstablishments(List<Long> establishmentIds);

  InvoiceIssuancePointResponseDto getCurrentSession(String userName);

  /**
   * Actualiza el secuencial de un punto de emisión basado en su código y establecimiento.
   *
   * @param code código del punto de emisión
   * @param establishmentId ID del establecimiento
   * @param newSequential nuevo valor del secuencial
   * @return DTO actualizado del punto de emisión
   * @throws ResponseStatusException si el punto de emisión no existe o no está activo
   */
  InvoiceIssuancePointResponseDto updateSequentialByCode(
      String code, Long establishmentId, Long newSequential);

  /**
   * Valida si existe un punto de emisión con el código especificado para el establecimiento.
   *
   * @param establishmentId ID del establecimiento
   * @param code código del punto de emisión
   * @return true si existe, false si no
   */
  boolean existsByCodeAndEstablishment(Long establishmentId, String code);

  /**
   * Obtiene un Establishment con su InvoiceIssuancePoint asociado por el ID del punto de emisión.
   * Esta estructura es compatible con EstablishmentDto de ebill-billing.
   *
   * @param idInvoiceIssuancePoint ID del punto de emisión
   * @return DTO con la información del establecimiento y su punto de emisión
   * @throws ResponseStatusException si el punto de emisión no existe
   */
  EstablishmentWithIssuancePointDto getEstablishmentWithIssuancePointById(
      Long idInvoiceIssuancePoint);
}
