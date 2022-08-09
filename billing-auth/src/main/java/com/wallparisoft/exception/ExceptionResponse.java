package com.wallparisoft.exception;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

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
