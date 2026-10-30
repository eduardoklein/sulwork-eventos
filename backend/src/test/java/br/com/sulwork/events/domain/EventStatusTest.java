package br.com.sulwork.events.domain;

import static br.com.sulwork.events.domain.EventStatus.ABERTO;
import static br.com.sulwork.events.domain.EventStatus.CANCELADO;
import static br.com.sulwork.events.domain.EventStatus.ENCERRADO;
import static br.com.sulwork.events.domain.EventStatus.RASCUNHO;
import static br.com.sulwork.events.domain.EventStatus.REALIZADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EventStatusTest {

	private static final Map<EventStatus, Set<EventStatus>> VALID_TRANSITIONS = Map.of(
			RASCUNHO, EnumSet.of(ABERTO, CANCELADO),
			ABERTO, EnumSet.of(ENCERRADO, CANCELADO),
			ENCERRADO, EnumSet.of(REALIZADO, CANCELADO),
			REALIZADO, EnumSet.noneOf(EventStatus.class),
			CANCELADO, EnumSet.noneOf(EventStatus.class));

	@ParameterizedTest
	@EnumSource(EventStatus.class)
	void allowedTransitionsAreExactlyThoseInTable(EventStatus source) {
		assertThat(source.allowedTransitions()).containsExactlyInAnyOrderElementsOf(VALID_TRANSITIONS.get(source));
	}

	@ParameterizedTest
	@EnumSource(EventStatus.class)
	void canTransitionToMatchesTableForAllPairs(EventStatus source) {
		for (EventStatus target : EventStatus.values()) {
			assertThat(source.canTransitionTo(target))
					.as("%s -> %s", source, target)
					.isEqualTo(VALID_TRANSITIONS.get(source).contains(target));
		}
	}

	@Test
	void importantInvalidTransitions() {
		assertThat(REALIZADO.canTransitionTo(ABERTO)).isFalse();
		assertThat(RASCUNHO.canTransitionTo(REALIZADO)).isFalse();
		assertThat(ENCERRADO.canTransitionTo(ABERTO)).isFalse();
		assertThat(CANCELADO.canTransitionTo(ABERTO)).isFalse();
		assertThat(ABERTO.canTransitionTo(RASCUNHO)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(EventStatus.class)
	void statusCannotGoToItself(EventStatus status) {
		assertThat(status.canTransitionTo(status)).isFalse();
	}

	@Test
	void onlyHeldAndCancelledAreFinal() {
		assertThat(EventStatus.values()).filteredOn(EventStatus::isFinal)
				.containsExactlyInAnyOrder(REALIZADO, CANCELADO);
	}

	@Test
	void onlyOpenAcceptsContributionsAndEnrollments() {
		for (EventStatus status : EventStatus.values()) {
			assertThat(status.acceptsContributions()).as("contribuições em %s", status).isEqualTo(status == ABERTO);
			assertThat(status.allowsParticipantChanges()).as("inscrições em %s", status).isEqualTo(status == ABERTO);
		}
	}

	@Test
	void menuEditableOnlyInDraftAndOpen() {
		for (EventStatus status : EventStatus.values()) {
			assertThat(status.allowsMenuChanges()).as("cardápio em %s", status)
					.isEqualTo(status == RASCUNHO || status == ABERTO);
		}
	}

	@Test
	void transitionSetCannotBeModified() {
		assertThatThrownBy(() -> RASCUNHO.allowedTransitions().add(REALIZADO))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
