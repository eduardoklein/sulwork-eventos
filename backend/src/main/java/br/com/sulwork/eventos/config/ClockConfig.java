package br.com.sulwork.eventos.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	/** O relógio guarda o instante real. O fuso de cada evento converte o prazo para esse instante. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
