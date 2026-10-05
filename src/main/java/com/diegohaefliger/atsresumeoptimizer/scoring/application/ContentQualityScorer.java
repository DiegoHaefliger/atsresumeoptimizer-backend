package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.BulletReview;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class ContentQualityScorer implements DimensionScorer {

	private static final Set<String> WEAK_VERBS =
			Set.of("auxiliei", "participei", "ajudei", "colaborei", "responsavel por", "responsável por");
	private static final int WEAK_VERB_PENALTY = 5;
	private static final int MAX_WEAK_VERB_PENALTY = 40;
	private static final double LOW_IMPACT_THRESHOLD = 3.0;
	private static final int LOW_IMPACT_PENALTY = 5;
	private static final int MAX_SCORE = 100;
	private static final Pattern LEADING_BULLET_MARKER = Pattern.compile("^[•\\-*◦\\d.)\\s]+");

	@Override
	public Dimension dimension() {
		return Dimension.CONTENT_QUALITY;
	}

	@Override
	public DimensionResult score(ScoringContext context) {
		List<Finding> findings = new ArrayList<>();
		int score = MAX_SCORE;

		long weakVerbLines = context.parsing().sections().stream()
				.flatMap(section -> section.content().lines())
				.filter(this::startsWithWeakVerb)
				.count();
		if (weakVerbLines > 0) {
			findings.add(Finding.of(FindingCode.WEAK_ACTION_VERB,
					"%d linha(s) começam com verbo fraco (\"auxiliei\", \"responsável por\"...).".formatted(weakVerbLines),
					"Comece cada bullet com um verbo de ação forte no passado (\"Liderei\", \"Implementei\", \"Reduzi\")."));
			score -= (int) Math.min(MAX_WEAK_VERB_PENALTY, weakVerbLines * WEAK_VERB_PENALTY);
		}

		for (BulletReview review : context.bulletReviews()) {
			double average = (review.impact() + review.clarity() + review.specificity()) / 3.0;
			if (average < LOW_IMPACT_THRESHOLD) {
				findings.add(Finding.of(FindingCode.BULLET_LOW_IMPACT,
						"Bullet pouco específico: \"%s\"".formatted(review.bullet()), review.suggestion()));
				score -= LOW_IMPACT_PENALTY;
			}
		}

		return DimensionResult.of(Dimension.CONTENT_QUALITY, Math.max(0, score), findings);
	}

	private boolean startsWithWeakVerb(String line) {
		String normalized = NormalizedText.of(line);
		String withoutBulletMarker = LEADING_BULLET_MARKER.matcher(normalized).replaceFirst("");
		return WEAK_VERBS.stream().anyMatch(verb -> withoutBulletMarker.startsWith(verb));
	}
}
