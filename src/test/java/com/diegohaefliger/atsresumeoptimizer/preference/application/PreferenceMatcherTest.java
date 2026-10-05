package com.diegohaefliger.atsresumeoptimizer.preference.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.CriterionMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.MatchStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceCriterion;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreferenceMatcherTest {

	private final PreferenceMatcher matcher = new PreferenceMatcher();

	@Test
	void returnsNothingWhenTheCandidateHasNoPreferences() {
		assertThat(matcher.match(JobPreference.EMPTY, offer().build())).isEmpty();
	}

	@Test
	void givesFullScoreWhenTheJobOffersEverythingTheCandidateWants() {
		JobPreference preference = new JobPreference(List.of(WorkModel.REMOTE), List.of(ContractType.CLT),
				new BigDecimal("8000"), new BigDecimal("10000"), List.of("plano de saúde"), List.of("pleno"),
				List.of("Curitiba"), List.of("Acme"), List.of());
		JobOffer offer = offer().workModel(WorkModel.REMOTE).contractType(ContractType.CLT)
				.salary(new BigDecimal("9000"), new BigDecimal("12000")).benefits(List.of("Plano de Saude"))
				.seniority("Pleno").company("ACME Tecnologia").build();

		PreferenceMatch match = matcher.match(preference, offer).orElseThrow();

		assertThat(match.score()).isEqualTo(100);
		assertThat(match.criteria()).extracting(CriterionMatch::status).containsOnly(MatchStatus.MATCH);
	}

	@Test
	void countsUnknownInformationAsHalfAndOnlyScoresCriteriaTheCandidateFilledIn() {
		JobPreference preference = preference().workModels(List.of(WorkModel.REMOTE)).minSalary("8000").build();
		JobOffer offer = offer().workModel(WorkModel.REMOTE).build();

		PreferenceMatch match = matcher.match(preference, offer).orElseThrow();

		assertThat(match.criteria()).extracting(CriterionMatch::criterion, CriterionMatch::status)
				.containsExactly(tuple(PreferenceCriterion.WORK_MODEL, MatchStatus.MATCH),
						tuple(PreferenceCriterion.SALARY, MatchStatus.UNKNOWN));
		assertThat(match.score()).isEqualTo(75);
	}

	@Test
	void leavesTheScoreEmptyWhenTheJobInformsNothingTheCandidateCaresAbout() {
		JobPreference preference = preference().workModels(List.of(WorkModel.REMOTE)).minSalary("8000").build();

		PreferenceMatch match = matcher.match(preference, offer().build()).orElseThrow();

		assertThat(match.score()).isNull();
		assertThat(match.criteria()).extracting(CriterionMatch::status).containsOnly(MatchStatus.UNKNOWN);
	}

	@Test
	void treatsAHybridJobAsPartialForSomeoneWhoWantsRemoteAndOnSiteAsMismatch() {
		JobPreference preference = preference().workModels(List.of(WorkModel.REMOTE)).build();

		assertThat(statusOf(preference, offer().workModel(WorkModel.HYBRID).build(), PreferenceCriterion.WORK_MODEL))
				.isEqualTo(MatchStatus.PARTIAL);
		assertThat(statusOf(preference, offer().workModel(WorkModel.ON_SITE).build(), PreferenceCriterion.WORK_MODEL))
				.isEqualTo(MatchStatus.MISMATCH);
	}

	@Test
	void comparesTheTopOfTheSalaryRangeWithTheDesiredAndTheMinimumSalary() {
		JobPreference preference = preference().minSalary("8000").desiredSalary("10000").build();

		assertThat(statusOf(preference, offer().salary(new BigDecimal("7000"), new BigDecimal("10500")).build(),
				PreferenceCriterion.SALARY)).isEqualTo(MatchStatus.MATCH);
		assertThat(statusOf(preference, offer().salary(new BigDecimal("9000"), null).build(), PreferenceCriterion.SALARY))
				.isEqualTo(MatchStatus.PARTIAL);
		assertThat(statusOf(preference, offer().salary(new BigDecimal("5000"), new BigDecimal("6000")).build(),
				PreferenceCriterion.SALARY)).isEqualTo(MatchStatus.MISMATCH);
	}

	@Test
	void findsDesiredBenefitsInTheExtractedListOrInTheRawJobText() {
		JobPreference preference = preference().benefits(List.of("vale refeição", "PLR", "auxílio creche")).build();
		JobOffer offer = offer().benefits(List.of("Vale-refeição", "Gympass")).rawText("Oferecemos PLR semestral.")
				.build();

		CriterionMatch benefits = criterion(preference, offer, PreferenceCriterion.BENEFITS);

		assertThat(benefits.status()).isEqualTo(MatchStatus.PARTIAL);
		assertThat(benefits.detail()).isEqualTo("Tem: vale refeição, PLR · Faltam: auxílio creche");
	}

	@Test
	void marksBenefitsAsUnknownWhenTheJobListsNone() {
		JobPreference preference = preference().benefits(List.of("PLR")).build();

		assertThat(statusOf(preference, offer().build(), PreferenceCriterion.BENEFITS)).isEqualTo(MatchStatus.UNKNOWN);
	}

	@Test
	void penalizesAnAvoidedCompanyAndRewardsAPreferredOne() {
		JobPreference preference = preference().preferredCompanies(List.of("Nubank")).avoidedCompanies(List.of("Acme"))
				.build();

		assertThat(statusOf(preference, offer().company("Acme S.A.").build(), PreferenceCriterion.COMPANY))
				.isEqualTo(MatchStatus.MISMATCH);
		assertThat(statusOf(preference, offer().company("nubank").build(), PreferenceCriterion.COMPANY))
				.isEqualTo(MatchStatus.MATCH);
		assertThat(statusOf(preference, offer().company("Outra").build(), PreferenceCriterion.COMPANY))
				.isEqualTo(MatchStatus.PARTIAL);
	}

	@Test
	void ignoresTheLocationOfARemoteJob() {
		JobPreference preference = preference().locations(List.of("Curitiba")).build();

		assertThat(statusOf(preference, offer().workModel(WorkModel.REMOTE).location("São Paulo").build(),
				PreferenceCriterion.LOCATION)).isEqualTo(MatchStatus.MATCH);
		assertThat(statusOf(preference, offer().workModel(WorkModel.ON_SITE).location("São Paulo - SP").build(),
				PreferenceCriterion.LOCATION)).isEqualTo(MatchStatus.MISMATCH);
	}

	@Test
	void matchesSeniorityIgnoringAccents() {
		JobPreference preference = preference().seniorities(List.of("Sênior")).build();

		assertThat(statusOf(preference, offer().seniority("senior").build(), PreferenceCriterion.SENIORITY))
				.isEqualTo(MatchStatus.MATCH);
	}

	private MatchStatus statusOf(JobPreference preference, JobOffer offer, PreferenceCriterion criterion) {
		return criterion(preference, offer, criterion).status();
	}

	private CriterionMatch criterion(JobPreference preference, JobOffer offer, PreferenceCriterion criterion) {
		return matcher.match(preference, offer).orElseThrow().criteria().stream()
				.filter(match -> match.criterion() == criterion)
				.findFirst()
				.orElseThrow();
	}

	private static PreferenceBuilder preference() {
		return new PreferenceBuilder();
	}

	private static OfferBuilder offer() {
		return new OfferBuilder();
	}

	private static final class PreferenceBuilder {
		private List<WorkModel> workModels = List.of();
		private BigDecimal minSalary;
		private BigDecimal desiredSalary;
		private List<String> benefits = List.of();
		private List<String> seniorities = List.of();
		private List<String> locations = List.of();
		private List<String> preferredCompanies = List.of();
		private List<String> avoidedCompanies = List.of();

		PreferenceBuilder workModels(List<WorkModel> value) {
			workModels = value;
			return this;
		}

		PreferenceBuilder minSalary(String value) {
			minSalary = new BigDecimal(value);
			return this;
		}

		PreferenceBuilder desiredSalary(String value) {
			desiredSalary = new BigDecimal(value);
			return this;
		}

		PreferenceBuilder benefits(List<String> value) {
			benefits = value;
			return this;
		}

		PreferenceBuilder seniorities(List<String> value) {
			seniorities = value;
			return this;
		}

		PreferenceBuilder locations(List<String> value) {
			locations = value;
			return this;
		}

		PreferenceBuilder preferredCompanies(List<String> value) {
			preferredCompanies = value;
			return this;
		}

		PreferenceBuilder avoidedCompanies(List<String> value) {
			avoidedCompanies = value;
			return this;
		}

		JobPreference build() {
			return new JobPreference(workModels, List.of(), minSalary, desiredSalary, benefits, seniorities, locations,
					preferredCompanies, avoidedCompanies);
		}
	}

	private static final class OfferBuilder {
		private String company;
		private WorkModel workModel;
		private ContractType contractType;
		private BigDecimal salaryMin;
		private BigDecimal salaryMax;
		private List<String> benefits = List.of();
		private String location;
		private String seniority;
		private String rawText = "";

		OfferBuilder company(String value) {
			company = value;
			return this;
		}

		OfferBuilder workModel(WorkModel value) {
			workModel = value;
			return this;
		}

		OfferBuilder contractType(ContractType value) {
			contractType = value;
			return this;
		}

		OfferBuilder salary(BigDecimal min, BigDecimal max) {
			salaryMin = min;
			salaryMax = max;
			return this;
		}

		OfferBuilder benefits(List<String> value) {
			benefits = value;
			return this;
		}

		OfferBuilder location(String value) {
			location = value;
			return this;
		}

		OfferBuilder seniority(String value) {
			seniority = value;
			return this;
		}

		OfferBuilder rawText(String value) {
			rawText = value;
			return this;
		}

		JobOffer build() {
			return new JobOffer(UUID.randomUUID(), "Backend Java", company, null, null, workModel, contractType, salaryMin,
					salaryMax, benefits, location, seniority, rawText);
		}
	}
}
