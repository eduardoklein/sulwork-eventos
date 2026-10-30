package br.com.sulwork.events.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.dto.UpdateEventRequest;
import br.com.sulwork.events.dto.CreateEventRequest;
import br.com.sulwork.events.dto.EventResponse;
import br.com.sulwork.events.dto.ChangeStatusRequest;
import br.com.sulwork.events.exception.BusinessRuleException;
import br.com.sulwork.events.exception.ResourceNotFoundException;
import br.com.sulwork.events.repository.EventRepository;

@Service
@Transactional
public class EventService {

	private final EventRepository events;
	private final Clock clock;
	private final ZoneId defaultTimeZone;

	public EventService(EventRepository events, Clock clock, @Value("${app.default-time-zone}") String defaultTimeZone) {
		this.events = events;
		this.clock = clock;
		this.defaultTimeZone = ZoneId.of(defaultTimeZone);
	}

	public EventResponse create(CreateEventRequest request) {
		EventStatus status = request.status() == null ? EventStatus.RASCUNHO : request.status();
		if (status != EventStatus.RASCUNHO && status != EventStatus.ABERTO) {
			throw new BusinessRuleException("Um evento novo só pode nascer em rascunho ou aberto.");
		}
		requireDeadlineNotAfterEvent(request.changeDeadline(), request.dateTime());

		Event event = new Event(request.name().strip(), request.description(), request.type(), request.dateTime(),
				request.changeDeadline(), timeZoneOrDefault(request.timeZone(), defaultTimeZone));
		event.setStatus(status);
		if (status == EventStatus.ABERTO) {
			requireFutureDeadline(event);
		}
		return response(events.save(event));
	}

	public List<EventResponse> list(EventStatus status, EventType type, LocalDate from, LocalDate until) {
		closeExpired();
		LocalDateTime start = from == null ? null : from.atStartOfDay();
		LocalDateTime end = until == null ? null : until.plusDays(1).atStartOfDay();
		return events.findFiltered(status, type, start, end).stream().map(this::response).toList();
	}

	public EventResponse find(Long id) {
		return response(findSyncedEntity(id));
	}

	public EventResponse update(Long id, UpdateEventRequest request) {
		Event event = findSyncedEntity(id);
		EventStatus status = event.getStatus();
		if (status == EventStatus.REALIZADO || status == EventStatus.CANCELADO) {
			throw new BusinessRuleException("Um evento " + status.getDescription().toLowerCase() + " não pode ser editado.");
		}
		ZoneId timeZone = timeZoneOrDefault(request.timeZone(), event.getTimeZone());
		if (status == EventStatus.ENCERRADO
				&& (!request.changeDeadline().equals(event.getChangeDeadline()) || !timeZone.equals(event.getTimeZone()))) {
			throw new BusinessRuleException("Em um evento encerrado, o prazo e o fuso não podem mudar.");
		}
		requireDeadlineNotAfterEvent(request.changeDeadline(), request.dateTime());

		event.setName(request.name().strip());
		event.setDescription(request.description());
		event.setType(request.type());
		event.setDateTime(request.dateTime());
		event.setChangeDeadline(request.changeDeadline());
		event.setTimeZone(timeZone);
		if (status == EventStatus.ABERTO) {
			requireFutureDeadline(event);
		}
		return response(events.saveAndFlush(event));
	}

	public EventResponse changeStatus(Long id, ChangeStatusRequest request) {
		Event event = findSyncedEntity(id);
		EventStatus current = event.getStatus();
		EventStatus newStatus = request.newStatus();
		if (request.newChangeDeadline() != null && newStatus != EventStatus.ABERTO) {
			throw new BusinessRuleException("O novo prazo só vale ao abrir o evento.");
		}
		if (!current.canTransitionTo(newStatus)) {
			throw new BusinessRuleException(invalidTransitionMessage(current, newStatus));
		}
		if (newStatus == EventStatus.ABERTO) {
			if (request.newChangeDeadline() != null) {
				requireDeadlineNotAfterEvent(request.newChangeDeadline(), event.getDateTime());
				event.setChangeDeadline(request.newChangeDeadline());
			}
			requireFutureDeadline(event);
		}
		if (newStatus == EventStatus.REALIZADO && !event.hasStarted(clock.instant())) {
			throw new BusinessRuleException("Um evento só pode ser marcado como realizado depois da data e hora marcadas.");
		}
		event.setStatus(newStatus);
		return response(events.saveAndFlush(event));
	}

	public void delete(Long id) {
		Event event = findSyncedEntity(id);
		if (event.getStatus() != EventStatus.RASCUNHO) {
			throw new BusinessRuleException(
					"Só eventos em rascunho podem ser excluídos. Cancele o evento para desfazê-lo.");
		}
		events.delete(event);
	}

	/** Closes the open events whose deadline has passed. Returns how many were closed. */
	public int closeExpired() {
		Instant now = clock.instant();
		return (int) events.findByStatus(EventStatus.ABERTO).stream()
				.filter(event -> event.closeIfDeadlineExpired(now))
				.count();
	}

	/** Finds the event with its status already up to date: if the deadline has passed, it shows as closed. */
	public Event findSyncedEntity(Long id) {
		Event event = events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
		event.closeIfDeadlineExpired(clock.instant());
		return event;
	}

	private EventResponse response(Event event) {
		return EventResponse.from(event, clock.instant());
	}

	private void requireDeadlineNotAfterEvent(LocalDateTime deadline, LocalDateTime dateTime) {
		if (deadline.isAfter(dateTime)) {
			throw new BusinessRuleException("O prazo para alterações não pode ser depois da data do evento.");
		}
	}

	private void requireFutureDeadline(Event event) {
		Instant now = clock.instant();
		if (event.isDeadlineExpired(now)) {
			throw new BusinessRuleException("Um evento aberto precisa de um prazo que ainda não passou.");
		}
	}

	private static String invalidTransitionMessage(EventStatus current, EventStatus newStatus) {
		String base = "Não é possível passar de " + current.getDescription() + " para " + newStatus.getDescription() + ". ";
		if (current.isFinal()) {
			return base + "Esse status é final.";
		}
		String allowed = current.allowedTransitions().stream().sorted()
				.map(EventStatus::getDescription).collect(Collectors.joining(", "));
		return base + "Transições permitidas: " + allowed + ".";
	}

	private static ZoneId timeZoneOrDefault(String timeZone, ZoneId fallback) {
		return timeZone == null || timeZone.isBlank() ? fallback : ZoneId.of(timeZone.strip());
	}

}
