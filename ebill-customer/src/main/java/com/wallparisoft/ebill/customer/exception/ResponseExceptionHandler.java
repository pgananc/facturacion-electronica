package com.wallparisoft.ebill.customer.exception;

import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.HANDLE_ERROR;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

/**
 * Control advice for exception.
 * 
 * @author PABI1
 *
 */
@ControllerAdvice
@RestController
@FieldDefaults(level = PRIVATE)
@Log4j2
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(Exception.class)
	public final ResponseEntity<BasicResponse> handleAllException(Exception ex,
																  WebRequest request) {
		BasicResponse basicResponse= BasicResponse.builder()
				.code(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.status(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.message(ex.getMessage()).build();
		return new ResponseEntity<BasicResponse>(basicResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ModelNotFoundException.class)
	public final ResponseEntity<BasicResponse> handleModelNotFoundException(ModelNotFoundException ex,
			WebRequest request) {
		BasicResponse basicResponse= BasicResponse.builder()
				.code(HttpStatus.NOT_FOUND.value())
				.status(HttpStatus.NOT_FOUND.getReasonPhrase())
				.message(ex.getMessage()).build();
		return new ResponseEntity<BasicResponse>(basicResponse, HttpStatus.NOT_FOUND);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
		log.debug(EventLog.builder()
				.service(traceElement.getClassName())
				.method(traceElement.getMethodName())
				.eventType(REQUEST.name())
				.level(HANDLE_ERROR.name())
				.build());
		BasicResponse basicResponse= BasicResponse.builder()
				.code(HttpStatus.BAD_REQUEST.value())
				.status(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message(ex.getMessage()).build();
		log.debug(EventLog.builder()
				.service(traceElement.getClassName())
				.method(traceElement.getMethodName())
				.information(ex.getMessage())
				.eventType(RESPONSE.name())
				.level(HANDLE_ERROR.name())
				.build());
		return new ResponseEntity<Object>(basicResponse, HttpStatus.BAD_REQUEST);
	}

}
