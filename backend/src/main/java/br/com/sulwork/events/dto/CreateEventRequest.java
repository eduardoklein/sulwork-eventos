package br.com.sulwork.events.dto;

import java.time.LocalDateTime;

import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.validation.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code status} and {@code timeZone} are optional: the event starts as RASCUNHO and uses the default time zone. */
public record CreateEventRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 1000) String description,
		@NotNull EventType type,
		@NotNull LocalDateTime dateTime,
		@NotNull LocalDateTime changeDeadline,
		@ValidTimeZone String timeZone,
		EventStatus status) {
}
