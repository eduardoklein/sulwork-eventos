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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Contribuições", description = "O que cada participante leva, com o limite da quantidade necessária.")
@RestController
@RequestMapping("/api/events/{eventId}/contributions")
public class ContributionController {

	private final ContributionService service;

	public ContributionController(ContributionService service) {
		this.service = service;
	}

	@Operation(summary = "Registra uma contribuição",
			description = "O segundo pedido da mesma pessoa para o mesmo item devolve 409: use a edição.")
	@PostMapping
	ResponseEntity<ContributionResponse> create(@PathVariable Long eventId,
			@Valid @RequestBody CreateContributionRequest request) {
		ContributionResponse created = service.create(eventId, request);
		return ResponseEntity.created(URI.create("/api/events/" + eventId + "/contributions/" + created.id())).body(created);
	}

	@Operation(summary = "Lista as contribuições do evento",
			description = "Filtros opcionais por item e por participante.")
	@GetMapping
	List<ContributionResponse> list(@PathVariable Long eventId, @Parameter(description = "Traz só as contribuições deste item") @RequestParam(required = false) Long itemId,
			@Parameter(description = "Traz só as contribuições desta inscrição") @RequestParam(required = false) Long participantId) {
		return service.list(eventId, itemId, participantId);
	}

	@Operation(summary = "Busca uma contribuição pelo id")
	@GetMapping("/{id}")
	ContributionResponse find(@PathVariable Long eventId, @PathVariable Long id) {
		return service.find(eventId, id);
	}

	@Operation(summary = "Altera uma contribuição",
			description = "Muda a quantidade e a observação. A soma do item nunca passa da quantidade necessária.")
	@PutMapping("/{id}")
	ContributionResponse update(@PathVariable Long eventId, @PathVariable Long id,
			@Valid @RequestBody UpdateContributionRequest request) {
		return service.update(eventId, id, request);
	}

	@Operation(summary = "Remove uma contribuição")
	@DeleteMapping("/{id}")
	ResponseEntity<Void> remove(@PathVariable Long eventId, @PathVariable Long id) {
		service.remove(eventId, id);
		return ResponseEntity.noContent().build();
	}

}
