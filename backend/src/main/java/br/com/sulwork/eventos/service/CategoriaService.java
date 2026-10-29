package br.com.sulwork.eventos.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.domain.CategoriaEvento;
import br.com.sulwork.eventos.domain.Evento;
import br.com.sulwork.eventos.dto.CategoriaRequest;
import br.com.sulwork.eventos.dto.CategoriaResponse;
import br.com.sulwork.eventos.exception.BusinessRuleException;
import br.com.sulwork.eventos.exception.ConflictException;
import br.com.sulwork.eventos.exception.ResourceNotFoundException;
import br.com.sulwork.eventos.repository.CategoriaEventoRepository;

@Service
@Transactional
public class CategoriaService {

	private final CategoriaEventoRepository categorias;
	private final EventoService eventos;

	public CategoriaService(CategoriaEventoRepository categorias, EventoService eventos) {
		this.categorias = categorias;
		this.eventos = eventos;
	}

	public List<CategoriaResponse> listar(Long eventoId) {
		eventos.buscarEntidadeSincronizada(eventoId);
		return categorias.resumir(eventoId, null);
	}

	public CategoriaResponse buscar(Long eventoId, Long id) {
		eventos.buscarEntidadeSincronizada(eventoId);
		buscarEntidade(eventoId, id);
		return resumo(eventoId, id);
	}

	public CategoriaResponse criar(Long eventoId, CategoriaRequest request) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		exigirCardapioEditavel(evento);
		String nome = request.nome().strip();
		exigirNomeLivre(eventoId, nome, null);

		CategoriaEvento categoria = new CategoriaEvento(evento, nome, request.unidade(), request.metaQuantidade());
		gravar(categoria);
		return resumo(eventoId, categoria.getId());
	}

	public CategoriaResponse atualizar(Long eventoId, Long id, CategoriaRequest request) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		exigirCardapioEditavel(evento);
		CategoriaEvento categoria = buscarEntidade(eventoId, id);
		String nome = request.nome().strip();
		exigirNomeLivre(eventoId, nome, id);
		if (request.unidade() != categoria.getUnidade() && categorias.temItens(id)) {
			throw new BusinessRuleException("A unidade não pode mudar enquanto a categoria tem itens.");
		}

		categoria.setNome(nome);
		categoria.setUnidade(request.unidade());
		categoria.setMetaQuantidade(request.metaQuantidade());
		gravar(categoria);
		return resumo(eventoId, id);
	}

	public void excluir(Long eventoId, Long id) {
		Evento evento = eventos.buscarEntidadeSincronizada(eventoId);
		exigirCardapioEditavel(evento);
		CategoriaEvento categoria = buscarEntidade(eventoId, id);
		if (categorias.temItens(id)) {
			throw new BusinessRuleException("Não é possível excluir uma categoria que ainda tem itens.");
		}
		categorias.delete(categoria);
	}

	private CategoriaEvento buscarEntidade(Long eventoId, Long id) {
		return categorias.findByIdAndEventoId(id, eventoId)
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
	}

	private CategoriaResponse resumo(Long eventoId, Long id) {
		return categorias.resumir(eventoId, id).getFirst();
	}

	private void exigirCardapioEditavel(Evento evento) {
		if (!evento.getStatus().allowsCatalogMutation()) {
			throw new BusinessRuleException(
					"Não é possível alterar o cardápio de um evento " + evento.getStatus().getDescricao().toLowerCase() + ".");
		}
	}

	private void exigirNomeLivre(Long eventoId, String nome, Long ignorarId) {
		if (categorias.existeNome(eventoId, nome, ignorarId)) {
			throw nomeRepetido();
		}
	}

	/** O índice único do banco decide quando duas pessoas criam o mesmo nome ao mesmo tempo. */
	private void gravar(CategoriaEvento categoria) {
		try {
			categorias.saveAndFlush(categoria);
		}
		catch (DataIntegrityViolationException ex) {
			throw nomeRepetido();
		}
	}

	private ConflictException nomeRepetido() {
		return new ConflictException("Já existe uma categoria com esse nome neste evento.");
	}

}
