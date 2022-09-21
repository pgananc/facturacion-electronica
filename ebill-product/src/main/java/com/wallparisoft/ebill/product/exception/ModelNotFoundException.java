package com.wallparisoft.ebill.product.exception;
/**
 * Class for exception model.
 * @author PABI1
 *
 */
public class ModelNotFoundException extends RuntimeException {

	public ModelNotFoundException(final String message) {
		super(message);
	}
}
