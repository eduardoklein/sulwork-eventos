package br.com.sulwork.events.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

	private MockMvc mvc;

	@BeforeEach
	void setUpMvc() {
		mvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void resourceNotFoundReturns404() throws Exception {
		mvc.perform(get("/test/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
				.andExpect(jsonPath("$.detail").value("Evento não encontrado."));
	}

	@Test
	void businessRuleReturns422() throws Exception {
		mvc.perform(get("/test/rule"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.title").value("Regra de negócio violada"))
				.andExpect(jsonPath("$.detail").value("Contribuição depois do prazo."));
	}

	@Test
	void conflictReturns409() throws Exception {
		mvc.perform(get("/test/conflict"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflito"))
				.andExpect(jsonPath("$.detail").value("CPF já inscrito no evento."));
	}

	@Test
	void validation400WithOneErrorPerField() throws Exception {
		mvc.perform(post("/test/validate").locale(Locale.of("pt", "BR"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\",\"quantity\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Dados inválidos."))
				.andExpect(jsonPath("$.errors.length()").value(2))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty())
				.andExpect(jsonPath("$.errors[1].field").value("quantity"))
				.andExpect(jsonPath("$.errors[1].message").isNotEmpty());
	}

	@Test
	void validBodyProducesNoError() throws Exception {
		mvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Ana\",\"quantity\":3}"))
				.andExpect(status().isOk());
	}

	@Test
	void brokenJsonReturns400() throws Exception {
		mvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{nome:"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Corpo da requisição ilegível ou mal formado."));
	}

	@Test
	void wrongTypeInAddressReturns400() throws Exception {
		mvc.perform(get("/test/number/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'id'."));
	}

	@Test
	void methodNotAllowedReturns405() throws Exception {
		mvc.perform(post("/test/rule"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.title").value("Método não permitido"));
	}

	@Test
	void unexpectedErrorReturns500WithoutLeakingInternalDetails() throws Exception {
		String body = mvc.perform(get("/test/error"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("Erro interno do servidor."))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("segredo interno");
	}

	@RestController
	static class TestController {

		record Body(@NotBlank String name, @Min(1) int quantity) {
		}

		@GetMapping("/test/not-found")
		void notFound() {
			throw new ResourceNotFoundException("Evento não encontrado.");
		}

		@GetMapping("/test/rule")
		void rule() {
			throw new BusinessRuleException("Contribuição depois do prazo.");
		}

		@GetMapping("/test/conflict")
		void conflict() {
			throw new ConflictException("CPF já inscrito no evento.");
		}

		@GetMapping("/test/error")
		void error() {
			throw new IllegalStateException("segredo interno");
		}

		@GetMapping("/test/number/{id}")
		void number(@PathVariable Long id) {
		}

		@PostMapping("/test/validate")
		void validate(@Valid @RequestBody Body body) {
		}

	}

}
