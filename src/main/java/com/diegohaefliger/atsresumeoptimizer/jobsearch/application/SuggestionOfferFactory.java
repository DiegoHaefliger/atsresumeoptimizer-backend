package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class SuggestionOfferFactory {

	private static final int DESCRIPTION_HEAD = 600;
	private static final Pattern REMOTE = Pattern.compile("remote|remoto|home office|anywhere");
	private static final Pattern HYBRID = Pattern.compile("hybrid|hibrido");
	private static final Pattern ON_SITE = Pattern.compile("on-site|onsite|on site|presencial");
	private static final Pattern TECH_LEAD = Pattern.compile("tech lead|lider tecnico|team lead");
	private static final Pattern SPECIALIST = Pattern.compile("especialista|specialist|staff|principal");
	private static final Pattern SENIOR = Pattern.compile("senior|\\bsr\\b|\\bsr\\.");
	private static final Pattern MID = Pattern.compile("pleno|\\bmid\\b|mid-level|\\bpl\\b");
	private static final Pattern JUNIOR = Pattern.compile("junior|\\bjr\\b|\\bjr\\.|entry level|trainee");

	JobOffer offer(SourceJob job, SourceJobDetail detail) {
		String head = NormalizedText.of(job.title() + " " + job.location() + " "
				+ detail.description().substring(0, Math.min(DESCRIPTION_HEAD, detail.description().length())));
		return new JobOffer(UUID.randomUUID(), null, job.title(), blankToNull(job.company()), job.url(), null,
				detail.workModel() != null ? detail.workModel() : workModel(head),
				detail.contractType() != null ? detail.contractType() : contractType(detail.employmentType()), detail.salary().min(),
				detail.salary().max(), List.of(),
				blankToNull(job.location()), seniority(job.title(), detail.seniorityLevel()).orElse(null),
				detail.description());
	}

	private WorkModel workModel(String head) {
		if (REMOTE.matcher(head).find()) {
			return WorkModel.REMOTE;
		}
		if (HYBRID.matcher(head).find()) {
			return WorkModel.HYBRID;
		}
		return ON_SITE.matcher(head).find() ? WorkModel.ON_SITE : null;
	}

	private ContractType contractType(String employmentType) {
		return switch (NormalizedText.of(employmentType)) {
			case "internship", "estagio" -> ContractType.INTERNSHIP;
			case "temporary", "temporario" -> ContractType.TEMPORARY;
			case "contract", "contrato" -> ContractType.PJ;
			default -> null;
		};
	}

	private Optional<String> seniority(String title, String level) {
		String normalized = NormalizedText.of(title);
		if (TECH_LEAD.matcher(normalized).find()) {
			return Optional.of("Tech Lead");
		}
		if (SPECIALIST.matcher(normalized).find()) {
			return Optional.of("Especialista");
		}
		if (SENIOR.matcher(normalized).find()) {
			return Optional.of("Sênior");
		}
		if (MID.matcher(normalized).find()) {
			return Optional.of("Pleno");
		}
		if (JUNIOR.matcher(normalized).find() || JUNIOR.matcher(NormalizedText.of(level)).find()) {
			return Optional.of("Júnior");
		}
		return levelSeniority(level);
	}

	private Optional<String> levelSeniority(String level) {
		return switch (NormalizedText.of(level)) {
			case "senior" -> Optional.of("Sênior");
			case "pleno" -> Optional.of("Pleno");
			case "especialista" -> Optional.of("Especialista");
			case "tech lead" -> Optional.of("Tech Lead");
			default -> Optional.empty();
		};
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text;
	}
}
