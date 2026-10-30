package br.com.sulwork.events.dto;

import java.time.LocalDateTime;

import br.com.sulwork.events.domain.EventStatus;
import jakarta.validation.constraints.NotNull;

/** {@code newChangeDeadline} only applies when opening the event. */
public record ChangeStatusRequest(@NotNull EventStatus newStatus, LocalDateTime newChangeDeadline) {
}
