package br.com.sulwork.eventos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DescricaoDosEnumsTest {

	@Test
	void todoValorTemDescricaoLegivel() {
		for (TipoEvento valor : TipoEvento.values()) {
			assertThat(valor.getDescricao()).isNotBlank();
		}
		for (UnidadeMedida valor : UnidadeMedida.values()) {
			assertThat(valor.getDescricao()).isNotBlank();
		}
		for (Alergeno valor : Alergeno.values()) {
			assertThat(valor.getDescricao()).isNotBlank();
		}
		for (CaracteristicaAlimentar valor : CaracteristicaAlimentar.values()) {
			assertThat(valor.getDescricao()).isNotBlank();
		}
		for (RestricaoAlimentar valor : RestricaoAlimentar.values()) {
			assertThat(valor.getDescricao()).isNotBlank();
		}
	}

	@Test
	void exemplosDeDescricao() {
		assertThat(TipoEvento.CAFE_DA_MANHA.getDescricao()).isEqualTo("Café da Manhã");
		assertThat(CaracteristicaAlimentar.ALTO_SODIO.getDescricao()).isEqualTo("Rico em sal");
	}

}
