package br.com.sulwork.eventos.dto;

import br.com.sulwork.eventos.domain.UnidadeMedida;

/**
 * Os números de uma categoria, todos na unidade dela. {@code percentual} é a cobertura ponderada (obrigatórios
 * valem 2, opcionais 1, e o déficit da meta entra no total) e é {@code null} para uma categoria sem itens e sem
 * meta. Obrigatórios e opcionais mostram 100 quando a categoria não tem nenhum item daquele tipo.
 */
public record CategoriaCoberturaResponse(
		Long categoriaId,
		String nome,
		UnidadeMedida unidade,
		int metaQuantidade,
		long totalItens,
		long quantidadeNecessaria,
		long quantidadeContribuida,
		long deficitDaMeta,
		long alvoEfetivo,
		boolean metaAtingida,
		Integer percentual,
		int percentualObrigatorios,
		int percentualOpcionais) {
}
