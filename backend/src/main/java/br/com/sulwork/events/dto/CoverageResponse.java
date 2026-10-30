package br.com.sulwork.events.dto;

import java.util.List;

import br.com.sulwork.events.domain.EventReadiness;

/**
 * The event coverage. The overall percentages are the average of the categories, and {@code overallPercentage} is
 * {@code null} when no category has items or a goal.
 */
public record CoverageResponse(
		EventReadiness readiness,
		Integer overallPercentage,
		int mandatoryPercentage,
		int optionalPercentage,
		long totalItems,
		List<CategoryCoverageResponse> categories) {
}
