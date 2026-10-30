package br.com.sulwork.events.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.dto.UpdateItemRequest;
import br.com.sulwork.events.dto.CreateItemRequest;
import br.com.sulwork.events.dto.ItemResponse;
import br.com.sulwork.events.exception.BusinessRuleException;
import br.com.sulwork.events.exception.ConflictException;
import br.com.sulwork.events.exception.ResourceNotFoundException;
import br.com.sulwork.events.repository.EventCategoryRepository;
import br.com.sulwork.events.repository.ContributionRepository;
import br.com.sulwork.events.repository.ContributionRepository.ItemTotal;
import br.com.sulwork.events.repository.EventItemRepository;

@Service
@Transactional
public class ItemService {

	private final EventItemRepository items;
	private final EventCategoryRepository categories;
	private final ContributionRepository contributions;
	private final EventService eventService;

	public ItemService(EventItemRepository items, EventCategoryRepository categories,
			ContributionRepository contributions, EventService eventService) {
		this.items = items;
		this.categories = categories;
		this.contributions = contributions;
		this.eventService = eventService;
	}

	public List<ItemResponse> list(Long eventId, Long categoryId) {
		eventService.findSyncedEntity(eventId);
		Map<Long, Long> totals = contributions.sumByItemOfEvent(eventId).stream()
				.collect(Collectors.toMap(ItemTotal::itemId, ItemTotal::total));
		return items.list(eventId, categoryId).stream()
				.map(item -> ItemResponse.from(item, totals.getOrDefault(item.getId(), 0L)))
				.toList();
	}

	public ItemResponse find(Long eventId, Long id) {
		eventService.findSyncedEntity(eventId);
		return response(findEntity(eventId, id));
	}

	public ItemResponse create(Long eventId, CreateItemRequest request) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireEditableMenu();
		EventCategory category = categories.findByIdAndEventId(request.categoryId(), eventId)
				.orElseThrow(() -> new BusinessRuleException("Categoria não encontrada neste evento."));
		String name = request.name().strip();
		requireFreeName(category.getId(), name, null);

		EventItem item = new EventItem(event, category, name, request.requiredQuantity(),
				Boolean.TRUE.equals(request.mandatory()));
		replaceContents(item.getAllergens(), request.allergens());
		replaceContents(item.getCharacteristics(), request.characteristics());
		saveOrConflict(item);
		return response(item);
	}

	public ItemResponse update(Long eventId, Long id, UpdateItemRequest request) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireEditableMenu();
		EventItem item = findEntityWithLock(eventId, id);
		String name = request.name().strip();
		requireFreeName(item.getCategory().getId(), name, id);
		long contributed = contributions.sumByItem(id);
		if (request.requiredQuantity() < contributed) {
			throw new BusinessRuleException("A quantidade necessária não pode ficar abaixo do que já foi contribuído ("
					+ contributed + ").");
		}

		item.setName(name);
		item.setRequiredQuantity(request.requiredQuantity());
		item.setMandatory(Boolean.TRUE.equals(request.mandatory()));
		replaceContents(item.getAllergens(), request.allergens());
		replaceContents(item.getCharacteristics(), request.characteristics());
		saveOrConflict(item);
		return response(item);
	}

	public void delete(Long eventId, Long id) {
		Event event = eventService.findSyncedEntity(eventId);
		event.requireEditableMenu();
		EventItem item = findEntityWithLock(eventId, id);
		if (contributions.existsByItemId(id)) {
			throw new BusinessRuleException("Não é possível excluir um item que já tem contribuições.");
		}
		items.delete(item);
	}

	private EventItem findEntity(Long eventId, Long id) {
		return items.find(id, eventId).orElseThrow(() -> new ResourceNotFoundException("Item não encontrado."));
	}

	private EventItem findEntityWithLock(Long eventId, Long id) {
		return items.findWithLock(id, eventId).orElseThrow(() -> new ResourceNotFoundException("Item não encontrado."));
	}

	private ItemResponse response(EventItem item) {
		return ItemResponse.from(item, contributions.sumByItem(item.getId()));
	}

	private static <T> void replaceContents(Set<T> current, Set<T> newStatus) {
		current.clear();
		current.addAll(newStatus == null ? new HashSet<>() : newStatus);
	}

	private void requireFreeName(Long categoryId, String name, Long ignoredId) {
		if (items.nameExists(categoryId, name, ignoredId)) {
			throw duplicateName();
		}
	}

	/** The unique index of the database decides when two people create the same name at the same time. */
	private void saveOrConflict(EventItem item) {
		try {
			items.saveAndFlush(item);
		}
		catch (DataIntegrityViolationException ex) {
			throw duplicateName();
		}
	}

	private ConflictException duplicateName() {
		return new ConflictException("Já existe um item com esse nome nesta categoria.");
	}

}
