package br.com.sulwork.events.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds the demo data migrations to Flyway when {@code app.demo-data} is {@code true}. The demo data lives in its own
 * folder so the tests, which need an empty database, can leave it out.
 */
@Configuration
public class DemoDataConfig {

	@Bean
	@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
	FlywayConfigurationCustomizer demoDataMigrations() {
		return configuration -> configuration.locations("classpath:db/migration", "classpath:db/demo");
	}

}
