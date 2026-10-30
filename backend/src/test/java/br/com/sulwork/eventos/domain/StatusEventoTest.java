package br.com.sulwork.eventos.domain;

import static br.com.sulwork.eventos.domain.StatusEvento.ABERTO;
import static br.com.sulwork.eventos.domain.StatusEvento.CANCELADO;
import static br.com.sulwork.eventos.domain.StatusEvento.ENCERRADO;
import static br.com.sulwork.eventos.domain.StatusEvento.RASCUNHO;
import static br.com.sulwork.eventos.domain.StatusEvento.REALIZADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StatusEventoTest {

	private static final Map<StatusEvento, Set<StatusEvento>> TRANSICOES_VALIDAS = Map.of(
			RASCUNHO, EnumSet.of(ABERTO, CANCELADO),
			ABERTO, EnumSet.of(ENCERRADO, CANCELADO),
			ENCERRADO, EnumSet.of(REALIZADO, CANCELADO),
			REALIZADO, EnumSet.noneOf(StatusEvento.class),
			CANCELADO, EnumSet.noneOf(StatusEvento.class));

	@ParameterizedTest
	@EnumSource(StatusEvento.class)
	void transicoesPermitidasSaoExatamenteAsDaTabela(StatusEvento origem) {
		assertThat(origem.transicoesPermitidas()).containsExactlyInAnyOrderElementsOf(TRANSICOES_VALIDAS.get(origem));
	}

	@ParameterizedTest
	@EnumSource(StatusEvento.class)
	void podeIrParaConcordaComATabelaParaTodosOsPares(StatusEvento origem) {
		for (StatusEvento destino : StatusEvento.values()) {
			assertThat(origem.podeIrPara(destino))
					.as("%s -> %s", origem, destino)
					.isEqualTo(TRANSICOES_VALIDAS.get(origem).contains(destino));
		}
	}

	@Test
	void transicoesInvalidasImportantes() {
		assertThat(REALIZADO.podeIrPara(ABERTO)).isFalse();
		assertThat(RASCUNHO.podeIrPara(REALIZADO)).isFalse();
		assertThat(ENCERRADO.podeIrPara(ABERTO)).isFalse();
		assertThat(CANCELADO.podeIrPara(ABERTO)).isFalse();
		assertThat(ABERTO.podeIrPara(RASCUNHO)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(StatusEvento.class)
	void statusNaoPodeIrParaSiMesmo(StatusEvento status) {
		assertThat(status.podeIrPara(status)).isFalse();
	}

	@Test
	void soRealizadoECanceladoSaoFinais() {
		assertThat(StatusEvento.values()).filteredOn(StatusEvento::isFinal)
				.containsExactlyInAnyOrder(REALIZADO, CANCELADO);
	}

	@Test
	void soAbertoAceitaContribuicoesEInscricoes() {
		for (StatusEvento status : StatusEvento.values()) {
			assertThat(status.aceitaContribuicoes()).as("contribuições em %s", status).isEqualTo(status == ABERTO);
			assertThat(status.permiteAlterarParticipantes()).as("inscrições em %s", status).isEqualTo(status == ABERTO);
		}
	}

	@Test
	void cardapioEditavelSoEmRascunhoEAberto() {
		for (StatusEvento status : StatusEvento.values()) {
			assertThat(status.permiteAlterarCardapio()).as("cardápio em %s", status)
					.isEqualTo(status == RASCUNHO || status == ABERTO);
		}
	}

	@Test
	void conjuntoDeTransicoesNaoPodeSerAlterado() {
		assertThatThrownBy(() -> RASCUNHO.transicoesPermitidas().add(REALIZADO))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
