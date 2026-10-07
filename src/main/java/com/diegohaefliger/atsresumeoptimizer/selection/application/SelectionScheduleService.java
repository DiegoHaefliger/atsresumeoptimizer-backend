package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionScheduleData;
import java.util.List;
import java.util.UUID;

public interface SelectionScheduleService {

	List<SelectionSchedule> list(UUID processId);

	SelectionSchedule schedule(UUID processId, SelectionScheduleData data);

	SelectionSchedule reschedule(UUID processId, UUID scheduleId, SelectionScheduleData data);

	SelectionSchedule complete(UUID processId, UUID scheduleId);

	SelectionSchedule cancel(UUID processId, UUID scheduleId);
}
