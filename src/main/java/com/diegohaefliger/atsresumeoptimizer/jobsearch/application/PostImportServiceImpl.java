package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.PostImportService;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.PostAlreadyImportedException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.PostTextUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
class PostImportServiceImpl implements PostImportService {

	static final String SOURCE = "LINKEDIN_POST";
	private static final int MIN_TEXT_LENGTH = 80;
	private static final int MAX_TITLE_LENGTH = 200;
	private static final int DEDUP_HEAD = 400;

	private final LinkedInPostReader reader;
	private final SuggestionOfferFactory offerFactory;
	private final JobPreferenceMatchService matchService;
	private final SuggestionRepository repository;
	private final SuggestionEntityMapper mapper;
	private final Clock clock;

	PostImportServiceImpl(LinkedInPostReader reader, SuggestionOfferFactory offerFactory,
			JobPreferenceMatchService matchService, SuggestionRepository repository, SuggestionEntityMapper mapper,
			Clock clock) {
		this.reader = reader;
		this.offerFactory = offerFactory;
		this.matchService = matchService;
		this.repository = repository;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	public JobSuggestion importPost(String url, String text) {
		PostLink link = PostLink.parse(url);
		if (repository.existsBySourceAndExternalId(SOURCE, link.externalId())) {
			throw new PostAlreadyImportedException();
		}
		String content = content(link, text);
		String key = dedupKey(content);
		if (repository.existsByDedupKey(key)) {
			throw new PostAlreadyImportedException();
		}
		return mapper.toDomain(repository.save(entity(link, content, key)));
	}

	private String content(PostLink link, String pasted) {
		String text = pasted == null || pasted.isBlank() ? reader.read(link.uri()).orElse("") : pasted.strip();
		if (text.length() < MIN_TEXT_LENGTH) {
			throw new PostTextUnavailableException();
		}
		return text;
	}

	private SuggestionEntity entity(PostLink link, String content, String key) {
		String title = title(content);
		SourceJob job = new SourceJob(link.externalId(), link.uri().toString(), title, "", "", null);
		Integer score = matchService.match(offerFactory.offer(job, new SourceJobDetail(content, "", "")))
				.map(PreferenceMatch::score).orElse(null);
		SuggestionEntity entity = new SuggestionEntity(UuidCreator.getTimeOrderedEpoch());
		entity.setSource(SOURCE);
		entity.setExternalId(link.externalId());
		entity.setUrl(job.url());
		entity.setTitle(title);
		entity.setDescription(content);
		entity.setDedupKey(key);
		entity.setScore(score);
		entity.setStatus(SuggestionStatus.NEW);
		entity.setFoundAt(Instant.now(clock));
		return entity;
	}

	private static String title(String content) {
		String first = content.lines().map(String::strip).filter(line -> !line.isEmpty()).findFirst().orElse(content);
		return first.length() <= MAX_TITLE_LENGTH ? first : first.substring(0, MAX_TITLE_LENGTH - 1) + "…";
	}

	private static String dedupKey(String content) {
		String normalized = NormalizedText.of(content);
		return Sha256.hex(SOURCE + "|" + normalized.substring(0, Math.min(DEDUP_HEAD, normalized.length())));
	}
}
