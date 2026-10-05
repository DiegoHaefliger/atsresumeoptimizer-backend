package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.language.application.PortugueseGrammarChecker;
import com.diegohaefliger.atsresumeoptimizer.language.domain.GrammarCheckException;
import com.diegohaefliger.atsresumeoptimizer.language.domain.GrammarIssue;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LanguageScorer implements DimensionScorer {

	private static final Logger LOGGER = LoggerFactory.getLogger(LanguageScorer.class);
	private static final int NEUTRAL_SCORE_ON_CHECKER_FAILURE = 100;
	private static final int MAX_FINDINGS = 20;
	private static final int PENALTY_PER_ISSUE = 2;
	private static final int MAX_PENALTY = 80;

	private final PortugueseGrammarChecker grammarChecker;

	LanguageScorer(PortugueseGrammarChecker grammarChecker) {
		this.grammarChecker = grammarChecker;
	}

	@Override
	public Dimension dimension() {
		return Dimension.LANGUAGE;
	}

	@Override
	public DimensionResult score(ScoringContext context) {
		List<GrammarIssue> issues;
		try {
			issues = grammarChecker.check(context.parsing().document().rawText());
		} catch (GrammarCheckException exception) {
			LOGGER.warn("Verificação de ortografia/gramática falhou, D7 sai sem penalidade nessa análise", exception);
			return DimensionResult.of(Dimension.LANGUAGE, NEUTRAL_SCORE_ON_CHECKER_FAILURE, List.of());
		}

		List<Finding> findings = issues.stream()
				.limit(MAX_FINDINGS)
				.map(issue -> new Finding(FindingCode.SPELLING_GRAMMAR_ERROR, FindingCode.SPELLING_GRAMMAR_ERROR.defaultSeverity(),
						issue.message(),
						issue.suggestedReplacements().isEmpty() ? null : String.join(", ", issue.suggestedReplacements()),
						issue.startOffset(), issue.endOffset(), null))
				.toList();

		int score = Math.max(0, 100 - Math.min(MAX_PENALTY, issues.size() * PENALTY_PER_ISSUE));
		return DimensionResult.of(Dimension.LANGUAGE, score, findings);
	}
}
