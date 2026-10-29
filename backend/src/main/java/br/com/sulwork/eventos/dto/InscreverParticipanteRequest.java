package br.com.sulwork.eventos.dto;

import java.util.Set;

import br.com.sulwork.eventos.domain.RestricaoAlimentar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code nome} e {@code restricoes} só valem para um CPF novo. Para uma pessoa já cadastrada, a inscrição não
 * altera nada: o cadastro muda só em {@code PUT /api/colaboradores/{id}}.
 */
public record InscreverParticipanteRequest(
		@NotBlank String cpf,
		@Size(max = 120) String nome,
		Set<RestricaoAlimentar> restricoes) {
}
