package br.com.sulwork.eventos.exception;

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
	void montarMvc() {
		mvc = MockMvcBuilders.standaloneSetup(new ControladorDeTeste())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void recursoNaoEncontradoDa404() throws Exception {
		mvc.perform(get("/teste/nao-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
				.andExpect(jsonPath("$.detail").value("Evento não encontrado."));
	}

	@Test
	void regraDeNegocioDa422() throws Exception {
		mvc.perform(get("/teste/regra"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.title").value("Regra de negócio violada"))
				.andExpect(jsonPath("$.detail").value("Contribuição depois do prazo."));
	}

	@Test
	void conflitoDa409() throws Exception {
		mvc.perform(get("/teste/conflito"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflito"))
				.andExpect(jsonPath("$.detail").value("CPF já inscrito no evento."));
	}

	@Test
	void validacaoDa400ComUmErroPorCampo() throws Exception {
		mvc.perform(post("/teste/validar").locale(Locale.of("pt", "BR"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"\",\"quantidade\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Dados inválidos."))
				.andExpect(jsonPath("$.erros.length()").value(2))
				.andExpect(jsonPath("$.erros[0].campo").value("nome"))
				.andExpect(jsonPath("$.erros[0].mensagem").isNotEmpty())
				.andExpect(jsonPath("$.erros[1].campo").value("quantidade"))
				.andExpect(jsonPath("$.erros[1].mensagem").isNotEmpty());
	}

	@Test
	void corpoValidoNaoGeraErro() throws Exception {
		mvc.perform(post("/teste/validar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Ana\",\"quantidade\":3}"))
				.andExpect(status().isOk());
	}

	@Test
	void jsonQuebradoDa400() throws Exception {
		mvc.perform(post("/teste/validar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{nome:"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Corpo da requisição ilegível ou mal formado."));
	}

	@Test
	void tipoErradoNoEnderecoDa400() throws Exception {
		mvc.perform(get("/teste/numero/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'id'."));
	}

	@Test
	void metodoNaoPermitidoDa405() throws Exception {
		mvc.perform(post("/teste/regra"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.title").value("Método não permitido"));
	}

	@Test
	void erroInesperadoDa500SemVazarDetalhesInternos() throws Exception {
		String corpo = mvc.perform(get("/teste/erro"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("Erro interno do servidor."))
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo).doesNotContain("segredo interno");
	}

	@RestController
	static class ControladorDeTeste {

		record Corpo(@NotBlank String nome, @Min(1) int quantidade) {
		}

		@GetMapping("/teste/nao-encontrado")
		void naoEncontrado() {
			throw new ResourceNotFoundException("Evento não encontrado.");
		}

		@GetMapping("/teste/regra")
		void regra() {
			throw new BusinessRuleException("Contribuição depois do prazo.");
		}

		@GetMapping("/teste/conflito")
		void conflito() {
			throw new ConflictException("CPF já inscrito no evento.");
		}

		@GetMapping("/teste/erro")
		void erro() {
			throw new IllegalStateException("segredo interno");
		}

		@GetMapping("/teste/numero/{id}")
		void numero(@PathVariable Long id) {
		}

		@PostMapping("/teste/validar")
		void validar(@Valid @RequestBody Corpo corpo) {
		}

	}

}
