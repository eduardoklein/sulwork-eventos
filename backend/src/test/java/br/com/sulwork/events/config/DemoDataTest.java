package br.com.sulwork.events.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sulwork.events.TestcontainersConfiguration;
import br.com.sulwork.events.validation.CpfValidator;

/** The demo data is on here (the other tests leave it out) and runs against the real clock. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.demo-data=true")
@AutoConfigureMockMvc
class DemoDataTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void listsFiveEventsOneInEachStatus() throws Exception {
		mvc.perform(get("/api/events"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[*].status",
						containsInAnyOrder("ABERTO", "ABERTO", "RASCUNHO", "REALIZADO", "CANCELADO")))
				.andExpect(jsonPath("$[0].name").value("Lanche da tarde do time"));
	}

	@Test
	void theOpenEventsAcceptContributionsBecauseTheDatesAreRelative() throws Exception {
		mvc.perform(get("/api/events").param("status", "ABERTO"))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].acceptsContributions", contains(true, true)))
				.andExpect(jsonPath("$[*].deadlineExpired", contains(false, false)));
	}

	@Test
	void theHeldEventAlreadyHappened() {
		Boolean inThePast = jdbc.queryForObject(
				"SELECT date_time < (now() AT TIME ZONE 'America/Sao_Paulo') FROM events WHERE status = 'REALIZADO'",
				Boolean.class);

		assertThat(inThePast).isTrue();
	}

	@Test
	void theLunchHasFourCategoriesWithTheirOwnUnitsAndFourteenItems() throws Exception {
		long lunch = eventId("Almoço de fim de ano");

		mvc.perform(get("/api/events/" + lunch + "/categories"))
				.andExpect(jsonPath("$[*].name", contains("Pratos principais", "Acompanhamentos", "Bebidas", "Sobremesas")))
				.andExpect(jsonPath("$[2].unit").value("MILLILITER"));
		mvc.perform(get("/api/events/" + lunch + "/items")).andExpect(jsonPath("$", hasSize(14)));
		mvc.perform(get("/api/events/" + lunch + "/participants")).andExpect(jsonPath("$", hasSize(8)));
	}

	@Test
	void theLunchCoverageIsInProgressAndShowsAGoalDeficit() throws Exception {
		long lunch = eventId("Almoço de fim de ano");

		mvc.perform(get("/api/events/" + lunch + "/coverage"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readiness").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.categories[3].name").value("Sobremesas"))
				.andExpect(jsonPath("$.categories[3].goalDeficit").value(10))
				.andExpect(jsonPath("$.categories[3].goalReached").value(false));
	}

	@Test
	void eachStatusShowsADifferentReadiness() throws Exception {
		mvc.perform(get("/api/events/" + eventId("Café da manhã de integração") + "/coverage"))
				.andExpect(jsonPath("$.readiness").value("MANDATORY_SECURED"));
		mvc.perform(get("/api/events/" + eventId("Lanche da tarde do time") + "/coverage"))
				.andExpect(jsonPath("$.readiness").value("COMPLETE"));
		mvc.perform(get("/api/events/" + eventId("Churrasco de confraternização") + "/coverage"))
				.andExpect(jsonPath("$.readiness").value("CRITICAL"));
	}

	@Test
	void theDietaryAnalysisWarnsAboutThePersonWhoCanEatAlmostNothing() throws Exception {
		long lunch = eventId("Almoço de fim de ano");

		mvc.perform(get("/api/events/" + lunch + "/dietary-analysis"))
				.andExpect(jsonPath("$.alerts", hasSize(1)))
				.andExpect(jsonPath("$.alerts[0].level").value("WARNING"))
				.andExpect(jsonPath("$.alerts[0].message")
						.value("Menos de 35% do cardápio é seguro para Hugo Pereira (4 de 14 itens)."))
				.andExpect(jsonPath("$.restrictions[?(@.restriction=='VEGAN')].peopleCount").value(2));
	}

	@Test
	void theSuggestionsAndTheSummaryWorkOnTheDemoData() throws Exception {
		long lunch = eventId("Almoço de fim de ano");
		long ana = jdbc.queryForObject("""
				SELECT p.id FROM event_participants p JOIN collaborators c ON c.id = p.collaborator_id
				WHERE p.event_id = ? AND c.name = 'Ana Souza'""", Long.class, lunch);

		mvc.perform(get("/api/events/" + lunch + "/suggestions").param("participantId", String.valueOf(ana)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].priority").isNumber())
				.andExpect(jsonPath("$[0].reasons").isNotEmpty());
		mvc.perform(get("/api/events/" + lunch + "/summary"))
				.andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("*Almoço de fim de ano*")))
				.andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Fábio Nunes: Lasanha de carne 8")));
	}

	@Test
	void theFictitiousCpfsAreValidAndDistinct() {
		List<String> cpfs = jdbc.queryForList("SELECT cpf FROM collaborators", String.class);

		assertThat(cpfs).hasSize(8).doesNotHaveDuplicates().allMatch(CpfValidator::isValid);
	}

	@Test
	void noItemIsContributedAboveWhatItRequires() {
		List<String> over = jdbc.queryForList("""
				SELECT i.name FROM event_items i JOIN contributions c ON c.item_id = i.id
				GROUP BY i.id, i.name, i.required_quantity HAVING sum(c.quantity) > i.required_quantity""",
				String.class);

		assertThat(over).isEmpty();
	}

	@Test
	void everyContributionComesFromSomeoneEnrolledInTheSameEvent() {
		Integer mismatches = jdbc.queryForObject("""
				SELECT count(*) FROM contributions c
				JOIN event_participants p ON p.id = c.participant_id
				JOIN event_items i ON i.id = c.item_id
				WHERE p.event_id <> i.event_id""", Integer.class);

		assertThat(mismatches).isZero();
	}

	private long eventId(String name) {
		return jdbc.queryForObject("SELECT id FROM events WHERE name = ?", Long.class, name);
	}

}
