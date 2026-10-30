package br.com.sulwork.events.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.DietaryConflict;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.ItemProgress;
import br.com.sulwork.events.dto.SuggestionResponse;

/**
 * Ranks the pending items by how urgent they are for one participant. Each rule adds points, and the points turn
 * into a priority from 0 to 100 against the most that person could score. Coverage is compared item by item, so
 * quantities in different units never mix.
 */
public final class SuggestionRanker {

	static final int MANDATORY_POINTS = 50;
	static final int NO_CONTRIBUTION_POINTS = 25;
	static final int BELOW_AVERAGE_POINTS = 20;
	static final int GOAL_PENDING_POINTS = 20;
	static final int SAFE_POINTS = 25;
	static final int UNSAFE_POINTS = -10;

	/** Quantities up to this are suggested whole. Larger ones are split in half. */
	private static final int WHOLE_QUANTITY_LIMIT = 3;
	private static final int SCALE = 12;

	private SuggestionRanker() {
	}

	public static List<SuggestionResponse> rank(List<ItemProgress> items, Set<DietaryRestriction> restrictions) {
		boolean hasRestrictions = !restrictions.isEmpty();
		BigDecimal averageCoverage = averageCoverage(items);
		Map<Long, Long> contributedByCategory = new HashMap<>();
		items.forEach(item -> contributedByCategory.merge(item.categoryId(), item.contributedQuantity(), Long::sum));
		int maxPoints = MANDATORY_POINTS + NO_CONTRIBUTION_POINTS + BELOW_AVERAGE_POINTS + GOAL_PENDING_POINTS
				+ (hasRestrictions ? SAFE_POINTS : 0);

		List<Candidate> candidates = new ArrayList<>();
		for (ItemProgress item : items) {
			if (item.pendingQuantity() == 0 || item.contributedByParticipant()) {
				continue;
			}
			candidates.add(candidate(item, restrictions, averageCoverage,
					contributedByCategory.get(item.categoryId()) >= item.categoryGoal()));
		}
		return candidates.stream()
				.sorted(Comparator.comparingInt(Candidate::points).reversed()
						.thenComparing(Candidate::coverage)
						.thenComparing(c -> c.item().itemId()))
				.map(c -> response(c, maxPoints, hasRestrictions))
				.toList();
	}

	private static Candidate candidate(ItemProgress item, Set<DietaryRestriction> restrictions,
			BigDecimal averageCoverage, boolean goalReached) {
		BigDecimal coverage = coverage(item);
		int points = 0;
		List<String> reasons = new ArrayList<>();
		if (item.mandatory()) {
			points += MANDATORY_POINTS;
			reasons.add("Item obrigatório.");
		}
		if (item.contributedQuantity() == 0) {
			points += NO_CONTRIBUTION_POINTS;
			reasons.add("Ninguém contribuiu ainda.");
		}
		if (coverage.compareTo(averageCoverage) < 0) {
			points += BELOW_AVERAGE_POINTS;
			reasons.add("A cobertura está abaixo da média do evento.");
		}
		if (!goalReached) {
			points += GOAL_PENDING_POINTS;
			reasons.add("A meta da categoria " + item.categoryName() + " ainda não foi atingida.");
		}
		Boolean safe = null;
		if (!restrictions.isEmpty()) {
			DietaryConflict conflict = conflictsOf(restrictions, item);
			safe = conflict.isEmpty();
			if (safe) {
				points += SAFE_POINTS;
				reasons.add("Seguro para as suas restrições.");
			}
			else {
				points += UNSAFE_POINTS;
				reasons.add("Não é seguro para as suas restrições: " + describe(conflict) + ".");
			}
		}
		return new Candidate(item, points, coverage, safe, reasons);
	}

	private static SuggestionResponse response(Candidate c, int maxPoints, boolean hasRestrictions) {
		ItemProgress item = c.item();
		long pending = item.pendingQuantity();
		int priority = Math.max(0, Math.min(c.points(), maxPoints)) * 100 / maxPoints;
		return new SuggestionResponse(item.itemId(), item.name(), item.categoryName(), item.unit(),
				item.requiredQuantity(), item.contributedQuantity(), pending, percentage(c.coverage()),
				suggestedQuantity(pending), priority, hasRestrictions ? c.safe() : null, c.reasons());
	}

	static long suggestedQuantity(long pending) {
		return pending <= WHOLE_QUANTITY_LIMIT ? pending : (pending + 1) / 2;
	}

	private static BigDecimal coverage(ItemProgress item) {
		long covered = Math.min(item.contributedQuantity(), item.requiredQuantity());
		return BigDecimal.valueOf(covered).divide(BigDecimal.valueOf(item.requiredQuantity()), SCALE, RoundingMode.HALF_UP);
	}

	private static BigDecimal averageCoverage(List<ItemProgress> items) {
		if (items.isEmpty()) {
			return BigDecimal.ZERO;
		}
		BigDecimal sum = items.stream().map(SuggestionRanker::coverage).reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(BigDecimal.valueOf(items.size()), SCALE, RoundingMode.HALF_UP);
	}

	private static int percentage(BigDecimal ratio) {
		return ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.DOWN).intValue();
	}

	private static DietaryConflict conflictsOf(Set<DietaryRestriction> restrictions, ItemProgress item) {
		Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
		Set<FoodCharacteristic> characteristics = EnumSet.noneOf(FoodCharacteristic.class);
		for (DietaryRestriction restriction : restrictions) {
			DietaryConflict conflict = restriction.findConflicts(item.allergens(), item.characteristics());
			allergens.addAll(conflict.allergens());
			characteristics.addAll(conflict.characteristics());
		}
		return new DietaryConflict(allergens, characteristics);
	}

	private static String describe(DietaryConflict conflict) {
		return Stream.concat(
				conflict.allergens().stream().sorted().map(Allergen::getDescription),
				conflict.characteristics().stream().sorted().map(FoodCharacteristic::getDescription))
				.collect(Collectors.joining(", "));
	}

	private record Candidate(ItemProgress item, int points, BigDecimal coverage, Boolean safe, List<String> reasons) {
	}

}
