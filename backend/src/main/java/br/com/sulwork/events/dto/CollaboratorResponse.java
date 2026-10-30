package br.com.sulwork.events.dto;

import java.util.Comparator;
import java.util.List;

import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.validation.CpfMask;

/** The CPF comes out masked: the full CPF only enters on enrollment and search. */
public record CollaboratorResponse(Long id, String name, String cpf, List<DietaryRestriction> restrictions) {

	public static CollaboratorResponse from(Collaborator collaborator) {
		return new CollaboratorResponse(
				collaborator.getId(),
				collaborator.getName(),
				CpfMask.mask(collaborator.getCpf()),
				collaborator.getRestrictions().stream().sorted(Comparator.naturalOrder()).toList());
	}

}
