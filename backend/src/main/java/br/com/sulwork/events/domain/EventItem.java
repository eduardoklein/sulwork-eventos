package br.com.sulwork.events.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "event_items")
public class EventItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private EventCategory category;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private int requiredQuantity;

	@Column(nullable = false)
	private boolean mandatory;

	@ElementCollection
	@BatchSize(size = 50)
	@CollectionTable(name = "item_allergens", joinColumns = @JoinColumn(name = "item_id"))
	@Column(name = "allergen", nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<Allergen> allergens = new HashSet<>();

	@ElementCollection
	@BatchSize(size = 50)
	@CollectionTable(name = "item_characteristics", joinColumns = @JoinColumn(name = "item_id"))
	@Column(name = "characteristic", nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<FoodCharacteristic> characteristics = new HashSet<>();

	protected EventItem() {
	}

	public EventItem(Event event, EventCategory category, String name, int requiredQuantity,
			boolean mandatory) {
		this.event = event;
		this.category = category;
		this.name = name;
		this.requiredQuantity = requiredQuantity;
		this.mandatory = mandatory;
	}

	/** The unit belongs to the category: all its items use the same one. */
	public MeasureUnit getUnit() {
		return category.getUnit();
	}

	public Long getId() {
		return id;
	}

	public Event getEvent() {
		return event;
	}

	public EventCategory getCategory() {
		return category;
	}

	public void setCategory(EventCategory category) {
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRequiredQuantity() {
		return requiredQuantity;
	}

	public void setRequiredQuantity(int requiredQuantity) {
		this.requiredQuantity = requiredQuantity;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public Set<Allergen> getAllergens() {
		return allergens;
	}

	public Set<FoodCharacteristic> getCharacteristics() {
		return characteristics;
	}

}
