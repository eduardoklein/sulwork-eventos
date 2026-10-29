package br.com.sulwork.eventos.dto;

import java.util.Set;

import br.com.sulwork.eventos.domain.RestricaoAlimentar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** As restrições são obrigatórias: uma lista vazia limpa, e a omissão não apaga as restrições por engano. */
public record AtualizarColaboradorRequest(
		@NotBlank @Size(max = 120) String nome,
		@NotNull Set<RestricaoAlimentar> restricoes) {
}
