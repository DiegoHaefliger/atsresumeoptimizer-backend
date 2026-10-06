package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.PrioritySkill;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrioritySkillTest {

	private static KeywordMatch match(String term, boolean found, String... sections) {
		return new KeywordMatch(term, null, true, found, false, found ? 1 : 0, List.of(sections));
	}

	@Test
	void picksOnlyTheSelectedRequirementsTheCandidateHasAndFlagsThoseUsedInExperience() {
		List<KeywordMatch> matches = List.of(match("Java", true, "EXPERIENCIA", "COMPETÊNCIAS"),
				match("Kubernetes", false), match("Docker", true, "COMPETÊNCIAS"),
				match("mensageria", false), match("SQL", true, "EXPERIENCIA"), match("Go", true, "EXPERIENCIA"));
		List<Section> sections = List.of(new Section("EXPERIENCIA", true, "Integração com Kafka e SQL."));

		List<PrioritySkill> skills = PrioritySkillSelector.select(matches,
				List.of("Java", "Kubernetes", "Docker", "mensageria", "SQL"), List.of(new RequirementEvidence("mensageria", "Kafka")), sections);

		assertThat(skills).containsExactly(new PrioritySkill("Java", null, true),
				new PrioritySkill("Docker", null, false), new PrioritySkill("mensageria", "Kafka", true),
				new PrioritySkill("SQL", null, true));
	}

	@Test
	void reportsExperienceBackedSkillsMissingFromBullets() {
		ResumeEntry entry = new ResumeEntry("Dev", "01/2020 – atual", "Acme", null,
				List.of(List.of(new TextSpan("Construí APIs em Java.", false))), null, List.of());
		StructuredResume content = new StructuredResume("Ana", null, List.of(new ResumeSection("EXPERIÊNCIA",
				ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, List.of(), List.of(),
				List.of(entry))), List.of());

		List<PrioritySkill> missing = PrioritySkillCoverage.missing(content, List.of(
				new PrioritySkill("Java", null, true), new PrioritySkill("SQL", null, true),
				new PrioritySkill("Docker", null, false)));

		assertThat(missing).containsExactly(new PrioritySkill("SQL", null, true));
	}
}
