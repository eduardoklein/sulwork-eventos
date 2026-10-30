package br.com.sulwork.events.exception;

/** Well-formed input that breaks a business rule. */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
