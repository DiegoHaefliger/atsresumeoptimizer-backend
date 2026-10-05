package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class JsonBracketRepairTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void leavesAlreadyValidJsonUntouched() {
		String valid = "{\"a\":[1,2,{\"b\":[3,4]}]}";

		assertThat(JsonBracketRepair.repair(valid)).isEqualTo(valid);
	}

	@Test
	void fixesANestedArrayClosedWithACurlyBraceInstead() {
		String valid = "{\"richLines\":[[{\"bold\":true}],[{\"bold\":true},{\"bold\":false}]]}";
		String broken = "{\"richLines\":[[{\"bold\":true}],[{\"bold\":true},{\"bold\":false}}]}";

		String repaired = JsonBracketRepair.repair(broken);

		assertThat(repaired).isEqualTo(valid);
		JsonNode node = objectMapper.readTree(repaired);
		assertThat(node.get("richLines")).hasSize(2);
		assertThat(node.get("richLines").get(1)).hasSize(2);
	}

	@Test
	void doesNotTouchBracketCharactersInsideStringValues() {
		String json = "{\"text\":\"array like [this] and object like {this}\"}";

		assertThat(JsonBracketRepair.repair(json)).isEqualTo(json);
	}
}
