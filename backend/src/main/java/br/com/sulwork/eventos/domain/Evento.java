package br.com.sulwork.eventos.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos")
public class Evento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoEvento tipo;

	@Column(nullable = false)
	private LocalDateTime dataHora;

	@Column(nullable = false)
	private LocalDateTime prazoAlteracoes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusEvento status = StatusEvento.RASCUNHO;

	@Column(nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	@Column(nullable = false)
	private LocalDateTime atualizadoEm;

	protected Evento() {
	}

	public Evento(String nome, String descricao, TipoEvento tipo, LocalDateTime dataHora, LocalDateTime prazoAlteracoes) {
		this.nome = nome;
		this.descricao = descricao;
		this.tipo = tipo;
		this.dataHora = dataHora;
		this.prazoAlteracoes = prazoAlteracoes;
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

	/** O prazo vale até o instante marcado: nesse instante ainda é possível alterar. */
	public boolean isPrazoExpirado(LocalDateTime agora) {
		return agora.isAfter(prazoAlteracoes);
	}

	public boolean isAbertoParaContribuicoes(LocalDateTime agora) {
		return status.allowsContributions() && !isPrazoExpirado(agora);
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public TipoEvento getTipo() {
		return tipo;
	}

	public void setTipo(TipoEvento tipo) {
		this.tipo = tipo;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}

	public LocalDateTime getPrazoAlteracoes() {
		return prazoAlteracoes;
	}

	public void setPrazoAlteracoes(LocalDateTime prazoAlteracoes) {
		this.prazoAlteracoes = prazoAlteracoes;
	}

	public StatusEvento getStatus() {
		return status;
	}

	public void setStatus(StatusEvento status) {
		this.status = status;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public LocalDateTime getAtualizadoEm() {
		return atualizadoEm;
	}

}
