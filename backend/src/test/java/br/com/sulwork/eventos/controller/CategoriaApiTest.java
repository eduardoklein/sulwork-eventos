package br.com.sulwork.eventos.controller;

import static org.hamcrest.Matchers.contains;
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
class CategoriaApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private EntityManager em;

	@Test
	void criaCategoriaComTotaisZerados() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);

		criar(evento, "Bebidas", "MILILITRO", 5000)
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/eventos/" + evento.getId() + "/categorias/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nome").value("Bebidas"))
				.andExpect(jsonPath("$.unidade").value("MILILITRO"))
				.andExpect(jsonPath("$.metaQuantidade").value(5000))
				.andExpect(jsonPath("$.totalItens").value(0))
				.andExpect(jsonPath("$.quantidadePlanejada").value(0))
				.andExpect(jsonPath("$.quantidadeContribuida").value(0));
	}

	@Test
	void criaCategoriaTambemEmEventoAberto() throws Exception {
		criar(salvarEvento(StatusEvento.ABERTO), "Salgados", "UNIDADE", 40).andExpect(status().isCreated());
	}

	@Test
	void nomeRepetidoNoMesmoEventoDa409SemDiferenciarMaiusculasNemEspacos() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		criar(evento, "Bebidas", "MILILITRO", 5000).andExpect(status().isCreated());

		criar(evento, "Bebidas", "MILILITRO", 1)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe uma categoria com esse nome neste evento."));
		criar(evento, "bebidas", "UNIDADE", 1).andExpect(status().isConflict());
		criar(evento, "  BEBIDAS  ", "UNIDADE", 1).andExpect(status().isConflict());
	}

	@Test
	void mesmoNomeEmOutroEventoEhAceito() throws Exception {
		criar(salvarEvento(StatusEvento.RASCUNHO), "Bebidas", "MILILITRO", 1).andExpect(status().isCreated());
		criar(salvarEvento(StatusEvento.RASCUNHO), "Bebidas", "MILILITRO", 1).andExpect(status().isCreated());
	}

	@Test
	void metaZeroEhAceitaMasNegativaDa400() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);

		criar(evento, "Sem meta", "UNIDADE", 0).andExpect(status().isCreated());
		criar(evento, "Meta negativa", "UNIDADE", -1)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("metaQuantidade"));
	}

	@Test
	void camposInvalidosDao400() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);

		mvc.perform(post(base(evento)).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("não deve estar em branco"))
				.andExpect(jsonPath("$.erros[?(@.campo=='unidade')].mensagem").value("não deve ser nulo"))
				.andExpect(jsonPath("$.erros[?(@.campo=='metaQuantidade')].mensagem").value("não deve ser nulo"));
		criar(evento, "Bebidas", "LITRO", 1).andExpect(status().isBadRequest());
	}

	@Test
	void eventoInexistenteDa404() throws Exception {
		mvc.perform(post("/api/eventos/999999/categorias").contentType(MediaType.APPLICATION_JSON)
						.content(corpo("Bebidas", "UNIDADE", 1)))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/eventos/999999/categorias")).andExpect(status().isNotFound());
	}

	@Test
	void cardapioSoMudaEmRascunhoEAberto() throws Exception {
		for (StatusEvento status : new StatusEvento[] { StatusEvento.ENCERRADO, StatusEvento.REALIZADO,
				StatusEvento.CANCELADO }) {
			Evento evento = salvarEvento(status);

			criar(evento, "Bebidas", "UNIDADE", 1)
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value(
							"Não é possível alterar o cardápio de um evento " + status.getDescricao().toLowerCase() + "."));
		}
	}

	@Test
	void eventoAbertoComPrazoVencidoJaContaComoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		evento.setPrazoAlteracoes(LocalDateTime.parse("2026-10-30T12:00:00"));
		eventos.saveAndFlush(evento);

		criar(evento, "Bebidas", "UNIDADE", 1)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível alterar o cardápio de um evento encerrado."));
	}

	@Test
	void listaNaOrdemDeCriacaoInclusiveEmEventoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		criar(evento, "Zebra", "UNIDADE", 1);
		criar(evento, "Abacate", "UNIDADE", 1);
		evento.setStatus(StatusEvento.ENCERRADO);
		eventos.saveAndFlush(evento);

		mvc.perform(get(base(evento)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].nome", contains("Zebra", "Abacate")));
	}

	@Test
	void totaisSomamItensEContribuicoesSoDaPropriaCategoria() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		CategoriaEvento bebidas = salvarCategoria(evento, "Bebidas", UnidadeMedida.MILILITRO);
		CategoriaEvento vazia = salvarCategoria(evento, "Doces", UnidadeMedida.UNIDADE);
		ItemEvento suco = salvarItem(evento, bebidas, "Suco", 10);
		ItemEvento agua = salvarItem(evento, bebidas, "Água", 5);
		ParticipanteEvento ana = salvarParticipante(evento, "Ana", "52998224725");
		ParticipanteEvento beto = salvarParticipante(evento, "Beto", "39053344705");
		em.persist(new Contribuicao(ana, suco, 4, null));
		em.persist(new Contribuicao(beto, suco, 3, null));
		em.persist(new Contribuicao(ana, agua, 2, null));
		em.flush();
		em.clear();

		mvc.perform(get(base(evento) + "/" + bebidas.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItens").value(2))
				.andExpect(jsonPath("$.quantidadePlanejada").value(15))
				.andExpect(jsonPath("$.quantidadeContribuida").value(9));
		mvc.perform(get(base(evento) + "/" + vazia.getId()))
				.andExpect(jsonPath("$.totalItens").value(0))
				.andExpect(jsonPath("$.quantidadePlanejada").value(0))
				.andExpect(jsonPath("$.quantidadeContribuida").value(0));
	}

	@Test
	void categoriaDeOutroEventoDa404() throws Exception {
		Evento dono = salvarEvento(StatusEvento.RASCUNHO);
		Evento outro = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(dono, "Bebidas", UnidadeMedida.UNIDADE);

		mvc.perform(get(base(outro) + "/" + categoria.getId())).andExpect(status().isNotFound());
		atualizar(outro, categoria, "X", "UNIDADE", 1).andExpect(status().isNotFound());
		mvc.perform(delete(base(outro) + "/" + categoria.getId())).andExpect(status().isNotFound());
		mvc.perform(get(base(dono) + "/" + categoria.getId())).andExpect(status().isOk());
	}

	@Test
	void editaNomeEMeta() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.MILILITRO);

		atualizar(evento, categoria, "Bebidas geladas", "MILILITRO", 8000)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Bebidas geladas"))
				.andExpect(jsonPath("$.metaQuantidade").value(8000));
	}

	@Test
	void categoriaPodeMudarSoACaixaDoProprioNome() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.MILILITRO);

		atualizar(evento, categoria, "bebidas", "MILILITRO", 1)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("bebidas"));
	}

	@Test
	void renomearParaONomeDeOutraCategoriaDa409() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		salvarCategoria(evento, "Bebidas", UnidadeMedida.MILILITRO);
		CategoriaEvento doces = salvarCategoria(evento, "Doces", UnidadeMedida.UNIDADE);

		atualizar(evento, doces, "BEBIDAS", "UNIDADE", 1).andExpect(status().isConflict());
	}

	@Test
	void unidadeMudaLivrementeEmCategoriaVazia() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.MILILITRO);

		atualizar(evento, categoria, "Bebidas", "UNIDADE", 10)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unidade").value("UNIDADE"));
	}

	@Test
	void unidadeFicaTravadaEnquantoACategoriaTemItens() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Farinhas", UnidadeMedida.GRAMA);
		salvarItem(evento, categoria, "Farinha de trigo", 500);

		atualizar(evento, categoria, "Farinhas", "MILILITRO", 1)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("A unidade não pode mudar enquanto a categoria tem itens."));
		atualizar(evento, categoria, "Farinhas e grãos", "GRAMA", 2000)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Farinhas e grãos"))
				.andExpect(jsonPath("$.unidade").value("GRAMA"));
	}

	@Test
	void naoEditaCategoriaDeEventoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		evento.setStatus(StatusEvento.ENCERRADO);
		eventos.saveAndFlush(evento);

		atualizar(evento, categoria, "Outro", "UNIDADE", 1).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void excluiCategoriaVazia() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);

		mvc.perform(delete(base(evento) + "/" + categoria.getId())).andExpect(status().isNoContent());
		mvc.perform(get(base(evento) + "/" + categoria.getId())).andExpect(status().isNotFound());
	}

	@Test
	void naoExcluiCategoriaQueAindaTemItens() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		salvarItem(evento, categoria, "Suco", 10);

		mvc.perform(delete(base(evento) + "/" + categoria.getId()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Não é possível excluir uma categoria que ainda tem itens."));
	}

	@Test
	void naoExcluiCategoriaDeEventoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.RASCUNHO);
		CategoriaEvento categoria = salvarCategoria(evento, "Bebidas", UnidadeMedida.UNIDADE);
		evento.setStatus(StatusEvento.ENCERRADO);
		eventos.saveAndFlush(evento);

		mvc.perform(delete(base(evento) + "/" + categoria.getId())).andExpect(status().isUnprocessableEntity());
	}

	private String base(Evento evento) {
		return "/api/eventos/" + evento.getId() + "/categorias";
	}

	private ResultActions criar(Evento evento, String nome, String unidade, int meta) throws Exception {
		return mvc.perform(post(base(evento)).contentType(MediaType.APPLICATION_JSON).content(corpo(nome, unidade, meta)));
	}

	private ResultActions atualizar(Evento evento, CategoriaEvento categoria, String nome, String unidade, int meta)
			throws Exception {
		return mvc.perform(put(base(evento) + "/" + categoria.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(nome, unidade, meta)));
	}

	private String corpo(String nome, String unidade, int meta) {
		return "{\"nome\":\"" + nome + "\",\"unidade\":\"" + unidade + "\",\"metaQuantidade\":" + meta + "}";
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

}
