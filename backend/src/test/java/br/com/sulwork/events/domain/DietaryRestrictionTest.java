package br.com.sulwork.events.domain;

import static br.com.sulwork.events.domain.Allergen.SHELLFISH;
import static br.com.sulwork.events.domain.Allergen.GLUTEN;
import static br.com.sulwork.events.domain.Allergen.LACTOSE;
import static br.com.sulwork.events.domain.Allergen.EGG;
import static br.com.sulwork.events.domain.Allergen.FISH;
import static br.com.sulwork.events.domain.FoodCharacteristic.ADDED_SUGAR;
import static br.com.sulwork.events.domain.FoodCharacteristic.HIGH_SODIUM;
import static br.com.sulwork.events.domain.FoodCharacteristic.ANIMAL_MEAT;
import static br.com.sulwork.events.domain.DietaryRestriction.SHELLFISH_ALLERGY;
import static br.com.sulwork.events.domain.DietaryRestriction.FISH_ALLERGY;
import static br.com.sulwork.events.domain.DietaryRestriction.CELIAC_GLUTEN_FREE;
import static br.com.sulwork.events.domain.DietaryRestriction.DIABETIC_NO_SUGAR;
import static br.com.sulwork.events.domain.DietaryRestriction.HYPERTENSIVE_LOW_SODIUM;
import static br.com.sulwork.events.domain.DietaryRestriction.VEGAN;
import static br.com.sulwork.events.domain.DietaryRestriction.VEGETARIAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DietaryRestrictionTest {

	private static final Set<Allergen> NO_ALLERGENS = Set.of();
	private static final Set<FoodCharacteristic> NO_CHARACTERISTICS = Set.of();

	@Test
	void itemWithGlutenAndLactoseConflictsWithVeganOnlyOnLactose() {
		DietaryConflict conflict = VEGAN.findConflicts(Set.of(GLUTEN, LACTOSE), NO_CHARACTERISTICS);

		assertThat(conflict.allergens()).containsExactly(LACTOSE);
		assertThat(conflict.characteristics()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(DietaryRestriction.class)
	void itemWithoutAllergensNorCharacteristicsConflictsWithNoRestriction(DietaryRestriction restriction) {
		assertThat(restriction.findConflicts(NO_ALLERGENS, NO_CHARACTERISTICS).isEmpty()).isTrue();
	}

	@Test
	void celiacConflictsWithGlutenOnly() {
		DietaryConflict conflict = CELIAC_GLUTEN_FREE.findConflicts(Set.of(GLUTEN, EGG), NO_CHARACTERISTICS);

		assertThat(conflict.allergens()).containsExactly(GLUTEN);
	}

	@Test
	void vegetarianAcceptsEggAndDairy() {
		DietaryConflict conflict = VEGETARIAN.findConflicts(Set.of(EGG, LACTOSE), NO_CHARACTERISTICS);

		assertThat(conflict.isEmpty()).isTrue();
	}

	@Test
	void vegetarianAndVeganRefuseFishAndShellfish() {
		Set<Allergen> item = Set.of(FISH, SHELLFISH);

		assertThat(VEGETARIAN.findConflicts(item, NO_CHARACTERISTICS).allergens())
				.containsExactlyInAnyOrder(FISH, SHELLFISH);
		assertThat(VEGAN.findConflicts(item, NO_CHARACTERISTICS).allergens())
				.containsExactlyInAnyOrder(FISH, SHELLFISH);
	}

	@Test
	void vegetarianAndVeganRefuseAnimalMeat() {
		Set<FoodCharacteristic> item = Set.of(ANIMAL_MEAT);

		assertThat(VEGETARIAN.findConflicts(NO_ALLERGENS, item).characteristics())
				.containsExactly(ANIMAL_MEAT);
		assertThat(VEGAN.findConflicts(NO_ALLERGENS, item).characteristics())
				.containsExactly(ANIMAL_MEAT);
	}

	@Test
	void mixedConflictReturnsBothTypes() {
		DietaryConflict conflict = VEGAN.findConflicts(Set.of(LACTOSE, GLUTEN), Set.of(ANIMAL_MEAT, HIGH_SODIUM));

		assertThat(conflict.allergens()).containsExactly(LACTOSE);
		assertThat(conflict.characteristics()).containsExactly(ANIMAL_MEAT);
		assertThat(conflict.isEmpty()).isFalse();
	}

	@Test
	void fishAllergyDoesNotConflictWithShellfishNorViceVersa() {
		assertThat(FISH_ALLERGY.findConflicts(Set.of(SHELLFISH), NO_CHARACTERISTICS).isEmpty()).isTrue();
		assertThat(SHELLFISH_ALLERGY.findConflicts(Set.of(FISH), NO_CHARACTERISTICS).isEmpty()).isTrue();
		assertThat(FISH_ALLERGY.findConflicts(Set.of(FISH), NO_CHARACTERISTICS).allergens()).containsExactly(FISH);
	}

	@Test
	void diabeticAndHypertensiveConflictWithTheirCharacteristic() {
		assertThat(DIABETIC_NO_SUGAR.findConflicts(NO_ALLERGENS, Set.of(ADDED_SUGAR, HIGH_SODIUM)).characteristics())
				.containsExactly(ADDED_SUGAR);
		assertThat(HYPERTENSIVE_LOW_SODIUM.findConflicts(NO_ALLERGENS, Set.of(ADDED_SUGAR, HIGH_SODIUM)).characteristics())
				.containsExactly(HIGH_SODIUM);
	}

	@Test
	void conflictSetsCannotBeModified() {
		assertThatThrownBy(() -> VEGAN.getConflictingAllergens().add(GLUTEN))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> VEGAN.findConflicts(Set.of(LACTOSE), NO_CHARACTERISTICS).allergens().add(GLUTEN))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
