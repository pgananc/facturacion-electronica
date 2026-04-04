package com.wallparisoft.ebillbilling.exception;

/**
 * Excepción lanzada cuando el usuario no tiene permisos para realizar la acción. Debe retornar HTTP
 * 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }

  public ForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }
}
