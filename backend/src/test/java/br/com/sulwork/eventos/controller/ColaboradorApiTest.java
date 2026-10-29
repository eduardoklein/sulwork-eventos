package br.com.sulwork.eventos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.TestcontainersConfiguration;
import br.com.sulwork.eventos.domain.Colaborador;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.ParticipanteEvento;
import br.com.sulwork.eventos.domain.RestricaoAlimentar;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.repository.ColaboradorRepository;
import br.com.sulwork.eventos.repository.EventoRepository;
import jakarta.persistence.EntityManager;

@Import({ TestcontainersConfiguration.class, RelogioFixoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ColaboradorApiTest {

	private static final String CPF_ANA = "39053344705";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ColaboradorRepository colaboradores;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private EntityManager em;

	@Test
	void buscaPorCpfExatoComOuSemMascaraEMostraOCpfMascarado() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA, RestricaoAlimentar.VEGANO);

		mvc.perform(get("/api/colaboradores").param("cpf", CPF_ANA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ana.getId()))
				.andExpect(jsonPath("$.nome").value("Ana"))
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"))
				.andExpect(jsonPath("$.restricoes", contains("VEGANO")));
		mvc.perform(get("/api/colaboradores").param("cpf", "390.533.447-05"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ana.getId()));
	}

	@Test
	void cpfSemCadastroDa404() throws Exception {
		mvc.perform(get("/api/colaboradores").param("cpf", CPF_ANA))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Colaborador não encontrado."));
	}

	@Test
	void cpfInvalidoNaBuscaDa422() throws Exception {
		mvc.perform(get("/api/colaboradores").param("cpf", "111.111.111-11"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void naoExisteListagemGeralDoCadastro() throws Exception {
		salvar("Ana", CPF_ANA);

		mvc.perform(get("/api/colaboradores")).andExpect(status().is4xxClientError());
	}

	@Test
	void buscaPorId() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA);

		mvc.perform(get("/api/colaboradores/" + ana.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana"))
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"));
		mvc.perform(get("/api/colaboradores/999999")).andExpect(status().isNotFound());
	}

	@Test
	void editaNomeESubstituiAsRestricoes() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA, RestricaoAlimentar.VEGANO);

		atualizar(ana.getId(), "{\"nome\":\"Ana Souza\",\"restricoes\":[\"ALERGIA_OVO\",\"CELIACO_SEM_GLUTEN\"]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$.restricoes", contains("CELIACO_SEM_GLUTEN", "ALERGIA_OVO")));

		em.flush();
		em.clear();
		Colaborador lida = colaboradores.findById(ana.getId()).orElseThrow();
		assertThat(lida.getNome()).isEqualTo("Ana Souza");
		assertThat(lida.getRestricoes()).containsExactlyInAnyOrder(RestricaoAlimentar.ALERGIA_OVO,
				RestricaoAlimentar.CELIACO_SEM_GLUTEN);
	}

	@Test
	void listaVaziaLimpaAsRestricoes() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA, RestricaoAlimentar.VEGANO);

		atualizar(ana.getId(), "{\"nome\":\"Ana\",\"restricoes\":[]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.restricoes", hasSize(0)));
	}

	@Test
	void restricoesOmitidasDao400ENaoApagamNada() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA, RestricaoAlimentar.VEGANO);

		atualizar(ana.getId(), "{\"nome\":\"Outro nome\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("restricoes"));

		em.flush();
		em.clear();
		Colaborador lida = colaboradores.findById(ana.getId()).orElseThrow();
		assertThat(lida.getNome()).isEqualTo("Ana");
		assertThat(lida.getRestricoes()).containsExactly(RestricaoAlimentar.VEGANO);
	}

	@Test
	void nomeEmBrancoDa400() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA);

		atualizar(ana.getId(), "{\"nome\":\" \",\"restricoes\":[]}").andExpect(status().isBadRequest());
	}

	@Test
	void ocpfNaoMudaPelaEdicao() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA);

		atualizar(ana.getId(), "{\"nome\":\"Ana\",\"cpf\":\"52998224725\",\"restricoes\":[]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cpf").value("***.533.447-**"));
		assertThat(colaboradores.findById(ana.getId()).orElseThrow().getCpf()).isEqualTo(CPF_ANA);
	}

	@Test
	void editarPessoaInexistenteDa404() throws Exception {
		atualizar(999999L, "{\"nome\":\"Ana\",\"restricoes\":[]}").andExpect(status().isNotFound());
	}

	@Test
	void aEdicaoAparecePraTodosOsEventosEMesmoEmEventoEncerrado() throws Exception {
		Colaborador ana = salvar("Ana", CPF_ANA);
		Evento encerrado = salvarEvento(StatusEvento.ENCERRADO);
		ParticipanteEvento participante = new ParticipanteEvento(encerrado, ana);
		em.persist(participante);
		em.flush();

		atualizar(ana.getId(), "{\"nome\":\"Ana Souza\",\"restricoes\":[\"VEGANO\"]}").andExpect(status().isOk());
		em.flush();
		em.clear();

		mvc.perform(get("/api/eventos/" + encerrado.getId() + "/participantes"))
				.andExpect(jsonPath("$[0].nome").value("Ana Souza"))
				.andExpect(jsonPath("$[0].restricoes", contains("VEGANO")));
	}

	private ResultActions atualizar(Long id, String corpo) throws Exception {
		return mvc.perform(put("/api/colaboradores/" + id).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	private Colaborador salvar(String nome, String cpf, RestricaoAlimentar... restricoes) {
		Colaborador colaborador = new Colaborador(nome, cpf);
		colaborador.getRestricoes().addAll(java.util.List.of(restricoes));
		return colaboradores.saveAndFlush(colaborador);
	}

	private Evento salvarEvento(StatusEvento status) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Evento evento = new Evento("Evento", null, TipoEvento.ALMOCO, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(status);
		return eventos.saveAndFlush(evento);
	}

}
