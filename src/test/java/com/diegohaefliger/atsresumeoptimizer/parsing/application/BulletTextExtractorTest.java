package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.List;
import org.junit.jupiter.api.Test;

class BulletTextExtractorTest {

	private final BulletTextExtractor extractor = new BulletTextExtractor();

	@Test
	void onlyExtractsLinesThatOpenWithABulletMarker() {
		String content = String.join("\n",
				"Desenvolvedora Backend Sênior — Jan 2020 – Dez 2022",
				"Empresa X | São Paulo, SP",
				"• Desenvolvimento e sustentação de microsserviços back-end do domínio de cartões, com foco em",
				"estabilidade em produção.",
				"• Liderança técnica do squad.",
				"Tecnologias: Java, Spring Boot");
		Section section = new Section("EXPERIENCIA", true, content);

		List<String> bullets = extractor.extract(List.of(section));

		assertThat(bullets).containsExactly(
				"• Desenvolvimento e sustentação de microsserviços back-end do domínio de cartões, com foco em estabilidade em produção.",
				"• Liderança técnica do squad.");
	}

	@Test
	void ignoresContentFromUnrecognizedSections() {
		Section section = new Section("DIEGO HAEFLIGER", false, "• Bullet dentro de uma seção não reconhecida");

		List<String> bullets = extractor.extract(List.of(section));

		assertThat(bullets).isEmpty();
	}

	@Test
	void recognizesHyphenAndNumberedMarkersToo() {
		Section section = new Section("EXPERIENCIA", true, String.join("\n",
				"- Bullet com hífen",
				"1. Bullet numerado",
				"2) Outro bullet numerado"));

		List<String> bullets = extractor.extract(List.of(section));

		assertThat(bullets).containsExactly("- Bullet com hífen", "1. Bullet numerado", "2) Outro bullet numerado");
	}

	@Test
	void recognizesTheMarkerEvenWithAZeroWidthSpaceRightAfterIt() {
		// Word/Google Docs exportam bullet como "•​ " — o zero-width space não é \s, sem
		// sanitizar isso o marcador nunca casa e o bullet inteiro passa batido.
		Section section = new Section("EXPERIENCIA", true, "•​ Migração de 5 projetos legados de Java 8 para Java 21.");

		List<String> bullets = extractor.extract(List.of(section));

		assertThat(bullets).containsExactly("• Migração de 5 projetos legados de Java 8 para Java 21.");
	}

	@Test
	void stripsTheLeadingMarkerFromABullet() {
		assertThat(extractor.stripMarker("• Liderança técnica do squad.")).isEqualTo("Liderança técnica do squad.");
		assertThat(extractor.stripMarker("- Bullet com hífen")).isEqualTo("Bullet com hífen");
		assertThat(extractor.stripMarker("1. Bullet numerado")).isEqualTo("Bullet numerado");
		assertThat(extractor.stripMarker("Sem marcador nenhum")).isEqualTo("Sem marcador nenhum");
	}

	@Test
	void groupsBulletsByTheJobHeaderAboveThem() {
		Section section = new Section("Experiência", true, """
				Analista — 2012 – 2022
				• Reescrita do legado.
				• Suporte ao comercial.
				Tecnologias: Java, Delphi
				Programador — 2011 – 2012
				• Módulos de ERP.""");

		assertThat(extractor.extractGroups(List.of(section)))
				.containsExactly(List.of("• Reescrita do legado.", "• Suporte ao comercial."), List.of("• Módulos de ERP."));
	}
}
