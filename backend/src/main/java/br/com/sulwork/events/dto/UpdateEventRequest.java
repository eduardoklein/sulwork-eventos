package br.com.sulwork.events.dto;

import java.time.LocalDateTime;

import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.validation.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code timeZone} is optional: if missing, the event keeps its current time zone. */
public record UpdateEventRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 1000) String description,
		@NotNull EventType type,
		@NotNull LocalDateTime dateTime,
		@NotNull LocalDateTime changeDeadline,
		@ValidTimeZone String timeZone) {
}
