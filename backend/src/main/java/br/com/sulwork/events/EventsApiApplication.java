package br.com.sulwork.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventsApiApplication.class, args);
	}

}
