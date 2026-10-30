package br.com.sulwork.eventos.domain;

public enum ProntidaoEvento {

	SEM_ITENS("Sem itens"),
	CRITICO("Crítico"),
	EM_ANDAMENTO("Em andamento"),
	OBRIGATORIOS_GARANTIDOS("Obrigatórios garantidos"),
	COMPLETO("Completo");

	private final String descricao;

	ProntidaoEvento(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
