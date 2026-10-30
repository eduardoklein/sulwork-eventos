package br.com.sulwork.events.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.sulwork.events.exception.BusinessRuleException;

class EventTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");
	private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 30, 12, 0);

	/** 12:00 in Sao Paulo (UTC-3) is 15:00 UTC. */
	private static final Instant DEADLINE_IN_SAO_PAULO = Instant.parse("2026-11-30T15:00:00Z");

	@Test
	void newEventStartsAsDraft() {
		assertThat(newEvent(SAO_PAULO).getStatus()).isEqualTo(EventStatus.RASCUNHO);
	}

	@Test
	void deadlineDoesNotExpireBeforeScheduledInstant() {
		assertThat(newEvent(SAO_PAULO).isDeadlineExpired(DEADLINE_IN_SAO_PAULO.minusSeconds(1))).isFalse();
	}

	@Test
	void deadlineStillHoldsAtExactInstant() {
		assertThat(newEvent(SAO_PAULO).isDeadlineExpired(DEADLINE_IN_SAO_PAULO)).isFalse();
	}

	@Test
	void deadlineExpiresOneSecondLater() {
		assertThat(newEvent(SAO_PAULO).isDeadlineExpired(DEADLINE_IN_SAO_PAULO.plusSeconds(1))).isTrue();
	}

	@Test
	void sameWallClockTimeExpiresAtDifferentInstantsInDifferentTimeZones() {
		Instant noonUtc = Instant.parse("2026-11-30T12:00:00Z");

		assertThat(newEvent(LISBON).isDeadlineExpired(noonUtc.plusSeconds(1))).isTrue();
		assertThat(newEvent(SAO_PAULO).isDeadlineExpired(noonUtc.plusSeconds(1))).isFalse();
	}

	@Test
	void utcServerDoesNotAnticipateDeadlineOfSaoPauloEvent() {
		Instant noonUtc = Instant.parse("2026-11-30T12:00:01Z");

		assertThat(newEvent(SAO_PAULO).isDeadlineExpired(noonUtc)).isFalse();
	}

	@Test
	void eventOnlyStartsAtScheduledInstantInEventTimeZone() {
		Event event = newEvent(SAO_PAULO);
		Instant start = Instant.parse("2026-12-01T15:00:00Z");

		assertThat(event.hasStarted(start.minusSeconds(1))).isFalse();
		assertThat(event.hasStarted(start)).isTrue();
		assertThat(event.hasStarted(start.plusSeconds(1))).isTrue();
	}

	@Test
	void openEventWithExpiredDeadlineBecomesClosed() {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(EventStatus.ABERTO);

		assertThat(event.closeIfDeadlineExpired(DEADLINE_IN_SAO_PAULO.plusSeconds(1))).isTrue();
		assertThat(event.getStatus()).isEqualTo(EventStatus.ENCERRADO);
	}

	@Test
	void openEventWithinDeadlineOrAtExactInstantIsNotClosed() {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(EventStatus.ABERTO);

		assertThat(event.closeIfDeadlineExpired(DEADLINE_IN_SAO_PAULO)).isFalse();
		assertThat(event.getStatus()).isEqualTo(EventStatus.ABERTO);
	}

	@ParameterizedTest
	@EnumSource(value = EventStatus.class, names = "ABERTO", mode = EnumSource.Mode.EXCLUDE)
	void onlyOpenEventIsClosedByDeadline(EventStatus status) {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(status);

		assertThat(event.closeIfDeadlineExpired(DEADLINE_IN_SAO_PAULO.plusSeconds(3600))).isFalse();
		assertThat(event.getStatus()).isEqualTo(status);
	}

	@ParameterizedTest
	@EnumSource(value = EventStatus.class, names = { "RASCUNHO", "ABERTO" })
	void menuEditableInDraftAndOpen(EventStatus status) {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(status);

		assertThatCode(event::requireEditableMenu).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(value = EventStatus.class, names = { "ENCERRADO", "REALIZADO", "CANCELADO" })
	void menuNotEditableInOtherStatuses(EventStatus status) {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(status);

		assertThatThrownBy(event::requireEditableMenu)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Não é possível alterar o cardápio de um evento " + status.getDescription().toLowerCase() + ".");
	}

	@Test
	void openEventWithinDeadlineAcceptsContributions() {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(EventStatus.ABERTO);

		assertThat(event.isOpenForContributions(DEADLINE_IN_SAO_PAULO.minusSeconds(3600))).isTrue();
	}

	@Test
	void openEventWithExpiredDeadlineDoesNotAcceptContributions() {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(EventStatus.ABERTO);

		assertThat(event.isOpenForContributions(DEADLINE_IN_SAO_PAULO.plusSeconds(60))).isFalse();
	}

	@ParameterizedTest
	@EnumSource(value = EventStatus.class, names = "ABERTO", mode = EnumSource.Mode.EXCLUDE)
	void statusOtherThanOpenNeverAcceptsContributionsEvenWithinDeadline(EventStatus status) {
		Event event = newEvent(SAO_PAULO);
		event.setStatus(status);

		assertThat(event.isOpenForContributions(DEADLINE_IN_SAO_PAULO.minusSeconds(3600))).isFalse();
	}

	private Event newEvent(ZoneId timeZone) {
		return new Event("Almoço de fim de ano", null, EventType.LUNCH,
				LocalDateTime.of(2026, 12, 1, 12, 0), DEADLINE, timeZone);
	}

}
