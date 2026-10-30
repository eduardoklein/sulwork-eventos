package br.com.sulwork.events.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.events.domain.Contribution;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {

	record ItemTotal(Long itemId, Long total) {
	}

	boolean existsByItemId(Long itemId);

	boolean existsByParticipantIdAndItemId(Long participantId, Long itemId);

	@Query("select c.item.id from Contribution c where c.id = :id and c.item.event.id = :eventId")
	Optional<Long> findItemId(@Param("id") Long id, @Param("eventId") Long eventId);

	@Query("""
			select c from Contribution c
			join fetch c.participant p join fetch p.collaborator
			join fetch c.item i join fetch i.category
			where c.id = :id and i.event.id = :eventId
			""")
	Optional<Contribution> find(@Param("id") Long id, @Param("eventId") Long eventId);

	/** Every filter is optional. In creation order. */
	@Query("""
			select c from Contribution c
			join fetch c.participant p join fetch p.collaborator
			join fetch c.item i join fetch i.category
			where i.event.id = :eventId
			  and (:itemId is null or i.id = :itemId)
			  and (:participantId is null or p.id = :participantId)
			order by c.id
			""")
	List<Contribution> list(@Param("eventId") Long eventId, @Param("itemId") Long itemId,
			@Param("participantId") Long participantId);

	@Query("select coalesce(sum(c.quantity), 0L) from Contribution c where c.item.id = :itemId and c.id <> :ignoredId")
	long sumByItemExcept(@Param("itemId") Long itemId, @Param("ignoredId") Long ignoredId);

	@Query("select coalesce(sum(c.quantity), 0L) from Contribution c where c.item.id = :itemId")
	long sumByItem(@Param("itemId") Long itemId);

	@Query("""
			select new br.com.sulwork.events.repository.ContributionRepository$ItemTotal(c.item.id, sum(c.quantity))
			from Contribution c where c.item.event.id = :eventId group by c.item.id
			""")
	List<ItemTotal> sumByItemOfEvent(@Param("eventId") Long eventId);

}
