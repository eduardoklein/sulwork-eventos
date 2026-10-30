package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.CoverageResponse;
import br.com.sulwork.events.service.CoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Cobertura", description = "Quanto das necessidades do evento já foi atendido.")
@RestController
@RequestMapping("/api/events/{eventId}/coverage")
public class CoverageController {

	private final CoverageService service;

	public CoverageController(CoverageService service) {
		this.service = service;
	}

	@Operation(summary = "Calcula a cobertura do evento",
			description = "A conta roda dentro de cada categoria. O geral é a média das categorias.")
	@GetMapping
	CoverageResponse calculate(@PathVariable Long eventId) {
		return service.calculate(eventId);
	}

}
