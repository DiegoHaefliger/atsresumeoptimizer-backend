package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.candidatequestion.application.CandidateQuestionService;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.interview.application.InterviewQuestionService;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class GetInterviewPrepTool extends AbstractJobTool {

	private static final String NOTHING_GENERATED =
			"Ainda não foram geradas perguntas de entrevista nem dúvidas para o recrutador nessa vaga.";

	private final InterviewQuestionService interviewQuestions;
	private final CandidateQuestionService candidateQuestions;

	GetInterviewPrepTool(ChatJobLookup jobs, InterviewQuestionService interviewQuestions,
			CandidateQuestionService candidateQuestions) {
		super(jobs, "Lendo o preparo de entrevista da vaga");
		this.interviewQuestions = interviewQuestions;
		this.candidateQuestions = candidateQuestions;
	}

	@Override
	public String name() {
		return "get_interview_prep";
	}

	@Override
	public String description() {
		return "Devolve as perguntas técnicas de entrevista e as perguntas para o candidato fazer ao recrutador que o app"
				+ " já gerou para a vaga.";
	}

	@Override
	String describe(JobOffer offer) {
		List<InterviewQuestionSet> interview = interviewQuestions.list(offer.jobPostingId());
		List<CandidateQuestionSet> candidate = candidateQuestions.list(offer.jobPostingId());
		if (interview.isEmpty() && candidate.isEmpty()) {
			return NOTHING_GENERATED;
		}
		StringBuilder text = new StringBuilder();
		if (!interview.isEmpty()) {
			text.append("Perguntas técnicas prováveis:\n");
			interview.getFirst().questions().forEach(question -> text.append("- %s (requisito: %s)\n".formatted(question.question(),
					question.requirement())));
		}
		if (!candidate.isEmpty()) {
			text.append("Perguntas para fazer ao recrutador:\n");
			candidate.getFirst().questions().forEach(question -> text.append("- %s\n".formatted(question.question())));
		}
		return text.toString();
	}
}
