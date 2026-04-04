package com.wallparisoft.ebillbilling.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Anotación para marcar endpoints que requieren rate limiting. */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

  /**
   * Tipo de rate limiting a aplicar.
   *
   * @return Tipo de rate limiting
   */
  RateLimitType type() default RateLimitType.QUERY;

  /**
   * Nombre del endpoint para logging.
   *
   * @return Nombre del endpoint
   */
  String endpoint() default "";
}
