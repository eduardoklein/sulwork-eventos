package br.com.sulwork.events.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	private static final String DESCRIPTION = """
			API da Sulwork para organizar eventos de alimentação: cafés da manhã, almoços, lanches e confraternizações \
			em que cada pessoa leva alguma coisa.

			**Erros.** Todo erro sai no formato Problem Details (RFC 7807), com a mensagem no campo "detail". Os códigos:

			- **400** entrada malformada. A lista "errors" traz o campo e a mensagem de cada erro.
			- **404** recurso que não existe.
			- **409** conflito com um dado que já existe.
			- **422** entrada bem formada que quebra uma regra de negócio.
			- **500** erro inesperado.

			**Sem login.** O sistema não tem autenticação: quem conhece um CPF vê o nome e as restrições dessa pessoa. \
			Por isso o CPF sai mascarado nas respostas.

			**Datas.** A data e o prazo de um evento são horários de parede no fuso do próprio evento.""";

	@Bean
	OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("Sulwork Eventos API")
				.version("1.0.0")
				.description(DESCRIPTION));
	}

}
