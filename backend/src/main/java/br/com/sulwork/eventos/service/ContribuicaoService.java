package br.com.sulwork.eventos.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.domain.Contribuicao;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.ItemEvento;
import br.com.sulwork.eventos.domain.ParticipanteEvento;
import br.com.sulwork.eventos.dto.AtualizarContribuicaoRequest;
import br.com.sulwork.eventos.dto.ContribuicaoResponse;
import br.com.sulwork.eventos.dto.CriarContribuicaoRequest;
import br.com.sulwork.eventos.exception.BusinessRuleException;
import br.com.sulwork.eventos.exception.ConflictException;
import br.com.sulwork.eventos.exception.ResourceNotFoundException;
import br.com.sulwork.eventos.repository.ContribuicaoRepository;
import br.com.sulwork.eventos.repository.ItemEventoRepository;
import br.com.sulwork.eventos.repository.ParticipanteEventoRepository;

/**
 * Toda mudança nas contribuições de um item bloqueia a linha do item primeiro. Assim, dois pedidos simultâneos não
 * leem o mesmo saldo livre, e a soma nunca passa da quantidade necessária.
 */
@Service
@Transactional
public class ContribuicaoService {

	private final ContribuicaoRepository contribuicoes;
	private final ItemEventoRepository itens;
	private final ParticipanteEventoRepository participantes;
	private final EventoService eventos;
	private final Clock clock;

	public ContribuicaoService(ContribuicaoRepository contribuicoes, ItemEventoRepository itens,
			ParticipanteEventoRepository participantes, EventoService eventos, Clock clock) {
		this.contribuicoes = contribuicoes;
		this.itens = itens;
		this.participantes = participantes;
		this.eventos = eventos;
		this.clock = clock;
	}

	public List<ContribuicaoResponse> listar(Long eventoId, Long itemId, Long participanteId) {
		eventos.buscarEntidadeSincronizada(eventoId);
		return contribuicoes.listar(eventoId, itemId, participanteId).stream().map(ContribuicaoResponse::de).toList();
	}

	public ContribuicaoResponse buscar(Long eventoId, Long id) {
		eventos.buscarEntidadeSincronizada(eventoId);
		return ContribuicaoResponse.de(buscarEntidade(eventoId, id));
	}

	public ContribuicaoResponse criar(Long eventoId, CriarContribuicaoRequest request) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		evento.exigirContribuicoesAbertas(clock.instant());
		ParticipanteEvento participante = participantes.buscar(request.participanteId(), eventoId)
				.orElseThrow(() -> new BusinessRuleException("Participante não encontrado neste evento."));
		ItemEvento item = itens.buscarComLock(request.itemId(), eventoId)
				.orElseThrow(() -> new BusinessRuleException("Item não encontrado neste evento."));
		if (contribuicoes.existsByParticipanteIdAndItemId(participante.getId(), item.getId())) {
			throw new ConflictException("Esta pessoa já contribui com este item. Edite a contribuição.");
		}
		long restante = item.getQuantidadeNecessaria() - contribuicoes.somarPorItem(item.getId());
		if (request.quantidade() > restante) {
			throw new BusinessRuleException(restante == 0
					? "Este item já está completo."
					: "Só cabem mais " + restante + " (" + descricaoDaUnidade(item) + ") neste item.");
		}

		Contribuicao contribuicao = new Contribuicao(participante, item, request.quantidade(), request.observacao());
		contribuicoes.saveAndFlush(contribuicao);
		return ContribuicaoResponse.de(contribuicao);
	}

	public ContribuicaoResponse atualizar(Long eventoId, Long id, AtualizarContribuicaoRequest request) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		evento.exigirContribuicoesAbertas(clock.instant());
		ItemEvento item = bloquearItemDaContribuicao(eventoId, id);
		Contribuicao contribuicao = buscarEntidade(eventoId, id);
		long maximo = item.getQuantidadeNecessaria() - contribuicoes.somarPorItemExceto(item.getId(), id);
		if (request.quantidade() > maximo) {
			throw new BusinessRuleException(
					"O máximo para esta contribuição é " + maximo + " (" + descricaoDaUnidade(item) + ").");
		}

		contribuicao.setQuantidade(request.quantidade());
		contribuicao.setObservacao(request.observacao());
		contribuicoes.saveAndFlush(contribuicao);
		return ContribuicaoResponse.de(contribuicao);
	}

	public void remover(Long eventoId, Long id) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		evento.exigirContribuicoesAbertas(clock.instant());
		bloquearItemDaContribuicao(eventoId, id);
		contribuicoes.delete(buscarEntidade(eventoId, id));
	}

	/** Descobre o item sem carregar a contribuição, bloqueia o item e só depois lê a contribuição já em dia. */
	private ItemEvento bloquearItemDaContribuicao(Long eventoId, Long id) {
		Long itemId = contribuicoes.buscarIdDoItem(id, eventoId).orElseThrow(this::naoEncontrada);
		return itens.buscarComLock(itemId, eventoId).orElseThrow(this::naoEncontrada);
	}

	private Contribuicao buscarEntidade(Long eventoId, Long id) {
		return contribuicoes.buscar(id, eventoId).orElseThrow(this::naoEncontrada);
	}

	private ResourceNotFoundException naoEncontrada() {
		return new ResourceNotFoundException("Contribuição não encontrada.");
	}

	private String descricaoDaUnidade(ItemEvento item) {
		return item.getUnidade().getDescricao().toLowerCase();
	}

}
