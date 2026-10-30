package br.com.sulwork.eventos.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.domain.QuantidadesDaCategoria;
import br.com.sulwork.eventos.domain.UnidadeMedida;
import br.com.sulwork.eventos.dto.CoberturaResponse;
import br.com.sulwork.eventos.repository.CoberturaDaCategoria;
import br.com.sulwork.eventos.repository.ItemEventoRepository;

@Service
@Transactional
public class CoberturaService {

	private final ItemEventoRepository itens;
	private final EventoService eventos;

	public CoberturaService(ItemEventoRepository itens, EventoService eventos) {
		this.itens = itens;
		this.eventos = eventos;
	}

	public CoberturaResponse calcular(Long eventoId) {
		eventos.buscarEntidadeSincronizada(eventoId);
		List<QuantidadesDaCategoria> categorias = itens.coberturaPorCategoria(eventoId).stream()
				.map(CoberturaService::quantidades).toList();
		return CalculoDeCobertura.calcular(categorias);
	}

	private static QuantidadesDaCategoria quantidades(CoberturaDaCategoria c) {
		return new QuantidadesDaCategoria(c.getCategoriaId(), c.getNome(), UnidadeMedida.valueOf(c.getUnidade()),
				c.getMetaQuantidade(), c.getTotalItens(), c.getNecessarioObrigatorios(), c.getNecessarioOpcionais(),
				c.getContribuidoObrigatorios(), c.getContribuidoOpcionais());
	}

}
