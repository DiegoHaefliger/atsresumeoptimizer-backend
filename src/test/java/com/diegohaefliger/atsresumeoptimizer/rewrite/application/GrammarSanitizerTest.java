package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GrammarSanitizerTest {

	@Test
	void collapsesDoubleSpacesIntoOne() {
		assertThat(GrammarSanitizer.sanitize("Desenvolvedora  Backend   Java")).isEqualTo("Desenvolvedora Backend Java");
	}

	@Test
	void replacesHyphenBeforeABrazilianStateCodeWithAnEnDash() {
		assertThat(GrammarSanitizer.sanitize("Panambi-RS")).isEqualTo("Panambi–RS");
		assertThat(GrammarSanitizer.sanitize("Porto - SP")).isEqualTo("Porto–SP");
	}

	@Test
	void replacesCommaBeforeABrazilianStateCodeWithAnEnDashToo() {
		// "Cidade, UF" é o formato que o próprio prompt de estruturação pede pro subheading — e é
		// exatamente o que o LanguageTool real aponta como "prefira uma meia-risca" nesse projeto.
		assertThat(GrammarSanitizer.sanitize("Panambi, RS")).isEqualTo("Panambi–RS");
		assertThat(GrammarSanitizer.sanitize("Empresa X | São Paulo, SP (remoto)"))
				.isEqualTo("Empresa X | São Paulo–SP (remoto)");
	}

	@Test
	void leavesAHyphenThatIsNotFollowedByAStateCodeAlone() {
		assertThat(GrammarSanitizer.sanitize("Jan 2020 - Dez 2022")).isEqualTo("Jan 2020 - Dez 2022");
		assertThat(GrammarSanitizer.sanitize("front-end")).isEqualTo("front-end");
	}

	@Test
	void returnsNullAndBlankUnchanged() {
		assertThat(GrammarSanitizer.sanitize(null)).isNull();
		assertThat(GrammarSanitizer.sanitize("")).isEqualTo("");
	}
}
