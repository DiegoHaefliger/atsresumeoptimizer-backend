package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;

/** {@code terms} é a lista que vale pra vaga, na ordem de importância; só uma análise de vaga pode editá-la. */
public record KeywordsPanelView(List<String> found, List<String> missing, List<String> semanticOnly,
		List<String> terms, List<String> selected, boolean editable) {
}
