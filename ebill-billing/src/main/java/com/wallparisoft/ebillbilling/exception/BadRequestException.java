package com.wallparisoft.ebillbilling.exception;

/**
 * Excepción lanzada cuando los datos de la solicitud son inválidos. Debe retornar HTTP 400 Bad
 * Request.
 */
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
