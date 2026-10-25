package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.Optional;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class TolerantJson {

	private static final JsonMapper MAPPER = JsonMapper.builder()
			.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
			.build();

	private TolerantJson() {
	}

	static Optional<JsonNode> parse(String json) {
		try {
			return Optional.of(MAPPER.readTree(json));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}
}
