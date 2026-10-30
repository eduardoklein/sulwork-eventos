package br.com.sulwork.events.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.repository.EventRepository;

/**
 * Without {@code @Transactional}: two real transactions interleave. The first one reads an expired open event and
 * closes it; while it is still open, a second one edits the name and commits.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EventStatusSyncConcurrencyTest {

	@Autowired
	private EventService eventService;

	@Autowired
	private EventRepository events;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@AfterEach
	void cleanDatabase() {
		jdbc.update("DELETE FROM events");
	}

	@Test
	void closingAnExpiredEventDoesNotUndoAnEditCommittedInTheMeantime() {
		Event event = new Event("Nome original", null, EventType.LUNCH, LocalDateTime.parse("2020-01-02T12:00:00"),
				LocalDateTime.parse("2020-01-01T12:00:00"), ZoneId.of("America/Sao_Paulo"));
		event.setStatus(EventStatus.ABERTO);
		Long id = events.save(event).getId();
		TransactionTemplate sync = new TransactionTemplate(transactionManager);
		TransactionTemplate edit = new TransactionTemplate(transactionManager);
		edit.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		sync.executeWithoutResult(outer -> {
			eventService.findSyncedEntity(id);
			edit.executeWithoutResult(inner -> jdbc.update("UPDATE events SET name = 'Nome editado' WHERE id = ?", id));
		});

		assertThat(jdbc.queryForObject("SELECT name FROM events WHERE id = ?", String.class, id))
				.isEqualTo("Nome editado");
		assertThat(jdbc.queryForObject("SELECT status FROM events WHERE id = ?", String.class, id))
				.isEqualTo("ENCERRADO");
	}

}
