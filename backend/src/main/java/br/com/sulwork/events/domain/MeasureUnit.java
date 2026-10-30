package br.com.sulwork.events.domain;

public enum MeasureUnit implements Described {

	UNIT("Unidade"),
	PORTION("Porção"),
	GRAM("Grama"),
	MILLILITER("Mililitro");

	private final String description;

	MeasureUnit(String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
