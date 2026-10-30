package br.com.sulwork.events.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestcontainersConfiguration;

/**
 * The schema CHECK constraints repeat the enum lists. These tests break if one list changes without the other.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class EnumsMatchSchemaTest {

	@Autowired
	private JdbcTemplate jdbc;

	@ParameterizedTest
	@EnumSource(EventType.class)
	void databaseAcceptsEveryEventType(EventType type) {
		assertThatCode(() -> jdbc.update("""
				INSERT INTO events (name, type, date_time, change_deadline, time_zone, status)
				VALUES ('Evento', ?, '2026-12-01 12:00', '2026-11-30 12:00', 'America/Sao_Paulo', 'ABERTO')""", type.name()))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(EventStatus.class)
	void databaseAcceptsEveryStatus(EventStatus status) {
		assertThatCode(() -> jdbc.update("""
				INSERT INTO events (name, type, date_time, change_deadline, time_zone, status)
				VALUES ('Evento', 'LUNCH', '2026-12-01 12:00', '2026-11-30 12:00', 'America/Sao_Paulo', ?)""", status.name()))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(MeasureUnit.class)
	void databaseAcceptsEveryUnit(MeasureUnit unit) {
		long event = insertEvent();

		assertThatCode(() -> jdbc.update("""
				INSERT INTO event_categories (event_id, name, unit) VALUES (?, 'Categoria', ?)""",
				event, unit.name())).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(Allergen.class)
	void databaseAcceptsEveryAllergen(Allergen allergen) {
		long item = insertItem();

		assertThatCode(() -> jdbc.update("INSERT INTO item_allergens (item_id, allergen) VALUES (?, ?)",
				item, allergen.name())).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(FoodCharacteristic.class)
	void databaseAcceptsEveryCharacteristic(FoodCharacteristic characteristic) {
		long item = insertItem();

		assertThatCode(() -> jdbc.update("INSERT INTO item_characteristics (item_id, characteristic) VALUES (?, ?)",
				item, characteristic.name())).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(DietaryRestriction.class)
	void databaseAcceptsEveryRestriction(DietaryRestriction restriction) {
		long collaborator = jdbc.queryForObject(
				"INSERT INTO collaborators (name, cpf) VALUES ('Ana', '52998224725') RETURNING id", Long.class);

		assertThatCode(() -> jdbc.update(
				"INSERT INTO collaborator_restrictions (collaborator_id, restriction) VALUES (?, ?)",
				collaborator, restriction.name())).doesNotThrowAnyException();
	}

	@Test
	void characteristicIsNotAcceptedAsAllergen() {
		long item = insertItem();

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO item_allergens (item_id, allergen) VALUES (?, 'HIGH_SODIUM')", item))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private long insertEvent() {
		return jdbc.queryForObject("""
				INSERT INTO events (name, type, date_time, change_deadline, time_zone, status)
				VALUES ('Evento', 'LUNCH', '2026-12-01 12:00', '2026-11-30 12:00', 'America/Sao_Paulo', 'ABERTO')
				RETURNING id""", Long.class);
	}

	private long insertItem() {
		long event = insertEvent();
		long category = jdbc.queryForObject("""
				INSERT INTO event_categories (event_id, name, unit) VALUES (?, 'Categoria', 'UNIT')
				RETURNING id""", Long.class, event);
		return jdbc.queryForObject("""
				INSERT INTO event_items (event_id, category_id, name, required_quantity)
				VALUES (?, ?, 'Item', 5) RETURNING id""", Long.class, event, category);
	}

}
