package br.com.sulwork.eventos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mantém o status em dia mesmo quando ninguém acessa o evento. */
@Component
public class EventoPrazoScheduler {

	private static final Logger log = LoggerFactory.getLogger(EventoPrazoScheduler.class);

	private final EventoService service;

	public EventoPrazoScheduler(EventoService service) {
		this.service = service;
	}

	@Scheduled(fixedDelay = 60_000)
	public void encerrarEventosVencidos() {
		int encerrados = service.encerrarVencidos();
		if (encerrados > 0) {
			log.info("{} evento(s) encerrado(s) pelo prazo", encerrados);
		}
	}

}
