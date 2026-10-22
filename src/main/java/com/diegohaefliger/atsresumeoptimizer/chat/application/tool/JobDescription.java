package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JobDescription {

	private static final int MAX_JOB_TEXT = 6000;
	private static final int MAX_RESUME_TEXT = 6000;
	private static final String RESUME_UNAVAILABLE = "O texto do currículo escolhido não está mais disponível.";
	private static final String NOT_INFORMED = "não informado";

	private final JobStructuringService jobService;
	private final ResumeService resumeService;

	public JobDescription(JobStructuringService jobService, ResumeService resumeService) {
		this.jobService = jobService;
		this.resumeService = resumeService;
	}

	public String withResume(JobOffer offer, UUID resumeVersionId) {
		return of(offer) + "\n\nCurrículo que o usuário escolheu para essa vaga:\n" + resumeText(resumeVersionId);
	}

	public String of(JobOffer offer) {
		List<String> keywords = jobService.keywords(offer.jobPostingId());
		List<String> selected = jobService.selectedKeywords(offer.jobPostingId());
		StringBuilder text = new StringBuilder();
		text.append("Vaga #%d — %s\n".formatted(offer.code(), offer.title()));
		text.append("Empresa: %s\n".formatted(orNotInformed(offer.company())));
		text.append("Senioridade: %s\n".formatted(orNotInformed(offer.seniority())));
		text.append("Local: %s\n".formatted(orNotInformed(offer.location())));
		text.append("Modelo de trabalho: %s\n".formatted(offer.workModel() == null ? NOT_INFORMED : offer.workModel()));
		text.append("Contrato: %s\n".formatted(offer.contractType() == null ? NOT_INFORMED : offer.contractType()));
		if (!offer.benefits().isEmpty()) {
			text.append("Benefícios: %s\n".formatted(String.join(", ", offer.benefits())));
		}
		if (!selected.isEmpty()) {
			text.append("Palavras-chave principais: %s\n".formatted(String.join(", ", selected)));
		}
		if (!keywords.isEmpty()) {
			text.append("Palavras-chave da vaga: %s\n".formatted(String.join(", ", keywords)));
		}
		text.append("Texto da vaga:\n").append(ChatText.limit(offer.rawText(), MAX_JOB_TEXT));
		return text.toString();
	}

	private String resumeText(UUID resumeVersionId) {
		try {
			return ChatText.limit(resumeService.getVersionText(resumeVersionId).rawText(), MAX_RESUME_TEXT);
		} catch (RuntimeException expired) {
			return RESUME_UNAVAILABLE;
		}
	}

	private static String orNotInformed(String value) {
		return value == null ? NOT_INFORMED : value;
	}
}
