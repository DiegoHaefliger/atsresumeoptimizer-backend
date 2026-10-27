package com.diegohaefliger.atsresumeoptimizer.careerprofile.application;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfileService;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CareerProfileServiceImpl implements CareerProfileService {

	private final CareerProfileRepository repository;
	private final CareerProfileEntityMapper mapper;

	CareerProfileServiceImpl(CareerProfileRepository repository, CareerProfileEntityMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CareerProfile> current() {
		return repository.findFirstByOrderByUpdatedAtDesc().map(mapper::toDomain);
	}

	@Override
	@Transactional
	public CareerProfile save(CareerProfile profile) {
		CareerProfileEntity entity = repository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new CareerProfileEntity(UuidCreator.getTimeOrderedEpoch()));
		mapper.update(profile, entity);
		entity.setUpdatedAt(Instant.now());
		repository.save(entity);
		return mapper.toDomain(entity);
	}
}
