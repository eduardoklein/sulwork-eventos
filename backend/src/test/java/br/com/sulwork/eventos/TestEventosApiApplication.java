package br.com.sulwork.eventos;

import org.springframework.boot.SpringApplication;

public class TestEventosApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(EventosApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
