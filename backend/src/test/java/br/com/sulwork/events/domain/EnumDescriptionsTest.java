package br.com.sulwork.events.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnumDescriptionsTest {

	@Test
	void everyValueHasReadableDescription() {
		for (EventType value : EventType.values()) {
			assertThat(value.getDescription()).isNotBlank();
		}
		for (EventStatus value : EventStatus.values()) {
			assertThat(value.getDescription()).isNotBlank();
		}
		for (MeasureUnit value : MeasureUnit.values()) {
			assertThat(value.getDescription()).isNotBlank();
		}
		for (Allergen value : Allergen.values()) {
			assertThat(value.getDescription()).isNotBlank();
		}
		for (FoodCharacteristic value : FoodCharacteristic.values()) {
			assertThat(value.getDescription()).isNotBlank();
		}
		for (DietaryRestriction value : DietaryRestriction.values()) {
			assertThat(value.getDescription()).isNotBlank();
		}
	}

	@Test
	void descriptionExamples() {
		assertThat(EventType.BREAKFAST.getDescription()).isEqualTo("Café da Manhã");
		assertThat(FoodCharacteristic.HIGH_SODIUM.getDescription()).isEqualTo("Rico em sal");
	}

}
