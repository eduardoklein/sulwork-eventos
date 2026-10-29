package br.com.sulwork.eventos.dto;

import java.time.LocalDateTime;

import br.com.sulwork.eventos.domain.StatusEvento;
import jakarta.validation.constraints.NotNull;

/** {@code novoPrazoAlteracoes} só vale ao abrir o evento. */
public record MudarStatusRequest(@NotNull StatusEvento novoStatus, LocalDateTime novoPrazoAlteracoes) {
}
