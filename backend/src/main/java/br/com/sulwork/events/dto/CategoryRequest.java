package br.com.sulwork.events.dto;

import br.com.sulwork.events.domain.MeasureUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
		@NotBlank @Size(max = 80) String name,
		@NotNull MeasureUnit unit,
		@NotNull @Min(0) Integer goalQuantity) {
}
