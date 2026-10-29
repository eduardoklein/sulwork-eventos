package br.com.sulwork.eventos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventosApiApplication.class, args);
	}

}
