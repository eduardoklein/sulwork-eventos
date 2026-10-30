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

	/**
	 * Soma, no banco, o que cada categoria do evento precisa e o que já recebeu, separando itens obrigatórios e
	 * opcionais. As contribuições são somadas por item antes do JOIN, para um item com várias contribuições não
	 * multiplicar a soma das quantidades necessárias. Categorias sem itens aparecem com zeros.
	 */
	@Query(nativeQuery = true, value = """
			-- Uma linha por categoria do evento, com tudo somado pelo banco.
			SELECT cat.id AS "categoriaId",
			       cat.nome AS "nome",
			       cat.unidade AS "unidade",
			       cat.meta_quantidade AS "metaQuantidade",
			       COUNT(i.id) AS "totalItens",
			       -- FILTER soma só as linhas que passam na condição: separa obrigatórios de opcionais
			       -- em uma única passada. COALESCE troca a soma vazia (NULL) por zero.
			       COALESCE(SUM(i.quantidade_necessaria) FILTER (WHERE i.obrigatorio), 0)::bigint AS "necessarioObrigatorios",
			       COALESCE(SUM(i.quantidade_necessaria) FILTER (WHERE NOT i.obrigatorio), 0)::bigint AS "necessarioOpcionais",
			       COALESCE(SUM(c.total) FILTER (WHERE i.obrigatorio), 0)::bigint AS "contribuidoObrigatorios",
			       COALESCE(SUM(c.total) FILTER (WHERE NOT i.obrigatorio), 0)::bigint AS "contribuidoOpcionais"
			FROM categorias_evento cat
			-- LEFT JOIN: uma categoria sem itens continua na lista, com zeros.
			LEFT JOIN itens_evento i ON i.categoria_id = cat.id
			-- As contribuições entram já somadas por item. Se entrassem linha a linha, um item com
			-- 4 contribuições viraria 4 linhas, e a quantidade necessária dele seria somada 4 vezes.
			LEFT JOIN (SELECT item_id, SUM(quantidade) AS total FROM contribuicoes GROUP BY item_id) c
			       ON c.item_id = i.id
			WHERE cat.evento_id = :eventoId
			GROUP BY cat.id, cat.nome, cat.unidade, cat.meta_quantidade
			ORDER BY cat.id
			""")
	List<CoberturaDaCategoria> coberturaPorCategoria(@Param("eventoId") Long eventoId);

	/** {@code ignorarId} nulo não ignora nenhum item. */
	@Query("""
			select count(i) > 0 from ItemEvento i
			where i.categoria.id = :categoriaId and lower(i.nome) = lower(:nome)
			  and (:ignorarId is null or i.id <> :ignorarId)
			""")
	boolean existeNome(@Param("categoriaId") Long categoriaId, @Param("nome") String nome,
			@Param("ignorarId") Long ignorarId);

}
