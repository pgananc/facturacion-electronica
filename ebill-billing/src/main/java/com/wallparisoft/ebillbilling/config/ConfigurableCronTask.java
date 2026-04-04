package com.wallparisoft.ebillbilling.config;

import com.wallparisoft.ebillbilling.event.CronConfigUpdatedEvent;
import com.wallparisoft.ebillbilling.service.ParamsService;
import com.wallparisoft.ebillbilling.service.TaskService;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConfigurableCronTask implements DisposableBean {
  private static final String CRON_TASK_EXPRESSION_PARAM = "CRON_TASK_EXPRESSION";
  private static final String CRON_TASK_ENABLED_PARAM = "CRON_TASK_ENABLED";
  private static final String DEFAULT_CRON_EXPRESSION = "0 */2 * * * *";
  private static final String DEFAULT_CRON_ENABLED = "false";

  private final TaskService taskService;
  private final ThreadPoolTaskScheduler taskScheduler;
  private final ParamsService paramsService;

  private boolean cronTaskEnabled;
  private String cronExpression;
  private ScheduledFuture<?> scheduledFuture;

  // Inyectar TaskService, ThreadPoolTaskScheduler y ParamsService
  public ConfigurableCronTask(
      TaskService taskService, ThreadPoolTaskScheduler taskScheduler, ParamsService paramsService) {
    this.taskService = taskService;
    this.taskScheduler = taskScheduler;
    this.paramsService = paramsService;
    log.info("Constructor de ConfigurableCronTask ejecutado.");
  }

  @PostConstruct
  public void init() {
    loadCronConfigFromDatabase();
    scheduleTask(); // Programar la tarea al inicio
  }

  /** Carga la configuración del cron desde la base de datos. */
  private void loadCronConfigFromDatabase() {
    // Leer expresión cron desde BD
    cronExpression =
        paramsService.getParamValueOrDefault(CRON_TASK_EXPRESSION_PARAM, DEFAULT_CRON_EXPRESSION);

    // Leer estado habilitado/deshabilitado desde BD
    String enabledValue =
        paramsService.getParamValueOrDefault(CRON_TASK_ENABLED_PARAM, DEFAULT_CRON_ENABLED);
    cronTaskEnabled = Boolean.parseBoolean(enabledValue);

    log.info(
        "Configuración del cron cargada desde BD: enabled={}, expression={}",
        cronTaskEnabled,
        cronExpression);
  }

  /**
   * Método para reprogramar la tarea en tiempo de ejecución. Recarga la configuración desde la base
   * de datos.
   */
  public void rescheduleTask() {
    loadCronConfigFromDatabase();
    log.info("Reprogramando tarea: enable={}, expression={}", cronTaskEnabled, cronExpression);

    // Cancelar la tarea actual si está programada
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false); // No interrumpir si ya está en ejecución
      scheduledFuture = null;
    }

    // Programar la nueva tarea solo si está habilitada
    if (cronTaskEnabled) {
      scheduleTask();
    } else {
      log.info("La tarea cron ha sido deshabilitada.");
    }
  }

  /**
   * Método para reprogramar la tarea en tiempo de ejecución con valores específicos.
   *
   * @param enable true para habilitar, false para deshabilitar.
   * @param expression La nueva expresión cron.
   */
  public void rescheduleTask(boolean enable, String expression) {
    this.cronTaskEnabled = enable;
    this.cronExpression = expression;
    log.info("Reprogramando tarea: enable=" + enable + ", expression=" + expression);

    // Cancelar la tarea actual si está programada
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false); // No interrumpir si ya está en ejecución
      scheduledFuture = null;
    }

    // Programar la nueva tarea solo si está habilitada
    if (cronTaskEnabled) {
      scheduleTask();
    } else {
      log.info("La tarea cron ha sido deshabilitada.");
    }
  }

  private void scheduleTask() {
    if (cronTaskEnabled) {
      try {
        // Programar la tarea usando el ThreadPoolTaskScheduler inyectado
        scheduledFuture =
            taskScheduler.schedule(taskService::executeTask, new CronTrigger(cronExpression));
        log.info("Tarea programada con expresión: " + cronExpression);
      } catch (Exception e) {
        log.error(
            "Error al programar la tarea cron con expresión '{}': {}",
            cronExpression,
            e.getMessage(),
            e);
      }
    } else {
      log.info("La tarea cron está deshabilitada y no se programará.");
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
    log.info(
        "Evento de actualización de configuración cron recibido para parámetro: {}",
        event.getParamCode());
    rescheduleTask();
  }

  @Override
  public void destroy() throws Exception {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(
          false); // Asegurarse de que la tarea se cancele al detener la aplicación
    }
    log.info("ConfigurableCronTask destruido.");
  }
}
