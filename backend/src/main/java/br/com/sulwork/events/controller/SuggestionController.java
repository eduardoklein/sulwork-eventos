package br.com.sulwork.events.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.SuggestionResponse;
import br.com.sulwork.events.service.SuggestionService;

@RestController
@RequestMapping("/api/events/{eventId}/suggestions")
public class SuggestionController {

	private final SuggestionService service;

	public SuggestionController(SuggestionService service) {
		this.service = service;
	}

	@GetMapping
	List<SuggestionResponse> suggest(@PathVariable Long eventId, @RequestParam Long participantId) {
		return service.suggest(eventId, participantId);
	}

}
