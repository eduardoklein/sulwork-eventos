package br.com.sulwork.events.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sulwork.events.TestcontainersConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Every endpoint must be documented, so a new endpoint without a summary breaks the build. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

	private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "patch", "delete");

	@Autowired
	private MockMvc mvc;

	private JsonNode docs() throws Exception {
		String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return new ObjectMapper().readTree(json);
	}

	@Test
	void showsTheTitleTheVersionAndTheDescriptionOfTheApi() throws Exception {
		JsonNode info = docs().get("info");

		assertThat(info.get("title").asString()).isEqualTo("Sulwork Eventos API");
		assertThat(info.get("version").asString()).isEqualTo("1.0.0");
		assertThat(info.get("description").asString()).contains("Problem Details").contains("Sem login");
	}

	@Test
	void everyOperationHasASummaryAndATag() throws Exception {
		List<String> undocumented = new ArrayList<>();
		int operations = 0;
		for (var path : docs().get("paths").properties()) {
			for (String method : HTTP_METHODS) {
				JsonNode operation = path.getValue().get(method);
				if (operation == null) {
					continue;
				}
				operations++;
				boolean hasSummary = operation.hasNonNull("summary") && !operation.get("summary").asString().isBlank();
				boolean hasTag = operation.has("tags") && operation.get("tags").size() > 0
						&& !operation.get("tags").get(0).asString().endsWith("-controller");
				if (!hasSummary || !hasTag) {
					undocumented.add(method.toUpperCase() + " " + path.getKey());
				}
			}
		}

		assertThat(operations).isGreaterThanOrEqualTo(30);
		assertThat(undocumented).as("operations without summary or tag").isEmpty();
	}

	@Test
	void describesTheTagsOfEveryGroup() throws Exception {
		List<String> tags = new ArrayList<>();
		docs().get("tags").forEach(tag -> tags.add(tag.get("name").asString()));

		assertThat(tags).contains("Eventos", "Categorias", "Itens do cardápio", "Participantes", "Colaboradores",
				"Contribuições", "Cobertura", "Análise alimentar", "Sugestões", "Resumo", "Domínio");
	}

}
