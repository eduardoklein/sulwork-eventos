package br.com.sulwork.eventos.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** O texto, quando informado, precisa ser o nome de um fuso horário (ex.: America/Sao_Paulo). */
@Documented
@Constraint(validatedBy = FusoValidoValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FusoValido {

	String message() default "fuso horário inválido";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
