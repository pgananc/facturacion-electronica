package com.wallparisoft.ebill.product.exception;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.HANDLE_ERROR;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
  public final ResponseEntity<com.wallparisoft.ebill.utils.response.BasicResponse>
      handleAllException(Exception ex, WebRequest request) {
    com.wallparisoft.ebill.utils.response.BasicResponse basicResponse =
        com.wallparisoft.ebill.utils.response.BasicResponse.builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message(ex.getMessage())
            .build();
    return new ResponseEntity<>(basicResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(com.wallparisoft.ebill.utils.exception.ModelNotFoundException.class)
  public final ResponseEntity<com.wallparisoft.ebill.utils.response.BasicResponse>
      handleModelNotFoundException(ModelNotFoundException ex, WebRequest request) {
    com.wallparisoft.ebill.utils.response.BasicResponse basicResponse =
        com.wallparisoft.ebill.utils.response.BasicResponse.builder()
            .code(HttpStatus.NOT_FOUND.value())
            .status(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .build();
    return new ResponseEntity<>(basicResponse, HttpStatus.NOT_FOUND);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String className = ex.getStackTrace()[0].getClassName();
    String methodName = ex.getStackTrace()[0].getMethodName();
    setLogs(EventLog.builder().service(className).method(methodName), REQUEST, HANDLE_ERROR);
    com.wallparisoft.ebill.utils.response.BasicResponse basicResponse =
        BasicResponse.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .status(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(ex.getMessage())
            .build();
    setLogs(
        EventLog.builder().service(className).method(methodName).information(ex.getMessage()),
        RESPONSE,
        HANDLE_ERROR);
    return new ResponseEntity<>(basicResponse, HttpStatus.BAD_REQUEST);
  }
}
