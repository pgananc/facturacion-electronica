package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.EstablishmentRequestDto;
import com.wallparisoft.ebill.customer.dto.EstablishmentResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IEstablishmentService {

  Page<EstablishmentResponseDto> getAllEstablishmentsByCompanyId(Long companyId, Pageable pageable);

  Page<EstablishmentResponseDto> getActiveEstablishmentsByCompanyId(
      Long companyId, Pageable pageable);

  /**
   * Obtiene todas las sucursales de una compañía sin paginación.
   *
   * @param companyId ID de la compañía
   * @return lista de sucursales
   */
  List<EstablishmentResponseDto> getAllEstablishmentsByCompanyId(Long companyId);

  EstablishmentResponseDto findEstablishmentByIdEstablishmentAndIdCompany(
      Long idEstablishment, Long idCompany);

  EstablishmentResponseDto createEstablishment(EstablishmentRequestDto dto);

  EstablishmentResponseDto updateEstablishment(
      Long id, Long companyId, EstablishmentRequestDto dto);

  void deactivateEstablishment(Long id, Long companyId);

  /**
   * Desactiva la sucursal y opcionalmente sus dependencias (puntos de emisión) en una transacción.
   *
   * @param id ID del establecimiento
   * @param companyId ID de la compañía (validación de pertenencia)
   * @param cascade si true, desactiva antes todos los puntos de emisión del establecimiento
   */
  void deactivateEstablishment(Long id, Long companyId, boolean cascade);

  /**
   * Valida si existe un establecimiento con el código especificado para la compañía.
   *
   * @param companyId ID de la compañía
   * @param code código del establecimiento
   * @return true si existe, false si no
   */
  boolean existsByCodeAndCompany(Long companyId, String code);
}
