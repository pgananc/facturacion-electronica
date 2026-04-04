package com.wallparisoft.ebillbilling.exception;

/** Excepción lanzada cuando un recurso no se encuentra. Debe retornar HTTP 404 Not Found. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
