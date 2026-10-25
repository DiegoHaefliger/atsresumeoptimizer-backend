package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSearchRunService;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.IncompleteSearchCriteriaException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.RunTrigger;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchAlreadyRunningException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchProgress;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteriaProvider;
import com.diegohaefliger.atsresumeoptimizer.preference.SearchSchedule;
import jakarta.annotation.PreDestroy;

@Service
class JobSearchRunServiceImpl implements JobSearchRunService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobSearchRunServiceImpl.class);

	private final JobSearchCriteriaProvider criteriaProvider;
	private final JobSearchCollector collector;
	private final SearchRunRepository runRepository;
	private final SearchRunEntityMapper runMapper;
	private final SearchProgressTracker progressTracker;
	private final Clock clock;
	private final Instant bootedAt;
	private final AtomicBoolean running = new AtomicBoolean();
	private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "job-search");
		thread.setDaemon(true);
		return thread;
	});

	JobSearchRunServiceImpl(JobSearchCriteriaProvider criteriaProvider, JobSearchCollector collector,
			SearchRunRepository runRepository, SearchRunEntityMapper runMapper, SearchProgressTracker progressTracker, Clock clock) {
		this.criteriaProvider = criteriaProvider;
		this.collector = collector;
		this.runRepository = runRepository;
		this.runMapper = runMapper;
		this.progressTracker = progressTracker;
		this.clock = clock;
		this.bootedAt = Instant.now(clock);
	}

	@Override
	public void startManual() {
		JobSearchCriteria criteria = criteriaProvider.current();
		if (!criteria.missingFields().isEmpty()) {
			throw new IncompleteSearchCriteriaException(criteria.missingFields());
		}
		start(RunTrigger.MANUAL, criteria);
	}

	@Override
	public void startIfDue() {
		JobSearchCriteria criteria = criteriaProvider.current();
		if (!criteria.enabled() || !criteria.missingFields().isEmpty() || running.get() || !due(criteria)) {
			return;
		}
		start(RunTrigger.SCHEDULED, criteria);
	}

	@Override
	public boolean running() {
		return running.get();
	}

	@Override
	public SearchProgress progress() {
		return progressTracker.snapshot();
	}

	@Override
	public Optional<SearchRun> lastRun() {
		return runRepository.findFirstByOrderByStartedAtDesc().map(runMapper::toDomain);
	}

	@Override
	public List<SearchRun> history() {
		return runRepository.findTop20ByOrderByStartedAtDesc().stream().map(runMapper::toDomain).toList();
	}

	@Override
	public Optional<Instant> nextRun() {
		JobSearchCriteria criteria = criteriaProvider.current();
		if (!criteria.enabled() || !criteria.missingFields().isEmpty()) {
			return Optional.empty();
		}
		return scheduledAfterLastRun(criteria).map(ZonedDateTime::toInstant).map(this::notBeforeMinGap);
	}

	private boolean due(JobSearchCriteria criteria) {
		Instant now = Instant.now(clock);
		return scheduledAfterLastRun(criteria).map(next -> !next.toInstant().isAfter(now)).orElse(false)
				&& !notBeforeMinGap(now).isAfter(now);
	}

	private Optional<ZonedDateTime> scheduledAfterLastRun(JobSearchCriteria criteria) {
		Instant reference = lastRun().map(SearchRun::startedAt).orElse(bootedAt);
		return Optional.ofNullable(SearchSchedule.parse(criteria.cron()).next(reference.atZone(clock.getZone())));
	}

	private Instant notBeforeMinGap(Instant candidate) {
		Instant earliest = lastRun().map(SearchRun::finishedAt).map(finished -> finished.plus(SearchSchedule.MIN_GAP))
				.orElse(candidate);
		return candidate.isBefore(earliest) ? earliest : candidate;
	}

	private void start(RunTrigger trigger, JobSearchCriteria criteria) {
		if (!running.compareAndSet(false, true)) {
			throw new SearchAlreadyRunningException();
		}
		progressTracker.reset();
		try {
			executor.execute(() -> execute(trigger, criteria));
		} catch (RuntimeException exception) {
			running.set(false);
			throw exception;
		}
	}

	private void execute(RunTrigger trigger, JobSearchCriteria criteria) {
		Instant startedAt = Instant.now(clock);
		JobSearchCollector.Outcome outcome;
		try {
			outcome = collector.collect(criteria);
		} catch (RuntimeException exception) {
			LOGGER.error("A busca de vagas falhou", exception);
			outcome = JobSearchCollector.Outcome.failed("Erro inesperado na busca. Veja o log.");
		}
		try {
			runRepository.save(new SearchRunEntity(java.util.UUID.randomUUID(), trigger, startedAt, Instant.now(clock),
					outcome.read(), outcome.added(), outcome.duplicates(), outcome.belowAdherence(), outcome.error()));
		} finally {
			running.set(false);
		}
	}

	@PreDestroy
	void shutdown() {
		executor.shutdownNow();
	}
}
