package br.com.sulwork.eventos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sulwork.eventos.domain.Colaborador;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {

	Optional<Colaborador> findByCpf(String cpf);

}
