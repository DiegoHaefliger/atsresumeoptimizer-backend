package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {

	private final LogSanitizer sanitizer = new LogSanitizer();

	@Test
	void masksProviderKeysButKeepsTheirPrefix() {
		String cleaned = sanitizer.clean("Incorrect API key provided: sk-proj-abcdEFGH1234ijkl e AIzaSyD-1234567890abcdefgh");

		assertThat(cleaned).contains("sk-proj-***").contains("AIza***").doesNotContain("abcdEFGH1234").doesNotContain("SyD-1234");
	}

	@Test
	void masksValuesOfSecretLookingKeys() {
		String cleaned = sanitizer.clean("{\"apiKey\": \"segredo123\", password=hunter22; Authorization: Bearer abc.def.ghijkl");

		assertThat(cleaned).doesNotContain("segredo123").doesNotContain("hunter22").doesNotContain("abc.def.ghijkl");
	}

	@Test
	void masksEmailsAndUrlPasswords() {
		String cleaned = sanitizer.clean("contato maria.silva@exemplo.com.br em postgresql://ats:minhasenha@db.exemplo.com/ats");

		assertThat(cleaned).doesNotContain("maria.silva").doesNotContain("minhasenha").contains("postgresql://ats:***@");
	}

	@Test
	void keepsOrdinaryLogLinesIntact() {
		String line = "2026-11-25T10:00:00 INFO [K7Q3XM2A] c.d.a.ai.LangChain4jAiPort : maxOutputTokens=2000 em 1200ms";

		assertThat(sanitizer.clean(line)).isEqualTo(line);
	}
}
