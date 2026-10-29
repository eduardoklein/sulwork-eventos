package br.com.sulwork.eventos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MapeamentoJpaTest {

	private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

	@Autowired
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void gravaELeDeVoltaTodoOGrafoDeEntidades() {
		Evento evento = new Evento("Almoço", "Fim de ano", TipoEvento.ALMOCO,
				LocalDateTime.of(2026, 12, 1, 12, 0), LocalDateTime.of(2026, 11, 30, 12, 0), FUSO);
		em.persist(evento);
		CategoriaEvento categoria = new CategoriaEvento(evento, "Bebidas", UnidadeMedida.MILILITRO, 5000);
		em.persist(categoria);
		ItemEvento item = new ItemEvento(evento, categoria, "Suco de laranja", 2000, true);
		item.getAlergenos().add(Alergeno.LACTOSE);
		item.getCaracteristicas().add(CaracteristicaAlimentar.ACUCAR_ADICIONADO);
		em.persist(item);
		Colaborador colaborador = new Colaborador("Ana", "52998224725");
		colaborador.getRestricoes().add(RestricaoAlimentar.VEGANO);
		em.persist(colaborador);
		ParticipanteEvento participante = new ParticipanteEvento(evento, colaborador);
		em.persist(participante);
		em.persist(new Contribuicao(participante, item, 1000, "Levo na garrafa"));
		em.flush();
		em.clear();

		Evento lido = em.find(Evento.class, evento.getId());
		assertThat(lido.getStatus()).isEqualTo(StatusEvento.RASCUNHO);
		assertThat(lido.getTipo()).isEqualTo(TipoEvento.ALMOCO);
		assertThat(lido.getFuso()).isEqualTo(FUSO);
		assertThat(lido.getCriadoEm()).isNotNull();
		assertThat(lido.getAtualizadoEm()).isNotNull();

		ItemEvento itemLido = em.find(ItemEvento.class, item.getId());
		assertThat(itemLido.getAlergenos()).containsExactly(Alergeno.LACTOSE);
		assertThat(itemLido.getCaracteristicas()).containsExactly(CaracteristicaAlimentar.ACUCAR_ADICIONADO);
		assertThat(itemLido.getUnidade()).isEqualTo(UnidadeMedida.MILILITRO);
		assertThat(itemLido.isObrigatorio()).isTrue();

		Colaborador colaboradorLido = em.find(Colaborador.class, colaborador.getId());
		assertThat(colaboradorLido.getCpf()).isEqualTo("52998224725");
		assertThat(colaboradorLido.getRestricoes()).containsExactly(RestricaoAlimentar.VEGANO);

		Contribuicao contribuicaoLida = em.find(Contribuicao.class, em
				.createQuery("select c.id from Contribuicao c", Long.class).getSingleResult());
		assertThat(contribuicaoLida.getQuantidade()).isEqualTo(1000);
		assertThat(contribuicaoLida.getObservacao()).isEqualTo("Levo na garrafa");
		assertThat(contribuicaoLida.getParticipante().getColaborador().getNome()).isEqualTo("Ana");
	}

	@Test
	void enumsSaoGravadosComoTexto() {
		Evento evento = new Evento("Lanche", null, TipoEvento.CAFE_DA_MANHA,
				LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 11, 30, 9, 0), FUSO);
		evento.setStatus(StatusEvento.ABERTO);
		em.persist(evento);
		em.flush();

		String tipo = jdbc.queryForObject("SELECT tipo FROM eventos WHERE id = ?", String.class, evento.getId());
		String status = jdbc.queryForObject("SELECT status FROM eventos WHERE id = ?", String.class, evento.getId());

		assertThat(tipo).isEqualTo("CAFE_DA_MANHA");
		assertThat(status).isEqualTo("ABERTO");
	}

	@Test
	void atualizarEventoAvancaAtualizadoEmEMantemCriadoEm() throws InterruptedException {
		Evento evento = new Evento("Lanche", null, TipoEvento.LANCHE,
				LocalDateTime.of(2026, 12, 1, 15, 0), LocalDateTime.of(2026, 11, 30, 15, 0), FUSO);
		em.persist(evento);
		em.flush();
		LocalDateTime criadoEm = evento.getCriadoEm();
		LocalDateTime atualizadoAntes = evento.getAtualizadoEm();

		Thread.sleep(5);
		evento.setNome("Lanche da tarde");
		em.flush();

		assertThat(evento.getCriadoEm()).isEqualTo(criadoEm);
		assertThat(evento.getAtualizadoEm()).isAfter(atualizadoAntes);
	}

	@Test
	void cpfRepetidoEhRejeitadoNaGravacao() {
		em.persist(new Colaborador("Ana", "52998224725"));
		em.flush();

		assertThatThrownBy(() -> {
			em.persist(new Colaborador("Outra Ana", "52998224725"));
			em.flush();
		}).isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("uq_colaboradores_cpf");
	}

}
