package br.com.sulwork.events.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.sulwork.events.TestcontainersConfiguration;

/** Without {@code app.demo-data}, the demo migration never runs and the database starts empty. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NoDemoDataByDefaultInTestsTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void theDatabaseStartsEmptyAndTheDemoMigrationIsNotApplied() {
		assertThat(jdbc.queryForObject("SELECT count(*) FROM events", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM collaborators", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '1000'",
				Integer.class)).isZero();
	}

}
