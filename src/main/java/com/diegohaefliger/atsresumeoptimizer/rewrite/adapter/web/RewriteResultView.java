package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.OriginalSection;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import java.util.List;
import java.util.UUID;

public record RewriteResultView(
		UUID resumeId,
		DocumentDownloadsView documents,
		BulletsPanelView bullets,
		ScoreComparisonView scoreComparison,
		boolean jobHighlighted,
		List<RemovedSkillResponse> removedSkills,
		StructuredResume content,
		ResumeContact contact,
		List<OriginalSection> originalSections,
		List<RequirementEvidence> evidencedRequirements,
		String aiProvider,
		String aiModel) {
}
