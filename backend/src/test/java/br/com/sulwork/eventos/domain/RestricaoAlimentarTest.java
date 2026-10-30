package br.com.sulwork.eventos.domain;

import static br.com.sulwork.eventos.domain.Alergeno.FRUTOS_DO_MAR;
import static br.com.sulwork.eventos.domain.Alergeno.GLUTEN;
import static br.com.sulwork.eventos.domain.Alergeno.LACTOSE;
import static br.com.sulwork.eventos.domain.Alergeno.OVO;
import static br.com.sulwork.eventos.domain.Alergeno.PEIXE;
import static br.com.sulwork.eventos.domain.CaracteristicaAlimentar.ACUCAR_ADICIONADO;
import static br.com.sulwork.eventos.domain.CaracteristicaAlimentar.ALTO_SODIO;
import static br.com.sulwork.eventos.domain.CaracteristicaAlimentar.CARNE_ORIGEM_ANIMAL;
import static br.com.sulwork.eventos.domain.RestricaoAlimentar.ALERGIA_FRUTOS_DO_MAR;
import static br.com.sulwork.eventos.domain.RestricaoAlimentar.ALERGIA_PEIXE;
import static br.com.sulwork.eventos.domain.RestricaoAlimentar.CELIACO_SEM_GLUTEN;
import static br.com.sulwork.eventos.domain.RestricaoAlimentar.DIABETICO_SEM_ACUCAR;
import static br.com.sulwork.eventos.domain.RestricaoAlimentar.HIPERTENSO_POUCO_SODIO;
import static br.com.sulwork.eventos.domain.RestricaoAlimentar.VEGANO;
import static br.com.sulwork.eventos.domain.RestricaoAlimentar.VEGETARIANO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RestricaoAlimentarTest {

	private static final Set<Alergeno> SEM_ALERGENOS = Set.of();
	private static final Set<CaracteristicaAlimentar> SEM_CARACTERISTICAS = Set.of();

	@Test
	void itemComGlutenELactoseConflitaComVeganoSoEmLactose() {
		ConflitoAlimentar conflito = VEGANO.encontrarConflitos(Set.of(GLUTEN, LACTOSE), SEM_CARACTERISTICAS);

		assertThat(conflito.alergenos()).containsExactly(LACTOSE);
		assertThat(conflito.caracteristicas()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(RestricaoAlimentar.class)
	void itemSemAlergenosNemCaracteristicasNaoConflitaComNenhumaRestricao(RestricaoAlimentar restricao) {
		assertThat(restricao.encontrarConflitos(SEM_ALERGENOS, SEM_CARACTERISTICAS).isVazio()).isTrue();
	}

	@Test
	void celiacoConflitaComGlutenEMaisNada() {
		ConflitoAlimentar conflito = CELIACO_SEM_GLUTEN.encontrarConflitos(Set.of(GLUTEN, OVO), SEM_CARACTERISTICAS);

		assertThat(conflito.alergenos()).containsExactly(GLUTEN);
	}

	@Test
	void vegetarianoAceitaOvoELaticinios() {
		ConflitoAlimentar conflito = VEGETARIANO.encontrarConflitos(Set.of(OVO, LACTOSE), SEM_CARACTERISTICAS);

		assertThat(conflito.isVazio()).isTrue();
	}

	@Test
	void vegetarianoEveganoRecusamPeixeEFrutosDoMar() {
		Set<Alergeno> item = Set.of(PEIXE, FRUTOS_DO_MAR);

		assertThat(VEGETARIANO.encontrarConflitos(item, SEM_CARACTERISTICAS).alergenos())
				.containsExactlyInAnyOrder(PEIXE, FRUTOS_DO_MAR);
		assertThat(VEGANO.encontrarConflitos(item, SEM_CARACTERISTICAS).alergenos())
				.containsExactlyInAnyOrder(PEIXE, FRUTOS_DO_MAR);
	}

	@Test
	void vegetarianoEveganoRecusamCarneDeOrigemAnimal() {
		Set<CaracteristicaAlimentar> item = Set.of(CARNE_ORIGEM_ANIMAL);

		assertThat(VEGETARIANO.encontrarConflitos(SEM_ALERGENOS, item).caracteristicas())
				.containsExactly(CARNE_ORIGEM_ANIMAL);
		assertThat(VEGANO.encontrarConflitos(SEM_ALERGENOS, item).caracteristicas())
				.containsExactly(CARNE_ORIGEM_ANIMAL);
	}

	@Test
	void conflitoMistoDevolveOsDoisTipos() {
		ConflitoAlimentar conflito = VEGANO.encontrarConflitos(Set.of(LACTOSE, GLUTEN), Set.of(CARNE_ORIGEM_ANIMAL, ALTO_SODIO));

		assertThat(conflito.alergenos()).containsExactly(LACTOSE);
		assertThat(conflito.caracteristicas()).containsExactly(CARNE_ORIGEM_ANIMAL);
		assertThat(conflito.isVazio()).isFalse();
	}

	@Test
	void alergiaAPeixeNaoConflitaComFrutosDoMarENemOContrario() {
		assertThat(ALERGIA_PEIXE.encontrarConflitos(Set.of(FRUTOS_DO_MAR), SEM_CARACTERISTICAS).isVazio()).isTrue();
		assertThat(ALERGIA_FRUTOS_DO_MAR.encontrarConflitos(Set.of(PEIXE), SEM_CARACTERISTICAS).isVazio()).isTrue();
		assertThat(ALERGIA_PEIXE.encontrarConflitos(Set.of(PEIXE), SEM_CARACTERISTICAS).alergenos()).containsExactly(PEIXE);
	}

	@Test
	void diabeticoEHipertensoConflitamComSuaCaracteristica() {
		assertThat(DIABETICO_SEM_ACUCAR.encontrarConflitos(SEM_ALERGENOS, Set.of(ACUCAR_ADICIONADO, ALTO_SODIO)).caracteristicas())
				.containsExactly(ACUCAR_ADICIONADO);
		assertThat(HIPERTENSO_POUCO_SODIO.encontrarConflitos(SEM_ALERGENOS, Set.of(ACUCAR_ADICIONADO, ALTO_SODIO)).caracteristicas())
				.containsExactly(ALTO_SODIO);
	}

	@Test
	void conjuntosDeConflitoNaoPodemSerAlterados() {
		assertThatThrownBy(() -> VEGANO.getAlergenosEmConflito().add(GLUTEN))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> VEGANO.encontrarConflitos(Set.of(LACTOSE), SEM_CARACTERISTICAS).alergenos().add(GLUTEN))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
