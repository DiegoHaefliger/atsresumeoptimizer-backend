package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkedInPostParserTest {

	@Test
	void readsTheFullTextFromTheTextSegments() {
		String html = """
				<html><body>
				<p class="attributed-text-segment-list__content text-color-text">Estamos contratando dev Java!</p>
				<p class="attributed-text-segment-list__content text-color-text">Remoto, PJ, 12 a 16 mil.</p>
				</body></html>
				""";

		assertThat(LinkedInPostParser.parse(html))
				.contains("Estamos contratando dev Java!\n\nRemoto, PJ, 12 a 16 mil.");
	}

	@Test
	void fallsBackToTheOpenGraphDescription() {
		String html = "<html><head><meta property=\"og:description\" content=\"Vaga Java remota\"></head></html>";

		assertThat(LinkedInPostParser.parse(html)).contains("Vaga Java remota");
	}

	@Test
	void ignoresTheLoginWall() {
		assertThat(LinkedInPostParser.parse("<html><a href=\"/authwall?trk=x\">Entrar</a></html>")).isEmpty();
	}

	@Test
	void ignoresEmptyPages() {
		assertThat(LinkedInPostParser.parse(null)).isEmpty();
		assertThat(LinkedInPostParser.parse("<html><body></body></html>")).isEmpty();
	}
}
