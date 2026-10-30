package br.com.sulwork.events.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.EnrollParticipantRequest;
import br.com.sulwork.events.dto.ParticipantResponse;
import br.com.sulwork.events.service.ParticipantService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events/{eventId}/participants")
public class ParticipantController {

	private final ParticipantService service;

	public ParticipantController(ParticipantService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ParticipantResponse> enroll(@PathVariable Long eventId,
			@Valid @RequestBody EnrollParticipantRequest request) {
		ParticipantResponse enrolled = service.enroll(eventId, request);
		return ResponseEntity.created(URI.create("/api/events/" + eventId + "/participants/" + enrolled.id()))
				.body(enrolled);
	}

	@GetMapping
	List<ParticipantResponse> list(@PathVariable Long eventId) {
		return service.list(eventId);
	}

	@GetMapping("/{id}")
	ParticipantResponse find(@PathVariable Long eventId, @PathVariable Long id) {
		return service.find(eventId, id);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> remove(@PathVariable Long eventId, @PathVariable Long id) {
		service.remove(eventId, id);
		return ResponseEntity.noContent().build();
	}

}
