package br.com.sulwork.events.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	/** The clock holds the real instant. Each event time zone converts the deadline to that instant. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
