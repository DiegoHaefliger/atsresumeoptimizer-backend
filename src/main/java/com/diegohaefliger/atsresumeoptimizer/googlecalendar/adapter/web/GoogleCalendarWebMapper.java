package com.diegohaefliger.atsresumeoptimizer.googlecalendar.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import org.mapstruct.Mapper;

@Mapper
interface GoogleCalendarWebMapper {

	GoogleCredentials toCredentials(GoogleCredentialsRequest request);

	GoogleStatusResponse toResponse(GoogleConnectionStatus status);
}
