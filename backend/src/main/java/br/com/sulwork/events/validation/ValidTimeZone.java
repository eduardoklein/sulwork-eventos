package br.com.sulwork.events.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The text, when given, must be the name of a time zone (e.g. America/Sao_Paulo). */
@Documented
@Constraint(validatedBy = ValidTimeZoneValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimeZone {

	String message() default "fuso horário inválido";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
