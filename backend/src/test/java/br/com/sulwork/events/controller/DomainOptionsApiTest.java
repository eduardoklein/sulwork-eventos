package br.com.sulwork.events.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Allergen;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.EventReadiness;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.FoodCharacteristic;
import br.com.sulwork.events.domain.MeasureUnit;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DomainOptionsApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void everyListHasOneOptionPerEnumValue() throws Exception {
		mvc.perform(get("/api/domain/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventTypes", hasSize(EventType.values().length)))
				.andExpect(jsonPath("$.eventStatuses", hasSize(EventStatus.values().length)))
				.andExpect(jsonPath("$.measureUnits", hasSize(MeasureUnit.values().length)))
				.andExpect(jsonPath("$.allergens", hasSize(Allergen.values().length)))
				.andExpect(jsonPath("$.foodCharacteristics", hasSize(FoodCharacteristic.values().length)))
				.andExpect(jsonPath("$.dietaryRestrictions", hasSize(DietaryRestriction.values().length)))
				.andExpect(jsonPath("$.eventReadiness", hasSize(EventReadiness.values().length)));
	}

	@Test
	void eachOptionHasTheCodeAndTheTextTheUserReads() throws Exception {
		mvc.perform(get("/api/domain/options"))
				.andExpect(jsonPath("$.eventTypes[0].code").value("BREAKFAST"))
				.andExpect(jsonPath("$.eventTypes[0].description").value("Café da Manhã"))
				.andExpect(jsonPath("$.measureUnits[3].code").value("MILLILITER"))
				.andExpect(jsonPath("$.measureUnits[3].description").value("Mililitro"))
				.andExpect(jsonPath("$.dietaryRestrictions[1].code").value("CELIAC_GLUTEN_FREE"))
				.andExpect(jsonPath("$.dietaryRestrictions[1].description").value("Celíaco (sem glúten)"))
				.andExpect(jsonPath("$.eventReadiness[3].code").value("MANDATORY_SECURED"))
				.andExpect(jsonPath("$.eventReadiness[3].description").value("Obrigatórios garantidos"));
	}

	@Test
	void theFiveEventStatusesKeepTheNamesTheChallengeAsks() throws Exception {
		mvc.perform(get("/api/domain/options"))
				.andExpect(jsonPath("$.eventStatuses[*].code",
						org.hamcrest.Matchers.contains("RASCUNHO", "ABERTO", "ENCERRADO", "REALIZADO", "CANCELADO")))
				.andExpect(jsonPath("$.eventStatuses[0].description").value("Rascunho"));
	}

}
