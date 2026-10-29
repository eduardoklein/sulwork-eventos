package br.com.sulwork.eventos.domain;

/**
 * Propriedades do item que não são alérgenos, mas importam para algumas restrições
 * (vegetarianos, diabéticos, hipertensos).
 */
public enum CaracteristicaAlimentar {

	CARNE_ORIGEM_ANIMAL("Carne de origem animal"),
	ACUCAR_ADICIONADO("Açúcar adicionado"),
	ALTO_SODIO("Rico em sal");

	private final String descricao;

	CaracteristicaAlimentar(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
