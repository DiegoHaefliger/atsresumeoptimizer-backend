package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import org.mapstruct.Mapper;

@Mapper
interface CalendarWebMapper {

	CalendarEventResponse toResponse(CalendarEvent event);
}
