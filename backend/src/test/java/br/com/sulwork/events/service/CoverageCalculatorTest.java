package br.com.sulwork.events.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.sulwork.events.domain.EventReadiness;
import br.com.sulwork.events.domain.CategoryQuantities;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.dto.CategoryCoverageResponse;
import br.com.sulwork.events.dto.CoverageResponse;

class CoverageCalculatorTest {

	@Test
	void formulaExample_mandatory6Of6_optional0Of4_gives75Percent() {
		CoverageResponse r = calculate(category("Salgados", 0, 10, 6, 4, 6, 0));

		assertThat(r.overallPercentage()).isEqualTo(75);
		assertThat(r.mandatoryPercentage()).isEqualTo(100);
		assertThat(r.optionalPercentage()).isZero();
		assertThat(r.readiness()).isEqualTo(EventReadiness.MANDATORY_SECURED);
		assertThat(r.categories().get(0).percentage()).isEqualTo(75);
	}

	@Test
	void goalDeficitEntersDenominator() {
		// Complete items (10 of 10), but the goal is 20: 10 are missing that no item asked for.
		CoverageResponse r = calculate(category("Doces", 20, 10, 10, 0, 10, 0));

		CategoryCoverageResponse c = r.categories().get(0);
		assertThat(c.goalDeficit()).isEqualTo(10);
		assertThat(c.effectiveTarget()).isEqualTo(20);
		assertThat(c.goalReached()).isFalse();
		assertThat(c.percentage()).isEqualTo(66); // 20 / 30
		assertThat(r.readiness()).isEqualTo(EventReadiness.MANDATORY_SECURED);
	}

	@Test
	void goalSmallerThanItemsSumProducesNoDeficit() {
		CategoryCoverageResponse c = calculate(category("Doces", 5, 10, 10, 0, 4, 0)).categories().get(0);

		assertThat(c.goalDeficit()).isZero();
		assertThat(c.effectiveTarget()).isEqualTo(10);
		assertThat(c.goalReached()).isFalse();
	}

	@Test
	void categoryWithGoalAndNoItemsCountsAsZero() {
		CoverageResponse r = calculate(category("Vazia", 20, 0, 0, 0, 0, 0));

		assertThat(r.categories().get(0).percentage()).isZero();
		assertThat(r.overallPercentage()).isZero();
		assertThat(r.readiness()).isEqualTo(EventReadiness.NO_ITEMS);
	}

	@Test
	void categoryWithoutItemsAndGoalStaysOutOfAverage() {
		CoverageResponse r = calculate(
				category("Cheia", 0, 10, 10, 0, 10, 0),
				category("Vazia", 0, 0, 0, 0, 0, 0));

		assertThat(r.categories().get(1).percentage()).isNull();
		assertThat(r.overallPercentage()).isEqualTo(100);
	}

	@Test
	void differentUnitsDoNotOverlap() {
		// Complete milliliters and empty units: the average of the categories is 50%.
		// Summing everything would give 2 x 5000 / (2 x 5000 + 2 x 40) = 99%, because the milliliter would dominate.
		CoverageResponse r = calculate(
				category("Bebidas", 0, 1, 5000, 0, 5000, 0),
				category("Salgados", 0, 1, 40, 0, 0, 0));

		assertThat(r.overallPercentage()).isEqualTo(50);
		assertThat(r.mandatoryPercentage()).isEqualTo(50);
	}

	@Test
	void eventWithoutCategoriesAndItems() {
		CoverageResponse r = calculate();

		assertThat(r.readiness()).isEqualTo(EventReadiness.NO_ITEMS);
		assertThat(r.overallPercentage()).isNull();
		assertThat(r.mandatoryPercentage()).isEqualTo(100);
		assertThat(r.optionalPercentage()).isEqualTo(100);
		assertThat(r.totalItems()).isZero();
		assertThat(r.categories()).isEmpty();
	}

	@Test
	void withoutMandatoryItemsMandatoryCoverageIs100() {
		CoverageResponse r = calculate(category("Doces", 0, 2, 0, 10, 0, 3));

		assertThat(r.mandatoryPercentage()).isEqualTo(100);
		assertThat(r.optionalPercentage()).isEqualTo(30);
		assertThat(r.readiness()).isEqualTo(EventReadiness.MANDATORY_SECURED);
	}

