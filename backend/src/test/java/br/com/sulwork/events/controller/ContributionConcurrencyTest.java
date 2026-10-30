package br.com.sulwork.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sulwork.events.TestCpfs;
import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.repository.EventCategoryRepository;
import br.com.sulwork.events.repository.CollaboratorRepository;
import br.com.sulwork.events.repository.EventRepository;
import br.com.sulwork.events.repository.EventItemRepository;
import br.com.sulwork.events.repository.EventParticipantRepository;

/**
 * Without {@code @Transactional}: each request runs in its own transaction, like real HTTP requests, and the setup
 * data stays saved in the database.
 */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
class ContributionConcurrencyTest {

	private static final int PEOPLE = 20;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

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

	@AfterEach
	void cleanDatabase() {
		for (String table : List.of("contributions", "event_participants", "item_allergens", "item_characteristics",
				"event_items", "event_categories", "collaborator_restrictions", "collaborators", "events")) {
			jdbc.update("DELETE FROM " + table);
		}
	}

	@Test
	void twentyPeopleCompetingForOneUnitOnlyOneStays() throws Exception {
		EventItem item = itemWithQuantity(1);
		List<EventParticipant> people = enroll(item.getEvent(), PEOPLE);

		List<Integer> responses = contributeAtTheSameTime(item, people, 1);

		assertThat(responses).filteredOn(s -> s == 201).hasSize(1);
		assertThat(responses).filteredOn(s -> s == 422).hasSize(PEOPLE - 1);
		assertThat(contributionsSum(item)).isEqualTo(1);
		assertThat(contributionsCount(item)).isEqualTo(1);
	}

	@Test
	void twentyPeopleCompetingForTenUnitsAcceptsExactlyTen() throws Exception {
		EventItem item = itemWithQuantity(10);
		List<EventParticipant> people = enroll(item.getEvent(), PEOPLE);

		List<Integer> responses = contributeAtTheSameTime(item, people, 1);

		assertThat(responses).filteredOn(s -> s == 201).hasSize(10);
		assertThat(responses).filteredOn(s -> s == 422).hasSize(10);
		assertThat(contributionsSum(item)).isEqualTo(10);
	}

	@Test
	void contributionsLargerThanBalanceNeverExceedLimit() throws Exception {
		EventItem item = itemWithQuantity(10);
		List<EventParticipant> people = enroll(item.getEvent(), PEOPLE);

		List<Integer> responses = contributeAtTheSameTime(item, people, 3);

		assertThat(responses).filteredOn(s -> s == 201).hasSize(3);
		assertThat(contributionsSum(item)).isEqualTo(9);
	}

	@Test
	void samePersonClickingManyTimesAtOnceContributesOnlyOnce() throws Exception {
		EventItem item = itemWithQuantity(100);
		EventParticipant ana = enroll(item.getEvent(), 1).get(0);

		List<Integer> responses = contributeAtTheSameTime(item, java.util.Collections.nCopies(10, ana), 2);

		assertThat(responses).filteredOn(s -> s == 201).hasSize(1);
		assertThat(responses).filteredOn(s -> s == 409).hasSize(9);
		assertThat(contributionsSum(item)).isEqualTo(2);
	}

	private List<Integer> contributeAtTheSameTime(EventItem item, List<EventParticipant> people, int quantity)
			throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(people.size());
		CountDownLatch ready = new CountDownLatch(people.size());
		CountDownLatch startSignal = new CountDownLatch(1);
		try {
			List<Future<Integer>> futures = new ArrayList<>();
			for (EventParticipant person : people) {
				futures.add(pool.submit(() -> {
					ready.countDown();
					startSignal.await();
					return mvc.perform(post("/api/events/" + item.getEvent().getId() + "/contributions")
									.contentType(MediaType.APPLICATION_JSON)
									.content("{\"participantId\":" + person.getId() + ",\"itemId\":" + item.getId()
											+ ",\"quantity\":" + quantity + "}"))
							.andReturn().getResponse().getStatus();
				}));
			}
			ready.await();
			startSignal.countDown();
			List<Integer> responses = new ArrayList<>();
			for (Future<Integer> future : futures) {
				responses.add(future.get(30, TimeUnit.SECONDS));
			}
			return responses;
		}
		finally {
			pool.shutdownNow();
		}
	}

	private EventItem itemWithQuantity(int quantity) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(EventStatus.ABERTO);
		events.save(event);
		EventCategory category = categories.save(new EventCategory(event, "Bebidas", MeasureUnit.UNIT, 0));
		return items.save(new EventItem(event, category, "Suco", quantity, false));
	}

	private List<EventParticipant> enroll(Event event, int quantity) {
		List<EventParticipant> people = new ArrayList<>();
		for (int i = 0; i < quantity; i++) {
			Collaborator collaborator = collaborators.save(new Collaborator("Pessoa " + i, TestCpfs.validCpf(i)));
			people.add(participants.save(new EventParticipant(event, collaborator)));
		}
		return people;
	}

	private int contributionsSum(EventItem item) {
		return jdbc.queryForObject("SELECT coalesce(sum(quantity), 0) FROM contributions WHERE item_id = ?",
				Integer.class, item.getId());
	}

	private int contributionsCount(EventItem item) {
		return jdbc.queryForObject("SELECT count(*) FROM contributions WHERE item_id = ?", Integer.class, item.getId());
	}

}
