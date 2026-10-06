package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
class CalendarController {

	private final SelectionCalendar calendar;
	private final CalendarWebMapper mapper;

	CalendarController(SelectionCalendar calendar, CalendarWebMapper mapper) {
		this.calendar = calendar;
		this.mapper = mapper;
	}

	@GetMapping("/events")
	List<CalendarEventResponse> events(@RequestParam Instant from, @RequestParam Instant to) {
		return calendar.between(from, to).stream().map(mapper::toResponse).toList();
	}
}
