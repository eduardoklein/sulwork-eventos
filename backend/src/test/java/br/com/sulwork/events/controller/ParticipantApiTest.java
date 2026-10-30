package br.com.sulwork.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import br.com.sulwork.events.domain.EventCategory;
import br.com.sulwork.events.domain.Collaborator;
import br.com.sulwork.events.domain.Contribution;
import br.com.sulwork.events.domain.Event;
import br.com.sulwork.events.domain.EventItem;
import br.com.sulwork.events.domain.EventParticipant;
import br.com.sulwork.events.domain.DietaryRestriction;
import br.com.sulwork.events.domain.EventStatus;
import br.com.sulwork.events.domain.EventType;
import br.com.sulwork.events.domain.MeasureUnit;
import br.com.sulwork.events.repository.CollaboratorRepository;
import br.com.sulwork.events.repository.ContributionRepository;
import br.com.sulwork.events.repository.EventRepository;
import jakarta.persistence.EntityManager;

/** The clock stays at 12:00 in Sao Paulo, November 1st, 2026. */
@Import({ TestcontainersConfiguration.class, FixedClockConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ParticipantApiTest {

	private static final String CPF_ANA = "39053344705";
	private static final String CPF_ANA_WITH_MASK = "390.533.447-05";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventRepository events;

	@Autowired
	private CollaboratorRepository collaborators;

	@Autowired
	private ContributionRepository contributions;

	@Autowired
	private EntityManager em;

	@Test
	void enrollsNewPersonAndShowsMaskedCpf() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);

		enroll(event, "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\",\"restrictions\":[\"VEGAN\",\"SOY_ALLERGY\"]}")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/events/" + event.getId() + "/participants/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.collaboratorId").isNumber())
				.andExpect(jsonPath("$.name").value("Ana"))
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"))
				.andExpect(jsonPath("$.restrictions", contains("SOY_ALLERGY", "VEGAN")))
				.andExpect(jsonPath("$.enrolledAt").isNotEmpty());
	}

	@Test
	void storesCpfAsElevenDigitsEvenWhenItComesMasked() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);

		enroll(event, "{\"cpf\":\"" + CPF_ANA_WITH_MASK + "\",\"name\":\"Ana\"}").andExpect(status().isCreated());

		assertThat(collaborators.findByCpf(CPF_ANA)).isPresent();
	}

	@Test
	void omittedRestrictionsBecomeEmptyListForNewPerson() throws Exception {
		enroll(saveEvent(EventStatus.ABERTO), "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.restrictions", hasSize(0)));
	}

	@Test
	void invalidCpfReturns422() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);

		enroll(event, "{\"cpf\":\"111.111.111-11\",\"name\":\"Ana\"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("CPF inválido."));
		enroll(event, "{\"cpf\":\"39053344706\",\"name\":\"Ana\"}").andExpect(status().isUnprocessableEntity());
		enroll(event, "{\"cpf\":\"abc\",\"name\":\"Ana\"}").andExpect(status().isUnprocessableEntity());
	}

	@Test
	void missingOrBlankCpfReturns400() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);

		enroll(event, "{\"name\":\"Ana\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("cpf"));
		enroll(event, "{\"cpf\":\" \",\"name\":\"Ana\"}").andExpect(status().isBadRequest());
	}

	@Test
	void newCpfWithoutNameReturns422() throws Exception {
		enroll(saveEvent(EventStatus.ABERTO), "{\"cpf\":\"" + CPF_ANA + "\"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Informe o nome para um CPF novo."));
		assertThat(collaborators.findByCpf(CPF_ANA)).isEmpty();
	}

	@Test
	void restrictionOutsideListReturns400NamingTheListPosition() throws Exception {
		enroll(saveEvent(EventStatus.ABERTO),
				"{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\",\"restrictions\":[\"VEGAN\",\"COME_PEDRA\"]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Dados inválidos."))
				.andExpect(jsonPath("$.errors[0].field").value("restrictions[1]"));
	}

	@Test
	void samePersonInTwoEventsUsesSameRecord() throws Exception {
		Event first = saveEvent(EventStatus.ABERTO);
		Event second = saveEvent(EventStatus.ABERTO);

		enroll(first, "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\",\"restrictions\":[\"VEGAN\"]}")
				.andExpect(status().isCreated());
		enroll(second, "{\"cpf\":\"" + CPF_ANA + "\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Ana"))
				.andExpect(jsonPath("$.restrictions", contains("VEGAN")));

		assertThat(collaborators.count()).isEqualTo(1);
	}

	@Test
	void enrollingRegisteredPersonDoesNotChangeNameNorRestrictions() throws Exception {
		Event first = saveEvent(EventStatus.ABERTO);
		Event second = saveEvent(EventStatus.ABERTO);
		enroll(first, "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\",\"restrictions\":[\"VEGAN\"]}");

		enroll(second, "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Nome Errado\",\"restrictions\":[]}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Ana"))
				.andExpect(jsonPath("$.restrictions", contains("VEGAN")));

		em.flush();
		em.clear();
		Collaborator ana = collaborators.findByCpf(CPF_ANA).orElseThrow();
		assertThat(ana.getName()).isEqualTo("Ana");
		assertThat(ana.getRestrictions()).containsExactly(DietaryRestriction.VEGAN);
	}

	@Test
	void samePersonTwiceInSameEventReturns409() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		enroll(event, "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\"}").andExpect(status().isCreated());

		enroll(event, "{\"cpf\":\"" + CPF_ANA + "\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Esta pessoa já está inscrita neste evento."));
		enroll(event, "{\"cpf\":\"" + CPF_ANA_WITH_MASK + "\"}").andExpect(status().isConflict());
	}

	@Test
	void onlyOpenEventAcceptsEnrollments() throws Exception {
		for (EventStatus status : new EventStatus[] { EventStatus.RASCUNHO, EventStatus.ENCERRADO,
				EventStatus.REALIZADO, EventStatus.CANCELADO }) {
			enroll(saveEvent(status), "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\"}")
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value("Só um evento aberto aceita mudanças nos participantes. "
							+ "Este evento está " + status.getDescription().toLowerCase() + "."));
		}
		assertThat(collaborators.count()).isZero();
	}

	@Test
	void openEventWithExpiredDeadlineNoLongerAcceptsEnrollments() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		event.setChangeDeadline(LocalDateTime.parse("2026-10-30T12:00:00"));
		events.saveAndFlush(event);

		enroll(event, "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\"}").andExpect(status().isUnprocessableEntity());
	}

	@Test
	void missingEventReturns404() throws Exception {
		mvc.perform(post("/api/events/999999/participants").contentType(MediaType.APPLICATION_JSON)
						.content("{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Ana\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/events/999999/participants")).andExpect(status().isNotFound());
	}

	@Test
	void listsInEnrollmentOrderEvenInClosedEvent() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		enroll(event, "{\"cpf\":\"" + CPF_ANA + "\",\"name\":\"Zélia\"}");
		enroll(event, "{\"cpf\":\"52998224725\",\"name\":\"Abel\"}");
		event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(event);

		mvc.perform(get(base(event)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", contains("Zélia", "Abel")));
	}

	@Test
	void findsOneEnrollmentButNotOneFromAnotherEvent() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		Event other = saveEvent(EventStatus.ABERTO);
		EventParticipant participant = saveParticipant(event, "Ana", CPF_ANA);

		mvc.perform(get(base(event) + "/" + participant.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ana"));
		mvc.perform(get(base(other) + "/" + participant.getId())).andExpect(status().isNotFound());
	}

	@Test
	void removingEnrollmentDeletesPersonsContributionsInEventAndKeepsRecord() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventCategory category = new EventCategory(event, "Bebidas", MeasureUnit.UNIT, 0);
		em.persist(category);
		EventItem item = new EventItem(event, category, "Suco", 10, false);
		em.persist(item);
		EventParticipant ana = saveParticipant(event, "Ana", CPF_ANA);
		EventParticipant beto = saveParticipant(event, "Beto", "52998224725");
		em.persist(new Contribution(ana, item, 4, null));
		em.persist(new Contribution(beto, item, 3, null));
		em.flush();
		em.clear();

		mvc.perform(delete(base(event) + "/" + ana.getId())).andExpect(status().isNoContent());
		em.flush();
		em.clear();

		assertThat(contributions.sumByItem(item.getId())).isEqualTo(3);
		assertThat(collaborators.findByCpf(CPF_ANA)).isPresent();
		mvc.perform(get(base(event))).andExpect(jsonPath("$[*].name", contains("Beto")));
	}

	@Test
	void doesNotRemoveEnrollmentOfClosedEvent() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventParticipant ana = saveParticipant(event, "Ana", CPF_ANA);
		event.setStatus(EventStatus.ENCERRADO);
		events.saveAndFlush(event);

		mvc.perform(delete(base(event) + "/" + ana.getId())).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void removingMissingEnrollmentReturns404() throws Exception {
		mvc.perform(delete(base(saveEvent(EventStatus.ABERTO)) + "/999999")).andExpect(status().isNotFound());
	}

	@Test
	void afterRemovalPersonCanEnrollAgain() throws Exception {
		Event event = saveEvent(EventStatus.ABERTO);
		EventParticipant ana = saveParticipant(event, "Ana", CPF_ANA);
		mvc.perform(delete(base(event) + "/" + ana.getId())).andExpect(status().isNoContent());

		enroll(event, "{\"cpf\":\"" + CPF_ANA + "\"}").andExpect(status().isCreated());
	}

	private String base(Event event) {
		return "/api/events/" + event.getId() + "/participants";
	}

	private ResultActions enroll(Event event, String body) throws Exception {
		return mvc.perform(post(base(event)).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Event saveEvent(EventStatus status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Event event = new Event("Evento", null, EventType.LUNCH, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		event.setStatus(status);
		return events.saveAndFlush(event);
	}

	private EventParticipant saveParticipant(Event event, String name, String cpf) {
		Collaborator collaborator = collaborators.findByCpf(cpf).orElseGet(() -> collaborators.saveAndFlush(new Collaborator(name, cpf)));
		EventParticipant participant = new EventParticipant(event, collaborator);
		em.persist(participant);
		em.flush();
		return participant;
	}

}
