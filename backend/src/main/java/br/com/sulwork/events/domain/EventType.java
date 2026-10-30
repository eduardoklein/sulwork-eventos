package br.com.sulwork.events.domain;

public enum EventType implements Described {

	BREAKFAST("Café da Manhã"),
	LUNCH("Almoço"),
	SNACK("Lanche"),
	GET_TOGETHER("Confraternização"),
	OTHER("Outro");

	private final String description;

	EventType(String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
