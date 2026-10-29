package br.com.sulwork.eventos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.eventos.domain.ParticipanteEvento;

public interface ParticipanteEventoRepository extends JpaRepository<ParticipanteEvento, Long> {

	boolean existsByEventoIdAndColaboradorId(Long eventoId, Long colaboradorId);

	@Query("select p from ParticipanteEvento p join fetch p.colaborador where p.id = :id and p.evento.id = :eventoId")
	Optional<ParticipanteEvento> buscar(@Param("id") Long id, @Param("eventoId") Long eventoId);

	/** Na ordem de inscrição. */
	@Query("select p from ParticipanteEvento p join fetch p.colaborador where p.evento.id = :eventoId order by p.id")
	List<ParticipanteEvento> listar(@Param("eventoId") Long eventoId);

}
