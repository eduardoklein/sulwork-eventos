package br.com.sulwork.events.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.ItemTraits;
import br.com.sulwork.events.domain.ParticipantDiet;
import br.com.sulwork.events.dto.DietaryAnalysisResponse;
import br.com.sulwork.events.repository.EventItemRepository;
import br.com.sulwork.events.repository.EventParticipantRepository;

@Service
@Transactional
public class DietaryAnalysisService {

	private final EventItemRepository items;
	private final EventParticipantRepository participants;
	private final EventService eventService;

	public DietaryAnalysisService(EventItemRepository items, EventParticipantRepository participants,
			EventService eventService) {
		this.items = items;
		this.participants = participants;
		this.eventService = eventService;
	}

	public DietaryAnalysisResponse analyze(Long eventId) {
		eventService.findSyncedEntity(eventId);
		List<ParticipantDiet> people = participants.list(eventId).stream().map(DietaryAnalysisService::diet).toList();
		List<ItemTraits> traits = items.list(eventId, null).stream().map(DietaryAnalysisService::traits).toList();
		return DietaryAnalysisCalculator.analyze(people, traits);
	}

	private static ParticipantDiet diet(EventParticipant participant) {
		var collaborator = participant.getCollaborator();
		return new ParticipantDiet(participant.getId(), collaborator.getName(), collaborator.getRestrictions());
	}

	private static ItemTraits traits(EventItem item) {
		return new ItemTraits(item.getId(), item.getName(), item.getAllergens(), item.getCharacteristics());
	}

}
