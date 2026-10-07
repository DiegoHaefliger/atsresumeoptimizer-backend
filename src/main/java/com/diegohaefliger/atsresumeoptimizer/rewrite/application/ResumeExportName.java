package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionOrigin;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ResumeExportName {

	private static final String PART_SEPARATOR = "_";
	private static final String JOB_CODE_FORMAT = "V%04d";
	private static final String DEFAULT_NAME = "Curriculo";
	private static final Pattern KNOWN_EXTENSION = Pattern.compile("\\.(pdf|docx|odt)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
	private static final Pattern WORD_SEPARATOR = Pattern.compile("[^A-Za-z0-9]+");

	private final ResumeService resumeService;
	private final AnalysisSnapshotPort analysisSnapshots;
	private final JobStructuringService jobStructuringService;

	ResumeExportName(ResumeService resumeService, AnalysisSnapshotPort analysisSnapshots,
			JobStructuringService jobStructuringService) {
		this.resumeService = resumeService;
		this.analysisSnapshots = analysisSnapshots;
		this.jobStructuringService = jobStructuringService;
	}

	/** Ex.: {@code DiegoHaefliger_Cielo_V0001_1}; empresa e código só entram quando o currículo foi adaptado pra uma vaga. */
	String baseName(UUID resumeId, UUID resumeVersionId, String candidateName) {
		ResumeVersionOrigin origin = resumeService.origin(resumeId, resumeVersionId);
		List<String> parts = new ArrayList<>();
		parts.add(StringUtils.hasText(candidateName) ? candidateName : titleWithoutExtension(resumeId));
		job(origin).ifPresent(job -> {
			parts.add(job.company());
			parts.add(job.code() != null ? JOB_CODE_FORMAT.formatted(job.code()) : null);
		});
		String head = parts.stream().map(ResumeExportName::camelCase).filter(part -> !part.isEmpty())
				.collect(Collectors.joining(PART_SEPARATOR));
		return (head.isEmpty() ? DEFAULT_NAME : head) + PART_SEPARATOR + origin.number();
	}

	private Optional<JobOffer> job(ResumeVersionOrigin origin) {
		return origin.sourceAnalysisId()
				.flatMap(analysisId -> analysisSnapshots.jobPostingId(new AnalysisId(analysisId)))
				.flatMap(jobStructuringService::offer);
	}

	private String titleWithoutExtension(UUID resumeId) {
		return KNOWN_EXTENSION.matcher(resumeService.title(resumeId)).replaceFirst("");
	}

	static String camelCase(String text) {
		if (text == null) {
			return "";
		}
		String plain = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		List<String> words = WORD_SEPARATOR.splitAsStream(plain).filter(word -> !word.isEmpty()).toList();
		// Frase toda em maiúsculas (cabeçalho "DIEGO HAEFLIGER") vira Title Case; sigla solta ("IBM") fica como está.
		boolean allCaps = words.size() > 1 && plain.chars().noneMatch(Character::isLowerCase);
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			String rest = word.substring(1);
			result.append(Character.toUpperCase(word.charAt(0))).append(allCaps ? rest.toLowerCase(Locale.ROOT) : rest);
		}
		return result.toString();
	}
}
