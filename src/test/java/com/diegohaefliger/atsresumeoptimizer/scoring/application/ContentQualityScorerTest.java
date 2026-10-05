package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContentQualityScorerTest {

	private final ContentQualityScorer scorer = new ContentQualityScorer();

	@Test
	void detectsWeakVerbBehindABulletMarker() {
		ScoringContext context = contextWithSectionContent("""
				• Auxiliei o time de backend a migrar o serviço de pagamentos
				- Responsável por rotina de deploy
				""");

		DimensionResult result = scorer.score(context);

		assertThat(result.findings()).anyMatch(finding -> finding.code() == FindingCode.WEAK_ACTION_VERB);
		assertThat(result.score()).isLessThan(100);
	}

	@Test
	void doesNotFlagAStrongActionVerbBehindABulletMarker() {
		ScoringContext context = contextWithSectionContent("• Liderei a migração do serviço de pagamentos");

		DimensionResult result = scorer.score(context);

		assertThat(result.findings()).noneMatch(finding -> finding.code() == FindingCode.WEAK_ACTION_VERB);
		assertThat(result.score()).isEqualTo(100);
	}

	private ScoringContext contextWithSectionContent(String sectionContent) {
		NormalizedDocument document = new NormalizedDocument(SourceFormat.PDF, sectionContent, "", 1);
		ParsingSignals signals = new ParsingSignals(false, false, false, false, false, false, false, false);
		List<Section> sections = List.of(new Section("Experiência Profissional", true, sectionContent));
		ParsingResult parsingResult = new ParsingResult(document, signals, sections,
				new ContactInfo(
						Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		JobStructured job = new JobStructured("Backend", "pleno", null, null, List.of(), List.of(), java.util.Map.of());
		return new ScoringContext(parsingResult, 1, job, List.of());
	}
}
