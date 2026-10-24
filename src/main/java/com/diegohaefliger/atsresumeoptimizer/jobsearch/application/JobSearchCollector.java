package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

@Component
class JobSearchCollector {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobSearchCollector.class);
	private static final Pattern JOB_ID = Pattern.compile("(\\d{7,})(?:[/?#]|$)");

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
		private int detailsFetched;

		Outcome outcome(String error) {
			return new Outcome(read, added, duplicates, belowAdherence, error);
		}
	}

	private final JobSource source;
	private final RequestPacer pacer;
	private final SuggestionOfferFactory offerFactory;
	private final JobPreferenceMatchService matchService;
	private final JobStructuringService jobService;
	private final SuggestionRepository repository;
	private final JobSearchProperties properties;
	private final Clock clock;

	JobSearchCollector(JobSource source, RequestPacer pacer, SuggestionOfferFactory offerFactory,
			JobPreferenceMatchService matchService, JobStructuringService jobService, SuggestionRepository repository,
			JobSearchProperties properties, Clock clock) {
		this.source = source;
		this.pacer = pacer;
		this.offerFactory = offerFactory;
		this.matchService = matchService;
		this.jobService = jobService;
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	Outcome collect(JobSearchCriteria criteria) {
		Counters counters = new Counters();
		Set<String> knownIds = new java.util.HashSet<>(repository.findExternalIdsBySource(source.name()));
		knownIds.addAll(registeredIds());
		Set<String> knownKeys = new java.util.HashSet<>(repository.findAllDedupKeys());
		try {
			for (SourceJob job : candidates(criteria)) {
				if (counters.detailsFetched >= criteria.maxResults()) {
					break;
				}
				evaluate(job, criteria, knownIds, knownKeys, counters);
			}
			return counters.outcome(null);
		} catch (JobSourceException exception) {
			LOGGER.warn("Busca interrompida: {}", exception.getMessage());
			return counters.outcome(exception.getMessage());
		}
	}

	private List<SourceJob> candidates(JobSearchCriteria criteria) {
		Map<String, SourceJob> byId = new LinkedHashMap<>();
		int pages = Math.max(1, (int) Math.ceil(criteria.maxResults() / (double) source.pageSize()));
		List<String> locations = locationsToSearch(criteria);
		for (String keyword : criteria.keywords()) {
			for (String location : locations) {
				SourceQuery query = new SourceQuery(keyword, location, criteria.postedWithin(), criteria.workModels(),
						criteria.seniorities(), criteria.contractTypes(), 0);
				for (int page = 0; page < pages; page++) {
					pacer.pause();
					List<SourceJob> found = source.search(query);
					found.forEach(job -> byId.putIfAbsent(job.externalId(), job));
					if (found.size() < source.pageSize()) {
						break;
					}
					query = query.nextPage(source.pageSize());
				}
			}
		}
		return new ArrayList<>(byId.values());
	}

	private List<String> locationsToSearch(JobSearchCriteria criteria) {
		boolean remoteOnly = criteria.workModels().equals(List.of(WorkModel.REMOTE));
		return remoteOnly || criteria.locations().isEmpty() ? List.of(properties.defaultLocation()) : criteria.locations();
	}

	private void evaluate(SourceJob job, JobSearchCriteria criteria, Set<String> knownIds, Set<String> knownKeys,
			Counters counters) {
		counters.read++;
		String key = dedupKey(job);
		if (isExcluded(job, criteria.excludedKeywords())) {
			return;
		}
		if (knownIds.contains(job.externalId()) || knownKeys.contains(key)) {
			counters.duplicates++;
			return;
		}
		pacer.pause();
		counters.detailsFetched++;
		Optional<SourceJobDetail> detail = source.detail(job.externalId());
		if (detail.isEmpty()) {
			return;
		}
		JobOffer offer = offerFactory.offer(job, detail.get());
		Integer score = matchService.match(offer).map(PreferenceMatch::score).orElse(null);
		boolean adherent = score != null && score >= criteria.minAdherence();
		save(job, detail.get(), key, score, adherent ? SuggestionStatus.NEW : SuggestionStatus.FILTERED);
		knownIds.add(job.externalId());
		knownKeys.add(key);
		if (adherent) {
			counters.added++;
		} else {
			counters.belowAdherence++;
		}
	}

	private void save(SourceJob job, SourceJobDetail detail, String key, Integer score, SuggestionStatus status) {
		SuggestionEntity entity = new SuggestionEntity(UuidCreator.getTimeOrderedEpoch());
		entity.setSource(source.name());
		entity.setExternalId(job.externalId());
		entity.setUrl(job.url());
		entity.setTitle(job.title());
		entity.setCompany(job.company());
		entity.setLocation(job.location());
		entity.setPostedOn(job.postedOn());
		entity.setDescription(detail.description());
		entity.setDedupKey(key);
		entity.setScore(score);
		entity.setStatus(status);
		entity.setFoundAt(Instant.now(clock));
		repository.save(entity);
	}

	private Set<String> registeredIds() {
		Set<String> ids = new java.util.HashSet<>();
		for (JobListing listing : jobService.listJobs()) {
			String url = listing.offer().sourceUrl();
			Matcher matcher = JOB_ID.matcher(url == null ? "" : url);
			while (matcher.find()) {
				ids.add(matcher.group(1));
			}
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
