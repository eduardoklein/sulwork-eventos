package br.com.sulwork.events.domain;

import java.util.Set;

/** A participant and the dietary restrictions of the person. */
public record ParticipantDiet(Long participantId, String name, Set<DietaryRestriction> restrictions) {
}
