package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

@Component
class JobSearchCollector {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobSearchCollector.class);
	private static final int HOURS_PER_DAY = 24;
	private static final int MAX_CONSECUTIVE_DETAIL_FAILURES = 3;
	private static final int SEARCH_ATTEMPTS = 2;
	private static final int TITLE_MAX = 500;
	private static final int NAME_MAX = 255;

	record Outcome(int read, int added, int duplicates, int belowAdherence, String error) {

		static Outcome failed(String error) {
			return new Outcome(0, 0, 0, 0, error);
		}
	}

	private static final class Counters {
		private int read;
		private int added;
		private int duplicates;
		private int belowAdherence;
		private final List<String> errors = new ArrayList<>();
		private int consecutiveDetailFailures;

		void fail(String message) {
			if (!errors.contains(message)) {
				errors.add(message);
			}
		}

		void add(Counters other) {
			read += other.read;
			added += other.added;
			duplicates += other.duplicates;
			belowAdherence += other.belowAdherence;
			errors.addAll(other.errors);
		}

		Outcome outcome() {
			return new Outcome(read, added, duplicates, belowAdherence, errors.isEmpty() ? null : String.join(" ", errors));
		}
	}

	private final List<JobSource> sources;
	private final RequestPacer pacer;
	private final SuggestionOfferFactory offerFactory;
	private final JobPreferenceMatchService matchService;
	private final JobStructuringService jobService;
	private final SuggestionRepository repository;
	private final JobSearchProperties properties;
	private final SearchProgressTracker progress;
	private final Clock clock;

	JobSearchCollector(List<JobSource> sources, RequestPacer pacer, SuggestionOfferFactory offerFactory,
			JobPreferenceMatchService matchService, JobStructuringService jobService, SuggestionRepository repository,
			JobSearchProperties properties, SearchProgressTracker progress, Clock clock) {
		this.sources = sources;
		this.pacer = pacer;
		this.offerFactory = offerFactory;
		this.matchService = matchService;
		this.jobService = jobService;
		this.repository = repository;
		this.properties = properties;
		this.progress = progress;
		this.clock = clock;
	}

	Outcome collect(JobSearchCriteria criteria) {
		Set<String> knownKeys = ConcurrentHashMap.newKeySet();
		knownKeys.addAll(repository.findAllDedupKeys());
		List<JobListing> registered = jobService.listJobs();
		Counters total = new Counters();
		List<JobSource> selected = sources.stream().filter(source -> isSelected(source, criteria)).toList();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<Counters>> runs = selected.stream()
					.map(source -> executor.submit(() -> collectFrom(source, criteria, registered, knownKeys)))
					.toList();
			for (Future<Counters> run : runs) {
				total.add(await(run));
			}
		}
		return total.outcome();
	}

	private static boolean isSelected(JobSource source, JobSearchCriteria criteria) {
		return criteria.sites().stream().anyMatch(site -> site.name().equals(source.name()));
	}

	private static Counters await(Future<Counters> run) {
		try {
			return run.get();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new JobSourceException("Busca interrompida.", exception);
		} catch (ExecutionException exception) {
			if (exception.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			throw new IllegalStateException(exception.getCause());
		}
	}

	private Counters collectFrom(JobSource source, JobSearchCriteria criteria, List<JobListing> registered,
			Set<String> knownKeys) {
		Counters counters = new Counters();
		try {
			Set<String> knownIds = new HashSet<>(repository.findExternalIdsBySource(source.name()));
			knownIds.addAll(registeredIds(source, registered));
			int detailsFetched = 0;
			for (SourceJob job : candidates(source, criteria, counters)) {
				if (detailsFetched >= criteria.maxResults()
						|| counters.consecutiveDetailFailures >= MAX_CONSECUTIVE_DETAIL_FAILURES) {
					break;
				}
				if (evaluate(source, job, criteria, knownIds, knownKeys, counters)) {
					detailsFetched++;
				}
			}
		} catch (JobSourceException exception) {
			LOGGER.warn("Busca interrompida em {}: {}", source.name(), exception.getMessage(), exception.getCause());
			counters.fail(exception.getMessage());
		} catch (RuntimeException exception) {
			LOGGER.error("Falha inesperada ao ler vagas de {}", source.name(), exception);
			counters.fail("Não deu pra ler as vagas da " + source.label() + ". A busca nas outras fontes continuou.");
		}
		return counters;
	}

	private List<SourceJob> candidates(JobSource source, JobSearchCriteria criteria, Counters counters) {
		Map<String, SourceJob> byId = new LinkedHashMap<>();
		int pages = Math.max(1, (int) Math.ceil(criteria.maxResults() / (double) source.pageSize()));
		List<String> locations = locationsToSearch(criteria);
		for (String keyword : criteria.keywords()) {
			for (String location : locations) {
				SourceQuery query = new SourceQuery(keyword, location, criteria.postedWithin(), criteria.workModels(),
						criteria.seniorities(), criteria.contractTypes(), 0);
				for (int page = 0; page < pages; page++) {
					Optional<List<SourceJob>> found = search(source, query, counters);
					if (found.isEmpty()) {
						break;
					}
					found.get().forEach(job -> byId.putIfAbsent(job.externalId(), job));
					if (found.get().size() < source.pageSize()) {
						break;
					}
					query = query.nextPage(source.pageSize());
				}
			}
		}
		return new ArrayList<>(byId.values());
	}

	private Optional<List<SourceJob>> search(JobSource source, SourceQuery query, Counters counters) {
		for (int attempt = 1; attempt <= SEARCH_ATTEMPTS; attempt++) {
			pacer.pause();
			try {
				return Optional.of(source.search(query));
			} catch (JobSourceException exception) {
				LOGGER.warn("Consulta em {} falhou (tentativa {}): {}", source.name(), attempt, exception.getMessage(),
						exception.getCause());
				if (attempt == SEARCH_ATTEMPTS) {
					counters.fail(exception.getMessage());
				}
			}
		}
		return Optional.empty();
	}

	private List<String> locationsToSearch(JobSearchCriteria criteria) {
		boolean remoteOnly = criteria.workModels().equals(List.of(WorkModel.REMOTE));
		return remoteOnly || criteria.locations().isEmpty() ? List.of(properties.defaultLocation()) : criteria.locations();
	}

	private boolean evaluate(JobSource source, SourceJob job, JobSearchCriteria criteria, Set<String> knownIds,
			Set<String> knownKeys, Counters counters) {
		counters.read++;
		progress.read();
		String key = dedupKey(job);
		if (isExcluded(job, criteria.excludedKeywords()) || isTooOld(job.postedOn(), criteria.postedWithin())) {
			return false;
		}
		if (knownIds.contains(job.externalId()) || knownKeys.contains(key)) {
			counters.duplicates++;
			return false;
		}
		Optional<SourceJobDetail> detail = job.detail() != null ? Optional.of(job.detail()) : fetchDetail(source, job, counters);
		if (detail.isEmpty() || isTooOld(job.postedOn() != null ? job.postedOn() : detail.get().postedOn(), criteria.postedWithin())) {
			return job.detail() == null;
		}
		JobOffer offer = offerFactory.offer(job, detail.get());
		Integer score = matchService.match(offer).map(PreferenceMatch::score).orElse(null);
		boolean adherent = score != null && score >= criteria.minAdherence();
		if (!knownKeys.add(key)) {
			counters.duplicates++;
			return job.detail() == null;
		}
		save(source, job, detail.get(), key, score, adherent ? SuggestionStatus.NEW : SuggestionStatus.FILTERED);
		knownIds.add(job.externalId());
		if (adherent) {
			counters.added++;
			progress.added();
		} else {
			counters.belowAdherence++;
			progress.belowAdherence();
		}
		return job.detail() == null;
	}

	private Optional<SourceJobDetail> fetchDetail(JobSource source, SourceJob job, Counters counters) {
		pacer.pause();
		try {
			Optional<SourceJobDetail> detail = source.detail(job.externalId());
			counters.consecutiveDetailFailures = 0;
			return detail;
		} catch (JobSourceException exception) {
			LOGGER.warn("Detalhe da vaga {} em {} falhou: {}", job.externalId(), source.name(), exception.getMessage(),
					exception.getCause());
			counters.consecutiveDetailFailures++;
			counters.fail(exception.getMessage());
			return Optional.empty();
		}
	}

	private boolean isTooOld(LocalDate postedOn, PostedWithin postedWithin) {
		Duration window = postedWithin.window();
		if (postedOn == null || window == null) {
			return false;
		}
		long days = Math.max(1, (window.toHours() + HOURS_PER_DAY - 1) / HOURS_PER_DAY);
		return postedOn.isBefore(LocalDate.now(clock).minusDays(days));
	}

	private void save(JobSource source, SourceJob job, SourceJobDetail detail, String key, Integer score,
			SuggestionStatus status) {
		SuggestionEntity entity = new SuggestionEntity(UuidCreator.getTimeOrderedEpoch());
		entity.setSource(source.name());
		entity.setExternalId(job.externalId());
		entity.setUrl(job.url());
		entity.setTitle(truncated(job.title(), TITLE_MAX));
		entity.setCompany(truncated(job.company(), NAME_MAX));
		entity.setLocation(truncated(job.location(), NAME_MAX));
		entity.setPostedOn(job.postedOn() != null ? job.postedOn() : detail.postedOn());
		entity.setDescription(detail.description());
		entity.setDedupKey(key);
		entity.setScore(score);
		entity.setStatus(status);
		entity.setFoundAt(Instant.now(clock));
		repository.save(entity);
	}

	private static String truncated(String value, int max) {
		return value != null && value.length() > max ? value.substring(0, max) : value;
	}

	private static Set<String> registeredIds(JobSource source, List<JobListing> registered) {
		Set<String> ids = new HashSet<>();
		for (JobListing listing : registered) {
			source.externalIdOf(listing.offer().sourceUrl()).ifPresent(ids::add);
		}
		return ids;
	}

	private static boolean isExcluded(SourceJob job, List<String> excludedKeywords) {
		String text = NormalizedText.of(job.title() + " " + job.company());
		return excludedKeywords.stream().map(NormalizedText::of).anyMatch(term -> NormalizedText.containsWord(text, term));
	}

	private static String dedupKey(SourceJob job) {
		return Sha256.hex(NormalizedText.of(job.company()) + "|" + NormalizedText.of(job.title()) + "|"
				+ NormalizedText.of(job.location()));
	}
}
