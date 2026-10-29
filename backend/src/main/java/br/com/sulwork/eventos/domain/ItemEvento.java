package br.com.sulwork.eventos.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "itens_evento")
public class ItemEvento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private CategoriaEvento categoria;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false)
	private int quantidadeNecessaria;

	@Column(nullable = false)
	private boolean obrigatorio;

	@ElementCollection
	@BatchSize(size = 50)
	@CollectionTable(name = "item_alergenos", joinColumns = @JoinColumn(name = "item_id"))
	@Column(name = "alergeno", nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<Alergeno> alergenos = new HashSet<>();

	@ElementCollection
	@BatchSize(size = 50)
	@CollectionTable(name = "item_caracteristicas", joinColumns = @JoinColumn(name = "item_id"))
	@Column(name = "caracteristica", nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<CaracteristicaAlimentar> caracteristicas = new HashSet<>();

	protected ItemEvento() {
	}

	public ItemEvento(Evento evento, CategoriaEvento categoria, String nome, int quantidadeNecessaria,
			boolean obrigatorio) {
		this.evento = evento;
		this.categoria = categoria;
		this.nome = nome;
		this.quantidadeNecessaria = quantidadeNecessaria;
		this.obrigatorio = obrigatorio;
	}

	/** A unidade pertence à categoria: todos os itens dela usam a mesma. */
	public UnidadeMedida getUnidade() {
		return categoria.getUnidade();
	}

	public Long getId() {
		return id;
	}

	public Evento getEvento() {
		return evento;
	}

	public CategoriaEvento getCategoria() {
		return categoria;
	}

	public void setCategoria(CategoriaEvento categoria) {
		this.categoria = categoria;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public int getQuantidadeNecessaria() {
		return quantidadeNecessaria;
	}

	public void setQuantidadeNecessaria(int quantidadeNecessaria) {
		this.quantidadeNecessaria = quantidadeNecessaria;
	}

	public boolean isObrigatorio() {
		return obrigatorio;
	}

	public void setObrigatorio(boolean obrigatorio) {
		this.obrigatorio = obrigatorio;
	}

	public Set<Alergeno> getAlergenos() {
		return alergenos;
	}

	public Set<CaracteristicaAlimentar> getCaracteristicas() {
		return caracteristicas;
	}

}
