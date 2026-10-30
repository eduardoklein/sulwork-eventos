package br.com.sulwork.events.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfMaskTest {

	@Test
	void showsOnlyTheSixMiddleDigits() {
		assertThat(CpfMask.mask("39053344705")).isEqualTo("***.533.447-**");
		assertThat(CpfMask.mask("52998224725")).isEqualTo("***.982.247-**");
	}

}
