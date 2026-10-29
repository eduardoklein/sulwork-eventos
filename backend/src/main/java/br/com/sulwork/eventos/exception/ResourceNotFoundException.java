package br.com.sulwork.eventos.exception;

/** O recurso pedido não existe. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

}
