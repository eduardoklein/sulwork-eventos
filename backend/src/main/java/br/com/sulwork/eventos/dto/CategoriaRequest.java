package br.com.sulwork.eventos.dto;

import br.com.sulwork.eventos.domain.UnidadeMedida;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
		@NotBlank @Size(max = 80) String nome,
		@NotNull UnidadeMedida unidade,
		@NotNull @Min(0) Integer metaQuantidade) {
}
