package br.com.sulwork.events.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.dto.UpdateEventRequest;
import br.com.sulwork.events.dto.CreateEventRequest;
import br.com.sulwork.events.dto.EventResponse;
import br.com.sulwork.events.dto.ChangeStatusRequest;
import br.com.sulwork.events.service.EventService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

	private final EventService service;

	public EventController(EventService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
		EventResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/events/" + created.id())).body(created);
	}

	@GetMapping
	List<EventResponse> list(
			@RequestParam(required = false) EventStatus status,
			@RequestParam(required = false) EventType type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until) {
		return service.list(status, type, from, until);
	}

	@GetMapping("/{id}")
	EventResponse find(@PathVariable Long id) {
		return service.find(id);
	}

	@PutMapping("/{id}")
	EventResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request) {
		return service.update(id, request);
	}

	@PatchMapping("/{id}/status")
	EventResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
		return service.changeStatus(id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
