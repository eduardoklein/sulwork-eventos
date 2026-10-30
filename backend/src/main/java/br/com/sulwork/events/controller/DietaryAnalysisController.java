package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.DietaryAnalysisResponse;
import br.com.sulwork.events.service.DietaryAnalysisService;

@RestController
@RequestMapping("/api/events/{eventId}/dietary-analysis")
public class DietaryAnalysisController {

	private final DietaryAnalysisService service;

	public DietaryAnalysisController(DietaryAnalysisService service) {
		this.service = service;
	}

	@GetMapping
	DietaryAnalysisResponse analyze(@PathVariable Long eventId) {
		return service.analyze(eventId);
	}

}
