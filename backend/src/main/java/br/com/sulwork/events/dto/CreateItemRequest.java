package br.com.sulwork.events.dto;

import java.util.Set;

import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.FoodCharacteristic;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mandatory, allergens and characteristics are optional: if missing, the item is optional and has none. */
public record CreateItemRequest(
		@NotBlank @Size(max = 120) String name,
		@NotNull Long categoryId,
		@NotNull @Min(1) Integer requiredQuantity,
		Boolean mandatory,
		Set<Allergen> allergens,
		Set<FoodCharacteristic> characteristics) {
}
