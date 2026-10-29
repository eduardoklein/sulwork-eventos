package br.com.sulwork.eventos.dto;

import java.time.LocalDateTime;

import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.validation.FusoValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code fuso} é opcional: se faltar, o evento mantém o fuso atual. */
public record AtualizarEventoRequest(
		@NotBlank @Size(max = 120) String nome,
		@Size(max = 1000) String descricao,
		@NotNull TipoEvento tipo,
		@NotNull LocalDateTime dataHora,
		@NotNull LocalDateTime prazoAlteracoes,
		@FusoValido String fuso) {
}
