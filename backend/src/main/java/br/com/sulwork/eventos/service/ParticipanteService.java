package br.com.sulwork.eventos.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.domain.Colaborador;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.ParticipanteEvento;
import br.com.sulwork.eventos.dto.InscreverParticipanteRequest;
import br.com.sulwork.eventos.dto.ParticipanteResponse;
import br.com.sulwork.eventos.exception.BusinessRuleException;
import br.com.sulwork.eventos.exception.ConflictException;
import br.com.sulwork.eventos.exception.ResourceNotFoundException;
import br.com.sulwork.eventos.repository.ColaboradorRepository;
import br.com.sulwork.eventos.repository.ParticipanteEventoRepository;
import br.com.sulwork.eventos.validation.CpfValidator;

@Service
@Transactional
public class ParticipanteService {

	private final ParticipanteEventoRepository participantes;
	private final ColaboradorRepository colaboradores;
	private final EventoService eventos;

	public ParticipanteService(ParticipanteEventoRepository participantes, ColaboradorRepository colaboradores,
			EventoService eventos) {
		this.participantes = participantes;
		this.colaboradores = colaboradores;
		this.eventos = eventos;
	}

	public List<ParticipanteResponse> listar(Long eventoId) {
		eventos.buscarEntidadeSincronizada(eventoId);
		return participantes.listar(eventoId).stream().map(ParticipanteResponse::de).toList();
	}

	public ParticipanteResponse buscar(Long eventoId, Long id) {
		eventos.buscarEntidadeSincronizada(eventoId);
		return ParticipanteResponse.de(buscarEntidade(eventoId, id));
	}

	/** Uma pessoa já cadastrada não é alterada pela inscrição: o cadastro muda só na rota de colaboradores. */
	public ParticipanteResponse inscrever(Long eventoId, InscreverParticipanteRequest request) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		exigirParticipantesEditaveis(evento);
		String cpf = CpfValidator.normalizeAndValidate(request.cpf());

		try {
			Colaborador colaborador = colaboradores.findByCpf(cpf).orElseGet(() -> novoColaborador(cpf, request));
			if (colaborador.getId() != null
					&& participantes.existsByEventoIdAndColaboradorId(eventoId, colaborador.getId())) {
				throw jaInscrita();
			}
			if (colaborador.getId() == null) {
				colaboradores.saveAndFlush(colaborador);
			}
			ParticipanteEvento participante = new ParticipanteEvento(evento, colaborador);
			participantes.saveAndFlush(participante);
			return ParticipanteResponse.de(participante);
		}
		catch (DataIntegrityViolationException ex) {
			throw jaInscrita();
		}
	}

	public void remover(Long eventoId, Long id) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		exigirParticipantesEditaveis(evento);
		participantes.delete(buscarEntidade(eventoId, id));
	}

	private Colaborador novoColaborador(String cpf, InscreverParticipanteRequest request) {
		if (request.nome() == null || request.nome().isBlank()) {
			throw new BusinessRuleException("Informe o nome para um CPF novo.");
		}
		Colaborador colaborador = new Colaborador(request.nome().strip(), cpf);
		colaborador.getRestricoes().addAll(request.restricoes() == null ? new HashSet<>() : request.restricoes());
		return colaborador;
	}

	private ParticipanteEvento buscarEntidade(Long eventoId, Long id) {
		return participantes.buscar(id, eventoId)
				.orElseThrow(() -> new ResourceNotFoundException("Participante não encontrado."));
	}

	private void exigirParticipantesEditaveis(Evento evento) {
		if (!evento.getStatus().allowsParticipantMutation()) {
			throw new BusinessRuleException("Só um evento aberto aceita mudanças nos participantes. Este evento está "
					+ evento.getStatus().getDescricao().toLowerCase() + ".");
		}
	}

	private ConflictException jaInscrita() {
		return new ConflictException("Esta pessoa já está inscrita neste evento.");
	}

}
