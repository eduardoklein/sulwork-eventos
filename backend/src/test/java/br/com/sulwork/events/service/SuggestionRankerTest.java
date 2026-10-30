package br.com.sulwork.events.service;

import static br.com.sulwork.events.domain.Allergen.GLUTEN;
import static br.com.sulwork.events.domain.Allergen.LACTOSE;
import static br.com.sulwork.events.domain.DietaryRestriction.CELIAC_GLUTEN_FREE;
import static br.com.sulwork.events.domain.DietaryRestriction.VEGAN;
import static br.com.sulwork.events.domain.FoodCharacteristic.ANIMAL_MEAT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.ItemProgress;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.dto.SuggestionResponse;

class SuggestionRankerTest {

	private static final Set<DietaryRestriction> NO_RESTRICTIONS = Set.of();

	@Test
	void mandatoryItemAtZeroCoverageComesBeforeOptionalItemAtFiftyPercent() {
		List<SuggestionResponse> result = SuggestionRanker.rank(
				List.of(item(1, false, 10, 5), item(2, true, 10, 0)), NO_RESTRICTIONS);

		assertThat(result).extracting(SuggestionResponse::itemId).containsExactly(2L, 1L);
	}

	@Test
	void completeItemsAreNotSuggested() {
		List<SuggestionResponse> result = SuggestionRanker.rank(
				List.of(item(1, true, 10, 10), item(2, false, 10, 3)), NO_RESTRICTIONS);

		assertThat(result).extracting(SuggestionResponse::itemId).containsExactly(2L);
	}

	@Test
	void itemsTheParticipantAlreadyContributesToAreNotSuggested() {
		List<SuggestionResponse> result = SuggestionRanker.rank(
				List.of(item(1, true, 10, 4, true), item(2, false, 10, 0)), NO_RESTRICTIONS);

		assertThat(result).extracting(SuggestionResponse::itemId).containsExactly(2L);
	}

	@Test
	void priorityIsAScaleFromZeroToOneHundredAgainstTheMostThePersonCouldScore() {
		// Mandatory (50) + nobody contributed yet (25) = 75 of at most 115 without restrictions: 65.
		SuggestionResponse suggestion = SuggestionRanker.rank(List.of(item(1, true, 10, 0)), NO_RESTRICTIONS).get(0);

		assertThat(suggestion.priority()).isEqualTo(65);
		assertThat(suggestion.reasons()).containsExactly("Item obrigatório.", "Ninguém contribuiu ainda.");
	}

	@Test
	void anItemThatScoresOnEveryRuleHasPriority100() {
		// Mandatory, no contribution, below average, goal pending and safe: 50 + 25 + 20 + 20 + 25 = 140 of 140.
		List<ItemProgress> items = List.of(
				itemIn(1, 1, "Food", 100, true, 10, 0, Set.of(), false),
				itemIn(2, 1, "Food", 100, false, 10, 9, Set.of(), false));

		SuggestionResponse top = SuggestionRanker.rank(items, Set.of(VEGAN)).get(0);

		assertThat(top.itemId()).isEqualTo(1L);
		assertThat(top.priority()).isEqualTo(100);
		assertThat(top.safeForParticipant()).isTrue();
		assertThat(top.reasons()).containsExactly("Item obrigatório.", "Ninguém contribuiu ainda.",
				"A cobertura está abaixo da média do evento.", "A meta da categoria Food ainda não foi atingida.",
				"Seguro para as suas restrições.");
	}

	@Test
	void anUnsafeItemIsStillListedButAfterTheSafeOnesAndWithAWarning() {
		List<ItemProgress> items = List.of(
				itemIn(1, 1, "Food", 0, true, 10, 0, Set.of(GLUTEN), false),
				itemIn(2, 1, "Food", 0, true, 10, 0, Set.of(), false));

		List<SuggestionResponse> result = SuggestionRanker.rank(items, Set.of(CELIAC_GLUTEN_FREE));

		assertThat(result).extracting(SuggestionResponse::itemId).containsExactly(2L, 1L);
		SuggestionResponse unsafe = result.get(1);
		assertThat(unsafe.safeForParticipant()).isFalse();
		assertThat(unsafe.reasons()).contains("Não é seguro para as suas restrições: Glúten.");
	}

	@Test
	void priorityNeverGoesBelowZero() {
		// Optional, covered, goal reached and unsafe: -10 points.
		List<ItemProgress> items = List.of(itemIn(1, 1, "Food", 0, false, 10, 9, Set.of(GLUTEN), false));

		assertThat(SuggestionRanker.rank(items, Set.of(CELIAC_GLUTEN_FREE)).get(0).priority()).isZero();
	}

	@Test
	void safetyIsNullForAPersonWithoutRestrictions() {
		assertThat(SuggestionRanker.rank(List.of(item(1, true, 10, 0)), NO_RESTRICTIONS).get(0).safeForParticipant())
				.isNull();
	}

	@Test
	void severalRestrictionsAddTheirConflictsInTheReason() {
		List<ItemProgress> items = List.of(new ItemProgress(1L, "Lasagna", 1L, "Food", MeasureUnit.UNIT, 0, 10, 0,
				true, Set.of(GLUTEN, LACTOSE), Set.of(ANIMAL_MEAT), false));

		SuggestionResponse suggestion = SuggestionRanker.rank(items, Set.of(VEGAN, CELIAC_GLUTEN_FREE)).get(0);

		assertThat(suggestion.reasons())
				.contains("Não é seguro para as suas restrições: Glúten, Lactose, Carne de origem animal.");
	}

