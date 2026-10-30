package br.com.sulwork.events.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.dto.UpdateCollaboratorRequest;
import br.com.sulwork.events.dto.CollaboratorResponse;
import br.com.sulwork.events.exception.ResourceNotFoundException;
import br.com.sulwork.events.repository.CollaboratorRepository;
import br.com.sulwork.events.validation.CpfValidator;

@Service
@Transactional
public class CollaboratorService {

	private final CollaboratorRepository collaborators;

	public CollaboratorService(CollaboratorRepository collaborators) {
		this.collaborators = collaborators;
	}

	@Transactional(readOnly = true)
	public CollaboratorResponse findByCpf(String cpf) {
		String normalized = CpfValidator.normalizeAndValidate(cpf);
		return collaborators.findByCpf(normalized).map(CollaboratorResponse::from)
				.orElseThrow(this::notFound);
	}

	@Transactional(readOnly = true)
	public CollaboratorResponse find(Long id) {
		return CollaboratorResponse.from(findEntity(id));
	}

	/** The only place that changes a person record. The CPF does not change: changing it would mean another person. */
	public CollaboratorResponse update(Long id, UpdateCollaboratorRequest request) {
		Collaborator collaborator = findEntity(id);
		collaborator.setName(request.name().strip());
		collaborator.getRestrictions().clear();
		collaborator.getRestrictions().addAll(request.restrictions());
		return CollaboratorResponse.from(collaborators.saveAndFlush(collaborator));
	}

	private Collaborator findEntity(Long id) {
		return collaborators.findById(id).orElseThrow(this::notFound);
	}

	private ResourceNotFoundException notFound() {
		return new ResourceNotFoundException("Colaborador não encontrado.");
	}

}
