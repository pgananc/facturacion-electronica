package com.wallparisoft.ebill.utils;

import java.security.SecureRandom;

public class TokenGenerator {

  private TokenGenerator() {
    super();
  }

  // Cadena de caracteres que se utilizarán para generar el token (solo letras)
  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  // Generador de números aleatorios criptográficamente seguro
  private static final SecureRandom RANDOM = new SecureRandom();

  /**
   * Genera un token aleatorio compuesto solo por letras.
   *
   * @param length La longitud del token que se desea generar (debe ser > 0).
   * @return El token generado como una cadena de texto.
   * @throws IllegalArgumentException si la longitud es menor o igual a cero.
   */
  public static String generateLetterToken(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("La longitud del token debe ser un número positivo.");
    }

    StringBuilder token = new StringBuilder(length);
    int alphabetLength = ALPHABET.length();

    for (int i = 0; i < length; i++) {
      // Selecciona un índice aleatorio dentro de la longitud del alfabeto
      int randomIndex = RANDOM.nextInt(alphabetLength);

      // Añade el carácter correspondiente al token
      token.append(ALPHABET.charAt(randomIndex));
    }

    return token.toString();
  }
}
