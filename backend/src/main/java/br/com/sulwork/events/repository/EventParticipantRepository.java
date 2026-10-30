package br.com.sulwork.events.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sulwork.events.domain.EventParticipant;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

	boolean existsByEventIdAndCollaboratorId(Long eventId, Long collaboratorId);

	@Query("select p from EventParticipant p join fetch p.collaborator where p.id = :id and p.event.id = :eventId")
	Optional<EventParticipant> find(@Param("id") Long id, @Param("eventId") Long eventId);

	/** In enrollment order. */
	@Query("select p from EventParticipant p join fetch p.collaborator where p.event.id = :eventId order by p.id")
	List<EventParticipant> list(@Param("eventId") Long eventId);

}
