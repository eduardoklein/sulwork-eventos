package br.com.sulwork.events.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;

public interface EventRepository extends JpaRepository<Event, Long> {

	List<Event> findByStatus(EventStatus status);

	/** Every filter is optional. {@code until} is exclusive. */
	@Query("""
			select e from Event e
			where (:status is null or e.status = :status)
			  and (:type is null or e.type = :type)
			  and (cast(:from as localdatetime) is null or e.dateTime >= :from)
			  and (cast(:until as localdatetime) is null or e.dateTime < :until)
			order by e.dateTime, e.id
			""")
	List<Event> findFiltered(@Param("status") EventStatus status, @Param("type") EventType type,
			@Param("from") LocalDateTime from, @Param("until") LocalDateTime until);

}
