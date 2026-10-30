package br.com.sulwork.events.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sulwork.events.domain.Collaborator;

public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {

	Optional<Collaborator> findByCpf(String cpf);

}
