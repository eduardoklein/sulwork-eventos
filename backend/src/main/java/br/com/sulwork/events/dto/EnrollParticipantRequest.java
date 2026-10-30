package br.com.sulwork.events.dto;

import java.util.Set;

import br.com.sulwork.events.domain.DietaryRestriction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code name} and {@code restrictions} only apply to a new CPF. For a person already registered, the enrollment
 * changes nothing: the record changes only in {@code PUT /api/collaborators/{id}}.
 */
public record EnrollParticipantRequest(
		@NotBlank String cpf,
		@Size(max = 120) String name,
		Set<DietaryRestriction> restrictions) {
}
