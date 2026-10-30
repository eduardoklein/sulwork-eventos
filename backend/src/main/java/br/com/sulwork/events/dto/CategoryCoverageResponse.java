package br.com.sulwork.events.dto;

import br.com.sulwork.events.domain.MeasureUnit;

/**
 * The numbers of a category, all in its unit. {@code percentage} is the weighted coverage (mandatory counts 2,
 * optional 1, and the goal deficit enters the total) and is {@code null} for a category without items and goal.
 * Mandatory and optional show 100 when the category has no item of that kind.
 */
public record CategoryCoverageResponse(
		Long categoryId,
		String name,
		MeasureUnit unit,
		int goalQuantity,
		long totalItems,
		long requiredQuantity,
		long contributedQuantity,
		long goalDeficit,
		long effectiveTarget,
		boolean goalReached,
		Integer percentage,
		int mandatoryPercentage,
		int optionalPercentage) {
}
