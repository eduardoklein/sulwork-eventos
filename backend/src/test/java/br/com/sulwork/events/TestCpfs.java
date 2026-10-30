package br.com.sulwork.events;

/** Generates CPFs with correct check digits, so tests can use many different people. */
public final class TestCpfs {

	private TestCpfs() {
	}

	public static String validCpf(int seed) {
		String base = String.format("%09d", 100_000_000 + seed * 7);
		String withFirstDigit = base + digit(base);
		return withFirstDigit + digit(withFirstDigit);
	}

	private static int digit(String digits) {
		int sum = 0;
		for (int i = 0; i < digits.length(); i++) {
			sum += (digits.charAt(i) - '0') * (digits.length() + 1 - i);
		}
		int remainder = (sum * 10) % 11;
		return remainder == 10 ? 0 : remainder;
	}

}
