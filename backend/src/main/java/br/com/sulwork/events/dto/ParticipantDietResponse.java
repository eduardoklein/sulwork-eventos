package br.com.sulwork.events.dto;

import java.util.List;

import br.com.sulwork.events.domain.DietaryRestriction;

/** How the menu looks for one participant. Items not listed in {@code unsafeItems} are safe. */
public record ParticipantDietResponse(
		Long participantId,
		String name,
		List<DietaryRestriction> restrictions,
		long totalItems,
		long safeItems,
		List<UnsafeItemResponse> unsafeItems) {
}
