package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.rewrite.BulletRewriteView;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.EditableResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RemovedSkillView;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface RewriteWebMapper {

	@Mapping(target = "documents", source = ".")
	@Mapping(target = "scoreComparison", source = ".")
	RewriteResultView toView(RewriteResult result);

	@Mapping(target = "documents", source = ".")
	EditedDocumentsView toView(ComposedResume composed);

	EditableResumeView toView(EditableResume editable);

	@Mapping(target = "docxVersionId", source = "docxResumeVersionId")
	@Mapping(target = "pdfVersionId", source = "pdfResumeVersionId")
	DocumentDownloadsView toDownloads(RewriteResult result);

	@Mapping(target = "docxVersionId", source = "docxResumeVersionId")
	@Mapping(target = "pdfVersionId", source = "pdfResumeVersionId")
	DocumentDownloadsView toDownloads(ComposedResume composed);

	RewriteBulletResponse toResponse(BulletRewriteView bullet);

	List<RewriteBulletResponse> toBulletResponses(List<BulletRewriteView> bullets);

	RemovedSkillResponse toResponse(RemovedSkillView removedSkill);

	default BulletsPanelView toBulletsPanel(List<BulletRewriteView> bullets) {
		return new BulletsPanelView(toBulletResponses(bullets));
	}

	default ScoreComparisonView toScoreComparison(RewriteResult result) {
		if (result.scoreBefore() == null && result.scoreAfter() == null) {
			return null;
		}
		return new ScoreComparisonView(result.scoreBefore(), result.scoreAfter());
	}
}
