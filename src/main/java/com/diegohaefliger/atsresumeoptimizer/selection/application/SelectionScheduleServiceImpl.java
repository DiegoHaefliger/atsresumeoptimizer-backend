package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleChanged;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidScheduleException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.ScheduleNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionScheduleData;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SelectionScheduleServiceImpl implements SelectionScheduleService {

	private final SelectionScheduleRepository repository;
	private final SelectionProcessRepository processRepository;
	private final SelectionScheduleEntityMapper mapper;
	private final Clock clock;
	private final ApplicationEventPublisher events;

	SelectionScheduleServiceImpl(SelectionScheduleRepository repository, SelectionProcessRepository processRepository,
			SelectionScheduleEntityMapper mapper, Clock clock,
			ApplicationEventPublisher events) {
		this.repository = repository;
		this.processRepository = processRepository;
		this.mapper = mapper;
		this.clock = clock;
		this.events = events;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SelectionSchedule> list(UUID processId) {
		requireProcess(processId);
		return repository.findByProcessIdOrderByScheduledAtAsc(processId).stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional
	public SelectionSchedule schedule(UUID processId, SelectionScheduleData data) {
		SelectionProcessEntity process = processRepository.findById(processId)
				.orElseThrow(() -> new SelectionProcessNotFoundException(processId));
		SelectionScheduleEntity entity = new SelectionScheduleEntity(
				processId, data.stage() == null ? process.getStage() : data.stage(), Instant.now(clock));
		mapper.update(data, entity);
		SelectionScheduleEntity saved = repository.save(entity);
		events.publishEvent(new ScheduleChanged(saved.getId()));
		return mapper.toDomain(saved);
	}

	@Override
	@Transactional
	public SelectionSchedule reschedule(UUID processId, UUID scheduleId, SelectionScheduleData data) {
		SelectionScheduleEntity previous = findActive(processId, scheduleId);
		Instant now = Instant.now(clock);
		previous.setStatus(ScheduleStatus.RESCHEDULED);
		previous.setUpdatedAt(now);
		SelectionScheduleEntity replacement = new SelectionScheduleEntity(
				processId, data.stage() == null ? previous.getStage() : data.stage(), now);
		mapper.update(data, replacement);
		SelectionScheduleEntity saved = repository.save(replacement);
		events.publishEvent(new ScheduleChanged(previous.getId()));
		events.publishEvent(new ScheduleChanged(saved.getId()));
		return mapper.toDomain(saved);
	}

	@Override
	@Transactional
	public SelectionSchedule complete(UUID processId, UUID scheduleId) {
		return close(processId, scheduleId, ScheduleStatus.DONE);
	}

	@Override
	@Transactional
	public SelectionSchedule cancel(UUID processId, UUID scheduleId) {
		return close(processId, scheduleId, ScheduleStatus.CANCELED);
	}

	private SelectionSchedule close(UUID processId, UUID scheduleId, ScheduleStatus status) {
		SelectionScheduleEntity entity = findActive(processId, scheduleId);
		entity.setStatus(status);
		entity.setUpdatedAt(Instant.now(clock));
		events.publishEvent(new ScheduleChanged(entity.getId()));
		return mapper.toDomain(entity);
	}

	private SelectionScheduleEntity findActive(UUID processId, UUID scheduleId) {
		requireProcess(processId);
		SelectionScheduleEntity entity = repository.findById(scheduleId)
				.filter(found -> found.getProcessId().equals(processId))
				.orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
		if (entity.getStatus() != ScheduleStatus.SCHEDULED) {
			throw new InvalidScheduleException("Só agendamentos ativos podem ser alterados.");
		}
		return entity;
	}

	private void requireProcess(UUID processId) {
		if (!processRepository.existsById(processId)) {
			throw new SelectionProcessNotFoundException(processId);
		}
	}
}
