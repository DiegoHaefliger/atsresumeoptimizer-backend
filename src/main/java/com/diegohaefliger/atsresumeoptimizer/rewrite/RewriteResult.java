package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.List;
import java.util.UUID;

public record RewriteResult(
		UUID resumeId,
		UUID docxResumeVersionId,
		UUID pdfResumeVersionId,
		List<BulletRewriteView> bullets,
		Integer scoreBefore,
		Integer scoreAfter,
		boolean jobHighlighted,
		List<RemovedSkillView> removedSkills,
		StructuredResume content,
		ResumeContact contact,
		List<OriginalSection> originalSections,
		List<RequirementEvidence> evidencedRequirements,
		String aiProvider,
		String aiModel) {

	public RewriteResult {
		bullets = bullets == null ? List.of() : List.copyOf(bullets);
		removedSkills = removedSkills == null ? List.of() : List.copyOf(removedSkills);
		originalSections = originalSections == null ? List.of() : List.copyOf(originalSections);
		evidencedRequirements = evidencedRequirements == null ? List.of() : List.copyOf(evidencedRequirements);
	}
}
