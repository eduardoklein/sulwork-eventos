package br.com.sulwork.events.domain;

import java.util.Set;

/** What an item contains that matters for dietary restrictions. */
public record ItemTraits(Long itemId, String name, Set<Allergen> allergens, Set<FoodCharacteristic> characteristics) {
}
