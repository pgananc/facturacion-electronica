package com.wallparisoft.ebillbilling.config;

import com.wallparisoft.ebillbilling.event.CronConfigUpdatedEvent;
import com.wallparisoft.ebillbilling.service.ISubscriptionService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * Componente para tarea programada que procesa cancelaciones de suscripciones programadas. Ejecuta
 * una vez al día (por defecto a la 1:00 AM) para cancelar suscripciones que fueron marcadas para
 * cancelación al vencer y ya expiraron.
 */
@Component
@Slf4j
public class SubscriptionCronTask implements DisposableBean {
  private static final String SUBSCRIPTION_CANCEL_CRON_EXPRESSION_PARAM =
      "SUBSCRIPTION_CANCEL_CRON_EXPRESSION";
  private static final String SUBSCRIPTION_CANCEL_CRON_ENABLED_PARAM =
      "SUBSCRIPTION_CANCEL_CRON_ENABLED";
  private static final String DEFAULT_CRON_EXPRESSION = "0 0 1 * * *"; // 1:00 AM diario
  private static final String DEFAULT_CRON_ENABLED = "true";

  private final ISubscriptionService subscriptionService;
  private final ThreadPoolTaskScheduler taskScheduler;
  private final ParamsService paramsService;

  private boolean cronTaskEnabled;
  private String cronExpression;
  private ScheduledFuture<?> scheduledFuture;

  public SubscriptionCronTask(
      ISubscriptionService subscriptionService,
      ThreadPoolTaskScheduler taskScheduler,
      ParamsService paramsService) {
    this.subscriptionService = subscriptionService;
    this.taskScheduler = taskScheduler;
    this.paramsService = paramsService;
    log.info("Constructor de SubscriptionCronTask ejecutado.");
  }

  @PostConstruct
  public void init() {
    loadCronConfigFromDatabase();
    scheduleTask();
  }

  /** Carga la configuración del cron desde la base de datos. */
  private void loadCronConfigFromDatabase() {
    cronExpression =
        paramsService.getParamValueOrDefault(
            SUBSCRIPTION_CANCEL_CRON_EXPRESSION_PARAM, DEFAULT_CRON_EXPRESSION);

    String enabledValue =
        paramsService.getParamValueOrDefault(
            SUBSCRIPTION_CANCEL_CRON_ENABLED_PARAM, DEFAULT_CRON_ENABLED);
    cronTaskEnabled = Boolean.parseBoolean(enabledValue);

    log.info(
        "Configuración del cron de cancelaciones de suscripciones cargada desde BD: enabled={}, expression={}",
        cronTaskEnabled,
        cronExpression);
  }

  /**
   * Método para reprogramar la tarea en tiempo de ejecución. Recarga la configuración desde la base
   * de datos.
   */
  public void rescheduleTask() {
    loadCronConfigFromDatabase();
    log.info(
        "Reprogramando tarea de cancelaciones de suscripciones: enable={}, expression={}",
        cronTaskEnabled,
        cronExpression);

    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
      scheduledFuture = null;
    }

    if (cronTaskEnabled) {
      scheduleTask();
    } else {
      log.info("La tarea cron de cancelaciones de suscripciones ha sido deshabilitada.");
    }
  }

  /**
   * Método para reprogramar la tarea en tiempo de ejecución con valores específicos.
   *
   * @param enable true para habilitar, false para deshabilitar
   * @param expression La nueva expresión cron
   */
  public void rescheduleTask(boolean enable, String expression) {
    this.cronTaskEnabled = enable;
    this.cronExpression = expression;
    log.info(
        "Reprogramando tarea de cancelaciones de suscripciones: enable={}, expression={}",
        enable,
        expression);

    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
      scheduledFuture = null;
    }

    if (cronTaskEnabled) {
      scheduleTask();
    } else {
      log.info("La tarea cron de cancelaciones de suscripciones ha sido deshabilitada.");
    }
  }

  private void scheduleTask() {
    if (cronTaskEnabled) {
      try {
        scheduledFuture =
            taskScheduler.schedule(this::executeTask, new CronTrigger(cronExpression));
        log.info(
            "Tarea de cancelaciones de suscripciones programada con expresión: {}", cronExpression);
      } catch (Exception e) {
        log.error(
            "Error al programar la tarea cron de cancelaciones de suscripciones con expresión '{}': {}",
            cronExpression,
            e.getMessage(),
            e);
      }
    } else {
      log.info(
          "La tarea cron de cancelaciones de suscripciones está deshabilitada y no se programará.");
    }
  }

  /** Ejecuta la tarea de procesamiento de cancelaciones programadas. */
  private void executeTask() {
    try {
      log.info("🔄 Ejecutando tarea programada de cancelaciones de suscripciones...");
      int cancelledCount = subscriptionService.processScheduledSubscriptionCancellations();
      log.info("✅ Tarea programada completada: {} suscripción(es) cancelada(s)", cancelledCount);
    } catch (Exception e) {
      log.error(
          "❌ Error al ejecutar tarea programada de cancelaciones de suscripciones: {}",
          e.getMessage(),
          e);
    }
  }

  /**
   * Escucha el evento de actualización de configuración del cron y reprograma la tarea
   * automáticamente.
   *
   * @param event Evento de actualización de configuración del cron
   */
  @EventListener
  @Async
  public void handleCronConfigUpdated(CronConfigUpdatedEvent event) {
    // Solo reprogramar si el evento es para nuestros parámetros
    if (SUBSCRIPTION_CANCEL_CRON_EXPRESSION_PARAM.equals(event.getParamCode())
        || SUBSCRIPTION_CANCEL_CRON_ENABLED_PARAM.equals(event.getParamCode())) {
      log.info(
          "Evento de actualización de configuración cron recibido para parámetro: {}",
          event.getParamCode());
      rescheduleTask();
    }
  }

  @Override
  public void destroy() throws Exception {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
    log.info("SubscriptionCronTask destruido.");
  }
}
