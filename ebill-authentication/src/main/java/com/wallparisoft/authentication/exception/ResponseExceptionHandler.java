package com.wallparisoft.authentication.exception;

import com.wallparisoft.ebill.utils.exception.InternalException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

  // Utilitario para crear respuestas estándar
  private ResponseEntity<ExceptionResponse> buildResponse(
      Exception ex, WebRequest req, HttpStatus status) {
    ExceptionResponse er =
        ExceptionResponse.builder()
            .timestamp(LocalDateTime.now())
            .message(ex.getMessage())
            .details(req.getDescription(false))
            .build();
    return new ResponseEntity<>(er, status);
  }

  // Manejo de excepciones genéricas
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request) {
    log.error("Excepción inesperada", ex);
    return buildResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Modelo no encontrado
  @ExceptionHandler(ModelNotFoundException.class)
  public ResponseEntity<ExceptionResponse> handleModelNotFound(
      ModelNotFoundException ex, WebRequest request) {
    log.info("Recurso no encontrado: {}", ex.getMessage());
    return buildResponse(ex, request, HttpStatus.NOT_FOUND);
  }

  // Recurso ya existente
  @ExceptionHandler(ResourceExistsException.class)
  public ResponseEntity<ExceptionResponse> handleResourceExists(
      ResourceExistsException ex, WebRequest request) {
    log.warn("Conflicto detectado: {}", ex.getMessage());
    return buildResponse(ex, request, HttpStatus.CONFLICT);
  }

  // Recurso en uso (conflicto por exclusividad)
  @ExceptionHandler(ResourceInUseException.class)
  public ResponseEntity<ExceptionResponse> handleResourceInUse(
      ResourceInUseException ex, WebRequest request) {
    log.warn("Recurso en uso: {}", ex.getMessage());
    return buildResponse(ex, request, HttpStatus.CONFLICT);
  }

  // Rate limiting excedido
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<RateLimitExceededResponse> handleRateLimitExceeded(
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

  // Excepción interna personalizada
  @ExceptionHandler(InternalException.class)
  public ResponseEntity<ExceptionResponse> handleInternal(
      InternalException ex, WebRequest request) {
    log.error("Error interno de la aplicación", ex);
    return buildResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Validaciones en @RequestBody (@Valid)
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (msg1, msg2) -> msg1 + "; " + msg2));

    ValidationErrorResponse er =
        ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .message("Validación fallida")
            .errors(fieldErrors)
            .build();

    log.warn("Errores de validación: {}", fieldErrors);
    return new ResponseEntity<>(er, HttpStatus.BAD_REQUEST);
  }

  // Validaciones en @RequestParam / @PathVariable / @Validated
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ExceptionResponse> handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {

    String message =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining("; "));

    ExceptionResponse er =
        ExceptionResponse.builder()
            .timestamp(LocalDateTime.now())
            .message("Violación de restricción")
            .details(message)
            .build();

    log.warn("Violaciones de constraints: {}", message);
    return new ResponseEntity<>(er, HttpStatus.BAD_REQUEST);
  }
}
