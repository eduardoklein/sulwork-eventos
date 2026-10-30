package br.com.sulwork.events.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
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

import br.com.sulwork.events.TestCpfs;
import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Contribution;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.repository.EventRepository;
import jakarta.persistence.EntityManager;

/** The clock stays at 12:00 in Sao Paulo, November 1st, 2026. */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContributionApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	@Test
	void registersContributionAndShowsPersonItemAndUnit() throws Exception {
		Scenario c = scenario(10);

		create(c.event, c.ana, c.juice, 4, "Levo na garrafa")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/events/" + c.event.getId() + "/contributions/")))
				.andExpect(jsonPath("$.participantName").value("Ana"))
				.andExpect(jsonPath("$.itemName").value("Suco"))
				.andExpect(jsonPath("$.unit").value("UNIT"))
				.andExpect(jsonPath("$.quantity").value(4))
				.andExpect(jsonPath("$.note").value("Levo na garrafa"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());
		mvc.perform(get("/api/events/" + c.event.getId() + "/items/" + c.juice.getId()))
				.andExpect(jsonPath("$.contributedQuantity").value(4))
				.andExpect(jsonPath("$.pendingQuantity").value(6));
	}

	@Test
	void onlyOpenEventAcceptsContributions() throws Exception {
		for (EventStatus status : new EventStatus[] { EventStatus.RASCUNHO, EventStatus.ENCERRADO,
				EventStatus.REALIZADO, EventStatus.CANCELADO }) {
			Scenario c = scenario(10);
			c.event.setStatus(status);
			events.saveAndFlush(c.event);

			create(c.event, c.ana, c.juice, 1, null)
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value("Só um evento aberto aceita contribuições. Este evento está "
							+ status.getDescription().toLowerCase() + "."));
		}
	}

	@Test
	void eventWithExpiredDeadlineDoesNotAcceptContributions() throws Exception {
		Scenario c = scenario(10);
		c.event.setChangeDeadline(LocalDateTime.parse("2026-10-30T12:00:00"));
		events.saveAndFlush(c.event);

		create(c.event, c.ana, c.juice, 1, null).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void acceptsUpToExactLimitAndRejectsAfter() throws Exception {
		Scenario c = scenario(10);

		create(c.event, c.ana, c.juice, 4, null).andExpect(status().isCreated());
		create(c.event, c.beto, c.juice, 6, null).andExpect(status().isCreated());
		create(c.event, c.cida, c.juice, 1, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Este item já está completo."));
	}

	@Test
	void exceedingRequiredReturns422WithWhatStillFits() throws Exception {
		Scenario c = scenario(10);
		create(c.event, c.ana, c.juice, 7, null).andExpect(status().isCreated());

		create(c.event, c.beto, c.juice, 4, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Só cabem mais 3 (unidade) neste item."));
		mvc.perform(get(base(c.event)).param("itemId", String.valueOf(c.juice.getId())))
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void samePersonContributingAgainToSameItemReturns409() throws Exception {
		Scenario c = scenario(10);
		create(c.event, c.ana, c.juice, 2, null).andExpect(status().isCreated());

		create(c.event, c.ana, c.juice, 2, null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Esta pessoa já contribui com este item. Edite a contribuição."));
		mvc.perform(get(base(c.event))).andExpect(jsonPath("$[0].quantity").value(2));
	}

	@Test
	void samePersonCanContributeToDifferentItems() throws Exception {
		Scenario c = scenario(10);

		create(c.event, c.ana, c.juice, 2, null).andExpect(status().isCreated());
		create(c.event, c.ana, c.water, 2, null).andExpect(status().isCreated());
	}

	@Test
	void participantOrItemNotInEventReturns422() throws Exception {
		Scenario c = scenario(10);
		Scenario other = scenario(10);

		create(c.event, other.ana, c.juice, 1, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Participante não encontrado neste evento."));
		create(c.event, c.ana, other.juice, 1, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Item não encontrado neste evento."));
		mvc.perform(post(base(c.event)).contentType(MediaType.APPLICATION_JSON)
						.content("{\"participantId\":999999,\"itemId\":" + c.juice.getId() + ",\"quantity\":1}"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(post("/api/events/999999/contributions").contentType(MediaType.APPLICATION_JSON)
						.content("{\"participantId\":1,\"itemId\":1,\"quantity\":1}"))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/events/999999/contributions")).andExpect(status().isNotFound());
	}

	@Test
	void invalidFieldsReturn400() throws Exception {
		Scenario c = scenario(10);

		mvc.perform(post(base(c.event)).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field=='participantId')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field=='itemId')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field=='quantity')]").exists());
		create(c.event, c.ana, c.juice, 1, "x".repeat(256)).andExpect(status().isBadRequest());
	}

	@Test
	void listsInCreationOrderAndFiltersByItemAndParticipant() throws Exception {
		Scenario c = scenario(10);
		create(c.event, c.beto, c.juice, 1, null);
		create(c.event, c.ana, c.water, 1, null);
		create(c.event, c.ana, c.juice, 1, null);

		mvc.perform(get(base(c.event)))
				.andExpect(jsonPath("$[*].participantName", contains("Beto", "Ana", "Ana")));
		mvc.perform(get(base(c.event)).param("itemId", String.valueOf(c.juice.getId())))
				.andExpect(jsonPath("$[*].participantName", contains("Beto", "Ana")));
		mvc.perform(get(base(c.event)).param("participantId", String.valueOf(c.ana.getId())))
				.andExpect(jsonPath("$[*].itemName", contains("Água", "Suco")));
	}

	@Test
	void changesQuantityAndNoteWithoutCreatingAnother() throws Exception {
		Scenario c = scenario(10);
		Contribution contribution = saveContribution(c.ana, c.juice, 3);

		update(c.event, contribution, 5, "Mudei de ideia")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(5))
				.andExpect(jsonPath("$.note").value("Mudei de ideia"))
				.andExpect(jsonPath("$.id").value(contribution.getId()));
		mvc.perform(get(base(c.event))).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void changeRespectsLimitCountingOnlyOtherPeople() throws Exception {
		Scenario c = scenario(10);
		Contribution anaContribution = saveContribution(c.ana, c.juice, 4);
		saveContribution(c.beto, c.juice, 3);

		update(c.event, anaContribution, 7, null).andExpect(status().isOk());
		update(c.event, anaContribution, 8, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O máximo para esta contribuição é 7 (unidade)."));
	}

	@Test
	void changeToZeroOrNegativeReturns400() throws Exception {
		Scenario c = scenario(10);
		Contribution contribution = saveContribution(c.ana, c.juice, 3);

		update(c.event, contribution, 0, null).andExpect(status().isBadRequest());
		update(c.event, contribution, -2, null).andExpect(status().isBadRequest());
	}

	@Test
	void changeDoesNotChangePersonNorItem() throws Exception {
		Scenario c = scenario(10);
		Contribution contribution = saveContribution(c.ana, c.juice, 3);

		mvc.perform(put(base(c.event) + "/" + contribution.getId()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"participantId\":" + c.beto.getId() + ",\"itemId\":" + c.water.getId() + ",\"quantity\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.participantName").value("Ana"))
				.andExpect(jsonPath("$.itemName").value("Suco"));
	}

	@Test
	void removingFreesQuantityForAnotherPerson() throws Exception {
		Scenario c = scenario(10);
		Contribution anaContribution = saveContribution(c.ana, c.juice, 10);
		create(c.event, c.beto, c.juice, 1, null).andExpect(status().isUnprocessableEntity());

		mvc.perform(delete(base(c.event) + "/" + anaContribution.getId())).andExpect(status().isNoContent());

		create(c.event, c.beto, c.juice, 10, null).andExpect(status().isCreated());
	}

	@Test
	void doesNotChangeNorRemoveInClosedEvent() throws Exception {
		Scenario c = scenario(10);
		Contribution contribution = saveContribution(c.ana, c.juice, 3);
		c.event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(c.event);

		update(c.event, contribution, 2, null).andExpect(status().isUnprocessableEntity());
		mvc.perform(delete(base(c.event) + "/" + contribution.getId())).andExpect(status().isUnprocessableEntity());
		mvc.perform(get(base(c.event))).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void contributionOfOtherEventOrMissingReturns404() throws Exception {
		Scenario c = scenario(10);
		Scenario other = scenario(10);
		Contribution contribution = saveContribution(c.ana, c.juice, 3);

		mvc.perform(get(base(other.event) + "/" + contribution.getId())).andExpect(status().isNotFound());
		update(other.event, contribution, 2, null).andExpect(status().isNotFound());
		mvc.perform(delete(base(other.event) + "/" + contribution.getId())).andExpect(status().isNotFound());
		mvc.perform(delete(base(c.event) + "/999999")).andExpect(status().isNotFound());
		mvc.perform(get(base(c.event) + "/" + contribution.getId())).andExpect(status().isOk());
	}

	@Test
	void itemReductionBelowContributedStaysProtected() throws Exception {
		Scenario c = scenario(10);
		saveContribution(c.ana, c.juice, 6);

		mvc.perform(put("/api/events/" + c.event.getId() + "/items/" + c.juice.getId())
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Suco\",\"requiredQuantity\":5}"))
				.andExpect(status().isUnprocessableEntity());
	}

	private String base(Event event) {
		return "/api/events/" + event.getId() + "/contributions";
	}

	private ResultActions create(Event event, EventParticipant participant, EventItem item, int quantity,
			String note) throws Exception {
		String body = "{\"participantId\":" + participant.getId() + ",\"itemId\":" + item.getId()
				+ ",\"quantity\":" + quantity
				+ (note == null ? "" : ",\"note\":\"" + note + "\"") + "}";
		return mvc.perform(post(base(event)).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions update(Event event, Contribution contribution, int quantity, String note)
			throws Exception {
		String body = "{\"quantity\":" + quantity
				+ (note == null ? "" : ",\"note\":\"" + note + "\"") + "}";
		return mvc.perform(put(base(event) + "/" + contribution.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private record Scenario(Event event, EventItem juice, EventItem water, EventParticipant ana,
			EventParticipant beto, EventParticipant cida) {
	}

	private static int cpfCounter = 0;

	private Scenario scenario(int juiceQuantity) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(EventStatus.ABERTO);
		events.saveAndFlush(event);
		EventCategory category = new EventCategory(event, "Bebidas", MeasureUnit.UNIT, 0);
		em.persist(category);
		EventItem juice = new EventItem(event, category, "Suco", juiceQuantity, false);
		EventItem water = new EventItem(event, category, "Água", 50, false);
		em.persist(juice);
		em.persist(water);
		Scenario scenario = new Scenario(event, juice, water, participant(event, "Ana"), participant(event, "Beto"),
				participant(event, "Cida"));
		em.flush();
		return scenario;
	}

	private EventParticipant participant(Event event, String name) {
		Collaborator collaborator = new Collaborator(name, TestCpfs.validCpf(cpfCounter++));
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		return participant;
	}

	private Contribution saveContribution(EventParticipant participant, EventItem item, int quantity) {
		Contribution contribution = new Contribution(participant, item, quantity, null);
		em.persist(contribution);
		em.flush();
		return contribution;
	}

}
