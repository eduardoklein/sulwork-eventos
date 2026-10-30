package br.com.sulwork.events.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.Described;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.EventReadiness;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.dto.DomainOptionsResponse;
import br.com.sulwork.events.dto.OptionResponse;

@Service
public class DomainOptionsService {

	public DomainOptionsResponse options() {
		return new DomainOptionsResponse(
				of(EventType.values()),
				of(EventStatus.values()),
				of(MeasureUnit.values()),
				of(Allergen.values()),
				of(FoodCharacteristic.values()),
				of(DietaryRestriction.values()),
				of(EventReadiness.values()));
	}

	private static <E extends Enum<E> & Described> List<OptionResponse> of(E[] values) {
		return Arrays.stream(values).map(value -> new OptionResponse(value.name(), value.getDescription())).toList();
	}

}
