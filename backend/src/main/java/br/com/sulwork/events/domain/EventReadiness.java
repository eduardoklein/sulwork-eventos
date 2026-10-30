package br.com.sulwork.events.domain;

public enum EventReadiness implements Described {

	NO_ITEMS("Sem itens"),
	CRITICAL("Crítico"),
	IN_PROGRESS("Em andamento"),
	MANDATORY_SECURED("Obrigatórios garantidos"),
	COMPLETE("Completo");

	private final String description;

	EventReadiness(String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
