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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "contribuicoes")
public class Contribuicao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "participante_id", nullable = false)
	private ParticipanteEvento participante;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private ItemEvento item;

	@Column(nullable = false)
	private int quantidade;

	private String observacao;

	@Column(nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	@Column(nullable = false)
	private LocalDateTime atualizadoEm;

	protected Contribuicao() {
	}

	public Contribuicao(ParticipanteEvento participante, ItemEvento item, int quantidade, String observacao) {
		this.participante = participante;
		this.item = item;
		this.quantidade = quantidade;
		this.observacao = observacao;
	}

	@PrePersist
	void aoCriar() {
		criadoEm = LocalDateTime.now();
		atualizadoEm = criadoEm;
	}

	@PreUpdate
	void aoAtualizar() {
		atualizadoEm = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public ParticipanteEvento getParticipante() {
		return participante;
	}

	public ItemEvento getItem() {
		return item;
	}

	public int getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(int quantidade) {
		this.quantidade = quantidade;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public LocalDateTime getAtualizadoEm() {
		return atualizadoEm;
	}

}
