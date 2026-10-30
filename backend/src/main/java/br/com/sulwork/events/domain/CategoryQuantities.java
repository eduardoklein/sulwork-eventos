package br.com.sulwork.events.domain;

/**
 * What a category needs and has already received. All quantities are in the category unit, so summing and
 * comparing here is valid. Summing different categories is not.
 */
public record CategoryQuantities(
		Long id,
		String name,
		MeasureUnit unit,
		int goal,
		long totalItems,
		long mandatoryRequired,
		long optionalRequired,
		long mandatoryContributed,
		long optionalContributed) {

	/** The sum of the required quantities of the items. */
	public long required() {
		return mandatoryRequired + optionalRequired;
	}

	public long contributed() {
		return mandatoryContributed + optionalContributed;
	}

	/** How much the goal exceeds the sum of the items: what no item has asked for yet. */
	public long goalDeficit() {
		return Math.max(0, goal - required());
	}

	/** The larger value between the goal and the sum of the items. */
	public long effectiveTarget() {
		return Math.max(goal, required());
	}

	public boolean goalReached() {
		return contributed() >= goal;
	}

	public boolean mandatoryComplete() {
		return mandatoryContributed == mandatoryRequired;
	}

	public boolean itemsComplete() {
		return mandatoryComplete() && optionalContributed == optionalRequired;
	}

	/** The denominator of the weighted coverage: mandatory counts 2, optional counts 1, and the goal deficit counts 1. */
	public long requiredWeight() {
		return 2 * mandatoryRequired + optionalRequired + goalDeficit();
	}

	public long contributedWeight() {
		return 2 * mandatoryContributed + optionalContributed;
	}

}
