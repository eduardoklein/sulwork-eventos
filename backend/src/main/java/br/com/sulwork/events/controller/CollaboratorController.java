package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.UpdateCollaboratorRequest;
import br.com.sulwork.events.dto.CollaboratorResponse;
import br.com.sulwork.events.service.CollaboratorService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Colaboradores", description = "O cadastro das pessoas, que vale para todos os eventos.")
@RestController
@RequestMapping("/api/collaborators")
public class CollaboratorController {

	private final CollaboratorService service;

	public CollaboratorController(CollaboratorService service) {
		this.service = service;
	}

	/** Finds one person by exact CPF. There is no general listing of the records. */
	@Operation(summary = "Busca uma pessoa pelo CPF",
			description = "Só pelo CPF exato. Não existe listagem geral do cadastro.")
	@GetMapping(params = "cpf")
	CollaboratorResponse findByCpf(@Parameter(description = "CPF com 11 dígitos ou com máscara") @RequestParam String cpf) {
		return service.findByCpf(cpf);
	}

	@Operation(summary = "Busca uma pessoa pelo id")
	@GetMapping("/{id}")
	CollaboratorResponse find(@PathVariable Long id) {
		return service.find(id);
	}

	@Operation(summary = "Edita o cadastro de uma pessoa",
			description = "É o único lugar que altera nome e restrições. O CPF não muda.")
	@PutMapping("/{id}")
	CollaboratorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCollaboratorRequest request) {
		return service.update(id, request);
	}

}
