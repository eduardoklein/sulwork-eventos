package br.com.sulwork.eventos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CoberturaPorCategoriaQueryTest {

	@Autowired
	private ItemEventoRepository itens;

	@Autowired
	private EntityManager em;

	private int contadorDeCpf = 0;

	@Test
	void somaMetaItensEContribuicoesSeparandoObrigatoriosEOpcionais() {
		Evento evento = evento();
		CategoriaEvento bebidas = categoria(evento, "Bebidas", UnidadeMedida.MILILITRO, 5000);
		ItemEvento suco = item(evento, bebidas, "Suco", 2000, true);
		ItemEvento refri = item(evento, bebidas, "Refrigerante", 3000, false);
		ParticipanteEvento ana = participante(evento);
		ParticipanteEvento beto = participante(evento);
		contribuicao(ana, suco, 1500);
		contribuicao(beto, suco, 500);
		contribuicao(ana, refri, 1000);
		em.flush();

		List<CoberturaDaCategoria> resultado = itens.coberturaPorCategoria(evento.getId());

		assertThat(resultado).hasSize(1);
		CoberturaDaCategoria c = resultado.get(0);
		assertThat(c.getCategoriaId()).isEqualTo(bebidas.getId());
		assertThat(c.getNome()).isEqualTo("Bebidas");
		assertThat(c.getUnidade()).isEqualTo("MILILITRO");
		assertThat(c.getMetaQuantidade()).isEqualTo(5000);
		assertThat(c.getTotalItens()).isEqualTo(2);
		assertThat(c.getNecessarioObrigatorios()).isEqualTo(2000);
		assertThat(c.getNecessarioOpcionais()).isEqualTo(3000);
		assertThat(c.getContribuidoObrigatorios()).isEqualTo(2000);
		assertThat(c.getContribuidoOpcionais()).isEqualTo(1000);
	}

	@Test
	void itemComVariasContribuicoesNaoMultiplicaAQuantidadeNecessaria() {
		Evento evento = evento();
		CategoriaEvento categoria = categoria(evento, "Doces", UnidadeMedida.UNIDADE, 0);
		ItemEvento brigadeiro = item(evento, categoria, "Brigadeiro", 100, true);
		for (int i = 0; i < 4; i++) {
			contribuicao(participante(evento), brigadeiro, 10);
		}
		em.flush();

		CoberturaDaCategoria c = itens.coberturaPorCategoria(evento.getId()).get(0);

		assertThat(c.getNecessarioObrigatorios()).isEqualTo(100);
		assertThat(c.getContribuidoObrigatorios()).isEqualTo(40);
		assertThat(c.getTotalItens()).isEqualTo(1);
	}

	@Test
	void itemSemContribuicoesContaNoNecessarioEFicaComZeroContribuido() {
		Evento evento = evento();
		CategoriaEvento categoria = categoria(evento, "Salgados", UnidadeMedida.UNIDADE, 0);
		item(evento, categoria, "Coxinha", 50, true);
		em.flush();

		CoberturaDaCategoria c = itens.coberturaPorCategoria(evento.getId()).get(0);

		assertThat(c.getNecessarioObrigatorios()).isEqualTo(50);
		assertThat(c.getContribuidoObrigatorios()).isZero();
		assertThat(c.getNecessarioOpcionais()).isZero();
		assertThat(c.getContribuidoOpcionais()).isZero();
	}

	@Test
	void categoriaSemItensApareceComZeros() {
		Evento evento = evento();
		categoria(evento, "Vazia", UnidadeMedida.UNIDADE, 20);
		em.flush();

		CoberturaDaCategoria c = itens.coberturaPorCategoria(evento.getId()).get(0);

		assertThat(c.getNome()).isEqualTo("Vazia");
		assertThat(c.getMetaQuantidade()).isEqualTo(20);
		assertThat(c.getTotalItens()).isZero();
		assertThat(c.getNecessarioObrigatorios()).isZero();
		assertThat(c.getContribuidoOpcionais()).isZero();
	}

	@Test
	void somaCadaCategoriaSeparadamenteNaOrdemDeCriacao() {
		Evento evento = evento();
		CategoriaEvento primeira = categoria(evento, "Zebra", UnidadeMedida.UNIDADE, 0);
		CategoriaEvento segunda = categoria(evento, "Abacate", UnidadeMedida.GRAMA, 0);
		ItemEvento a = item(evento, primeira, "A", 10, true);
		ItemEvento b = item(evento, segunda, "B", 300, true);
		contribuicao(participante(evento), a, 10);
		contribuicao(participante(evento), b, 100);
		em.flush();

		List<CoberturaDaCategoria> resultado = itens.coberturaPorCategoria(evento.getId());

		assertThat(resultado).extracting(CoberturaDaCategoria::getNome).containsExactly("Zebra", "Abacate");
		assertThat(resultado).extracting(CoberturaDaCategoria::getContribuidoObrigatorios).containsExactly(10L, 100L);
		assertThat(resultado).extracting(CoberturaDaCategoria::getNecessarioObrigatorios).containsExactly(10L, 300L);
	}

	@Test
	void naoMisturaCategoriasDeOutroEvento() {
		Evento evento = evento();
		Evento outro = evento();
		categoria(evento, "Minha", UnidadeMedida.UNIDADE, 0);
		CategoriaEvento dela = categoria(outro, "Dela", UnidadeMedida.UNIDADE, 0);
		item(outro, dela, "Item", 99, true);
		em.flush();

		List<CoberturaDaCategoria> resultado = itens.coberturaPorCategoria(evento.getId());

		assertThat(resultado).extracting(CoberturaDaCategoria::getNome).containsExactly("Minha");
		assertThat(resultado.get(0).getNecessarioObrigatorios()).isZero();
	}

	@Test
	void eventoSemCategoriasDevolveListaVazia() {
		Evento evento = evento();
		em.flush();

		assertThat(itens.coberturaPorCategoria(evento.getId())).isEmpty();
		assertThat(itens.coberturaPorCategoria(999_999L)).isEmpty();
	}

	private Evento evento() {
		LocalDateTime data = LocalDateTime.parse("2026-12-01T12:00:00");
		Evento evento = new Evento("Evento", null, TipoEvento.ALMOCO, data, data.minusDays(1),
				ZoneId.of("America/Sao_Paulo"));
		evento.setStatus(StatusEvento.ABERTO);
		em.persist(evento);
		return evento;
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
		int numero = contadorDeCpf++;
		Colaborador colaborador = new Colaborador("Pessoa " + numero, CpfsDeTeste.cpfValido(numero));
		em.persist(colaborador);
		ParticipanteEvento participante = new ParticipanteEvento(evento, colaborador);
		em.persist(participante);
		return participante;
	}

	private void contribuicao(ParticipanteEvento participante, ItemEvento item, int quantidade) {
		em.persist(new Contribuicao(participante, item, quantidade, null));
	}

}
