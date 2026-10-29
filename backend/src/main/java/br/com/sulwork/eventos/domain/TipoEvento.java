package br.com.sulwork.eventos.domain;

public enum TipoEvento {

	CAFE_DA_MANHA("Café da Manhã"),
	ALMOCO("Almoço"),
	LANCHE("Lanche"),
	CONFRATERNIZACAO("Confraternização"),
	OUTRO("Outro");

	private final String descricao;

	TipoEvento(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
