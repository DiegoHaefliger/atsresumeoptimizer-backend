package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.InvalidSuggestionStateException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestionNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionCounts;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobOrigin;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;

@ExtendWith(MockitoExtension.class)
class JobSuggestionServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");

	@Mock
	private SuggestionRepository repository;

	@Mock
	private JobStructuringService jobService;

	@Mock
	private BackgroundJobStructurer structurer;

	private JobSuggestionServiceImpl service;
	private SuggestionEntity suggestion;

	@BeforeEach
	void setUp() {
		service = new JobSuggestionServiceImpl(repository, new SuggestionEntityMapperImpl(), jobService, structurer,
				Clock.fixed(NOW, ZoneOffset.UTC));
		suggestion = new SuggestionEntity(UUID.randomUUID());
		suggestion.setSource("LINKEDIN");
		suggestion.setExternalId("111");
		suggestion.setUrl("https://www.linkedin.com/jobs/view/dev-java-111");
		suggestion.setTitle("Dev Java");
		suggestion.setCompany("Acme");
		suggestion.setLocation("Curitiba, Brazil");
		suggestion.setDescription("Vamos construir APIs de pagamento");
		suggestion.setStatus(SuggestionStatus.NEW);
	}

	private void found() {
		when(repository.findById(suggestion.getId())).thenReturn(Optional.of(suggestion));
	}

	@Test
	void promotingRegistersAJobTaggedAsAutomaticSearch() {
		found();
		UUID jobId = UUID.randomUUID();
		when(jobService.register(anyString(), any(JobDetails.class))).thenReturn(jobId);

		UUID promoted = service.promote(suggestion.getId());

		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<JobDetails> details = ArgumentCaptor.forClass(JobDetails.class);
		verify(jobService).register(text.capture(), details.capture());
		verify(structurer).structureAfterCommit(text.getValue(), details.getValue());
		assertThat(text.getValue()).isEqualTo("Dev Java\nAcme · Curitiba, Brazil\n\nVamos construir APIs de pagamento");
		assertThat(details.getValue().origin()).isEqualTo(JobOrigin.AUTOMATIC_SEARCH);
		assertThat(details.getValue().sourceUrl()).isEqualTo("https://www.linkedin.com/jobs/view/dev-java-111");
		assertThat(details.getValue().company()).isEqualTo("Acme");
		assertThat(promoted).isEqualTo(jobId);
		assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.PROMOTED);
		assertThat(suggestion.getJobPostingId()).isEqualTo(jobId);
		assertThat(suggestion.getDecidedAt()).isEqualTo(NOW);
	}

	@Test
	void promotingOrganizesTheDescriptionIntoTopics() {
		found();
		suggestion.setDescription("Responsabilidades:\n• Criar APIs\n• Revisar código");
		when(jobService.register(anyString(), any(JobDetails.class))).thenReturn(UUID.randomUUID());

		service.promote(suggestion.getId());

		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(jobService).register(text.capture(), any(JobDetails.class));
		assertThat(text.getValue()).endsWith("**Responsabilidades**\n- Criar APIs\n- Revisar código");
	}

	@Test
	void promotingCleansEntitiesSavedInTheDescriptionByAnEarlierSearch() {
		found();
		suggestion.setDescription("Experiência em&nbsp;Java;&nbsp;Spring");
		when(jobService.register(anyString(), any(JobDetails.class))).thenReturn(UUID.randomUUID());

		service.promote(suggestion.getId());

		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(jobService).register(text.capture(), any(JobDetails.class));
		assertThat(text.getValue()).endsWith("Experiência em Java; Spring");
	}

	@Test
	void promotingTwiceReturnsTheSameJobWithoutRegisteringAgain() {
		found();
		UUID jobId = UUID.randomUUID();
		suggestion.setStatus(SuggestionStatus.PROMOTED);
		suggestion.setJobPostingId(jobId);

		assertThat(service.promote(suggestion.getId())).isEqualTo(jobId);

		verify(jobService, never()).register(anyString(), any());
	}

	@Test
	void aDiscardedSuggestionCanStillBePromoted() {
		found();
		suggestion.setStatus(SuggestionStatus.DISCARDED);
		when(jobService.register(anyString(), any(JobDetails.class))).thenReturn(UUID.randomUUID());

		service.promote(suggestion.getId());

		assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.PROMOTED);
	}

	@Test
	void discardingKeepsTheDecisionSoTheJobNeverComesBack() {
		found();
		when(repository.save(suggestion)).thenReturn(suggestion);

		var discarded = service.discard(suggestion.getId());

		assertThat(discarded.status()).isEqualTo(SuggestionStatus.DISCARDED);
		assertThat(suggestion.getDecidedAt()).isEqualTo(NOW);
	}

	@Test
	void cannotDiscardAPromotedSuggestion() {
		found();
		suggestion.setStatus(SuggestionStatus.PROMOTED);

		assertThatThrownBy(() -> service.discard(suggestion.getId())).isInstanceOf(InvalidSuggestionStateException.class);
	}

	@Test
	void restoringBringsADiscardedSuggestionBack() {
		found();
		suggestion.setStatus(SuggestionStatus.DISCARDED);
		suggestion.setDecidedAt(NOW);

		var restored = service.restore(suggestion.getId());

		assertThat(restored.status()).isEqualTo(SuggestionStatus.NEW);
		assertThat(suggestion.getDecidedAt()).isNull();
	}

	@Test
	void failsWhenTheSuggestionDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.promote(id)).isInstanceOf(JobSuggestionNotFoundException.class);
	}

	@Test
	void listsTheSuggestionsBelowTheMinimumHighestScoreFirst() {
		suggestion.setStatus(SuggestionStatus.FILTERED);
		suggestion.setScore(65);
		when(repository.findByStatus(eq(SuggestionStatus.FILTERED), any(Sort.class))).thenReturn(List.of(suggestion));

		var listed = service.belowAdherence();

		assertThat(listed).hasSize(1);
		assertThat(listed.getFirst().score()).isEqualTo(65);
		assertThat(listed.getFirst().status()).isEqualTo(SuggestionStatus.FILTERED);
	}

	@Test
	void sortsEveryTabByHighestScoreThenNewest() {
		when(repository.findByStatus(any(SuggestionStatus.class), any(Sort.class))).thenReturn(List.of());

		service.newSuggestions();

		var sort = org.mockito.ArgumentCaptor.forClass(Sort.class);
		org.mockito.Mockito.verify(repository).findByStatus(eq(SuggestionStatus.NEW), sort.capture());
		assertThat(sort.getValue().stream().map(order -> order.getProperty() + ":" + order.getDirection()))
				.containsExactly("score:DESC", "foundAt:DESC");
	}

	@Test
	void aSuggestionBelowTheMinimumCanStillBePromoted() {
		found();
		suggestion.setStatus(SuggestionStatus.FILTERED);
		when(jobService.register(anyString(), any(JobDetails.class))).thenReturn(UUID.randomUUID());

		service.promote(suggestion.getId());

		assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.PROMOTED);
	}

	@Test
	void listsOnlyTheNewSuggestionsAndCountsEachTab() {
		when(repository.findByStatus(eq(SuggestionStatus.NEW), any(Sort.class))).thenReturn(List.of(suggestion));
		when(repository.countByStatus(SuggestionStatus.NEW)).thenReturn(3L);
		when(repository.countByStatus(SuggestionStatus.FILTERED)).thenReturn(5L);
		when(repository.countByStatus(SuggestionStatus.DISCARDED)).thenReturn(2L);

		assertThat(service.newSuggestions()).hasSize(1);
		assertThat(service.counts()).isEqualTo(new SuggestionCounts(3, 5, 2));
	}
}
