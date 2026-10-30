package br.com.sulwork.events.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.Contribution;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.dto.UpdateContributionRequest;
import br.com.sulwork.events.dto.ContributionResponse;
import br.com.sulwork.events.dto.CreateContributionRequest;
import br.com.sulwork.events.exception.BusinessRuleException;
import br.com.sulwork.events.exception.ConflictException;
import br.com.sulwork.events.exception.ResourceNotFoundException;
import br.com.sulwork.events.repository.ContributionRepository;
import br.com.sulwork.events.repository.EventItemRepository;
import br.com.sulwork.events.repository.EventParticipantRepository;

/**
 * Every change to the contributions of an item locks the item row first. This way, two simultaneous requests do
 * not read the same free balance, and the sum never exceeds the required quantity.
 */
@Service
@Transactional
public class ContributionService {

	private final ContributionRepository contributions;
	private final EventItemRepository items;
	private final EventParticipantRepository participants;
	private final EventService eventService;
	private final Clock clock;

	public ContributionService(ContributionRepository contributions, EventItemRepository items,
			EventParticipantRepository participants, EventService eventService, Clock clock) {
		this.contributions = contributions;
		this.items = items;
		this.participants = participants;
		this.eventService = eventService;
		this.clock = clock;
	}

	public List<ContributionResponse> list(Long eventId, Long itemId, Long participantId) {
		eventService.findSyncedEntity(eventId);
		return contributions.list(eventId, itemId, participantId).stream().map(ContributionResponse::from).toList();
	}

	public ContributionResponse find(Long eventId, Long id) {
		eventService.findSyncedEntity(eventId);
		return ContributionResponse.from(findEntity(eventId, id));
	}

	public ContributionResponse create(Long eventId, CreateContributionRequest request) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireOpenContributions(clock.instant());
		EventParticipant participant = participants.find(request.participantId(), eventId)
				.orElseThrow(() -> new BusinessRuleException("Participante não encontrado neste evento."));
		EventItem item = items.findWithLock(request.itemId(), eventId)
				.orElseThrow(() -> new BusinessRuleException("Item não encontrado neste evento."));
		if (contributions.existsByParticipantIdAndItemId(participant.getId(), item.getId())) {
			throw new ConflictException("Esta pessoa já contribui com este item. Edite a contribuição.");
		}
		long remaining = item.getRequiredQuantity() - contributions.sumByItem(item.getId());
		if (request.quantity() > remaining) {
			throw new BusinessRuleException(remaining == 0
					? "Este item já está completo."
					: "Só cabem mais " + remaining + " (" + unitDescription(item) + ") neste item.");
		}

		Contribution contribution = new Contribution(participant, item, request.quantity(), request.note());
		contributions.saveAndFlush(contribution);
		return ContributionResponse.from(contribution);
	}

	public ContributionResponse update(Long eventId, Long id, UpdateContributionRequest request) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireOpenContributions(clock.instant());
		EventItem item = lockItemOfContribution(eventId, id);
		Contribution contribution = findEntity(eventId, id);
		long maximum = item.getRequiredQuantity() - contributions.sumByItemExcept(item.getId(), id);
		if (request.quantity() > maximum) {
			throw new BusinessRuleException(
					"O máximo para esta contribuição é " + maximum + " (" + unitDescription(item) + ").");
		}

		contribution.setQuantity(request.quantity());
		contribution.setNote(request.note());
		contributions.saveAndFlush(contribution);
		return ContributionResponse.from(contribution);
	}

	public void remove(Long eventId, Long id) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireOpenContributions(clock.instant());
		lockItemOfContribution(eventId, id);
		contributions.delete(findEntity(eventId, id));
	}

	/** Finds the item without loading the contribution, locks the item and only then reads the up-to-date contribution. */
	private EventItem lockItemOfContribution(Long eventId, Long id) {
		Long itemId = contributions.findItemId(id, eventId).orElseThrow(this::notFound);
		return items.findWithLock(itemId, eventId).orElseThrow(this::notFound);
	}

	private Contribution findEntity(Long eventId, Long id) {
		return contributions.find(id, eventId).orElseThrow(this::notFound);
	}

	private ResourceNotFoundException notFound() {
		return new ResourceNotFoundException("Contribuição não encontrada.");
	}

	private String unitDescription(EventItem item) {
		return item.getUnit().getDescription().toLowerCase();
	}

}
