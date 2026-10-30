package br.com.sulwork.events.validation;

public final class CpfMask {

	private CpfMask() {
	}

	/** Shows only the six middle digits: 39053344705 becomes ***.533.447-**. */
	public static String mask(String cpfWithElevenDigits) {
		return "***." + cpfWithElevenDigits.substring(3, 6) + "." + cpfWithElevenDigits.substring(6, 9) + "-**";
	}

}
