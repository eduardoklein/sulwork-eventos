package br.com.sulwork.eventos.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "colaboradores")
public class Colaborador {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nome;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 11, unique = true)
	private String cpf;

	@ElementCollection
	@CollectionTable(name = "colaborador_restricoes", joinColumns = @JoinColumn(name = "colaborador_id"))
	@Column(name = "restricao", nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<RestricaoAlimentar> restricoes = new HashSet<>();

	protected Colaborador() {
	}

	public Colaborador(String nome, String cpf) {
		this.nome = nome;
		this.cpf = cpf;
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

	public String getCpf() {
		return cpf;
	}

	public Set<RestricaoAlimentar> getRestricoes() {
		return restricoes;
	}

}
