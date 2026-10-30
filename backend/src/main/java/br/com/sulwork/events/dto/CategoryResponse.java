package br.com.sulwork.events.dto;

import br.com.sulwork.events.domain.MeasureUnit;

/** The totals sum the category items: what was planned and what people have already contributed. */
public record CategoryResponse(
		Long id,
		String name,
		MeasureUnit unit,
		int goalQuantity,
		long totalItems,
		long plannedQuantity,
		long contributedQuantity) {
}
