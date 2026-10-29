package br.com.sulwork.eventos.dto;

import br.com.sulwork.eventos.domain.UnidadeMedida;

/** Os totais somam os itens da categoria: o planejado e o que as pessoas já contribuíram. */
public record CategoriaResponse(
		Long id,
		String nome,
		UnidadeMedida unidade,
		int metaQuantidade,
		long totalItens,
		long quantidadePlanejada,
		long quantidadeContribuida) {
}