	@Test
	void suggestedQuantityIsTheWholeBalanceUpToThreeAndHalfRoundedUpAfter() {
		assertThat(SuggestionRanker.suggestedQuantity(1)).isEqualTo(1);
		assertThat(SuggestionRanker.suggestedQuantity(3)).isEqualTo(3);
		assertThat(SuggestionRanker.suggestedQuantity(4)).isEqualTo(2);
		assertThat(SuggestionRanker.suggestedQuantity(7)).isEqualTo(4);
		assertThat(SuggestionRanker.suggestedQuantity(10)).isEqualTo(5);
	}

	@Test
	void theSuggestionCarriesTheQuantitiesAndTheCoverage() {
		SuggestionResponse suggestion = SuggestionRanker.rank(List.of(item(1, false, 9, 3)), NO_RESTRICTIONS).get(0);

		assertThat(suggestion.requiredQuantity()).isEqualTo(9);
		assertThat(suggestion.contributedQuantity()).isEqualTo(3);
		assertThat(suggestion.pendingQuantity()).isEqualTo(6);
		assertThat(suggestion.suggestedQuantity()).isEqualTo(3);
		assertThat(suggestion.coveragePercentage()).isEqualTo(33);
	}

	@Test
	void tiesGoToTheLessCoveredItemAndThenToTheOlderOne() {
		// P and Q are below the average and in a category with a pending goal: same points, P is less covered.
		List<ItemProgress> items = List.of(
				itemIn(3, 1, "Food", 100, false, 10, 4, Set.of(), false),
				itemIn(2, 1, "Food", 100, false, 10, 2, Set.of(), false),
				itemIn(4, 1, "Food", 100, false, 10, 9, Set.of(), false),
				itemIn(1, 1, "Food", 100, false, 10, 2, Set.of(), false));

		List<SuggestionResponse> result = SuggestionRanker.rank(items, NO_RESTRICTIONS);

		assertThat(result).extracting(SuggestionResponse::itemId).containsExactly(1L, 2L, 3L, 4L);
	}

	@Test
	void theCategoryGoalOnlyScoresWhileItIsNotReached() {
		List<ItemProgress> pending = List.of(itemIn(1, 1, "Drinks", 100, false, 10, 4, Set.of(), false));
		List<ItemProgress> reached = List.of(itemIn(1, 1, "Drinks", 4, false, 10, 4, Set.of(), false));
		List<ItemProgress> noGoal = List.of(itemIn(1, 1, "Drinks", 0, false, 10, 4, Set.of(), false));

		assertThat(SuggestionRanker.rank(pending, NO_RESTRICTIONS).get(0).reasons())
				.contains("A meta da categoria Drinks ainda não foi atingida.");
		assertThat(SuggestionRanker.rank(reached, NO_RESTRICTIONS).get(0).reasons())
				.noneMatch(reason -> reason.startsWith("A meta da categoria"));
		assertThat(SuggestionRanker.rank(noGoal, NO_RESTRICTIONS).get(0).reasons())
				.noneMatch(reason -> reason.startsWith("A meta da categoria"));
	}

	@Test
	void belowAverageComparesItemCoverageAndNotRawQuantities() {
		// 10% of a huge item and 100% of a small one: average 55%. The huge item is below it.
		List<ItemProgress> items = List.of(
				itemIn(1, 1, "Food", 0, false, 1000, 100, Set.of(), false),
				itemIn(2, 1, "Food", 0, false, 2, 2, Set.of(), false));

		assertThat(SuggestionRanker.rank(items, NO_RESTRICTIONS).get(0).reasons())
				.contains("A cobertura está abaixo da média do evento.");
	}

	@Test
	void itemsAllAtTheSameCoverageAreNotBelowAverage() {
		List<ItemProgress> items = List.of(item(1, false, 10, 5), item(2, false, 10, 5));

		assertThat(SuggestionRanker.rank(items, NO_RESTRICTIONS))
				.allSatisfy(s -> assertThat(s.reasons()).doesNotContain("A cobertura está abaixo da média do evento."));
	}

	@Test
	void noItemsMeansNoSuggestions() {
		assertThat(SuggestionRanker.rank(List.of(), Set.of(VEGAN))).isEmpty();
	}

	private static ItemProgress item(long id, boolean mandatory, int required, long contributed) {
		return item(id, mandatory, required, contributed, false);
	}

	private static ItemProgress item(long id, boolean mandatory, int required, long contributed, boolean own) {
		return new ItemProgress(id, "Item " + id, 1L, "Food", MeasureUnit.UNIT, 0, required, contributed, mandatory,
				Set.of(), Set.of(), own);
	}

	private static ItemProgress itemIn(long id, long categoryId, String categoryName, int goal, boolean mandatory,
			int required, long contributed, Set<Allergen> allergens, boolean own) {
		return new ItemProgress(id, "Item " + id, categoryId, categoryName, MeasureUnit.UNIT, goal, required,
				contributed, mandatory, allergens, Set.<FoodCharacteristic>of(), own);
	}

}
