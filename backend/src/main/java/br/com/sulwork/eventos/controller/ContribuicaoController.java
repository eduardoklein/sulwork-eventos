package br.com.sulwork.eventos.controller;

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

import br.com.sulwork.eventos.dto.AtualizarContribuicaoRequest;
import br.com.sulwork.eventos.dto.ContribuicaoResponse;
import br.com.sulwork.eventos.dto.CriarContribuicaoRequest;
import br.com.sulwork.eventos.service.ContribuicaoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos/{eventoId}/contribuicoes")
public class ContribuicaoController {

	private final ContribuicaoService service;

	public ContribuicaoController(ContribuicaoService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ContribuicaoResponse> criar(@PathVariable Long eventoId,
			@Valid @RequestBody CriarContribuicaoRequest request) {
		ContribuicaoResponse criada = service.criar(eventoId, request);
		return ResponseEntity.created(URI.create("/api/eventos/" + eventoId + "/contribuicoes/" + criada.id())).body(criada);
	}

	@GetMapping
	List<ContribuicaoResponse> listar(@PathVariable Long eventoId, @RequestParam(required = false) Long itemId,
			@RequestParam(required = false) Long participanteId) {
		return service.listar(eventoId, itemId, participanteId);
	}

	@GetMapping("/{id}")
	ContribuicaoResponse buscar(@PathVariable Long eventoId, @PathVariable Long id) {
		return service.buscar(eventoId, id);
	}

	@PutMapping("/{id}")
	ContribuicaoResponse atualizar(@PathVariable Long eventoId, @PathVariable Long id,
			@Valid @RequestBody AtualizarContribuicaoRequest request) {
		return service.atualizar(eventoId, id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> remover(@PathVariable Long eventoId, @PathVariable Long id) {
		service.remover(eventoId, id);
		return ResponseEntity.noContent().build();
	}

}
