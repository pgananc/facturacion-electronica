package com.wallparisoft.ebill.customer.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

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
	public final ResponseEntity<ExceptionResponse> manejarTodasExcepciones(ModelNotFoundException ex,
			WebRequest request) {

		ExceptionResponse er = ExceptionResponse.builder().timestamp(LocalDateTime.now()).message(ex.getMessage())
				.details(request.getDescription(false)).build();
		return new ResponseEntity<ExceptionResponse>(er, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ModelNotFoundException.class)
	public final ResponseEntity<ExceptionResponse> manejarModeloException(ModelNotFoundException ex,
			WebRequest request) {
		ExceptionResponse er = ExceptionResponse.builder().timestamp(LocalDateTime.now()).message(ex.getMessage())
				.details(request.getDescription(false)).build();
		return new ResponseEntity<ExceptionResponse>(er, HttpStatus.NOT_FOUND);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		ExceptionResponse er = ExceptionResponse.builder().timestamp(LocalDateTime.now()).message(ex.getMessage())
				.details(request.getDescription(false)).build();
		return new ResponseEntity<Object>(er, HttpStatus.BAD_REQUEST);
	}

}
