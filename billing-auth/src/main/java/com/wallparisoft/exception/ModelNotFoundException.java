package com.wallparisoft.exception;
/**
 * Class for exception model.
 * @author PABI1
 *
 */
public class ModelNotFoundException extends RuntimeException {

	public ModelNotFoundException(String mensaje) {
		super(mensaje);
	}
}
