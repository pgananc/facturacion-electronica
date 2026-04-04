package com.wallparisoft.ebillbilling.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Configuración para el SRI (Servicio de Rentas Internas) */
@Slf4j
@Getter
@Configuration
public class SriConfiguration {

  /** Ambiente del SRI configurado en application.properties 1 = PRUEBAS 2 = PRODUCCION */
  @Value("${sri.environment:1}")
  private String environment;

  /**
   * Verifica si el ambiente configurado es de pruebas
   *
   * @return true si es ambiente de pruebas
   */
  public boolean isPruebas() {
    return "1".equals(environment);
  }

  /**
   * Verifica si el ambiente configurado es de producción
   *
   * @return true si es ambiente de producción
   */
  public boolean isProduccion() {
    return "2".equals(environment);
  }

  /**
   * Obtiene el nombre legible del ambiente
   *
   * @return "PRUEBAS" o "PRODUCCION"
   */
  public String getEnvironmentName() {
    return isPruebas() ? "PRUEBAS" : "PRODUCCION";
  }

  /** Log del ambiente configurado al iniciar la aplicación */
  @org.springframework.context.event.EventListener(
      org.springframework.boot.context.event.ApplicationReadyEvent.class)
  public void logEnvironment() {
    if (isProduccion()) {
      log.warn("🚨 ⚠️  SRI CONFIGURADO EN AMBIENTE DE PRODUCCIÓN ⚠️  🚨");
      log.warn("🚨 Las facturas se enviarán al SRI REAL");
    } else {
      log.info("🧪 SRI configurado en ambiente de PRUEBAS");
      log.info("🧪 Las facturas se enviarán al SRI de pruebas");
    }
  }
}
