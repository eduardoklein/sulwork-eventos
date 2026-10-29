package br.com.sulwork.eventos.controller;

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

import br.com.sulwork.eventos.TestcontainersConfiguration;
import br.com.sulwork.eventos.domain.CategoriaEvento;
import br.com.sulwork.eventos.domain.Colaborador;
import br.com.sulwork.eventos.domain.Contribuicao;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.ItemEvento;
import br.com.sulwork.eventos.domain.ParticipanteEvento;
import br.com.sulwork.eventos.domain.RestricaoAlimentar;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.domain.UnidadeMedida;
import br.com.sulwork.eventos.repository.ColaboradorRepository;
import br.com.sulwork.eventos.repository.ContribuicaoRepository;
import br.com.sulwork.eventos.repository.EventoRepository;
import jakarta.persistence.EntityManager;

/** O relógio fica em 12:00 de São Paulo, 1º de novembro de 2026. */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ParticipanteApiTest {

	private static final String CPF_ANA = "39053344705";
	private static final String CPF_ANA_COM_MASCARA = "390.533.447-05";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private ColaboradorRepository colaboradores;

	@Autowired
	private ContribuicaoRepository contribuicoes;

	@Autowired
	private EntityManager em;

	@Test
	void inscrevePessoaNovaEMostraOCpfMascarado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);

		inscrever(evento, "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\",\"restricoes\":[\"VEGANO\",\"ALERGIA_SOJA\"]}")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/eventos/" + evento.getId() + "/participantes/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.colaboradorId").isNumber())
				.andExpect(jsonPath("$.nome").value("Ana"))
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"))
				.andExpect(jsonPath("$.restricoes", contains("ALERGIA_SOJA", "VEGANO")))
				.andExpect(jsonPath("$.inscritoEm").isNotEmpty());
	}

	@Test
	void guardaOCpfSoComOsOnzeDigitosMesmoQuandoVemComMascara() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);

		inscrever(evento, "{\"cpf\":\"" + CPF_ANA_COM_MASCARA + "\",\"nome\":\"Ana\"}").andExpect(status().isCreated());

		assertThat(colaboradores.findByCpf(CPF_ANA)).isPresent();
	}

	@Test
	void restricoesOmitidasViramListaVaziaParaPessoaNova() throws Exception {
		inscrever(salvarEvento(StatusEvento.ABERTO), "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.restricoes", hasSize(0)));
	}

	@Test
	void cpfInvalidoDa422() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);

		inscrever(evento, "{\"cpf\":\"111.111.111-11\",\"nome\":\"Ana\"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("CPF inválido."));
		inscrever(evento, "{\"cpf\":\"39053344706\",\"nome\":\"Ana\"}").andExpect(status().isUnprocessableEntity());
		inscrever(evento, "{\"cpf\":\"abc\",\"nome\":\"Ana\"}").andExpect(status().isUnprocessableEntity());
	}

	@Test
	void cpfAusenteOuEmBrancoDa400() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);

		inscrever(evento, "{\"nome\":\"Ana\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("cpf"));
		inscrever(evento, "{\"cpf\":\" \",\"nome\":\"Ana\"}").andExpect(status().isBadRequest());
	}

	@Test
	void cpfNovoSemNomeDa422() throws Exception {
		inscrever(salvarEvento(StatusEvento.ABERTO), "{\"cpf\":\"" + CPF_ANA + "\"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Informe o nome para um CPF novo."));
		assertThat(colaboradores.findByCpf(CPF_ANA)).isEmpty();
	}

	@Test
	void restricaoForaDaListaDa400() throws Exception {
		inscrever(salvarEvento(StatusEvento.ABERTO),
				"{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\",\"restricoes\":[\"COME_PEDRA\"]}")
				.andExpect(status().isBadRequest());
	}

	@Test
	void amesmaPessoaEmDoisEventosUsaOMesmoCadastro() throws Exception {
		Evento primeiro = salvarEvento(StatusEvento.ABERTO);
		Evento segundo = salvarEvento(StatusEvento.ABERTO);

		inscrever(primeiro, "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\",\"restricoes\":[\"VEGANO\"]}")
				.andExpect(status().isCreated());
		inscrever(segundo, "{\"cpf\":\"" + CPF_ANA + "\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Ana"))
				.andExpect(jsonPath("$.restricoes", contains("VEGANO")));

		assertThat(colaboradores.count()).isEqualTo(1);
	}

	@Test
	void inscreverPessoaCadastradaNaoAlteraONomeNemAsRestricoes() throws Exception {
		Evento primeiro = salvarEvento(StatusEvento.ABERTO);
		Evento segundo = salvarEvento(StatusEvento.ABERTO);
		inscrever(primeiro, "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\",\"restricoes\":[\"VEGANO\"]}");

		inscrever(segundo, "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Nome Errado\",\"restricoes\":[]}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Ana"))
				.andExpect(jsonPath("$.restricoes", contains("VEGANO")));

		em.flush();
		em.clear();
		Colaborador ana = colaboradores.findByCpf(CPF_ANA).orElseThrow();
		assertThat(ana.getNome()).isEqualTo("Ana");
		assertThat(ana.getRestricoes()).containsExactly(RestricaoAlimentar.VEGANO);
	}

	@Test
	void amesmaPessoaDuasVezesNoMesmoEventoDa409() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		inscrever(evento, "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\"}").andExpect(status().isCreated());

		inscrever(evento, "{\"cpf\":\"" + CPF_ANA + "\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Esta pessoa já está inscrita neste evento."));
		inscrever(evento, "{\"cpf\":\"" + CPF_ANA_COM_MASCARA + "\"}").andExpect(status().isConflict());
	}

	@Test
	void soEventoAbertoAceitaInscricoes() throws Exception {
		for (StatusEvento status : new StatusEvento[] { StatusEvento.RASCUNHO, StatusEvento.ENCERRADO,
				StatusEvento.REALIZADO, StatusEvento.CANCELADO }) {
			inscrever(salvarEvento(status), "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\"}")
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.detail").value("Só um evento aberto aceita mudanças nos participantes. "
							+ "Este evento está " + status.getDescricao().toLowerCase() + "."));
		}
		assertThat(colaboradores.count()).isZero();
	}

	@Test
	void eventoAbertoComPrazoVencidoJaNaoAceitaInscricoes() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		evento.setPrazoAlteracoes(LocalDateTime.parse("2026-10-30T12:00:00"));
		eventos.saveAndFlush(evento);

		inscrever(evento, "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\"}").andExpect(status().isUnprocessableEntity());
	}

	@Test
	void eventoInexistenteDa404() throws Exception {
		mvc.perform(post("/api/eventos/999999/participantes").contentType(MediaType.APPLICATION_JSON)
						.content("{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Ana\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/eventos/999999/participantes")).andExpect(status().isNotFound());
	}

	@Test
	void listaNaOrdemDeInscricaoInclusiveEmEventoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		inscrever(evento, "{\"cpf\":\"" + CPF_ANA + "\",\"nome\":\"Zélia\"}");
		inscrever(evento, "{\"cpf\":\"52998224725\",\"nome\":\"Abel\"}");
		evento.setStatus(StatusEvento.ENCERRADO);
		eventos.saveAndFlush(evento);

		mvc.perform(get(base(evento)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].nome", contains("Zélia", "Abel")));
	}

	@Test
	void buscaUmaInscricaoEnaoAchaADeOutroEvento() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		Evento outro = salvarEvento(StatusEvento.ABERTO);
		ParticipanteEvento participante = salvarParticipante(evento, "Ana", CPF_ANA);

		mvc.perform(get(base(evento) + "/" + participante.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana"));
		mvc.perform(get(base(outro) + "/" + participante.getId())).andExpect(status().isNotFound());
	}

	@Test
	void removerInscricaoApagaAsContribuicoesDaPessoaNoEventoEMantemOCadastro() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		CategoriaEvento categoria = new CategoriaEvento(evento, "Bebidas", UnidadeMedida.UNIDADE, 0);
		em.persist(categoria);
		ItemEvento item = new ItemEvento(evento, categoria, "Suco", 10, false);
		em.persist(item);
		ParticipanteEvento ana = salvarParticipante(evento, "Ana", CPF_ANA);
		ParticipanteEvento beto = salvarParticipante(evento, "Beto", "52998224725");
		em.persist(new Contribuicao(ana, item, 4, null));
		em.persist(new Contribuicao(beto, item, 3, null));
		em.flush();
		em.clear();

		mvc.perform(delete(base(evento) + "/" + ana.getId())).andExpect(status().isNoContent());
		em.flush();
		em.clear();

		assertThat(contribuicoes.somarPorItem(item.getId())).isEqualTo(3);
		assertThat(colaboradores.findByCpf(CPF_ANA)).isPresent();
		mvc.perform(get(base(evento))).andExpect(jsonPath("$[*].nome", contains("Beto")));
	}

	@Test
	void naoRemoveInscricaoDeEventoEncerrado() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		ParticipanteEvento ana = salvarParticipante(evento, "Ana", CPF_ANA);
		evento.setStatus(StatusEvento.ENCERRADO);
		eventos.saveAndFlush(evento);

		mvc.perform(delete(base(evento) + "/" + ana.getId())).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void removerInscricaoInexistenteDa404() throws Exception {
		mvc.perform(delete(base(salvarEvento(StatusEvento.ABERTO)) + "/999999")).andExpect(status().isNotFound());
	}

	@Test
	void depoisDeRemovidaAPessoaPodeSeInscreverDeNovo() throws Exception {
		Evento evento = salvarEvento(StatusEvento.ABERTO);
		ParticipanteEvento ana = salvarParticipante(evento, "Ana", CPF_ANA);
		mvc.perform(delete(base(evento) + "/" + ana.getId())).andExpect(status().isNoContent());

		inscrever(evento, "{\"cpf\":\"" + CPF_ANA + "\"}").andExpect(status().isCreated());
	}

	private String base(Evento evento) {
		return "/api/eventos/" + evento.getId() + "/participantes";
	}

	private ResultActions inscrever(Evento evento, String corpo) throws Exception {
		return mvc.perform(post(base(evento)).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private Evento salvarEvento(StatusEvento status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Evento evento = new Evento("Evento", null, TipoEvento.ALMOCO, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(status);
		return eventos.saveAndFlush(evento);
	}

	private ParticipanteEvento salvarParticipante(Evento evento, String nome, String cpf) {
		Colaborador colaborador = colaboradores.findByCpf(cpf).orElseGet(() -> colaboradores.saveAndFlush(new Colaborador(nome, cpf)));
		ParticipanteEvento participante = new ParticipanteEvento(evento, colaborador);
		em.persist(participante);
		em.flush();
		return participante;
	}

}
