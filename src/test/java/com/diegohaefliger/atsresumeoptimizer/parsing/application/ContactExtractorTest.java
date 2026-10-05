package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContactExtractorTest {

	private final ContactExtractor extractor = new ContactExtractor();

	@Test
	void extractsEmailPhoneAndLinkedIn() {
		String text = """
			Ana Silva
			ana.silva@example.com | (11) 99999-0000 | linkedin.com/in/ana-silva

			Experiência Profissional
			Desenvolvedora Backend Java.
			""";

		var contact = extractor.extract(text);

		assertThat(contact.email()).contains("ana.silva@example.com");
		assertThat(contact.phone()).contains("(11) 99999-0000");
		assertThat(contact.linkedInProfile()).contains("linkedin.com/in/ana-silva");
	}

	@Test
	void extractsPhoneWithTheDigitNineSeparatedFromTheRestByASpaceAndNormalizesTheDisplayFormat() {
		String text = "João Pessoa, PB, Brasil | (11) 9 8888-7777 | joao@exemplo.com";

		var contact = extractor.extract(text);

		assertThat(contact.phone()).contains("(11) 98888-7777");
	}

	@Test
	void extractsPhoneInVariousCommonBrazilianFormatsAndNormalizesAllOfThemTheSameWay() {
		assertThat(extractor.extract("Contato: 61999990000").phone()).contains("(61) 99999-0000");
		assertThat(extractor.extract("Fone: (11) 3222-1234").phone()).contains("(11) 3222-1234");
		assertThat(extractor.extract("+55 11 98888-7777").phone()).contains("(11) 98888-7777");
		assertThat(extractor.extract("11.9.8888.7777").phone()).contains("(11) 98888-7777");
	}

	@Test
	void doesNotFalselyMatchACpfOrCepAsAPhoneNumber() {
		assertThat(extractor.extract("CPF: 123.456.789-10").phone()).isEmpty();
		assertThat(extractor.extract("CEP: 58000-000").phone()).isEmpty();
	}

	@Test
	void extractsGithubAndLocation() {
		String text = """
			Ana Silva
			(55) 99999-0000 | ana@exemplo.com | linkedin.com/in/ana-silva | github.com/anasilva
			Panambi, RS, Brasil
			""";

		var contact = extractor.extract(text);

		assertThat(contact.githubProfile()).contains("github.com/anasilva");
		assertThat(contact.location()).contains("Panambi, RS");
	}

	@Test
	void extractsPortfolioButIgnoresLinkedInAndGithubUrls() {
		String text = """
			Ana Silva
			linkedin.com/in/ana-silva | github.com/anasilva | https://ana.dev/portfolio
			""";

		var contact = extractor.extract(text);

		assertThat(contact.portfolio()).contains("https://ana.dev/portfolio");
	}

	@Test
	void returnsEmptyWhenNoContactInfoIsPresent() {
		var contact = extractor.extract("Desenvolvedora Backend Java com experiência em Spring Boot.");

		assertThat(contact.email()).isEmpty();
		assertThat(contact.phone()).isEmpty();
		assertThat(contact.linkedInProfile()).isEmpty();
		assertThat(contact.githubProfile()).isEmpty();
		assertThat(contact.location()).isEmpty();
		assertThat(contact.portfolio()).isEmpty();
	}

	@Test
	void ignoresATechListThatWrapsIntoTwoCapitalLettersOnTheNextLine() {
		String text = """
			Ana Silva
			ana@exemplo.com · github.com/anasilva · Kibana, CI
			Tecnologias: Java, Grafana, Kibana,
			CI/CD
			""";

		assertThat(extractor.extract(text).location()).isEmpty();
	}

	@Test
	void ignoresACompanyCityInTheExperienceBody() {
		String text = """
			Ana Silva
			ana@email.com
			Desenvolvedora Backend
			Resumo profissional com foco em APIs.
			EXPERIÊNCIA PROFISSIONAL
			Analista de Sistemas
			Fockink, RS
			""";

		assertThat(extractor.extract(text).location()).isEmpty();
	}

	@Test
	void extractsACityWithAPrepositionInTheName() {
		assertThat(extractor.extract("Ana Silva\nSão José dos Campos, SP").location()).contains("São José dos Campos, SP");
	}
}
