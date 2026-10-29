package br.com.sulwork.eventos.dto;

import java.util.Set;

import br.com.sulwork.eventos.domain.Alergeno;
import br.com.sulwork.eventos.domain.CaracteristicaAlimentar;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** O item não muda de categoria: por isso não há {@code categoriaId} aqui. */
public record AtualizarItemRequest(
		@NotBlank @Size(max = 120) String nome,
		@NotNull @Min(1) Integer quantidadeNecessaria,
		Boolean obrigatorio,
		Set<Alergeno> alergenos,
		Set<CaracteristicaAlimentar> caracteristicas) {
}
