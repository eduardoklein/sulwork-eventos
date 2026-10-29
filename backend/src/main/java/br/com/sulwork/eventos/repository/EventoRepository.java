package br.com.sulwork.eventos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

	List<Evento> findByStatus(StatusEvento status);

	/** Cada filtro é opcional. {@code ate} é exclusivo. */
	@Query("""
			select e from Evento e
			where (:status is null or e.status = :status)
			  and (:tipo is null or e.tipo = :tipo)
			  and (cast(:de as localdatetime) is null or e.dataHora >= :de)
			  and (cast(:ate as localdatetime) is null or e.dataHora < :ate)
			order by e.dataHora, e.id
			""")
	List<Evento> filtrar(@Param("status") StatusEvento status, @Param("tipo") TipoEvento tipo,
			@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);

}
