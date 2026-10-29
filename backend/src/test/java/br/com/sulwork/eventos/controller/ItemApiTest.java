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
import br.com.sulwork.eventos.domain.Alergeno;
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
class ItemApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private EntityManager em;

	@Test
	void criaItemComAlergenosECaracteristicasEmOrdemFixa() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Doces", UnidadeMedida.UNIDADE);

		criar(evento, corpoCriar("Brigadeiro", categoria.getId(), 40, true,
				"[\"OVO\",\"LACTOSE\"]", "[\"ACUCAR_ADICIONADO\"]"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/eventos/" + evento.getId() + "/itens/")))
				.andExpect(jsonPath("$.nome").value("Brigadeiro"))
				.andExpect(jsonPath("$.categoriaId").value(categoria.getId()))
				.andExpect(jsonPath("$.categoriaNome").value("Doces"))
				.andExpect(jsonPath("$.unidade").value("UNIDADE"))
				.andExpect(jsonPath("$.quantidadeNecessaria").value(40))
				.andExpect(jsonPath("$.obrigatorio").value(true))
				.andExpect(jsonPath("$.quantidadeContribuida").value(0))
				.andExpect(jsonPath("$.quantidadePendente").value(40))
				.andExpect(jsonPath("$.percentualCoberto").value(0))
				.andExpect(jsonPath("$.alergenos", contains("LACTOSE", "OVO")))
				.andExpect(jsonPath("$.caracteristicas", contains("ACUCAR_ADICIONADO")));
	}

	@Test
	void criaItemSemListasENaoObrigatorioPorPadrao() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.MILILITRO);

		criar(evento, "{\"nome\":\"Suco\",\"categoriaId\":" + categoria.getId() + ",\"quantidadeNecessaria\":2000}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.obrigatorio").value(false))
				.andExpect(jsonPath("$.alergenos", hasSize(0)))
				.andExpect(jsonPath("$.caracteristicas", hasSize(0)));
	}

	@Test
	void nomeRepetidoNaMesmaCategoriaDa409SemDiferenciarMaiusculasNemEspacos() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		criar(evento, corpoCriar("Suco", categoria.getId(), 10, false, "[]", "[]")).andExpect(status().isCreated());

		criar(evento, corpoCriar("Suco", categoria.getId(), 5, false, "[]", "[]"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um item com esse nome nesta categoria."));
		criar(evento, corpoCriar("  SUCO ", categoria.getId(), 5, false, "[]", "[]")).andExpect(status().isConflict());
	}

	@Test
	void mesmoNomeEmCategoriaDiferenteEhAceito() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento bebidas = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		CategoriaEvento sobremesas = salvarCategoria(evento, "Sobremesas", UnidadeMedida.UNIDADE);

		criar(evento, corpoCriar("Gelo", bebidas.getId(), 10, false, "[]", "[]")).andExpect(status().isCreated());
		criar(evento, corpoCriar("Gelo", sobremesas.getId(), 10, false, "[]", "[]")).andExpect(status().isCreated());
	}

	@Test
	void camposInvalidosDao400() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);

		criar(evento, "{\"nome\":\"\",\"quantidadeNecessaria\":0}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("não deve estar em branco"))
				.andExpect(jsonPath("$.erros[?(@.campo=='categoriaId')].mensagem").value("não deve ser nulo"))
				.andExpect(jsonPath("$.erros[?(@.campo=='quantidadeNecessaria')]").exists());
		criar(evento, corpoCriar("Suco", categoria.getId(), 5, false, "[\"POEIRA\"]", "[]"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void categoriaInexistenteOuDeOutroEventoDa422() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		Evento outro = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoriaDeOutro = salvarCategoria(outro, "Bebidas", UnidadeMedida.UNIDADE);

		criar(evento, corpoCriar("Suco", 999999L, 5, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Categoria não encontrada neste evento."));
		criar(evento, corpoCriar("Suco", categoriaDeOutro.getId(), 5, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void eventoInexistenteDa404() throws Exception {
		mvc.perform(post("/api/eventos/999999/itens").contentType(MediaType.APPLICATION_JSON)
						.content(corpoCriar("Suco", 1L, 5, false, "[]", "[]")))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/eventos/999999/itens")).andExpect(status().isNotFound());
	}

	@Test
	void cardapioSoMudaEmRascunhoEAberto() throws Exception {
		for (StatusEvento status : new StatusEvento[] { StatusEvento.ENCERRADO, StatusEvento.REALIZADO,
				StatusEvento.CANCELADO }) {
			Evento evento = salvarEvento(status);
			CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);

			criar(evento, corpoCriar("Suco", categoria.getId(), 5, false, "[]", "[]"))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value(
							"Não é possível alterar o cardápio de um evento " + status.getDescricao().toLowerCase() + "."));
		}
	}

	@Test
	void eventoAbertoComPrazoVencidoJaContaComoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		evento.setPrazoAlteracoes(LocalDateTime.parse("2026-10-30T12:00:00"));
		eventos.saveAndFlush(evento);

		criar(evento, corpoCriar("Suco", categoria.getId(), 5, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void listaNaOrdemDeCriacaoEFiltraPorCategoria() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento bebidas = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		CategoriaEvento doces = salvarCategoria(evento, "Doces", UnidadeMedida.UNIDADE);
		salvarItem(evento, bebidas, "Zebra", 1);
		salvarItem(evento, doces, "Bolo", 1);
		salvarItem(evento, bebidas, "Abacaxi", 1);

		mvc.perform(get(base(evento)))
				.andExpect(jsonPath("$[*].nome", contains("Zebra", "Bolo", "Abacaxi")));
		mvc.perform(get(base(evento)).param("categoriaId", String.valueOf(bebidas.getId())))
				.andExpect(jsonPath("$[*].nome", contains("Zebra", "Abacaxi")));
	}

	@Test
	void contribuicoesAlimentamPendenteEPercentualArredondadoParaBaixo() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		ItemEvento suco = salvarItem(evento, categoria, "Suco", 10);
		ItemEvento quase = salvarItem(evento, categoria, "Quase completo", 200);
		ItemEvento completo = salvarItem(evento, categoria, "Completo", 5);
		ParticipanteEvento ana = salvarParticipante(evento, "Ana", "52998224725");
		ParticipanteEvento beto = salvarParticipante(evento, "Beto", "39053344705");
		contribuir(ana, suco, 4);
		contribuir(beto, suco, 3);
		contribuir(ana, quase, 199);
		contribuir(beto, completo, 5);

		mvc.perform(get(base(evento) + "/" + suco.getId()))
				.andExpect(jsonPath("$.quantidadeContribuida").value(7))
				.andExpect(jsonPath("$.quantidadePendente").value(3))
				.andExpect(jsonPath("$.percentualCoberto").value(70));
		mvc.perform(get(base(evento) + "/" + quase.getId()))
				.andExpect(jsonPath("$.percentualCoberto").value(99))
				.andExpect(jsonPath("$.quantidadePendente").value(1));
		mvc.perform(get(base(evento) + "/" + completo.getId()))
				.andExpect(jsonPath("$.percentualCoberto").value(100))
				.andExpect(jsonPath("$.quantidadePendente").value(0));
		mvc.perform(get(base(evento)))
				.andExpect(jsonPath("$[0].quantidadeContribuida").value(7))
				.andExpect(jsonPath("$[1].quantidadeContribuida").value(199))
				.andExpect(jsonPath("$[2].quantidadeContribuida").value(5));
	}

	@Test
	void itemDeOutroEventoDa404() throws Exception {
		Evento dono = salvarEvento(StatusEvento.RASCUNHO);
		Evento outro = salvarEvento(StatusEvento.RASCUNHO);
		ItemEvento item = salvarItem(dono, salvarCategoria(dono, "Bebidas", UnidadeMedida.UNIDADE), "Suco", 5);

		mvc.perform(get(base(outro) + "/" + item.getId())).andExpect(status().isNotFound());
		atualizar(outro, item, corpoAtualizar("X", 5, false, "[]", "[]")).andExpect(status().isNotFound());
		mvc.perform(delete(base(outro) + "/" + item.getId())).andExpect(status().isNotFound());
	}

	@Test
	void editaNomeQuantidadeObrigatoriedadeEListas() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Doces", UnidadeMedida.UNIDADE);
		criar(evento, corpoCriar("Brigadeiro", categoria.getId(), 40, false, "[\"LACTOSE\"]", "[]"));
		ItemEvento item = em.createQuery("select i from ItemEvento i", ItemEvento.class).getSingleResult();

		atualizar(evento, item, corpoAtualizar("Beijinho", 60, true, "[\"OVO\"]", "[\"ACUCAR_ADICIONADO\"]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Beijinho"))
				.andExpect(jsonPath("$.quantidadeNecessaria").value(60))
				.andExpect(jsonPath("$.obrigatorio").value(true))
				.andExpect(jsonPath("$.alergenos", contains("OVO")))
				.andExpect(jsonPath("$.caracteristicas", contains("ACUCAR_ADICIONADO")));
		em.flush();
		em.clear();
		mvc.perform(get(base(evento) + "/" + item.getId()))
				.andExpect(jsonPath("$.alergenos", contains("OVO")));
	}

	@Test
	void itemNaoMudaDeCategoria() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento original = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		CategoriaEvento outra = salvarCategoria(evento, "Doces", UnidadeMedida.UNIDADE);
		ItemEvento item = salvarItem(evento, original, "Suco", 5);

		atualizar(evento, item, "{\"nome\":\"Suco\",\"categoriaId\":" + outra.getId() + ",\"quantidadeNecessaria\":5}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoriaId").value(original.getId()));
	}

	@Test
	void itemPodeMudarSoACaixaDoProprioNome() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		ItemEvento item = salvarItem(evento, salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE), "Suco", 5);

		atualizar(evento, item, corpoAtualizar("suco", 5, false, "[]", "[]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("suco"));
	}

	@Test
	void renomearParaONomeDeOutroItemDaMesmaCategoriaDa409() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		salvarItem(evento, categoria, "Suco", 5);
		ItemEvento agua = salvarItem(evento, categoria, "Água", 5);

		atualizar(evento, agua, corpoAtualizar("SUCO", 5, false, "[]", "[]")).andExpect(status().isConflict());
	}

	@Test
	void naoReduzAQuantidadeAbaixoDoQueJaFoiContribuido() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		ItemEvento item = salvarItem(evento, salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE), "Suco", 10);
		contribuir(salvarParticipante(evento, "Ana", "52998224725"), item, 4);
		contribuir(salvarParticipante(evento, "Beto", "39053344705"), item, 3);

		atualizar(evento, item, corpoAtualizar("Suco", 6, false, "[]", "[]"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(
						"A quantidade necessária não pode ficar abaixo do que já foi contribuído (7)."));
		atualizar(evento, item, corpoAtualizar("Suco", 7, false, "[]", "[]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.percentualCoberto").value(100));
		atualizar(evento, item, corpoAtualizar("Suco", 20, false, "[]", "[]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidadePendente").value(13));
	}

	@Test
	void naoEditaItemDeEventoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		ItemEvento item = salvarItem(evento, salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE), "Suco", 5);
		evento.setStatus(StatusEvento.ENCERRADO);
		eventos.saveAndFlush(evento);

		atualizar(evento, item, corpoAtualizar("Outro", 5, false, "[]", "[]")).andExpect(status().isUnprocessableEntity());
		mvc.perform(delete(base(evento) + "/" + item.getId())).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void excluiItemSemContribuicoes() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		ItemEvento item = salvarItem(evento, salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE), "Suco", 5);

		mvc.perform(delete(base(evento) + "/" + item.getId())).andExpect(status().isNoContent());
		mvc.perform(get(base(evento) + "/" + item.getId())).andExpect(status().isNotFound());
	}

	@Test
	void naoExcluiItemQueJaTemContribuicoes() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		ItemEvento item = salvarItem(evento, salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE), "Suco", 5);
		contribuir(salvarParticipante(evento, "Ana", "52998224725"), item, 1);

		mvc.perform(delete(base(evento) + "/" + item.getId()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível excluir um item que já tem contribuições."));
		mvc.perform(get(base(evento) + "/" + item.getId())).andExpect(status().isOk());
	}

	private String base(Evento evento) {
		return "/api/eventos/" + evento.getId() + "/itens";
	}

	private ResultActions criar(Evento evento, String corpo) throws Exception {
		return mvc.perform(post(base(evento)).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private ResultActions atualizar(Evento evento, ItemEvento item, String corpo) throws Exception {
		return mvc.perform(put(base(evento) + "/" + item.getId()).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private String corpoCriar(String nome, Long categoriaId, int quantidade, boolean obrigatorio, String alergenos,
			String caracteristicas) {
		return "{\"nome\":\"" + nome + "\",\"categoriaId\":" + categoriaId + ",\"quantidadeNecessaria\":" + quantidade
				+ ",\"obrigatorio\":" + obrigatorio + ",\"alergenos\":" + alergenos + ",\"caracteristicas\":"
				+ caracteristicas + "}";
	}

	private String corpoAtualizar(String nome, int quantidade, boolean obrigatorio, String alergenos,
			String caracteristicas) {
		return "{\"nome\":\"" + nome + "\",\"quantidadeNecessaria\":" + quantidade + ",\"obrigatorio\":" + obrigatorio
				+ ",\"alergenos\":" + alergenos + ",\"caracteristicas\":" + caracteristicas + "}";
	}

	private Evento salvarEvento(StatusEvento status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Evento evento = new Evento("Evento", null, TipoEvento.ALMOCO, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(status);
		return eventos.saveAndFlush(evento);
	}

	private CategoriaEvento salvarCategoria(Evento evento, String nome, UnidadeMedida unidade) {
		CategoriaEvento categoria = new CategoriaEvento(evento, nome, unidade, 0);
		em.persist(categoria);
		em.flush();
		return categoria;
	}

	private ItemEvento salvarItem(Evento evento, CategoriaEvento categoria, String nome, int quantidade) {
		ItemEvento item = new ItemEvento(evento, categoria, nome, quantidade, false);
		item.getAlergenos().add(Alergeno.GLUTEN);
		em.persist(item);
		em.flush();
		return item;
	}

	private ParticipanteEvento salvarParticipante(Evento evento, String nome, String cpf) {
		Colaborador colaborador = new Colaborador(nome, cpf);
		em.persist(colaborador);
		ParticipanteEvento participante = new ParticipanteEvento(evento, colaborador);
		em.persist(participante);
		em.flush();
		return participante;
	}

	private void contribuir(ParticipanteEvento participante, ItemEvento item, int quantidade) {
		em.persist(new Contribuicao(participante, item, quantidade, null));
		em.flush();
	}

}
