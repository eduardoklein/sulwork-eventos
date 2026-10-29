package br.com.sulwork.eventos.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum StatusEvento {

	RASCUNHO("Rascunho", true, false, false) {
		@Override
		public Set<StatusEvento> allowedTransitions() {
			return transicoes(ABERTO, CANCELADO);
		}
	},
	ABERTO("Aberto", true, true, true) {
		@Override
		public Set<StatusEvento> allowedTransitions() {
			return transicoes(ENCERRADO, CANCELADO);
		}
	},
	ENCERRADO("Encerrado", false, false, false) {
		@Override
		public Set<StatusEvento> allowedTransitions() {
			return transicoes(REALIZADO, CANCELADO);
		}
	},
	REALIZADO("Realizado", false, false, false) {
		@Override
		public Set<StatusEvento> allowedTransitions() {
			return Set.of();
		}
	},
	CANCELADO("Cancelado", false, false, false) {
		@Override
		public Set<StatusEvento> allowedTransitions() {
			return Set.of();
		}
	};

	private final String descricao;
	private final boolean catalogMutation;
	private final boolean participantMutation;
	private final boolean contributions;

	StatusEvento(String descricao, boolean catalogMutation, boolean participantMutation, boolean contributions) {
		this.descricao = descricao;
		this.catalogMutation = catalogMutation;
		this.participantMutation = participantMutation;
		this.contributions = contributions;
	}

	public abstract Set<StatusEvento> allowedTransitions();

	public boolean allowsCatalogMutation() {
		return catalogMutation;
	}

	public boolean allowsParticipantMutation() {
		return participantMutation;
	}

	public boolean allowsContributions() {
		return contributions;
	}

	public boolean canTransitionTo(StatusEvento novo) {
		return allowedTransitions().contains(novo);
	}

	public boolean isTerminal() {
		return allowedTransitions().isEmpty();
	}

	public String getDescricao() {
		return descricao;
	}

	private static Set<StatusEvento> transicoes(StatusEvento primeiro, StatusEvento... demais) {
		return Collections.unmodifiableSet(EnumSet.of(primeiro, demais));
	}

}
