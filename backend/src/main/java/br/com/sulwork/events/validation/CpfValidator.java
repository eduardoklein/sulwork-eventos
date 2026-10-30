package br.com.sulwork.events.validation;

import java.util.regex.Pattern;

import br.com.sulwork.events.exception.BusinessRuleException;

public final class CpfValidator {

	private static final Pattern FORMAT = Pattern.compile("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");

	private CpfValidator() {
	}

	/** Accepts 11 digits or the full masked format, and checks the check digits. */
	public static boolean isValid(String cpf) {
		if (cpf == null || !FORMAT.matcher(cpf.strip()).matches()) {
			return false;
		}
		String digits = onlyDigits(cpf);
		if (digits.chars().distinct().count() == 1) {
			return false;
		}
		return checkDigit(digits, 9) == digits.charAt(9) - '0'
				&& checkDigit(digits, 10) == digits.charAt(10) - '0';
	}

	/** Returns only the 11 digits, which is how the CPF is stored. */
	public static String normalizeAndValidate(String cpf) {
		if (!isValid(cpf)) {
			throw new BusinessRuleException("CPF inválido.");
		}
		return onlyDigits(cpf);
	}

	private static String onlyDigits(String cpf) {
		return cpf.replaceAll("\\D", "");
	}

	/** Calculates the digit that comes after the first {@code quantity} positions (modulo 11). */
	private static int checkDigit(String digits, int quantity) {
		int sum = 0;
		for (int i = 0; i < quantity; i++) {
			sum += (digits.charAt(i) - '0') * (quantity + 1 - i);
		}
		int remainder = (sum * 10) % 11;
		return remainder == 10 ? 0 : remainder;
	}

}
