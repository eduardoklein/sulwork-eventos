package br.com.sulwork.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.UncommittedTransaction;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.repository.EventRepository;

/** Without {@code @Transactional}: another enrollment creates the same new person at the same time. */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentConcurrencyTest {

	private static final String CPF = "39053344705";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private EventRepository events;

	@AfterEach
	void cleanDatabase() {
		for (String table : List.of("event_participants", "collaborator_restrictions", "collaborators", "events")) {
			jdbc.update("DELETE FROM " + table);
		}
	}

	@Test
	void enrollingANewCpfThatAnotherRequestIsCreatingReusesThePersonInsteadOfFailing() throws Exception {
		Event event = openEvent();
		Thread other = UncommittedTransaction.hold(transactionManager,
				() -> jdbc.update("INSERT INTO collaborators (name, cpf) VALUES ('Ana Souza', ?)", CPF));

		mvc.perform(post("/api/events/" + event.getId() + "/participants").contentType(MediaType.APPLICATION_JSON)
						.content("{\"cpf\":\"" + CPF + "\",\"name\":\"Ana\",\"restrictions\":[\"VEGAN\"]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Ana Souza"))
				.andExpect(jsonPath("$.restrictions").isEmpty());
		other.join();

		assertThat(jdbc.queryForObject("SELECT count(*) FROM collaborators WHERE cpf = ?", Integer.class, CPF))
				.isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM event_participants", Integer.class)).isEqualTo(1);
	}

	@Test
	void enrollingThePersonThatAnotherRequestIsEnrollingInTheSameEventSaysSheIsAlreadyEnrolled() throws Exception {
		Event event = openEvent();
		Long collaborator = jdbc.queryForObject(
				"INSERT INTO collaborators (name, cpf) VALUES ('Ana Souza', ?) RETURNING id", Long.class, CPF);
		Thread other = UncommittedTransaction.hold(transactionManager, () -> jdbc.update(
				"INSERT INTO event_participants (event_id, collaborator_id) VALUES (?, ?)", event.getId(), collaborator));

		mvc.perform(post("/api/events/" + event.getId() + "/participants").contentType(MediaType.APPLICATION_JSON)
						.content("{\"cpf\":\"" + CPF + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Esta pessoa já está inscrita neste evento."));
		other.join();

		assertThat(jdbc.queryForObject("SELECT count(*) FROM event_participants", Integer.class)).isEqualTo(1);
	}

	private Event openEvent() {
		LocalDateTime date = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, date, date.minusDays(1), ZoneId.of("America/Sao_Paulo"));
		event.setStatus(EventStatus.ABERTO);
		return events.save(event);
	}

}
