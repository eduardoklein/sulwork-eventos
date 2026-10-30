package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.DietaryAnalysisResponse;
import br.com.sulwork.events.service.DietaryAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Análise alimentar", description = "Quem pode ficar sem comer, a partir das restrições e dos itens.")
@RestController
@RequestMapping("/api/events/{eventId}/dietary-analysis")
public class DietaryAnalysisController {

	private final DietaryAnalysisService service;

	public DietaryAnalysisController(DietaryAnalysisService service) {
		this.service = service;
	}

	@Operation(summary = "Analisa a segurança do cardápio por participante",
			description = "Mostra os itens inseguros de cada pessoa, o resumo por restrição e os alertas.")
	@GetMapping
	DietaryAnalysisResponse analyze(@PathVariable Long eventId) {
		return service.analyze(eventId);
	}

}
