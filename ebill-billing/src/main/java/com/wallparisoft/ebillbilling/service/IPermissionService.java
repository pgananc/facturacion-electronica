package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.permission.BulkUpdatePlansRequestDto;
import com.wallparisoft.ebillbilling.dto.permission.BulkUpdateResponseDto;
import com.wallparisoft.ebillbilling.dto.permission.PlanFeatureDto;
import java.util.List;

/** Servicio para gestión de permisos y features de planes de suscripción. */
public interface IPermissionService {

  /**
   * Obtiene las features de todos los planes de suscripción.
   *
   * @return Lista de features por plan
   */
  List<PlanFeatureDto> getPlanFeatures();

  /**
   * Actualiza las features de múltiples planes en una operación masiva.
   *
   * @param request DTO con las actualizaciones a realizar
   * @return Resultado de la operación masiva
   */
  BulkUpdateResponseDto updatePlansBulk(BulkUpdatePlansRequestDto request);
}
