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
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.RunTrigger;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchAlreadyRunningException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchDisabledException;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteriaProvider;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;

@ExtendWith(MockitoExtension.class)
class JobSearchRunServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");

	@Mock
	private JobSearchCriteriaProvider criteriaProvider;

	@Mock
	private JobSearchCollector collector;

	@Mock
	private SearchRunRepository runRepository;

	private JobSearchRunServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new JobSearchRunServiceImpl(criteriaProvider, collector, runRepository, new SearchRunEntityMapperImpl(),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@AfterEach
	void tearDown() {
		service.shutdown();
	}

	private static JobSearchCriteria criteria(boolean enabled, List<String> keywords) {
		return new JobSearchCriteria(keywords, List.of(), List.of(), List.of(), List.of(), List.of(),
				PostedWithin.PAST_WEEK, 50, 70, enabled, Duration.ofHours(12));
	}

	private static SearchRunEntity lastRunFinishedAt(Instant finishedAt) {
		return new SearchRunEntity(UUID.randomUUID(), RunTrigger.SCHEDULED, finishedAt.minusSeconds(60), finishedAt, 1, 1, 0,
				0, null);
	}

	@Test
	void manualRunIsRefusedWhenTheSearchIsOff() {
		when(criteriaProvider.current()).thenReturn(criteria(false, List.of("java")));

		assertThatThrownBy(service::startManual).isInstanceOf(SearchDisabledException.class);
		verify(collector, never()).collect(any());
	}

	@Test
	void manualRunIsRefusedWithoutKeywords() {
		when(criteriaProvider.current()).thenReturn(criteria(true, List.of()));

		assertThatThrownBy(service::startManual).isInstanceOf(SearchDisabledException.class);
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
	void scheduledRunStartsWhenThereIsNoPreviousRun() {
		var criteria = criteria(true, List.of("java"));
		when(criteriaProvider.current()).thenReturn(criteria);
		when(runRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.empty());
		when(collector.collect(criteria)).thenReturn(new JobSearchCollector.Outcome(0, 0, 0, 0, null));

		service.startIfDue();

		ArgumentCaptor<SearchRunEntity> saved = ArgumentCaptor.forClass(SearchRunEntity.class);
		verify(runRepository, timeout(2000)).save(saved.capture());
		assertThat(saved.getValue().getTrigger()).isEqualTo(RunTrigger.SCHEDULED);
	}

	@Test
	void scheduledRunWaitsForTheConfiguredInterval() {
		when(criteriaProvider.current()).thenReturn(criteria(true, List.of("java")));
		when(runRepository.findFirstByOrderByStartedAtDesc())
				.thenReturn(Optional.of(lastRunFinishedAt(NOW.minus(Duration.ofHours(11)))));

		service.startIfDue();

		verify(collector, never()).collect(any());
	}

	@Test
	void scheduledRunStartsOnceTheIntervalHasPassed() {
		var criteria = criteria(true, List.of("java"));
		when(criteriaProvider.current()).thenReturn(criteria);
		when(runRepository.findFirstByOrderByStartedAtDesc())
				.thenReturn(Optional.of(lastRunFinishedAt(NOW.minus(Duration.ofHours(12)))));
		when(collector.collect(criteria)).thenReturn(new JobSearchCollector.Outcome(0, 0, 0, 0, null));

		service.startIfDue();

		verify(runRepository, timeout(2000)).save(any(SearchRunEntity.class));
	}

	@Test
	void scheduledRunDoesNothingWhenTheSearchIsOff() {
		when(criteriaProvider.current()).thenReturn(criteria(false, List.of("java")));

		service.startIfDue();

		verify(collector, never()).collect(any());
		verify(runRepository, never()).findFirstByOrderByStartedAtDesc();
	}
}
