package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisReportView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.DimensionView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.FindingView;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class GetJobAnalysisTool extends AbstractJobTool {

	private static final int MAX_FINDINGS = 15;
	private static final String NO_ANALYSIS = "Ainda não existe análise do currículo para essa vaga.";

	private final AnalysisQueryService analyses;

	GetJobAnalysisTool(ChatJobLookup jobs, AnalysisQueryService analyses) {
		super(jobs, "Lendo a análise da vaga");
		this.analyses = analyses;
	}

	@Override
	public String name() {
		return "get_job_analysis";
	}

	@Override
	public String description() {
		return "Devolve a análise ATS mais recente do currículo contra a vaga: nota geral, notas por dimensão, palavras-chave"
				+ " encontradas e faltantes e os principais problemas apontados.";
	}

	@Override
	String describe(JobOffer offer) {
		return analyses.generatedResumes(offer.jobPostingId()).stream()
				.filter(resume -> resume.sourceAnalysisId() != null)
				.max(Comparator.comparing(ResumeSummary::createdAt))
				.map(resume -> report(offer, analyses.get(new AnalysisId(resume.sourceAnalysisId()))))
				.orElse(NO_ANALYSIS);
	}

	private static String report(JobOffer offer, AnalysisReportView view) {
		StringBuilder text = new StringBuilder("Análise do currículo para a vaga %s:\n".formatted(ChatJobLookup.display(offer.code())));
		text.append("Nota geral: %s\n".formatted(view.score() == null || view.score().overall() == null ? "indisponível" : view.score().overall()));
		if (view.score() != null && !view.score().dimensions().isEmpty()) {
			text.append("Notas por dimensão: ").append(view.score().dimensions().stream()
					.map((DimensionView dimension) -> dimension.code() + "=" + dimension.score())
					.collect(Collectors.joining(", "))).append('\n');
		}
		if (view.keywords() != null) {
			text.append("Palavras-chave encontradas: %s\n".formatted(String.join(", ", view.keywords().found())));
			text.append("Palavras-chave faltando: %s\n".formatted(String.join(", ", view.keywords().missing())));
		}
		List<FindingView> findings = view.findings() == null ? List.of() : view.findings().items();
		findings.stream().limit(MAX_FINDINGS).forEach(finding -> text.append("- [%s] %s%s\n".formatted(finding.severity(),
				finding.message(), finding.suggestion() == null ? "" : " Sugestão: " + finding.suggestion())));
		return text.toString();
	}
}
