package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.IncompleteSearchCriteriaException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.RunTrigger;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchAlreadyRunningException;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteriaProvider;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import com.diegohaefliger.atsresumeoptimizer.preference.SearchSite;

@ExtendWith(MockitoExtension.class)
class JobSearchRunServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");
	private static final String TWICE_A_DAY = "0 0 */12 * * *";
	private static final String DAILY_AT_MIDNIGHT = "0 0 0 * * *";

	@Mock
	private JobSearchCriteriaProvider criteriaProvider;

	@Mock
	private JobSearchCollector collector;

	@Mock
	private SearchRunRepository runRepository;

	private final SearchProgressTracker progress = new SearchProgressTracker();

	private JobSearchRunServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new JobSearchRunServiceImpl(criteriaProvider, collector, runRepository, new SearchRunEntityMapperImpl(),
				progress, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@AfterEach
	void tearDown() {
		service.shutdown();
	}

	private static JobSearchCriteria criteria(boolean enabled, List<String> keywords) {
		return criteria(enabled, keywords, TWICE_A_DAY);
	}

	private static JobSearchCriteria criteria(boolean enabled, List<String> keywords, String cron) {
		return new JobSearchCriteria(keywords, List.of(), List.of("Pleno"), List.of("Curitiba"),
				List.of(WorkModel.REMOTE), List.of(ContractType.CLT), PostedWithin.PAST_WEEK, 50, 70, enabled, cron, List.of(SearchSite.values()));
	}

	private static JobSearchCriteria withoutSites() {
		return new JobSearchCriteria(List.of("java"), List.of(), List.of("Pleno"), List.of("Curitiba"),
				List.of(WorkModel.REMOTE), List.of(ContractType.CLT), PostedWithin.PAST_WEEK, 50, 70, true, TWICE_A_DAY,
				List.of());
	}

	private static JobSearchCriteria withoutFilters() {
		return new JobSearchCriteria(List.of("java"), List.of(), List.of(), List.of(), List.of(), List.of(),
				PostedWithin.PAST_WEEK, 50, 70, true, TWICE_A_DAY, List.of(SearchSite.values()));
	}

	private static SearchRunEntity lastRunFinishedAt(Instant finishedAt) {
		return new SearchRunEntity(UUID.randomUUID(), RunTrigger.SCHEDULED, finishedAt.minusSeconds(60), finishedAt, 1, 1, 0,
				0, null);
	}

	@Test
	void manualRunWorksEvenWithTheScheduleOff() {
		var criteria = criteria(false, List.of("java"));
		when(criteriaProvider.current()).thenReturn(criteria);
		when(collector.collect(criteria)).thenReturn(new JobSearchCollector.Outcome(0, 0, 0, 0, null));

		service.startManual();

		verify(runRepository, timeout(2000)).save(any(SearchRunEntity.class));
	}

	@Test
	void manualRunIsRefusedWithoutKeywords() {
		when(criteriaProvider.current()).thenReturn(criteria(false, List.of()));

		assertThatThrownBy(service::startManual).isInstanceOf(IncompleteSearchCriteriaException.class)
				.hasMessageContaining("Cargos e palavras-chave");
		verify(collector, never()).collect(any());
	}

	@Test
	void manualRunIsRefusedWhenNoSiteIsSelected() {
		when(criteriaProvider.current()).thenReturn(withoutSites());

		assertThatThrownBy(service::startManual).isInstanceOf(IncompleteSearchCriteriaException.class)
				.hasMessageContaining("Sites de busca");
		verify(collector, never()).collect(any());
	}

	@Test
	void manualRunIsRefusedAndNamesTheMissingFilters() {
		when(criteriaProvider.current()).thenReturn(withoutFilters());

		assertThatThrownBy(service::startManual).isInstanceOf(IncompleteSearchCriteriaException.class)
				.hasMessage("Preencha nas Preferências antes de buscar: Senioridade, Modelo de trabalho, Tipo de contrato, Localização.");
		verify(collector, never()).collect(any());
		verify(runRepository, never()).save(any());
	}

	@Test
	void scheduledRunSkipsWhenTheFiltersAreMissing() {
		when(criteriaProvider.current()).thenReturn(withoutFilters());

		service.startIfDue();

		verify(collector, never()).collect(any());
	}

	@Test
	void nextRunIsEmptyWhenTheFiltersAreMissing() {
		when(criteriaProvider.current()).thenReturn(withoutFilters());

		assertThat(service.nextRun()).isEmpty();
	}

	@Test
	void manualRunCollectsAndRecordsTheResult() {
		var criteria = criteria(true, List.of("java"));
		when(criteriaProvider.current()).thenReturn(criteria);
		when(collector.collect(criteria)).thenReturn(new JobSearchCollector.Outcome(10, 3, 5, 2, null));

		service.startManual();

		ArgumentCaptor<SearchRunEntity> saved = ArgumentCaptor.forClass(SearchRunEntity.class);
		verify(runRepository, timeout(2000)).save(saved.capture());
		assertThat(saved.getValue().getTrigger()).isEqualTo(RunTrigger.MANUAL);
		assertThat(saved.getValue().getRead()).isEqualTo(10);
		assertThat(saved.getValue().getAdded()).isEqualTo(3);
		assertThat(saved.getValue().getDuplicates()).isEqualTo(5);
		assertThat(saved.getValue().getBelowAdherence()).isEqualTo(2);
		assertThat(saved.getValue().getError()).isNull();
	}

	@Test
	void aCrashInTheCollectorBecomesARunWithAnError() {
		var criteria = criteria(true, List.of("java"));
		when(criteriaProvider.current()).thenReturn(criteria);
		when(collector.collect(criteria)).thenThrow(new IllegalStateException("boom"));

		service.startManual();

		ArgumentCaptor<SearchRunEntity> saved = ArgumentCaptor.forClass(SearchRunEntity.class);
		verify(runRepository, timeout(2000)).save(saved.capture());
		assertThat(saved.getValue().getError()).isNotBlank();
	}

	@Test
	void refusesASecondRunWhileOneIsInProgress() throws InterruptedException {
		var criteria = criteria(true, List.of("java"));
		when(criteriaProvider.current()).thenReturn(criteria);
		CountDownLatch inside = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		when(collector.collect(criteria)).thenAnswer(invocation -> {
			inside.countDown();
			release.await(5, TimeUnit.SECONDS);
			return new JobSearchCollector.Outcome(0, 0, 0, 0, null);
		});

		service.startManual();
		assertThat(inside.await(2, TimeUnit.SECONDS)).isTrue();

		assertThat(service.running()).isTrue();
		assertThatThrownBy(service::startManual).isInstanceOf(SearchAlreadyRunningException.class);
		release.countDown();
		verify(runRepository, timeout(2000)).save(any());
	}

	@Test
	void scheduledRunWaitsForTheNextSlotAfterBootWhenNothingRanYet() {
		when(criteriaProvider.current()).thenReturn(criteria(true, List.of("java")));
		when(runRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.empty());

		service.startIfDue();

		verify(collector, never()).collect(any());
	}

	@Test
	void scheduledRunWaitsForTheNextSlot() {
		when(criteriaProvider.current()).thenReturn(criteria(true, List.of("java"), DAILY_AT_MIDNIGHT));
		when(runRepository.findFirstByOrderByStartedAtDesc())
				.thenReturn(Optional.of(lastRunFinishedAt(NOW.minus(Duration.ofHours(10)))));

		service.startIfDue();

		verify(collector, never()).collect(any());
	}

	@Test
	void scheduledRunStartsOnceTheSlotHasPassed() {
		var criteria = criteria(true, List.of("java"));
		when(criteriaProvider.current()).thenReturn(criteria);
		when(runRepository.findFirstByOrderByStartedAtDesc())
				.thenReturn(Optional.of(lastRunFinishedAt(NOW.minus(Duration.ofHours(13)))));
		when(collector.collect(criteria)).thenReturn(new JobSearchCollector.Outcome(0, 0, 0, 0, null));

		service.startIfDue();

		ArgumentCaptor<SearchRunEntity> saved = ArgumentCaptor.forClass(SearchRunEntity.class);
		verify(runRepository, timeout(2000)).save(saved.capture());
		assertThat(saved.getValue().getTrigger()).isEqualTo(RunTrigger.SCHEDULED);
	}

	@Test
	void scheduledRunKeepsAtLeastOneHourAfterTheLastRun() {
		when(criteriaProvider.current()).thenReturn(criteria(true, List.of("java")));
		when(runRepository.findFirstByOrderByStartedAtDesc())
				.thenReturn(Optional.of(lastRunFinishedAt(NOW.minus(Duration.ofMinutes(30)))));

		service.startIfDue();

		verify(collector, never()).collect(any());
	}

	@Test
	void nextRunIsTheNextSlotAfterTheLastRun() {
		when(criteriaProvider.current()).thenReturn(criteria(true, List.of("java")));
		when(runRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.empty());

		assertThat(service.nextRun()).contains(Instant.parse("2026-10-25T00:00:00Z"));
	}

	@Test
	void nextRunIsEmptyWhenTheScheduleIsOff() {
		when(criteriaProvider.current()).thenReturn(criteria(false, List.of("java")));

		assertThat(service.nextRun()).isEmpty();
	}

	@Test
	void scheduledRunDoesNothingWhenTheSearchIsOff() {
		when(criteriaProvider.current()).thenReturn(criteria(false, List.of("java")));

		service.startIfDue();

		verify(collector, never()).collect(any());
		verify(runRepository, never()).findFirstByOrderByStartedAtDesc();
	}
}
