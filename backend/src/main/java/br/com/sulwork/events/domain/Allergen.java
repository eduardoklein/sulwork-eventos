package br.com.sulwork.events.domain;

public enum Allergen {

	GLUTEN("Glúten"),
	LACTOSE("Lactose"),
	PEANUT("Amendoim"),
	TREE_NUTS("Castanhas"),
	SHELLFISH("Frutos do mar"),
	FISH("Peixe"),
	EGG("Ovo"),
	SOY("Soja");

	private final String description;

	Allergen(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
