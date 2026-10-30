package br.com.sulwork.eventos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.CpfsDeTeste;
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

@Import({ TestcontainersConfiguration.class, RelogioFixoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoberturaApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private EntityManager em;

	private int contadorDeCpf = 0;

	@Test
	void calculaACoberturaDeDuasCategoriasComUnidadesDiferentes() throws Exception {
		Evento evento = evento(StatusEvento.ABERTO);
		CategoriaEvento pratos = categoria(evento, "Pratos", UnidadeMedida.UNIDADE, 0);
		CategoriaEvento bebidas = categoria(evento, "Bebidas", UnidadeMedida.MILILITRO, 5000);
		ItemEvento lasanha = item(evento, pratos, "Lasanha", 6, true);
		item(evento, pratos, "Sobremesa", 4, false);
		ItemEvento suco = item(evento, bebidas, "Suco", 2000, false);
		ParticipanteEvento ana = participante(evento);
		contribuicao(ana, lasanha, 6);
		contribuicao(ana, suco, 1000);
		em.flush();

		mvc.perform(get("/api/eventos/" + evento.getId() + "/cobertura"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prontidao").value("OBRIGATORIOS_GARANTIDOS"))
				.andExpect(jsonPath("$.percentualGeral").value(47)) // (75 + 20) / 2
				.andExpect(jsonPath("$.percentualObrigatorios").value(100))
				.andExpect(jsonPath("$.percentualOpcionais").value(25)) // (0 + 50) / 2
				.andExpect(jsonPath("$.totalItens").value(3))
				.andExpect(jsonPath("$.categorias[0].nome").value("Pratos"))
				.andExpect(jsonPath("$.categorias[0].percentual").value(75)) // (2 x 6) / (2 x 6 + 4)
				.andExpect(jsonPath("$.categorias[0].quantidadeNecessaria").value(10))
				.andExpect(jsonPath("$.categorias[0].quantidadeContribuida").value(6))
				.andExpect(jsonPath("$.categorias[0].metaAtingida").value(true))
				.andExpect(jsonPath("$.categorias[1].nome").value("Bebidas"))
				.andExpect(jsonPath("$.categorias[1].unidade").value("MILILITRO"))
				.andExpect(jsonPath("$.categorias[1].deficitDaMeta").value(3000))
				.andExpect(jsonPath("$.categorias[1].alvoEfetivo").value(5000))
				.andExpect(jsonPath("$.categorias[1].percentual").value(20)) // 1000 / (2000 + 3000)
				.andExpect(jsonPath("$.categorias[1].metaAtingida").value(false));
	}

	@Test
	void eventoComTudoCompletoEMetasAtingidasEhCompleto() throws Exception {
		Evento evento = evento(StatusEvento.ABERTO);
		CategoriaEvento categoria = categoria(evento, "Pratos", UnidadeMedida.UNIDADE, 5);
		ItemEvento item = item(evento, categoria, "Lasanha", 10, true);
		contribuicao(participante(evento), item, 10);
		em.flush();

		mvc.perform(get("/api/eventos/" + evento.getId() + "/cobertura"))
				.andExpect(jsonPath("$.prontidao").value("COMPLETO"))
				.andExpect(jsonPath("$.percentualGeral").value(100));
	}

	@Test
	void eventoSemCategoriasEhSemItensEPercentualGeralNulo() throws Exception {
		Evento evento = evento(StatusEvento.RASCUNHO);
		em.flush();

		mvc.perform(get("/api/eventos/" + evento.getId() + "/cobertura"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prontidao").value("SEM_ITENS"))
				.andExpect(jsonPath("$.percentualGeral").doesNotExist())
				.andExpect(jsonPath("$.percentualObrigatorios").value(100))
				.andExpect(jsonPath("$.totalItens").value(0));
	}

	@Test
	void obrigatoriosAbaixoDeMetadeSaoCriticos() throws Exception {
		Evento evento = evento(StatusEvento.ABERTO);
		CategoriaEvento categoria = categoria(evento, "Pratos", UnidadeMedida.UNIDADE, 0);
		ItemEvento item = item(evento, categoria, "Lasanha", 4, true);
		contribuicao(participante(evento), item, 1);
		em.flush();

		mvc.perform(get("/api/eventos/" + evento.getId() + "/cobertura"))
				.andExpect(jsonPath("$.prontidao").value("CRITICO"))
				.andExpect(jsonPath("$.percentualObrigatorios").value(25));
	}

	@Test
	void umEventoEncerradoAindaMostraACobertura() throws Exception {
		Evento evento = evento(StatusEvento.ENCERRADO);
		em.flush();

		mvc.perform(get("/api/eventos/" + evento.getId() + "/cobertura")).andExpect(status().isOk());
	}

	@Test
	void eventoInexistenteDa404() throws Exception {
		mvc.perform(get("/api/eventos/999999/cobertura")).andExpect(status().isNotFound());
	}

	@Test
	void naoMisturaOsDadosDeOutroEvento() throws Exception {
		Evento evento = evento(StatusEvento.ABERTO);
		Evento outro = evento(StatusEvento.ABERTO);
		CategoriaEvento categoria = categoria(outro, "Dela", UnidadeMedida.UNIDADE, 0);
		item(outro, categoria, "Item", 10, true);
		em.flush();

		mvc.perform(get("/api/eventos/" + evento.getId() + "/cobertura"))
				.andExpect(jsonPath("$.totalItens").value(0))
				.andExpect(jsonPath("$.categorias.length()").value(0));
	}

	private Evento evento(StatusEvento status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Evento evento = new Evento("Evento", null, TipoEvento.ALMOCO, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(status);
		return eventos.saveAndFlush(evento);
	}

	private CategoriaEvento categoria(Evento evento, String nome, UnidadeMedida unidade, int meta) {
		CategoriaEvento categoria = new CategoriaEvento(evento, nome, unidade, meta);
		em.persist(categoria);
		return categoria;
	}

	private ItemEvento item(Evento evento, CategoriaEvento categoria, String nome, int quantidade, boolean obrigatorio) {
		ItemEvento item = new ItemEvento(evento, categoria, nome, quantidade, obrigatorio);
		em.persist(item);
		return item;
	}

	private ParticipanteEvento participante(Evento evento) {
		Colaborador colaborador = new Colaborador("Ana", CpfsDeTeste.cpfValido(contadorDeCpf++));
		em.persist(colaborador);
		ParticipanteEvento participante = new ParticipanteEvento(evento, colaborador);
		em.persist(participante);
		return participante;
	}

	private void contribuicao(ParticipanteEvento participante, ItemEvento item, int quantidade) {
		em.persist(new Contribuicao(participante, item, quantidade, null));
	}

}
