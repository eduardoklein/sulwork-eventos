package br.com.sulwork.events.dto;

import br.com.sulwork.events.domain.DietaryRestriction;

/** One restriction present in the event: how many people have it and how many items are compatible with it. */
public record RestrictionSummaryResponse(
		DietaryRestriction restriction,
		String description,
		long peopleCount,
		long compatibleItems,
		long totalItems) {
}
