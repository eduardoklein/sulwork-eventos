package br.com.sulwork.events.dto;

import java.util.Set;

import br.com.sulwork.events.domain.DietaryRestriction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Restrictions are required: an empty list clears them, and omitting them does not erase them by mistake. */
public record UpdateCollaboratorRequest(
		@NotBlank @Size(max = 120) String name,
		@NotNull Set<DietaryRestriction> restrictions) {
}
