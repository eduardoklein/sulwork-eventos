package br.com.sulwork.eventos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EventoTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final ZoneId LISBOA = ZoneId.of("Europe/Lisbon");
	private static final LocalDateTime PRAZO = LocalDateTime.of(2026, 11, 30, 12, 0);

	/** 12:00 em São Paulo (UTC-3) é 15:00 UTC. */
	private static final Instant PRAZO_EM_SAO_PAULO = Instant.parse("2026-11-30T15:00:00Z");

	@Test
	void eventoNovoComecaEmRascunho() {
		assertThat(novoEvento(SAO_PAULO).getStatus()).isEqualTo(StatusEvento.RASCUNHO);
	}

	@Test
	void prazoNaoExpiraAntesDoInstanteMarcado() {
		assertThat(novoEvento(SAO_PAULO).isPrazoExpirado(PRAZO_EM_SAO_PAULO.minusSeconds(1))).isFalse();
	}

	@Test
	void prazoAindaVaiNoInstanteExatoDoPrazo() {
		assertThat(novoEvento(SAO_PAULO).isPrazoExpirado(PRAZO_EM_SAO_PAULO)).isFalse();
	}

	@Test
	void prazoExpiraUmSegundoDepois() {
		assertThat(novoEvento(SAO_PAULO).isPrazoExpirado(PRAZO_EM_SAO_PAULO.plusSeconds(1))).isTrue();
	}

	@Test
	void mesmoHorarioDeParedeVenceEmInstantesDiferentesEmFusosDiferentes() {
		Instant meioDiaEmUtc = Instant.parse("2026-11-30T12:00:00Z");

		assertThat(novoEvento(LISBOA).isPrazoExpirado(meioDiaEmUtc.plusSeconds(1))).isTrue();
		assertThat(novoEvento(SAO_PAULO).isPrazoExpirado(meioDiaEmUtc.plusSeconds(1))).isFalse();
	}

	@Test
	void servidorEmUtcNaoAntecipaOPrazoDeUmEventoEmSaoPaulo() {
		Instant meioDiaEmUtc = Instant.parse("2026-11-30T12:00:01Z");

		assertThat(novoEvento(SAO_PAULO).isPrazoExpirado(meioDiaEmUtc)).isFalse();
	}

	@Test
	void eventoAbertoNoPrazoAceitaContribuicoes() {
		Evento evento = novoEvento(SAO_PAULO);
		evento.setStatus(StatusEvento.ABERTO);

		assertThat(evento.isAbertoParaContribuicoes(PRAZO_EM_SAO_PAULO.minusSeconds(3600))).isTrue();
	}

	@Test
	void eventoAbertoComPrazoVencidoNaoAceitaContribuicoes() {
		Evento evento = novoEvento(SAO_PAULO);
		evento.setStatus(StatusEvento.ABERTO);

		assertThat(evento.isAbertoParaContribuicoes(PRAZO_EM_SAO_PAULO.plusSeconds(60))).isFalse();
	}

	@ParameterizedTest
	@EnumSource(value = StatusEvento.class, names = "ABERTO", mode = EnumSource.Mode.EXCLUDE)
	void statusDiferenteDeAbertoNuncaAceitaContribuicoesMesmoNoPrazo(StatusEvento status) {
		Evento evento = novoEvento(SAO_PAULO);
		evento.setStatus(status);

		assertThat(evento.isAbertoParaContribuicoes(PRAZO_EM_SAO_PAULO.minusSeconds(3600))).isFalse();
	}

	private Evento novoEvento(ZoneId fuso) {
		return new Evento("Almoço de fim de ano", null, TipoEvento.ALMOCO,
				LocalDateTime.of(2026, 12, 1, 12, 0), PRAZO, fuso);
	}

}
