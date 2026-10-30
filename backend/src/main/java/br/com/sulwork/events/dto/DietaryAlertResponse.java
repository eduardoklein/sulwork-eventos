package br.com.sulwork.events.dto;

import br.com.sulwork.events.domain.AlertLevel;

/** {@code participantId} is {@code null} for an alert about the whole event. */
public record DietaryAlertResponse(AlertLevel level, String message, Long participantId) {
}
