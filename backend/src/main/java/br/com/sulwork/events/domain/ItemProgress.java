package br.com.sulwork.events.domain;

import java.util.Set;

/** An item with how much has been contributed so far, as seen by one participant. */
public record ItemProgress(
		Long itemId,
		String name,
		Long categoryId,
		String categoryName,
		MeasureUnit unit,
		int categoryGoal,
		int requiredQuantity,
		long contributedQuantity,
		boolean mandatory,
		Set<Allergen> allergens,
		Set<FoodCharacteristic> characteristics,
		boolean contributedByParticipant) {

	public long pendingQuantity() {
		return Math.max(0, requiredQuantity - contributedQuantity);
	}
}
