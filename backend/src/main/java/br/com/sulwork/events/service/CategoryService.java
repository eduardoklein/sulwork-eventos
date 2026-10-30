package br.com.sulwork.events.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.dto.CategoryRequest;
import br.com.sulwork.events.dto.CategoryResponse;
import br.com.sulwork.events.exception.BusinessRuleException;
import br.com.sulwork.events.exception.ConflictException;
import br.com.sulwork.events.exception.ResourceNotFoundException;
import br.com.sulwork.events.repository.EventCategoryRepository;

@Service
@Transactional
public class CategoryService {

	private final EventCategoryRepository categories;
	private final EventService eventService;

	public CategoryService(EventCategoryRepository categories, EventService eventService) {
		this.categories = categories;
		this.eventService = eventService;
	}

	public List<CategoryResponse> list(Long eventId) {
		eventService.findSyncedEntity(eventId);
		return categories.summarize(eventId, null);
	}

	public CategoryResponse find(Long eventId, Long id) {
		eventService.findSyncedEntity(eventId);
		findEntity(eventId, id);
		return summary(eventId, id);
	}

	public CategoryResponse create(Long eventId, CategoryRequest request) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireEditableMenu();
		String name = request.name().strip();
		requireFreeName(eventId, name, null);

		EventCategory category = new EventCategory(event, name, request.unit(), request.goalQuantity());
		saveOrConflict(category);
		return summary(eventId, category.getId());
	}

	public CategoryResponse update(Long eventId, Long id, CategoryRequest request) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireEditableMenu();
		EventCategory category = findEntityWithLock(eventId, id);
		String name = request.name().strip();
		requireFreeName(eventId, name, id);
		if (request.unit() != category.getUnit() && categories.hasItems(id)) {
			throw new BusinessRuleException("A unidade não pode mudar enquanto a categoria tem itens.");
		}

		category.setName(name);
		category.setUnit(request.unit());
		category.setGoalQuantity(request.goalQuantity());
		saveOrConflict(category);
		return summary(eventId, id);
	}

	public void delete(Long eventId, Long id) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireEditableMenu();
		EventCategory category = findEntityWithLock(eventId, id);
		if (categories.hasItems(id)) {
			throw new BusinessRuleException("Não é possível excluir uma categoria que ainda tem itens.");
		}
		categories.delete(category);
	}

	private EventCategory findEntity(Long eventId, Long id) {
		return categories.findByIdAndEventId(id, eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
	}

	private EventCategory findEntityWithLock(Long eventId, Long id) {
		return categories.findWithLock(id, eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
	}

	private CategoryResponse summary(Long eventId, Long id) {
		return categories.summarize(eventId, id).getFirst();
	}

	private void requireFreeName(Long eventId, String name, Long ignoredId) {
		if (categories.nameExists(eventId, name, ignoredId)) {
			throw duplicateName();
		}
	}

	/** The unique index of the database decides when two people create the same name at the same time. */
	private void saveOrConflict(EventCategory category) {
		try {
			categories.saveAndFlush(category);
		}
		catch (DataIntegrityViolationException ex) {
			throw duplicateName();
		}
	}

	private ConflictException duplicateName() {
		return new ConflictException("Já existe uma categoria com esse nome neste evento.");
	}

}
