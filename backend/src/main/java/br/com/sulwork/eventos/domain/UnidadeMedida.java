package br.com.sulwork.eventos.domain;

public enum UnidadeMedida {

	UNIDADE("Unidade"),
	PORCAO("Porção"),
	GRAMA("Grama"),
	MILILITRO("Mililitro");

	private final String descricao;

	UnidadeMedida(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
