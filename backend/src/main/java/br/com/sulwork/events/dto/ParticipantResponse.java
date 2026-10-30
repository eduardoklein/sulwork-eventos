package br.com.sulwork.events.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.validation.CpfMask;

/** {@code id} is the enrollment id in this event. {@code collaboratorId} is the person id. */
public record ParticipantResponse(
		Long id,
		Long collaboratorId,
		String name,
		String cpf,
		List<DietaryRestriction> restrictions,
		LocalDateTime enrolledAt) {

	public static ParticipantResponse from(EventParticipant participant) {
		var collaborator = participant.getCollaborator();
		return new ParticipantResponse(
				participant.getId(),
				collaborator.getId(),
				collaborator.getName(),
				CpfMask.mask(collaborator.getCpf()),
				collaborator.getRestrictions().stream().sorted(Comparator.naturalOrder()).toList(),
				participant.getCreatedAt());
	}

}
