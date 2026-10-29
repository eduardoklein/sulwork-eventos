package br.com.sulwork.eventos.dto;

import java.time.LocalDateTime;

import br.com.sulwork.eventos.domain.Contribuicao;
import br.com.sulwork.eventos.domain.UnidadeMedida;

public record ContribuicaoResponse(
		Long id,
		Long participanteId,
		String participanteNome,
		Long itemId,
		String itemNome,
		UnidadeMedida unidade,
		int quantidade,
		String observacao,
		LocalDateTime criadoEm,
		LocalDateTime atualizadoEm) {

	public static ContribuicaoResponse de(Contribuicao contribuicao) {
		var participante = contribuicao.getParticipante();
		var item = contribuicao.getItem();
		return new ContribuicaoResponse(
				contribuicao.getId(),
				participante.getId(),
				participante.getColaborador().getNome(),
				item.getId(),
				item.getNome(),
				item.getUnidade(),
				contribuicao.getQuantidade(),
				contribuicao.getObservacao(),
				contribuicao.getCriadoEm(),
				contribuicao.getAtualizadoEm());
	}

}
