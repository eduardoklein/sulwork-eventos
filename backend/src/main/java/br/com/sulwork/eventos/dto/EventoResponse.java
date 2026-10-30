package br.com.sulwork.eventos.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;

public record EventoResponse(
		Long id,
		String nome,
		String descricao,
		TipoEvento tipo,
		LocalDateTime dataHora,
		LocalDateTime prazoAlteracoes,
		String fuso,
		StatusEvento status,
		boolean prazoExpirado,
		boolean aceitaContribuicoes,
		List<StatusEvento> transicoesPermitidas,
		LocalDateTime criadoEm,
		LocalDateTime atualizadoEm) {

	public static EventoResponse de(Evento evento, Instant agora) {
		return new EventoResponse(
				evento.getId(),
				evento.getNome(),
				evento.getDescricao(),
				evento.getTipo(),
				evento.getDataHora(),
				evento.getPrazoAlteracoes(),
				evento.getFuso().getId(),
				evento.getStatus(),
				evento.isPrazoExpirado(agora),
				evento.isAbertoParaContribuicoes(agora),
				evento.getStatus().transicoesPermitidas().stream().sorted(Comparator.naturalOrder()).toList(),
				evento.getCriadoEm(),
				evento.getAtualizadoEm());
	}

}
