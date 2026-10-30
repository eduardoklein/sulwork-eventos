package br.com.sulwork.events;

import org.springframework.boot.SpringApplication;

public class TestEventsApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(EventsApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
