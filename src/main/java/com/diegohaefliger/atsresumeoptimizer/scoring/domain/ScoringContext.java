package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import com.diegohaefliger.atsresumeoptimizer.ai.BulletReview;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import java.util.List;

/** {@code job} nulo no modo GENERAL sem cargo-alvo; {@code bulletReviews} vazio quando a IA não respondeu. */
public record ScoringContext(ParsingResult parsing, Integer pageCount, JobStructured job, List<BulletReview> bulletReviews) {

	public ScoringContext {
		bulletReviews = bulletReviews == null ? List.of() : List.copyOf(bulletReviews);
	}
}
