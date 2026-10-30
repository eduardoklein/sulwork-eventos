package br.com.sulwork.events.domain;

/**
 * Item properties that are not allergens but matter for some restrictions
 * (vegetarians, diabetics, hypertensives).
 */
public enum FoodCharacteristic {

	ANIMAL_MEAT("Carne de origem animal"),
	ADDED_SUGAR("Açúcar adicionado"),
	HIGH_SODIUM("Rico em sal");

	private final String description;

	FoodCharacteristic(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
