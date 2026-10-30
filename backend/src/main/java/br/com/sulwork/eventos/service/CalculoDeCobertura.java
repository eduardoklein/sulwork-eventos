package br.com.sulwork.eventos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import br.com.sulwork.eventos.domain.ProntidaoEvento;
import br.com.sulwork.eventos.domain.QuantidadesDaCategoria;
import br.com.sulwork.eventos.dto.CategoriaCoberturaResponse;
import br.com.sulwork.eventos.dto.CoberturaResponse;

/**
 * Cobertura do evento. A conta pesada roda dentro de cada categoria, onde a unidade é uma só. O geral é a média
 * das categorias: somar mililitros com unidades faria a unidade maior dominar o resultado.
 */
public final class CalculoDeCobertura {

	private static final int ESCALA = 12;
	private static final BigDecimal METADE = new BigDecimal("0.5");

	private CalculoDeCobertura() {
	}

	public static CoberturaResponse calcular(List<QuantidadesDaCategoria> categorias) {
		BigDecimal geral = media(categorias, c -> c.pesoNecessario() > 0, c -> razao(c.pesoContribuido(), c.pesoNecessario()));
		BigDecimal obrigatorios = media(categorias, c -> c.necessarioObrigatorios() > 0,
				c -> razao(c.contribuidoObrigatorios(), c.necessarioObrigatorios()));
		BigDecimal opcionais = media(categorias, c -> c.necessarioOpcionais() > 0,
				c -> razao(c.contribuidoOpcionais(), c.necessarioOpcionais()));
		long totalItens = categorias.stream().mapToLong(QuantidadesDaCategoria::totalItens).sum();

		return new CoberturaResponse(
				prontidao(categorias, totalItens, obrigatorios),
				geral == null ? null : percentual(geral),
				obrigatorios == null ? 100 : percentual(obrigatorios),
				opcionais == null ? 100 : percentual(opcionais),
				totalItens,
				categorias.stream().map(CalculoDeCobertura::categoria).toList());
	}

	private static ProntidaoEvento prontidao(List<QuantidadesDaCategoria> categorias, long totalItens,
			BigDecimal obrigatorios) {
		if (totalItens == 0) {
			return ProntidaoEvento.SEM_ITENS;
		}
		if (obrigatorios != null && obrigatorios.compareTo(METADE) < 0) {
			return ProntidaoEvento.CRITICO;
		}
		if (categorias.stream().allMatch(c -> c.itensCompletos() && c.metaAtingida())) {
			return ProntidaoEvento.COMPLETO;
		}
		if (categorias.stream().allMatch(QuantidadesDaCategoria::obrigatoriosCompletos)) {
			return ProntidaoEvento.OBRIGATORIOS_GARANTIDOS;
		}
		return ProntidaoEvento.EM_ANDAMENTO;
	}

	private static CategoriaCoberturaResponse categoria(QuantidadesDaCategoria c) {
		return new CategoriaCoberturaResponse(
				c.id(),
				c.nome(),
				c.unidade(),
				c.meta(),
				c.totalItens(),
				c.necessario(),
				c.contribuido(),
				c.deficitDaMeta(),
				c.alvoEfetivo(),
				c.metaAtingida(),
				c.pesoNecessario() == 0 ? null : percentualInteiro(c.pesoContribuido(), c.pesoNecessario()),
				c.necessarioObrigatorios() == 0 ? 100
						: percentualInteiro(c.contribuidoObrigatorios(), c.necessarioObrigatorios()),
				c.necessarioOpcionais() == 0 ? 100
						: percentualInteiro(c.contribuidoOpcionais(), c.necessarioOpcionais()));
	}

	/** A média das razões das categorias que passam no filtro, ou {@code null} se nenhuma passa. */
	private static BigDecimal media(List<QuantidadesDaCategoria> categorias, Predicate<QuantidadesDaCategoria> conta,
			Function<QuantidadesDaCategoria, BigDecimal> razao) {
		List<BigDecimal> razoes = categorias.stream().filter(conta).map(razao).toList();
		if (razoes.isEmpty()) {
			return null;
		}
		BigDecimal soma = razoes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return soma.divide(BigDecimal.valueOf(razoes.size()), ESCALA, RoundingMode.HALF_UP);
	}

	private static BigDecimal razao(long parte, long total) {
		return BigDecimal.valueOf(parte).divide(BigDecimal.valueOf(total), ESCALA, RoundingMode.HALF_UP);
	}

	/** Arredonda para baixo: só mostra 100 quando está completo. */
	private static int percentual(BigDecimal razao) {
		return razao.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.DOWN).intValue();
	}

	/** Conta só com inteiros, sem erro de arredondamento de ponto flutuante. */
	private static int percentualInteiro(long parte, long total) {
		return (int) (parte * 100 / total);
	}

}
