package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;

public interface GoogleCalendarService {

	GoogleConnectionStatus status();

	String authorizationUrl();

	String completeAuthorization(String code, String state);

	int syncAll();

	void disconnect();
}
