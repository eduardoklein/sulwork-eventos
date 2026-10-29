package br.com.sulwork.eventos.validation;

import java.time.ZoneId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FusoValidoValidator implements ConstraintValidator<FusoValido, String> {

	@Override
	public boolean isValid(String fuso, ConstraintValidatorContext context) {
		return fuso == null || fuso.isBlank() || ZoneId.getAvailableZoneIds().contains(fuso.strip());
	}

}
