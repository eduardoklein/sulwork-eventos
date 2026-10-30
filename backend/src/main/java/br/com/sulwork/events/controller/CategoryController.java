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
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.CategoryRequest;
import br.com.sulwork.events.dto.CategoryResponse;
import br.com.sulwork.events.service.CategoryService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Categorias", description = "Categorias do cardápio, cada uma com a sua unidade e a sua meta.")
@RestController
@RequestMapping("/api/events/{eventId}/categories")
public class CategoryController {

	private final CategoryService service;

	public CategoryController(CategoryService service) {
		this.service = service;
	}

	@Operation(summary = "Cria uma categoria")
	@PostMapping
	ResponseEntity<CategoryResponse> create(@PathVariable Long eventId, @Valid @RequestBody CategoryRequest request) {
		CategoryResponse created = service.create(eventId, request);
		return ResponseEntity.created(URI.create("/api/events/" + eventId + "/categories/" + created.id())).body(created);
	}

	@Operation(summary = "Lista as categorias do evento",
			description = "Cada categoria traz o total de itens e as quantidades planejada e contribuída.")
	@GetMapping
	List<CategoryResponse> list(@PathVariable Long eventId) {
		return service.list(eventId);
	}

	@Operation(summary = "Busca uma categoria pelo id")
	@GetMapping("/{id}")
	CategoryResponse find(@PathVariable Long eventId, @PathVariable Long id) {
		return service.find(eventId, id);
	}

	@Operation(summary = "Edita uma categoria",
			description = "A unidade fica travada enquanto a categoria tem itens.")
	@PutMapping("/{id}")
	CategoryResponse update(@PathVariable Long eventId, @PathVariable Long id,
			@Valid @RequestBody CategoryRequest request) {
		return service.update(eventId, id, request);
	}

	@Operation(summary = "Exclui uma categoria",
			description = "Só categorias sem itens podem ser excluídas.")
	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long id) {
		service.delete(eventId, id);
		return ResponseEntity.noContent().build();
	}

}
