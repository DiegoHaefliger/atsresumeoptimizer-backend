package com.diegohaefliger.atsresumeoptimizer.googlecalendar.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;
import org.mapstruct.Mapper;

@Mapper
interface GoogleCalendarWebMapper {

	GoogleStatusResponse toResponse(GoogleConnectionStatus status);
}
