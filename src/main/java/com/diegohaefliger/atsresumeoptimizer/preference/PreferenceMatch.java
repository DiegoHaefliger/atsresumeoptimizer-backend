package com.diegohaefliger.atsresumeoptimizer.preference;

import java.util.List;

/** {@code score} nulo quando a vaga não informa nada do que o candidato definiu. */
public record PreferenceMatch(Integer score, List<CriterionMatch> criteria) {

	public PreferenceMatch {
		criteria = List.copyOf(criteria);
	}
}
