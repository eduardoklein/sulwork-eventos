package br.com.sulwork.eventos.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import br.com.sulwork.eventos.domain.ParticipanteEvento;
import br.com.sulwork.eventos.domain.RestricaoAlimentar;
import br.com.sulwork.eventos.validation.CpfMascara;

/** {@code id} é o da inscrição neste evento. {@code colaboradorId} é o da pessoa. */
public record ParticipanteResponse(
		Long id,
		Long colaboradorId,
		String nome,
		String cpf,
		List<RestricaoAlimentar> restricoes,
		LocalDateTime inscritoEm) {

	public static ParticipanteResponse de(ParticipanteEvento participante) {
		var colaborador = participante.getColaborador();
		return new ParticipanteResponse(
				participante.getId(),
				colaborador.getId(),
				colaborador.getNome(),
				CpfMascara.mascarar(colaborador.getCpf()),
				colaborador.getRestricoes().stream().sorted(Comparator.naturalOrder()).toList(),
				participante.getCriadoEm());
	}

}
