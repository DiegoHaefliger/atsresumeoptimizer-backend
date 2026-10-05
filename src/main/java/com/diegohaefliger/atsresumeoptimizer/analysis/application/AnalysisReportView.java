package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import java.util.List;

public record AnalysisReportView(
		AnalysisHeaderView header,
		ScoreSummaryView score,
		KeywordsPanelView keywords,
		FindingsListView findings,
		List<String> blockers,
		String error,
		AnalysisJobView job,
		PreferenceMatch preferenceMatch) {
}
