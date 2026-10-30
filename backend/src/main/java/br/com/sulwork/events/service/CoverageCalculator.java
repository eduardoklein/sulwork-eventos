package br.com.sulwork.events.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import br.com.sulwork.events.domain.EventReadiness;
import br.com.sulwork.events.domain.CategoryQuantities;
import br.com.sulwork.events.dto.CategoryCoverageResponse;
import br.com.sulwork.events.dto.CoverageResponse;

/**
 * Event coverage. The heavy calculation runs inside each category, where the unit is a single one. The overall
 * value is the average of the categories: summing milliliters with units would let the larger unit dominate.
 */
public final class CoverageCalculator {

	private static final int SCALE = 12;
	private static final BigDecimal HALF = new BigDecimal("0.5");

	private CoverageCalculator() {
	}

	public static CoverageResponse calculate(List<CategoryQuantities> categories) {
		BigDecimal overall = average(categories, c -> c.requiredWeight() > 0, c -> ratio(c.contributedWeight(), c.requiredWeight()));
		BigDecimal mandatory = average(categories, c -> c.mandatoryRequired() > 0,
				c -> ratio(c.mandatoryContributed(), c.mandatoryRequired()));
		BigDecimal optional = average(categories, c -> c.optionalRequired() > 0,
				c -> ratio(c.optionalContributed(), c.optionalRequired()));
		long totalItems = categories.stream().mapToLong(CategoryQuantities::totalItems).sum();

		return new CoverageResponse(
				readiness(categories, totalItems, mandatory),
				overall == null ? null : percentage(overall),
				mandatory == null ? 100 : percentage(mandatory),
				optional == null ? 100 : percentage(optional),
				totalItems,
				categories.stream().map(CoverageCalculator::category).toList());
	}

	private static EventReadiness readiness(List<CategoryQuantities> categories, long totalItems,
			BigDecimal mandatory) {
		if (totalItems == 0) {
			return EventReadiness.NO_ITEMS;
		}
		if (mandatory != null && mandatory.compareTo(HALF) < 0) {
			return EventReadiness.CRITICAL;
		}
		if (categories.stream().allMatch(c -> c.itemsComplete() && c.goalReached())) {
			return EventReadiness.COMPLETE;
		}
		if (categories.stream().allMatch(CategoryQuantities::mandatoryComplete)) {
			return EventReadiness.MANDATORY_SECURED;
		}
		return EventReadiness.IN_PROGRESS;
	}

	private static CategoryCoverageResponse category(CategoryQuantities c) {
		return new CategoryCoverageResponse(
				c.id(),
				c.name(),
				c.unit(),
				c.goal(),
				c.totalItems(),
				c.required(),
				c.contributed(),
				c.goalDeficit(),
				c.effectiveTarget(),
				c.goalReached(),
				c.requiredWeight() == 0 ? null : integerPercentage(c.contributedWeight(), c.requiredWeight()),
				c.mandatoryRequired() == 0 ? 100
						: integerPercentage(c.mandatoryContributed(), c.mandatoryRequired()),
				c.optionalRequired() == 0 ? 100
						: integerPercentage(c.optionalContributed(), c.optionalRequired()));
	}

	/** The average of the ratios of the categories that pass the filter, or {@code null} if none passes. */
	private static BigDecimal average(List<CategoryQuantities> categories, Predicate<CategoryQuantities> counts,
			Function<CategoryQuantities, BigDecimal> ratio) {
		List<BigDecimal> ratios = categories.stream().filter(counts).map(ratio).toList();
		if (ratios.isEmpty()) {
			return null;
		}
		BigDecimal sum = ratios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(BigDecimal.valueOf(ratios.size()), SCALE, RoundingMode.HALF_UP);
	}

	private static BigDecimal ratio(long part, long total) {
		return BigDecimal.valueOf(part).divide(BigDecimal.valueOf(total), SCALE, RoundingMode.HALF_UP);
	}

	/** Rounds down: it only shows 100 when complete. */
	private static int percentage(BigDecimal ratio) {
		return ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.DOWN).intValue();
	}

	/** Uses integers only, without floating point rounding error. */
	private static int integerPercentage(long part, long total) {
		return (int) (part * 100 / total);
	}

}
