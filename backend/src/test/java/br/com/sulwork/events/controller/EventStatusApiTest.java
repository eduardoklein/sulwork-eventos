package br.com.sulwork.events.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/** The clock stays at 12:00 in Sao Paulo, November 1st, 2026. */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventStatusApiTest {

	private static final String FUTURE_DATE = "2026-12-01T12:00:00";
	private static final String PAST_DATE = "2026-10-31T12:00:00";
	private static final String DATE_NOW = "2026-11-01T12:00:00";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Test
	void draftGoesToOpen() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO, FUTURE_DATE);

		changeStatus(event, "ABERTO", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.acceptsContributions").value(true))
				.andExpect(jsonPath("$.allowedTransitions[0]").value("ENCERRADO"));
	}

	@Test
	void doesNotOpenDraftWithExpiredDeadline() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO, PAST_DATE);

		changeStatus(event, "ABERTO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento aberto precisa de um prazo que ainda não passou."));
	}

	@Test
	void opensDraftWithExpiredDeadlineWhenCallBringsNewDeadline() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO, FUTURE_DATE);
		event.setChangeDeadline(LocalDateTime.parse("2026-10-30T12:00:00"));
		events.saveAndFlush(event);

		changeStatus(event, "ABERTO", "2026-11-20T12:00:00")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.changeDeadline").value("2026-11-20T12:00:00"));
	}

	@Test
	void newDeadlineAfterEventDateReturns422() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO, FUTURE_DATE);

		changeStatus(event, "ABERTO", "2026-12-02T12:00:00")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O prazo para alterações não pode ser depois da data do evento."));
	}

	@Test
	void newDeadlineOnlyAppliesWhenOpening() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO, FUTURE_DATE);

		changeStatus(event, "CANCELADO", "2026-11-20T12:00:00")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O novo prazo só vale ao abrir o evento."));
	}

	@Test
	void validTransitionsWork() throws Exception {
		changeStatus(saveEvent(EventStatus.ABERTO, FUTURE_DATE), "ENCERRADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADO"));
		changeStatus(saveEvent(EventStatus.ABERTO, FUTURE_DATE), "CANCELADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
		changeStatus(saveEvent(EventStatus.RASCUNHO, FUTURE_DATE), "CANCELADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
		changeStatus(saveEvent(EventStatus.ENCERRADO, FUTURE_DATE), "CANCELADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
	}

	@Test
	void closedGoesToHeldAfterEventDate() throws Exception {
		changeStatus(saveEvent(EventStatus.ENCERRADO, PAST_DATE), "REALIZADO", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REALIZADO"))
				.andExpect(jsonPath("$.allowedTransitions.length()").value(0));
	}

	@Test
	void closedGoesToHeldAtExactEventInstant() throws Exception {
		changeStatus(saveEvent(EventStatus.ENCERRADO, DATE_NOW), "REALIZADO", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REALIZADO"));
	}

	@Test
	void doesNotMarkAsHeldBeforeEventDate() throws Exception {
		Event event = saveEvent(EventStatus.ENCERRADO, FUTURE_DATE);

		changeStatus(event, "REALIZADO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail")
						.value("Um evento só pode ser marcado como realizado depois da data e hora marcadas."));
		mvc.perform(get("/api/events/" + event.getId())).andExpect(jsonPath("$.status").value("ENCERRADO"));
	}

	@Test
	void heldDoesNotGoBackToOpen() throws Exception {
		changeStatus(saveEvent(EventStatus.REALIZADO, PAST_DATE), "ABERTO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível passar de Realizado para Aberto. Esse status é final."));
	}

	@Test
	void cancelledIsFinal() throws Exception {
		changeStatus(saveEvent(EventStatus.CANCELADO, FUTURE_DATE), "RASCUNHO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível passar de Cancelado para Rascunho. Esse status é final."));
	}

	@Test
	void closedDoesNotReopenAndListsAllowedTransitions() throws Exception {
		changeStatus(saveEvent(EventStatus.ENCERRADO, FUTURE_DATE), "ABERTO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(
						"Não é possível passar de Encerrado para Aberto. Transições permitidas: Realizado, Cancelado."));
	}

	@Test
	void draftDoesNotSkipToHeld() throws Exception {
		changeStatus(saveEvent(EventStatus.RASCUNHO, PAST_DATE), "REALIZADO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(
						"Não é possível passar de Rascunho para Realizado. Transições permitidas: Aberto, Cancelado."));
	}

	@Test
	void movingToSameStatusReturns422() throws Exception {
		changeStatus(saveEvent(EventStatus.ABERTO, FUTURE_DATE), "ABERTO", null)
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void missingOrOutOfListStatusReturns400() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO, FUTURE_DATE);

		mvc.perform(patch("/api/events/" + event.getId() + "/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("newStatus"));
		changeStatus(event, "EXPLODIDO", null).andExpect(status().isBadRequest());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(patch("/api/events/999999/status").contentType(MediaType.APPLICATION_JSON)
						.content("{\"newStatus\":\"ABERTO\"}"))
				.andExpect(status().isNotFound());
	}

	private ResultActions changeStatus(Event event, String newStatus, String newDeadline) throws Exception {
		String body = "{\"newStatus\":\"" + newStatus + "\""
				+ (newDeadline == null ? "" : ",\"newChangeDeadline\":\"" + newDeadline + "\"") + "}";
		return mvc.perform(patch("/api/events/" + event.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Event saveEvent(EventStatus status, String dateTime) {
		LocalDateTime data = LocalDateTime.parse(dateTime);
		Event event = new Event("Evento", null, EventType.SNACK, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

}
