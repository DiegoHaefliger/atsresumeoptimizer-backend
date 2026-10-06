package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidStageMovementException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessData;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.StageMovement;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.UnknownJobPostingException;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SelectionProcessServiceImpl implements SelectionProcessService {

	private final SelectionProcessRepository repository;
	private final SelectionStageMovementRepository movementRepository;
	private final SelectionProcessEntityMapper mapper;
	private final JobStructuringService jobService;

	SelectionProcessServiceImpl(SelectionProcessRepository repository,
			SelectionStageMovementRepository movementRepository, SelectionProcessEntityMapper mapper,
			JobStructuringService jobService) {
		this.repository = repository;
		this.movementRepository = movementRepository;
		this.mapper = mapper;
		this.jobService = jobService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SelectionProcess> list(Optional<SelectionStage> stage) {
		List<SelectionProcessEntity> entities = stage
				.map(repository::findByStageOrderByUpdatedAtDesc)
				.orElseGet(repository::findAllByOrderByUpdatedAtDesc);
		Map<UUID, JobOffer> offers =
				jobService.offers(entities.stream().map(SelectionProcessEntity::getJobPostingId).distinct().toList());
		return entities.stream()
				.map(entity -> mapper.toDomain(entity, offers.get(entity.getJobPostingId()), List.of()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public SelectionProcess get(UUID id) {
		return withHistory(find(id));
	}

	@Override
	@Transactional
	public SelectionProcess create(SelectionProcessData data, SelectionStage initialStage) {
		requireJob(data.jobPostingId());
		Instant now = Instant.now();
		SelectionProcessEntity entity =
				new SelectionProcessEntity(UuidCreator.getTimeOrderedEpoch(), initialStage, now);
		mapper.update(data, entity);
		repository.save(entity);
		movementRepository.save(new SelectionStageMovementEntity(entity.getId(), initialStage, null, now));
		return withHistory(entity);
	}

	@Override
	@Transactional
	public SelectionProcess update(UUID id, SelectionProcessData data) {
		requireJob(data.jobPostingId());
		SelectionProcessEntity entity = find(id);
		mapper.update(data, entity);
		entity.setUpdatedAt(Instant.now());
		return withHistory(entity);
	}

	@Override
	@Transactional
	public SelectionProcess moveTo(UUID id, SelectionStage stage, String note) {
		SelectionProcessEntity entity = find(id);
		if (entity.getStage() == stage) {
			throw new InvalidStageMovementException("O processo já está na etapa %s.".formatted(stage.label()));
		}
		Instant now = Instant.now();
		entity.setStage(stage);
		entity.setUpdatedAt(now);
		movementRepository.save(new SelectionStageMovementEntity(id, stage, blankToNull(note), now));
		return withHistory(entity);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		repository.delete(find(id));
	}

	private void requireJob(UUID jobPostingId) {
		if (jobService.offer(jobPostingId).isEmpty()) {
			throw new UnknownJobPostingException(jobPostingId);
		}
	}

	private SelectionProcessEntity find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new SelectionProcessNotFoundException(id));
	}

	private SelectionProcess withHistory(SelectionProcessEntity entity) {
		List<StageMovement> history = movementRepository.findByProcessIdOrderByMovedAtAsc(entity.getId()).stream()
				.map(mapper::toDomain)
				.toList();
		return mapper.toDomain(entity, jobService.offer(entity.getJobPostingId()).orElse(null), history);
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.strip();
	}
}
