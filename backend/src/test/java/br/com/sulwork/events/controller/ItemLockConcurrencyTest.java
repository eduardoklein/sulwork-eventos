package br.com.sulwork.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.dto.CreateContributionRequest;
import br.com.sulwork.events.repository.CollaboratorRepository;
import br.com.sulwork.events.repository.EventCategoryRepository;
import br.com.sulwork.events.repository.EventItemRepository;
import br.com.sulwork.events.repository.EventParticipantRepository;
import br.com.sulwork.events.repository.EventRepository;
import br.com.sulwork.events.service.ContributionService;

/**
 * Without {@code @Transactional}: a real contribution request is in the middle of its transaction (it already
 * locked the item and inserted the contribution) while the organizer edits or deletes the item.
 */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
class ItemLockConcurrencyTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ContributionService contributionService;

	@Autowired
	private EventRepository events;

	@Autowired
	private EventCategoryRepository categories;

	@Autowired
	private EventItemRepository items;

	@Autowired
	private CollaboratorRepository collaborators;

	@Autowired
	private EventParticipantRepository participants;

	private Event event;
	private EventItem item;
	private EventParticipant participant;

	@BeforeEach
	void setUp() {
		LocalDateTime date = LocalDateTime.parse("2026-12-01T12:00:00");
		event = new Event("Evento", null, EventType.LUNCH, date, date.minusDays(1), ZoneId.of("America/Sao_Paulo"));
		event.setStatus(EventStatus.ABERTO);
		events.save(event);
		EventCategory category = categories.save(new EventCategory(event, "Bebidas", MeasureUnit.UNIT, 0));
		item = items.save(new EventItem(event, category, "Suco", 10, false));
		Collaborator ana = collaborators.save(new Collaborator("Ana", "39053344705"));
		participant = participants.save(new EventParticipant(event, ana));
	}

	@AfterEach
	void cleanDatabase() {
		for (String table : List.of("contributions", "event_participants", "item_allergens", "item_characteristics",
				"event_items", "event_categories", "collaborator_restrictions", "collaborators", "events")) {
			jdbc.update("DELETE FROM " + table);
		}
	}

	@Test
	void reducingTheItemWhileAContributionIsInFlightCountsThatContribution() throws Exception {
		Thread other = UncommittedTransaction.hold(transactionManager, () -> contribute(8));

		mvc.perform(put(itemPath()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Suco\",\"requiredQuantity\":5}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail")
						.value("A quantidade necessária não pode ficar abaixo do que já foi contribuído (8)."));
		other.join();

		assertThat(requiredQuantity()).isEqualTo(10);
		assertThat(contributedSum()).isEqualTo(8);
	}

	@Test
	void deletingTheItemWhileAContributionIsInFlightKeepsTheContribution() throws Exception {
		Thread other = UncommittedTransaction.hold(transactionManager, () -> contribute(3));

		mvc.perform(delete(itemPath()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível excluir um item que já tem contribuições."));
		other.join();

		assertThat(contributedSum()).isEqualTo(3);
	}

	private void contribute(int quantity) {
		contributionService.create(event.getId(),
				new CreateContributionRequest(participant.getId(), item.getId(), quantity, null));
	}

	private String itemPath() {
		return "/api/events/" + event.getId() + "/items/" + item.getId();
	}

	private int requiredQuantity() {
		return jdbc.queryForObject("SELECT required_quantity FROM event_items WHERE id = ?", Integer.class,
				item.getId());
	}

	private int contributedSum() {
		return jdbc.queryForObject("SELECT coalesce(sum(quantity), 0) FROM contributions WHERE item_id = ?",
				Integer.class, item.getId());
	}

}
