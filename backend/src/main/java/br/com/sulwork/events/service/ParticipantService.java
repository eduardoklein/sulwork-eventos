package br.com.sulwork.events.service;

import java.util.HashSet;
import java.util.List;

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

		try {
			Collaborator collaborator = collaborators.findByCpf(cpf).orElseGet(() -> newCollaborator(cpf, request));
			if (collaborator.getId() != null
					&& participants.existsByEventIdAndCollaboratorId(eventId, collaborator.getId())) {
				throw alreadyEnrolled();
			}
			if (collaborator.getId() == null) {
				collaborators.saveAndFlush(collaborator);
			}
			EventParticipant participant = new EventParticipant(event, collaborator);
			participants.saveAndFlush(participant);
			return ParticipantResponse.from(participant);
		}
		catch (DataIntegrityViolationException ex) {
			throw alreadyEnrolled();
		}
	}

	public void remove(Long eventId, Long id) {
		Event event = eventService.findSyncedEntity(eventId);
		requireEditableParticipants(event);
		participants.delete(findEntity(eventId, id));
	}

	private Collaborator newCollaborator(String cpf, EnrollParticipantRequest request) {
		if (request.name() == null || request.name().isBlank()) {
			throw new BusinessRuleException("Informe o nome para um CPF novo.");
		}
		Collaborator collaborator = new Collaborator(request.name().strip(), cpf);
		collaborator.getRestrictions().addAll(request.restrictions() == null ? new HashSet<>() : request.restrictions());
		return collaborator;
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
