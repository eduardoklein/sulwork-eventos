package br.com.sulwork.events.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
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

@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoverageApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	private int cpfCounter = 0;

	@Test
	void calculatesCoverageOfTwoCategoriesWithDifferentUnits() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory mains = category(event, "Pratos", MeasureUnit.UNIT, 0);
		EventCategory drinks = category(event, "Bebidas", MeasureUnit.MILLILITER, 5000);
		EventItem lasagna = item(event, mains, "Lasanha", 6, true);
		item(event, mains, "Sobremesa", 4, false);
		EventItem juice = item(event, drinks, "Suco", 2000, false);
		EventParticipant ana = participant(event);
		contribution(ana, lasagna, 6);
		contribution(ana, juice, 1000);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/coverage"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readiness").value("MANDATORY_SECURED"))
				.andExpect(jsonPath("$.overallPercentage").value(47)) // (75 + 20) / 2
				.andExpect(jsonPath("$.mandatoryPercentage").value(100))
				.andExpect(jsonPath("$.optionalPercentage").value(25)) // (0 + 50) / 2
				.andExpect(jsonPath("$.totalItems").value(3))
				.andExpect(jsonPath("$.categories[0].name").value("Pratos"))
				.andExpect(jsonPath("$.categories[0].percentage").value(75)) // (2 x 6) / (2 x 6 + 4)
				.andExpect(jsonPath("$.categories[0].requiredQuantity").value(10))
				.andExpect(jsonPath("$.categories[0].contributedQuantity").value(6))
				.andExpect(jsonPath("$.categories[0].goalReached").value(true))
				.andExpect(jsonPath("$.categories[1].name").value("Bebidas"))
				.andExpect(jsonPath("$.categories[1].unit").value("MILLILITER"))
				.andExpect(jsonPath("$.categories[1].goalDeficit").value(3000))
				.andExpect(jsonPath("$.categories[1].effectiveTarget").value(5000))
				.andExpect(jsonPath("$.categories[1].percentage").value(20)) // 1000 / (2000 + 3000)
				.andExpect(jsonPath("$.categories[1].goalReached").value(false));
	}

	@Test
	void eventWithEverythingCompleteAndGoalsReachedIsComplete() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = category(event, "Pratos", MeasureUnit.UNIT, 5);
		EventItem item = item(event, category, "Lasanha", 10, true);
		contribution(participant(event), item, 10);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/coverage"))
				.andExpect(jsonPath("$.readiness").value("COMPLETE"))
				.andExpect(jsonPath("$.overallPercentage").value(100));
	}

	@Test
	void eventWithoutCategoriesIsNoItemsAndNullOverallPercentage() throws Exception {
		Event event = event(EventStatus.RASCUNHO);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/coverage"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readiness").value("NO_ITEMS"))
				.andExpect(jsonPath("$.overallPercentage").doesNotExist())
				.andExpect(jsonPath("$.mandatoryPercentage").value(100))
				.andExpect(jsonPath("$.totalItems").value(0));
	}

	@Test
	void mandatoryBelowHalfAreCritical() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = category(event, "Pratos", MeasureUnit.UNIT, 0);
		EventItem item = item(event, category, "Lasanha", 4, true);
		contribution(participant(event), item, 1);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/coverage"))
				.andExpect(jsonPath("$.readiness").value("CRITICAL"))
				.andExpect(jsonPath("$.mandatoryPercentage").value(25));
	}

	@Test
	void closedEventStillShowsCoverage() throws Exception {
		Event event = event(EventStatus.ENCERRADO);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/coverage")).andExpect(status().isOk());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(get("/api/events/999999/coverage")).andExpect(status().isNotFound());
	}

	@Test
	void doesNotMixDataOfAnotherEvent() throws Exception {
		Event event = event(EventStatus.ABERTO);
		Event other = event(EventStatus.ABERTO);
		EventCategory category = category(other, "Dela", MeasureUnit.UNIT, 0);
		item(other, category, "Item", 10, true);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/coverage"))
				.andExpect(jsonPath("$.totalItems").value(0))
				.andExpect(jsonPath("$.categories.length()").value(0));
	}

	private Event event(EventStatus status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

	private EventCategory category(Event event, String name, MeasureUnit unit, int goal) {
		EventCategory category = new EventCategory(event, name, unit, goal);
		em.persist(category);
		return category;
	}

	private EventItem item(Event event, EventCategory category, String name, int quantity, boolean mandatory) {
		EventItem item = new EventItem(event, category, name, quantity, mandatory);
		em.persist(item);
		return item;
	}

	private EventParticipant participant(Event event) {
		Collaborator collaborator = new Collaborator("Ana", TestCpfs.validCpf(cpfCounter++));
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		return participant;
	}

	private void contribution(EventParticipant participant, EventItem item, int quantity) {
		em.persist(new Contribution(participant, item, quantity, null));
	}

}
