package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.permission.BulkUpdatePlansRequestDto;
import com.wallparisoft.ebillbilling.dto.permission.BulkUpdateResponseDto;
import com.wallparisoft.ebillbilling.dto.permission.FeatureSetDto;
import com.wallparisoft.ebillbilling.dto.permission.PlanFeatureDto;
import com.wallparisoft.ebillbilling.dto.permission.PlanFeatureUpdateDto;
import com.wallparisoft.ebillbilling.dto.permission.UpdateResultDto;
import com.wallparisoft.ebillbilling.entity.SubscriptionPlan;
import com.wallparisoft.ebillbilling.exception.NotFoundException;
import com.wallparisoft.ebillbilling.repository.ISubscriptionPlanRepo;
import com.wallparisoft.ebillbilling.service.IPermissionService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio de permisos y features de planes. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements IPermissionService {

  private final ISubscriptionPlanRepo subscriptionPlanRepo;

  @Override
  public List<PlanFeatureDto> getPlanFeatures() {
    log.info("📋 Obteniendo features de todos los planes");

    List<SubscriptionPlan> plans = subscriptionPlanRepo.findAll();

    return plans.stream()
        .filter(plan -> plan.getIsActive() != null && plan.getIsActive())
        .map(
            plan -> {
              FeatureSetDto features =
                  FeatureSetDto.builder()
                      .hasApiAccess(plan.getHasApiAccess())
                      .hasMultiUser(plan.getHasMultiUser())
                      .hasAdvancedReports(plan.getHasAdvancedReports())
                      .hasElectronicInvoice(plan.getHasElectronicInvoice())
                      .hasCreditNote(plan.getHasCreditNote())
                      .hasDebitNote(plan.getHasDebitNote())
                      .hasRetention(plan.getHasRetention())
                      .hasGuide(plan.getHasGuide())
                      .hasPrioritySupport(plan.getHasPrioritySupport())
                      .hasCustomBranding(plan.getHasCustomBranding())
                      .hasAutomatedBackups(plan.getHasAutomatedBackups())
                      .build();

              return PlanFeatureDto.builder()
                  .planCode(plan.getCode())
                  .planName(plan.getName())
                  .features(features)
                  .build();
            })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public BulkUpdateResponseDto updatePlansBulk(BulkUpdatePlansRequestDto request) {
    log.info("🔄 Actualizando features de {} planes en bulk", request.getUpdates().size());

    List<UpdateResultDto> results = new ArrayList<>();
    int successCount = 0;
    int failedCount = 0;

    for (PlanFeatureUpdateDto update : request.getUpdates()) {
      try {
        SubscriptionPlan plan =
            subscriptionPlanRepo
                .findByCode(update.getPlanCode())
                .orElseThrow(
                    () ->
                        new NotFoundException(
                            "Plan con código '" + update.getPlanCode() + "' no encontrado"));

        // Actualizar features si están presentes en el DTO
        FeatureSetDto features = update.getFeatures();
        if (features != null) {
          if (features.getHasApiAccess() != null) {
            plan.setHasApiAccess(features.getHasApiAccess());
          }
          if (features.getHasMultiUser() != null) {
            plan.setHasMultiUser(features.getHasMultiUser());
          }
          if (features.getHasAdvancedReports() != null) {
            plan.setHasAdvancedReports(features.getHasAdvancedReports());
          }
          if (features.getHasElectronicInvoice() != null) {
            plan.setHasElectronicInvoice(features.getHasElectronicInvoice());
          }
          if (features.getHasCreditNote() != null) {
            plan.setHasCreditNote(features.getHasCreditNote());
          }
          if (features.getHasDebitNote() != null) {
            plan.setHasDebitNote(features.getHasDebitNote());
          }
          if (features.getHasRetention() != null) {
            plan.setHasRetention(features.getHasRetention());
          }
          if (features.getHasGuide() != null) {
            plan.setHasGuide(features.getHasGuide());
          }
          if (features.getHasPrioritySupport() != null) {
            plan.setHasPrioritySupport(features.getHasPrioritySupport());
          }
          if (features.getHasCustomBranding() != null) {
            plan.setHasCustomBranding(features.getHasCustomBranding());
          }
          if (features.getHasAutomatedBackups() != null) {
            plan.setHasAutomatedBackups(features.getHasAutomatedBackups());
          }
        }

        subscriptionPlanRepo.save(plan);
        successCount++;

        results.add(
            UpdateResultDto.builder()
                .identifier(update.getPlanCode())
                .success(true)
                .message("Plan actualizado exitosamente")
                .build());

        log.info("✅ Plan {} actualizado exitosamente", update.getPlanCode());

      } catch (Exception e) {
        failedCount++;
        String errorMessage = e.getMessage() != null ? e.getMessage() : "Error desconocido";

        results.add(
            UpdateResultDto.builder()
                .identifier(update.getPlanCode())
                .success(false)
                .message("Error al actualizar plan")
                .error(errorMessage)
                .build());

        log.error("❌ Error al actualizar plan {}: {}", update.getPlanCode(), errorMessage, e);
      }
    }

    boolean overallSuccess = failedCount == 0;
    String message =
        overallSuccess
            ? String.format("Se actualizaron exitosamente %d plan(es)", successCount)
            : String.format(
                "Se actualizaron %d plan(es) exitosamente, %d fallaron", successCount, failedCount);

    return BulkUpdateResponseDto.builder()
        .success(overallSuccess)
        .message(message)
        .totalUpdated(successCount)
        .totalFailed(failedCount)
        .results(results)
        .timestamp(LocalDateTime.now())
        .build();
  }
}
