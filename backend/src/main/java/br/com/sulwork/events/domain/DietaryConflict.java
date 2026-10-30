package br.com.sulwork.events.domain;

import java.util.Set;

public record DietaryConflict(Set<Allergen> allergens, Set<FoodCharacteristic> characteristics) {

	public boolean isEmpty() {
		return allergens.isEmpty() && characteristics.isEmpty();
	}

}
