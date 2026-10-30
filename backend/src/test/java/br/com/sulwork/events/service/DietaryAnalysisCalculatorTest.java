package br.com.sulwork.events.service;

import static br.com.sulwork.events.domain.Allergen.EGG;
import static br.com.sulwork.events.domain.Allergen.GLUTEN;
import static br.com.sulwork.events.domain.Allergen.LACTOSE;
import static br.com.sulwork.events.domain.DietaryRestriction.CELIAC_GLUTEN_FREE;
import static br.com.sulwork.events.domain.DietaryRestriction.DIABETIC_NO_SUGAR;
import static br.com.sulwork.events.domain.DietaryRestriction.EGG_ALLERGY;
import static br.com.sulwork.events.domain.DietaryRestriction.LACTOSE_INTOLERANCE;
import static br.com.sulwork.events.domain.DietaryRestriction.VEGAN;
import static br.com.sulwork.events.domain.FoodCharacteristic.ADDED_SUGAR;
import static br.com.sulwork.events.domain.FoodCharacteristic.ANIMAL_MEAT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.com.sulwork.events.domain.AlertLevel;
import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.ItemTraits;
import br.com.sulwork.events.domain.ParticipantDiet;
import br.com.sulwork.events.dto.DietaryAlertResponse;
import br.com.sulwork.events.dto.DietaryAnalysisResponse;
import br.com.sulwork.events.dto.ParticipantDietResponse;

class DietaryAnalysisCalculatorTest {

