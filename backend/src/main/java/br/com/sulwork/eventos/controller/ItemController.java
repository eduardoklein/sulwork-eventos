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

import br.com.sulwork.eventos.dto.AtualizarItemRequest;
import br.com.sulwork.eventos.dto.CriarItemRequest;
import br.com.sulwork.eventos.dto.ItemResponse;
import br.com.sulwork.eventos.service.ItemService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos/{eventoId}/itens")
public class ItemController {

	private final ItemService service;

	public ItemController(ItemService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ItemResponse> criar(@PathVariable Long eventoId, @Valid @RequestBody CriarItemRequest request) {
		ItemResponse criado = service.criar(eventoId, request);
		return ResponseEntity.created(URI.create("/api/eventos/" + eventoId + "/itens/" + criado.id())).body(criado);
	}

	@GetMapping
	List<ItemResponse> listar(@PathVariable Long eventoId, @RequestParam(required = false) Long categoriaId) {
		return service.listar(eventoId, categoriaId);
	}

	@GetMapping("/{id}")
	ItemResponse buscar(@PathVariable Long eventoId, @PathVariable Long id) {
		return service.buscar(eventoId, id);
	}

	@PutMapping("/{id}")
	ItemResponse atualizar(@PathVariable Long eventoId, @PathVariable Long id,
			@Valid @RequestBody AtualizarItemRequest request) {
		return service.atualizar(eventoId, id, request);
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> excluir(@PathVariable Long eventoId, @PathVariable Long id) {
		service.excluir(eventoId, id);
		return ResponseEntity.noContent().build();
	}

}
