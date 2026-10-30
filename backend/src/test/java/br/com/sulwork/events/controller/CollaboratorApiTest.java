package br.com.sulwork.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.repository.CollaboratorRepository;
import br.com.sulwork.events.repository.EventRepository;
import jakarta.persistence.EntityManager;

@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CollaboratorApiTest {

	private static final String CPF_ANA = "39053344705";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private CollaboratorRepository collaborators;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	@Test
	void findsByExactCpfWithOrWithoutMaskAndShowsMaskedCpf() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA, DietaryRestriction.VEGAN);

		mvc.perform(get("/api/collaborators").param("cpf", CPF_ANA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ana.getId()))
				.andExpect(jsonPath("$.name").value("Ana"))
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"))
				.andExpect(jsonPath("$.restrictions", contains("VEGAN")));
		mvc.perform(get("/api/collaborators").param("cpf", "390.533.447-05"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ana.getId()));
	}

	@Test
	void cpfWithoutRecordReturns404() throws Exception {
		mvc.perform(get("/api/collaborators").param("cpf", CPF_ANA))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Colaborador não encontrado."));
	}

	@Test
	void invalidCpfInSearchReturns422() throws Exception {
		mvc.perform(get("/api/collaborators").param("cpf", "111.111.111-11"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void noGeneralListingOfRecordsExists() throws Exception {
		saveEvent("Ana", CPF_ANA);

		mvc.perform(get("/api/collaborators")).andExpect(status().is4xxClientError());
	}

	@Test
	void findsById() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA);

		mvc.perform(get("/api/collaborators/" + ana.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ana"))
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"));
		mvc.perform(get("/api/collaborators/999999")).andExpect(status().isNotFound());
	}

	@Test
	void editsNameAndReplacesRestrictions() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA, DietaryRestriction.VEGAN);

		update(ana.getId(), "{\"name\":\"Ana Souza\",\"restrictions\":[\"EGG_ALLERGY\",\"CELIAC_GLUTEN_FREE\"]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ana Souza"))
				.andExpect(jsonPath("$.restrictions", contains("CELIAC_GLUTEN_FREE", "EGG_ALLERGY")));

		em.flush();
		em.clear();
		Collaborator loaded = collaborators.findById(ana.getId()).orElseThrow();
		assertThat(loaded.getName()).isEqualTo("Ana Souza");
		assertThat(loaded.getRestrictions()).containsExactlyInAnyOrder(DietaryRestriction.EGG_ALLERGY,
				DietaryRestriction.CELIAC_GLUTEN_FREE);
	}

	@Test
	void emptyListClearsRestrictions() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA, DietaryRestriction.VEGAN);

		update(ana.getId(), "{\"name\":\"Ana\",\"restrictions\":[]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.restrictions", hasSize(0)));
	}

	@Test
	void omittedRestrictionsReturn400AndDeleteNothing() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA, DietaryRestriction.VEGAN);

		update(ana.getId(), "{\"name\":\"Outro nome\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("restrictions"));

		em.flush();
		em.clear();
		Collaborator loaded = collaborators.findById(ana.getId()).orElseThrow();
		assertThat(loaded.getName()).isEqualTo("Ana");
		assertThat(loaded.getRestrictions()).containsExactly(DietaryRestriction.VEGAN);
	}

	@Test
	void blankNameReturns400() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA);

		update(ana.getId(), "{\"name\":\" \",\"restrictions\":[]}").andExpect(status().isBadRequest());
	}

	@Test
	void cpfDoesNotChangeOnEdit() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA);

		update(ana.getId(), "{\"name\":\"Ana\",\"cpf\":\"52998224725\",\"restrictions\":[]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"));
		assertThat(collaborators.findById(ana.getId()).orElseThrow().getCpf()).isEqualTo(CPF_ANA);
	}

	@Test
	void editingMissingPersonReturns404() throws Exception {
		update(999999L, "{\"name\":\"Ana\",\"restrictions\":[]}").andExpect(status().isNotFound());
	}

	@Test
	void editAppearsInAllEventsEvenInClosedEvent() throws Exception {
		Collaborator ana = saveEvent("Ana", CPF_ANA);
		Event closed = saveEvent(EventStatus.ENCERRADO);
		EventParticipant participant = new EventParticipant(closed, ana);
		em.persist(participant);
		em.flush();

		update(ana.getId(), "{\"name\":\"Ana Souza\",\"restrictions\":[\"VEGAN\"]}").andExpect(status().isOk());
		em.flush();
		em.clear();

		mvc.perform(get("/api/events/" + closed.getId() + "/participants"))
				.andExpect(jsonPath("$[0].name").value("Ana Souza"))
				.andExpect(jsonPath("$[0].restrictions", contains("VEGAN")));
	}

	private ResultActions update(Long id, String body) throws Exception {
		return mvc.perform(put("/api/collaborators/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Collaborator saveEvent(String name, String cpf, DietaryRestriction... restrictions) {
		Collaborator collaborator = new Collaborator(name, cpf);
		collaborator.getRestrictions().addAll(java.util.List.of(restrictions));
		return collaborators.saveAndFlush(collaborator);
	}

	private Event saveEvent(EventStatus status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

}
