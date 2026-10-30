package br.com.sulwork.events.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.sulwork.events.domain.ItemProgress;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.dto.CoverageResponse;

/**
 * Builds the text of the event summary for a chat group. Section titles use single asterisks, which WhatsApp and
 * Slack show in bold. Dietary restrictions and CPFs stay out: the text goes to a group.
 */
public final class SummaryTextBuilder {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

	private SummaryTextBuilder() {
	}

	public record Header(String name, String typeDescription, LocalDateTime dateTime, LocalDateTime changeDeadline,
			String timeZone, String statusDescription) {
	}

	public record ContributionLine(String participantName, String itemName, int quantity, MeasureUnit unit) {
	}

	public static String build(Header header, CoverageResponse coverage, List<ItemProgress> items,
			List<ContributionLine> contributions) {
		StringBuilder text = new StringBuilder();
		text.append('*').append(header.name()).append("* (").append(header.typeDescription()).append(")\n");
		text.append("Data: ").append(DATE_TIME.format(header.dateTime())).append(" (").append(header.timeZone())
				.append(")\n");
		text.append("Prazo para contribuir: ").append(DATE_TIME.format(header.changeDeadline())).append('\n');
		text.append("Situação: ").append(header.statusDescription()).append("\n\n");
		coverage(text, coverage);
		if (!items.isEmpty()) {
			missing(text, items);
		}
		contributors(text, contributions);
		return text.toString().stripTrailing();
	}

	private static void coverage(StringBuilder text, CoverageResponse coverage) {
		text.append("*Cobertura*\n");
		if (coverage.overallPercentage() == null || coverage.totalItems() == 0) {
			text.append("Ainda não há itens no cardápio.\n\n");
			return;
		}
		text.append("Geral: ").append(coverage.overallPercentage()).append("% | Obrigatórios: ")
				.append(coverage.mandatoryPercentage()).append("% | Opcionais: ")
				.append(coverage.optionalPercentage()).append("%\n");
		text.append("Prontidão: ").append(coverage.readiness().getDescription()).append("\n\n");
	}

	private static void missing(StringBuilder text, List<ItemProgress> items) {
		List<ItemProgress> pending = items.stream().filter(item -> item.pendingQuantity() > 0).toList();
		text.append("*O que ainda falta*\n");
		if (pending.isEmpty()) {
			text.append("Nada: o cardápio está completo.\n\n");
			return;
		}
		missingGroup(text, "Obrigatórios:", pending.stream().filter(ItemProgress::mandatory).toList());
		missingGroup(text, "Opcionais:", pending.stream().filter(item -> !item.mandatory()).toList());
		text.append('\n');
	}

	private static void missingGroup(StringBuilder text, String title, List<ItemProgress> group) {
		if (group.isEmpty()) {
			return;
		}
		text.append(title).append('\n');
		group.stream()
				.sorted(Comparator.comparingDouble(SummaryTextBuilder::coverageOf).thenComparing(ItemProgress::itemId))
				.forEach(item -> {
					long pending = item.pendingQuantity();
					text.append("- ").append(item.name()).append(" (").append(item.categoryName()).append("): ")
							.append(pending == 1 ? "falta " : "faltam ").append(pending).append(" (")
							.append(item.unit().getDescription().toLowerCase()).append(")\n");
				});
	}

	private static double coverageOf(ItemProgress item) {
		return (double) Math.min(item.contributedQuantity(), item.requiredQuantity()) / item.requiredQuantity();
	}

	private static void contributors(StringBuilder text, List<ContributionLine> contributions) {
		text.append("*Quem já vai levar*\n");
		if (contributions.isEmpty()) {
			text.append("Ninguém contribuiu ainda.\n");
			return;
		}
		Map<String, List<ContributionLine>> byPerson = new LinkedHashMap<>();
		contributions.forEach(line -> byPerson.computeIfAbsent(line.participantName(), name -> new ArrayList<>())
				.add(line));
		byPerson.forEach((name, lines) -> text.append("- ").append(name).append(": ")
				.append(lines.stream()
						.map(line -> line.itemName() + " " + line.quantity() + " ("
								+ line.unit().getDescription().toLowerCase() + ")")
						.collect(Collectors.joining("; ")))
				.append('\n'));
	}

}
