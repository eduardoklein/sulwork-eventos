package br.com.sulwork.eventos.dto;

import java.util.Comparator;
import java.util.List;

import br.com.sulwork.eventos.domain.Colaborador;
import br.com.sulwork.eventos.domain.RestricaoAlimentar;
import br.com.sulwork.eventos.validation.CpfMascara;

/** O CPF sai mascarado: o CPF completo só entra na inscrição e na busca. */
public record ColaboradorResponse(Long id, String nome, String cpf, List<RestricaoAlimentar> restricoes) {

	public static ColaboradorResponse de(Colaborador colaborador) {
		return new ColaboradorResponse(
				colaborador.getId(),
				colaborador.getNome(),
				CpfMascara.mascarar(colaborador.getCpf()),
				colaborador.getRestricoes().stream().sorted(Comparator.naturalOrder()).toList());
	}

}
