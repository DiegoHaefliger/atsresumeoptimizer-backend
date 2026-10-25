package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import org.junit.jupiter.api.Test;

class SuggestionOfferFactoryTest {

	private final SuggestionOfferFactory factory = new SuggestionOfferFactory();

	private static SourceJob job(String title, String location) {
		return new SourceJob("1", "https://example.test/1", title, "Acme", location, LocalDate.of(2026, 10, 1));
	}

	private static SourceJobDetail detail(String description, String level, String type) {
		return new SourceJobDetail(description, level, type);
	}

	@Test
	void infersTheSeniorityFromTheTitle() {
		assertThat(factory.offer(job("Desenvolvedor Java Sênior", "Brazil"), detail("texto", null, null)).seniority())
				.isEqualTo("Sênior");
		assertThat(factory.offer(job("Dev Java Pl", "Brazil"), detail("texto", null, null)).seniority()).isEqualTo("Pleno");
		assertThat(factory.offer(job("Jr Developer", "Brazil"), detail("texto", null, null)).seniority()).isEqualTo("Júnior");
		assertThat(factory.offer(job("Tech Lead Backend", "Brazil"), detail("texto", null, null)).seniority())
				.isEqualTo("Tech Lead");
	}

	@Test
	void carriesTheSalaryTheSourceInformed() {
		SourceJobDetail detail = new SourceJobDetail("texto", null, null, null, null, null,
				new SalaryRange(new java.math.BigDecimal("10000"), new java.math.BigDecimal("12000")));

		JobOffer offer = factory.offer(job("Dev Java", "Brazil"), detail);

		assertThat(offer.salaryMin()).isEqualByComparingTo("10000");
		assertThat(offer.salaryMax()).isEqualByComparingTo("12000");
	}

	@Test
	void leavesTheSeniorityOutWhenNothingSaysIt() {
		assertThat(factory.offer(job("Desenvolvedor Java", "Brazil"), detail("texto", "Not Applicable", null)).seniority())
				.isNull();
	}

	@Test
	void doesNotConfuseJavaScriptWithSeniority() {
		assertThat(factory.offer(job("Desenvolvedor JavaScript", "Brazil"), detail("texto", null, null)).seniority()).isNull();
	}

	@Test
	void infersTheWorkModelFromTitleLocationOrTheStartOfTheDescription() {
		assertThat(factory.offer(job("Dev Java (Remoto)", "Brazil"), detail("texto", null, null)).workModel())
				.isEqualTo(WorkModel.REMOTE);
		assertThat(factory.offer(job("Dev Java", "São Paulo"), detail("Modelo híbrido, 3x por semana", null, null)).workModel())
				.isEqualTo(WorkModel.HYBRID);
		assertThat(factory.offer(job("Dev Java", "São Paulo"), detail("Trabalho presencial", null, null)).workModel())
				.isEqualTo(WorkModel.ON_SITE);
		assertThat(factory.offer(job("Dev Java", "São Paulo"), detail("texto", null, null)).workModel()).isNull();
	}

	@Test
	void mapsOnlyTheEmploymentTypesThatSayTheContract() {
		assertThat(factory.offer(job("Dev", "BR"), detail("t", null, "Internship")).contractType())
				.isEqualTo(ContractType.INTERNSHIP);
		assertThat(factory.offer(job("Dev", "BR"), detail("t", null, "Contract")).contractType()).isEqualTo(ContractType.PJ);
		assertThat(factory.offer(job("Dev", "BR"), detail("t", null, "Full-time")).contractType()).isNull();
	}

	@Test
	void keepsTheDescriptionAndTheSourceData() {
		var offer = factory.offer(job("Dev", "Curitiba"), detail("Descrição completa", null, null));

		assertThat(offer.rawText()).isEqualTo("Descrição completa");
		assertThat(offer.company()).isEqualTo("Acme");
		assertThat(offer.location()).isEqualTo("Curitiba");
		assertThat(offer.sourceUrl()).isEqualTo("https://example.test/1");
	}
}
