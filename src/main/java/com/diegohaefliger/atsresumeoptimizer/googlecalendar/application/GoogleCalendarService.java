package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;

public interface GoogleCalendarService {

	GoogleConnectionStatus status();

	GoogleConnectionStatus saveCredentials(GoogleCredentials credentials);

	void removeCredentials();

	String authorizationUrl();

	String completeAuthorization(String code, String state);

	int syncAll();

	void disconnect();
}
