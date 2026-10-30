package br.com.sulwork.events.service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import br.com.sulwork.events.domain.AlertLevel;
import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.DietaryConflict;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.ItemTraits;
import br.com.sulwork.events.domain.ParticipantDiet;
import br.com.sulwork.events.dto.DietaryAlertResponse;
import br.com.sulwork.events.dto.DietaryAnalysisResponse;
import br.com.sulwork.events.dto.ParticipantDietResponse;
import br.com.sulwork.events.dto.RestrictionSummaryResponse;
import br.com.sulwork.events.dto.UnsafeItemResponse;

/**
 * Crosses the restrictions of the participants with the allergens and characteristics of the items. An item with
 * nothing marked counts as safe, so the analysis is only as good as the item registration.
 */
public final class DietaryAnalysisCalculator {

	/** A person is warned when less than this share of the menu is safe. */
	private static final int WARNING_PERCENTAGE = 35;

	private DietaryAnalysisCalculator() {
	}

	public static DietaryAnalysisResponse analyze(List<ParticipantDiet> participants, List<ItemTraits> items) {
		List<ParticipantDietResponse> people = participants.stream().map(p -> participant(p, items)).toList();
		return new DietaryAnalysisResponse(items.size(), people, restrictionSummaries(participants, items),
				alerts(participants, people, items.size()));
	}

	private static ParticipantDietResponse participant(ParticipantDiet participant, List<ItemTraits> items) {
		List<UnsafeItemResponse> unsafe = items.stream()
				.map(item -> unsafeItem(item, conflictsOf(participant.restrictions(), item)))
				.filter(Objects::nonNull)
				.toList();
		return new ParticipantDietResponse(participant.participantId(), participant.name(),
				participant.restrictions().stream().sorted().toList(), items.size(), items.size() - unsafe.size(),
				unsafe);
	}

	private static UnsafeItemResponse unsafeItem(ItemTraits item, DietaryConflict conflict) {
		if (conflict.isEmpty()) {
			return null;
		}
		return new UnsafeItemResponse(item.itemId(), item.name(),
				conflict.allergens().stream().sorted(Comparator.naturalOrder()).toList(),
				conflict.characteristics().stream().sorted(Comparator.naturalOrder()).toList());
	}

	private static DietaryConflict conflictsOf(Set<DietaryRestriction> restrictions, ItemTraits item) {
		Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
		Set<FoodCharacteristic> characteristics = EnumSet.noneOf(FoodCharacteristic.class);
		for (DietaryRestriction restriction : restrictions) {
			DietaryConflict conflict = restriction.findConflicts(item.allergens(), item.characteristics());
			allergens.addAll(conflict.allergens());
			characteristics.addAll(conflict.characteristics());
		}
		return new DietaryConflict(allergens, characteristics);
	}

	private static List<RestrictionSummaryResponse> restrictionSummaries(List<ParticipantDiet> participants,
			List<ItemTraits> items) {
		Set<DietaryRestriction> present = EnumSet.noneOf(DietaryRestriction.class);
		participants.forEach(p -> present.addAll(p.restrictions()));
		return present.stream().map(restriction -> new RestrictionSummaryResponse(
				restriction,
				restriction.getDescription(),
				participants.stream().filter(p -> p.restrictions().contains(restriction)).count(),
				items.stream().filter(item -> conflictsOf(Set.of(restriction), item).isEmpty()).count(),
				items.size())).toList();
	}

	private static List<DietaryAlertResponse> alerts(List<ParticipantDiet> participants,
			List<ParticipantDietResponse> people, int totalItems) {
		boolean hasRestrictedPeople = participants.stream().anyMatch(p -> !p.restrictions().isEmpty());
		if (totalItems == 0 || !hasRestrictedPeople) {
			return List.of();
		}
		List<DietaryAlertResponse> alerts = people.stream()
				.filter(p -> !p.restrictions().isEmpty())
				.map(DietaryAnalysisCalculator::alertFor)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(DietaryAlertResponse::level))
				.toList();
		if (alerts.isEmpty()) {
			return List.of(new DietaryAlertResponse(AlertLevel.INFO,
					"Cardápio inclusivo: toda pessoa com restrição tem itens seguros.", null));
		}
		return alerts;
	}

	private static DietaryAlertResponse alertFor(ParticipantDietResponse person) {
		if (person.safeItems() == 0) {
			return new DietaryAlertResponse(AlertLevel.CRITICAL,
					person.name() + " não tem nenhum item seguro no cardápio.", person.participantId());
		}
		if (person.safeItems() * 100 < (long) WARNING_PERCENTAGE * person.totalItems()) {
			return new DietaryAlertResponse(AlertLevel.WARNING,
					"Menos de " + WARNING_PERCENTAGE + "% do cardápio é seguro para " + person.name() + " ("
							+ person.safeItems() + " de " + person.totalItems() + " itens).",
					person.participantId());
		}
		return null;
	}

}
