package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.SummaryResponse;
import br.com.sulwork.events.service.SummaryService;

@RestController
@RequestMapping("/api/events/{eventId}/summary")
public class SummaryController {

	private final SummaryService service;

	public SummaryController(SummaryService service) {
		this.service = service;
	}

	@GetMapping
	SummaryResponse summarize(@PathVariable Long eventId) {
		return service.summarize(eventId);
	}

}
