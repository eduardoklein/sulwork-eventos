package br.com.sulwork.eventos.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fixa o relógio em 12:00 de São Paulo, 1º de novembro de 2026. */
@TestConfiguration
class RelogioFixoConfig {

	static final Instant AGORA = Instant.parse("2026-11-01T15:00:00Z");

	@Bean
	@Primary
	Clock clockFixo() {
		return Clock.fixed(AGORA, ZoneOffset.UTC);
	}

}
