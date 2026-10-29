package br.com.sulwork.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code participanteId} é o da inscrição no evento. */
public record CriarContribuicaoRequest(
		@NotNull Long participanteId,
		@NotNull Long itemId,
		@NotNull @Min(1) Integer quantidade,
		@Size(max = 255) String observacao) {
}
