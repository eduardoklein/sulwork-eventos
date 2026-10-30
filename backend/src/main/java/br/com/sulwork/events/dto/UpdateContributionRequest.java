package br.com.sulwork.events.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The person and the item do not change. To zero the contribution, remove it. */
public record UpdateContributionRequest(
		@NotNull @Min(1) Integer quantity,
		@Size(max = 255) String note) {
}
