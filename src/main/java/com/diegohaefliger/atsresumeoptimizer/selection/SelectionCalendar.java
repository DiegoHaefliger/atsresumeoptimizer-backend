package com.diegohaefliger.atsresumeoptimizer.selection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SelectionCalendar {

	List<CalendarEvent> between(Instant from, Instant to);

	List<CalendarEvent> pendingBetween(Instant from, Instant to);

	Optional<CalendarEvent> find(UUID scheduleId);
}
