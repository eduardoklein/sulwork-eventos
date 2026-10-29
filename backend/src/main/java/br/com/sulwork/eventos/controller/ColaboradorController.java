package br.com.sulwork.eventos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sulwork.eventos.dto.AtualizarColaboradorRequest;
import br.com.sulwork.eventos.dto.ColaboradorResponse;
import br.com.sulwork.eventos.service.ColaboradorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/colaboradores")
public class ColaboradorController {

	private final ColaboradorService service;

	public ColaboradorController(ColaboradorService service) {
		this.service = service;
	}

	/** Busca uma pessoa pelo CPF exato. Não existe listagem geral do cadastro. */
	@GetMapping(params = "cpf")
	ColaboradorResponse buscarPorCpf(@RequestParam String cpf) {
		return service.buscarPorCpf(cpf);
	}

	@GetMapping("/{id}")
	ColaboradorResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PutMapping("/{id}")
	ColaboradorResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarColaboradorRequest request) {
		return service.atualizar(id, request);
	}

}
