package br.com.sulwork.events.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class JpaMappingTest {

	private static final ZoneId TIME_ZONE = ZoneId.of("America/Sao_Paulo");

	@Autowired
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void savesAndReadsBackWholeEntityGraph() {
		Event event = new Event("Almoço", "Fim de ano", EventType.LUNCH,
				LocalDateTime.of(2026, 12, 1, 12, 0), LocalDateTime.of(2026, 11, 30, 12, 0), TIME_ZONE);
		em.persist(event);
		EventCategory category = new EventCategory(event, "Bebidas", MeasureUnit.MILLILITER, 5000);
		em.persist(category);
		EventItem item = new EventItem(event, category, "Suco de laranja", 2000, true);
		item.getAllergens().add(Allergen.LACTOSE);
		item.getCharacteristics().add(FoodCharacteristic.ADDED_SUGAR);
		em.persist(item);
		Collaborator collaborator = new Collaborator("Ana", "52998224725");
		collaborator.getRestrictions().add(DietaryRestriction.VEGAN);
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		em.persist(new Contribution(participant, item, 1000, "Levo na garrafa"));
		em.flush();
		em.clear();

		Event loadedEvent = em.find(Event.class, event.getId());
		assertThat(loadedEvent.getStatus()).isEqualTo(EventStatus.RASCUNHO);
		assertThat(loadedEvent.getType()).isEqualTo(EventType.LUNCH);
		assertThat(loadedEvent.getTimeZone()).isEqualTo(TIME_ZONE);
		assertThat(loadedEvent.getCreatedAt()).isNotNull();
		assertThat(loadedEvent.getUpdatedAt()).isNotNull();

		EventItem loadedItem = em.find(EventItem.class, item.getId());
		assertThat(loadedItem.getAllergens()).containsExactly(Allergen.LACTOSE);
		assertThat(loadedItem.getCharacteristics()).containsExactly(FoodCharacteristic.ADDED_SUGAR);
		assertThat(loadedItem.getUnit()).isEqualTo(MeasureUnit.MILLILITER);
		assertThat(loadedItem.isMandatory()).isTrue();

		Collaborator loadedCollaborator = em.find(Collaborator.class, collaborator.getId());
		assertThat(loadedCollaborator.getCpf()).isEqualTo("52998224725");
		assertThat(loadedCollaborator.getRestrictions()).containsExactly(DietaryRestriction.VEGAN);

		Contribution loadedContribution = em.find(Contribution.class, em
				.createQuery("select c.id from Contribution c", Long.class).getSingleResult());
		assertThat(loadedContribution.getQuantity()).isEqualTo(1000);
		assertThat(loadedContribution.getNote()).isEqualTo("Levo na garrafa");
		assertThat(loadedContribution.getParticipant().getCollaborator().getName()).isEqualTo("Ana");
	}

	@Test
	void enumsAreStoredAsText() {
		Event event = new Event("Lanche", null, EventType.BREAKFAST,
				LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 11, 30, 9, 0), TIME_ZONE);
		event.setStatus(EventStatus.ABERTO);
		em.persist(event);
		em.flush();

		String type = jdbc.queryForObject("SELECT type FROM events WHERE id = ?", String.class, event.getId());
		String status = jdbc.queryForObject("SELECT status FROM events WHERE id = ?", String.class, event.getId());

		assertThat(type).isEqualTo("BREAKFAST");
		assertThat(status).isEqualTo("ABERTO");
	}

	@Test
	void updatingEventAdvancesUpdatedAtAndKeepsCreatedAt() throws InterruptedException {
		Event event = new Event("Lanche", null, EventType.SNACK,
				LocalDateTime.of(2026, 12, 1, 15, 0), LocalDateTime.of(2026, 11, 30, 15, 0), TIME_ZONE);
		em.persist(event);
		em.flush();
		LocalDateTime createdAt = event.getCreatedAt();
		LocalDateTime updatedBefore = event.getUpdatedAt();

		Thread.sleep(5);
		event.setName("Lanche da tarde");
		em.flush();

		assertThat(event.getCreatedAt()).isEqualTo(createdAt);
		assertThat(event.getUpdatedAt()).isAfter(updatedBefore);
	}

	@Test
	void repeatedCpfIsRejectedOnSave() {
		em.persist(new Collaborator("Ana", "52998224725"));
		em.flush();

		assertThatThrownBy(() -> {
			em.persist(new Collaborator("Outra Ana", "52998224725"));
			em.flush();
		}).isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("uq_collaborators_cpf");
	}

}
