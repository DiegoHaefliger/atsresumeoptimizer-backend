package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeywordMatchScorerTest {

	@Mock
	private TechTermRepository techTermRepository;

	@Test
	void matchesKeywordWithDifferentSeparatorSpellingFromTheJobPosting() {
		when(techTermRepository.findAll()).thenReturn(List.of());
		KeywordMatchScorer scorer = new KeywordMatchScorer(new KeywordMatcher(techTermRepository));

		ScoringContext context = contextWithResumeTextAndKeywords("Experiência com NodeJS em produção.", List.of("Node.js"));

		DimensionResult result = scorer.score(context);

		assertThat(result.score()).isEqualTo(100);
		KeywordMatch match = result.keywordMatches().get(0);
		assertThat(match.found()).isTrue();
	}

	@Test
	void doesNotMatchAMultiWordKeywordWhoseParticlesAppearFarApart() {
		when(techTermRepository.findAll()).thenReturn(List.of());
		KeywordMatchScorer scorer = new KeywordMatchScorer(new KeywordMatcher(techTermRepository));

		ScoringContext context = contextWithResumeTextAndKeywords(
				"Trabalho com CI para outros produtos, não uso CD físico.", List.of("CI/CD"));

		DimensionResult result = scorer.score(context);

		assertThat(result.score()).isZero();
	}

	private ScoringContext contextWithResumeTextAndKeywords(String rawText, List<String> requiredKeywords) {
		NormalizedDocument document = new NormalizedDocument(SourceFormat.PDF, rawText, "", 1);
		ParsingSignals signals = new ParsingSignals(false, false, false, false, false, false, false, false);
		ParsingResult parsingResult = new ParsingResult(document, signals, List.of(),
				new ContactInfo(
						Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		JobStructured job = new JobStructured("Backend", "pleno", null, null, List.of(), requiredKeywords, Map.of());
		return new ScoringContext(parsingResult, 1, job, List.of());
	}
}
