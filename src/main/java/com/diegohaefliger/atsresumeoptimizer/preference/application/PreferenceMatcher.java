package com.diegohaefliger.atsresumeoptimizer.preference.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.CriterionMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.MatchStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceCriterion;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
class PreferenceMatcher {

	private static final Locale PT_BR = Locale.of("pt", "BR");
	private static final int MAX_SCORE = 100;
	private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}]+");

	Optional<PreferenceMatch> match(JobPreference preference, JobOffer offer) {
		List<CriterionMatch> criteria = Stream.of(
						workModel(preference, offer),
						salary(preference, offer),
						contractType(preference, offer),
						benefits(preference, offer),
						company(preference, offer),
						seniority(preference, offer),
						location(preference, offer))
				.flatMap(Optional::stream)
				.toList();
		if (criteria.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new PreferenceMatch(score(criteria), criteria));
	}

	private Integer score(List<CriterionMatch> criteria) {
		if (criteria.stream().allMatch(match -> match.status() == MatchStatus.UNKNOWN)) {
			return null;
		}
		double totalWeight = criteria.stream().mapToInt(match -> match.criterion().weight()).sum();
		double achieved = criteria.stream()
				.mapToDouble(match -> match.criterion().weight() * match.status().fulfillment())
				.sum();
		return (int) Math.round(achieved / totalWeight * MAX_SCORE);
	}

	private Optional<CriterionMatch> workModel(JobPreference preference, JobOffer offer) {
		if (preference.workModels().isEmpty()) {
			return Optional.empty();
		}
		WorkModel offered = offer.workModel();
		if (offered == null) {
			return unknown(PreferenceCriterion.WORK_MODEL, "Vaga não informa o modelo de trabalho");
		}
		String detail = "Vaga: " + offered.label();
		if (preference.workModels().contains(offered)) {
			return result(PreferenceCriterion.WORK_MODEL, MatchStatus.MATCH, detail);
		}
		MatchStatus status = offered == WorkModel.HYBRID ? MatchStatus.PARTIAL : MatchStatus.MISMATCH;
		return result(PreferenceCriterion.WORK_MODEL, status, detail);
	}

	private Optional<CriterionMatch> salary(JobPreference preference, JobOffer offer) {
		BigDecimal target = preference.desiredSalary() != null ? preference.desiredSalary() : preference.minSalary();
		if (target == null) {
			return Optional.empty();
		}
		BigDecimal offered = offer.salaryMax() != null ? offer.salaryMax() : offer.salaryMin();
		if (offered == null) {
			return unknown(PreferenceCriterion.SALARY, "Vaga não informa salário");
		}
		String detail = "Vaga: " + salaryRange(offer);
		if (offered.compareTo(target) >= 0) {
			return result(PreferenceCriterion.SALARY, MatchStatus.MATCH, detail);
		}
		boolean aboveMinimum = preference.minSalary() != null && offered.compareTo(preference.minSalary()) >= 0;
		return result(PreferenceCriterion.SALARY, aboveMinimum ? MatchStatus.PARTIAL : MatchStatus.MISMATCH, detail);
	}

	private Optional<CriterionMatch> contractType(JobPreference preference, JobOffer offer) {
		if (preference.contractTypes().isEmpty()) {
			return Optional.empty();
		}
		ContractType offered = offer.contractType();
		if (offered == null) {
			return unknown(PreferenceCriterion.CONTRACT_TYPE, "Vaga não informa o tipo de contrato");
		}
		MatchStatus status = preference.contractTypes().contains(offered) ? MatchStatus.MATCH : MatchStatus.MISMATCH;
		return result(PreferenceCriterion.CONTRACT_TYPE, status, "Vaga: " + offered.label());
	}

	private Optional<CriterionMatch> benefits(JobPreference preference, JobOffer offer) {
		if (preference.benefits().isEmpty()) {
			return Optional.empty();
		}
		String normalizedRawText = normalized(offer.rawText());
		List<String> normalizedOffered = offer.benefits().stream().map(this::normalized).toList();
		List<String> found = preference.benefits().stream()
				.filter(desired -> offersBenefit(normalized(desired), normalizedOffered, normalizedRawText))
				.toList();
		List<String> missing = preference.benefits().stream().filter(desired -> !found.contains(desired)).toList();
		if (found.isEmpty() && offer.benefits().isEmpty()) {
			return unknown(PreferenceCriterion.BENEFITS, "Vaga não lista benefícios");
		}
		MatchStatus status = missing.isEmpty() ? MatchStatus.MATCH : found.isEmpty() ? MatchStatus.MISMATCH : MatchStatus.PARTIAL;
		return result(PreferenceCriterion.BENEFITS, status, benefitsDetail(found, missing));
	}

	private boolean offersBenefit(String desired, List<String> offered, String rawText) {
		return offered.stream().anyMatch(benefit -> benefit.contains(desired) || desired.contains(benefit))
				|| NormalizedText.containsWord(rawText, desired);
	}

	private String benefitsDetail(List<String> found, List<String> missing) {
		String foundPart = found.isEmpty() ? "" : "Tem: " + String.join(", ", found);
		String missingPart = missing.isEmpty() ? "" : "Faltam: " + String.join(", ", missing);
		return Stream.of(foundPart, missingPart).filter(part -> !part.isEmpty()).collect(Collectors.joining(" · "));
	}

	private Optional<CriterionMatch> company(JobPreference preference, JobOffer offer) {
		if (preference.preferredCompanies().isEmpty() && preference.avoidedCompanies().isEmpty()) {
			return Optional.empty();
		}
		if (offer.company() == null || offer.company().isBlank()) {
			return unknown(PreferenceCriterion.COMPANY, "Vaga não informa a empresa");
		}
		String company = normalized(offer.company());
		if (containsName(preference.avoidedCompanies(), company)) {
			return result(PreferenceCriterion.COMPANY, MatchStatus.MISMATCH, offer.company() + " está na sua lista de empresas evitadas");
		}
		if (containsName(preference.preferredCompanies(), company)) {
			return result(PreferenceCriterion.COMPANY, MatchStatus.MATCH, offer.company() + " está na sua lista de empresas preferidas");
		}
		MatchStatus status = preference.preferredCompanies().isEmpty() ? MatchStatus.MATCH : MatchStatus.PARTIAL;
		return result(PreferenceCriterion.COMPANY, status, "Empresa: " + offer.company());
	}

	private Optional<CriterionMatch> seniority(JobPreference preference, JobOffer offer) {
		if (preference.seniorities().isEmpty()) {
			return Optional.empty();
		}
		if (offer.seniority() == null || offer.seniority().isBlank()) {
			return unknown(PreferenceCriterion.SENIORITY, "Vaga não informa a senioridade");
		}
		MatchStatus status = containsName(preference.seniorities(), normalized(offer.seniority()))
				? MatchStatus.MATCH
				: MatchStatus.MISMATCH;
		return result(PreferenceCriterion.SENIORITY, status, "Vaga: " + offer.seniority());
	}

	private Optional<CriterionMatch> location(JobPreference preference, JobOffer offer) {
		if (preference.locations().isEmpty()) {
			return Optional.empty();
		}
		if (offer.workModel() == WorkModel.REMOTE) {
			return result(PreferenceCriterion.LOCATION, MatchStatus.MATCH, "Vaga remota");
		}
		if (offer.location() == null || offer.location().isBlank()) {
			return unknown(PreferenceCriterion.LOCATION, "Vaga não informa a localização");
		}
		MatchStatus status = containsName(preference.locations(), normalized(offer.location()))
				? MatchStatus.MATCH
				: MatchStatus.MISMATCH;
		return result(PreferenceCriterion.LOCATION, status, "Vaga: " + offer.location());
	}

	private boolean containsName(List<String> names, String normalizedOffered) {
		return names.stream()
				.map(this::normalized)
				.anyMatch(name -> normalizedOffered.contains(name) || name.contains(normalizedOffered));
	}

	private String normalized(String text) {
		return NormalizedText.of(text == null ? null : PUNCTUATION.matcher(text).replaceAll(" "));
	}

	private String salaryRange(JobOffer offer) {
		NumberFormat currency = NumberFormat.getCurrencyInstance(PT_BR);
		currency.setMaximumFractionDigits(0);
		if (offer.salaryMin() == null || offer.salaryMax() == null || offer.salaryMin().compareTo(offer.salaryMax()) == 0) {
			BigDecimal single = offer.salaryMax() != null ? offer.salaryMax() : offer.salaryMin();
			return currency.format(single);
		}
		return currency.format(offer.salaryMin()) + " a " + currency.format(offer.salaryMax());
	}

	private Optional<CriterionMatch> unknown(PreferenceCriterion criterion, String detail) {
		return result(criterion, MatchStatus.UNKNOWN, detail);
	}

	private Optional<CriterionMatch> result(PreferenceCriterion criterion, MatchStatus status, String detail) {
		return Optional.of(new CriterionMatch(criterion, status, detail));
	}
}
