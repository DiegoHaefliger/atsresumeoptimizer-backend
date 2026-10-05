package com.diegohaefliger.atsresumeoptimizer.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TextSpanTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void treatsMissingBoldAsFalse() {
		assertThat(objectMapper.readValue("{\"text\":\"Java\"}", TextSpan.class)).isEqualTo(new TextSpan("Java", false));
	}

	@Test
	void treatsNullBoldAsFalse() {
		assertThat(objectMapper.readValue("{\"text\":\"Java\",\"bold\":null}", TextSpan.class))
				.isEqualTo(new TextSpan("Java", false));
	}

	@Test
	void keepsExplicitBold() {
		assertThat(objectMapper.readValue("{\"text\":\"Java\",\"bold\":true}", TextSpan.class))
				.isEqualTo(new TextSpan("Java", true));
	}

	@Test
	void parsesEntryBulletsWithoutBold() {
		String json = """
				{"name":"Diego","sections":[{"title":"Experiência","semanticType":"EXPERIENCE","kind":"ENTRIES",
				 "entries":[{"heading":"Dev","bullets":[[{"text":"Migração para Java 21."}]]}]}],"removedSkills":[]}""";

		StructuredResume resume = objectMapper.readValue(json, StructuredResume.class);

		assertThat(resume.sections().getFirst().entries().getFirst().bullets().getFirst())
				.containsExactly(new TextSpan("Migração para Java 21.", false));
	}
}
