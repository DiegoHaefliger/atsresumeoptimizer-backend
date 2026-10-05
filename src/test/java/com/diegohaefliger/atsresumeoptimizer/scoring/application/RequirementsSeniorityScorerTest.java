package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RequirementsSeniorityScorerTest {

	private final RequirementsSeniorityScorer scorer = new RequirementsSeniorityScorer();

	@Test
	void doesNotPenalizeOngoingRoleWithoutAnExplicitEndYear() {
		int startYear = Year.now().getValue() - 5;
		ScoringContext context = contextWithRawText("""
				Desenvolvedora Backend Java na Empresa X, %d até o momento atual.
				""".formatted(startYear), 3);

		DimensionResult result = scorer.score(context);

		assertThat(result.findings()).noneMatch(finding -> finding.code() == FindingCode.REQUIREMENTS_MIN_EXPERIENCE_NOT_MET);
		assertThat(result.score()).isEqualTo(100);
	}

	@Test
	void stillFlagsShortOngoingRoleThatDoesNotMeetTheMinimum() {
		int startYear = Year.now().getValue() - 1;
		ScoringContext context = contextWithRawText("""
				Desenvolvedora Backend Java na Empresa X, %d até o momento atual.
				""".formatted(startYear), 3);

		DimensionResult result = scorer.score(context);

		assertThat(result.findings()).anyMatch(finding -> finding.code() == FindingCode.REQUIREMENTS_MIN_EXPERIENCE_NOT_MET);
	}

	private ScoringContext contextWithRawText(String rawText, int minYearsExperience) {
		NormalizedDocument document = new NormalizedDocument(SourceFormat.PDF, rawText, "", 1);
		ParsingSignals signals = new ParsingSignals(false, false, false, false, false, false, false, false);
		ParsingResult parsingResult = new ParsingResult(document, signals, List.of(),
				new ContactInfo(
						Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		JobStructured job = new JobStructured("Backend Java", "pleno", minYearsExperience, null, List.of(), List.of(), Map.of());
		return new ScoringContext(parsingResult, 1, job, List.of());
	}
}
