package com.wallparisoft.ebill.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/**
 * Validador para cédulas y RUC ecuatorianos.
 *
 * <p>Implementa los algoritmos oficiales del SRI (Servicio de Rentas Internas) de Ecuador.
 */
public class EcuadorianIdentificationValidator
    implements ConstraintValidator<ValidEcuadorianIdentification, String> {

  private static final Set<String> VALID_PROVINCES =
      Set.of(
          "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15",
          "16", "17", "18", "19", "20", "21", "22", "23", "24", "30");

  /** RUC genérico para "Consumidor Final" según normativa SRI Ecuador. */
  private static final String CONSUMIDOR_FINAL = "9999999999999";

  private boolean allowNull;

  @Override
  public void initialize(ValidEcuadorianIdentification constraintAnnotation) {
    this.allowNull = constraintAnnotation.allowNull();
  }

  @Override
  public boolean isValid(String identification, ConstraintValidatorContext context) {
    return isValidEcuadorianId(identification, allowNull);
  }

  /**
   * Valida una identificación ecuatoriana (cédula o RUC). Método estático reutilizable por otros
   * validadores.
   *
   * @param identification identificación a validar
   * @param allowNull si se permiten valores nulos o vacíos
   * @return true si es válida
   */
  public static boolean isValidEcuadorianId(String identification, boolean allowNull) {
    if (identification == null || identification.trim().isEmpty()) {
      return allowNull;
    }

    identification = identification.trim();

    if (CONSUMIDOR_FINAL.equals(identification)) {
      return true;
    }

    if (!identification.matches("^\\d+$")) {
      return false;
    }

    int length = identification.length();
    if (length != 10 && length != 13) {
      return false;
    }

    String provinceCode = identification.substring(0, 2);
    if (!VALID_PROVINCES.contains(provinceCode)) {
      return false;
    }

    int thirdDigit = Character.getNumericValue(identification.charAt(2));

    if (length == 10) {
      return validateCedula(identification, thirdDigit);
    } else {
      return validateRuc(identification, thirdDigit);
    }
  }

  /**
   * Valida una cédula ecuatoriana (10 dígitos).
   *
   * @param cedula cédula a validar
   * @param thirdDigit tercer dígito (debe ser 0-5 para personas naturales)
   * @return true si es válida
   */
  private static boolean validateCedula(String cedula, int thirdDigit) {
    // El tercer dígito debe ser menor a 6 (personas naturales)
    if (thirdDigit >= 6) {
      return false;
    }

    // Algoritmo módulo 10
    return validateModulo10(cedula);
  }

  /**
   * Valida un RUC ecuatoriano (13 dígitos).
   *
   * @param ruc RUC a validar
   * @param thirdDigit tercer dígito (determina el tipo de RUC)
   * @return true si es válido
   */
  private static boolean validateRuc(String ruc, int thirdDigit) {
    if (thirdDigit < 6) {
      // RUC Persona Natural: cédula + 001
      String cedula = ruc.substring(0, 10);
      String establishment = ruc.substring(10, 13);

      // Debe terminar en 001
      if (!"001".equals(establishment)) {
        return false;
      }

      // Validar la cédula (primeros 10 dígitos)
      return validateModulo10(cedula);

    } else if (thirdDigit == 6) {
      // RUC Sociedad Pública (tercer dígito = 6)
      return validateRucSociedadPublica(ruc);

    } else if (thirdDigit == 9) {
      // RUC Sociedad Privada (tercer dígito = 9)
      return validateRucSociedadPrivada(ruc);

    } else {
      // Tercer dígito inválido (debe ser 0-5, 6 o 9)
      return false;
    }
  }

  /**
   * Valida RUC de Sociedad Pública (tercer dígito = 6).
   *
   * @param ruc RUC a validar
   * @return true si es válido
   */
  private static boolean validateRucSociedadPublica(String ruc) {
    // Los últimos 4 dígitos deben ser 0001
    String establishment = ruc.substring(9, 13);
    if (!"0001".equals(establishment)) {
      return false;
    }

    // Validar con módulo 11 (primeros 9 dígitos)
    int[] coefficients = {3, 2, 7, 6, 5, 4, 3, 2};
    return validateModulo11(ruc.substring(0, 9), coefficients);
  }

  /**
   * Valida RUC de Sociedad Privada (tercer dígito = 9).
   *
   * @param ruc RUC a validar
   * @return true si es válido
   */
  private static boolean validateRucSociedadPrivada(String ruc) {
    // Los últimos 3 dígitos deben ser 001
    String establishment = ruc.substring(10, 13);
    if (!"001".equals(establishment)) {
      return false;
    }

    // Validar con módulo 11 (primeros 10 dígitos)
    int[] coefficients = {4, 3, 2, 7, 6, 5, 4, 3, 2};
    return validateModulo11(ruc.substring(0, 10), coefficients);
  }

  /**
   * Algoritmo de validación módulo 10 (para cédulas y RUC persona natural).
   *
   * @param digits dígitos a validar
   * @return true si el dígito verificador es correcto
   */
  private static boolean validateModulo10(String digits) {
    int sum = 0;

    // Procesar los primeros 9 dígitos
    for (int i = 0; i < 9; i++) {
      int digit = Character.getNumericValue(digits.charAt(i));

      // Multiplicar por 2 si la posición es par (0, 2, 4, 6, 8)
      if (i % 2 == 0) {
        digit *= 2;
        // Si el resultado es mayor a 9, restar 9
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
    }

    // Calcular el dígito verificador
    int verifier = (10 - (sum % 10)) % 10;

    // Comparar con el último dígito
    int lastDigit = Character.getNumericValue(digits.charAt(9));

    return verifier == lastDigit;
  }

  /**
   * Algoritmo de validación módulo 11 (para RUC de sociedades).
   *
   * @param digits dígitos a validar (sin el dígito verificador)
   * @param coefficients coeficientes para multiplicar
   * @return true si el dígito verificador es correcto
   */
  private static boolean validateModulo11(String digits, int[] coefficients) {
    int sum = 0;

    // Multiplicar cada dígito por su coeficiente
    for (int i = 0; i < coefficients.length; i++) {
      int digit = Character.getNumericValue(digits.charAt(i));
      sum += digit * coefficients[i];
    }

    // Calcular el dígito verificador
    int remainder = sum % 11;
    int verifier = (remainder == 0) ? 0 : (11 - remainder);

    // Comparar con el dígito verificador (posición 8 para sociedades)
    int checkDigit = Character.getNumericValue(digits.charAt(coefficients.length));

    return verifier == checkDigit;
  }
}
