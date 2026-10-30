package br.com.sulwork.events.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.ItemProgress;
import br.com.sulwork.events.dto.SuggestionResponse;
import br.com.sulwork.events.exception.ResourceNotFoundException;
import br.com.sulwork.events.repository.ContributionRepository;
import br.com.sulwork.events.repository.ContributionRepository.ItemTotal;
import br.com.sulwork.events.repository.EventItemRepository;
import br.com.sulwork.events.repository.EventParticipantRepository;

@Service
@Transactional
public class SuggestionService {

	private final EventItemRepository items;
	private final ContributionRepository contributions;
	private final EventParticipantRepository participants;
	private final EventService eventService;
	private final Clock clock;

	public SuggestionService(EventItemRepository items, ContributionRepository contributions,
			EventParticipantRepository participants, EventService eventService, Clock clock) {
		this.items = items;
		this.contributions = contributions;
		this.participants = participants;
		this.eventService = eventService;
		this.clock = clock;
	}

	/** An event that no longer accepts contributions has nothing to suggest. */
	public List<SuggestionResponse> suggest(Long eventId, Long participantId) {
		Event event = eventService.findSyncedEntity(eventId);
		EventParticipant participant = participants.find(participantId, eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Participante não encontrado."));
		if (!event.isOpenForContributions(clock.instant())) {
			return List.of();
		}
		Map<Long, Long> totals = contributions.sumByItemOfEvent(eventId).stream()
				.collect(Collectors.toMap(ItemTotal::itemId, ItemTotal::total));
		Set<Long> ownItems = contributions.list(eventId, null, participantId).stream()
				.map(contribution -> contribution.getItem().getId()).collect(Collectors.toSet());
		List<ItemProgress> progress = items.list(eventId, null).stream()
				.map(item -> progress(item, totals.getOrDefault(item.getId(), 0L), ownItems.contains(item.getId())))
				.toList();
		return SuggestionRanker.rank(progress, participant.getCollaborator().getRestrictions());
	}

	private static ItemProgress progress(EventItem item, long contributed, boolean contributedByParticipant) {
		var category = item.getCategory();
		return new ItemProgress(item.getId(), item.getName(), category.getId(), category.getName(), item.getUnit(),
				category.getGoalQuantity(), item.getRequiredQuantity(), contributed, item.isMandatory(),
				item.getAllergens(), item.getCharacteristics(), contributedByParticipant);
	}

}
