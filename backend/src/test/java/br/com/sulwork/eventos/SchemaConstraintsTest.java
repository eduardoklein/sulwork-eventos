package br.com.sulwork.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SchemaConstraintsTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void mesmoColaboradorEmEventosDiferentesEhAceito() {
		long colaborador = inserirColaborador("52998224725");

		assertThatCode(() -> {
			inserirParticipante(inserirEvento(), colaborador);
			inserirParticipante(inserirEvento(), colaborador);
		}).doesNotThrowAnyException();
	}

	@Test
	void mesmoColaboradorDuasVezesNoMesmoEventoEhRejeitado() {
		long evento = inserirEvento();
		long colaborador = inserirColaborador("52998224725");
		inserirParticipante(evento, colaborador);

		assertThatThrownBy(() -> inserirParticipante(evento, colaborador))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cpfDuplicadoEhRejeitado() {
		inserirColaborador("52998224725");

		assertThatThrownBy(() -> inserirColaborador("52998224725"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cpfComMascaraEhRejeitado() {
		assertThatThrownBy(() -> inserirColaborador("529.982.247-25"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cpfComMenosDeOnzeDigitosEhRejeitado() {
		assertThatThrownBy(() -> inserirColaborador("5299822472"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void statusForaDaListaEhRejeitado() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO eventos (nome, tipo, data_hora, prazo_alteracoes, fuso, status)
				VALUES ('Almoço', 'ALMOCO', '2026-12-01 12:00', '2026-11-30 12:00', 'America/Sao_Paulo', 'EXPLODIDO')"""))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void prazoDepoisDaDataDoEventoEhRejeitado() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO eventos (nome, tipo, data_hora, prazo_alteracoes, fuso, status)
				VALUES ('Almoço', 'ALMOCO', '2026-12-01 12:00', '2026-12-02 12:00', 'America/Sao_Paulo', 'ABERTO')"""))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void nomeDeCategoriaRepetidoNoMesmoEventoEhRejeitadoSemDiferenciarMaiusculas() {
		long evento = inserirEvento();
		inserirCategoria(evento, "Bebidas");

		assertThatThrownBy(() -> inserirCategoria(evento, "bebidas"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void mesmoNomeDeCategoriaEmEventosDiferentesEhAceito() {
		inserirCategoria(inserirEvento(), "Bebidas");

		assertThatCode(() -> inserirCategoria(inserirEvento(), "Bebidas")).doesNotThrowAnyException();
	}

	@Test
	void quantidadeNecessariaZeroEhRejeitada() {
		long evento = inserirEvento();
		long categoria = inserirCategoria(evento, "Bebidas");

		assertThatThrownBy(() -> inserirItem(evento, categoria, 0))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void nomeDeItemRepetidoNaMesmaCategoriaEhRejeitadoSemDiferenciarMaiusculas() {
		long evento = inserirEvento();
		long categoria = inserirCategoria(evento, "Bebidas");
		inserirItem(evento, categoria, 5);

		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO itens_evento (evento_id, categoria_id, nome, quantidade_necessaria)
				VALUES (?, ?, 'SUCO', 3)""", evento, categoria))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void mesmoNomeDeItemEmCategoriasDiferentesEhAceito() {
		long evento = inserirEvento();

		assertThatCode(() -> {
			inserirItem(evento, inserirCategoria(evento, "Bebidas"), 5);
			inserirItem(evento, inserirCategoria(evento, "Sobremesas"), 5);
		}).doesNotThrowAnyException();
	}

	@Test
	void itemSemCategoriaEhRejeitado() {
		long evento = inserirEvento();

		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO itens_evento (evento_id, categoria_id, nome, quantidade_necessaria)
				VALUES (?, NULL, 'Suco', 5)""", evento))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void contribuicaoComQuantidadeZeroEhRejeitada() {
		long evento = inserirEvento();
		long item = inserirItem(evento, inserirCategoria(evento, "Bebidas"), 5);
		long participante = inserirParticipante(evento, inserirColaborador("52998224725"));

		assertThatThrownBy(() -> inserirContribuicao(participante, item, 0))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void mesmaPessoaContribuindoDuasVezesComOMesmoItemEhRejeitada() {
		long evento = inserirEvento();
		long item = inserirItem(evento, inserirCategoria(evento, "Bebidas"), 5);
		long participante = inserirParticipante(evento, inserirColaborador("52998224725"));
		inserirContribuicao(participante, item, 2);

		assertThatThrownBy(() -> inserirContribuicao(participante, item, 1))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void apagarCategoriaQueAindaTemItensEhBloqueado() {
		long evento = inserirEvento();
		long categoria = inserirCategoria(evento, "Bebidas");
		inserirItem(evento, categoria, 5);

		assertThatThrownBy(() -> jdbc.update("DELETE FROM categorias_evento WHERE id = ?", categoria))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void apagarEventoRemoveEmCascataTudoQueDependeDele() {
		long evento = inserirEvento();
		long item = inserirItem(evento, inserirCategoria(evento, "Bebidas"), 5);
		long participante = inserirParticipante(evento, inserirColaborador("52998224725"));
		inserirContribuicao(participante, item, 2);

		jdbc.update("DELETE FROM eventos WHERE id = ?", evento);

		assertThat(contar("categorias_evento")).isZero();
		assertThat(contar("itens_evento")).isZero();
		assertThat(contar("participantes_evento")).isZero();
		assertThat(contar("contribuicoes")).isZero();
		assertThat(contar("colaboradores")).isEqualTo(1);
	}

	private long inserirEvento() {
		return jdbc.queryForObject("""
				INSERT INTO eventos (nome, tipo, data_hora, prazo_alteracoes, fuso, status)
				VALUES ('Almoço', 'ALMOCO', '2026-12-01 12:00', '2026-11-30 12:00', 'America/Sao_Paulo', 'ABERTO')
				RETURNING id""", Long.class);
	}

	private long inserirCategoria(long evento, String nome) {
		return jdbc.queryForObject("""
				INSERT INTO categorias_evento (evento_id, nome, unidade, meta_quantidade)
				VALUES (?, ?, 'UNIDADE', 10) RETURNING id""", Long.class, evento, nome);
	}

	private long inserirItem(long evento, long categoria, int quantidade) {
		return jdbc.queryForObject("""
				INSERT INTO itens_evento (evento_id, categoria_id, nome, quantidade_necessaria)
				VALUES (?, ?, 'Suco', ?) RETURNING id""", Long.class, evento, categoria, quantidade);
	}

	private long inserirColaborador(String cpf) {
		return jdbc.queryForObject("""
				INSERT INTO colaboradores (nome, cpf) VALUES ('Ana', ?) RETURNING id""", Long.class, cpf);
	}

	private long inserirParticipante(long evento, long colaborador) {
		return jdbc.queryForObject("""
				INSERT INTO participantes_evento (evento_id, colaborador_id)
				VALUES (?, ?) RETURNING id""", Long.class, evento, colaborador);
	}

	private void inserirContribuicao(long participante, long item, int quantidade) {
		jdbc.update("""
				INSERT INTO contribuicoes (participante_id, item_id, quantidade)
				VALUES (?, ?, ?)""", participante, item, quantidade);
	}

	private int contar(String tabela) {
		return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
	}

}
