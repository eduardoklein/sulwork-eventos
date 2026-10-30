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

import br.com.sulwork.events.dto.UpdateItemRequest;
import br.com.sulwork.events.dto.CreateItemRequest;
import br.com.sulwork.events.dto.ItemResponse;
import br.com.sulwork.events.service.ItemService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Itens do cardápio", description = "Itens de cada categoria, com alérgenos e características.")
@RestController
@RequestMapping("/api/events/{eventId}/items")
public class ItemController {

	private final ItemService service;

	public ItemController(ItemService service) {
		this.service = service;
	}

	@Operation(summary = "Cria um item do cardápio")
	@PostMapping
	ResponseEntity<ItemResponse> create(@PathVariable Long eventId, @Valid @RequestBody CreateItemRequest request) {
		ItemResponse created = service.create(eventId, request);
		return ResponseEntity.created(URI.create("/api/events/" + eventId + "/items/" + created.id())).body(created);
	}

	@Operation(summary = "Lista os itens do cardápio",
			description = "Cada item traz a quantidade contribuída, a pendente e o percentual coberto.")
	@GetMapping
	List<ItemResponse> list(@PathVariable Long eventId, @Parameter(description = "Traz só os itens desta categoria") @RequestParam(required = false) Long categoryId) {
		return service.list(eventId, categoryId);
	}

	@Operation(summary = "Busca um item pelo id")
	@GetMapping("/{id}")
	ItemResponse find(@PathVariable Long eventId, @PathVariable Long id) {
		return service.find(eventId, id);
	}

	@Operation(summary = "Edita um item",
			description = "O item não muda de categoria, e a quantidade não fica abaixo do que já foi contribuído.")
	@PutMapping("/{id}")
	ItemResponse update(@PathVariable Long eventId, @PathVariable Long id,
			@Valid @RequestBody UpdateItemRequest request) {
		return service.update(eventId, id, request);
	}

	@Operation(summary = "Exclui um item",
			description = "Só itens sem contribuições podem ser excluídos.")
	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long id) {
		service.delete(eventId, id);
		return ResponseEntity.noContent().build();
	}

}
