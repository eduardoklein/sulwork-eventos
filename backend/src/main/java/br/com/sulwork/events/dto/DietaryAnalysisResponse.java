package br.com.sulwork.events.dto;

import java.util.List;

public record DietaryAnalysisResponse(
		long totalItems,
		List<ParticipantDietResponse> participants,
		List<RestrictionSummaryResponse> restrictions,
		List<DietaryAlertResponse> alerts) {
}
