package br.com.sulwork.eventos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.TestcontainersConfiguration;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.repository.EventoRepository;
import br.com.sulwork.eventos.service.EventoPrazoScheduler;

/** O relógio fica em 12:00 de São Paulo, 1º de novembro de 2026 (15:00 UTC). */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventoPrazoApiTest {

	private static final String PRAZO_VENCIDO = "2026-10-31T12:00:00";
	private static final String PRAZO_FUTURO = "2026-11-20T12:00:00";
	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final ZoneId LISBOA = ZoneId.of("Europe/Lisbon");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private EventoPrazoScheduler scheduler;

	@Autowired
	private ScheduledAnnotationBeanPostProcessor agendamentos;

	@Test
	void lerEventoAbertoComPrazoVencidoMostraEncerradoENaoAceitaContribuicoes() throws Exception {
		Evento evento = salvar("Vencido", StatusEvento.ABERTO, PRAZO_VENCIDO, SAO_PAULO);

		mvc.perform(get("/api/eventos/" + evento.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENCERRADO"))
				.andExpect(jsonPath("$.aceitaContribuicoes").value(false));
	}

	@Test
	void lerEventoAbertoNoPrazoMantemAberto() throws Exception {
		Evento evento = salvar("No prazo", StatusEvento.ABERTO, PRAZO_FUTURO, SAO_PAULO);

		mvc.perform(get("/api/eventos/" + evento.getId()))
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.aceitaContribuicoes").value(true));
	}

	@Test
	void rascunhoComPrazoVencidoContinuaRascunho() throws Exception {
		Evento evento = salvar("Rascunho", StatusEvento.RASCUNHO, PRAZO_VENCIDO, SAO_PAULO);

		mvc.perform(get("/api/eventos/" + evento.getId())).andExpect(jsonPath("$.status").value("RASCUNHO"));
	}

	@Test
	void listarEncerraOsVencidosAntesDeResponder() throws Exception {
		salvar("A vencido", StatusEvento.ABERTO, PRAZO_VENCIDO, SAO_PAULO);
		salvar("B no prazo", StatusEvento.ABERTO, PRAZO_FUTURO, SAO_PAULO);

		mvc.perform(get("/api/eventos").param("status", "ENCERRADO"))
				.andExpect(jsonPath("$[*].nome", contains("A vencido")));
		mvc.perform(get("/api/eventos").param("status", "ABERTO"))
				.andExpect(jsonPath("$[*].nome", contains("B no prazo")));
	}

	@Test
	void omesmoHorarioDeParedeVenceEmInstantesDiferentesPorFuso() throws Exception {
		// 12:00 de 01/11: em Lisboa foi às 12:00 UTC (já passou); em São Paulo é 15:00 UTC (instante exato, ainda vale).
		Evento lisboa = salvar("Lisboa", StatusEvento.ABERTO, "2026-11-01T12:00:00", LISBOA);
		Evento saoPaulo = salvar("São Paulo", StatusEvento.ABERTO, "2026-11-01T12:00:00", SAO_PAULO);

		mvc.perform(get("/api/eventos/" + lisboa.getId())).andExpect(jsonPath("$.status").value("ENCERRADO"));
		mvc.perform(get("/api/eventos/" + saoPaulo.getId())).andExpect(jsonPath("$.status").value("ABERTO"));
	}

	@Test
	void editarEventoVencidoSegueAsRegrasDeEncerrado() throws Exception {
		Evento evento = salvar("Vencido", StatusEvento.ABERTO, PRAZO_VENCIDO, SAO_PAULO);
		String corpo = "{\"nome\":\"Novo nome\",\"tipo\":\"LANCHE\",\"dataHora\":\"2026-12-01T12:00:00\","
				+ "\"prazoAlteracoes\":\"" + PRAZO_VENCIDO + "\"}";

		mvc.perform(put("/api/eventos/" + evento.getId()).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Novo nome"))
				.andExpect(jsonPath("$.status").value("ENCERRADO"));
	}

	@Test
	void cancelarEventoVencidoFunciona() throws Exception {
		Evento evento = salvar("Vencido", StatusEvento.ABERTO, PRAZO_VENCIDO, SAO_PAULO);

		mvc.perform(patch("/api/eventos/" + evento.getId() + "/status").contentType(MediaType.APPLICATION_JSON)
						.content("{\"novoStatus\":\"CANCELADO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));
	}

	@Test
	void agendadorEncerraSoOsAbertosVencidosEDizQuantos() {
		Evento vencido1 = salvar("Vencido 1", StatusEvento.ABERTO, PRAZO_VENCIDO, SAO_PAULO);
		Evento vencido2 = salvar("Vencido 2", StatusEvento.ABERTO, "2026-11-01T11:59:59", SAO_PAULO);
		Evento noPrazo = salvar("No prazo", StatusEvento.ABERTO, PRAZO_FUTURO, SAO_PAULO);
		Evento rascunho = salvar("Rascunho", StatusEvento.RASCUNHO, PRAZO_VENCIDO, SAO_PAULO);

		scheduler.encerrarEventosVencidos();
		eventos.flush();

		assertThat(statusDe(vencido1)).isEqualTo(StatusEvento.ENCERRADO);
		assertThat(statusDe(vencido2)).isEqualTo(StatusEvento.ENCERRADO);
		assertThat(statusDe(noPrazo)).isEqualTo(StatusEvento.ABERTO);
		assertThat(statusDe(rascunho)).isEqualTo(StatusEvento.RASCUNHO);
	}

	@Test
	void agendadorSemEventosVencidosNaoMudaNada() {
		Evento noPrazo = salvar("No prazo", StatusEvento.ABERTO, PRAZO_FUTURO, SAO_PAULO);

		scheduler.encerrarEventosVencidos();

		assertThat(statusDe(noPrazo)).isEqualTo(StatusEvento.ABERTO);
	}

	@Test
	void oAgendamentoEstaRegistradoNoContexto() {
		assertThat(agendamentos.getScheduledTasks()).hasSize(1);
	}

	private StatusEvento statusDe(Evento evento) {
		return eventos.findById(evento.getId()).orElseThrow().getStatus();
	}

	private Evento salvar(String nome, StatusEvento status, String prazo, ZoneId fuso) {
		Evento evento = new Evento(nome, null, TipoEvento.LANCHE, LocalDateTime.parse("2026-12-01T12:00:00"),
				LocalDateTime.parse(prazo), fuso);
		evento.setStatus(status);
		return eventos.saveAndFlush(evento);
	}

}
