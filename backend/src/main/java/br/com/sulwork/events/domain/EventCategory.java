package br.com.sulwork.events.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_categories")
public class EventCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MeasureUnit unit;

	@Column(nullable = false)
	private int goalQuantity;

	protected EventCategory() {
	}

	public EventCategory(Event event, String name, MeasureUnit unit, int goalQuantity) {
		this.event = event;
		this.name = name;
		this.unit = unit;
		this.goalQuantity = goalQuantity;
	}

	public Long getId() {
		return id;
	}

	public Event getEvent() {
		return event;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MeasureUnit getUnit() {
		return unit;
	}

	public void setUnit(MeasureUnit unit) {
		this.unit = unit;
	}

	public int getGoalQuantity() {
		return goalQuantity;
	}

	public void setGoalQuantity(int goalQuantity) {
		this.goalQuantity = goalQuantity;
	}

}
