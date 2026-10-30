package br.com.sulwork.eventos.domain;

/**
 * O que uma categoria precisa e já recebeu. Todas as quantidades estão na unidade da categoria, então somar e
 * comparar aqui é válido. Somar categorias diferentes não é.
 */
public record QuantidadesDaCategoria(
		Long id,
		String nome,
		UnidadeMedida unidade,
		int meta,
		long totalItens,
		long necessarioObrigatorios,
		long necessarioOpcionais,
		long contribuidoObrigatorios,
		long contribuidoOpcionais) {

	/** A soma das quantidades necessárias dos itens. */
	public long necessario() {
		return necessarioObrigatorios + necessarioOpcionais;
	}

	public long contribuido() {
		return contribuidoObrigatorios + contribuidoOpcionais;
	}

	/** O quanto a meta passa da soma dos itens: o que ainda não foi pedido em nenhum item. */
	public long deficitDaMeta() {
		return Math.max(0, meta - necessario());
	}

	/** O maior valor entre a meta e a soma dos itens. */
	public long alvoEfetivo() {
		return Math.max(meta, necessario());
	}

	public boolean metaAtingida() {
		return contribuido() >= meta;
	}

	public boolean obrigatoriosCompletos() {
		return contribuidoObrigatorios == necessarioObrigatorios;
	}

	public boolean itensCompletos() {
		return obrigatoriosCompletos() && contribuidoOpcionais == necessarioOpcionais;
	}

	/** O denominador da cobertura ponderada: obrigatórios valem 2, opcionais valem 1, e o déficit da meta vale 1. */
	public long pesoNecessario() {
		return 2 * necessarioObrigatorios + necessarioOpcionais + deficitDaMeta();
	}

	public long pesoContribuido() {
		return 2 * contribuidoObrigatorios + contribuidoOpcionais;
	}

}
