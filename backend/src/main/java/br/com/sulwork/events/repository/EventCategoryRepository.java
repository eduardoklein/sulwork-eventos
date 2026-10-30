package br.com.sulwork.events.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.dto.CategoryResponse;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {

	Optional<EventCategory> findByIdAndEventId(Long id, Long eventId);

	/** A null {@code ignoredId} ignores no category. */
	@Query("""
			select count(c) > 0 from EventCategory c
			where c.event.id = :eventId and lower(c.name) = lower(:name)
			  and (:ignoredId is null or c.id <> :ignoredId)
			""")
	boolean nameExists(@Param("eventId") Long eventId, @Param("name") String name,
			@Param("ignoredId") Long ignoredId);

	@Query("select count(i) > 0 from EventItem i where i.category.id = :categoryId")
	boolean hasItems(@Param("categoryId") Long categoryId);

	/** A null {@code categoryId} brings all categories of the event, in creation order. */
	@Query("""
			select new br.com.sulwork.events.dto.CategoryResponse(
			  c.id, c.name, c.unit, c.goalQuantity,
			  (select count(i) from EventItem i where i.category = c),
			  (select coalesce(sum(i.requiredQuantity), 0L) from EventItem i where i.category = c),
			  (select coalesce(sum(ct.quantity), 0L) from Contribution ct where ct.item.category = c))
			from EventCategory c
			where c.event.id = :eventId and (:categoryId is null or c.id = :categoryId)
			order by c.id
			""")
	List<CategoryResponse> summarize(@Param("eventId") Long eventId, @Param("categoryId") Long categoryId);

}
