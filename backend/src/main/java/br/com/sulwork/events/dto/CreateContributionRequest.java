package br.com.sulwork.events.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code participantId} is the enrollment id in the event. */
public record CreateContributionRequest(
		@NotNull Long participantId,
		@NotNull Long itemId,
		@NotNull @Min(1) Integer quantity,
		@Size(max = 255) String note) {
}
