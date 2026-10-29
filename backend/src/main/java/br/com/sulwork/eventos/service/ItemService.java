package br.com.sulwork.eventos.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.domain.CategoriaEvento;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.domain.ItemEvento;
import br.com.sulwork.eventos.dto.AtualizarItemRequest;
import br.com.sulwork.eventos.dto.CriarItemRequest;
import br.com.sulwork.eventos.dto.ItemResponse;
import br.com.sulwork.eventos.exception.BusinessRuleException;
import br.com.sulwork.eventos.exception.ConflictException;
import br.com.sulwork.eventos.exception.ResourceNotFoundException;
import br.com.sulwork.eventos.repository.CategoriaEventoRepository;
import br.com.sulwork.eventos.repository.ContribuicaoRepository;
import br.com.sulwork.eventos.repository.ContribuicaoRepository.TotalPorItem;
import br.com.sulwork.eventos.repository.ItemEventoRepository;

@Service
@Transactional
public class ItemService {

	private final ItemEventoRepository itens;
	private final CategoriaEventoRepository categorias;
	private final ContribuicaoRepository contribuicoes;
	private final EventoService eventos;

	public ItemService(ItemEventoRepository itens, CategoriaEventoRepository categorias,
			ContribuicaoRepository contribuicoes, EventoService eventos) {
		this.itens = itens;
		this.categorias = categorias;
		this.contribuicoes = contribuicoes;
		this.eventos = eventos;
	}

	public List<ItemResponse> listar(Long eventoId, Long categoriaId) {
		eventos.buscarEntidadeSincronizada(eventoId);
		Map<Long, Long> totais = contribuicoes.somarPorItemDoEvento(eventoId).stream()
				.collect(Collectors.toMap(TotalPorItem::itemId, TotalPorItem::total));
		return itens.listar(eventoId, categoriaId).stream()
				.map(item -> ItemResponse.de(item, totais.getOrDefault(item.getId(), 0L)))
				.toList();
	}

	public ItemResponse buscar(Long eventoId, Long id) {
		eventos.buscarEntidadeSincronizada(eventoId);
		return resposta(buscarEntidade(eventoId, id));
	}

	public ItemResponse criar(Long eventoId, CriarItemRequest request) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		evento.exigirCardapioEditavel();
		CategoriaEvento categoria = categorias.findByIdAndEventoId(request.categoriaId(), eventoId)
				.orElseThrow(() -> new BusinessRuleException("Categoria não encontrada neste evento."));
		String nome = request.nome().strip();
		exigirNomeLivre(categoria.getId(), nome, null);

		ItemEvento item = new ItemEvento(evento, categoria, nome, request.quantidadeNecessaria(),
				Boolean.TRUE.equals(request.obrigatorio()));
		substituir(item.getAlergenos(), request.alergenos());
		substituir(item.getCaracteristicas(), request.caracteristicas());
		gravar(item);
		return resposta(item);
	}

	public ItemResponse atualizar(Long eventoId, Long id, AtualizarItemRequest request) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		evento.exigirCardapioEditavel();
		ItemEvento item = buscarEntidade(eventoId, id);
		String nome = request.nome().strip();
		exigirNomeLivre(item.getCategoria().getId(), nome, id);
		long contribuida = contribuicoes.somarPorItem(id);
		if (request.quantidadeNecessaria() < contribuida) {
			throw new BusinessRuleException("A quantidade necessária não pode ficar abaixo do que já foi contribuído ("
					+ contribuida + ").");
		}

		item.setNome(nome);
		item.setQuantidadeNecessaria(request.quantidadeNecessaria());
		item.setObrigatorio(Boolean.TRUE.equals(request.obrigatorio()));
		substituir(item.getAlergenos(), request.alergenos());
		substituir(item.getCaracteristicas(), request.caracteristicas());
		gravar(item);
		return resposta(item);
	}

	public void excluir(Long eventoId, Long id) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		evento.exigirCardapioEditavel();
		ItemEvento item = buscarEntidade(eventoId, id);
		if (contribuicoes.existsByItemId(id)) {
			throw new BusinessRuleException("Não é possível excluir um item que já tem contribuições.");
		}
		itens.delete(item);
	}

	private ItemEvento buscarEntidade(Long eventoId, Long id) {
		return itens.buscar(id, eventoId).orElseThrow(() -> new ResourceNotFoundException("Item não encontrado."));
	}

	private ItemResponse resposta(ItemEvento item) {
		return ItemResponse.de(item, contribuicoes.somarPorItem(item.getId()));
	}

	private static <T> void substituir(Set<T> atual, Set<T> novo) {
		atual.clear();
		atual.addAll(novo == null ? new HashSet<>() : novo);
	}

	private void exigirNomeLivre(Long categoriaId, String nome, Long ignorarId) {
		if (itens.existeNome(categoriaId, nome, ignorarId)) {
			throw nomeRepetido();
		}
	}

	/** O índice único do banco decide quando duas pessoas criam o mesmo nome ao mesmo tempo. */
	private void gravar(ItemEvento item) {
		try {
			itens.saveAndFlush(item);
		}
		catch (DataIntegrityViolationException ex) {
			throw nomeRepetido();
		}
	}

	private ConflictException nomeRepetido() {
		return new ConflictException("Já existe um item com esse nome nesta categoria.");
	}

}
