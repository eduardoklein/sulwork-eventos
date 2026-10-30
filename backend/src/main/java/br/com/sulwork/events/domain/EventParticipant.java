package br.com.sulwork.events.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** The enrollment of a collaborator in an event. */
@Entity
@Table(name = "event_participants")
public class EventParticipant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "collaborator_id", nullable = false)
	private Collaborator collaborator;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected EventParticipant() {
	}

	public EventParticipant(Event event, Collaborator collaborator) {
		this.event = event;
		this.collaborator = collaborator;
	}

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Event getEvent() {
		return event;
	}

	public Collaborator getCollaborator() {
		return collaborator;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
