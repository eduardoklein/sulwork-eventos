package br.com.sulwork.events.dto;

import java.util.List;

import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.FoodCharacteristic;

/** An item that is not safe for a person, with what conflicts. */
public record UnsafeItemResponse(
		Long itemId,
		String itemName,
		List<Allergen> conflictingAllergens,
		List<FoodCharacteristic> conflictingCharacteristics) {
}
