package br.com.sulwork.events.exception;

/** The operation conflicts with data that already exists. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
