package br.com.sulwork.eventos.validation;

public final class CpfMascara {

	private CpfMascara() {
	}

	/** Mostra só os seis dígitos do meio: 39053344705 vira ***.533.447-**. */
	public static String mascarar(String cpfComOnzeDigitos) {
		return "***." + cpfComOnzeDigitos.substring(3, 6) + "." + cpfComOnzeDigitos.substring(6, 9) + "-**";
	}

}
