package br.com.sulwork.eventos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.eventos.domain.ItemEvento;
import jakarta.persistence.LockModeType;

public interface ItemEventoRepository extends JpaRepository<ItemEvento, Long> {

	@Query("select i from ItemEvento i join fetch i.categoria where i.id = :id and i.evento.id = :eventoId")
	Optional<ItemEvento> buscar(@Param("id") Long id, @Param("eventoId") Long eventoId);

	/**
	 * Bloqueia a linha do item até o fim da transação (SELECT ... FOR UPDATE). Quem mexe na soma das contribuições
	 * do item, ou na quantidade ou existência dele, passa por aqui primeiro, e os pedidos simultâneos esperam a vez.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from ItemEvento i where i.id = :id and i.evento.id = :eventoId")
	Optional<ItemEvento> buscarComLock(@Param("id") Long id, @Param("eventoId") Long eventoId);

	/** {@code categoriaId} nulo traz todos os itens do evento, na ordem de criação. */
	@Query("""
			select i from ItemEvento i join fetch i.categoria
			where i.evento.id = :eventoId and (:categoriaId is null or i.categoria.id = :categoriaId)
			order by i.id
			""")
	List<ItemEvento> listar(@Param("eventoId") Long eventoId, @Param("categoriaId") Long categoriaId);

	/** {@code ignorarId} nulo não ignora nenhum item. */
	@Query("""
			select count(i) > 0 from ItemEvento i
			where i.categoria.id = :categoriaId and lower(i.nome) = lower(:nome)
			  and (:ignorarId is null or i.id <> :ignorarId)
			""")
	boolean existeNome(@Param("categoriaId") Long categoriaId, @Param("nome") String nome,
			@Param("ignorarId") Long ignorarId);

}
