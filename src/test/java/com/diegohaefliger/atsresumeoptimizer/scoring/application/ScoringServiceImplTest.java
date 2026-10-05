package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.language.application.PortugueseGrammarChecker;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.SensitiveDataDetector;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringOutcome;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileV1;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringServiceImplTest {

	@Mock
	private TechTermRepository techTermRepository;

	@Test
	void scoresAJobMatchAnalysisAcrossAllSevenDimensionsAndCollectsBlockers() {
		when(techTermRepository.findAll()).thenReturn(List.of());

		List<DimensionScorer> scorers = List.of(
				new ParseabilityScorer(), new StructureSectionsScorer(), new ContactDataScorer(new SensitiveDataDetector()),
				new KeywordMatchScorer(new KeywordMatcher(techTermRepository)), new RequirementsSeniorityScorer(),
				new ContentQualityScorer(), new LanguageScorer(new PortugueseGrammarChecker()));
		ScoringServiceImpl scoringService = new ScoringServiceImpl(scorers);

		NormalizedDocument document = new NormalizedDocument(SourceFormat.PDF, """
				Ana Silva
				ana.silva@email.com

				EXPERIENCIA PROFISSIONAL
				Desenvolvedora Backend Java na Empresa X, 2020 a 2023
				""", "", 1);
		ParsingSignals signals = new ParsingSignals(false, false, false, false, false, false, false, false);
		List<Section> sections = List.of(new Section("EXPERIENCIA PROFISSIONAL", true,
				"Desenvolvedora Backend Java na Empresa X, 2020 a 2023"));
		ContactInfo contact = new ContactInfo(Optional.of("ana.silva@email.com"), Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty(), Optional.empty());
		ParsingResult parsingResult = new ParsingResult(document, signals, sections, contact);

		JobStructured job = new JobStructured("Backend Java", "pleno", 3, null, List.of("inglês"),
				List.of("Java", "Kafka"), Map.of());
		ScoringContext context = new ScoringContext(parsingResult, 1, job, List.of());

		ScoringOutcome outcome = scoringService.score(ScoringProfileV1.JOB_MATCH, context);

		assertThat(outcome.overallScore()).isBetween(0, 100);
		assertThat(outcome.dimensions()).hasSize(7);
		assertThat(outcome.blockers()).contains(FindingCode.KEYWORD_REQUIRED_MISSING);
	}
}
