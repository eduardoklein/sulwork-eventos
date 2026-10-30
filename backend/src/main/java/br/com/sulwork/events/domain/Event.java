package br.com.sulwork.events.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import br.com.sulwork.events.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventType type;

	@Column(nullable = false)
	private LocalDateTime dateTime;

	@Column(nullable = false)
	private LocalDateTime changeDeadline;

	/** Time zone in which {@code dateTime} and {@code changeDeadline} are read. */
	@Column(nullable = false)
	private ZoneId timeZone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventStatus status = EventStatus.RASCUNHO;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected Event() {
	}

	public Event(String name, String description, EventType type, LocalDateTime dateTime, LocalDateTime changeDeadline,
			ZoneId timeZone) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.dateTime = dateTime;
		this.changeDeadline = changeDeadline;
		this.timeZone = timeZone;
	}

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	/** The deadline holds until the scheduled instant: at that exact instant changes are still allowed. */
	public boolean isDeadlineExpired(Instant now) {
		return now.isAfter(changeDeadline.atZone(timeZone).toInstant());
	}

	/** The event date and time are read in the event time zone. At the scheduled instant, the event has started. */
	public boolean hasStarted(Instant now) {
		return !now.isBefore(dateTime.atZone(timeZone).toInstant());
	}

	public boolean isOpenForContributions(Instant now) {
		return status.acceptsContributions() && !isDeadlineExpired(now);
	}

	public void requireEditableMenu() {
		if (!status.allowsMenuChanges()) {
			throw new BusinessRuleException(
					"Não é possível alterar o cardápio de um evento " + status.getDescription().toLowerCase() + ".");
		}
	}

	/** Checks the status first, then the deadline. */
	public void requireOpenContributions(Instant now) {
		if (!status.acceptsContributions()) {
			throw new BusinessRuleException("Só um evento aberto aceita contribuições. Este evento está "
					+ status.getDescription().toLowerCase() + ".");
		}
		if (isDeadlineExpired(now)) {
			throw new BusinessRuleException("O prazo para alterações já passou.");
		}
	}

	/** An open event whose deadline has passed becomes closed. Returns {@code true} if the status changed. */
	public boolean closeIfDeadlineExpired(Instant now) {
		if (status == EventStatus.ABERTO && isDeadlineExpired(now)) {
			status = EventStatus.ENCERRADO;
			return true;
		}
		return false;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public LocalDateTime getChangeDeadline() {
		return changeDeadline;
	}

	public void setChangeDeadline(LocalDateTime changeDeadline) {
		this.changeDeadline = changeDeadline;
	}

	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	public EventStatus getStatus() {
		return status;
	}

	public void setStatus(EventStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

}
