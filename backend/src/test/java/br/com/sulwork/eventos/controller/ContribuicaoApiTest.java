package br.com.sulwork.eventos.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import br.com.sulwork.eventos.TestcontainersConfiguration;
import br.com.sulwork.eventos.domain.CategoriaEvento;
import br.com.sulwork.eventos.domain.Colaborador;
import br.com.sulwork.eventos.domain.Contribuicao;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.ItemEvento;
import br.com.sulwork.eventos.domain.ParticipanteEvento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.domain.UnidadeMedida;
import br.com.sulwork.eventos.repository.EventoRepository;
import jakarta.persistence.EntityManager;

/** O relógio fica em 12:00 de São Paulo, 1º de novembro de 2026. */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContribuicaoApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private EntityManager em;

	@Test
	void registraContribuicaoEMostraPessoaItemEUnidade() throws Exception {
		Cenario c = cenario(10);

		criar(c.evento, c.ana, c.suco, 4, "Levo na garrafa")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/eventos/" + c.evento.getId() + "/contribuicoes/")))
				.andExpect(jsonPath("$.participanteNome").value("Ana"))
				.andExpect(jsonPath("$.itemNome").value("Suco"))
				.andExpect(jsonPath("$.unidade").value("UNIDADE"))
				.andExpect(jsonPath("$.quantidade").value(4))
				.andExpect(jsonPath("$.observacao").value("Levo na garrafa"))
				.andExpect(jsonPath("$.criadoEm").isNotEmpty());
		mvc.perform(get("/api/eventos/" + c.evento.getId() + "/itens/" + c.suco.getId()))
				.andExpect(jsonPath("$.quantidadeContribuida").value(4))
				.andExpect(jsonPath("$.quantidadePendente").value(6));
	}

	@Test
	void soEventoAbertoAceitaContribuicoes() throws Exception {
		for (StatusEvento status : new StatusEvento[] { StatusEvento.RASCUNHO, StatusEvento.ENCERRADO,
				StatusEvento.REALIZADO, StatusEvento.CANCELADO }) {
			Cenario c = cenario(10);
			c.evento.setStatus(status);
			eventos.saveAndFlush(c.evento);

			criar(c.evento, c.ana, c.suco, 1, null)
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value("Só um evento aberto aceita contribuições. Este evento está "
							+ status.getDescricao().toLowerCase() + "."));
		}
	}

	@Test
	void eventoComPrazoVencidoNaoAceitaContribuicoes() throws Exception {
		Cenario c = cenario(10);
		c.evento.setPrazoAlteracoes(LocalDateTime.parse("2026-10-30T12:00:00"));
		eventos.saveAndFlush(c.evento);

		criar(c.evento, c.ana, c.suco, 1, null).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void aceitaAteOLimiteExatoERecusaDepois() throws Exception {
		Cenario c = cenario(10);

		criar(c.evento, c.ana, c.suco, 4, null).andExpect(status().isCreated());
		criar(c.evento, c.beto, c.suco, 6, null).andExpect(status().isCreated());
		criar(c.evento, c.cida, c.suco, 1, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Este item já está completo."));
	}

	@Test
	void passarDoNecessarioDa422ComOQueAindaCabe() throws Exception {
		Cenario c = cenario(10);
		criar(c.evento, c.ana, c.suco, 7, null).andExpect(status().isCreated());

		criar(c.evento, c.beto, c.suco, 4, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Só cabem mais 3 (unidade) neste item."));
		mvc.perform(get(base(c.evento)).param("itemId", String.valueOf(c.suco.getId())))
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void amesmaPessoaContribuindoDeNovoComOMesmoItemDa409() throws Exception {
		Cenario c = cenario(10);
		criar(c.evento, c.ana, c.suco, 2, null).andExpect(status().isCreated());

		criar(c.evento, c.ana, c.suco, 2, null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Esta pessoa já contribui com este item. Edite a contribuição."));
		mvc.perform(get(base(c.evento))).andExpect(jsonPath("$[0].quantidade").value(2));
	}

	@Test
	void amesmaPessoaPodeContribuirComItensDiferentes() throws Exception {
		Cenario c = cenario(10);

		criar(c.evento, c.ana, c.suco, 2, null).andExpect(status().isCreated());
		criar(c.evento, c.ana, c.agua, 2, null).andExpect(status().isCreated());
	}

	@Test
	void participanteOuItemQueNaoSaoDoEventoDao422() throws Exception {
		Cenario c = cenario(10);
		Cenario outro = cenario(10);

		criar(c.evento, outro.ana, c.suco, 1, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Participante não encontrado neste evento."));
		criar(c.evento, c.ana, outro.suco, 1, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Item não encontrado neste evento."));
		mvc.perform(post(base(c.evento)).contentType(MediaType.APPLICATION_JSON)
						.content("{\"participanteId\":999999,\"itemId\":" + c.suco.getId() + ",\"quantidade\":1}"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void eventoInexistenteDa404() throws Exception {
		mvc.perform(post("/api/eventos/999999/contribuicoes").contentType(MediaType.APPLICATION_JSON)
						.content("{\"participanteId\":1,\"itemId\":1,\"quantidade\":1}"))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/eventos/999999/contribuicoes")).andExpect(status().isNotFound());
	}

	@Test
	void camposInvalidosDao400() throws Exception {
		Cenario c = cenario(10);

		mvc.perform(post(base(c.evento)).contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='participanteId')]").exists())
				.andExpect(jsonPath("$.erros[?(@.campo=='itemId')]").exists())
				.andExpect(jsonPath("$.erros[?(@.campo=='quantidade')]").exists());
		criar(c.evento, c.ana, c.suco, 1, "x".repeat(256)).andExpect(status().isBadRequest());
	}

	@Test
	void listaNaOrdemDeCriacaoEFiltraPorItemEPorParticipante() throws Exception {
		Cenario c = cenario(10);
		criar(c.evento, c.beto, c.suco, 1, null);
		criar(c.evento, c.ana, c.agua, 1, null);
		criar(c.evento, c.ana, c.suco, 1, null);

		mvc.perform(get(base(c.evento)))
				.andExpect(jsonPath("$[*].participanteNome", contains("Beto", "Ana", "Ana")));
		mvc.perform(get(base(c.evento)).param("itemId", String.valueOf(c.suco.getId())))
				.andExpect(jsonPath("$[*].participanteNome", contains("Beto", "Ana")));
		mvc.perform(get(base(c.evento)).param("participanteId", String.valueOf(c.ana.getId())))
				.andExpect(jsonPath("$[*].itemNome", contains("Água", "Suco")));
	}

	@Test
	void alteraQuantidadeEObservacaoSemCriarOutra() throws Exception {
		Cenario c = cenario(10);
		Contribuicao contribuicao = salvarContribuicao(c.ana, c.suco, 3);

		atualizar(c.evento, contribuicao, 5, "Mudei de ideia")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidade").value(5))
				.andExpect(jsonPath("$.observacao").value("Mudei de ideia"))
				.andExpect(jsonPath("$.id").value(contribuicao.getId()));
		mvc.perform(get(base(c.evento))).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void alteracaoRespeitaOLimiteContandoSoAsOutrasPessoas() throws Exception {
		Cenario c = cenario(10);
		Contribuicao daAna = salvarContribuicao(c.ana, c.suco, 4);
		salvarContribuicao(c.beto, c.suco, 3);

		atualizar(c.evento, daAna, 7, null).andExpect(status().isOk());
		atualizar(c.evento, daAna, 8, null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("O máximo para esta contribuição é 7 (unidade)."));
	}

	@Test
	void alteracaoParaZeroOuNegativoDa400() throws Exception {
		Cenario c = cenario(10);
		Contribuicao contribuicao = salvarContribuicao(c.ana, c.suco, 3);

		atualizar(c.evento, contribuicao, 0, null).andExpect(status().isBadRequest());
		atualizar(c.evento, contribuicao, -2, null).andExpect(status().isBadRequest());
	}

	@Test
	void alteracaoNaoMudaAPessoaNemOItem() throws Exception {
		Cenario c = cenario(10);
		Contribuicao contribuicao = salvarContribuicao(c.ana, c.suco, 3);

		mvc.perform(put(base(c.evento) + "/" + contribuicao.getId()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"participanteId\":" + c.beto.getId() + ",\"itemId\":" + c.agua.getId() + ",\"quantidade\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.participanteNome").value("Ana"))
				.andExpect(jsonPath("$.itemNome").value("Suco"));
	}

	@Test
	void removerLiberaAQuantidadeParaOutraPessoa() throws Exception {
		Cenario c = cenario(10);
		Contribuicao daAna = salvarContribuicao(c.ana, c.suco, 10);
		criar(c.evento, c.beto, c.suco, 1, null).andExpect(status().isUnprocessableEntity());

		mvc.perform(delete(base(c.evento) + "/" + daAna.getId())).andExpect(status().isNoContent());

		criar(c.evento, c.beto, c.suco, 10, null).andExpect(status().isCreated());
	}

	@Test
	void naoAlteraNemRemoveEmEventoEncerrado() throws Exception {
		Cenario c = cenario(10);
		Contribuicao contribuicao = salvarContribuicao(c.ana, c.suco, 3);
		c.evento.setStatus(StatusEvento.ENCERRADO);
		eventos.saveAndFlush(c.evento);

		atualizar(c.evento, contribuicao, 2, null).andExpect(status().isUnprocessableEntity());
		mvc.perform(delete(base(c.evento) + "/" + contribuicao.getId())).andExpect(status().isUnprocessableEntity());
		mvc.perform(get(base(c.evento))).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void contribuicaoDeOutroEventoOuInexistenteDa404() throws Exception {
		Cenario c = cenario(10);
		Cenario outro = cenario(10);
		Contribuicao contribuicao = salvarContribuicao(c.ana, c.suco, 3);

		mvc.perform(get(base(outro.evento) + "/" + contribuicao.getId())).andExpect(status().isNotFound());
		atualizar(outro.evento, contribuicao, 2, null).andExpect(status().isNotFound());
		mvc.perform(delete(base(outro.evento) + "/" + contribuicao.getId())).andExpect(status().isNotFound());
		mvc.perform(delete(base(c.evento) + "/999999")).andExpect(status().isNotFound());
		mvc.perform(get(base(c.evento) + "/" + contribuicao.getId())).andExpect(status().isOk());
	}

	@Test
	void reducaoDoItemAbaixoDoContribuidoContinuaProtegida() throws Exception {
		Cenario c = cenario(10);
		salvarContribuicao(c.ana, c.suco, 6);

		mvc.perform(put("/api/eventos/" + c.evento.getId() + "/itens/" + c.suco.getId())
						.contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Suco\",\"quantidadeNecessaria\":5}"))
				.andExpect(status().isUnprocessableEntity());
	}

	private String base(Evento evento) {
		return "/api/eventos/" + evento.getId() + "/contribuicoes";
	}

	private ResultActions criar(Evento evento, ParticipanteEvento participante, ItemEvento item, int quantidade,
			String observacao) throws Exception {
		String corpo = "{\"participanteId\":" + participante.getId() + ",\"itemId\":" + item.getId()
				+ ",\"quantidade\":" + quantidade
				+ (observacao == null ? "" : ",\"observacao\":\"" + observacao + "\"") + "}";
		return mvc.perform(post(base(evento)).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private ResultActions atualizar(Evento evento, Contribuicao contribuicao, int quantidade, String observacao)
			throws Exception {
		String corpo = "{\"quantidade\":" + quantidade
				+ (observacao == null ? "" : ",\"observacao\":\"" + observacao + "\"") + "}";
		return mvc.perform(put(base(evento) + "/" + contribuicao.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

	private record Cenario(Evento evento, ItemEvento suco, ItemEvento agua, ParticipanteEvento ana,
			ParticipanteEvento beto, ParticipanteEvento cida) {
	}

	private static int contadorDeCpf = 0;

	private Cenario cenario(int quantidadeDoSuco) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Evento evento = new Evento("Evento", null, TipoEvento.ALMOCO, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(StatusEvento.ABERTO);
		eventos.saveAndFlush(evento);
		CategoriaEvento categoria = new CategoriaEvento(evento, "Bebidas", UnidadeMedida.UNIDADE, 0);
		em.persist(categoria);
		ItemEvento suco = new ItemEvento(evento, categoria, "Suco", quantidadeDoSuco, false);
		ItemEvento agua = new ItemEvento(evento, categoria, "Água", 50, false);
		em.persist(suco);
		em.persist(agua);
		Cenario cenario = new Cenario(evento, suco, agua, participante(evento, "Ana"), participante(evento, "Beto"),
				participante(evento, "Cida"));
		em.flush();
		return cenario;
	}

	private ParticipanteEvento participante(Evento evento, String nome) {
		Colaborador colaborador = new Colaborador(nome, CpfsDeTeste.cpfValido(contadorDeCpf++));
		em.persist(colaborador);
		ParticipanteEvento participante = new ParticipanteEvento(evento, colaborador);
		em.persist(participante);
		return participante;
	}

	private Contribuicao salvarContribuicao(ParticipanteEvento participante, ItemEvento item, int quantidade) {
		Contribuicao contribuicao = new Contribuicao(participante, item, quantidade, null);
		em.persist(contribuicao);
		em.flush();
		return contribuicao;
	}

}
