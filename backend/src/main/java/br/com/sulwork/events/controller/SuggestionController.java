package br.com.sulwork.events.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.SuggestionResponse;
import br.com.sulwork.events.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Sugestões", description = "O que cada participante pode levar, por prioridade.")
@RestController
@RequestMapping("/api/events/{eventId}/suggestions")
public class SuggestionController {

	private final SuggestionService service;

	public SuggestionController(SuggestionService service) {
		this.service = service;
	}

	@Operation(summary = "Sugere o que um participante pode levar",
			description = "Itens ordenados por prioridade de 0 a 100, com os motivos. A lista fica vazia se o evento não aceita contribuições.")
	@GetMapping
	List<SuggestionResponse> suggest(@PathVariable Long eventId, @Parameter(description = "O id da inscrição da pessoa neste evento") @RequestParam Long participantId) {
		return service.suggest(eventId, participantId);
	}

}
