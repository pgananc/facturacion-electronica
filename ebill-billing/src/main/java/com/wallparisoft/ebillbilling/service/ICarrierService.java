package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.CarrierDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICarrierService {

  /**
   * Obtiene todos los transportistas de una empresa
   *
   * @param idCompany ID de la empresa
   * @return Lista de transportistas
   */
  List<CarrierDto> getCarriersByCompany(Long idCompany);

  /**
   * Obtiene todos los transportistas de una empresa con paginación
   *
   * @param idCompany ID de la empresa
   * @param pageable Paginación
   * @return Página de transportistas
   */
  Page<CarrierDto> getCarriersByCompany(Long idCompany, Pageable pageable);

  /**
   * Obtiene todos los transportistas activos de una empresa
   *
   * @param idCompany ID de la empresa
   * @return Lista de transportistas activos
   */
  List<CarrierDto> getActiveCarriersByCompany(Long idCompany);

  /**
   * Obtiene todos los transportistas activos de una empresa con paginación
   *
   * @param idCompany ID de la empresa
   * @param pageable Paginación
   * @return Página de transportistas activos
   */
  Page<CarrierDto> getActiveCarriersByCompany(Long idCompany, Pageable pageable);

  /**
   * Obtiene un transportista por ID
   *
   * @param idCarrier ID del transportista
   * @return CarrierDto o null si no existe
   */
  CarrierDto getCarrierById(Long idCarrier);

  /**
   * Crea un nuevo transportista
   *
   * @param carrierDto DTO con los datos del transportista
   * @return CarrierDto creado
   */
  CarrierDto createCarrier(CarrierDto carrierDto);

  /**
   * Actualiza un transportista existente
   *
   * @param idCarrier ID del transportista
   * @param carrierDto DTO con los datos actualizados
   * @return CarrierDto actualizado
   */
  CarrierDto updateCarrier(Long idCarrier, CarrierDto carrierDto);

  /**
   * Elimina (desactiva) un transportista
   *
   * @param idCarrier ID del transportista
   * @return true si se eliminó correctamente
   */
  boolean deleteCarrier(Long idCarrier);
}
