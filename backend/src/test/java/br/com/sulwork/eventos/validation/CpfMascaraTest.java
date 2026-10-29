package br.com.sulwork.eventos.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfMascaraTest {

	@Test
	void mostraSoOsSeisDigitosDoMeio() {
		assertThat(CpfMascara.mascarar("39053344705")).isEqualTo("***.533.447-**");
		assertThat(CpfMascara.mascarar("52998224725")).isEqualTo("***.982.247-**");
	}

}
