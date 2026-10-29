package br.com.sulwork.eventos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sulwork.eventos.CpfsDeTeste;
import br.com.sulwork.eventos.TestcontainersConfiguration;
import br.com.sulwork.eventos.domain.CategoriaEvento;
import br.com.sulwork.eventos.domain.Colaborador;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.ItemEvento;
import br.com.sulwork.eventos.domain.ParticipanteEvento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.domain.UnidadeMedida;
import br.com.sulwork.eventos.repository.CategoriaEventoRepository;
import br.com.sulwork.eventos.repository.ColaboradorRepository;
import br.com.sulwork.eventos.repository.EventoRepository;
import br.com.sulwork.eventos.repository.ItemEventoRepository;
import br.com.sulwork.eventos.repository.ParticipanteEventoRepository;

/**
 * Sem {@code @Transactional}: cada pedido roda na sua transação, como requisições HTTP de verdade, e os dados de
 * preparação ficam gravados no banco.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
class ContribuicaoConcorrenciaTest {

	private static final int PESSOAS = 20;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private CategoriaEventoRepository categorias;

	@Autowired
	private ItemEventoRepository itens;

	@Autowired
	private ColaboradorRepository colaboradores;

	@Autowired
	private ParticipanteEventoRepository participantes;

	@AfterEach
	void limparOBanco() {
		for (String tabela : List.of("contribuicoes", "participantes_evento", "item_alergenos", "item_caracteristicas",
				"itens_evento", "categorias_evento", "colaborador_restricoes", "colaboradores", "eventos")) {
			jdbc.update("DELETE FROM " + tabela);
		}
	}

	@Test
	void vintePessoasDisputandoUmaUnidadeSoUmaFica() throws Exception {
		ItemEvento item = itemComQuantidade(1);
		List<ParticipanteEvento> pessoas = inscrever(item.getEvento(), PESSOAS);

		List<Integer> respostas = contribuirAoMesmoTempo(item, pessoas, 1);

		assertThat(respostas).filteredOn(s -> s == 201).hasSize(1);
		assertThat(respostas).filteredOn(s -> s == 422).hasSize(PESSOAS - 1);
		assertThat(somaDasContribuicoes(item)).isEqualTo(1);
		assertThat(totalDeContribuicoes(item)).isEqualTo(1);
	}

	@Test
	void vintePessoasDisputandoDezUnidadesAceitaExatamenteDez() throws Exception {
		ItemEvento item = itemComQuantidade(10);
		List<ParticipanteEvento> pessoas = inscrever(item.getEvento(), PESSOAS);

		List<Integer> respostas = contribuirAoMesmoTempo(item, pessoas, 1);

		assertThat(respostas).filteredOn(s -> s == 201).hasSize(10);
		assertThat(respostas).filteredOn(s -> s == 422).hasSize(10);
		assertThat(somaDasContribuicoes(item)).isEqualTo(10);
	}

	@Test
	void contribuicoesMaioresQueOSaldoNuncaEstouramOLimite() throws Exception {
		ItemEvento item = itemComQuantidade(10);
		List<ParticipanteEvento> pessoas = inscrever(item.getEvento(), PESSOAS);

		List<Integer> respostas = contribuirAoMesmoTempo(item, pessoas, 3);

		assertThat(respostas).filteredOn(s -> s == 201).hasSize(3);
		assertThat(somaDasContribuicoes(item)).isEqualTo(9);
	}

	@Test
	void mesmaPessoaClicandoVariasVezesAoMesmoTempoContribuiUmaVezSo() throws Exception {
		ItemEvento item = itemComQuantidade(100);
		ParticipanteEvento ana = inscrever(item.getEvento(), 1).get(0);

		List<Integer> respostas = contribuirAoMesmoTempo(item, java.util.Collections.nCopies(10, ana), 2);

		assertThat(respostas).filteredOn(s -> s == 201).hasSize(1);
		assertThat(respostas).filteredOn(s -> s == 409).hasSize(9);
		assertThat(somaDasContribuicoes(item)).isEqualTo(2);
	}

	private List<Integer> contribuirAoMesmoTempo(ItemEvento item, List<ParticipanteEvento> pessoas, int quantidade)
			throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(pessoas.size());
		CountDownLatch prontos = new CountDownLatch(pessoas.size());
		CountDownLatch largada = new CountDownLatch(1);
		try {
			List<Future<Integer>> futuros = new ArrayList<>();
			for (ParticipanteEvento pessoa : pessoas) {
				futuros.add(pool.submit(() -> {
					prontos.countDown();
					largada.await();
					return mvc.perform(post("/api/eventos/" + item.getEvento().getId() + "/contribuicoes")
									.contentType(MediaType.APPLICATION_JSON)
									.content("{\"participanteId\":" + pessoa.getId() + ",\"itemId\":" + item.getId()
											+ ",\"quantidade\":" + quantidade + "}"))
							.andReturn().getResponse().getStatus();
				}));
			}
			prontos.await();
			largada.countDown();
			List<Integer> respostas = new ArrayList<>();
			for (Future<Integer> futuro : futuros) {
				respostas.add(futuro.get(30, TimeUnit.SECONDS));
			}
			return respostas;
		}
		finally {
			pool.shutdownNow();
		}
	}

	private ItemEvento itemComQuantidade(int quantidade) {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Evento evento = new Evento("Evento", null, TipoEvento.ALMOCO, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(StatusEvento.ABERTO);
		eventos.save(evento);
		CategoriaEvento categoria = categorias.save(new CategoriaEvento(evento, "Bebidas", UnidadeMedida.UNIDADE, 0));
		return itens.save(new ItemEvento(evento, categoria, "Suco", quantidade, false));
	}

	private List<ParticipanteEvento> inscrever(Evento evento, int quantidade) {
		List<ParticipanteEvento> pessoas = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			Colaborador colaborador = colaboradores.save(new Colaborador("Pessoa " + i, CpfsDeTeste.cpfValido(i)));
			pessoas.add(participantes.save(new ParticipanteEvento(evento, colaborador)));
		}
		return pessoas;
	}

	private int somaDasContribuicoes(ItemEvento item) {
		return jdbc.queryForObject("SELECT coalesce(sum(quantidade), 0) FROM contribuicoes WHERE item_id = ?",
				Integer.class, item.getId());
	}

	private int totalDeContribuicoes(ItemEvento item) {
		return jdbc.queryForObject("SELECT count(*) FROM contribuicoes WHERE item_id = ?", Integer.class, item.getId());
	}

}
