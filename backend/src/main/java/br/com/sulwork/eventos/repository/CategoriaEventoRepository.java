package br.com.sulwork.eventos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.eventos.domain.CategoriaEvento;
import br.com.sulwork.eventos.dto.CategoriaResponse;

public interface CategoriaEventoRepository extends JpaRepository<CategoriaEvento, Long> {

	Optional<CategoriaEvento> findByIdAndEventoId(Long id, Long eventoId);

	/** {@code ignorarId} nulo não ignora nenhuma categoria. */
	@Query("""
			select count(c) > 0 from CategoriaEvento c
			where c.evento.id = :eventoId and lower(c.nome) = lower(:nome)
			  and (:ignorarId is null or c.id <> :ignorarId)
			""")
	boolean existeNome(@Param("eventoId") Long eventoId, @Param("nome") String nome,
			@Param("ignorarId") Long ignorarId);

	@Query("select count(i) > 0 from ItemEvento i where i.categoria.id = :categoriaId")
	boolean temItens(@Param("categoriaId") Long categoriaId);

	/** {@code categoriaId} nulo traz todas as categorias do evento, na ordem de criação. */
	@Query("""
			select new br.com.sulwork.eventos.dto.CategoriaResponse(
			  c.id, c.nome, c.unidade, c.metaQuantidade,
			  (select count(i) from ItemEvento i where i.categoria = c),
			  (select coalesce(sum(i.quantidadeNecessaria), 0L) from ItemEvento i where i.categoria = c),
			  (select coalesce(sum(ct.quantidade), 0L) from Contribuicao ct where ct.item.categoria = c))
			from CategoriaEvento c
			where c.evento.id = :eventoId and (:categoriaId is null or c.id = :categoriaId)
			order by c.id
			""")
	List<CategoriaResponse> resumir(@Param("eventoId") Long eventoId, @Param("categoriaId") Long categoriaId);

}
