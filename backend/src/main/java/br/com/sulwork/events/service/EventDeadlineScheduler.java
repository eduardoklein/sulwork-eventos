package br.com.sulwork.events.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Keeps the status up to date even when nobody accesses the event. */
@Component
public class EventDeadlineScheduler {

	private static final Logger log = LoggerFactory.getLogger(EventDeadlineScheduler.class);

	private final EventService service;

	public EventDeadlineScheduler(EventService service) {
		this.service = service;
	}

	@Scheduled(fixedDelay = 60_000)
	public void closeExpiredEvents() {
		int closedCount = service.closeExpired();
		if (closedCount > 0) {
			log.info("{} evento(s) encerrado(s) pelo prazo", closedCount);
		}
	}

}
