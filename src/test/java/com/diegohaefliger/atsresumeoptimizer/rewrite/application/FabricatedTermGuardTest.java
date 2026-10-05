package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabulary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FabricatedTermGuardTest {

	private static final Map<String, List<String>> SPELLINGS = Map.of(
			"kubernetes", List.of("kubernetes", "k8s"),
			"terraform", List.of("terraform", "iac"));

	private static final TechVocabulary VOCABULARY = text -> {
		String normalized = NormalizedText.of(text);
		Set<String> found = new HashSet<>();
		SPELLINGS.forEach((canonical, forms) -> {
			if (forms.stream().anyMatch(form -> NormalizedText.containsWord(normalized, form))) {
				found.add(canonical);
			}
		});
		return found;
	};

	private static final String ORIGINAL = """
			RESUMO
			Desenvolvedora backend. Experiência com K8s.
			COMPETÊNCIAS
			Java, Docker, K8s
			EXPERIÊNCIA
			• Criação de pipelines de build com Docker.""";

	private final FabricatedTermGuard guard =
			new FabricatedTermGuard(VOCABULARY, ORIGINAL, List.of("Criação de pipelines de build com Docker."), List.of());

	@Test
	void dropsSkillsThatAreNotInTheOriginalButKeepsKnownAliases() {
		ResumeSection skills = new ResumeSection("COMPETÊNCIAS", ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Cloud", "Kubernetes, Terraform, AWS"), new KeyValueLine("Linguagens", "Java")),
				null, null);

		StructuredResume result = guard.apply(resume(skills));

		assertThat(result.sections().getFirst().keyValues())
				.containsExactly(new KeyValueLine("Cloud", "Kubernetes"), new KeyValueLine("Linguagens", "Java"));
	}

	@Test
	void revertsABulletThatMentionsANewTermToTheClosestOriginalBullet() {
		ResumeEntry entry = new ResumeEntry("Dev", null, null, null,
				List.of(List.of(new TextSpan("Criação de pipelines de build com Docker e Terraform.", false))),
				"Docker, Terraform");
		ResumeSection experience = new ResumeSection("EXPERIÊNCIA", ResumeSectionSemanticType.EXPERIENCE,
				ResumeSectionKind.ENTRIES, null, null, null, List.of(entry));

		ResumeEntry guarded = guard.apply(resume(experience)).sections().getFirst().entries().getFirst();

		assertThat(guarded.bullets())
				.containsExactly(List.of(new TextSpan("Criação de pipelines de build com Docker.", false)));
		assertThat(guarded.technologies()).isEqualTo("Docker");
	}

	@Test
	void removesOnlyTheSummarySentenceWithTheNewTerm() {
		ResumeSection summary = new ResumeSection("RESUMO", ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH,
				"Desenvolvedora backend com Kubernetes. Experiência com IaC em nuvem.", null, null, null);

		StructuredResume result = guard.apply(resume(summary));

		assertThat(result.sections().getFirst().paragraph()).isEqualTo("Desenvolvedora backend com Kubernetes.");
	}

	@Test
	void keepsContentThatIsFullyGrounded() {
		ResumeSection skills = new ResumeSection("COMPETÊNCIAS", ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Competências", "Java, Docker, K8s")), null, null);
		StructuredResume original = resume(skills);

		assertThat(guard.apply(original)).isEqualTo(original);
	}

	private StructuredResume resume(ResumeSection section) {
		return new StructuredResume("Ana", "Backend", List.of(section));
	}
}
