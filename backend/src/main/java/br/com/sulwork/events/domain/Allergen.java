package br.com.sulwork.events.domain;

public enum Allergen implements Described {

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

	@Override
	public String getDescription() {
		return description;
	}

}
