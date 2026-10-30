package br.com.sulwork.events.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.events.domain.Collaborator;

public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {

	Optional<Collaborator> findByCpf(String cpf);

	/**
	 * Creates the person unless the CPF already exists, and returns the new id. When another transaction is creating
	 * the same CPF, the database waits for it and then returns nothing instead of failing.
	 */
	@Query(value = """
			INSERT INTO collaborators (name, cpf) VALUES (:name, :cpf)
			ON CONFLICT (cpf) DO NOTHING
			RETURNING id
			""", nativeQuery = true)
	Optional<Long> insertIfAbsent(@Param("name") String name, @Param("cpf") String cpf);

}
