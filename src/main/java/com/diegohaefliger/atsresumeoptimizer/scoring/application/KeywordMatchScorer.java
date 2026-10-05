package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.JobKeywordMatcher;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class KeywordMatchScorer implements DimensionScorer {

	private static final int STUFFING_THRESHOLD = 8;

	private final JobKeywordMatcher keywordMatcher;

	KeywordMatchScorer(JobKeywordMatcher keywordMatcher) {
		this.keywordMatcher = keywordMatcher;
	}

	@Override
	public Dimension dimension() {
		return Dimension.KEYWORD_MATCH;
	}

	@Override
	public DimensionResult score(ScoringContext context) {
		JobStructured job = context.job();
		List<KeywordMatch> matches = keywordMatcher.match(context.parsing(), job);
		if (matches.isEmpty()) {
			return new DimensionResult(Dimension.KEYWORD_MATCH, 0, List.of(), List.of());
		}

		List<Finding> findings = new ArrayList<>();
		for (KeywordMatch match : matches) {
			if (!match.found()) {
				findings.add(Finding.of(FindingCode.KEYWORD_REQUIRED_MISSING,
						"A palavra-chave \"%s\", exigida pela vaga, não aparece no currículo.".formatted(match.term()),
						"Inclua \"%s\" no currículo, se você realmente tiver essa experiência.".formatted(match.term())));
			}
			if (match.occurrences() > STUFFING_THRESHOLD) {
				findings.add(Finding.of(FindingCode.KEYWORD_STUFFING,
						"\"%s\" aparece %d vezes — pode parecer keyword stuffing.".formatted(match.term(), match.occurrences()),
						"Reduza a repetição da palavra-chave para um uso mais natural."));
			}
		}

		long foundCount = matches.stream().filter(KeywordMatch::found).count();
		int score = (int) Math.round(100.0 * foundCount / matches.size());
		return new DimensionResult(Dimension.KEYWORD_MATCH, score, findings, matches);
	}
}
