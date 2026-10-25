package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class HtmlTextTest {

	@Test
	void turnsEscapedNonBreakingSpacesIntoSpaces() {
		String text = HtmlText.plain(Jsoup.parse("<p>Experiência em&nbsp;Java</p>").body());

		assertThat(text).isEqualTo("Experiência em Java");
	}

	@Test
	void decodesAnEntityThatWasEscapedTwiceBySource() {
		String text = HtmlText.plain(Jsoup.parse("<p>Java&amp;nbsp;e&amp;nbsp;Spring &amp;amp; Kotlin</p>").body());

		assertThat(text).isEqualTo("Java e Spring & Kotlin");
	}

	@Test
	void decodesEntitiesInTextThatHasNoTags() {
		assertThat(HtmlText.plain(Jsoup.parse("Realizar a integração;&nbsp;Definir a estrutura").body()))
				.isEqualTo("Realizar a integração; Definir a estrutura");
	}
}
