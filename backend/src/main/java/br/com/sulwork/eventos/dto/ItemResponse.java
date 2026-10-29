package br.com.sulwork.eventos.dto;

import java.util.Comparator;
import java.util.List;

import br.com.sulwork.eventos.domain.Alergeno;
import br.com.sulwork.eventos.domain.CaracteristicaAlimentar;
import br.com.sulwork.eventos.domain.ItemEvento;
import br.com.sulwork.eventos.domain.UnidadeMedida;

/** O percentual arredonda para baixo: só mostra 100 quando o item está completo. */
public record ItemResponse(
		Long id,
		String nome,
		Long categoriaId,
		String categoriaNome,
		UnidadeMedida unidade,
		int quantidadeNecessaria,
		boolean obrigatorio,
		long quantidadeContribuida,
		long quantidadePendente,
		int percentualCoberto,
		List<Alergeno> alergenos,
		List<CaracteristicaAlimentar> caracteristicas) {

	public static ItemResponse de(ItemEvento item, long contribuida) {
		int necessaria = item.getQuantidadeNecessaria();
		return new ItemResponse(
				item.getId(),
				item.getNome(),
				item.getCategoria().getId(),
				item.getCategoria().getNome(),
				item.getUnidade(),
				necessaria,
				item.isObrigatorio(),
				contribuida,
				Math.max(0, necessaria - contribuida),
				(int) Math.min(100, contribuida * 100 / necessaria),
				item.getAlergenos().stream().sorted(Comparator.naturalOrder()).toList(),
				item.getCaracteristicas().stream().sorted(Comparator.naturalOrder()).toList());
	}

}
