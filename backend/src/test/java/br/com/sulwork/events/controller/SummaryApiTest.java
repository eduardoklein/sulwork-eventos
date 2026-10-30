package br.com.sulwork.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.events.TestCpfs;
import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Contribution;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.repository.EventRepository;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SummaryApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private EntityManager em;

	@Test
	void buildsTheSummaryFromTheRealData() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = new EventCategory(event, "Pratos", MeasureUnit.UNIT, 0);
		em.persist(category);
		EventItem lasagna = new EventItem(event, category, "Lasagna", 10, true);
		EventItem cake = new EventItem(event, category, "Cake", 4, false);
		em.persist(lasagna);
		em.persist(cake);
		EventParticipant ana = participant(event, "Ana");
		em.persist(new Contribution(ana, lasagna, 6, null));
		em.flush();

		String text = body(event);

		assertThat(text).startsWith("*Almoço de fim de ano* (Almoço)\nData: 18/12/2026 às 12:00 (America/Sao_Paulo)\n"
				+ "Prazo para contribuir: 17/12/2026 às 12:00\nSituação: Aberto\n");
		assertThat(text).contains("*Cobertura*\nGeral: ");
		assertThat(text).contains("*O que ainda falta*\nObrigatórios:\n- Lasagna (Pratos): faltam 4 (unidade)\n"
				+ "Opcionais:\n- Cake (Pratos): faltam 4 (unidade)\n");
		assertThat(text).endsWith("*Quem já vai levar*\n- Ana: Lasagna 6 (unidade)");
	}

	@Test
	void doesNotLeakRestrictionsNorCpfsIntoTheGroupText() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = new EventCategory(event, "Pratos", MeasureUnit.UNIT, 0);
		em.persist(category);
		EventItem rice = new EventItem(event, category, "Rice", 4, true);
		em.persist(rice);
		EventParticipant ana = participant(event, "Ana", DietaryRestriction.VEGAN);
		em.persist(new Contribution(ana, rice, 2, null));
		em.flush();

		String text = body(event);

		assertThat(text).doesNotContainIgnoringCase("vegan").doesNotContainIgnoringCase("restri");
		assertThat(text).doesNotContain(TestCpfs.validCpf(0));
	}

	@Test
	void anEventWithoutItemsSaysSo() throws Exception {
		Event event = event(EventStatus.RASCUNHO);
		em.flush();

		assertThat(body(event)).contains("Ainda não há itens no cardápio.").contains("Ninguém contribuiu ainda.")
				.contains("Situação: Rascunho");
	}

	@Test
	void aCompleteMenuSaysNothingIsMissing() throws Exception {
		Event event = event(EventStatus.ABERTO);
		EventCategory category = new EventCategory(event, "Pratos", MeasureUnit.UNIT, 0);
		em.persist(category);
		EventItem rice = new EventItem(event, category, "Rice", 4, true);
		em.persist(rice);
		em.persist(new Contribution(participant(event, "Ana"), rice, 4, null));
		em.flush();

		assertThat(body(event)).contains("Nada: o cardápio está completo.").contains("Prontidão: Completo");
	}

	@Test
	void aClosedEventStillHasASummary() throws Exception {
		Event event = event(EventStatus.ENCERRADO);
		em.flush();

		mvc.perform(get("/api/events/" + event.getId() + "/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.text").isNotEmpty());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(get("/api/events/999999/summary")).andExpect(status().isNotFound());
	}

	private String body(Event event) throws Exception {
		String json = mvc.perform(get("/api/events/" + event.getId() + "/summary"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return new ObjectMapper().readTree(json).get("text").asString();
	}

	private Event event(EventStatus status) {
		LocalDateTime date = LocalDateTime.parse("2026-12-18T12:00:00");
		Event event = new Event("Almoço de fim de ano", null, EventType.LUNCH, date, date.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

	private EventParticipant participant(Event event, String name, DietaryRestriction... restrictions) {
		Collaborator collaborator = new Collaborator(name, TestCpfs.validCpf(0));
		collaborator.getRestrictions().addAll(List.of(restrictions));
		em.persist(collaborator);
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		return participant;
	}

}
