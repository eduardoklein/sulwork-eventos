package br.com.sulwork.events.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.UpdateItemRequest;
import br.com.sulwork.events.dto.CreateItemRequest;
import br.com.sulwork.events.dto.ItemResponse;
import br.com.sulwork.events.service.ItemService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events/{eventId}/items")
public class ItemController {

	private final ItemService service;

	public ItemController(ItemService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ItemResponse> create(@PathVariable Long eventId, @Valid @RequestBody CreateItemRequest request) {
		ItemResponse created = service.create(eventId, request);
		return ResponseEntity.created(URI.create("/api/events/" + eventId + "/items/" + created.id())).body(created);
	}

	@GetMapping
	List<ItemResponse> list(@PathVariable Long eventId, @RequestParam(required = false) Long categoryId) {
		return service.list(eventId, categoryId);
	}

	@GetMapping("/{id}")
	ItemResponse find(@PathVariable Long eventId, @PathVariable Long id) {
		return service.find(eventId, id);
	}

	@PutMapping("/{id}")
	ItemResponse update(@PathVariable Long eventId, @PathVariable Long id,
			@Valid @RequestBody UpdateItemRequest request) {
		return service.update(eventId, id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long id) {
		service.delete(eventId, id);
		return ResponseEntity.noContent().build();
	}

}
