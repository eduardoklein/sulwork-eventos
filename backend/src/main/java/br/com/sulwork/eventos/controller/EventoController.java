package br.com.sulwork.eventos.controller;

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

import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.dto.AtualizarEventoRequest;
import br.com.sulwork.eventos.dto.CriarEventoRequest;
import br.com.sulwork.eventos.dto.EventoResponse;
import br.com.sulwork.eventos.dto.MudarStatusRequest;
import br.com.sulwork.eventos.service.EventoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

	private final EventoService service;

	public EventoController(EventoService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<EventoResponse> criar(@Valid @RequestBody CriarEventoRequest request) {
		EventoResponse criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/eventos/" + criado.id())).body(criado);
	}

	@GetMapping
	List<EventoResponse> listar(
			@RequestParam(required = false) StatusEvento status,
			@RequestParam(required = false) TipoEvento tipo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
		return service.listar(status, tipo, de, ate);
	}

	@GetMapping("/{id}")
	EventoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PutMapping("/{id}")
	EventoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarEventoRequest request) {
		return service.atualizar(id, request);
	}

	@PatchMapping("/{id}/status")
	EventoResponse mudarStatus(@PathVariable Long id, @Valid @RequestBody MudarStatusRequest request) {
		return service.mudarStatus(id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> excluir(@PathVariable Long id) {
		service.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