	@Test
	void criticalWhenMandatoryAverageIsBelow50() {
		CoverageResponse r = calculate(category("Pratos", 0, 4, 4, 0, 1, 0));

		assertThat(r.mandatoryPercentage()).isEqualTo(25);
		assertThat(r.readiness()).isEqualTo(EventReadiness.CRITICAL);
	}

	@Test
	void exactly50IsNotCritical() {
		CoverageResponse r = calculate(category("Pratos", 0, 4, 4, 0, 2, 0));

		assertThat(r.mandatoryPercentage()).isEqualTo(50);
		assertThat(r.readiness()).isEqualTo(EventReadiness.IN_PROGRESS);
	}

	@Test
	void completeWhenAllItemsAreCompleteAndGoalsReached() {
		CoverageResponse r = calculate(
				category("Pratos", 8, 4, 6, 4, 6, 4),
				category("Bebidas", 0, 1, 10, 0, 10, 0));

		assertThat(r.readiness()).isEqualTo(EventReadiness.COMPLETE);
		assertThat(r.overallPercentage()).isEqualTo(100);
		assertThat(r.categories()).allMatch(CategoryCoverageResponse::goalReached);
	}

	@Test
	void completeItemsButGoalNotReachedIsNotComplete() {
		CoverageResponse r = calculate(category("Pratos", 20, 2, 5, 5, 5, 5));

		assertThat(r.readiness()).isEqualTo(EventReadiness.MANDATORY_SECURED);
		assertThat(r.overallPercentage()).isLessThan(100);
	}

	@Test
	void categoryWithMissingMandatoryBlocksMandatorySecured() {
		CoverageResponse r = calculate(
				category("Pratos", 0, 2, 10, 0, 10, 0),
				category("Bebidas", 0, 2, 10, 0, 9, 0));

		assertThat(r.readiness()).isEqualTo(EventReadiness.IN_PROGRESS);
	}

	@Test
	void percentagesRoundDownWithoutFloatingPointError() {
		assertThat(calculate(category("A", 0, 1, 1, 0, 0, 0)).categories().get(0).mandatoryPercentage()).isZero();
		assertThat(calculate(category("A", 0, 1, 3, 0, 1, 0)).categories().get(0).mandatoryPercentage()).isEqualTo(33);
		assertThat(calculate(category("A", 0, 1, 200, 0, 199, 0)).categories().get(0).mandatoryPercentage())
				.isEqualTo(99);
		assertThat(calculate(category("A", 0, 1, 100, 0, 29, 0)).categories().get(0).mandatoryPercentage())
				.isEqualTo(29);
		assertThat(calculate(category("A", 0, 1, 7, 0, 7, 0)).categories().get(0).mandatoryPercentage())
				.isEqualTo(100);
	}

	@Test
	void averageOfFractionsSummingToWholeDoesNotLoseAPoint() {
		CoverageResponse r = calculate(
				category("A", 0, 1, 3, 0, 1, 0),
				category("B", 0, 1, 3, 0, 2, 0),
				category("C", 0, 1, 3, 0, 3, 0));

		assertThat(r.mandatoryPercentage()).isEqualTo(66); // (1/3 + 2/3 + 1) / 3 = 2/3
		assertThat(calculate(category("A", 0, 1, 3, 0, 1, 0), category("B", 0, 1, 3, 0, 2, 0))
				.mandatoryPercentage()).isEqualTo(50);
	}

	@Test
	void sumsTotalItemsOfAllCategories() {
		CoverageResponse r = calculate(category("A", 0, 3, 1, 0, 0, 0), category("B", 0, 4, 1, 0, 0, 0));

		assertThat(r.totalItems()).isEqualTo(7);
	}

	private static CoverageResponse calculate(CategoryQuantities... categories) {
		return CoverageCalculator.calculate(List.of(categories));
	}

	/** The numbers are, in order: goal, total items, required and contributed, mandatory and optional. */
	private static CategoryQuantities category(String name, int goal, long totalItems, long mandatoryRequired,
			long optionalRequired, long mandatoryContributed, long optionalContributed) {
		return new CategoryQuantities(1L, name, MeasureUnit.UNIT, goal, totalItems, mandatoryRequired,
				optionalRequired, mandatoryContributed, optionalContributed);
	}

}
