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
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.eventos.dto.CategoriaRequest;
import br.com.sulwork.eventos.dto.CategoriaResponse;
import br.com.sulwork.eventos.service.CategoriaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos/{eventoId}/categorias")
public class CategoriaController {

	private final CategoriaService service;

	public CategoriaController(CategoriaService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<CategoriaResponse> criar(@PathVariable Long eventoId, @Valid @RequestBody CategoriaRequest request) {
		CategoriaResponse criada = service.criar(eventoId, request);
		return ResponseEntity.created(URI.create("/api/eventos/" + eventoId + "/categorias/" + criada.id())).body(criada);
	}

	@GetMapping
	List<CategoriaResponse> listar(@PathVariable Long eventoId) {
		return service.listar(eventoId);
	}

	@GetMapping("/{id}")
	CategoriaResponse buscar(@PathVariable Long eventoId, @PathVariable Long id) {
		return service.buscar(eventoId, id);
	}

	@PutMapping("/{id}")
	CategoriaResponse atualizar(@PathVariable Long eventoId, @PathVariable Long id,
			@Valid @RequestBody CategoriaRequest request) {
		return service.atualizar(eventoId, id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> excluir(@PathVariable Long eventoId, @PathVariable Long id) {
		service.excluir(eventoId, id);
		return ResponseEntity.noContent().build();
	}

}
