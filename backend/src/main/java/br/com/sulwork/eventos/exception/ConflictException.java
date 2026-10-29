package br.com.sulwork.eventos.exception;

/** A operação conflita com um dado que já existe. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
