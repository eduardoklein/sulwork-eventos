package br.com.sulwork.eventos.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static br.com.sulwork.eventos.domain.Alergeno.AMENDOIM;
import static br.com.sulwork.eventos.domain.Alergeno.CASTANHAS;
import static br.com.sulwork.eventos.domain.Alergeno.FRUTOS_DO_MAR;
import static br.com.sulwork.eventos.domain.Alergeno.GLUTEN;
import static br.com.sulwork.eventos.domain.Alergeno.LACTOSE;
import static br.com.sulwork.eventos.domain.Alergeno.OVO;
import static br.com.sulwork.eventos.domain.Alergeno.PEIXE;
import static br.com.sulwork.eventos.domain.Alergeno.SOJA;
import static br.com.sulwork.eventos.domain.CaracteristicaAlimentar.ACUCAR_ADICIONADO;
import static br.com.sulwork.eventos.domain.CaracteristicaAlimentar.ALTO_SODIO;
import static br.com.sulwork.eventos.domain.CaracteristicaAlimentar.CARNE_ORIGEM_ANIMAL;

public enum RestricaoAlimentar {

	INTOLERANCIA_LACTOSE("Intolerância à lactose", alergenos(LACTOSE), nenhuma()),
	CELIACO_SEM_GLUTEN("Celíaco (sem glúten)", alergenos(GLUTEN), nenhuma()),
	ALERGIA_AMENDOIM_CASTANHAS("Alergia a amendoim e castanhas", alergenos(AMENDOIM, CASTANHAS), nenhuma()),
	ALERGIA_FRUTOS_DO_MAR("Alergia a frutos do mar", alergenos(FRUTOS_DO_MAR), nenhuma()),
	ALERGIA_PEIXE("Alergia a peixe", alergenos(PEIXE), nenhuma()),
	ALERGIA_OVO("Alergia a ovo", alergenos(OVO), nenhuma()),
	ALERGIA_SOJA("Alergia a soja", alergenos(SOJA), nenhuma()),
	DIABETICO_SEM_ACUCAR("Diabético (sem açúcar)", nenhum(), caracteristicas(ACUCAR_ADICIONADO)),
	HIPERTENSO_POUCO_SODIO("Hipertenso (pouco sal)", nenhum(), caracteristicas(ALTO_SODIO)),
	VEGETARIANO("Vegetariano", alergenos(FRUTOS_DO_MAR, PEIXE), caracteristicas(CARNE_ORIGEM_ANIMAL)),
	VEGANO("Vegano", alergenos(FRUTOS_DO_MAR, PEIXE, LACTOSE, OVO), caracteristicas(CARNE_ORIGEM_ANIMAL));

	private final String descricao;
	private final Set<Alergeno> alergenosEmConflito;
	private final Set<CaracteristicaAlimentar> caracteristicasEmConflito;

	RestricaoAlimentar(String descricao, Set<Alergeno> alergenosEmConflito,
			Set<CaracteristicaAlimentar> caracteristicasEmConflito) {
		this.descricao = descricao;
		this.alergenosEmConflito = alergenosEmConflito;
		this.caracteristicasEmConflito = caracteristicasEmConflito;
	}

	public String getDescricao() {
		return descricao;
	}

	public Set<Alergeno> getAlergenosEmConflito() {
		return alergenosEmConflito;
	}

	public Set<CaracteristicaAlimentar> getCaracteristicasEmConflito() {
		return caracteristicasEmConflito;
	}

	public ConflitoAlimentar encontrarConflitos(Set<Alergeno> alergenosDoItem,
			Set<CaracteristicaAlimentar> caracteristicasDoItem) {
		return new ConflitoAlimentar(
				intersecao(alergenosEmConflito, alergenosDoItem, Alergeno.class),
				intersecao(caracteristicasEmConflito, caracteristicasDoItem, CaracteristicaAlimentar.class));
	}

	private static <E extends Enum<E>> Set<E> intersecao(Set<E> emConflito, Set<E> doItem, Class<E> tipo) {
		EnumSet<E> resultado = EnumSet.noneOf(tipo);
		resultado.addAll(emConflito);
		resultado.retainAll(doItem);
		return Collections.unmodifiableSet(resultado);
	}

	private static Set<Alergeno> alergenos(Alergeno primeiro, Alergeno... demais) {
		return Collections.unmodifiableSet(EnumSet.of(primeiro, demais));
	}

	private static Set<Alergeno> nenhum() {
		return Collections.unmodifiableSet(EnumSet.noneOf(Alergeno.class));
	}

	private static Set<CaracteristicaAlimentar> caracteristicas(CaracteristicaAlimentar primeira,
			CaracteristicaAlimentar... demais) {
		return Collections.unmodifiableSet(EnumSet.of(primeira, demais));
	}

	private static Set<CaracteristicaAlimentar> nenhuma() {
		return Collections.unmodifiableSet(EnumSet.noneOf(CaracteristicaAlimentar.class));
	}

}
