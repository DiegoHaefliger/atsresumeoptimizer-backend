package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCalendarItem;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import java.time.Instant;
import java.util.List;

public interface GoogleCalendarService {

	GoogleConnectionStatus status();

	void saveCredentials(GoogleCredentials credentials);

	void clearCredentials();

	String authorizationUrl();

	String completeAuthorization(String code, String state);

	List<GoogleCalendarItem> events(Instant from, Instant to);

	int syncAll();

	void disconnect();
}
