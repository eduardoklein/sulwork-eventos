package br.com.sulwork.eventos.exception;

/** Entrada bem formada que quebra uma regra de negócio. */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
