package com.wallparisoft.exception;

import java.time.LocalDateTime;

import com.wallparisoft.ebill.utils.exception.InternalException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Control advice for exception.
 * 
 * @author PABI1
 *
 */
@ControllerAdvice
@RestController
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(Exception.class)
	public final ResponseEntity<ExceptionResponse> manageAllException(ModelNotFoundException ex,
																	  WebRequest request) {

		ExceptionResponse er = ExceptionResponse.builder().timestamp(LocalDateTime.now()).message(ex.getMessage())
				.details(request.getDescription(false)).build();
		return new ResponseEntity<ExceptionResponse>(er, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ModelNotFoundException.class)
	public final ResponseEntity<ExceptionResponse> manageModelException(ModelNotFoundException ex,
																		WebRequest request) {
		ExceptionResponse er = ExceptionResponse.builder().timestamp(LocalDateTime.now()).message(ex.getMessage())
				.details(request.getDescription(false)).build();
		return new ResponseEntity<ExceptionResponse>(er, HttpStatus.NOT_FOUND);
	}
	@ExceptionHandler(InternalException.class)
	public final ResponseEntity<ExceptionResponse> manageInternalException(InternalException ex,
																		WebRequest request) {
		ExceptionResponse er = ExceptionResponse.builder().timestamp(LocalDateTime.now()).message(ex.getMessage())
				.details(request.getDescription(false)).build();
		return new ResponseEntity<ExceptionResponse>(er, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		ExceptionResponse er = ExceptionResponse.builder().timestamp(LocalDateTime.now()).message(ex.getMessage())
				.details(request.getDescription(false)).build();
		return new ResponseEntity<Object>(er, HttpStatus.BAD_REQUEST);
	}

}
