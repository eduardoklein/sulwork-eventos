package br.com.sulwork.events.dto;

import java.util.List;

import br.com.sulwork.events.domain.MeasureUnit;

/**
 * A suggestion of what a participant could bring. {@code priority} goes from 0 to 100, where 100 is an item that
 * scores on every rule that applies to that person. {@code safeForParticipant} is {@code null} when the person has
 * no dietary restrictions.
 */
public record SuggestionResponse(
		Long itemId,
		String itemName,
		String categoryName,
		MeasureUnit unit,
		int requiredQuantity,
		long contributedQuantity,
		long pendingQuantity,
		int coveragePercentage,
		long suggestedQuantity,
		int priority,
		Boolean safeForParticipant,
		List<String> reasons) {
}
