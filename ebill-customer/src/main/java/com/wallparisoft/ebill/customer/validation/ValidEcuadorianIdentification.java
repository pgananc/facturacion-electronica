package com.wallparisoft.ebill.customer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validación personalizada para cédulas y RUC ecuatorianos.
 *
 * <p>Valida:
 *
 * <ul>
 *   <li>Cédula: 10 dígitos con algoritmo módulo 10
 *   <li>RUC Persona Natural: 13 dígitos (cédula + 001)
 *   <li>RUC Sociedad Privada: 13 dígitos (tercer dígito = 9) con algoritmo módulo 11
 *   <li>RUC Sociedad Pública: 13 dígitos (tercer dígito = 6) con algoritmo módulo 11
 * </ul>
 *
 * <p>Ejemplo de uso:
 *
 * <pre>
 * &#64;Data
 * public class ClientDto {
 *   &#64;ValidEcuadorianIdentification(message = "Cédula o RUC inválido")
 *   private String identification;
 * }
 * </pre>
 */
@Documented
@Constraint(validatedBy = EcuadorianIdentificationValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEcuadorianIdentification {

  /**
   * Mensaje de error por defecto.
   *
   * @return mensaje de error
   */
  String message() default "Cédula o RUC ecuatoriano inválido";

  /**
   * Grupos de validación.
   *
   * @return grupos
   */
  Class<?>[] groups() default {};

  /**
   * Payload adicional.
   *
   * @return payload
   */
  Class<? extends Payload>[] payload() default {};

  /**
   * Permite valores nulos. Por defecto false (null no es válido).
   *
   * @return true si permite null
   */
  boolean allowNull() default false;
}
