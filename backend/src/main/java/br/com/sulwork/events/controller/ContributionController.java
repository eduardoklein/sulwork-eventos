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

import br.com.sulwork.events.dto.UpdateContributionRequest;
import br.com.sulwork.events.dto.ContributionResponse;
import br.com.sulwork.events.dto.CreateContributionRequest;
import br.com.sulwork.events.service.ContributionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events/{eventId}/contributions")
public class ContributionController {

	private final ContributionService service;

	public ContributionController(ContributionService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ContributionResponse> create(@PathVariable Long eventId,
			@Valid @RequestBody CreateContributionRequest request) {
		ContributionResponse created = service.create(eventId, request);
		return ResponseEntity.created(URI.create("/api/events/" + eventId + "/contributions/" + created.id())).body(created);
	}

	@GetMapping
	List<ContributionResponse> list(@PathVariable Long eventId, @RequestParam(required = false) Long itemId,
			@RequestParam(required = false) Long participantId) {
		return service.list(eventId, itemId, participantId);
	}

	@GetMapping("/{id}")
	ContributionResponse find(@PathVariable Long eventId, @PathVariable Long id) {
		return service.find(eventId, id);
	}

	@PutMapping("/{id}")
	ContributionResponse update(@PathVariable Long eventId, @PathVariable Long id,
			@Valid @RequestBody UpdateContributionRequest request) {
		return service.update(eventId, id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> remove(@PathVariable Long eventId, @PathVariable Long id) {
		service.remove(eventId, id);
		return ResponseEntity.noContent().build();
	}

}
