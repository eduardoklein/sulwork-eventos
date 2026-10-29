package br.com.sulwork.eventos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EventoTest {

	private static final LocalDateTime PRAZO = LocalDateTime.of(2026, 11, 30, 12, 0);

	@Test
	void eventoNovoComecaEmRascunho() {
		assertThat(novoEvento().getStatus()).isEqualTo(StatusEvento.RASCUNHO);
	}

	@Test
	void prazoNaoExpiraAntesDoInstanteMarcado() {
		assertThat(novoEvento().isPrazoExpirado(PRAZO.minusSeconds(1))).isFalse();
	}

	@Test
	void prazoAindaVaiNoInstanteExatoDoPrazo() {
		assertThat(novoEvento().isPrazoExpirado(PRAZO)).isFalse();
	}

	@Test
	void prazoExpiraUmSegundoDepois() {
		assertThat(novoEvento().isPrazoExpirado(PRAZO.plusSeconds(1))).isTrue();
	}

	@Test
	void eventoAbertoNoPrazoAceitaContribuicoes() {
		Evento evento = novoEvento();
		evento.setStatus(StatusEvento.ABERTO);

		assertThat(evento.isAbertoParaContribuicoes(PRAZO.minusHours(1))).isTrue();
	}

	@Test
	void eventoAbertoComPrazoVencidoNaoAceitaContribuicoes() {
		Evento evento = novoEvento();
		evento.setStatus(StatusEvento.ABERTO);

		assertThat(evento.isAbertoParaContribuicoes(PRAZO.plusMinutes(1))).isFalse();
	}

	@ParameterizedTest
	@EnumSource(value = StatusEvento.class, names = "ABERTO", mode = EnumSource.Mode.EXCLUDE)
	void statusDiferenteDeAbertoNuncaAceitaContribuicoesMesmoNoPrazo(StatusEvento status) {
		Evento evento = novoEvento();
		evento.setStatus(status);

		assertThat(evento.isAbertoParaContribuicoes(PRAZO.minusHours(1))).isFalse();
	}

	private Evento novoEvento() {
		return new Evento("Almoço de fim de ano", null, TipoEvento.ALMOCO,
				LocalDateTime.of(2026, 12, 1, 12, 0), PRAZO);
	}

}
