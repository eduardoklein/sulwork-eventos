package br.com.sulwork.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.repository.EventRepository;
import br.com.sulwork.events.service.EventDeadlineScheduler;

/** The clock stays at 12:00 in Sao Paulo, November 1st, 2026 (15:00 UTC). */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventDeadlineApiTest {

	private static final String EXPIRED_DEADLINE = "2026-10-31T12:00:00";
	private static final String FUTURE_DEADLINE = "2026-11-20T12:00:00";
	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EventDeadlineScheduler scheduler;

	@Autowired
	private ScheduledAnnotationBeanPostProcessor schedules;

	@Test
	void readingOpenEventWithExpiredDeadlineShowsClosedAndRejectsContributions() throws Exception {
		Event event = saveEvent("Vencido", EventStatus.ABERTO, EXPIRED_DEADLINE, SAO_PAULO);

		mvc.perform(get("/api/events/" + event.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENCERRADO"))
				.andExpect(jsonPath("$.acceptsContributions").value(false));
	}

	@Test
	void readingOpenEventWithinDeadlineKeepsOpen() throws Exception {
		Event event = saveEvent("No prazo", EventStatus.ABERTO, FUTURE_DEADLINE, SAO_PAULO);

		mvc.perform(get("/api/events/" + event.getId()))
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.acceptsContributions").value(true));
	}

	@Test
	void draftWithExpiredDeadlineStaysDraft() throws Exception {
		Event event = saveEvent("Rascunho", EventStatus.RASCUNHO, EXPIRED_DEADLINE, SAO_PAULO);

		mvc.perform(get("/api/events/" + event.getId())).andExpect(jsonPath("$.status").value("RASCUNHO"));
	}

	@Test
	void listingClosesExpiredBeforeResponding() throws Exception {
		saveEvent("A vencido", EventStatus.ABERTO, EXPIRED_DEADLINE, SAO_PAULO);
		saveEvent("B no prazo", EventStatus.ABERTO, FUTURE_DEADLINE, SAO_PAULO);

		mvc.perform(get("/api/events").param("status", "ENCERRADO"))
				.andExpect(jsonPath("$[*].name", contains("A vencido")));
		mvc.perform(get("/api/events").param("status", "ABERTO"))
				.andExpect(jsonPath("$[*].name", contains("B no prazo")));
	}

	@Test
	void sameWallClockTimeExpiresAtDifferentInstantsPerTimeZone() throws Exception {
		// 12:00 on 11/01: in Lisbon it was at 12:00 UTC (already passed); in Sao Paulo it is 15:00 UTC (exact instant, still holds).
		Event lisbon = saveEvent("Lisboa", EventStatus.ABERTO, "2026-11-01T12:00:00", LISBON);
		Event saoPaulo = saveEvent("São Paulo", EventStatus.ABERTO, "2026-11-01T12:00:00", SAO_PAULO);

		mvc.perform(get("/api/events/" + lisbon.getId())).andExpect(jsonPath("$.status").value("ENCERRADO"));
		mvc.perform(get("/api/events/" + saoPaulo.getId())).andExpect(jsonPath("$.status").value("ABERTO"));
	}

	@Test
	void editingExpiredEventFollowsClosedRules() throws Exception {
		Event event = saveEvent("Vencido", EventStatus.ABERTO, EXPIRED_DEADLINE, SAO_PAULO);
		String body = "{\"name\":\"Novo nome\",\"type\":\"SNACK\",\"dateTime\":\"2026-12-01T12:00:00\","
				+ "\"changeDeadline\":\"" + EXPIRED_DEADLINE + "\"}";

		mvc.perform(put("/api/events/" + event.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Novo nome"))
				.andExpect(jsonPath("$.status").value("ENCERRADO"));
	}

	@Test
	void cancellingExpiredEventWorks() throws Exception {
		Event event = saveEvent("Vencido", EventStatus.ABERTO, EXPIRED_DEADLINE, SAO_PAULO);

		mvc.perform(patch("/api/events/" + event.getId() + "/status").contentType(MediaType.APPLICATION_JSON)
						.content("{\"newStatus\":\"CANCELADO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));
	}

	@Test
	void schedulerClosesOnlyExpiredOpenEventsAndReportsCount() {
		Event expired1 = saveEvent("Vencido 1", EventStatus.ABERTO, EXPIRED_DEADLINE, SAO_PAULO);
		Event expired2 = saveEvent("Vencido 2", EventStatus.ABERTO, "2026-11-01T11:59:59", SAO_PAULO);
		Event onTime = saveEvent("No prazo", EventStatus.ABERTO, FUTURE_DEADLINE, SAO_PAULO);
		Event draft = saveEvent("Rascunho", EventStatus.RASCUNHO, EXPIRED_DEADLINE, SAO_PAULO);

		scheduler.closeExpiredEvents();
		events.flush();

		assertThat(statusOf(expired1)).isEqualTo(EventStatus.ENCERRADO);
		assertThat(statusOf(expired2)).isEqualTo(EventStatus.ENCERRADO);
		assertThat(statusOf(onTime)).isEqualTo(EventStatus.ABERTO);
		assertThat(statusOf(draft)).isEqualTo(EventStatus.RASCUNHO);
	}

	@Test
	void schedulerWithoutExpiredEventsChangesNothing() {
		Event onTime = saveEvent("No prazo", EventStatus.ABERTO, FUTURE_DEADLINE, SAO_PAULO);

		scheduler.closeExpiredEvents();

		assertThat(statusOf(onTime)).isEqualTo(EventStatus.ABERTO);
	}

	@Test
	void schedulingIsRegisteredInContext() {
		assertThat(schedules.getScheduledTasks()).hasSize(1);
	}

	private EventStatus statusOf(Event event) {
		return events.findById(event.getId()).orElseThrow().getStatus();
	}

	private Event saveEvent(String name, EventStatus status, String deadline, ZoneId timeZone) {
		Event event = new Event(name, null, EventType.SNACK, LocalDateTime.parse("2026-12-01T12:00:00"),
				LocalDateTime.parse(deadline), timeZone);
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

}
