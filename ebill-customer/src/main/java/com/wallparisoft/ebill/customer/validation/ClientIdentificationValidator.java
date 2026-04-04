package com.wallparisoft.ebill.customer.validation;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.enumeration.IdentificationTypeEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador a nivel de clase que determina si aplicar validación ecuatoriana según el tipo de
 * identificación del cliente. Solo aplica reglas estrictas de cédula/RUC para tipos ecuatorianos
 * (CED, RUC). Para Pasaporte, Identificación del exterior y otros tipos, solo verifica que el campo
 * no esté vacío y tenga máximo 20 caracteres.
 */
public class ClientIdentificationValidator
    implements ConstraintValidator<ValidClientIdentification, ClientDto> {

  private static final int MAX_FOREIGN_ID_LENGTH = 20;

  @Override
  public boolean isValid(ClientDto dto, ConstraintValidatorContext context) {
    if (dto == null) {
      return true;
    }

    String identification = dto.getIdentification();
    Integer idType = dto.getIdType();

    if (identification == null || identification.trim().isEmpty()) {
      addFieldError(context, "La identificación es requerida");
      return false;
    }

    identification = identification.trim();

    if (idType != null && IdentificationTypeEnum.requiresEcuadorianValidation(idType)) {
      if (!EcuadorianIdentificationValidator.isValidEcuadorianId(identification, false)) {
        addFieldError(context, "Cédula o RUC del cliente inválido");
        return false;
      }
    } else {
      if (identification.length() > MAX_FOREIGN_ID_LENGTH) {
        addFieldError(
            context, "La identificación no puede exceder " + MAX_FOREIGN_ID_LENGTH + " caracteres");
        return false;
      }
    }

    return true;
  }

  private void addFieldError(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode("identification")
        .addConstraintViolation();
  }
}
