package br.com.sulwork.eventos.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.TestcontainersConfiguration;

/**
 * Os CHECK do schema repetem as listas dos enums. Estes testes quebram se uma lista mudar sem a outra.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class EnumsEstaoNoSchemaTest {

	@Autowired
	private JdbcTemplate jdbc;

	@ParameterizedTest
	@EnumSource(TipoEvento.class)
	void bancoAceitaTodoTipoDeEvento(TipoEvento tipo) {
		assertThatCode(() -> jdbc.update("""
				INSERT INTO eventos (nome, tipo, data_hora, prazo_alteracoes, status)
				VALUES ('Evento', ?, '2026-12-01 12:00', '2026-11-30 12:00', 'ABERTO')""", tipo.name()))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(UnidadeMedida.class)
	void bancoAceitaTodaUnidade(UnidadeMedida unidade) {
		long evento = inserirEvento();

		assertThatCode(() -> jdbc.update("""
				INSERT INTO categorias_evento (evento_id, nome, unidade) VALUES (?, 'Categoria', ?)""",
				evento, unidade.name())).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(Alergeno.class)
	void bancoAceitaTodoAlergeno(Alergeno alergeno) {
		long item = inserirItem();

		assertThatCode(() -> jdbc.update("INSERT INTO item_alergenos (item_id, alergeno) VALUES (?, ?)",
				item, alergeno.name())).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(CaracteristicaAlimentar.class)
	void bancoAceitaTodaCaracteristica(CaracteristicaAlimentar caracteristica) {
		long item = inserirItem();

		assertThatCode(() -> jdbc.update("INSERT INTO item_caracteristicas (item_id, caracteristica) VALUES (?, ?)",
				item, caracteristica.name())).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(RestricaoAlimentar.class)
	void bancoAceitaTodaRestricao(RestricaoAlimentar restricao) {
		long colaborador = jdbc.queryForObject(
				"INSERT INTO colaboradores (nome, cpf) VALUES ('Ana', '52998224725') RETURNING id", Long.class);

		assertThatCode(() -> jdbc.update(
				"INSERT INTO colaborador_restricoes (colaborador_id, restricao) VALUES (?, ?)",
				colaborador, restricao.name())).doesNotThrowAnyException();
	}

	@Test
	void caracteristicaNaoEhAceitaComoAlergeno() {
		long item = inserirItem();

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO item_alergenos (item_id, alergeno) VALUES (?, 'ALTO_SODIO')", item))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private long inserirEvento() {
		return jdbc.queryForObject("""
				INSERT INTO eventos (nome, tipo, data_hora, prazo_alteracoes, status)
				VALUES ('Evento', 'ALMOCO', '2026-12-01 12:00', '2026-11-30 12:00', 'ABERTO')
				RETURNING id""", Long.class);
	}

	private long inserirItem() {
		long evento = inserirEvento();
		long categoria = jdbc.queryForObject("""
				INSERT INTO categorias_evento (evento_id, nome, unidade) VALUES (?, 'Categoria', 'UNIDADE')
				RETURNING id""", Long.class, evento);
		return jdbc.queryForObject("""
				INSERT INTO itens_evento (evento_id, categoria_id, nome, quantidade_necessaria)
				VALUES (?, ?, 'Item', 5) RETURNING id""", Long.class, evento, categoria);
	}

}
