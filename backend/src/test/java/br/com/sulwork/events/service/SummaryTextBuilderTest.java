package br.com.sulwork.events.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.sulwork.events.domain.EventReadiness;
import br.com.sulwork.events.domain.ItemProgress;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.dto.CoverageResponse;
import br.com.sulwork.events.service.SummaryTextBuilder.ContributionLine;
import br.com.sulwork.events.service.SummaryTextBuilder.Header;

class SummaryTextBuilderTest {

	private static final Header HEADER = new Header("Almoço de fim de ano", "Almoço",
			LocalDateTime.parse("2026-12-18T12:00:00"), LocalDateTime.parse("2026-12-16T18:00:00"),
			"America/Sao_Paulo", "Aberto");

	@Test
	void buildsTheWholeSummaryForAChatGroup() {
		CoverageResponse coverage = new CoverageResponse(EventReadiness.MANDATORY_SECURED, 47, 100, 25, 3, List.of());
		List<ItemProgress> items = List.of(
				item(1, "Lasagna", "Pratos", MeasureUnit.UNIT, true, 10, 6),
				item(2, "Salad", "Pratos", MeasureUnit.UNIT, true, 5, 5),
				item(3, "Juice", "Bebidas", MeasureUnit.MILLILITER, false, 2000, 1000));
		List<ContributionLine> lines = List.of(
				new ContributionLine("Ana", "Lasagna", 6, MeasureUnit.UNIT),
				new ContributionLine("Beto", "Juice", 1000, MeasureUnit.MILLILITER),
				new ContributionLine("Ana", "Salad", 5, MeasureUnit.UNIT));

		String text = SummaryTextBuilder.build(HEADER, coverage, items, lines);

		assertThat(text).isEqualTo("""
				*Almoço de fim de ano* (Almoço)
				Data: 18/12/2026 às 12:00 (America/Sao_Paulo)
				Prazo para contribuir: 16/12/2026 às 18:00
				Situação: Aberto

				*Cobertura*
				Geral: 47% | Obrigatórios: 100% | Opcionais: 25%
				Prontidão: Obrigatórios garantidos

				*O que ainda falta*
				Obrigatórios:
				- Lasagna (Pratos): faltam 4 (unidade)
				Opcionais:
				- Juice (Bebidas): faltam 1000 (mililitro)

				*Quem já vai levar*
				- Ana: Lasagna 6 (unidade); Salad 5 (unidade)
				- Beto: Juice 1000 (mililitro)""");
	}

	@Test
	void usesTheSingularWhenOnlyOneUnitIsMissing() {
		String text = SummaryTextBuilder.build(HEADER, coverage(), List.of(
				item(1, "Cake", "Doces", MeasureUnit.UNIT, true, 4, 3)), List.of());

		assertThat(text).contains("- Cake (Doces): falta 1 (unidade)");
	}

	@Test
	void listsTheLessCoveredItemsFirstInsideEachGroup() {
		String text = SummaryTextBuilder.build(HEADER, coverage(), List.of(
				item(1, "Half", "Food", MeasureUnit.UNIT, true, 10, 5),
				item(2, "Nothing", "Food", MeasureUnit.UNIT, true, 10, 0),
				item(3, "Almost", "Food", MeasureUnit.UNIT, true, 10, 9)), List.of());

		assertThat(text.indexOf("Nothing")).isLessThan(text.indexOf("Half"));
		assertThat(text.indexOf("Half")).isLessThan(text.indexOf("Almost"));
	}

	@Test
	void showsMandatoryItemsBeforeOptionalOnes() {
		String text = SummaryTextBuilder.build(HEADER, coverage(), List.of(
				item(1, "Optional thing", "Food", MeasureUnit.UNIT, false, 10, 0),
				item(2, "Mandatory thing", "Food", MeasureUnit.UNIT, true, 10, 9)), List.of());

		assertThat(text.indexOf("Obrigatórios:\n")).isLessThan(text.indexOf("Opcionais:\n"));
		assertThat(text.indexOf("Mandatory thing")).isLessThan(text.indexOf("Optional thing"));
	}

	@Test
	void leavesOutTheGroupThatHasNothingMissing() {
		String onlyOptional = SummaryTextBuilder.build(HEADER, coverage(), List.of(
				item(1, "Juice", "Drinks", MeasureUnit.UNIT, false, 10, 0)), List.of());
		String onlyMandatory = SummaryTextBuilder.build(HEADER, coverage(), List.of(
				item(1, "Rice", "Food", MeasureUnit.UNIT, true, 10, 0)), List.of());

		assertThat(onlyOptional).doesNotContain("Obrigatórios:\n").contains("Opcionais:\n");
		assertThat(onlyMandatory).contains("Obrigatórios:\n").doesNotContain("Opcionais:\n");
	}

	@Test
	void saysTheMenuIsCompleteWhenNothingIsMissing() {
		String text = SummaryTextBuilder.build(HEADER, coverage(), List.of(
				item(1, "Rice", "Food", MeasureUnit.UNIT, true, 10, 10)), List.of());

		assertThat(text).contains("*O que ainda falta*\nNada: o cardápio está completo.");
	}

	@Test
	void anEventWithoutItemsSaysSoAndSkipsTheMissingSection() {
		CoverageResponse empty = new CoverageResponse(EventReadiness.NO_ITEMS, null, 100, 100, 0, List.of());

		String text = SummaryTextBuilder.build(HEADER, empty, List.of(), List.of());

		assertThat(text).contains("*Cobertura*\nAinda não há itens no cardápio.")
				.doesNotContain("O que ainda falta")
				.contains("*Quem já vai levar*\nNinguém contribuiu ainda.");
	}

	@Test
	void groupsTheContributionsOfEachPersonInOrderOfFirstAppearance() {
		String text = SummaryTextBuilder.build(HEADER, coverage(), List.of(), List.of(
				new ContributionLine("Beto", "Rice", 1, MeasureUnit.UNIT),
				new ContributionLine("Ana", "Cake", 2, MeasureUnit.PORTION),
				new ContributionLine("Beto", "Juice", 500, MeasureUnit.MILLILITER)));

		assertThat(text).endsWith("- Beto: Rice 1 (unidade); Juice 500 (mililitro)\n- Ana: Cake 2 (porção)");
	}

	@Test
	void theTextEndsWithoutTrailingBlankLines() {
		String text = SummaryTextBuilder.build(HEADER, coverage(), List.of(), List.of());

		assertThat(text).isEqualTo(text.stripTrailing());
	}

	@Test
	void formatsTheDatesAndNamesTheTimeZone() {
		String text = SummaryTextBuilder.build(HEADER, coverage(), List.of(), List.of());

		assertThat(text).startsWith("*Almoço de fim de ano* (Almoço)\nData: 18/12/2026 às 12:00 (America/Sao_Paulo)\n"
				+ "Prazo para contribuir: 16/12/2026 às 18:00\nSituação: Aberto\n");
	}

	private static CoverageResponse coverage() {
		return new CoverageResponse(EventReadiness.IN_PROGRESS, 50, 50, 50, 1, List.of());
	}

	private static ItemProgress item(long id, String name, String category, MeasureUnit unit, boolean mandatory,
			int required, long contributed) {
		return new ItemProgress(id, name, 1L, category, unit, 0, required, contributed, mandatory, Set.of(), Set.of(),
				false);
	}

}
