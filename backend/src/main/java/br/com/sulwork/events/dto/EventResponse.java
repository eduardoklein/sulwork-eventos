package br.com.sulwork.events.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;

public record EventResponse(
		Long id,
		String name,
		String description,
		EventType type,
		LocalDateTime dateTime,
		LocalDateTime changeDeadline,
		String timeZone,
		EventStatus status,
		boolean deadlineExpired,
		boolean acceptsContributions,
		List<EventStatus> allowedTransitions,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static EventResponse from(Event event, Instant now) {
		return new EventResponse(
				event.getId(),
				event.getName(),
				event.getDescription(),
				event.getType(),
				event.getDateTime(),
				event.getChangeDeadline(),
				event.getTimeZone().getId(),
				event.getStatus(),
				event.isDeadlineExpired(now),
				event.isOpenForContributions(now),
				event.getStatus().allowedTransitions().stream().sorted(Comparator.naturalOrder()).toList(),
				event.getCreatedAt(),
				event.getUpdatedAt());
	}

}
