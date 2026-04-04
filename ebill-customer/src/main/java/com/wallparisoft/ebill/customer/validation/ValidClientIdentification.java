package com.wallparisoft.ebill.customer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validación a nivel de clase para la identificación del cliente. Aplica reglas de validación
 * ecuatoriana (cédula/RUC) solo cuando el tipo de identificación lo requiere. Para tipos como
 * Pasaporte o Identificación del exterior, se omite la validación ecuatoriana y solo se verifica
 * que el campo no esté vacío.
 */
@Documented
@Constraint(validatedBy = ClientIdentificationValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidClientIdentification {

  String message() default "Identificación del cliente inválida";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
