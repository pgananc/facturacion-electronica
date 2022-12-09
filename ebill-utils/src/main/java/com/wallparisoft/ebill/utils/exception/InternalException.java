package com.wallparisoft.ebill.utils.exception;
/**
 * Class for exception model.
 * @author PABI1
 *
 */
public class InternalException extends RuntimeException {

	public InternalException(String message) {
		super(message);
	}

	public InternalException(String message, Throwable cause) {
		super(message, cause);
	}
}
