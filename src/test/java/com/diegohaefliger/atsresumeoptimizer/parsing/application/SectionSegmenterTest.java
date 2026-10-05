package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectionSegmenterTest {

	private final SectionSegmenter segmenter = new SectionSegmenter();

	@Test
	void splitsRecognizedSectionsFromTheDictionary() {
		String text = """
			Ana Silva
			ana.silva@example.com

			Experiência Profissional
			Desenvolvedora Backend na Empresa X, 2020 a 2023.

			Formação Acadêmica
			Ciência da Computação, Universidade Y.
			""";

		List<Section> sections = segmenter.segment(text);

		assertThat(sections).hasSize(2);
		assertThat(sections.get(0).title()).isEqualTo("Experiência Profissional");
		assertThat(sections.get(0).recognized()).isTrue();
		assertThat(sections.get(0).content()).contains("Desenvolvedora Backend na Empresa X, 2020 a 2023.");
		assertThat(sections.get(1).title()).isEqualTo("Formação Acadêmica");
		assertThat(sections.get(1).recognized()).isTrue();
	}

	@Test
	void allCapsHeadingNotInDictionaryIsUnrecognized() {
		String text = """
			Experiência Profissional
			Desenvolvedora Backend.

			OUTRAS INFORMAÇÕES
			Disponível para viagens.
			""";

		List<Section> sections = segmenter.segment(text);

		assertThat(sections).hasSize(2);
		Section unrecognized = sections.get(1);
		assertThat(unrecognized.title()).isEqualTo("OUTRAS INFORMAÇÕES");
		assertThat(unrecognized.recognized()).isFalse();
	}

	@Test
	void recognizesCommonHeadingVariantsWithNumberingPunctuationOrPlural() {
		String text = """
			1. Experiências Profissionais
			Desenvolvedora Backend.

			Formações Acadêmicas:
			Ciência da Computação.

			• Cursos e Certificações
			AWS Certified.
			""";

		List<Section> sections = segmenter.segment(text);

		assertThat(sections).hasSize(3);
		assertThat(sections).allMatch(Section::recognized);
	}

	@Test
	void plainSentencesInTitleCaseAreNotTreatedAsHeadings() {
		String text = """
			Experiência Profissional
			Desenvolvedora Backend Java
			Empresa X - 2020 a 2023
			""";

		List<Section> sections = segmenter.segment(text);

		assertThat(sections).hasSize(1);
		assertThat(sections.get(0).content()).contains("Desenvolvedora Backend Java").contains("Empresa X - 2020 a 2023");
	}

	@Test
	void singleWordAcronymWrappedOntoItsOwnLineIsNotTreatedAsATitle() {
		String text = """
			Experiência Profissional
			Tecnologias: Java, Spring Boot, Docker, Kubernetes, AWS, Swagger/OpenAPI, Dynatrace, Grafana, Kibana,
			CI/CD
			""";

		List<Section> sections = segmenter.segment(text);

		assertThat(sections).hasSize(1);
		assertThat(sections.get(0).title()).isEqualTo("Experiência Profissional");
		assertThat(sections.get(0).content()).contains("CI/CD");
	}

	@Test
	void aLineRepeatingTheTitleRightAfterItIsContentNotASecondSection() {
		String text = """
			OBJETIVO
			Objetivo
			RESUMO PROFISSIONAL
			Resumo profissional
			Texto real do resumo
			""";

		List<Section> sections = segmenter.segment(text);

		assertThat(sections).extracting(Section::title).containsExactly("OBJETIVO", "RESUMO PROFISSIONAL");
		assertThat(sections.get(0).content()).isEqualTo("Objetivo");
		assertThat(sections.get(1).content()).isEqualTo("Resumo profissional\nTexto real do resumo");
	}
}
