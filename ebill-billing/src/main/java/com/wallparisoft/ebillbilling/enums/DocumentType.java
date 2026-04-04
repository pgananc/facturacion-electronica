package com.wallparisoft.ebillbilling.enums;

import com.wallparisoft.ebillbilling.entity.SubscriptionPlan;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Tipos de documento que pueden estar habilitados en un plan de suscripción. Centraliza la relación
 * entre el código del tipo y el flag correspondiente en {@link SubscriptionPlan}.
 */
public enum DocumentType {
  INVOICE("INVOICE", plan -> Boolean.TRUE.equals(plan.getHasElectronicInvoice())),
  CREDIT_NOTE("CREDIT_NOTE", plan -> Boolean.TRUE.equals(plan.getHasCreditNote())),
  DEBIT_NOTE("DEBIT_NOTE", plan -> Boolean.TRUE.equals(plan.getHasDebitNote())),
  RETENTION("RETENTION", plan -> Boolean.TRUE.equals(plan.getHasRetention())),
  GUIDE("GUIDE", plan -> Boolean.TRUE.equals(plan.getHasGuide()));

  private final String code;
  private final Predicate<SubscriptionPlan> planHasFeature;

  DocumentType(String code, Predicate<SubscriptionPlan> planHasFeature) {
    this.code = code;
    this.planHasFeature = planHasFeature;
  }

  public String getCode() {
    return code;
  }

  /** Indica si el plan incluye este tipo de documento. */
  public boolean isAvailableIn(SubscriptionPlan plan) {
    return plan != null && Boolean.TRUE.equals(planHasFeature.test(plan));
  }

  /**
   * Parsea el código de tipo de documento (case-insensitive). Útil cuando viene como string del
   * request (CanEmitRequestDto, validateCanEmit, etc.).
   */
  public static Optional<DocumentType> from(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    String normalized = code.trim().toUpperCase();
    return Arrays.stream(values()).filter(dt -> dt.code.equals(normalized)).findFirst();
  }
}
