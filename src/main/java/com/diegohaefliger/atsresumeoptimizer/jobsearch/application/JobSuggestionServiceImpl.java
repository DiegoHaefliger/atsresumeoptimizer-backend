package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSuggestionService;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.InvalidSuggestionStateException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestionNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobOrigin;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;

@Service
class JobSuggestionServiceImpl implements JobSuggestionService {

	private final SuggestionRepository repository;
	private final SuggestionEntityMapper mapper;
	private final JobStructuringService jobService;
	private final Clock clock;

	JobSuggestionServiceImpl(SuggestionRepository repository, SuggestionEntityMapper mapper,
			JobStructuringService jobService, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.jobService = jobService;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobSuggestion> newSuggestions() {
		return list(SuggestionStatus.NEW);
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobSuggestion> belowAdherence() {
		Sort byScore = Sort.by(Sort.Order.desc("score").nullsLast(), Sort.Order.desc("foundAt"));
		return repository.findByStatus(SuggestionStatus.FILTERED, byScore).stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobSuggestion> discarded() {
		return list(SuggestionStatus.DISCARDED);
	}

	@Override
	@Transactional(readOnly = true)
	public long countNew() {
		return repository.countByStatus(SuggestionStatus.NEW);
	}

	@Override
	@Transactional
	public UUID promote(UUID id) {
		SuggestionEntity suggestion = find(id);
		if (suggestion.getStatus() == SuggestionStatus.PROMOTED) {
			return suggestion.getJobPostingId();
		}
		UUID jobId = jobService.registerAndStructure(jobText(suggestion), details(suggestion)).id();
		suggestion.setStatus(SuggestionStatus.PROMOTED);
		suggestion.setJobPostingId(jobId);
		suggestion.setDecidedAt(Instant.now(clock));
		repository.save(suggestion);
		return jobId;
	}

	@Override
	@Transactional
	public JobSuggestion discard(UUID id) {
		SuggestionEntity suggestion = find(id);
		if (suggestion.getStatus() == SuggestionStatus.PROMOTED) {
			throw new InvalidSuggestionStateException();
		}
		suggestion.setStatus(SuggestionStatus.DISCARDED);
		suggestion.setDecidedAt(Instant.now(clock));
		return mapper.toDomain(repository.save(suggestion));
	}

	@Override
	@Transactional
	public JobSuggestion restore(UUID id) {
		SuggestionEntity suggestion = find(id);
		if (suggestion.getStatus() == SuggestionStatus.DISCARDED) {
			suggestion.setStatus(SuggestionStatus.NEW);
			suggestion.setDecidedAt(null);
			repository.save(suggestion);
		}
		return mapper.toDomain(suggestion);
	}

	private List<JobSuggestion> list(SuggestionStatus status) {
		return repository.findByStatusOrderByFoundAtDesc(status).stream().map(mapper::toDomain).toList();
	}

	private SuggestionEntity find(UUID id) {
		return repository.findById(id).orElseThrow(JobSuggestionNotFoundException::new);
	}

	private static String jobText(SuggestionEntity suggestion) {
		String header = suggestion.getTitle()
				+ (suggestion.getCompany() == null ? "" : "\n" + suggestion.getCompany())
				+ (suggestion.getLocation() == null ? "" : " · " + suggestion.getLocation());
		return header + "\n\n" + suggestion.getDescription();
	}

	private static JobDetails details(SuggestionEntity suggestion) {
		return new JobDetails(suggestion.getCompany(), suggestion.getUrl(), null, null, null, List.of(),
				suggestion.getTitle(), null, null, null, JobOrigin.AUTOMATIC_SEARCH);
	}
}
