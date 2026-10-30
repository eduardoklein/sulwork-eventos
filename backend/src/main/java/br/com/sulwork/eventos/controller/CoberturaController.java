package br.com.sulwork.eventos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.eventos.dto.CoberturaResponse;
import br.com.sulwork.eventos.service.CoberturaService;

@RestController
@RequestMapping("/api/eventos/{eventoId}/cobertura")
public class CoberturaController {

	private final CoberturaService service;

	public CoberturaController(CoberturaService service) {
		this.service = service;
	}

	@GetMapping
	CoberturaResponse calcular(@PathVariable Long eventoId) {
		return service.calcular(eventoId);
	}

}
