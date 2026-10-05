package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;

public record AnalysisJobView(String title, String company, String sourceUrl, String interviewUrl, WorkModel workModel) {
}
