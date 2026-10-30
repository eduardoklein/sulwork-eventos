package br.com.sulwork.events.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestCpfs;
import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Contribution;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventCategory;
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
class SuggestionApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	private int cpfCounter = 0;

	@Test
	void ranksPendingItemsAndSkipsCompleteOnesAndTheOnesTheParticipantAlreadyBrings() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = category(event, "Food", 0);
		item(event, category, "Lasagna", true, 10);
		EventItem cake = item(event, category, "Cake", false, 10);
		EventItem rice = item(event, category, "Rice", true, 10);
		EventItem salad = item(event, category, "Salad", false, 10);
		EventParticipant ana = participant(event, "Ana");
		EventParticipant beto = participant(event, "Beto");
		contribute(beto, rice, 10);
		contribute(beto, cake, 5);
		contribute(ana, salad, 2);
		em.flush();

		suggest(event, ana)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].itemName", contains("Lasagna", "Cake")))
				.andExpect(jsonPath("$[0].requiredQuantity").value(10))
				.andExpect(jsonPath("$[0].contributedQuantity").value(0))
				.andExpect(jsonPath("$[0].pendingQuantity").value(10))
				.andExpect(jsonPath("$[0].suggestedQuantity").value(5))
				.andExpect(jsonPath("$[0].coveragePercentage").value(0))
				.andExpect(jsonPath("$[0].unit").value("UNIT"))
				.andExpect(jsonPath("$[0].safeForParticipant").doesNotExist())
				.andExpect(jsonPath("$[0].reasons[0]").value("Item obrigatório."))
				.andExpect(jsonPath("$[1].suggestedQuantity").value(3))
				.andExpect(jsonPath("$[1].coveragePercentage").value(50));
	}

	@Test
	void flagsItemsThatAreNotSafeForAParticipantWithRestrictions() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = category(event, "Food", 0);
		item(event, category, "Lasagna", true, 10, Allergen.GLUTEN);
		item(event, category, "Rice", true, 10);
		EventParticipant ana = participant(event, "Ana", DietaryRestriction.CELIAC_GLUTEN_FREE);
		em.flush();

		suggest(event, ana)
				.andExpect(jsonPath("$[*].itemName", contains("Rice", "Lasagna")))
				.andExpect(jsonPath("$[0].safeForParticipant").value(true))
				.andExpect(jsonPath("$[1].safeForParticipant").value(false))
				.andExpect(jsonPath("$[1].reasons[*]", hasItem(
						"Não é seguro para as suas restrições: Glúten.")))
				.andExpect(jsonPath("$[0].priority").value(greaterThan(60)));
	}

	@Test
	void returnsAnEmptyListWhenEverythingIsComplete() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventItem rice = item(event, category(event, "Food", 0), "Rice", true, 4);
		EventParticipant ana = participant(event, "Ana");
		contribute(participant(event, "Beto"), rice, 4);
		em.flush();

		suggest(event, ana).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void returnsAnEmptyListWhenTheEventDoesNotAcceptContributions() throws Exception {
		Event event = event(EventStatus.ABERTO);
		item(event, category(event, "Food", 0), "Rice", true, 4);
		EventParticipant ana = participant(event, "Ana");
		event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(event);

		suggest(event, ana).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void anOpenEventWithAnExpiredDeadlineHasNoSuggestions() throws Exception {
		Event event = event(EventStatus.ABERTO);
		item(event, category(event, "Food", 0), "Rice", true, 4);
		EventParticipant ana = participant(event, "Ana");
		event.setChangeDeadline(LocalDateTime.parse("2026-10-30T12:00:00"));
		events.saveAndFlush(event);

		suggest(event, ana).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void missingParticipantParameterReturns400() throws Exception {
		Event event = event(EventStatus.ABERTO);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/suggestions"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Parâmetro obrigatório ausente: 'participantId'."));
	}

	@Test
	void participantOfAnotherEventReturns404() throws Exception {
		Event event = event(EventStatus.ABERTO);
		Event other = event(EventStatus.ABERTO);
		EventParticipant stranger = participant(other, "Ana");
		em.flush();

		suggest(event, stranger)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Participante não encontrado."));
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(get("/api/events/999999/suggestions").param("participantId", "1"))
				.andExpect(status().isNotFound());
	}

	private ResultActions suggest(Event event, EventParticipant participant) throws Exception {
		return mvc.perform(get("/api/events/" + event.getId() + "/suggestions")
				.param("participantId", String.valueOf(participant.getId())));
	}

	private Event event(EventStatus status) {
		LocalDateTime date = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Event", null, EventType.LUNCH, date, date.minusDays(1), ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

	private EventCategory category(Event event, String name, int goal) {
		EventCategory category = new EventCategory(event, name, MeasureUnit.UNIT, goal);
		em.persist(category);
		return category;
	}

	private EventItem item(Event event, EventCategory category, String name, boolean mandatory, int quantity,
			Allergen... allergens) {
		EventItem item = new EventItem(event, category, name, quantity, mandatory);
		item.getAllergens().addAll(List.of(allergens));
		em.persist(item);
		return item;
	}

	private EventParticipant participant(Event event, String name, DietaryRestriction... restrictions) {
		Collaborator collaborator = new Collaborator(name, TestCpfs.validCpf(cpfCounter++));
		collaborator.getRestrictions().addAll(List.of(restrictions));
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		return participant;
	}

	private void contribute(EventParticipant participant, EventItem item, int quantity) {
		em.persist(new Contribution(participant, item, quantity, null));
	}

}
