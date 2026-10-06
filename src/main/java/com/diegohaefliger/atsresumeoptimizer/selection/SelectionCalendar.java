package com.diegohaefliger.atsresumeoptimizer.selection;

import java.time.Instant;
import java.util.List;

public interface SelectionCalendar {

	List<CalendarEvent> between(Instant from, Instant to);

	List<CalendarEvent> pendingBetween(Instant from, Instant to);
}
