package com.wallparisoft.ebill.customer.exception;

import com.wallparisoft.ebill.customer.response.BasicResponse;
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
		BasicResponse basicResponse= BasicResponse.builder()
				.code(HttpStatus.BAD_REQUEST.value())
				.status(HttpStatus.BAD_REQUEST.getReasonPhrase())
				.message(ex.getMessage()).build();
		return new ResponseEntity<Object>(basicResponse, HttpStatus.BAD_REQUEST);
	}

}
