package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuplicatedContextCleanerTest {

	private static final String FIRST_BULLET = "Atendimento a clientes e controle de estoque da loja.";

	private static String contextAfterCleaning(String context) {
		ResumeEntry entry = new ResumeEntry("Vendedora", "2020 – 2023", "Loja | Curitiba, PR", context,
				List.of(List.of(new TextSpan(FIRST_BULLET, false)), List.of(new TextSpan("Abertura e fechamento de caixa.", false))),
				null);
		StructuredResume content = new StructuredResume("Ana", null, List.of(new ResumeSection("Experiência",
				ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null, List.of(entry))));
		return DuplicatedContextCleaner.apply(content).sections().getFirst().entries().getFirst().context();
	}

	@Test
	void dropsAContextThatRepeatsABullet() {
		assertThat(contextAfterCleaning("Atendimento a clientes e controle de estoque da loja")).isNull();
	}

	@Test
	void keepsAContextThatOnlyMentionsPartOfABullet() {
		assertThat(contextAfterCleaning("Controle de estoque")).isEqualTo("Controle de estoque");
		assertThat(contextAfterCleaning("Loja de departamentos com 40 funcionários."))
				.isEqualTo("Loja de departamentos com 40 funcionários.");
	}
}
