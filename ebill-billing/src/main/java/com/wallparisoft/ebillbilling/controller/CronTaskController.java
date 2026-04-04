package com.wallparisoft.ebillbilling.controller;

import com.wallparisoft.ebillbilling.config.ConfigurableCronTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cron-task")
@RequiredArgsConstructor
@Slf4j
public class CronTaskController {

  private final ConfigurableCronTask configurableCronTask;

  /**
   * Endpoint para recargar la configuración del cron desde la base de datos y reprogramar la tarea.
   * Útil cuando se actualiza directamente en la BD sin usar el servicio.
   *
   * @return Respuesta indicando si la operación fue exitosa
   */
  @PostMapping("/reload")
  public ResponseEntity<String> reloadCronConfig() {
    try {
      log.info("Recargando configuración del cron desde la base de datos...");
      configurableCronTask.rescheduleTask();
      return ResponseEntity.ok("Configuración del cron recargada exitosamente");
    } catch (Exception e) {
      log.error("Error al recargar la configuración del cron", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error al recargar la configuración del cron: " + e.getMessage());
    }
  }
}
