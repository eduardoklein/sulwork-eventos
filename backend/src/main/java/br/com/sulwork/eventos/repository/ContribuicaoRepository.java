package br.com.sulwork.eventos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.eventos.domain.Contribuicao;

public interface ContribuicaoRepository extends JpaRepository<Contribuicao, Long> {

	record TotalPorItem(Long itemId, Long total) {
	}

	boolean existsByItemId(Long itemId);

	boolean existsByParticipanteIdAndItemId(Long participanteId, Long itemId);

	@Query("select c.item.id from Contribuicao c where c.id = :id and c.item.evento.id = :eventoId")
	Optional<Long> buscarIdDoItem(@Param("id") Long id, @Param("eventoId") Long eventoId);

	@Query("""
			select c from Contribuicao c
			join fetch c.participante p join fetch p.colaborador
			join fetch c.item i join fetch i.categoria
			where c.id = :id and i.evento.id = :eventoId
			""")
	Optional<Contribuicao> buscar(@Param("id") Long id, @Param("eventoId") Long eventoId);

	/** Cada filtro é opcional. Na ordem de criação. */
	@Query("""
			select c from Contribuicao c
			join fetch c.participante p join fetch p.colaborador
			join fetch c.item i join fetch i.categoria
			where i.evento.id = :eventoId
			  and (:itemId is null or i.id = :itemId)
			  and (:participanteId is null or p.id = :participanteId)
			order by c.id
			""")
	List<Contribuicao> listar(@Param("eventoId") Long eventoId, @Param("itemId") Long itemId,
			@Param("participanteId") Long participanteId);

	@Query("select coalesce(sum(c.quantidade), 0L) from Contribuicao c where c.item.id = :itemId and c.id <> :ignorarId")
	long somarPorItemExceto(@Param("itemId") Long itemId, @Param("ignorarId") Long ignorarId);

	@Query("select coalesce(sum(c.quantidade), 0L) from Contribuicao c where c.item.id = :itemId")
	long somarPorItem(@Param("itemId") Long itemId);

	@Query("""
			select new br.com.sulwork.eventos.repository.ContribuicaoRepository$TotalPorItem(c.item.id, sum(c.quantidade))
			from Contribuicao c where c.item.evento.id = :eventoId group by c.item.id
			""")
	List<TotalPorItem> somarPorItemDoEvento(@Param("eventoId") Long eventoId);

}
