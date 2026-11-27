package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntrySubheadingSplitterTest {

	private static String split(String subheading) {
		ResumeEntry entry = new ResumeEntry("Analista", "2020 – 2023", subheading, null, List.of(), null);
		StructuredResume content = new StructuredResume("Ana", null, List.of(new ResumeSection("Experiência",
				ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null, List.of(entry))));
		return EntrySubheadingSplitter.apply(content).sections().getFirst().entries().getFirst().subheading();
	}

	@Test
	void separatesTheCityAndStateFromTheCompany() {
		assertThat(split("Acme Ltda - São Paulo/SP")).isEqualTo("Acme Ltda | São Paulo/SP");
		assertThat(split("Acme, Rio de Janeiro, RJ")).isEqualTo("Acme | Rio de Janeiro, RJ");
		assertThat(split("Coca-Cola — Porto Alegre - RS")).isEqualTo("Coca-Cola | Porto Alegre - RS");
	}

	@Test
	void leavesSubheadingsItCannotSplitSafely() {
		assertThat(split("Acme | Curitiba, PR")).isEqualTo("Acme | Curitiba, PR");
		assertThat(split("Coca-Cola")).isEqualTo("Coca-Cola");
		assertThat(split("Acme Remoto")).isEqualTo("Acme Remoto");
	}
}
