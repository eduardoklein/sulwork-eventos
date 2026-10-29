package br.com.sulwork.eventos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import br.com.sulwork.eventos.TestcontainersConfiguration;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.repository.EventoRepository;

/** O relógio fica em 12:00 de São Paulo, 1º de novembro de 2026. */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventoStatusApiTest {

	private static final String DATA_FUTURA = "2026-12-01T12:00:00";
	private static final String DATA_PASSADA = "2026-10-31T12:00:00";
	private static final String DATA_AGORA = "2026-11-01T12:00:00";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Test
	void rascunhoVaiParaAberto() throws Exception {
		Evento evento = salvar(StatusEvento.RASCUNHO, DATA_FUTURA);

		mudar(evento, "ABERTO", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.aceitaContribuicoes").value(true))
				.andExpect(jsonPath("$.transicoesPermitidas[0]").value("ENCERRADO"));
	}

	@Test
	void naoAbreRascunhoComPrazoVencido() throws Exception {
		Evento evento = salvar(StatusEvento.RASCUNHO, DATA_PASSADA);

		mudar(evento, "ABERTO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Um evento aberto precisa de um prazo que ainda não passou."));
	}

	@Test
	void abreRascunhoComPrazoVencidoSeAChamadaTrouxerUmPrazoNovo() throws Exception {
		Evento evento = salvar(StatusEvento.RASCUNHO, DATA_FUTURA);
		evento.setPrazoAlteracoes(LocalDateTime.parse("2026-10-30T12:00:00"));
		eventos.saveAndFlush(evento);

		mudar(evento, "ABERTO", "2026-11-20T12:00:00")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.prazoAlteracoes").value("2026-11-20T12:00:00"));
	}

	@Test
	void prazoNovoDepoisDaDataDoEventoDa422() throws Exception {
		Evento evento = salvar(StatusEvento.RASCUNHO, DATA_FUTURA);

		mudar(evento, "ABERTO", "2026-12-02T12:00:00")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O prazo para alterações não pode ser depois da data do evento."));
	}

	@Test
	void prazoNovoSoValeAoAbrir() throws Exception {
		Evento evento = salvar(StatusEvento.RASCUNHO, DATA_FUTURA);

		mudar(evento, "CANCELADO", "2026-11-20T12:00:00")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O novo prazo só vale ao abrir o evento."));
	}

	@Test
	void transicoesValidasFuncionam() throws Exception {
		mudar(salvar(StatusEvento.ABERTO, DATA_FUTURA), "ENCERRADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADO"));
		mudar(salvar(StatusEvento.ABERTO, DATA_FUTURA), "CANCELADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
		mudar(salvar(StatusEvento.RASCUNHO, DATA_FUTURA), "CANCELADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
		mudar(salvar(StatusEvento.ENCERRADO, DATA_FUTURA), "CANCELADO", null)
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
	}

	@Test
	void encerradoVaiParaRealizadoDepoisDaDataDoEvento() throws Exception {
		mudar(salvar(StatusEvento.ENCERRADO, DATA_PASSADA), "REALIZADO", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REALIZADO"))
				.andExpect(jsonPath("$.transicoesPermitidas.length()").value(0));
	}

	@Test
	void encerradoVaiParaRealizadoNoInstanteExatoDoEvento() throws Exception {
		mudar(salvar(StatusEvento.ENCERRADO, DATA_AGORA), "REALIZADO", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REALIZADO"));
	}

	@Test
	void naoMarcaComoRealizadoAntesDaDataDoEvento() throws Exception {
		Evento evento = salvar(StatusEvento.ENCERRADO, DATA_FUTURA);

		mudar(evento, "REALIZADO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail")
						.value("Um evento só pode ser marcado como realizado depois da data e hora marcadas."));
		mvc.perform(get("/api/eventos/" + evento.getId())).andExpect(jsonPath("$.status").value("ENCERRADO"));
	}

	@Test
	void realizadoNaoVoltaParaAberto() throws Exception {
		mudar(salvar(StatusEvento.REALIZADO, DATA_PASSADA), "ABERTO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível passar de Realizado para Aberto. Esse status é final."));
	}

	@Test
	void canceladoEFinal() throws Exception {
		mudar(salvar(StatusEvento.CANCELADO, DATA_FUTURA), "RASCUNHO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível passar de Cancelado para Rascunho. Esse status é final."));
	}

	@Test
	void encerradoNaoReabreEListaAsTransicoesPermitidas() throws Exception {
		mudar(salvar(StatusEvento.ENCERRADO, DATA_FUTURA), "ABERTO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(
						"Não é possível passar de Encerrado para Aberto. Transições permitidas: Realizado, Cancelado."));
	}

	@Test
	void rascunhoNaoPulaParaRealizado() throws Exception {
		mudar(salvar(StatusEvento.RASCUNHO, DATA_PASSADA), "REALIZADO", null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(
						"Não é possível passar de Rascunho para Realizado. Transições permitidas: Aberto, Cancelado."));
	}

	@Test
	void passarParaOMesmoStatusDa422() throws Exception {
		mudar(salvar(StatusEvento.ABERTO, DATA_FUTURA), "ABERTO", null)
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void statusAusenteOuForaDaListaDa400() throws Exception {
		Evento evento = salvar(StatusEvento.RASCUNHO, DATA_FUTURA);

		mvc.perform(patch("/api/eventos/" + evento.getId() + "/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("novoStatus"));
		mudar(evento, "EXPLODIDO", null).andExpect(status().isBadRequest());
	}

	@Test
	void eventoInexistenteDa404() throws Exception {
		mvc.perform(patch("/api/eventos/999999/status").contentType(MediaType.APPLICATION_JSON)
						.content("{\"novoStatus\":\"ABERTO\"}"))
				.andExpect(status().isNotFound());
	}

	private ResultActions mudar(Evento evento, String novoStatus, String novoPrazo) throws Exception {
		String corpo = "{\"novoStatus\":\"" + novoStatus + "\""
				+ (novoPrazo == null ? "" : ",\"novoPrazoAlteracoes\":\"" + novoPrazo + "\"") + "}";
		return mvc.perform(patch("/api/eventos/" + evento.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private Evento salvar(StatusEvento status, String dataHora) {
		LocalDateTime data = LocalDateTime.parse(dataHora);
		Evento evento = new Evento("Evento", null, TipoEvento.LANCHE, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(status);
		return eventos.saveAndFlush(evento);
	}

}
