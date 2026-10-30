package br.com.sulwork.eventos.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum StatusEvento {

	RASCUNHO("Rascunho", true, false, false) {
		@Override
		public Set<StatusEvento> transicoesPermitidas() {
			return transicoes(ABERTO, CANCELADO);
		}
	},
	ABERTO("Aberto", true, true, true) {
		@Override
		public Set<StatusEvento> transicoesPermitidas() {
			return transicoes(ENCERRADO, CANCELADO);
		}
	},
	ENCERRADO("Encerrado", false, false, false) {
		@Override
		public Set<StatusEvento> transicoesPermitidas() {
			return transicoes(REALIZADO, CANCELADO);
		}
	},
	REALIZADO("Realizado", false, false, false) {
		@Override
		public Set<StatusEvento> transicoesPermitidas() {
			return Set.of();
		}
	},
	CANCELADO("Cancelado", false, false, false) {
		@Override
		public Set<StatusEvento> transicoesPermitidas() {
			return Set.of();
		}
	};

	private final String descricao;
	private final boolean cardapioEditavel;
	private final boolean participantesEditaveis;
	private final boolean contribuicoesAbertas;

	StatusEvento(String descricao, boolean cardapioEditavel, boolean participantesEditaveis,
			boolean contribuicoesAbertas) {
		this.descricao = descricao;
		this.cardapioEditavel = cardapioEditavel;
		this.participantesEditaveis = participantesEditaveis;
		this.contribuicoesAbertas = contribuicoesAbertas;
	}

	public abstract Set<StatusEvento> transicoesPermitidas();

	public boolean permiteAlterarCardapio() {
		return cardapioEditavel;
	}

	public boolean permiteAlterarParticipantes() {
		return participantesEditaveis;
	}

	public boolean aceitaContribuicoes() {
		return contribuicoesAbertas;
	}

	public boolean podeIrPara(StatusEvento novo) {
		return transicoesPermitidas().contains(novo);
	}

	public boolean isFinal() {
		return transicoesPermitidas().isEmpty();
	}

	public String getDescricao() {
		return descricao;
	}

	private static Set<StatusEvento> transicoes(StatusEvento primeiro, StatusEvento... demais) {
		return Collections.unmodifiableSet(EnumSet.of(primeiro, demais));
	}

}
