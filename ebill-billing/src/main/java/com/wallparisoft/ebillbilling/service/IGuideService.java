package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.dto.GuideListResponseDto;
import com.wallparisoft.ebillbilling.entity.Guide;
import com.wallparisoft.ebillbilling.response.GuideResponse;
import java.nio.file.Path;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IGuideService extends ICRUD<Guide> {

  /**
   * Lista todas las guías de remisión con datos del cliente y transportista.
   *
   * @return Lista de GuideListResponseDto
   */
  List<GuideListResponseDto> listWithClientData();

  GuideResponse saveGuide(GuideDto guideDto);

  GuideResponse updateGuide(Long id, GuideDto guideDto);

  GuideDto getGuideById(Long id);

  Page<Guide> searchGuides(String guideNumber, Long idClient, Long idCarrier, Pageable pageable);

  /**
   * Busca guías de remisión por idCompany (obligatorio) y filtros opcionales con paginación.
   * Retorna un DTO optimizado para listado con datos del cliente (destinatario) y transportista.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de GuideSearchResponseDto
   */
  Page<com.wallparisoft.ebillbilling.dto.GuideSearchResponseDto> searchGuidesByCompany(
      com.wallparisoft.ebillbilling.dto.GuideSearchByCompanyDto searchDto, Pageable pageable);

  /**
   * Guarda una guía de remisión como borrador (sin enviar al SRI).
   *
   * @param guideDto DTO con los datos de la guía
   * @return GuideResponse con el resultado
   */
  GuideResponse saveGuideDraft(GuideDto guideDto);

  /**
   * Envía un borrador de guía de remisión al SRI.
   *
   * @param idGuide ID de la guía
   * @param password Contraseña de la firma digital
   * @return GuideResponse con el resultado
   */
  GuideResponse sendGuideToSri(Long idGuide, String password);

  /**
   * Obtiene la ruta del archivo PDF de una guía de remisión.
   *
   * @param idGuide ID de la guía
   * @return Path al archivo PDF
   */
  Path getPdfFilePath(Long idGuide);

  /**
   * Obtiene la ruta del archivo XML autorizado de una guía de remisión.
   *
   * @param idGuide ID de la guía
   * @return Path al archivo XML
   */
  Path getXmlFilePath(Long idGuide);

  /**
   * Envía la guía de remisión por email al cliente.
   *
   * @param idGuide ID de la guía
   * @param email Email alternativo (opcional, null usa el del cliente)
   * @return true si se envió correctamente
   */
  boolean sendGuideByEmail(Long idGuide, String email);

  /**
   * Ejecuta el envío automático de guías de remisión pendientes al SRI. Este método es llamado por
   * una tarea programada para procesar borradores y guías devueltas automáticamente con sistema de
   * reintentos.
   */
  void executeSendGuideToSri();
}
