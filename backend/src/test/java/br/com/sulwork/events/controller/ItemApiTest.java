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

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Allergen;
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
class ItemApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	@Test
	void createsItemWithAllergensAndCharacteristicsInFixedOrder() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Doces", MeasureUnit.UNIT);

		create(event, createBody("Brigadeiro", category.getId(), 40, true,
				"[\"EGG\",\"LACTOSE\"]", "[\"ADDED_SUGAR\"]"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/events/" + event.getId() + "/items/")))
				.andExpect(jsonPath("$.name").value("Brigadeiro"))
				.andExpect(jsonPath("$.categoryId").value(category.getId()))
				.andExpect(jsonPath("$.categoryName").value("Doces"))
				.andExpect(jsonPath("$.unit").value("UNIT"))
				.andExpect(jsonPath("$.requiredQuantity").value(40))
				.andExpect(jsonPath("$.mandatory").value(true))
				.andExpect(jsonPath("$.contributedQuantity").value(0))
				.andExpect(jsonPath("$.pendingQuantity").value(40))
				.andExpect(jsonPath("$.coveredPercentage").value(0))
				.andExpect(jsonPath("$.allergens", contains("LACTOSE", "EGG")))
				.andExpect(jsonPath("$.characteristics", contains("ADDED_SUGAR")));
	}

	@Test
	void createsItemWithoutListsAndNotMandatoryByDefault() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.MILLILITER);

		create(event, "{\"name\":\"Suco\",\"categoryId\":" + category.getId() + ",\"requiredQuantity\":2000}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.mandatory").value(false))
				.andExpect(jsonPath("$.allergens", hasSize(0)))
				.andExpect(jsonPath("$.characteristics", hasSize(0)));
	}

	@Test
	void repeatedNameInSameCategoryReturns409IgnoringCaseAndSpaces() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		create(event, createBody("Suco", category.getId(), 10, false, "[]", "[]")).andExpect(status().isCreated());

		create(event, createBody("Suco", category.getId(), 5, false, "[]", "[]"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um item com esse nome nesta categoria."));
		create(event, createBody("  SUCO ", category.getId(), 5, false, "[]", "[]")).andExpect(status().isConflict());
	}

	@Test
	void sameNameInDifferentCategoryIsAccepted() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory drinks = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		EventCategory desserts = saveCategory(event, "Sobremesas", MeasureUnit.UNIT);

		create(event, createBody("Gelo", drinks.getId(), 10, false, "[]", "[]")).andExpect(status().isCreated());
		create(event, createBody("Gelo", desserts.getId(), 10, false, "[]", "[]")).andExpect(status().isCreated());
	}

	@Test
	void invalidFieldsReturn400() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);

		create(event, "{\"name\":\"\",\"requiredQuantity\":0}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field=='name')].message").value("não deve estar em branco"))
				.andExpect(jsonPath("$.errors[?(@.field=='categoryId')].message").value("não deve ser nulo"))
				.andExpect(jsonPath("$.errors[?(@.field=='requiredQuantity')]").exists());
		create(event, createBody("Suco", category.getId(), 5, false, "[\"POEIRA\"]", "[]"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingOrOtherEventCategoryReturns422() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		Event other = saveEvent(EventStatus.RASCUNHO);
		EventCategory otherEventCategory = saveCategory(other, "Bebidas", MeasureUnit.UNIT);

		create(event, createBody("Suco", 999999L, 5, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Categoria não encontrada neste evento."));
		create(event, createBody("Suco", otherEventCategory.getId(), 5, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(post("/api/events/999999/items").contentType(MediaType.APPLICATION_JSON)
						.content(createBody("Suco", 1L, 5, false, "[]", "[]")))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/events/999999/items")).andExpect(status().isNotFound());
	}

	@Test
	void menuChangesOnlyInDraftAndOpen() throws Exception {
		for (EventStatus status : new EventStatus[] { EventStatus.ENCERRADO, EventStatus.REALIZADO,
				EventStatus.CANCELADO }) {
			Event event = saveEvent(status);
			EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);

			create(event, createBody("Suco", category.getId(), 5, false, "[]", "[]"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value(
							"Não é possível alterar o cardápio de um evento " + status.getDescription().toLowerCase() + "."));
		}
	}

	@Test
	void openEventWithExpiredDeadlineAlreadyCountsAsClosed() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		event.setChangeDeadline(LocalDateTime.parse("2026-10-30T12:00:00"));
		events.saveAndFlush(event);

		create(event, createBody("Suco", category.getId(), 5, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void listsInCreationOrderAndFiltersByCategory() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory drinks = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		EventCategory sweets = saveCategory(event, "Doces", MeasureUnit.UNIT);
		saveItem(event, drinks, "Zebra", 1);
		saveItem(event, sweets, "Bolo", 1);
		saveItem(event, drinks, "Abacaxi", 1);

		mvc.perform(get(base(event)))
				.andExpect(jsonPath("$[*].name", contains("Zebra", "Bolo", "Abacaxi")));
		mvc.perform(get(base(event)).param("categoryId", String.valueOf(drinks.getId())))
				.andExpect(jsonPath("$[*].name", contains("Zebra", "Abacaxi")));
	}

	@Test
	void contributionsFeedPendingAndPercentageRoundedDown() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		EventItem juice = saveItem(event, category, "Suco", 10);
		EventItem almostComplete = saveItem(event, category, "Quase completo", 200);
		EventItem complete = saveItem(event, category, "Completo", 5);
		EventParticipant ana = saveParticipant(event, "Ana", "52998224725");
		EventParticipant beto = saveParticipant(event, "Beto", "39053344705");
		contribute(ana, juice, 4);
		contribute(beto, juice, 3);
		contribute(ana, almostComplete, 199);
		contribute(beto, complete, 5);

		mvc.perform(get(base(event) + "/" + juice.getId()))
				.andExpect(jsonPath("$.contributedQuantity").value(7))
				.andExpect(jsonPath("$.pendingQuantity").value(3))
				.andExpect(jsonPath("$.coveredPercentage").value(70));
		mvc.perform(get(base(event) + "/" + almostComplete.getId()))
				.andExpect(jsonPath("$.coveredPercentage").value(99))
				.andExpect(jsonPath("$.pendingQuantity").value(1));
		mvc.perform(get(base(event) + "/" + complete.getId()))
				.andExpect(jsonPath("$.coveredPercentage").value(100))
				.andExpect(jsonPath("$.pendingQuantity").value(0));
		mvc.perform(get(base(event)))
				.andExpect(jsonPath("$[0].contributedQuantity").value(7))
				.andExpect(jsonPath("$[1].contributedQuantity").value(199))
				.andExpect(jsonPath("$[2].contributedQuantity").value(5));
	}

	@Test
	void itemOfAnotherEventReturns404() throws Exception {
		Event owner = saveEvent(EventStatus.RASCUNHO);
		Event other = saveEvent(EventStatus.RASCUNHO);
		EventItem item = saveItem(owner, saveCategory(owner, "Bebidas", MeasureUnit.UNIT), "Suco", 5);

		mvc.perform(get(base(other) + "/" + item.getId())).andExpect(status().isNotFound());
		update(other, item, updateBody("X", 5, false, "[]", "[]")).andExpect(status().isNotFound());
		mvc.perform(delete(base(other) + "/" + item.getId())).andExpect(status().isNotFound());
	}

	@Test
	void editsNameQuantityMandatoryAndLists() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Doces", MeasureUnit.UNIT);
		create(event, createBody("Brigadeiro", category.getId(), 40, false, "[\"LACTOSE\"]", "[]"));
		EventItem item = em.createQuery("select i from EventItem i", EventItem.class).getSingleResult();

		update(event, item, updateBody("Beijinho", 60, true, "[\"EGG\"]", "[\"ADDED_SUGAR\"]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Beijinho"))
				.andExpect(jsonPath("$.requiredQuantity").value(60))
				.andExpect(jsonPath("$.mandatory").value(true))
				.andExpect(jsonPath("$.allergens", contains("EGG")))
				.andExpect(jsonPath("$.characteristics", contains("ADDED_SUGAR")));
		em.flush();
		em.clear();
		mvc.perform(get(base(event) + "/" + item.getId()))
				.andExpect(jsonPath("$.allergens", contains("EGG")));
	}

	@Test
	void itemDoesNotChangeCategory() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory original = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		EventCategory other = saveCategory(event, "Doces", MeasureUnit.UNIT);
		EventItem item = saveItem(event, original, "Suco", 5);

		update(event, item, "{\"name\":\"Suco\",\"categoryId\":" + other.getId() + ",\"requiredQuantity\":5}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoryId").value(original.getId()));
	}

	@Test
	void itemCanChangeOnlyCaseOfOwnName() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventItem item = saveItem(event, saveCategory(event, "Bebidas", MeasureUnit.UNIT), "Suco", 5);

		update(event, item, updateBody("suco", 5, false, "[]", "[]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("suco"));
	}

	@Test
	void renamingToNameOfAnotherItemInSameCategoryReturns409() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		saveItem(event, category, "Suco", 5);
		EventItem water = saveItem(event, category, "Água", 5);

		update(event, water, updateBody("SUCO", 5, false, "[]", "[]")).andExpect(status().isConflict());
	}

	@Test
	void doesNotReduceQuantityBelowWhatWasContributed() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventItem item = saveItem(event, saveCategory(event, "Bebidas", MeasureUnit.UNIT), "Suco", 10);
		contribute(saveParticipant(event, "Ana", "52998224725"), item, 4);
		contribute(saveParticipant(event, "Beto", "39053344705"), item, 3);

		update(event, item, updateBody("Suco", 6, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(
						"A quantidade necessária não pode ficar abaixo do que já foi contribuído (7)."));
		update(event, item, updateBody("Suco", 7, false, "[]", "[]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.coveredPercentage").value(100));
		update(event, item, updateBody("Suco", 20, false, "[]", "[]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pendingQuantity").value(13));
	}

	@Test
	void doesNotEditItemOfClosedEvent() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventItem item = saveItem(event, saveCategory(event, "Bebidas", MeasureUnit.UNIT), "Suco", 5);
		event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(event);

		update(event, item, updateBody("Outro", 5, false, "[]", "[]")).andExpect(status().isUnprocessableEntity());
		mvc.perform(delete(base(event) + "/" + item.getId())).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void deletesItemWithoutContributions() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventItem item = saveItem(event, saveCategory(event, "Bebidas", MeasureUnit.UNIT), "Suco", 5);

		mvc.perform(delete(base(event) + "/" + item.getId())).andExpect(status().isNoContent());
		mvc.perform(get(base(event) + "/" + item.getId())).andExpect(status().isNotFound());
	}

	@Test
	void doesNotDeleteItemThatAlreadyHasContributions() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventItem item = saveItem(event, saveCategory(event, "Bebidas", MeasureUnit.UNIT), "Suco", 5);
		contribute(saveParticipant(event, "Ana", "52998224725"), item, 1);

		mvc.perform(delete(base(event) + "/" + item.getId()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível excluir um item que já tem contribuições."));
		mvc.perform(get(base(event) + "/" + item.getId())).andExpect(status().isOk());
	}

	private String base(Event event) {
		return "/api/events/" + event.getId() + "/items";
	}

	private ResultActions create(Event event, String body) throws Exception {
		return mvc.perform(post(base(event)).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions update(Event event, EventItem item, String body) throws Exception {
		return mvc.perform(put(base(event) + "/" + item.getId()).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String createBody(String name, Long categoryId, int quantity, boolean mandatory, String allergens,
			String characteristics) {
		return "{\"name\":\"" + name + "\",\"categoryId\":" + categoryId + ",\"requiredQuantity\":" + quantity
				+ ",\"mandatory\":" + mandatory + ",\"allergens\":" + allergens + ",\"characteristics\":"
				+ characteristics + "}";
	}

	private String updateBody(String name, int quantity, boolean mandatory, String allergens,
			String characteristics) {
		return "{\"name\":\"" + name + "\",\"requiredQuantity\":" + quantity + ",\"mandatory\":" + mandatory
				+ ",\"allergens\":" + allergens + ",\"characteristics\":" + characteristics + "}";
	}

	private Event saveEvent(EventStatus status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

	private EventCategory saveCategory(Event event, String name, MeasureUnit unit) {
		EventCategory category = new EventCategory(event, name, unit, 0);
		em.persist(category);
		em.flush();
		return category;
	}

	private EventItem saveItem(Event event, EventCategory category, String name, int quantity) {
		EventItem item = new EventItem(event, category, name, quantity, false);
		item.getAllergens().add(Allergen.GLUTEN);
		em.persist(item);
		em.flush();
		return item;
	}

	private EventParticipant saveParticipant(Event event, String name, String cpf) {
		Collaborator collaborator = new Collaborator(name, cpf);
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		em.flush();
		return participant;
	}

	private void contribute(EventParticipant participant, EventItem item, int quantity) {
		em.persist(new Contribution(participant, item, quantity, null));
		em.flush();
	}

}
