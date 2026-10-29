package br.com.sulwork.eventos.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A inscrição de um colaborador em um evento. */
@Entity
@Table(name = "participantes_evento")
public class ParticipanteEvento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "colaborador_id", nullable = false)
	private Colaborador colaborador;

	@Column(nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	protected ParticipanteEvento() {
	}

	public ParticipanteEvento(Evento evento, Colaborador colaborador) {
		this.evento = evento;
		this.colaborador = colaborador;
	}

	@PrePersist
	void aoCriar() {
		criadoEm = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Evento getEvento() {
		return evento;
	}

	public Colaborador getColaborador() {
		return colaborador;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

}
