package br.com.sulwork.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A pessoa e o item não mudam. Para zerar a contribuição, remova-a. */
public record AtualizarContribuicaoRequest(
		@NotNull @Min(1) Integer quantidade,
		@Size(max = 255) String observacao) {
}
