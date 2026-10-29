package br.com.sulwork.eventos.controller;

/** Gera CPFs com dígitos verificadores corretos, para os testes precisarem de muitas pessoas diferentes. */
final class CpfsDeTeste {

	private CpfsDeTeste() {
	}

	static String cpfValido(int semente) {
		String base = String.format("%09d", 100_000_000 + semente * 7);
		String comPrimeiro = base + digito(base);
		return comPrimeiro + digito(comPrimeiro);
	}

	private static int digito(String digitos) {
		int soma = 0;
		for (int i = 0; i < digitos.length(); i++) {
			soma += (digitos.charAt(i) - '0') * (digitos.length() + 1 - i);
		}
		int resto = (soma * 10) % 11;
		return resto == 10 ? 0 : resto;
	}

}
