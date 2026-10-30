package br.com.sulwork.events.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CoveragePerCategoryQueryTest {

	@Autowired
	private EventItemRepository items;

	@Autowired
	private EntityManager em;

	private int cpfCounter = 0;

	@Test
	void sumsGoalItemsAndContributionsSeparatingMandatoryAndOptional() {
		Event event = event();
		EventCategory drinks = category(event, "Bebidas", MeasureUnit.MILLILITER, 5000);
		EventItem juice = item(event, drinks, "Suco", 2000, true);
		EventItem soda = item(event, drinks, "Refrigerante", 3000, false);
		EventParticipant ana = participant(event);
		EventParticipant beto = participant(event);
		contribution(ana, juice, 1500);
		contribution(beto, juice, 500);
		contribution(ana, soda, 1000);
		em.flush();

		List<CategoryCoverage> result = items.coveragePerCategory(event.getId());

		assertThat(result).hasSize(1);
		CategoryCoverage c = result.get(0);
		assertThat(c.getCategoryId()).isEqualTo(drinks.getId());
		assertThat(c.getName()).isEqualTo("Bebidas");
		assertThat(c.getUnit()).isEqualTo("MILLILITER");
		assertThat(c.getGoalQuantity()).isEqualTo(5000);
		assertThat(c.getTotalItems()).isEqualTo(2);
		assertThat(c.getMandatoryRequired()).isEqualTo(2000);
		assertThat(c.getOptionalRequired()).isEqualTo(3000);
		assertThat(c.getMandatoryContributed()).isEqualTo(2000);
		assertThat(c.getOptionalContributed()).isEqualTo(1000);
	}

	@Test
	void itemWithManyContributionsDoesNotMultiplyRequiredQuantity() {
		Event event = event();
		EventCategory category = category(event, "Doces", MeasureUnit.UNIT, 0);
		EventItem brigadeiro = item(event, category, "Brigadeiro", 100, true);
		for (int i = 0; i < 4; i++) {
			contribution(participant(event), brigadeiro, 10);
		}
		em.flush();

		CategoryCoverage c = items.coveragePerCategory(event.getId()).get(0);

		assertThat(c.getMandatoryRequired()).isEqualTo(100);
		assertThat(c.getMandatoryContributed()).isEqualTo(40);
		assertThat(c.getTotalItems()).isEqualTo(1);
	}

	@Test
	void itemWithoutContributionsCountsAsRequiredAndZeroContributed() {
		Event event = event();
		EventCategory category = category(event, "Salgados", MeasureUnit.UNIT, 0);
		item(event, category, "Coxinha", 50, true);
		em.flush();

		CategoryCoverage c = items.coveragePerCategory(event.getId()).get(0);

		assertThat(c.getMandatoryRequired()).isEqualTo(50);
		assertThat(c.getMandatoryContributed()).isZero();
		assertThat(c.getOptionalRequired()).isZero();
		assertThat(c.getOptionalContributed()).isZero();
	}

	@Test
	void categoryWithoutItemsAppearsWithZeros() {
		Event event = event();
		category(event, "Vazia", MeasureUnit.UNIT, 20);
		em.flush();

		CategoryCoverage c = items.coveragePerCategory(event.getId()).get(0);

		assertThat(c.getName()).isEqualTo("Vazia");
		assertThat(c.getGoalQuantity()).isEqualTo(20);
		assertThat(c.getTotalItems()).isZero();
		assertThat(c.getMandatoryRequired()).isZero();
		assertThat(c.getOptionalContributed()).isZero();
	}

	@Test
	void sumsEachCategorySeparatelyInCreationOrder() {
		Event event = event();
		EventCategory first = category(event, "Zebra", MeasureUnit.UNIT, 0);
		EventCategory second = category(event, "Abacate", MeasureUnit.GRAM, 0);
		EventItem a = item(event, first, "A", 10, true);
		EventItem b = item(event, second, "B", 300, true);
		contribution(participant(event), a, 10);
		contribution(participant(event), b, 100);
		em.flush();

		List<CategoryCoverage> result = items.coveragePerCategory(event.getId());

		assertThat(result).extracting(CategoryCoverage::getName).containsExactly("Zebra", "Abacate");
		assertThat(result).extracting(CategoryCoverage::getMandatoryContributed).containsExactly(10L, 100L);
		assertThat(result).extracting(CategoryCoverage::getMandatoryRequired).containsExactly(10L, 300L);
	}

	@Test
	void doesNotMixCategoriesOfAnotherEvent() {
		Event event = event();
		Event other = event();
		category(event, "Minha", MeasureUnit.UNIT, 0);
		EventCategory theirs = category(other, "Dela", MeasureUnit.UNIT, 0);
		item(other, theirs, "Item", 99, true);
		em.flush();

		List<CategoryCoverage> result = items.coveragePerCategory(event.getId());

		assertThat(result).extracting(CategoryCoverage::getName).containsExactly("Minha");
		assertThat(result.get(0).getMandatoryRequired()).isZero();
	}

	@Test
	void eventWithoutCategoriesReturnsEmptyList() {
		Event event = event();
		em.flush();

		assertThat(items.coveragePerCategory(event.getId())).isEmpty();
		assertThat(items.coveragePerCategory(999_999L)).isEmpty();
	}

	private Event event() {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(EventStatus.ABERTO);
		em.persist(event);
		return event;
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
		int number = cpfCounter++;
		Collaborator collaborator = new Collaborator("Pessoa " + number, TestCpfs.validCpf(number));
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		return participant;
	}

	private void contribution(EventParticipant participant, EventItem item, int quantity) {
		em.persist(new Contribution(participant, item, quantity, null));
	}

}
