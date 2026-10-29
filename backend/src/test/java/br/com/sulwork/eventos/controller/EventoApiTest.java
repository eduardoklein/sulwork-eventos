package br.com.sulwork.eventos.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.TestcontainersConfiguration;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.repository.EventoRepository;

@Import({ TestcontainersConfiguration.class, EventoApiTest.RelogioFixo.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventoApiTest {

	/** 12:00 em São Paulo no dia 1º de novembro de 2026. */
	private static final Instant AGORA = Instant.parse("2026-11-01T15:00:00Z");
	private static final String DATA_DO_EVENTO = "2026-12-01T12:00:00";
	private static final String PRAZO_FUTURO = "2026-11-30T12:00:00";
	private static final String PRAZO_VENCIDO = "2026-10-30T12:00:00";

	@TestConfiguration
	static class RelogioFixo {

		@Bean
		@Primary
		Clock clockFixo() {
			return Clock.fixed(AGORA, ZoneOffset.UTC);
		}

	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Test
	void criaEventoEmRascunhoComFusoPadraoECamposCalculados() throws Exception {
		criar(corpo("Almoço de fim de ano", "ALMOCO", DATA_DO_EVENTO, PRAZO_FUTURO, null, null))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/eventos/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nome").value("Almoço de fim de ano"))
				.andExpect(jsonPath("$.status").value("RASCUNHO"))
				.andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"))
				.andExpect(jsonPath("$.prazoExpirado").value(false))
				.andExpect(jsonPath("$.aceitaContribuicoes").value(false))
				.andExpect(jsonPath("$.transicoesPermitidas", contains("ABERTO", "CANCELADO")));
	}

	@Test
	void criaEventoAbertoComPrazoNoFuturo() throws Exception {
		criar(corpo("Lanche", "LANCHE", DATA_DO_EVENTO, PRAZO_FUTURO, null, "ABERTO"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.aceitaContribuicoes").value(true))
				.andExpect(jsonPath("$.transicoesPermitidas", contains("ENCERRADO", "CANCELADO")));
	}

	@Test
	void rascunhoComPrazoVencidoMostraPrazoExpiradoENaoAceitaContribuicoes() throws Exception {
		criar(corpo("Café", "CAFE_DA_MANHA", DATA_DO_EVENTO, PRAZO_VENCIDO, null, null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.prazoExpirado").value(true))
				.andExpect(jsonPath("$.aceitaContribuicoes").value(false));
	}

	@Test
	void criaEventoComFusoInformado() throws Exception {
		criar(corpo("Almoço em Lisboa", "ALMOCO", DATA_DO_EVENTO, PRAZO_FUTURO, "Europe/Lisbon", null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fuso").value("Europe/Lisbon"));
	}

	@Test
	void naoCriaEventoAbertoComPrazoVencido() throws Exception {
		criar(corpo("Lanche", "LANCHE", DATA_DO_EVENTO, PRAZO_VENCIDO, null, "ABERTO"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento aberto precisa de um prazo que ainda não passou."));
	}

	@Test
	void prazoNoFusoDoEventoValeNaHoraCerta() throws Exception {
		// 12:00 de Lisboa em 01/11 já passou (agora são 15:00 UTC); 12:00 de São Paulo ainda não.
		criar(corpo("Lisboa", "LANCHE", DATA_DO_EVENTO, "2026-11-01T12:00:00", "Europe/Lisbon", "ABERTO"))
				.andExpect(status().isUnprocessableEntity());
		criar(corpo("São Paulo", "LANCHE", DATA_DO_EVENTO, "2026-11-01T12:00:00", "America/Sao_Paulo", "ABERTO"))
				.andExpect(status().isCreated());
	}

	@Test
	void naoCriaEventoEmStatusQueNaoSejaRascunhoOuAberto() throws Exception {
		criar(corpo("Lanche", "LANCHE", DATA_DO_EVENTO, PRAZO_FUTURO, null, "ENCERRADO"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento novo só pode nascer em rascunho ou aberto."));
	}

	@Test
	void naoCriaEventoComPrazoDepoisDaDataDoEvento() throws Exception {
		criar(corpo("Lanche", "LANCHE", DATA_DO_EVENTO, "2026-12-02T12:00:00", null, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O prazo para alterações não pode ser depois da data do evento."));
	}

	@Test
	void camposInvalidosDao400ComMensagensEmPortugues() throws Exception {
		criar("{\"nome\":\"\",\"tipo\":\"ALMOCO\",\"dataHora\":\"" + DATA_DO_EVENTO + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("não deve estar em branco"))
				.andExpect(jsonPath("$.erros[?(@.campo=='prazoAlteracoes')].mensagem").value("não deve ser nulo"));
	}

	@Test
	void fusoInexistenteDa400() throws Exception {
		criar(corpo("Lanche", "LANCHE", DATA_DO_EVENTO, PRAZO_FUTURO, "Marte/Olimpo", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("fuso"));
	}

	@Test
	void tipoForaDaListaDa400() throws Exception {
		criar(corpo("Lanche", "JANTAR", DATA_DO_EVENTO, PRAZO_FUTURO, null, null))
				.andExpect(status().isBadRequest());
	}

	@Test
	void buscaEventoExistente() throws Exception {
		Evento evento = salvar("Almoço", TipoEvento.ALMOCO, StatusEvento.RASCUNHO, "2026-12-01T12:00:00");

		mvc.perform(get("/api/eventos/" + evento.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Almoço"));
	}

	@Test
	void eventoInexistenteDa404() throws Exception {
		mvc.perform(get("/api/eventos/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Evento não encontrado."));
	}

	@Test
	void listaOrdenadaPelaDataDoEventoEFiltraPorStatusTipoEData() throws Exception {
		salvar("C - dezembro", TipoEvento.ALMOCO, StatusEvento.ABERTO, "2026-12-20T12:00:00");
		salvar("A - novembro", TipoEvento.LANCHE, StatusEvento.RASCUNHO, "2026-11-10T15:00:00");
		salvar("B - dezembro cedo", TipoEvento.ALMOCO, StatusEvento.RASCUNHO, "2026-12-05T12:00:00");

		mvc.perform(get("/api/eventos"))
				.andExpect(jsonPath("$[*].nome", contains("A - novembro", "B - dezembro cedo", "C - dezembro")));
		mvc.perform(get("/api/eventos").param("status", "RASCUNHO"))
				.andExpect(jsonPath("$[*].nome", contains("A - novembro", "B - dezembro cedo")));
		mvc.perform(get("/api/eventos").param("tipo", "ALMOCO"))
				.andExpect(jsonPath("$[*].nome", contains("B - dezembro cedo", "C - dezembro")));
		mvc.perform(get("/api/eventos").param("de", "2026-12-01").param("ate", "2026-12-05"))
				.andExpect(jsonPath("$[*].nome", contains("B - dezembro cedo")));
		mvc.perform(get("/api/eventos").param("status", "ENCERRADO"))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void filtroDeStatusInvalidoDa400() throws Exception {
		mvc.perform(get("/api/eventos").param("status", "EXPLODIDO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'status'."));
	}

	@Test
	void editaRascunhoInteiro() throws Exception {
		Evento evento = salvar("Antigo", TipoEvento.LANCHE, StatusEvento.RASCUNHO, "2026-12-01T12:00:00");

		atualizar(evento, corpoAtualizar("Novo nome", "ALMOCO", "2026-12-10T12:00:00", "2026-12-09T12:00:00", "Europe/Lisbon"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Novo nome"))
				.andExpect(jsonPath("$.tipo").value("ALMOCO"))
				.andExpect(jsonPath("$.prazoAlteracoes").value("2026-12-09T12:00:00"))
				.andExpect(jsonPath("$.fuso").value("Europe/Lisbon"));
	}

	@Test
	void edicaoSemFusoMantemOFusoAtual() throws Exception {
		Evento evento = salvar("Antigo", TipoEvento.LANCHE, StatusEvento.RASCUNHO, "2026-12-01T12:00:00");

		atualizar(evento, corpoAtualizar("Novo", "LANCHE", "2026-12-01T12:00:00", PRAZO_FUTURO, null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"));
	}

	@Test
	void naoEditaEventoAbertoComPrazoJaVencido() throws Exception {
		Evento evento = salvar("Aberto", TipoEvento.LANCHE, StatusEvento.ABERTO, "2026-12-01T12:00:00");

		atualizar(evento, corpoAtualizar("Aberto", "LANCHE", DATA_DO_EVENTO, PRAZO_VENCIDO, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento aberto precisa de um prazo que ainda não passou."));
	}

	@Test
	void eventoEncerradoEditaNomeEDataMasNaoOPrazo() throws Exception {
		Evento evento = salvar("Encerrado", TipoEvento.LANCHE, StatusEvento.ENCERRADO, "2026-12-01T12:00:00");
		String prazoAtual = evento.getPrazoAlteracoes().toString();

		atualizar(evento, corpoAtualizar("Novo nome", "LANCHE", "2026-12-02T12:00:00", prazoAtual, null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Novo nome"))
				.andExpect(jsonPath("$.dataHora").value("2026-12-02T12:00:00"));

		atualizar(evento, corpoAtualizar("Novo nome", "LANCHE", "2026-12-02T12:00:00", "2026-11-29T12:00:00", null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Em um evento encerrado, o prazo e o fuso não podem mudar."));
		atualizar(evento, corpoAtualizar("Novo nome", "LANCHE", "2026-12-02T12:00:00", prazoAtual, "Europe/Lisbon"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void naoEditaEventoRealizadoNemCancelado() throws Exception {
		Evento realizado = salvar("Realizado", TipoEvento.LANCHE, StatusEvento.REALIZADO, "2026-12-01T12:00:00");
		Evento cancelado = salvar("Cancelado", TipoEvento.LANCHE, StatusEvento.CANCELADO, "2026-12-01T12:00:00");

		atualizar(realizado, corpoAtualizar("X", "LANCHE", DATA_DO_EVENTO, PRAZO_FUTURO, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento realizado não pode ser editado."));
		atualizar(cancelado, corpoAtualizar("X", "LANCHE", DATA_DO_EVENTO, PRAZO_FUTURO, null))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento cancelado não pode ser editado."));
	}

	@Test
	void editarEventoInexistenteDa404() throws Exception {
		mvc.perform(put("/api/eventos/999999").contentType(MediaType.APPLICATION_JSON)
						.content(corpoAtualizar("X", "LANCHE", DATA_DO_EVENTO, PRAZO_FUTURO, null)))
				.andExpect(status().isNotFound());
	}

	@Test
	void excluiRascunho() throws Exception {
		Evento evento = salvar("Rascunho", TipoEvento.LANCHE, StatusEvento.RASCUNHO, "2026-12-01T12:00:00");

		mvc.perform(delete("/api/eventos/" + evento.getId())).andExpect(status().isNoContent());
		mvc.perform(get("/api/eventos/" + evento.getId())).andExpect(status().isNotFound());
	}

	@Test
	void soRascunhoPodeSerExcluido() throws Exception {
		for (StatusEvento status : new StatusEvento[] { StatusEvento.ABERTO, StatusEvento.ENCERRADO,
				StatusEvento.REALIZADO, StatusEvento.CANCELADO }) {
			Evento evento = salvar(status.name(), TipoEvento.LANCHE, status, "2026-12-01T12:00:00");

			mvc.perform(delete("/api/eventos/" + evento.getId()))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail")
							.value("Só eventos em rascunho podem ser excluídos. Cancele o evento para desfazê-lo."));
		}
	}

	@Test
	void excluirEventoInexistenteDa404() throws Exception {
		mvc.perform(delete("/api/eventos/999999")).andExpect(status().isNotFound());
	}

	private ResultActions criar(String corpo) throws Exception {
		return mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private ResultActions atualizar(Evento evento, String corpo) throws Exception {
		return mvc.perform(put("/api/eventos/" + evento.getId()).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private Evento salvar(String nome, TipoEvento tipo, StatusEvento status, String dataHora) {
		LocalDateTime data = LocalDateTime.parse(dataHora);
		Evento evento = new Evento(nome, null, tipo, data, data.minusDays(1), ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(status);
		return eventos.saveAndFlush(evento);
	}

	private String corpo(String nome, String tipo, String dataHora, String prazo, String fuso, String status) {
		return "{\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo + "\",\"dataHora\":\"" + dataHora
				+ "\",\"prazoAlteracoes\":\"" + prazo + "\""
				+ (fuso == null ? "" : ",\"fuso\":\"" + fuso + "\"")
				+ (status == null ? "" : ",\"status\":\"" + status + "\"") + "}";
	}

	private String corpoAtualizar(String nome, String tipo, String dataHora, String prazo, String fuso) {
		return corpo(nome, tipo, dataHora, prazo, fuso, null);
	}

}
