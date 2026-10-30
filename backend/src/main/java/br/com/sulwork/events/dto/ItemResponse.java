package br.com.sulwork.events.dto;

import java.util.Comparator;
import java.util.List;

import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.MeasureUnit;

/** The percentage rounds down: it only shows 100 when the item is complete. */
public record ItemResponse(
		Long id,
		String name,
		Long categoryId,
		String categoryName,
		MeasureUnit unit,
		int requiredQuantity,
		boolean mandatory,
		long contributedQuantity,
		long pendingQuantity,
		int coveredPercentage,
		List<Allergen> allergens,
		List<FoodCharacteristic> characteristics) {

	public static ItemResponse from(EventItem item, long contributed) {
		int required = item.getRequiredQuantity();
		return new ItemResponse(
				item.getId(),
				item.getName(),
				item.getCategory().getId(),
				item.getCategory().getName(),
				item.getUnit(),
				required,
				item.isMandatory(),
				contributed,
				Math.max(0, required - contributed),
				(int) Math.min(100, contributed * 100 / required),
				item.getAllergens().stream().sorted(Comparator.naturalOrder()).toList(),
				item.getCharacteristics().stream().sorted(Comparator.naturalOrder()).toList());
	}

}
