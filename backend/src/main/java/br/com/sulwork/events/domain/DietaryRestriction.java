package br.com.sulwork.events.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static br.com.sulwork.events.domain.Allergen.PEANUT;
import static br.com.sulwork.events.domain.Allergen.TREE_NUTS;
import static br.com.sulwork.events.domain.Allergen.SHELLFISH;
import static br.com.sulwork.events.domain.Allergen.GLUTEN;
import static br.com.sulwork.events.domain.Allergen.LACTOSE;
import static br.com.sulwork.events.domain.Allergen.EGG;
import static br.com.sulwork.events.domain.Allergen.FISH;
import static br.com.sulwork.events.domain.Allergen.SOY;
import static br.com.sulwork.events.domain.FoodCharacteristic.ADDED_SUGAR;
import static br.com.sulwork.events.domain.FoodCharacteristic.HIGH_SODIUM;
import static br.com.sulwork.events.domain.FoodCharacteristic.ANIMAL_MEAT;

public enum DietaryRestriction {

	LACTOSE_INTOLERANCE("Intolerância à lactose", allergens(LACTOSE), noCharacteristics()),
	CELIAC_GLUTEN_FREE("Celíaco (sem glúten)", allergens(GLUTEN), noCharacteristics()),
	PEANUT_TREE_NUT_ALLERGY("Alergia a amendoim e castanhas", allergens(PEANUT, TREE_NUTS), noCharacteristics()),
	SHELLFISH_ALLERGY("Alergia a frutos do mar", allergens(SHELLFISH), noCharacteristics()),
	FISH_ALLERGY("Alergia a peixe", allergens(FISH), noCharacteristics()),
	EGG_ALLERGY("Alergia a ovo", allergens(EGG), noCharacteristics()),
	SOY_ALLERGY("Alergia a soja", allergens(SOY), noCharacteristics()),
	DIABETIC_NO_SUGAR("Diabético (sem açúcar)", noAllergens(), characteristics(ADDED_SUGAR)),
	HYPERTENSIVE_LOW_SODIUM("Hipertenso (pouco sal)", noAllergens(), characteristics(HIGH_SODIUM)),
	VEGETARIAN("Vegetariano", allergens(SHELLFISH, FISH), characteristics(ANIMAL_MEAT)),
	VEGAN("Vegano", allergens(SHELLFISH, FISH, LACTOSE, EGG), characteristics(ANIMAL_MEAT));

	private final String description;
	private final Set<Allergen> conflictingAllergens;
	private final Set<FoodCharacteristic> conflictingCharacteristics;

	DietaryRestriction(String description, Set<Allergen> conflictingAllergens,
			Set<FoodCharacteristic> conflictingCharacteristics) {
		this.description = description;
		this.conflictingAllergens = conflictingAllergens;
		this.conflictingCharacteristics = conflictingCharacteristics;
	}

	public String getDescription() {
		return description;
	}

	public Set<Allergen> getConflictingAllergens() {
		return conflictingAllergens;
	}

	public Set<FoodCharacteristic> getConflictingCharacteristics() {
		return conflictingCharacteristics;
	}

	public DietaryConflict findConflicts(Set<Allergen> itemAllergens,
			Set<FoodCharacteristic> itemCharacteristics) {
		return new DietaryConflict(
				intersection(conflictingAllergens, itemAllergens, Allergen.class),
				intersection(conflictingCharacteristics, itemCharacteristics, FoodCharacteristic.class));
	}

	private static <E extends Enum<E>> Set<E> intersection(Set<E> conflicting, Set<E> ofItem, Class<E> type) {
		EnumSet<E> result = EnumSet.noneOf(type);
		result.addAll(conflicting);
		result.retainAll(ofItem);
		return Collections.unmodifiableSet(result);
	}

	private static Set<Allergen> allergens(Allergen first, Allergen... rest) {
		return Collections.unmodifiableSet(EnumSet.of(first, rest));
	}

	private static Set<Allergen> noAllergens() {
		return Collections.unmodifiableSet(EnumSet.noneOf(Allergen.class));
	}

	private static Set<FoodCharacteristic> characteristics(FoodCharacteristic first,
			FoodCharacteristic... rest) {
		return Collections.unmodifiableSet(EnumSet.of(first, rest));
	}

	private static Set<FoodCharacteristic> noCharacteristics() {
		return Collections.unmodifiableSet(EnumSet.noneOf(FoodCharacteristic.class));
	}

}
