package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionSummary;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class GetJobResumeTool extends AbstractJobTool {

	private static final int MAX_RESUME_TEXT = 6000;
	private static final String NO_RESUME = "Ainda não existe currículo gerado para essa vaga.";

	private final AnalysisQueryService analyses;
	private final ResumeService resumes;

	GetJobResumeTool(ChatJobLookup jobs, AnalysisQueryService analyses, ResumeService resumes) {
		super(jobs, "Lendo o currículo da vaga");
		this.analyses = analyses;
		this.resumes = resumes;
	}

	@Override
	public String name() {
		return "get_job_resume";
	}

	@Override
	public String description() {
		return "Devolve o texto do currículo mais recente que o app gerou (adaptou) para a vaga.";
	}

	@Override
	String describe(JobOffer offer) {
		List<ResumeSummary> generated = analyses.generatedResumes(offer.jobPostingId());
		return generated.stream()
				.flatMap(resume -> resume.versions().stream())
				.max(Comparator.comparing(ResumeVersionSummary::createdAt))
				.map(version -> "Currículo gerado para a vaga %s:\n%s".formatted(ChatJobLookup.display(offer.code()),
						ChatText.limit(resumes.getVersionText(version.id()).rawText(), MAX_RESUME_TEXT)))
				.orElse(NO_RESUME);
	}
}
