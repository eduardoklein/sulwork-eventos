package br.com.sulwork.events.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.dto.UpdateEventRequest;
import br.com.sulwork.events.dto.CreateEventRequest;
import br.com.sulwork.events.dto.EventResponse;
import br.com.sulwork.events.dto.ChangeStatusRequest;
import br.com.sulwork.events.service.EventService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Eventos", description = "Cria, consulta, edita, muda o status e exclui eventos de alimentação.")
@RestController
@RequestMapping("/api/events")
public class EventController {

	private final EventService service;

	public EventController(EventService service) {
		this.service = service;
	}

	@Operation(summary = "Cria um evento",
			description = "O evento nasce em rascunho ou aberto. O fuso é opcional: se faltar, vale o padrão do servidor.")
	@PostMapping
	ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
		EventResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/events/" + created.id())).body(created);
	}

	@Operation(summary = "Lista os eventos",
			description = "Filtros opcionais de status, tipo e datas, na ordem da data do evento.")
	@GetMapping
	List<EventResponse> list(
			@Parameter(description = "Filtra pelo status do evento") @RequestParam(required = false) EventStatus status,
			@Parameter(description = "Filtra pelo tipo do evento") @RequestParam(required = false) EventType type,
			@Parameter(description = "Eventos a partir desta data (inclusive), no formato AAAA-MM-DD") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Eventos até esta data (inclusive), no formato AAAA-MM-DD") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until) {
		return service.list(status, type, from, until);
	}

	@Operation(summary = "Busca um evento pelo id")
	@GetMapping("/{id}")
	EventResponse find(@PathVariable Long id) {
		return service.find(id);
	}

	@Operation(summary = "Edita um evento",
			description = "O que pode mudar depende do status do evento.")
	@PutMapping("/{id}")
	EventResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request) {
		return service.update(id, request);
	}

	@Operation(summary = "Muda o status do evento",
			description = "Respeita a máquina de estados. O prazo novo só vale ao abrir o evento.")
	@PatchMapping("/{id}/status")
	EventResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
		return service.changeStatus(id, request);
	}

	@Operation(summary = "Exclui um evento",
			description = "Só eventos em rascunho podem ser excluídos. Os outros se cancelam.")
	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
