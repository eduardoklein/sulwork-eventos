package br.com.sulwork.events.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.events.domain.EventItem;
import jakarta.persistence.LockModeType;

public interface EventItemRepository extends JpaRepository<EventItem, Long> {

	@Query("select i from EventItem i join fetch i.category where i.id = :id and i.event.id = :eventId")
	Optional<EventItem> find(@Param("id") Long id, @Param("eventId") Long eventId);

	/**
	 * Locks the item row until the end of the transaction (SELECT ... FOR UPDATE). Whoever touches the sum of the
	 * item contributions, or its quantity or existence, passes through here first, and simultaneous requests wait
	 * their turn.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from EventItem i where i.id = :id and i.event.id = :eventId")
	Optional<EventItem> findWithLock(@Param("id") Long id, @Param("eventId") Long eventId);

	/** A null {@code categoryId} brings all items of the event, in creation order. */
	@Query("""
			select i from EventItem i join fetch i.category
			where i.event.id = :eventId and (:categoryId is null or i.category.id = :categoryId)
			order by i.id
			""")
	List<EventItem> list(@Param("eventId") Long eventId, @Param("categoryId") Long categoryId);

	/**
	 * Sums, in the database, what each category of the event needs and what it has already received, separating
	 * mandatory and optional items. Contributions are summed per item before the JOIN, so an item with many
	 * contributions does not multiply the sum of the required quantities. Categories without items appear with zeros.
	 */
	@Query(nativeQuery = true, value = """
			-- One row per category of the event, with everything summed by the database.
			SELECT cat.id AS "categoryId",
			       cat.name AS "name",
			       cat.unit AS "unit",
			       cat.goal_quantity AS "goalQuantity",
			       COUNT(i.id) AS "totalItems",
			       -- FILTER sums only the rows that pass the condition: it separates mandatory from optional
			       -- in a single pass. COALESCE turns the empty sum (NULL) into zero.
			       COALESCE(SUM(i.required_quantity) FILTER (WHERE i.mandatory), 0)::bigint AS "mandatoryRequired",
			       COALESCE(SUM(i.required_quantity) FILTER (WHERE NOT i.mandatory), 0)::bigint AS "optionalRequired",
			       COALESCE(SUM(c.total) FILTER (WHERE i.mandatory), 0)::bigint AS "mandatoryContributed",
			       COALESCE(SUM(c.total) FILTER (WHERE NOT i.mandatory), 0)::bigint AS "optionalContributed"
			FROM event_categories cat
			-- LEFT JOIN: a category without items stays in the list, with zeros.
			LEFT JOIN event_items i ON i.category_id = cat.id
			-- Contributions come already summed per item. If they came row by row, an item with
			-- 4 contributions would become 4 rows, and its required quantity would be summed 4 times.
			LEFT JOIN (SELECT item_id, SUM(quantity) AS total FROM contributions GROUP BY item_id) c
			       ON c.item_id = i.id
			WHERE cat.event_id = :eventId
			GROUP BY cat.id, cat.name, cat.unit, cat.goal_quantity
			ORDER BY cat.id
			""")
	List<CategoryCoverage> coveragePerCategory(@Param("eventId") Long eventId);

	/** A null {@code ignoredId} ignores no item. */
	@Query("""
			select count(i) > 0 from EventItem i
			where i.category.id = :categoryId and lower(i.name) = lower(:name)
			  and (:ignoredId is null or i.id <> :ignoredId)
			""")
	boolean nameExists(@Param("categoryId") Long categoryId, @Param("name") String name,
			@Param("ignoredId") Long ignoredId);

}
