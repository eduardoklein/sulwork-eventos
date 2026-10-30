package br.com.sulwork.eventos.dto;

import java.util.List;

import br.com.sulwork.eventos.domain.ProntidaoEvento;

/**
 * A cobertura do evento. Os percentuais gerais são a média das categorias, e {@code percentualGeral} é
 * {@code null} quando nenhuma categoria tem itens nem meta.
 */
public record CoberturaResponse(
		ProntidaoEvento prontidao,
		Integer percentualGeral,
		int percentualObrigatorios,
		int percentualOpcionais,
		long totalItens,
		List<CategoriaCoberturaResponse> categorias) {
}
