package br.com.sulwork.events.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum EventStatus {

	RASCUNHO("Rascunho", true, false, false) {
		@Override
		public Set<EventStatus> allowedTransitions() {
			return transitions(ABERTO, CANCELADO);
		}
	},
	ABERTO("Aberto", true, true, true) {
		@Override
		public Set<EventStatus> allowedTransitions() {
			return transitions(ENCERRADO, CANCELADO);
		}
	},
	ENCERRADO("Encerrado", false, false, false) {
		@Override
		public Set<EventStatus> allowedTransitions() {
			return transitions(REALIZADO, CANCELADO);
		}
	},
	REALIZADO("Realizado", false, false, false) {
		@Override
		public Set<EventStatus> allowedTransitions() {
			return Set.of();
		}
	},
	CANCELADO("Cancelado", false, false, false) {
		@Override
		public Set<EventStatus> allowedTransitions() {
			return Set.of();
		}
	};

	private final String description;
	private final boolean menuEditable;
	private final boolean participantsEditable;
	private final boolean contributionsOpen;

	EventStatus(String description, boolean menuEditable, boolean participantsEditable,
			boolean contributionsOpen) {
		this.description = description;
		this.menuEditable = menuEditable;
		this.participantsEditable = participantsEditable;
		this.contributionsOpen = contributionsOpen;
	}

	public abstract Set<EventStatus> allowedTransitions();

	public boolean allowsMenuChanges() {
		return menuEditable;
	}

	public boolean allowsParticipantChanges() {
		return participantsEditable;
	}

	public boolean acceptsContributions() {
		return contributionsOpen;
	}

	public boolean canTransitionTo(EventStatus newStatus) {
		return allowedTransitions().contains(newStatus);
	}

	public boolean isFinal() {
		return allowedTransitions().isEmpty();
	}

	public String getDescription() {
		return description;
	}

	private static Set<EventStatus> transitions(EventStatus first, EventStatus... rest) {
		return Collections.unmodifiableSet(EnumSet.of(first, rest));
	}

}
