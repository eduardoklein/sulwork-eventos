package br.com.sulwork.events.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
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
@Table(name = "collaborators")
public class Collaborator {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 11, unique = true)
	private String cpf;

	@ElementCollection
	@BatchSize(size = 50)
	@CollectionTable(name = "collaborator_restrictions", joinColumns = @JoinColumn(name = "collaborator_id"))
	@Column(name = "restriction", nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<DietaryRestriction> restrictions = new HashSet<>();

	protected Collaborator() {
	}

	public Collaborator(String name, String cpf) {
		this.name = name;
		this.cpf = cpf;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCpf() {
		return cpf;
	}

	public Set<DietaryRestriction> getRestrictions() {
		return restrictions;
	}

}
