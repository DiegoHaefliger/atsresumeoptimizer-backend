package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.application.SensitiveDataDetector;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContactDataScorerTest {

	private final ContactDataScorer scorer = new ContactDataScorer(new SensitiveDataDetector());

	@Test
	void flagsAValidCpfLeftInTheResume() {
		ScoringContext context = contextWithRawText("Ana Silva, CPF 111.444.777-35, desenvolvedora backend.");

		DimensionResult result = scorer.score(context);

		assertThat(result.findings()).anyMatch(finding -> finding.code() == FindingCode.SENSITIVE_DATA_PRESENT);
	}

	@Test
	void doesNotFlagAResumeWithoutSensitiveData() {
		ScoringContext context = contextWithRawText("Ana Silva, desenvolvedora backend com experiência em Java.");

		DimensionResult result = scorer.score(context);

		assertThat(result.findings()).isEmpty();
	}

	private ScoringContext contextWithRawText(String rawText) {
		NormalizedDocument document = new NormalizedDocument(SourceFormat.PDF, rawText, rawText, 1);
		ParsingResult parsingResult = new ParsingResult(document,
				new ParsingSignals(false, false, false, false, false, false, false, false), List.of(),
				new ContactInfo(Optional.of("ana@email.com"), Optional.of("11999999999"), Optional.empty(), Optional.empty(),
					Optional.empty(), Optional.empty()));
		return new ScoringContext(parsingResult, 1, null, List.of());
	}
}
