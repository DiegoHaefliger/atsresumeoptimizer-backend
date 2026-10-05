package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Heurística de MVP: anos = intervalo entre o menor e o maior ano citado, sem timeline por cargo. */
@Component
class RequirementsSeniorityScorer implements DimensionScorer {

	private static final Pattern FOUR_DIGIT_YEAR = Pattern.compile("\\b(19|20)\\d{2}\\b");
	private static final Set<String> ONGOING_ROLE_MARKERS =
			Set.of("atual", "atualmente", "presente", "current", "ongoing", "hoje");
	private static final int EXPERIENCE_PENALTY = 40;
	private static final int LANGUAGE_PENALTY = 20;

	@Override
	public Dimension dimension() {
		return Dimension.REQUIREMENTS_SENIORITY;
	}

	@Override
	public DimensionResult score(ScoringContext context) {
		JobStructured job = context.job();
		if (job == null) {
			return DimensionResult.of(Dimension.REQUIREMENTS_SENIORITY, 0, List.of());
		}

		String haystack = NormalizedText.of(context.parsing().document().rawText());
		List<Finding> findings = new ArrayList<>();
		int score = 100;

		Integer estimatedYears = estimateYearsOfExperience(context.parsing().document().rawText());
		if (job.minYearsExperience() != null && estimatedYears != null && estimatedYears < job.minYearsExperience()) {
			findings.add(Finding.of(FindingCode.REQUIREMENTS_MIN_EXPERIENCE_NOT_MET,
					"A vaga pede pelo menos %d anos de experiência; o currículo sugere cerca de %d."
							.formatted(job.minYearsExperience(), estimatedYears),
					"Deixe claro no currículo o tempo total de experiência na área."));
			score -= EXPERIENCE_PENALTY;
		}

		for (String language : job.languages()) {
			if (!haystack.contains(NormalizedText.of(language))) {
				findings.add(Finding.of(FindingCode.REQUIREMENTS_LANGUAGE_NOT_EVIDENCED,
						"A vaga exige \"%s\" e isso não aparece evidenciado no currículo.".formatted(language),
						"Inclua o idioma e o nível de proficiência no currículo, se aplicável."));
				score -= LANGUAGE_PENALTY;
			}
		}

		return DimensionResult.of(Dimension.REQUIREMENTS_SENIORITY, Math.max(0, score), findings);
	}

	private Integer estimateYearsOfExperience(String text) {
		Matcher matcher = FOUR_DIGIT_YEAR.matcher(text);
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		boolean found = false;
		while (matcher.find()) {
			found = true;
			int year = Integer.parseInt(matcher.group());
			min = Math.min(min, year);
			max = Math.max(max, year);
		}
		if (!found) {
			return null;
		}
		if (hasOngoingRoleMarker(text)) {
			max = Math.max(max, Year.now().getValue());
		}
		return Math.max(0, max - min);
	}

	private boolean hasOngoingRoleMarker(String text) {
		String normalized = NormalizedText.of(text);
		return ONGOING_ROLE_MARKERS.stream().anyMatch(normalized::contains);
	}
}
