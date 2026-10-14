package com.diegohaefliger.atsresumeoptimizer.jobnote.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.JobNote;
import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.UnknownNoteJobException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JobNoteServiceImpl implements JobNoteService {

	private final JobNoteRepository repository;
	private final JobNoteEntityMapper mapper;
	private final JobStructuringService jobService;

	JobNoteServiceImpl(JobNoteRepository repository, JobNoteEntityMapper mapper, JobStructuringService jobService) {
		this.repository = repository;
		this.mapper = mapper;
		this.jobService = jobService;
	}

	@Override
	@Transactional(readOnly = true)
	public JobNote get(UUID jobPostingId) {
		requireJob(jobPostingId);
		return repository.findByJobPostingId(jobPostingId)
				.map(mapper::toDomain)
				.orElseGet(() -> JobNote.empty(jobPostingId));
	}

	@Override
	@Transactional
	public JobNote save(UUID jobPostingId, String content) {
		requireJob(jobPostingId);
		Instant now = Instant.now();
		JobNoteEntity entity = repository.findByJobPostingId(jobPostingId)
				.orElseGet(() -> new JobNoteEntity(jobPostingId, content, now));
		entity.replace(content, now);
		return mapper.toDomain(repository.save(entity));
	}

	private void requireJob(UUID jobPostingId) {
		if (jobService.offer(jobPostingId).isEmpty()) {
			throw new UnknownNoteJobException(jobPostingId);
		}
	}
}
