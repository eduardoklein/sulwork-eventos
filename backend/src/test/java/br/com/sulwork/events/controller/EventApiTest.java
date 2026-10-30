package br.com.sulwork.events.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.repository.EventRepository;

@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventApiTest {

	private static final String EVENT_DATE = "2026-12-01T12:00:00";
	private static final String FUTURE_DEADLINE = "2026-11-30T12:00:00";
	private static final String EXPIRED_DEADLINE = "2026-10-30T12:00:00";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Test
	void createsDraftEventWithDefaultTimeZoneAndCalculatedFields() throws Exception {
		create(body("Almoço de fim de ano", "LUNCH", EVENT_DATE, FUTURE_DEADLINE, null, null))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/events/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Almoço de fim de ano"))
				.andExpect(jsonPath("$.status").value("RASCUNHO"))
				.andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"))
				.andExpect(jsonPath("$.deadlineExpired").value(false))
				.andExpect(jsonPath("$.acceptsContributions").value(false))
				.andExpect(jsonPath("$.allowedTransitions", contains("ABERTO", "CANCELADO")));
	}

	@Test
	void createsOpenEventWithFutureDeadline() throws Exception {
		create(body("Lanche", "SNACK", EVENT_DATE, FUTURE_DEADLINE, null, "ABERTO"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.acceptsContributions").value(true))
				.andExpect(jsonPath("$.allowedTransitions", contains("ENCERRADO", "CANCELADO")));
	}

	@Test
	void draftWithExpiredDeadlineShowsDeadlineExpiredAndRejectsContributions() throws Exception {
		create(body("Café", "BREAKFAST", EVENT_DATE, EXPIRED_DEADLINE, null, null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.deadlineExpired").value(true))
				.andExpect(jsonPath("$.acceptsContributions").value(false));
	}

	@Test
	void createsEventWithGivenTimeZone() throws Exception {
		create(body("Almoço em Lisboa", "LUNCH", EVENT_DATE, FUTURE_DEADLINE, "Europe/Lisbon", null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.timeZone").value("Europe/Lisbon"));
	}

	@Test
	void doesNotCreateOpenEventWithExpiredDeadline() throws Exception {
		create(body("Lanche", "SNACK", EVENT_DATE, EXPIRED_DEADLINE, null, "ABERTO"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento aberto precisa de um prazo que ainda não passou."));
	}

	@Test
	void deadlineInEventTimeZoneHoldsAtRightTime() throws Exception {
		// 12:00 in Lisbon on 11/01 has already passed (now it is 15:00 UTC); 12:00 in Sao Paulo has not.
		create(body("Lisboa", "SNACK", EVENT_DATE, "2026-11-01T12:00:00", "Europe/Lisbon", "ABERTO"))
				.andExpect(status().isUnprocessableEntity());
		create(body("São Paulo", "SNACK", EVENT_DATE, "2026-11-01T12:00:00", "America/Sao_Paulo", "ABERTO"))
				.andExpect(status().isCreated());
	}

	@Test
	void doesNotCreateEventInStatusOtherThanDraftOrOpen() throws Exception {
		create(body("Lanche", "SNACK", EVENT_DATE, FUTURE_DEADLINE, null, "ENCERRADO"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento novo só pode nascer em rascunho ou aberto."));
	}

	@Test
	void doesNotCreateEventWithDeadlineAfterEventDate() throws Exception {
		create(body("Lanche", "SNACK", EVENT_DATE, "2026-12-02T12:00:00", null, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O prazo para alterações não pode ser depois da data do evento."));
	}

	@Test
	void invalidFieldsReturn400WithPortugueseMessages() throws Exception {
		create("{\"name\":\"\",\"type\":\"LUNCH\",\"dateTime\":\"" + EVENT_DATE + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field=='name')].message").value("não deve estar em branco"))
				.andExpect(jsonPath("$.errors[?(@.field=='changeDeadline')].message").value("não deve ser nulo"));
	}

	@Test
	void nonexistentTimeZoneReturns400() throws Exception {
		create(body("Lanche", "SNACK", EVENT_DATE, FUTURE_DEADLINE, "Marte/Olimpo", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("timeZone"));
	}

	@Test
	void typeOutsideListReturns400() throws Exception {
		create(body("Lanche", "JANTAR", EVENT_DATE, FUTURE_DEADLINE, null, null))
				.andExpect(status().isBadRequest());
	}

	@Test
	void findsExistingEvent() throws Exception {
		Event event = saveEvent("Almoço", EventType.LUNCH, EventStatus.RASCUNHO, "2026-12-01T12:00:00");

		mvc.perform(get("/api/events/" + event.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Almoço"));
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(get("/api/events/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Evento não encontrado."));
	}

	@Test
	void listOrderedByEventDateAndFiltersByStatusTypeAndDate() throws Exception {
		saveEvent("C - dezembro", EventType.LUNCH, EventStatus.ABERTO, "2026-12-20T12:00:00");
		saveEvent("A - novembro", EventType.SNACK, EventStatus.RASCUNHO, "2026-11-10T15:00:00");
		saveEvent("B - dezembro cedo", EventType.LUNCH, EventStatus.RASCUNHO, "2026-12-05T12:00:00");

		mvc.perform(get("/api/events"))
				.andExpect(jsonPath("$[*].name", contains("A - novembro", "B - dezembro cedo", "C - dezembro")));
		mvc.perform(get("/api/events").param("status", "RASCUNHO"))
				.andExpect(jsonPath("$[*].name", contains("A - novembro", "B - dezembro cedo")));
		mvc.perform(get("/api/events").param("type", "LUNCH"))
				.andExpect(jsonPath("$[*].name", contains("B - dezembro cedo", "C - dezembro")));
		mvc.perform(get("/api/events").param("from", "2026-12-01").param("until", "2026-12-05"))
				.andExpect(jsonPath("$[*].name", contains("B - dezembro cedo")));
		mvc.perform(get("/api/events").param("status", "ENCERRADO"))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void invalidStatusFilterReturns400() throws Exception {
		mvc.perform(get("/api/events").param("status", "EXPLODIDO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'status'."));
	}

	@Test
	void editsWholeDraft() throws Exception {
		Event event = saveEvent("Antigo", EventType.SNACK, EventStatus.RASCUNHO, "2026-12-01T12:00:00");

		update(event, updateBody("Novo nome", "LUNCH", "2026-12-10T12:00:00", "2026-12-09T12:00:00", "Europe/Lisbon"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Novo nome"))
				.andExpect(jsonPath("$.type").value("LUNCH"))
				.andExpect(jsonPath("$.changeDeadline").value("2026-12-09T12:00:00"))
				.andExpect(jsonPath("$.timeZone").value("Europe/Lisbon"));
	}

	@Test
	void editWithoutTimeZoneKeepsCurrentTimeZone() throws Exception {
		Event event = saveEvent("Antigo", EventType.SNACK, EventStatus.RASCUNHO, "2026-12-01T12:00:00");

		update(event, updateBody("Novo", "SNACK", "2026-12-01T12:00:00", FUTURE_DEADLINE, null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"));
	}

	@Test
	void doesNotEditOpenEventWithAlreadyExpiredDeadline() throws Exception {
		Event event = saveEvent("Aberto", EventType.SNACK, EventStatus.ABERTO, "2026-12-01T12:00:00");

		update(event, updateBody("Aberto", "SNACK", EVENT_DATE, EXPIRED_DEADLINE, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento aberto precisa de um prazo que ainda não passou."));
	}

	@Test
	void closedEventEditsNameAndDateButNotDeadline() throws Exception {
		Event event = saveEvent("Encerrado", EventType.SNACK, EventStatus.ENCERRADO, "2026-12-01T12:00:00");
		String currentDeadline = event.getChangeDeadline().toString();

		update(event, updateBody("Novo nome", "SNACK", "2026-12-02T12:00:00", currentDeadline, null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Novo nome"))
				.andExpect(jsonPath("$.dateTime").value("2026-12-02T12:00:00"));

		update(event, updateBody("Novo nome", "SNACK", "2026-12-02T12:00:00", "2026-11-29T12:00:00", null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Em um evento encerrado, o prazo e o fuso não podem mudar."));
		update(event, updateBody("Novo nome", "SNACK", "2026-12-02T12:00:00", currentDeadline, "Europe/Lisbon"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void doesNotEditHeldNorCancelledEvent() throws Exception {
		Event held = saveEvent("Realizado", EventType.SNACK, EventStatus.REALIZADO, "2026-12-01T12:00:00");
		Event cancelled = saveEvent("Cancelado", EventType.SNACK, EventStatus.CANCELADO, "2026-12-01T12:00:00");

		update(held, updateBody("X", "SNACK", EVENT_DATE, FUTURE_DEADLINE, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento realizado não pode ser editado."));
		update(cancelled, updateBody("X", "SNACK", EVENT_DATE, FUTURE_DEADLINE, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento cancelado não pode ser editado."));
	}

	@Test
	void editingMissingEventReturns404() throws Exception {
		mvc.perform(put("/api/events/999999").contentType(MediaType.APPLICATION_JSON)
						.content(updateBody("X", "SNACK", EVENT_DATE, FUTURE_DEADLINE, null)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletesDraft() throws Exception {
		Event event = saveEvent("Rascunho", EventType.SNACK, EventStatus.RASCUNHO, "2026-12-01T12:00:00");

		mvc.perform(delete("/api/events/" + event.getId())).andExpect(status().isNoContent());
		mvc.perform(get("/api/events/" + event.getId())).andExpect(status().isNotFound());
	}

	@Test
	void onlyDraftCanBeDeleted() throws Exception {
		for (EventStatus status : new EventStatus[] { EventStatus.ABERTO, EventStatus.ENCERRADO,
				EventStatus.REALIZADO, EventStatus.CANCELADO }) {
			Event event = saveEvent(status.name(), EventType.SNACK, status, "2026-12-01T12:00:00");

			mvc.perform(delete("/api/events/" + event.getId()))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail")
							.value("Só eventos em rascunho podem ser excluídos. Cancele o evento para desfazê-lo."));
		}
	}

	@Test
	void deletingMissingEventReturns404() throws Exception {
		mvc.perform(delete("/api/events/999999")).andExpect(status().isNotFound());
	}

	private ResultActions create(String body) throws Exception {
		return mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions update(Event event, String body) throws Exception {
		return mvc.perform(put("/api/events/" + event.getId()).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Event saveEvent(String name, EventType type, EventStatus status, String dateTime) {
		LocalDateTime data = LocalDateTime.parse(dateTime);
		Event event = new Event(name, null, type, data, data.minusDays(1), ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

	private String body(String name, String type, String dateTime, String deadline, String timeZone, String status) {
		return "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"dateTime\":\"" + dateTime
				+ "\",\"changeDeadline\":\"" + deadline + "\""
				+ (timeZone == null ? "" : ",\"timeZone\":\"" + timeZone + "\"")
				+ (status == null ? "" : ",\"status\":\"" + status + "\"") + "}";
	}

	private String updateBody(String name, String type, String dateTime, String deadline, String timeZone) {
		return body(name, type, dateTime, deadline, timeZone, null);
	}

}
