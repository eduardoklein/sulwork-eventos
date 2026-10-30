package br.com.sulwork.events.controller;

import static org.hamcrest.Matchers.contains;
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
class CategoryApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	@Test
	void createsCategoryWithZeroTotals() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);

		create(event, "Bebidas", "MILLILITER", 5000)
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/events/" + event.getId() + "/categories/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Bebidas"))
				.andExpect(jsonPath("$.unit").value("MILLILITER"))
				.andExpect(jsonPath("$.goalQuantity").value(5000))
				.andExpect(jsonPath("$.totalItems").value(0))
				.andExpect(jsonPath("$.plannedQuantity").value(0))
				.andExpect(jsonPath("$.contributedQuantity").value(0));
	}

	@Test
	void createsCategoryAlsoInOpenEvent() throws Exception {
		create(saveEvent(EventStatus.ABERTO), "Salgados", "UNIT", 40).andExpect(status().isCreated());
	}

	@Test
	void repeatedNameInSameEventReturns409IgnoringCaseAndSpaces() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		create(event, "Bebidas", "MILLILITER", 5000).andExpect(status().isCreated());

		create(event, "Bebidas", "MILLILITER", 1)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe uma categoria com esse nome neste evento."));
		create(event, "bebidas", "UNIT", 1).andExpect(status().isConflict());
		create(event, "  BEBIDAS  ", "UNIT", 1).andExpect(status().isConflict());
	}

	@Test
	void sameNameInAnotherEventIsAccepted() throws Exception {
		create(saveEvent(EventStatus.RASCUNHO), "Bebidas", "MILLILITER", 1).andExpect(status().isCreated());
		create(saveEvent(EventStatus.RASCUNHO), "Bebidas", "MILLILITER", 1).andExpect(status().isCreated());
	}

	@Test
	void zeroGoalIsAcceptedButNegativeReturns400() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);

		create(event, "Sem meta", "UNIT", 0).andExpect(status().isCreated());
		create(event, "Meta negativa", "UNIT", -1)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("goalQuantity"));
	}

	@Test
	void invalidFieldsReturn400() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);

		mvc.perform(post(base(event)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field=='name')].message").value("não deve estar em branco"))
				.andExpect(jsonPath("$.errors[?(@.field=='unit')].message").value("não deve ser nulo"))
				.andExpect(jsonPath("$.errors[?(@.field=='goalQuantity')].message").value("não deve ser nulo"));
		create(event, "Bebidas", "LITRO", 1).andExpect(status().isBadRequest());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(post("/api/events/999999/categories").contentType(MediaType.APPLICATION_JSON)
						.content(body("Bebidas", "UNIT", 1)))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/events/999999/categories")).andExpect(status().isNotFound());
	}

	@Test
	void menuChangesOnlyInDraftAndOpen() throws Exception {
		for (EventStatus status : new EventStatus[] { EventStatus.ENCERRADO, EventStatus.REALIZADO,
				EventStatus.CANCELADO }) {
			Event event = saveEvent(status);

			create(event, "Bebidas", "UNIT", 1)
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value(
							"Não é possível alterar o cardápio de um evento " + status.getDescription().toLowerCase() + "."));
		}
	}

	@Test
	void openEventWithExpiredDeadlineAlreadyCountsAsClosed() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		event.setChangeDeadline(LocalDateTime.parse("2026-10-30T12:00:00"));
		events.saveAndFlush(event);

		create(event, "Bebidas", "UNIT", 1)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível alterar o cardápio de um evento encerrado."));
	}

	@Test
	void listsInCreationOrderEvenInClosedEvent() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		create(event, "Zebra", "UNIT", 1);
		create(event, "Abacate", "UNIT", 1);
		event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(event);

		mvc.perform(get(base(event)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", contains("Zebra", "Abacate")));
	}

	@Test
	void totalsSumItemsAndContributionsOfOwnCategoryOnly() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventCategory drinks = saveCategory(event, "Bebidas", MeasureUnit.MILLILITER);
		EventCategory empty = saveCategory(event, "Doces", MeasureUnit.UNIT);
		EventItem juice = saveItem(event, drinks, "Suco", 10);
		EventItem water = saveItem(event, drinks, "Água", 5);
		EventParticipant ana = saveParticipant(event, "Ana", "52998224725");
		EventParticipant beto = saveParticipant(event, "Beto", "39053344705");
		em.persist(new Contribution(ana, juice, 4, null));
		em.persist(new Contribution(beto, juice, 3, null));
		em.persist(new Contribution(ana, water, 2, null));
		em.flush();
		em.clear();

		mvc.perform(get(base(event) + "/" + drinks.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(2))
				.andExpect(jsonPath("$.plannedQuantity").value(15))
				.andExpect(jsonPath("$.contributedQuantity").value(9));
		mvc.perform(get(base(event) + "/" + empty.getId()))
				.andExpect(jsonPath("$.totalItems").value(0))
				.andExpect(jsonPath("$.plannedQuantity").value(0))
				.andExpect(jsonPath("$.contributedQuantity").value(0));
	}

	@Test
	void categoryOfAnotherEventReturns404() throws Exception {
		Event owner = saveEvent(EventStatus.RASCUNHO);
		Event other = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(owner, "Bebidas", MeasureUnit.UNIT);

		mvc.perform(get(base(other) + "/" + category.getId())).andExpect(status().isNotFound());
		update(other, category, "X", "UNIT", 1).andExpect(status().isNotFound());
		mvc.perform(delete(base(other) + "/" + category.getId())).andExpect(status().isNotFound());
		mvc.perform(get(base(owner) + "/" + category.getId())).andExpect(status().isOk());
	}

	@Test
	void editsNameAndGoal() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.MILLILITER);

		update(event, category, "Bebidas geladas", "MILLILITER", 8000)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Bebidas geladas"))
				.andExpect(jsonPath("$.goalQuantity").value(8000));
	}

	@Test
	void categoryCanChangeOnlyCaseOfOwnName() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.MILLILITER);

		update(event, category, "bebidas", "MILLILITER", 1)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("bebidas"));
	}

	@Test
	void renamingToNameOfAnotherCategoryReturns409() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		saveCategory(event, "Bebidas", MeasureUnit.MILLILITER);
		EventCategory sweets = saveCategory(event, "Doces", MeasureUnit.UNIT);

		update(event, sweets, "BEBIDAS", "UNIT", 1).andExpect(status().isConflict());
	}

	@Test
	void unitChangesFreelyInEmptyCategory() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.MILLILITER);

		update(event, category, "Bebidas", "UNIT", 10)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unit").value("UNIT"));
	}

	@Test
	void unitStaysLockedWhileCategoryHasItems() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Farinhas", MeasureUnit.GRAM);
		saveItem(event, category, "Farinha de trigo", 500);

		update(event, category, "Farinhas", "MILLILITER", 1)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("A unidade não pode mudar enquanto a categoria tem itens."));
		update(event, category, "Farinhas e grãos", "GRAM", 2000)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Farinhas e grãos"))
				.andExpect(jsonPath("$.unit").value("GRAM"));
	}

	@Test
	void doesNotEditCategoryOfClosedEvent() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(event);

		update(event, category, "Outro", "UNIT", 1).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void deletesEmptyCategory() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);

		mvc.perform(delete(base(event) + "/" + category.getId())).andExpect(status().isNoContent());
		mvc.perform(get(base(event) + "/" + category.getId())).andExpect(status().isNotFound());
	}

	@Test
	void doesNotDeleteCategoryThatStillHasItems() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		saveItem(event, category, "Suco", 10);

		mvc.perform(delete(base(event) + "/" + category.getId()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível excluir uma categoria que ainda tem itens."));
	}

	@Test
	void doesNotDeleteCategoryOfClosedEvent() throws Exception {
		Event event = saveEvent(EventStatus.RASCUNHO);
		EventCategory category = saveCategory(event, "Bebidas", MeasureUnit.UNIT);
		event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(event);

		mvc.perform(delete(base(event) + "/" + category.getId())).andExpect(status().isUnprocessableEntity());
	}

	private String base(Event event) {
		return "/api/events/" + event.getId() + "/categories";
	}

	private ResultActions create(Event event, String name, String unit, int goal) throws Exception {
		return mvc.perform(post(base(event)).contentType(MediaType.APPLICATION_JSON).content(body(name, unit, goal)));
	}

	private ResultActions update(Event event, EventCategory category, String name, String unit, int goal)
			throws Exception {
		return mvc.perform(put(base(event) + "/" + category.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(body(name, unit, goal)));
	}

	private String body(String name, String unit, int goal) {
		return "{\"name\":\"" + name + "\",\"unit\":\"" + unit + "\",\"goalQuantity\":" + goal + "}";
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

}
