package br.com.sulwork.eventos.domain;

public enum Alergeno {

	GLUTEN("Glúten"),
	LACTOSE("Lactose"),
	AMENDOIM("Amendoim"),
	CASTANHAS("Castanhas"),
	FRUTOS_DO_MAR("Frutos do mar"),
	PEIXE("Peixe"),
	OVO("Ovo"),
	SOJA("Soja");

	private final String descricao;

	Alergeno(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