	@Test
	void celiacPersonWithAnAllGlutenMenuGetsACriticalAlert() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", CELIAC_GLUTEN_FREE)),
				List.of(withAllergens(1, GLUTEN), withAllergens(2, GLUTEN), withAllergens(3, GLUTEN)));

		assertThat(result.alerts()).hasSize(1);
		DietaryAlertResponse alert = result.alerts().get(0);
		assertThat(alert.level()).isEqualTo(AlertLevel.CRITICAL);
		assertThat(alert.message()).isEqualTo("Ana não tem nenhum item seguro no cardápio.");
		assertThat(alert.participantId()).isEqualTo(1L);
		ParticipantDietResponse ana = result.participants().get(0);
		assertThat(ana.safeItems()).isZero();
		assertThat(ana.unsafeItems()).hasSize(3);
		assertThat(ana.unsafeItems().get(0).conflictingAllergens()).containsExactly(GLUTEN);
	}

	@Test
	void warnsWhenLessThan35PercentOfTheMenuIsSafe() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", CELIAC_GLUTEN_FREE)), menu(20, 6));

		assertThat(result.alerts()).hasSize(1);
		assertThat(result.alerts().get(0).level()).isEqualTo(AlertLevel.WARNING);
		assertThat(result.alerts().get(0).message())
				.isEqualTo("Menos de 35% do cardápio é seguro para Ana (6 de 20 itens).");
	}

	@Test
	void exactly35PercentSafeIsNotAWarning() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", CELIAC_GLUTEN_FREE)), menu(20, 7));

		assertThat(result.alerts()).extracting(DietaryAlertResponse::level).containsExactly(AlertLevel.INFO);
	}

	@Test
	void inclusiveMenuProducesOneInformativeAlertAboutTheWholeEvent() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", CELIAC_GLUTEN_FREE)), menu(4, 3));

		assertThat(result.alerts()).hasSize(1);
		assertThat(result.alerts().get(0).level()).isEqualTo(AlertLevel.INFO);
		assertThat(result.alerts().get(0).message()).isEqualTo("Cardápio inclusivo: toda pessoa com restrição tem itens seguros.");
		assertThat(result.alerts().get(0).participantId()).isNull();
	}

	@Test
	void noAlertsWhenNobodyHasARestriction() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana"), person(2, "Beto")), menu(4, 1));

		assertThat(result.alerts()).isEmpty();
		assertThat(result.restrictions()).isEmpty();
	}

	@Test
	void noAlertsForAnEmptyMenuEvenWithRestrictedPeople() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", CELIAC_GLUTEN_FREE)), List.of());

		assertThat(result.totalItems()).isZero();
		assertThat(result.alerts()).isEmpty();
		assertThat(result.participants().get(0).safeItems()).isZero();
	}

	@Test
	void peopleWithoutRestrictionsAppearWithEveryItemSafe() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Beto")), List.of(withAllergens(1, GLUTEN), withAllergens(2, LACTOSE)));

		ParticipantDietResponse beto = result.participants().get(0);
		assertThat(beto.restrictions()).isEmpty();
		assertThat(beto.safeItems()).isEqualTo(2);
		assertThat(beto.unsafeItems()).isEmpty();
	}

	@Test
	void anItemWithNothingMarkedIsSafeForEveryone() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", VEGAN, CELIAC_GLUTEN_FREE)), List.of(item(1, Set.of(), Set.of())));

		assertThat(result.participants().get(0).safeItems()).isEqualTo(1);
	}

	@Test
	void severalRestrictionsOfOnePersonAddTheirConflicts() {
		ItemTraits lasagna = item(1, Set.of(GLUTEN, LACTOSE, EGG), Set.of());

		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", VEGAN, CELIAC_GLUTEN_FREE)), List.of(lasagna));

		assertThat(result.participants().get(0).unsafeItems().get(0).conflictingAllergens())
				.containsExactly(GLUTEN, LACTOSE, EGG);
	}

	@Test
	void characteristicConflictsAppearSeparatelyFromAllergens() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", DIABETIC_NO_SUGAR)),
				List.of(item(1, Set.of(LACTOSE), Set.of(ADDED_SUGAR))));

		var unsafe = result.participants().get(0).unsafeItems().get(0);
		assertThat(unsafe.conflictingAllergens()).isEmpty();
		assertThat(unsafe.conflictingCharacteristics()).containsExactly(ADDED_SUGAR);
	}

	@Test
	void restrictionSummaryCountsPeopleAndCompatibleItemsInEnumOrder() {
		List<ItemTraits> items = List.of(
				item(1, Set.of(GLUTEN), Set.of()),
				item(2, Set.of(LACTOSE), Set.of()),
				item(3, Set.of(), Set.of(ANIMAL_MEAT)),
				item(4, Set.of(), Set.of()));

		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", VEGAN), person(2, "Beto", VEGAN, CELIAC_GLUTEN_FREE), person(3, "Cida")), items);

		assertThat(result.restrictions()).extracting(r -> r.restriction())
				.containsExactly(CELIAC_GLUTEN_FREE, VEGAN);
		var celiac = result.restrictions().get(0);
		assertThat(celiac.peopleCount()).isEqualTo(1);
		assertThat(celiac.compatibleItems()).isEqualTo(3);
		assertThat(celiac.totalItems()).isEqualTo(4);
		assertThat(celiac.description()).isEqualTo("Celíaco (sem glúten)");
		var vegan = result.restrictions().get(1);
		assertThat(vegan.peopleCount()).isEqualTo(2);
		assertThat(vegan.compatibleItems()).isEqualTo(2);
	}

	@Test
	void criticalAlertsComeBeforeWarnings() {
		// Every item has gluten, and nine of ten also have egg: the celiac person has no safe item (critical),
		// the person allergic to egg has one of ten (warning). The warning person comes first in the input.
		List<ItemTraits> items = IntStream.range(0, 10)
				.mapToObj(i -> i == 0 ? withAllergens(i, GLUTEN) : withAllergens(i, GLUTEN, EGG))
				.toList();

		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Beto", EGG_ALLERGY), person(2, "Ana", CELIAC_GLUTEN_FREE)), items);

		assertThat(result.alerts()).extracting(DietaryAlertResponse::level)
				.containsExactly(AlertLevel.CRITICAL, AlertLevel.WARNING);
		assertThat(result.alerts()).extracting(DietaryAlertResponse::participantId).containsExactly(2L, 1L);
	}

	@Test
	void aWarningForOnePersonSuppressesTheInformativeAlert() {
		DietaryAnalysisResponse result = DietaryAnalysisCalculator.analyze(
				List.of(person(1, "Ana", CELIAC_GLUTEN_FREE), person(2, "Beto", VEGAN)), menu(10, 2));

		assertThat(result.alerts()).extracting(DietaryAlertResponse::level).doesNotContain(AlertLevel.INFO);
	}

	private static ParticipantDiet person(long id, String name, DietaryRestriction... restrictions) {
		return new ParticipantDiet(id, name, Set.of(restrictions));
	}

	private static ItemTraits item(long id, Set<Allergen> allergens, Set<FoodCharacteristic> characteristics) {
		return new ItemTraits(id, "Item " + id, allergens, characteristics);
	}

	private static ItemTraits withAllergens(long id, Allergen... allergens) {
		return item(id, Set.of(allergens), Set.of());
	}

	/** A menu where {@code safe} items have no gluten and the rest are all gluten. */
	private static List<ItemTraits> menu(int total, int safe) {
		return IntStream.range(0, total)
				.mapToObj(i -> i < safe ? item(i, Set.of(), Set.of()) : withAllergens(i, GLUTEN))
				.toList();
	}

}
