package com.wallparisoft.ebillbilling.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/** Manejador global de excepciones para estandarizar las respuestas de error. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("status", 400);
    body.put("error", "Bad Request");
    body.put("message", "Datos inválidos. Verifica la información");
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                error -> {
                  Map<String, String> errorDetail = new HashMap<>();
                  errorDetail.put("field", error.getField());
                  errorDetail.put("message", error.getDefaultMessage());
                  return errorDetail;
                })
            .collect(Collectors.toList());

    body.put("errors", errors);

    log.error(
        "❌ Validación fallida en {}: {}",
        request.getDescription(false).replace("uri=", ""),
        errors);

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFoundException(
      NotFoundException ex, WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("status", 404);
    body.put("error", "Not Found");
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    log.warn("❌ Recurso no encontrado: {}", ex.getMessage());

    return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, Object>> handleConflictException(
      ConflictException ex, WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("status", 409);
    body.put("error", "Conflict");
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    // Agregar detalles si están disponibles
    if (ex.getIdCompany() != null) {
      Map<String, Object> details = new HashMap<>();
      details.put("idCompany", ex.getIdCompany());
      if (ex.getExistingSubscriptionId() != null) {
        details.put("existingSubscriptionId", ex.getExistingSubscriptionId());
      }
      if (ex.getExpiresAt() != null) {
        details.put("expiresAt", ex.getExpiresAt());
      }
      body.put("details", details);
    }

    log.warn("❌ Conflicto: {}", ex.getMessage());

    return new ResponseEntity<>(body, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequestException(
      BadRequestException ex, WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("status", 400);
    body.put("error", "Bad Request");
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    log.warn("❌ Solicitud inválida: {}", ex.getMessage());

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<Map<String, Object>> handleForbiddenException(
      ForbiddenException ex, WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("status", 403);
    body.put("error", "Forbidden");
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    log.warn("❌ Acceso denegado: {}", ex.getMessage());

    return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("status", 400);
    body.put("error", "Bad Request");
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    log.warn("❌ Argumento inválido: {}", ex.getMessage());

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalStateException(
      IllegalStateException ex, WebRequest request) {

    // Si el mensaje indica falta de permisos, retornar 403
    if (ex.getMessage() != null
        && (ex.getMessage().contains("permisos")
            || ex.getMessage().contains("SUPER_ADMIN")
            || ex.getMessage().contains("super administrador"))) {
      Map<String, Object> body = new HashMap<>();
      body.put("status", 403);
      body.put("error", "Forbidden");
      body.put("message", ex.getMessage());
      body.put("timestamp", LocalDateTime.now());
      body.put("path", request.getDescription(false).replace("uri=", ""));

      log.warn("❌ Acceso denegado: {}", ex.getMessage());

      return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // Caso contrario, retornar 400
    Map<String, Object> body = new HashMap<>();
    body.put("status", 400);
    body.put("error", "Bad Request");
    body.put("message", ex.getMessage());
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    log.warn("❌ Estado inválido: {}", ex.getMessage());

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<Map<String, Object>> handleNullPointerException(
      NullPointerException ex, WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("status", 500);
    body.put("error", "Internal Server Error");
    body.put(
        "message",
        ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor: Referencia nula");
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    log.error("❌ NullPointerException: {}", ex.getMessage(), ex);

    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<?> handleRateLimitExceeded(
      RateLimitExceededException ex, WebRequest request) {
    log.warn("Rate limit excedido: {}", ex.getMessage());

    RateLimitExceededResponse response =
        RateLimitExceededResponse.builder()
            .timestamp(ex.getTimestamp())
            .message(ex.getMessage())
            .attemptsUsed(ex.getAttemptsUsed())
            .maxAttempts(ex.getMaxAttempts())
            .minutesUntilNextAttempt(ex.getMinutesUntilNextAttempt())
            .cooldownMinutes(ex.getCooldownMinutes())
            .build();

    return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS); // 429
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(
      Exception ex, WebRequest request) {

    // No capturar RateLimitExceededException aquí - debe ser manejada por su handler específico
    if (ex instanceof RateLimitExceededException) {
      log.error(
          "❌ ERROR: RateLimitExceededException capturada por handler genérico! Esto no debería pasar.");
      // Relanzar para que el handler específico la capture
      throw (RateLimitExceededException) ex;
    }

    Map<String, Object> body = new HashMap<>();
    body.put("status", 500);
    body.put("error", "Internal Server Error");
    body.put("message", "Error interno del servidor. Contacta al administrador");
    body.put("timestamp", LocalDateTime.now());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    log.error("❌ Error interno del servidor: {}", ex.getMessage(), ex);

    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
