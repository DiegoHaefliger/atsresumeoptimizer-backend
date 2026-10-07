package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.RecruiterContact;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidScheduleException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SelectionCalendarImpl implements SelectionCalendar {

	private static final Duration MAX_RANGE = Duration.ofDays(366);

	private final SelectionScheduleRepository scheduleRepository;
	private final SelectionProcessRepository processRepository;
	private final JobStructuringService jobService;

	SelectionCalendarImpl(SelectionScheduleRepository scheduleRepository, SelectionProcessRepository processRepository,
			JobStructuringService jobService) {
		this.scheduleRepository = scheduleRepository;
		this.processRepository = processRepository;
		this.jobService = jobService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CalendarEvent> between(Instant from, Instant to) {
		requireValidRange(from, to);
		return toEvents(scheduleRepository.findByScheduledAtBetweenOrderByScheduledAtAsc(from, to));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CalendarEvent> pendingBetween(Instant from, Instant to) {
		requireValidRange(from, to);
		return toEvents(scheduleRepository.findByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(
				ScheduleStatus.SCHEDULED, from, to));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CalendarEvent> find(UUID scheduleId) {
		return scheduleRepository.findById(scheduleId).map(schedule -> toEvents(List.of(schedule)).getFirst());
	}

	private static void requireValidRange(Instant from, Instant to) {
		if (!from.isBefore(to)) {
			throw new InvalidScheduleException("O início do intervalo deve ser anterior ao fim.");
		}
		if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
			throw new InvalidScheduleException("O intervalo máximo é de %d dias.".formatted(MAX_RANGE.toDays()));
		}
	}

	private List<CalendarEvent> toEvents(List<SelectionScheduleEntity> schedules) {
		if (schedules.isEmpty()) {
			return List.of();
		}
		Map<UUID, SelectionProcessEntity> processes = processRepository
				.findAllById(schedules.stream().map(SelectionScheduleEntity::getProcessId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(SelectionProcessEntity::getId, Function.identity()));
		Map<UUID, JobOffer> offers = jobService.offers(
				processes.values().stream().map(SelectionProcessEntity::getJobPostingId).distinct().toList());
		return schedules.stream().map(schedule -> toEvent(schedule, processes, offers)).toList();
	}

	private static CalendarEvent toEvent(SelectionScheduleEntity schedule, Map<UUID, SelectionProcessEntity> processes,
			Map<UUID, JobOffer> offers) {
		SelectionProcessEntity process = processes.get(schedule.getProcessId());
		JobOffer offer = offers.get(process.getJobPostingId());
		return new CalendarEvent(schedule.getId(), schedule.getProcessId(), offer == null ? null : offer.company(),
				offer == null ? null : offer.title(), schedule.getStage(), schedule.getStatus(),
				schedule.getScheduledAt(), schedule.getDurationMinutes(), schedule.getLocation(), schedule.getNotes(),
				new RecruiterContact(process.getContactName(), process.getContactEmail(), process.getContactPhone()));
	}
}
