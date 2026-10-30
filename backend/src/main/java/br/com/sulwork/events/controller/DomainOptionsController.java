package br.com.sulwork.events.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.events.dto.DomainOptionsResponse;
import br.com.sulwork.events.service.DomainOptionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Domínio", description = "As listas fechadas do sistema, com o código e o texto de cada opção.")
@RestController
@RequestMapping("/api/domain/options")
public class DomainOptionsController {

	private final DomainOptionsService service;

	public DomainOptionsController(DomainOptionsService service) {
		this.service = service;
	}

	@Operation(summary = "Lista as opções do sistema",
			description = "Tipos de evento, status, unidades, alérgenos, características, restrições alimentares e níveis de prontidão, com o código e a descrição de cada um.")
	@GetMapping
	DomainOptionsResponse options() {
		return service.options();
	}

}
