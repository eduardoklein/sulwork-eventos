package br.com.sulwork.events.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.CategoryQuantities;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.dto.CoverageResponse;
import br.com.sulwork.events.repository.CategoryCoverage;
import br.com.sulwork.events.repository.EventItemRepository;

@Service
@Transactional
public class CoverageService {

	private final EventItemRepository items;
	private final EventService eventService;

	public CoverageService(EventItemRepository items, EventService eventService) {
		this.items = items;
		this.eventService = eventService;
	}

	public CoverageResponse calculate(Long eventId) {
		eventService.findSyncedEntity(eventId);
		List<CategoryQuantities> categories = items.coveragePerCategory(eventId).stream()
				.map(CoverageService::quantities).toList();
		return CoverageCalculator.calculate(categories);
	}

	private static CategoryQuantities quantities(CategoryCoverage c) {
		return new CategoryQuantities(c.getCategoryId(), c.getName(), MeasureUnit.valueOf(c.getUnit()),
				c.getGoalQuantity(), c.getTotalItems(), c.getMandatoryRequired(), c.getOptionalRequired(),
				c.getMandatoryContributed(), c.getOptionalContributed());
	}

}
