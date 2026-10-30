package br.com.sulwork.events.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.repository.EventCategoryRepository;
import br.com.sulwork.events.repository.EventRepository;

/**
 * Without {@code @Transactional}: another transaction holds an uncommitted change to the category or its items
 * while the API request runs, like two people using the system at the same time.
 */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
class CatalogConcurrencyTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private EventRepository events;

	@Autowired
	private EventCategoryRepository categories;

	@AfterEach
	void cleanDatabase() {
		for (String table : List.of("contributions", "event_participants", "item_allergens", "item_characteristics",
				"event_items", "event_categories", "collaborator_restrictions", "collaborators", "events")) {
			jdbc.update("DELETE FROM " + table);
		}
	}

	@Test
	void deletingACategoryWhileAnItemIsBeingAddedToItIsRefused() throws Exception {
		EventCategory category = category();
		Thread other = UncommittedTransaction.hold(transactionManager, () -> insertItem(category, "Suco"));

		mvc.perform(delete(categoryPath(category)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível excluir uma categoria que ainda tem itens."));
		other.join();
	}

	@Test
	void changingTheUnitWhileAnItemIsBeingAddedIsRefused() throws Exception {
		EventCategory category = category();
		Thread other = UncommittedTransaction.hold(transactionManager, () -> insertItem(category, "Suco"));

		mvc.perform(put(categoryPath(category)).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Bebidas\",\"unit\":\"MILLILITER\",\"goalQuantity\":0}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("A unidade não pode mudar enquanto a categoria tem itens."));
		other.join();
	}

	@Test
	void addingAnItemWhileItsCategoryIsBeingDeletedSaysTheCategoryIsGone() throws Exception {
		EventCategory category = category();
		Thread other = UncommittedTransaction.hold(transactionManager, () -> jdbc.update("DELETE FROM event_categories WHERE id = ?", category.getId()));

		mvc.perform(post("/api/events/" + category.getEvent().getId() + "/items").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Suco\",\"categoryId\":" + category.getId() + ",\"requiredQuantity\":5}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Categoria não encontrada neste evento."));
		other.join();
	}

	private void insertItem(EventCategory category, String name) {
		jdbc.update("""
				INSERT INTO event_items (event_id, category_id, name, required_quantity, mandatory)
				VALUES (?, ?, ?, 5, false)""", category.getEvent().getId(), category.getId(), name);
	}

	private String categoryPath(EventCategory category) {
		return "/api/events/" + category.getEvent().getId() + "/categories/" + category.getId();
	}

	private EventCategory category() {
		LocalDateTime date = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, date, date.minusDays(1), ZoneId.of("America/Sao_Paulo"));
		event.setStatus(EventStatus.RASCUNHO);
		events.save(event);
		return categories.save(new EventCategory(event, "Bebidas", MeasureUnit.UNIT, 0));
	}

}
