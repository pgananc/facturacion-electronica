package com.wallparisoft.ebillbilling.event;

import org.springframework.context.ApplicationEvent;

/** Evento que se publica cuando se actualiza la configuración del cron en la base de datos. */
public class CronConfigUpdatedEvent extends ApplicationEvent {

  private final String paramCode;

  public CronConfigUpdatedEvent(Object source, String paramCode) {
    super(source);
    this.paramCode = paramCode;
  }

  public String getParamCode() {
    return paramCode;
  }
}
