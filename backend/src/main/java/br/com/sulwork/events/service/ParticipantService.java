package br.com.sulwork.events.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.dto.EnrollParticipantRequest;
import br.com.sulwork.events.dto.ParticipantResponse;
import br.com.sulwork.events.exception.BusinessRuleException;
import br.com.sulwork.events.exception.ConflictException;
import br.com.sulwork.events.exception.ResourceNotFoundException;
import br.com.sulwork.events.repository.CollaboratorRepository;
import br.com.sulwork.events.repository.EventParticipantRepository;
import br.com.sulwork.events.validation.CpfValidator;

@Service
@Transactional
public class ParticipantService {

	private final EventParticipantRepository participants;
	private final CollaboratorRepository collaborators;
	private final EventService eventService;

	public ParticipantService(EventParticipantRepository participants, CollaboratorRepository collaborators,
			EventService eventService) {
		this.participants = participants;
		this.collaborators = collaborators;
		this.eventService = eventService;
	}

	public List<ParticipantResponse> list(Long eventId) {
		eventService.findSyncedEntity(eventId);
		return participants.list(eventId).stream().map(ParticipantResponse::from).toList();
	}

	public ParticipantResponse find(Long eventId, Long id) {
		eventService.findSyncedEntity(eventId);
		return ParticipantResponse.from(findEntity(eventId, id));
	}

	/** A person already registered is not changed by the enrollment: the record changes only in the collaborators route. */
	public ParticipantResponse enroll(Long eventId, EnrollParticipantRequest request) {
		Event event = eventService.findSyncedEntity(eventId);
		requireEditableParticipants(event);
		String cpf = CpfValidator.normalizeAndValidate(request.cpf());
		Collaborator collaborator = collaborators.findByCpf(cpf).orElseGet(() -> newCollaborator(cpf, request));
		if (participants.existsByEventIdAndCollaboratorId(eventId, collaborator.getId())) {
			throw alreadyEnrolled();
		}

		EventParticipant participant = new EventParticipant(event, collaborator);
		try {
			participants.saveAndFlush(participant);
		}
		catch (DataIntegrityViolationException ex) {
			throw alreadyEnrolled();
		}
		return ParticipantResponse.from(participant);
	}

	public void remove(Long eventId, Long id) {
		Event event = eventService.findSyncedEntity(eventId);
		requireEditableParticipants(event);
		participants.delete(findEntity(eventId, id));
	}

	/**
	 * Creates the person. If another request created the same CPF in the meantime, that person is reused as is,
	 * because an enrollment never changes an existing record.
	 */
	private Collaborator newCollaborator(String cpf, EnrollParticipantRequest request) {
		if (request.name() == null || request.name().isBlank()) {
			throw new BusinessRuleException("Informe o nome para um CPF novo.");
		}
		Optional<Long> createdId = collaborators.insertIfAbsent(request.name().strip(), cpf);
		if (createdId.isEmpty()) {
			return collaborators.findByCpf(cpf).orElseThrow();
		}
		Collaborator collaborator = collaborators.findById(createdId.get()).orElseThrow();
		collaborator.getRestrictions().addAll(request.restrictions() == null ? new HashSet<>() : request.restrictions());
		return collaborators.saveAndFlush(collaborator);
	}

	private EventParticipant findEntity(Long eventId, Long id) {
		return participants.find(id, eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Participante não encontrado."));
	}

	private void requireEditableParticipants(Event event) {
		if (!event.getStatus().allowsParticipantChanges()) {
			throw new BusinessRuleException("Só um evento aberto aceita mudanças nos participantes. Este evento está "
					+ event.getStatus().getDescription().toLowerCase() + ".");
		}
	}

	private ConflictException alreadyEnrolled() {
		return new ConflictException("Esta pessoa já está inscrita neste evento.");
	}

}
