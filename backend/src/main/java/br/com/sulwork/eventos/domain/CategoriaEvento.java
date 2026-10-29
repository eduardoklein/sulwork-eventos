package br.com.sulwork.eventos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias_evento")
public class CategoriaEvento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	@Column(nullable = false)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UnidadeMedida unidade;

	@Column(nullable = false)
	private int metaQuantidade;

	protected CategoriaEvento() {
	}

	public CategoriaEvento(Evento evento, String nome, UnidadeMedida unidade, int metaQuantidade) {
		this.evento = evento;
		this.nome = nome;
		this.unidade = unidade;
		this.metaQuantidade = metaQuantidade;
	}

	public Long getId() {
		return id;
	}

	public Evento getEvento() {
		return evento;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public UnidadeMedida getUnidade() {
		return unidade;
	}

	public void setUnidade(UnidadeMedida unidade) {
		this.unidade = unidade;
	}

	public int getMetaQuantidade() {
		return metaQuantidade;
	}

	public void setMetaQuantidade(int metaQuantidade) {
		this.metaQuantidade = metaQuantidade;
	}

}
