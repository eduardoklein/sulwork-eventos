package br.com.sulwork.events.dto;

import java.util.Set;

import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.FoodCharacteristic;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The item does not change category: that is why there is no {@code categoryId} here. */
public record UpdateItemRequest(
		@NotBlank @Size(max = 120) String name,
		@NotNull @Min(1) Integer requiredQuantity,
		Boolean mandatory,
		Set<Allergen> allergens,
		Set<FoodCharacteristic> characteristics) {
}
