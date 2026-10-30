package br.com.sulwork.events.controller;

import static org.hamcrest.Matchers.contains;
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
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestCpfs;
import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.repository.EventRepository;
import jakarta.persistence.EntityManager;

@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DietaryAnalysisApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	private int cpfCounter = 0;

	@Test
	void analyzesTheMenuForACeliacPersonAndAVeganPerson() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = category(event);
		item(event, category, "Lasagna", Allergen.GLUTEN, Allergen.LACTOSE);
		item(event, category, "Salad");
		itemWithCharacteristic(event, category, "Beef", FoodCharacteristic.ANIMAL_MEAT);
		item(event, category, "Cake", Allergen.GLUTEN, Allergen.EGG, Allergen.LACTOSE);
		participant(event, "Ana", DietaryRestriction.CELIAC_GLUTEN_FREE);
		EventParticipant beto = participant(event, "Beto", DietaryRestriction.VEGAN);
		participant(event, "Cida");
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/dietary-analysis"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(4))
				.andExpect(jsonPath("$.participants", hasSize(3)))
				.andExpect(jsonPath("$.participants[0].name").value("Ana"))
				.andExpect(jsonPath("$.participants[0].safeItems").value(2))
				.andExpect(jsonPath("$.participants[0].unsafeItems[*].itemName", contains("Lasagna", "Cake")))
				.andExpect(jsonPath("$.participants[0].unsafeItems[0].conflictingAllergens", contains("GLUTEN")))
				.andExpect(jsonPath("$.participants[1].name").value("Beto"))
				.andExpect(jsonPath("$.participants[1].safeItems").value(1))
				.andExpect(jsonPath("$.participants[1].unsafeItems[*].itemName", contains("Lasagna", "Beef", "Cake")))
				.andExpect(jsonPath("$.participants[1].unsafeItems[1].conflictingCharacteristics", contains("ANIMAL_MEAT")))
				.andExpect(jsonPath("$.participants[2].name").value("Cida"))
				.andExpect(jsonPath("$.participants[2].safeItems").value(4))
				.andExpect(jsonPath("$.restrictions[*].restriction", contains("CELIAC_GLUTEN_FREE", "VEGAN")))
				.andExpect(jsonPath("$.restrictions[1].peopleCount").value(1))
				.andExpect(jsonPath("$.restrictions[1].compatibleItems").value(1))
				.andExpect(jsonPath("$.alerts", hasSize(1)))
				.andExpect(jsonPath("$.alerts[0].level").value("WARNING"))
				.andExpect(jsonPath("$.alerts[0].participantId").value(beto.getId()))
				.andExpect(jsonPath("$.alerts[0].message")
						.value("Menos de 35% do cardápio é seguro para Beto (1 de 4 itens)."));
	}

	@Test
	void celiacPersonWithAnAllGlutenMenuGetsACriticalAlert() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = category(event);
		item(event, category, "Lasagna", Allergen.GLUTEN);
		item(event, category, "Bread", Allergen.GLUTEN);
		participant(event, "Ana", DietaryRestriction.CELIAC_GLUTEN_FREE);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/dietary-analysis"))
				.andExpect(jsonPath("$.alerts[0].level").value("CRITICAL"))
				.andExpect(jsonPath("$.alerts[0].message").value("Ana não tem nenhum item seguro no cardápio."));
	}

	@Test
	void inclusiveMenuGetsOneInformativeAlert() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = category(event);
		item(event, category, "Salad");
		participant(event, "Ana", DietaryRestriction.CELIAC_GLUTEN_FREE);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/dietary-analysis"))
				.andExpect(jsonPath("$.alerts", hasSize(1)))
				.andExpect(jsonPath("$.alerts[0].level").value("INFO"))
				.andExpect(jsonPath("$.alerts[0].participantId").doesNotExist());
	}

	@Test
	void eventWithoutParticipantsOrItemsReturnsEmptyLists() throws Exception {
		Event event = event(EventStatus.RASCUNHO);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/dietary-analysis"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(0))
				.andExpect(jsonPath("$.participants", hasSize(0)))
				.andExpect(jsonPath("$.restrictions", hasSize(0)))
				.andExpect(jsonPath("$.alerts", hasSize(0)));
	}

	@Test
	void closedEventStillShowsTheAnalysis() throws Exception {
		Event event = event(EventStatus.ENCERRADO);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/dietary-analysis")).andExpect(status().isOk());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(get("/api/events/999999/dietary-analysis")).andExpect(status().isNotFound());
	}

	@Test
	void doesNotMixDataOfAnotherEvent() throws Exception {
		Event event = event(EventStatus.ABERTO);
		Event other = event(EventStatus.ABERTO);
		item(other, category(other), "Lasagna", Allergen.GLUTEN);
		participant(other, "Ana", DietaryRestriction.CELIAC_GLUTEN_FREE);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/dietary-analysis"))
				.andExpect(jsonPath("$.totalItems").value(0))
				.andExpect(jsonPath("$.participants", hasSize(0)));
	}

	private Event event(EventStatus status) {
		LocalDateTime date = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Event", null, EventType.LUNCH, date, date.minusDays(1), ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

	private EventCategory category(Event event) {
		EventCategory category = new EventCategory(event, "Food", MeasureUnit.UNIT, 0);
		em.persist(category);
		return category;
	}

	private void item(Event event, EventCategory category, String name, Allergen... allergens) {
		EventItem item = new EventItem(event, category, name, 10, false);
		item.getAllergens().addAll(List.of(allergens));
		em.persist(item);
	}

	private void itemWithCharacteristic(Event event, EventCategory category, String name,
			FoodCharacteristic characteristic) {
		EventItem item = new EventItem(event, category, name, 10, false);
		item.getCharacteristics().add(characteristic);
		em.persist(item);
	}

	private EventParticipant participant(Event event, String name, DietaryRestriction... restrictions) {
		Collaborator collaborator = new Collaborator(name, TestCpfs.validCpf(cpfCounter++));
		collaborator.getRestrictions().addAll(List.of(restrictions));
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		return participant;
	}

}
