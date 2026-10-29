package br.com.sulwork.eventos.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.sulwork.eventos.exception.BusinessRuleException;

class CpfValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = { "39053344705", "390.533.447-05", "52998224725", "529.982.247-25", "  390.533.447-05  " })
	void cpfValidoComOuSemMascaraEhAceito(String cpf) {
		assertThat(CpfValidator.isValid(cpf)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "39053344706", "390.533.447-04", "52998224720" })
	void digitoVerificadorErradoEhRecusado(String cpf) {
		assertThat(CpfValidator.isValid(cpf)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "3905334470", "390533447051", "390.533.447-0", "390.533.447-055" })
	void tamanhoErradoEhRecusado(String cpf) {
		assertThat(CpfValidator.isValid(cpf)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "00000000000", "11111111111", "22222222222", "33333333333", "44444444444",
			"55555555555", "66666666666", "77777777777", "88888888888", "99999999999", "111.111.111-11" })
	void digitosRepetidosSaoRecusados(String cpf) {
		assertThat(CpfValidator.isValid(cpf)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc390.533.447-05xyz", "39053344705a", "390.533.447-0x", "390 533 447 05",
			"390-533-447.05", "390.53344705", "390533.447-05" })
	void letrasEFormatosMistosSaoRecusados(String cpf) {
		assertThat(CpfValidator.isValid(cpf)).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "   " })
	void nuloVazioEEmBrancoSaoRecusados(String cpf) {
		assertThat(CpfValidator.isValid(cpf)).isFalse();
	}

	@Test
	void normalizeAndValidateDevolveSoOsOnzeDigitos() {
		assertThat(CpfValidator.normalizeAndValidate("390.533.447-05")).isEqualTo("39053344705");
		assertThat(CpfValidator.normalizeAndValidate("39053344705")).isEqualTo("39053344705");
		assertThat(CpfValidator.normalizeAndValidate("  390.533.447-05 ")).isEqualTo("39053344705");
	}

	@Test
	void normalizeAndValidateLancaErroDeRegraDeNegocioParaCpfInvalido() {
		assertThatThrownBy(() -> CpfValidator.normalizeAndValidate("111.111.111-11"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("CPF inválido.");
		assertThatThrownBy(() -> CpfValidator.normalizeAndValidate(null))
				.isInstanceOf(BusinessRuleException.class);
	}

}
