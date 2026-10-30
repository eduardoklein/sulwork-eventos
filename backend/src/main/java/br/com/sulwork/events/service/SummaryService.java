package br.com.sulwork.events.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.ItemProgress;
import br.com.sulwork.events.dto.SummaryResponse;
import br.com.sulwork.events.repository.ContributionRepository;
import br.com.sulwork.events.repository.ContributionRepository.ItemTotal;
import br.com.sulwork.events.repository.EventItemRepository;
import br.com.sulwork.events.service.SummaryTextBuilder.ContributionLine;
import br.com.sulwork.events.service.SummaryTextBuilder.Header;

@Service
@Transactional
public class SummaryService {

	private final EventItemRepository items;
	private final ContributionRepository contributions;
	private final EventService eventService;
	private final CoverageService coverageService;

	public SummaryService(EventItemRepository items, ContributionRepository contributions, EventService eventService,
			CoverageService coverageService) {
		this.items = items;
		this.contributions = contributions;
		this.eventService = eventService;
		this.coverageService = coverageService;
	}

	public SummaryResponse summarize(Long eventId) {
		Event event = eventService.findSyncedEntity(eventId);
		Map<Long, Long> totals = contributions.sumByItemOfEvent(eventId).stream()
				.collect(Collectors.toMap(ItemTotal::itemId, ItemTotal::total));
		List<ItemProgress> progress = items.list(eventId, null).stream()
				.map(item -> progress(item, totals.getOrDefault(item.getId(), 0L))).toList();
		List<ContributionLine> lines = contributions.list(eventId, null, null).stream()
				.map(c -> new ContributionLine(c.getParticipant().getCollaborator().getName(), c.getItem().getName(),
						c.getQuantity(), c.getItem().getUnit()))
				.toList();
		Header header = new Header(event.getName(), event.getType().getDescription(), event.getDateTime(),
				event.getChangeDeadline(), event.getTimeZone().getId(), event.getStatus().getDescription());
		return new SummaryResponse(
				SummaryTextBuilder.build(header, coverageService.calculate(eventId), progress, lines));
	}

	private static ItemProgress progress(EventItem item, long contributed) {
		var category = item.getCategory();
		return new ItemProgress(item.getId(), item.getName(), category.getId(), category.getName(), item.getUnit(),
				category.getGoalQuantity(), item.getRequiredQuantity(), contributed, item.isMandatory(),
				item.getAllergens(), item.getCharacteristics(), false);
	}

}
