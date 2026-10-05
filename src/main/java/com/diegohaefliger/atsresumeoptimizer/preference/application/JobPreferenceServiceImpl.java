package com.diegohaefliger.atsresumeoptimizer.preference.application;

import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JobPreferenceServiceImpl implements JobPreferenceService {

	private final JobPreferenceRepository repository;
	private final JobPreferenceEntityMapper mapper;

	JobPreferenceServiceImpl(JobPreferenceRepository repository, JobPreferenceEntityMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public JobPreference current() {
		return repository.findFirstByOrderByUpdatedAtDesc().map(mapper::toDomain).orElse(JobPreference.EMPTY);
	}

	@Override
	@Transactional
	public JobPreference save(JobPreference preference) {
		JobPreferenceEntity entity = repository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new JobPreferenceEntity(UuidCreator.getTimeOrderedEpoch()));
		mapper.update(preference, entity);
		entity.setUpdatedAt(Instant.now());
		repository.save(entity);
		return mapper.toDomain(entity);
	}
}
