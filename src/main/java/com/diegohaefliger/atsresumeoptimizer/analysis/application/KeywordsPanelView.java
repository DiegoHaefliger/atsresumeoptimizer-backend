package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;

public record KeywordsPanelView(List<String> found, List<String> missing, List<String> semanticOnly) {
}
