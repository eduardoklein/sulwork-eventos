package br.com.sulwork.events.validation;

import java.time.ZoneId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

	@Override
	public boolean isValid(String timeZone, ConstraintValidatorContext context) {
		return timeZone == null || timeZone.isBlank() || ZoneId.getAvailableZoneIds().contains(timeZone.strip());
	}

}
