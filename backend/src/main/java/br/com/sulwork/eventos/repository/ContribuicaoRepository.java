package br.com.sulwork.eventos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.eventos.domain.Contribuicao;

public interface ContribuicaoRepository extends JpaRepository<Contribuicao, Long> {

	record TotalPorItem(Long itemId, Long total) {
	}

	boolean existsByItemId(Long itemId);

	@Query("select coalesce(sum(c.quantidade), 0L) from Contribuicao c where c.item.id = :itemId")
	long somarPorItem(@Param("itemId") Long itemId);

	@Query("""
			select new br.com.sulwork.eventos.repository.ContribuicaoRepository$TotalPorItem(c.item.id, sum(c.quantidade))
			from Contribuicao c where c.item.evento.id = :eventoId group by c.item.id
			""")
	List<TotalPorItem> somarPorItemDoEvento(@Param("eventoId") Long eventoId);

}
