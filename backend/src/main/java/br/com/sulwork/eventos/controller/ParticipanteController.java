package br.com.sulwork.eventos.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.eventos.dto.InscreverParticipanteRequest;
import br.com.sulwork.eventos.dto.ParticipanteResponse;
import br.com.sulwork.eventos.service.ParticipanteService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos/{eventoId}/participantes")
public class ParticipanteController {

	private final ParticipanteService service;

	public ParticipanteController(ParticipanteService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ParticipanteResponse> inscrever(@PathVariable Long eventoId,
			@Valid @RequestBody InscreverParticipanteRequest request) {
		ParticipanteResponse inscrito = service.inscrever(eventoId, request);
		return ResponseEntity.created(URI.create("/api/eventos/" + eventoId + "/participantes/" + inscrito.id()))
				.body(inscrito);
	}

	@GetMapping
	List<ParticipanteResponse> listar(@PathVariable Long eventoId) {
		return service.listar(eventoId);
	}

	@GetMapping("/{id}")
	ParticipanteResponse buscar(@PathVariable Long eventoId, @PathVariable Long id) {
		return service.buscar(eventoId, id);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> remover(@PathVariable Long eventoId, @PathVariable Long id) {
		service.remover(eventoId, id);
		return ResponseEntity.noContent().build();
	}

}
