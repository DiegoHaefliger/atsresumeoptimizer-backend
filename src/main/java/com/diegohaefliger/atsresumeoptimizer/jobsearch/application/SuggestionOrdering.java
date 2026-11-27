package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionSort;

final class SuggestionOrdering {

	private static final Order SCORE_DESC = Order.desc("score").nullsLast();
	private static final Order POSTED_DESC = Order.desc("postedOn").nullsLast();
	private static final Order FOUND_DESC = Order.desc("foundAt");
	private static final Order STABLE = Order.asc("id");

	private static final Map<SuggestionSort, Sort> SORTS = Map.of(
			SuggestionSort.SCORE_DESC, Sort.by(SCORE_DESC, POSTED_DESC, FOUND_DESC, STABLE),
			SuggestionSort.SCORE_ASC, Sort.by(Order.asc("score").nullsLast(), POSTED_DESC, FOUND_DESC, STABLE),
			SuggestionSort.POSTED_DESC, Sort.by(POSTED_DESC, SCORE_DESC, FOUND_DESC, STABLE),
			SuggestionSort.POSTED_ASC, Sort.by(Order.asc("postedOn").nullsLast(), SCORE_DESC, FOUND_DESC, STABLE),
			SuggestionSort.FOUND_DESC, Sort.by(FOUND_DESC, SCORE_DESC, STABLE));

	private SuggestionOrdering() {
	}

	static Sort of(SuggestionSort sort) {
		return SORTS.get(sort == null ? SuggestionSort.SCORE_DESC : sort);
	}
}
