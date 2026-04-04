package com.wallparisoft.ebill.customer.exception;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Control advice for exception.
 *
 * @author PABI1
 */
@ControllerAdvice
@FieldDefaults(level = PRIVATE)
@Slf4j
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(Exception.class)
  public final ResponseEntity<BasicResponse> handleAllException(Exception ex, WebRequest request) {
    BasicResponse basicResponse =
        BasicResponse.builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message(ex.getMessage())
            .build();
    return new ResponseEntity<>(basicResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ModelNotFoundException.class)
  public final ResponseEntity<BasicResponse> handleModelNotFoundException(
      ModelNotFoundException ex, WebRequest request) {
    BasicResponse basicResponse =
        BasicResponse.builder()
            .code(HttpStatus.NOT_FOUND.value())
            .status(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .build();
    return new ResponseEntity<>(basicResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IllegalStateException.class)
  public final ResponseEntity<BasicResponse> handleIllegalStateException(
      IllegalStateException ex, WebRequest request) {
    BasicResponse basicResponse =
        BasicResponse.builder()
            .code(HttpStatus.CONFLICT.value())
            .status(HttpStatus.CONFLICT.getReasonPhrase())
            .message(ex.getMessage())
            .build();
    return new ResponseEntity<>(basicResponse, HttpStatus.CONFLICT);
  }

  // @Override
  // protected ResponseEntity<Object> handleMethodArgumentNotValid(
  // MethodArgumentNotValidException ex,
  // HttpHeaders headers,
  // HttpStatusCode status,
  // WebRequest request) {
  // String className = ex.getStackTrace()[0].getClassName();
  // String methodName = ex.getStackTrace()[0].getMethodName();
  // setLogs(EventLog.builder().service(className).method(methodName), REQUEST,
  // HANDLE_ERROR);
  // Map<String, String> errors = new HashMap<>();
  //
  // // 2. Iterar sobre todos los errores de campo (FieldError)
  // String errorMessage = "";
  // ex.getBindingResult()
  // .getAllErrors()
  // .forEach(
  // error -> {
  // String fieldName = ((FieldError) error).getField();
  //
  // // 3. ¡Aquí obtienes el default message!
  // errorMessage = error.getDefaultMessage() +"\n";
  //
  //// errors.put(fieldName, errorMessage);
  // });
  // BasicResponse basicResponse =
  // BasicResponse.builder()
  // .code(HttpStatus.BAD_REQUEST.value())
  // .status(HttpStatus.BAD_REQUEST.getReasonPhrase())
  // .message(errorMessage)
  // .build();
  // setLogs(
  // EventLog.builder().service(className).method(methodName).information(ex.getMessage()),
  // RESPONSE,
  // HANDLE_ERROR);
  // return new ResponseEntity<>(basicResponse, HttpStatus.BAD_REQUEST);
  // }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      @NonNull MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (msg1, msg2) -> msg1 + "; " + msg2));

    String messages = fieldErrors.values().stream().collect(Collectors.joining("\n"));
    ValidationErrorResponse er =
        ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .message(messages)
            .errors(fieldErrors)
            .build();

    log.warn("Errores de validación: {}", fieldErrors);
    return new ResponseEntity<>(er, HttpStatus.BAD_REQUEST);
  }
}
