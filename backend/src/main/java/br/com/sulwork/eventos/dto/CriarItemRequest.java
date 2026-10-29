package br.com.sulwork.eventos.dto;

import java.util.Set;

import br.com.sulwork.eventos.domain.Alergeno;
import br.com.sulwork.eventos.domain.CaracteristicaAlimentar;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Obrigatório, alérgenos e características são opcionais: se faltarem, o item é opcional e fica sem nenhum. */
public record CriarItemRequest(
		@NotBlank @Size(max = 120) String nome,
		@NotNull Long categoriaId,
		@NotNull @Min(1) Integer quantidadeNecessaria,
		Boolean obrigatorio,
		Set<Alergeno> alergenos,
		Set<CaracteristicaAlimentar> caracteristicas) {
}
