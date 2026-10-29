package br.com.sulwork.eventos.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.StatusEvento;
import br.com.sulwork.eventos.domain.TipoEvento;
import br.com.sulwork.eventos.dto.AtualizarEventoRequest;
import br.com.sulwork.eventos.dto.CriarEventoRequest;
import br.com.sulwork.eventos.dto.EventoResponse;
import br.com.sulwork.eventos.exception.BusinessRuleException;
import br.com.sulwork.eventos.exception.ResourceNotFoundException;
import br.com.sulwork.eventos.repository.EventoRepository;

@Service
@Transactional
public class EventoService {

	private final EventoRepository eventos;
	private final Clock clock;
	private final ZoneId fusoPadrao;

	public EventoService(EventoRepository eventos, Clock clock, @Value("${app.fuso-padrao}") String fusoPadrao) {
		this.eventos = eventos;
		this.clock = clock;
		this.fusoPadrao = ZoneId.of(fusoPadrao);
	}

	public EventoResponse criar(CriarEventoRequest request) {
		StatusEvento status = request.status() == null ? StatusEvento.RASCUNHO : request.status();
		if (status != StatusEvento.RASCUNHO && status != StatusEvento.ABERTO) {
			throw new BusinessRuleException("Um evento novo só pode nascer em rascunho ou aberto.");
		}
		exigirPrazoAteADataDoEvento(request.prazoAlteracoes(), request.dataHora());

		Evento evento = new Evento(request.nome().strip(), request.descricao(), request.tipo(), request.dataHora(),
				request.prazoAlteracoes(), fusoOu(request.fuso(), fusoPadrao));
		evento.setStatus(status);
		if (status == StatusEvento.ABERTO) {
			exigirPrazoNoFuturo(evento);
		}
		return resposta(eventos.save(evento));
	}

	@Transactional(readOnly = true)
	public List<EventoResponse> listar(StatusEvento status, TipoEvento tipo, LocalDate de, LocalDate ate) {
		LocalDateTime inicio = de == null ? null : de.atStartOfDay();
		LocalDateTime fim = ate == null ? null : ate.plusDays(1).atStartOfDay();
		return eventos.filtrar(status, tipo, inicio, fim).stream().map(this::resposta).toList();
	}

	@Transactional(readOnly = true)
	public EventoResponse buscar(Long id) {
		return resposta(buscarEntidade(id));
	}

	public EventoResponse atualizar(Long id, AtualizarEventoRequest request) {
		Evento evento = buscarEntidade(id);
		StatusEvento status = evento.getStatus();
		if (status == StatusEvento.REALIZADO || status == StatusEvento.CANCELADO) {
			throw new BusinessRuleException("Um evento " + status.getDescricao().toLowerCase() + " não pode ser editado.");
		}
		ZoneId fuso = fusoOu(request.fuso(), evento.getFuso());
		if (status == StatusEvento.ENCERRADO
				&& (!request.prazoAlteracoes().equals(evento.getPrazoAlteracoes()) || !fuso.equals(evento.getFuso()))) {
			throw new BusinessRuleException("Em um evento encerrado, o prazo e o fuso não podem mudar.");
		}
		exigirPrazoAteADataDoEvento(request.prazoAlteracoes(), request.dataHora());

		evento.setNome(request.nome().strip());
		evento.setDescricao(request.descricao());
		evento.setTipo(request.tipo());
		evento.setDataHora(request.dataHora());
		evento.setPrazoAlteracoes(request.prazoAlteracoes());
		evento.setFuso(fuso);
		if (status == StatusEvento.ABERTO) {
			exigirPrazoNoFuturo(evento);
		}
		return resposta(eventos.saveAndFlush(evento));
	}

	public void excluir(Long id) {
		Evento evento = buscarEntidade(id);
		if (evento.getStatus() != StatusEvento.RASCUNHO) {
			throw new BusinessRuleException(
					"Só eventos em rascunho podem ser excluídos. Cancele o evento para desfazê-lo.");
		}
		eventos.delete(evento);
	}

	private Evento buscarEntidade(Long id) {
		return eventos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
	}

	private EventoResponse resposta(Evento evento) {
		return EventoResponse.de(evento, clock.instant());
	}

	private void exigirPrazoAteADataDoEvento(LocalDateTime prazo, LocalDateTime dataHora) {
		if (prazo.isAfter(dataHora)) {
			throw new BusinessRuleException("O prazo para alterações não pode ser depois da data do evento.");
		}
	}

	private void exigirPrazoNoFuturo(Evento evento) {
		Instant agora = clock.instant();
		if (evento.isPrazoExpirado(agora)) {
			throw new BusinessRuleException("Um evento aberto precisa de um prazo que ainda não passou.");
		}
	}

	private static ZoneId fusoOu(String fuso, ZoneId padrao) {
		return fuso == null || fuso.isBlank() ? padrao : ZoneId.of(fuso.strip());
	}

}
