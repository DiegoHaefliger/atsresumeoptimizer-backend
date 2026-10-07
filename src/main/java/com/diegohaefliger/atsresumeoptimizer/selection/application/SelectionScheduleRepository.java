package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SelectionScheduleRepository extends JpaRepository<SelectionScheduleEntity, UUID> {

	List<SelectionScheduleEntity> findByProcessIdOrderByScheduledAtAsc(UUID processId);

	List<SelectionScheduleEntity> findByProcessIdInAndStatusOrderByScheduledAtAsc(
			Collection<UUID> processIds, ScheduleStatus status);

	List<SelectionScheduleEntity> findByScheduledAtBetweenOrderByScheduledAtAsc(Instant from, Instant to);

	List<SelectionScheduleEntity> findByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(
			ScheduleStatus status, Instant from, Instant to);
}
