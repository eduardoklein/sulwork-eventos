package br.com.sulwork.eventos.domain;

import java.util.Set;

public record ConflitoAlimentar(Set<Alergeno> alergenos, Set<CaracteristicaAlimentar> caracteristicas) {

	public boolean isVazio() {
		return alergenos.isEmpty() && caracteristicas.isEmpty();
	}

}
