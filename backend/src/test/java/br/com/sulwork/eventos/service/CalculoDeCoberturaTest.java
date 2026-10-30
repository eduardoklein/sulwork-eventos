package br.com.sulwork.eventos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.sulwork.eventos.domain.ProntidaoEvento;
import br.com.sulwork.eventos.domain.QuantidadesDaCategoria;
import br.com.sulwork.eventos.domain.UnidadeMedida;
import br.com.sulwork.eventos.dto.CategoriaCoberturaResponse;
import br.com.sulwork.eventos.dto.CoberturaResponse;

class CalculoDeCoberturaTest {

	@Test
	void exemploDoEnunciadoDaFormula_obrigatorios6De6_opcionais0De4_da75Porcento() {
		CoberturaResponse r = calcular(categoria("Salgados", 0, 10, 6, 4, 6, 0));

		assertThat(r.percentualGeral()).isEqualTo(75);
		assertThat(r.percentualObrigatorios()).isEqualTo(100);
		assertThat(r.percentualOpcionais()).isZero();
		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.OBRIGATORIOS_GARANTIDOS);
		assertThat(r.categorias().get(0).percentual()).isEqualTo(75);
	}

	@Test
	void deficitDaMetaEntraNoDenominador() {
		// Itens completos (10 de 10), mas a meta é 20: faltam 10 que nenhum item pediu.
		CoberturaResponse r = calcular(categoria("Doces", 20, 10, 10, 0, 10, 0));

		CategoriaCoberturaResponse c = r.categorias().get(0);
		assertThat(c.deficitDaMeta()).isEqualTo(10);
		assertThat(c.alvoEfetivo()).isEqualTo(20);
		assertThat(c.metaAtingida()).isFalse();
		assertThat(c.percentual()).isEqualTo(66); // 20 / 30
		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.OBRIGATORIOS_GARANTIDOS);
	}

	@Test
	void metaMenorQueASomaDosItensNaoGeraDeficit() {
		CategoriaCoberturaResponse c = calcular(categoria("Doces", 5, 10, 10, 0, 4, 0)).categorias().get(0);

		assertThat(c.deficitDaMeta()).isZero();
		assertThat(c.alvoEfetivo()).isEqualTo(10);
		assertThat(c.metaAtingida()).isFalse();
	}

	@Test
	void categoriaComMetaESemItensContaComoZero() {
		CoberturaResponse r = calcular(categoria("Vazia", 20, 0, 0, 0, 0, 0));

		assertThat(r.categorias().get(0).percentual()).isZero();
		assertThat(r.percentualGeral()).isZero();
		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.SEM_ITENS);
	}

	@Test
	void categoriaSemItensESemMetaFicaForaDaMedia() {
		CoberturaResponse r = calcular(
				categoria("Cheia", 0, 10, 10, 0, 10, 0),
				categoria("Vazia", 0, 0, 0, 0, 0, 0));

		assertThat(r.categorias().get(1).percentual()).isNull();
		assertThat(r.percentualGeral()).isEqualTo(100);
	}

	@Test
	void unidadesDiferentesNaoSeSobrepoem() {
		// Mililitros completos e unidades vazias: a média das categorias dá 50%.
		// Somar tudo daria 2 x 5000 / (2 x 5000 + 2 x 40) = 99%, porque o mililitro dominaria.
		CoberturaResponse r = calcular(
				categoria("Bebidas", 0, 1, 5000, 0, 5000, 0),
				categoria("Salgados", 0, 1, 40, 0, 0, 0));

		assertThat(r.percentualGeral()).isEqualTo(50);
		assertThat(r.percentualObrigatorios()).isEqualTo(50);
	}

	@Test
	void eventoSemCategoriasESemItens() {
		CoberturaResponse r = calcular();

		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.SEM_ITENS);
		assertThat(r.percentualGeral()).isNull();
		assertThat(r.percentualObrigatorios()).isEqualTo(100);
		assertThat(r.percentualOpcionais()).isEqualTo(100);
		assertThat(r.totalItens()).isZero();
		assertThat(r.categorias()).isEmpty();
	}

	@Test
	void semItensObrigatoriosACoberturaDosObrigatoriosEh100() {
		CoberturaResponse r = calcular(categoria("Doces", 0, 2, 0, 10, 0, 3));

		assertThat(r.percentualObrigatorios()).isEqualTo(100);
		assertThat(r.percentualOpcionais()).isEqualTo(30);
		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.OBRIGATORIOS_GARANTIDOS);
	}

	@Test
	void criticoQuandoAMediaDosObrigatoriosFicaAbaixoDe50() {
		CoberturaResponse r = calcular(categoria("Pratos", 0, 4, 4, 0, 1, 0));

		assertThat(r.percentualObrigatorios()).isEqualTo(25);
		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.CRITICO);
	}

	@Test
	void exatamente50NaoEhCritico() {
		CoberturaResponse r = calcular(categoria("Pratos", 0, 4, 4, 0, 2, 0));

		assertThat(r.percentualObrigatorios()).isEqualTo(50);
		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.EM_ANDAMENTO);
	}

	@Test
	void completoQuandoTodosOsItensEstaoCompletosEAsMetasForamAtingidas() {
		CoberturaResponse r = calcular(
				categoria("Pratos", 8, 4, 6, 4, 6, 4),
				categoria("Bebidas", 0, 1, 10, 0, 10, 0));

		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.COMPLETO);
		assertThat(r.percentualGeral()).isEqualTo(100);
		assertThat(r.categorias()).allMatch(CategoriaCoberturaResponse::metaAtingida);
	}

	@Test
	void itensCompletosMasMetaNaoAtingidaNaoEhCompleto() {
		CoberturaResponse r = calcular(categoria("Pratos", 20, 2, 5, 5, 5, 5));

		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.OBRIGATORIOS_GARANTIDOS);
		assertThat(r.percentualGeral()).isLessThan(100);
	}

	@Test
	void umaCategoriaComObrigatorioFaltandoImpedeObrigatoriosGarantidos() {
		CoberturaResponse r = calcular(
				categoria("Pratos", 0, 2, 10, 0, 10, 0),
				categoria("Bebidas", 0, 2, 10, 0, 9, 0));

		assertThat(r.prontidao()).isEqualTo(ProntidaoEvento.EM_ANDAMENTO);
	}

	@Test
	void percentuaisArredondamParaBaixoSemErroDeFlutuante() {
		assertThat(calcular(categoria("A", 0, 1, 1, 0, 0, 0)).categorias().get(0).percentualObrigatorios()).isZero();
		assertThat(calcular(categoria("A", 0, 1, 3, 0, 1, 0)).categorias().get(0).percentualObrigatorios()).isEqualTo(33);
		assertThat(calcular(categoria("A", 0, 1, 200, 0, 199, 0)).categorias().get(0).percentualObrigatorios())
				.isEqualTo(99);
		assertThat(calcular(categoria("A", 0, 1, 100, 0, 29, 0)).categorias().get(0).percentualObrigatorios())
				.isEqualTo(29);
		assertThat(calcular(categoria("A", 0, 1, 7, 0, 7, 0)).categorias().get(0).percentualObrigatorios())
				.isEqualTo(100);
	}

	@Test
	void mediaDeFracoesQueSomamUmInteiroNaoPerdeUmPonto() {
		CoberturaResponse r = calcular(
				categoria("A", 0, 1, 3, 0, 1, 0),
				categoria("B", 0, 1, 3, 0, 2, 0),
				categoria("C", 0, 1, 3, 0, 3, 0));

		assertThat(r.percentualObrigatorios()).isEqualTo(66); // (1/3 + 2/3 + 1) / 3 = 2/3
		assertThat(calcular(categoria("A", 0, 1, 3, 0, 1, 0), categoria("B", 0, 1, 3, 0, 2, 0))
				.percentualObrigatorios()).isEqualTo(50);
	}

	@Test
	void somaOTotalDeItensDeTodasAsCategorias() {
		CoberturaResponse r = calcular(categoria("A", 0, 3, 1, 0, 0, 0), categoria("B", 0, 4, 1, 0, 0, 0));

		assertThat(r.totalItens()).isEqualTo(7);
	}

	private static CoberturaResponse calcular(QuantidadesDaCategoria... categorias) {
		return CalculoDeCobertura.calcular(List.of(categorias));
	}

	/** Os números são, na ordem: meta, total de itens, necessário e contribuído, obrigatórios e opcionais. */
	private static QuantidadesDaCategoria categoria(String nome, int meta, long totalItens, long necessarioObrigatorios,
			long necessarioOpcionais, long contribuidoObrigatorios, long contribuidoOpcionais) {
		return new QuantidadesDaCategoria(1L, nome, UnidadeMedida.UNIDADE, meta, totalItens, necessarioObrigatorios,
				necessarioOpcionais, contribuidoObrigatorios, contribuidoOpcionais);
	}

}
