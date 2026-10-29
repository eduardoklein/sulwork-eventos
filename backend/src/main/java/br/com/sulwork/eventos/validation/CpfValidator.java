package br.com.sulwork.eventos.validation;

import java.util.regex.Pattern;

import br.com.sulwork.eventos.exception.BusinessRuleException;

public final class CpfValidator {

	private static final Pattern FORMATO = Pattern.compile("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");

	private CpfValidator() {
	}

	/** Aceita 11 dígitos ou o formato completo com máscara, e confere os dígitos verificadores. */
	public static boolean isValid(String cpf) {
		if (cpf == null || !FORMATO.matcher(cpf.strip()).matches()) {
			return false;
		}
		String digitos = soDigitos(cpf);
		if (digitos.chars().distinct().count() == 1) {
			return false;
		}
		return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
				&& digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
	}

	/** Devolve só os 11 dígitos, que é como o CPF é guardado. */
	public static String normalizeAndValidate(String cpf) {
		if (!isValid(cpf)) {
			throw new BusinessRuleException("CPF inválido.");
		}
		return soDigitos(cpf);
	}

	private static String soDigitos(String cpf) {
		return cpf.replaceAll("\\D", "");
	}

	/** Calcula o dígito que vem depois das primeiras {@code quantidade} posições (módulo 11). */
	private static int digitoVerificador(String digitos, int quantidade) {
		int soma = 0;
		for (int i = 0; i < quantidade; i++) {
			soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
		}
		int resto = (soma * 10) % 11;
		return resto == 10 ? 0 : resto;
	}

}
