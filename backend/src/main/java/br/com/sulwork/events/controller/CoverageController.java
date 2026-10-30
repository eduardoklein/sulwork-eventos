package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.CoverageResponse;
import br.com.sulwork.events.service.CoverageService;

@RestController
@RequestMapping("/api/events/{eventId}/coverage")
public class CoverageController {

	private final CoverageService service;

	public CoverageController(CoverageService service) {
		this.service = service;
	}

	@GetMapping
	CoverageResponse calculate(@PathVariable Long eventId) {
		return service.calculate(eventId);
	}

}
