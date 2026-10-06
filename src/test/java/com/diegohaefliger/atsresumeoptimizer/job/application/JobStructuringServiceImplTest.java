package com.diegohaefliger.atsresumeoptimizer.job.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.JobConditions;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobRegistration;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringResult;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.job.domain.DuplicateJobPostingException;
import com.diegohaefliger.atsresumeoptimizer.job.domain.JobPostingNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JobStructuringServiceImplTest {

	@Mock
	private AiPort aiPort;
	@Mock
	private JobPostingRepository repository;

	private JobStructuringServiceImpl service;

	private final JobStructured structured = new JobStructured(
			"Backend Java", "pleno", 3, null, List.of("inglês"), List.of("Java", "Spring"), Map.of());

	@Test
	void callsAiAndPersistsANewJobPostingOnFirstStructuring() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		when(repository.findByTextHash(any())).thenReturn(Optional.empty());
		when(aiPort.structureJob("vaga de java"))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 100, 50, BigDecimal.ONE, false)));

		JobStructuringResult result = service.structureFromText("vaga de java");

		assertThat(result.structured().title()).isEqualTo("Backend Java");
		assertThat(result.usage().cached()).isFalse();
		verify(repository).saveAndFlush(any());
	}

	@Test
	void restructuresAnExistingPostingSoANewPromptVersionTakesEffect() throws Exception {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting existing = new JobPosting(UUID.randomUUID(), "Antigo", "vaga de java",
				"hash", new ObjectMapper().writeValueAsString(structured));
		when(repository.findByTextHash(any())).thenReturn(Optional.of(existing));
		when(aiPort.structureJob("vaga de java"))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 100, 50, BigDecimal.ONE, false)));

		JobStructuringResult result = service.structureFromText("vaga de java");

		assertThat(result.jobPostingId()).isEqualTo(existing.id());
		assertThat(existing.title()).isEqualTo("Backend Java");
	}

	@Test
	void reusesTheStoredStructureWhenTheAiIsUnavailable() throws Exception {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting existing = new JobPosting(UUID.randomUUID(), "Backend Java", "vaga de java",
				"hash", new ObjectMapper().writeValueAsString(structured));
		when(repository.findByTextHash(any())).thenReturn(Optional.of(existing));
		when(aiPort.structureJob("vaga de java")).thenThrow(new AiCallException("fora do ar", null));

		JobStructuringResult result = service.structureFromText("vaga de java");

		assertThat(result.structured().title()).isEqualTo("Backend Java");
		assertThat(result.usage().cached()).isTrue();
	}

	@Test
	void fallsBackToTheConcurrentlyInsertedRowWhenTwoAnalysesRaceOnTheSameJobText() throws Exception {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		when(repository.findByTextHash(any())).thenReturn(Optional.empty());
		when(aiPort.structureJob("vaga de java"))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 100, 50, BigDecimal.ONE, false)));
		org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate text_hash"))
				.when(repository).saveAndFlush(any());
		JobPosting concurrentlyInserted = new JobPosting(UUID.randomUUID(), "Backend Java", "vaga de java",
				"hash", new ObjectMapper().writeValueAsString(structured));
		when(repository.findByTextHash(any())).thenReturn(Optional.empty(), Optional.of(concurrentlyInserted));

		JobStructuringResult result = service.structureFromText("vaga de java");

		assertThat(result.jobPostingId()).isEqualTo(concurrentlyInserted.id());
		assertThat(result.structured().title()).isEqualTo("Backend Java");
	}

	@Test
	void returnsTheRawTextOfAnExistingJobPosting() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting posting = new JobPosting(UUID.randomUUID(), "Backend Java", "vaga de java", "hash", "{}");
		when(repository.findById(posting.id())).thenReturn(Optional.of(posting));

		String rawText = service.getRawText(posting.id());

		assertThat(rawText).isEqualTo("vaga de java");
	}

	@Test
	void throwsWhenTheJobPostingDoesNotExist() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		when(repository.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getRawText(UUID.randomUUID()))
				.isInstanceOf(JobPostingNotFoundException.class);
	}

	@Test
	void registersANewJobPostingWithTheUserDetailsWithoutCallingAi() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		when(repository.findByTextHash(any())).thenReturn(Optional.empty());

		UUID id = service.register("vaga de java", new JobDetails("Acme", "https://acme.com/vaga", WorkModel.HYBRID, null, null, null, null, null, null));

		ArgumentCaptor<JobPosting> saved = ArgumentCaptor.forClass(JobPosting.class);
		verify(repository).saveAndFlush(saved.capture());
		assertThat(saved.getValue().id()).isEqualTo(id);
		assertThat(saved.getValue().company()).isEqualTo("Acme");
		assertThat(saved.getValue().workModel()).isEqualTo(WorkModel.HYBRID);
		assertThat(saved.getValue().isStructured()).isFalse();
		verifyNoInteractions(aiPort);
	}

	@Test
	void keepsTheDetailsAlreadyStoredWhenTheSameJobIsRegisteredAgainWithBlankFields() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting existing = new JobPosting(UUID.randomUUID(), null, "vaga de java", "hash", null);
		existing.applyDetails(new JobDetails("Acme", "https://acme.com/vaga", WorkModel.REMOTE, null, null, null, null, null, null));
		when(repository.findByTextHash(any())).thenReturn(Optional.of(existing));

		UUID id = service.register("vaga de java", new JobDetails(" ", null, WorkModel.HYBRID, null, null, null, null, null, null));

		assertThat(id).isEqualTo(existing.id());
		assertThat(existing.company()).isEqualTo("Acme");
		assertThat(existing.workModel()).isEqualTo(WorkModel.HYBRID);
	}

	@Test
	void hidesTheJobFromTheRecentListAndShowsItAgainWhenTheSameJobIsRegistered() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting posting = new JobPosting(UUID.randomUUID(), null, "vaga de java", "hash", null);
		when(repository.findById(posting.id())).thenReturn(Optional.of(posting));
		when(repository.findByTextHash(any())).thenReturn(Optional.of(posting));

		service.hideFromRecent(posting.id());
		assertThat(posting.isHiddenFromRecent()).isTrue();

		service.register("vaga de java", JobDetails.NONE);
		assertThat(posting.isHiddenFromRecent()).isFalse();
	}

	@Test
	void registersAJobTypedByTheUserAndStructuresItRightAway() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting posting = new JobPosting(UUID.randomUUID(), null, "vaga de java", "hash", null);
		when(repository.findByTextHash(any())).thenReturn(Optional.empty(), Optional.of(posting));
		when(aiPort.structureJob("vaga de java"))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 100, 50, BigDecimal.ONE, false)));

		JobRegistration registration = service.registerAndStructure("vaga de java", JobDetails.NONE);

		assertThat(registration.title()).isEqualTo("Backend Java");
		assertThat(registration.requiredKeywords()).containsExactly("Java", "Spring");
		assertThat(posting.isSynthetic()).isFalse();
	}

	@Test
	void keepsTheRegisteredJobWithoutTitleWhenTheAiFails() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		when(repository.findByTextHash(any())).thenReturn(Optional.empty());
		when(aiPort.structureJob(any())).thenThrow(new AiCallException("fora do ar", null));

		JobRegistration registration = service.registerAndStructure("vaga de java", JobDetails.NONE);

		assertThat(registration.id()).isNotNull();
		assertThat(registration.title()).isNull();
		verify(repository).saveAndFlush(any());
	}

	@Test
	void marksTheTargetRoleProfileAsSyntheticSoItStaysOutOfTheJobList() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		when(repository.findByTextHash(any())).thenReturn(Optional.empty());
		when(aiPort.structureJob(any()))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 100, 50, BigDecimal.ONE, false)));

		service.structureFromTargetRole("Backend Java");

		ArgumentCaptor<JobPosting> saved = ArgumentCaptor.forClass(JobPosting.class);
		verify(repository).saveAndFlush(saved.capture());
		assertThat(saved.getValue().isSynthetic()).isTrue();
	}

	@Test
	void listsTheRegisteredJobsWithTheirOfferAndRegistrationDate() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting posting = new JobPosting(UUID.randomUUID(), "Backend Java", "vaga de java", "hash", null);
		posting.applyDetails(new JobDetails("Acme", null, WorkModel.REMOTE, null, null, null, null, null, null));
		when(repository.findTop100BySyntheticFalseAndHiddenAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(posting));

		List<JobListing> jobs = service.listJobs();

		assertThat(jobs).singleElement().satisfies(listing -> {
			assertThat(listing.offer().jobPostingId()).isEqualTo(posting.id());
			assertThat(listing.offer().company()).isEqualTo("Acme");
			assertThat(listing.registeredAt()).isNotNull();
		});
	}

	@Test
	void updatingTheTextReadsTheJobAgainAndReplacesTheTypedDetails() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting posting = new JobPosting(UUID.randomUUID(), "Título antigo", "vaga antiga", "hash-antigo", "{}");
		posting.applyDetails(new JobDetails("Acme", "https://acme.com/vaga", WorkModel.REMOTE, null, null, null, null, null, null));
		when(repository.findById(posting.id())).thenReturn(Optional.of(posting));
		when(repository.findByTextHash(any())).thenReturn(Optional.empty(), Optional.of(posting));
		when(aiPort.structureJob("vaga nova de java"))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 100, 50, BigDecimal.ONE, false)));

		JobRegistration registration =
				service.update(posting.id(), "vaga nova de java", new JobDetails(null, null, WorkModel.HYBRID, null, null, null, null, null, null));

		assertThat(registration.title()).isEqualTo("Backend Java");
		assertThat(posting.rawText()).isEqualTo("vaga nova de java");
		assertThat(posting.company()).isNull();
		assertThat(posting.workModel()).isEqualTo(WorkModel.HYBRID);
	}

	@Test
	void refusesToTurnAJobIntoACopyOfAnotherOne() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting posting = new JobPosting(UUID.randomUUID(), null, "vaga a", "hash-a", null);
		JobPosting other = new JobPosting(UUID.randomUUID(), null, "vaga b", "hash-b", null);
		when(repository.findById(posting.id())).thenReturn(Optional.of(posting));
		when(repository.findByTextHash(any())).thenReturn(Optional.of(other));

		assertThatThrownBy(() -> service.update(posting.id(), "vaga b", JobDetails.NONE))
				.isInstanceOf(DuplicateJobPostingException.class);
		verifyNoInteractions(aiPort);
	}

	@Test
	void hidingAJobThatDoesNotExistFails() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		when(repository.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.hideFromRecent(UUID.randomUUID()))
				.isInstanceOf(JobPostingNotFoundException.class);
	}

	@Test
	void structuresARegisteredButNotYetStructuredPostingInPlace() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting registered = new JobPosting(UUID.randomUUID(), null, "vaga de java", "hash", null);
		when(repository.findByTextHash(any())).thenReturn(Optional.of(registered));
		when(aiPort.structureJob("vaga de java"))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 100, 50, BigDecimal.ONE, false)));

		JobStructuringResult result = service.structureFromText("vaga de java");

		assertThat(result.jobPostingId()).isEqualTo(registered.id());
		assertThat(registered.title()).isEqualTo("Backend Java");
		assertThat(registered.isStructured()).isTrue();
	}

	@Test
	void buildsTheOfferPreferringWhatTheUserTypedOverWhatTheAiExtracted() throws Exception {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobStructured withConditions = new JobStructured("Backend Java", "pleno", 3, null, List.of(), List.of("Java"),
				Map.of(), new JobConditions("Acme Ltda", "ON_SITE", "pj", new BigDecimal("9000"), new BigDecimal("11000"),
						List.of("PLR"), "Curitiba - PR"));
		JobPosting posting = new JobPosting(UUID.randomUUID(), "Backend Java", "vaga de java", "hash",
				new ObjectMapper().writeValueAsString(withConditions));
		posting.applyDetails(new JobDetails(null, "https://acme.com/vaga", WorkModel.REMOTE, null, null, null, null, null, null));
		when(repository.findById(posting.id())).thenReturn(Optional.of(posting));

		JobOffer offer = service.offer(posting.id()).orElseThrow();

		assertThat(offer.company()).isEqualTo("Acme Ltda");
		assertThat(offer.workModel()).isEqualTo(WorkModel.REMOTE);
		assertThat(offer.contractType()).isEqualTo(ContractType.PJ);
		assertThat(offer.salaryMax()).isEqualByComparingTo("11000");
		assertThat(offer.benefits()).containsExactly("PLR");
		assertThat(offer.sourceUrl()).isEqualTo("https://acme.com/vaga");
		assertThat(offer.seniority()).isEqualTo("pleno");
	}

	@Test
	void buildsAnOfferOnlyFromTheUserDetailsWhileTheJobIsNotStructuredYet() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting registered = new JobPosting(UUID.randomUUID(), null, "vaga de java", "hash", null);
		registered.applyDetails(new JobDetails("Acme", null, WorkModel.HYBRID, null, null, null, null, null, null));
		when(repository.findAllById(List.of(registered.id()))).thenReturn(List.of(registered));

		Map<UUID, JobOffer> offers = service.offers(List.of(registered.id()));

		assertThat(offers.get(registered.id()).company()).isEqualTo("Acme");
		assertThat(offers.get(registered.id()).contractType()).isNull();
	}

	@Test
	void theSalaryAndBenefitsTypedByTheUserOverrideTheOnesExtractedByTheAi() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting registered = new JobPosting(UUID.randomUUID(), null, "vaga de java", "hash", null);
		registered.applyDetails(new JobDetails(null, null, null, "https://meet.example.com/1", new java.math.BigDecimal("9000"),
				List.of("VR", " ", "Plano de saúde"), null, null, null));
		when(repository.findAllById(List.of(registered.id()))).thenReturn(List.of(registered));

		JobOffer offer = service.offers(List.of(registered.id())).get(registered.id());

		assertThat(offer.interviewUrl()).isEqualTo("https://meet.example.com/1");
		assertThat(offer.salaryMin()).isEqualByComparingTo("9000");
		assertThat(offer.salaryMax()).isEqualByComparingTo("9000");
		assertThat(offer.benefits()).containsExactly("VR", "Plano de saúde");
	}

	@Test
	void theTitleAndSeniorityTypedByTheUserOverrideTheOnesExtractedByTheAi() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobStructured extracted = new JobStructured("Dev Java", "pleno", 3, null, List.of(), List.of("Java"), Map.of());
		when(repository.findByTextHash(any())).thenReturn(Optional.empty());
		when(aiPort.structureJob(any())).thenReturn(new AiResult<>(extracted, new AiUsage("gpt-4o-mini", 10, 5, BigDecimal.ONE, false)));

		JobStructured result = service.structureFromText("vaga de java",
				new JobDetails(null, null, null, null, null, null, "Engenheiro Backend", "Sênior", null)).structured();

		assertThat(result.title()).isEqualTo("Engenheiro Backend");
		assertThat(result.seniority()).isEqualTo("Sênior");
		assertThat(result.requiredKeywords()).containsExactly("Java");
	}

	@Test
	void theContractTypeTypedByTheUserOverridesTheOneExtractedByTheAi() {
		service = new JobStructuringServiceImpl(aiPort, repository, new ObjectMapper());
		JobPosting registered = new JobPosting(UUID.randomUUID(), null, "vaga de java", "hash", null);
		registered.applyDetails(new JobDetails(null, null, null, null, null, null, null, null, ContractType.PJ));
		when(repository.findAllById(List.of(registered.id()))).thenReturn(List.of(registered));

		JobOffer offer = service.offers(List.of(registered.id())).get(registered.id());

		assertThat(offer.contractType()).isEqualTo(ContractType.PJ);
	}
}
