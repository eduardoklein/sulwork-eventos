package br.com.sulwork.events.dto;

import java.time.LocalDateTime;

import br.com.sulwork.events.domain.Contribution;
import br.com.sulwork.events.domain.MeasureUnit;

public record ContributionResponse(
		Long id,
		Long participantId,
		String participantName,
		Long itemId,
		String itemName,
		MeasureUnit unit,
		int quantity,
		String note,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static ContributionResponse from(Contribution contribution) {
		var participant = contribution.getParticipant();
		var item = contribution.getItem();
		return new ContributionResponse(
				contribution.getId(),
				participant.getId(),
				participant.getCollaborator().getName(),
				item.getId(),
				item.getName(),
				item.getUnit(),
				contribution.getQuantity(),
				contribution.getNote(),
				contribution.getCreatedAt(),
				contribution.getUpdatedAt());
	}

}
