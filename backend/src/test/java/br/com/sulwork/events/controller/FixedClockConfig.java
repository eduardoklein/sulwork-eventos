package br.com.sulwork.events.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fixes the clock at 12:00 in Sao Paulo, November 1st, 2026. */
@TestConfiguration
class FixedClockConfig {

	static final Instant NOW = Instant.parse("2026-11-01T15:00:00Z");

	@Bean
	@Primary
	Clock fixedClock() {
		return Clock.fixed(NOW, ZoneOffset.UTC);
	}

}
