package com.wallparisoft.ebill.customer.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Class exception response.
 * 
 * @author PABI1
 *
 */
@Data
@Builder
public class ExceptionResponse {

	private LocalDateTime timestamp;
	private String message;
	private String details;

}
