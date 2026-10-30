package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.SummaryResponse;
import br.com.sulwork.events.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Resumo", description = "O texto do evento para compartilhar em um grupo.")
@RestController
@RequestMapping("/api/events/{eventId}/summary")
public class SummaryController {

	private final SummaryService service;

	public SummaryController(SummaryService service) {
		this.service = service;
	}

	@Operation(summary = "Gera o resumo do evento para compartilhar",
			description = "Texto pronto para WhatsApp, Slack ou Teams. Não leva CPF nem restrições alimentares.")
	@GetMapping
	SummaryResponse summarize(@PathVariable Long eventId) {
		return service.summarize(eventId);
	}

}
