package br.com.sulwork.events.exception;

/** The requested resource does not exist. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

}
