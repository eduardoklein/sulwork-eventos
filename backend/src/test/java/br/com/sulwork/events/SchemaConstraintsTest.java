package br.com.sulwork.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SchemaConstraintsTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void sameCollaboratorInDifferentEventsIsAccepted() {
		long collaborator = insertCollaborator("52998224725");

		assertThatCode(() -> {
			insertParticipant(insertEvent(), collaborator);
			insertParticipant(insertEvent(), collaborator);
		}).doesNotThrowAnyException();
	}

	@Test
	void sameCollaboratorTwiceInSameEventIsRejected() {
		long event = insertEvent();
		long collaborator = insertCollaborator("52998224725");
		insertParticipant(event, collaborator);

		assertThatThrownBy(() -> insertParticipant(event, collaborator))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void duplicateCpfIsRejected() {
		insertCollaborator("52998224725");

		assertThatThrownBy(() -> insertCollaborator("52998224725"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cpfWithMaskIsRejected() {
		assertThatThrownBy(() -> insertCollaborator("529.982.247-25"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cpfWithFewerThanElevenDigitsIsRejected() {
		assertThatThrownBy(() -> insertCollaborator("5299822472"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void statusOutsideListIsRejected() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO events (name, type, date_time, change_deadline, time_zone, status)
				VALUES ('Almoço', 'LUNCH', '2026-12-01 12:00', '2026-11-30 12:00', 'America/Sao_Paulo', 'EXPLODIDO')"""))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deadlineAfterEventDateIsRejected() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO events (name, type, date_time, change_deadline, time_zone, status)
				VALUES ('Almoço', 'LUNCH', '2026-12-01 12:00', '2026-12-02 12:00', 'America/Sao_Paulo', 'ABERTO')"""))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void repeatedCategoryNameInSameEventIsRejectedIgnoringCase() {
		long event = insertEvent();
		insertCategory(event, "Bebidas");

		assertThatThrownBy(() -> insertCategory(event, "bebidas"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void sameCategoryNameInDifferentEventsIsAccepted() {
		insertCategory(insertEvent(), "Bebidas");

		assertThatCode(() -> insertCategory(insertEvent(), "Bebidas")).doesNotThrowAnyException();
	}

	@Test
	void requiredQuantityZeroIsRejected() {
		long event = insertEvent();
		long category = insertCategory(event, "Bebidas");

		assertThatThrownBy(() -> insertItem(event, category, 0))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void repeatedItemNameInSameCategoryIsRejectedIgnoringCase() {
		long event = insertEvent();
		long category = insertCategory(event, "Bebidas");
		insertItem(event, category, 5);

		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO event_items (event_id, category_id, name, required_quantity)
				VALUES (?, ?, 'SUCO', 3)""", event, category))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void sameItemNameInDifferentCategoriesIsAccepted() {
		long event = insertEvent();

		assertThatCode(() -> {
			insertItem(event, insertCategory(event, "Bebidas"), 5);
			insertItem(event, insertCategory(event, "Sobremesas"), 5);
		}).doesNotThrowAnyException();
	}

	@Test
	void itemWithoutCategoryIsRejected() {
		long event = insertEvent();

		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO event_items (event_id, category_id, name, required_quantity)
				VALUES (?, NULL, 'Suco', 5)""", event))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void contributionWithZeroQuantityIsRejected() {
		long event = insertEvent();
		long item = insertItem(event, insertCategory(event, "Bebidas"), 5);
		long participant = insertParticipant(event, insertCollaborator("52998224725"));

		assertThatThrownBy(() -> insertContribution(participant, item, 0))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void samePersonContributingTwiceToSameItemIsRejected() {
		long event = insertEvent();
		long item = insertItem(event, insertCategory(event, "Bebidas"), 5);
		long participant = insertParticipant(event, insertCollaborator("52998224725"));
		insertContribution(participant, item, 2);

		assertThatThrownBy(() -> insertContribution(participant, item, 1))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingCategoryThatStillHasItemsIsBlocked() {
		long event = insertEvent();
		long category = insertCategory(event, "Bebidas");
		insertItem(event, category, 5);

		assertThatThrownBy(() -> jdbc.update("DELETE FROM event_categories WHERE id = ?", category))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingEventCascadesEverythingThatDependsOnIt() {
		long event = insertEvent();
		long item = insertItem(event, insertCategory(event, "Bebidas"), 5);
		long participant = insertParticipant(event, insertCollaborator("52998224725"));
		insertContribution(participant, item, 2);

		jdbc.update("DELETE FROM events WHERE id = ?", event);

		assertThat(countRows("event_categories")).isZero();
		assertThat(countRows("event_items")).isZero();
		assertThat(countRows("event_participants")).isZero();
		assertThat(countRows("contributions")).isZero();
		assertThat(countRows("collaborators")).isEqualTo(1);
	}

	private long insertEvent() {
		return jdbc.queryForObject("""
				INSERT INTO events (name, type, date_time, change_deadline, time_zone, status)
				VALUES ('Almoço', 'LUNCH', '2026-12-01 12:00', '2026-11-30 12:00', 'America/Sao_Paulo', 'ABERTO')
				RETURNING id""", Long.class);
	}

	private long insertCategory(long event, String name) {
		return jdbc.queryForObject("""
				INSERT INTO event_categories (event_id, name, unit, goal_quantity)
				VALUES (?, ?, 'UNIT', 10) RETURNING id""", Long.class, event, name);
	}

	private long insertItem(long event, long category, int quantity) {
		return jdbc.queryForObject("""
				INSERT INTO event_items (event_id, category_id, name, required_quantity)
				VALUES (?, ?, 'Suco', ?) RETURNING id""", Long.class, event, category, quantity);
	}

	private long insertCollaborator(String cpf) {
		return jdbc.queryForObject("""
				INSERT INTO collaborators (name, cpf) VALUES ('Ana', ?) RETURNING id""", Long.class, cpf);
	}

	private long insertParticipant(long event, long collaborator) {
		return jdbc.queryForObject("""
				INSERT INTO event_participants (event_id, collaborator_id)
				VALUES (?, ?) RETURNING id""", Long.class, event, collaborator);
	}

	private void insertContribution(long participant, long item, int quantity) {
		jdbc.update("""
				INSERT INTO contributions (participant_id, item_id, quantity)
				VALUES (?, ?, ?)""", participant, item, quantity);
	}

	private int countRows(String table) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
	}

}
