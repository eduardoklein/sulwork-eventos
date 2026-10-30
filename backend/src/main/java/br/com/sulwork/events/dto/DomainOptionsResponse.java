package br.com.sulwork.events.dto;

import java.util.List;

/** Every closed list of the domain, so the frontend does not repeat the labels. */
public record DomainOptionsResponse(
		List<OptionResponse> eventTypes,
		List<OptionResponse> eventStatuses,
		List<OptionResponse> measureUnits,
		List<OptionResponse> allergens,
		List<OptionResponse> foodCharacteristics,
		List<OptionResponse> dietaryRestrictions,
		List<OptionResponse> eventReadiness) {
}
