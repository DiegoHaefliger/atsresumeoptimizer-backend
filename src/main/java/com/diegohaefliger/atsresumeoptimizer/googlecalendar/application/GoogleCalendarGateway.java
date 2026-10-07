package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleTokens;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCalendarItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;

interface GoogleCalendarGateway {

	String authorizationUrl(GoogleCredentials credentials, String state);

	GoogleTokens exchangeCode(GoogleCredentials credentials, String code);

	String refreshAccessToken(GoogleCredentials credentials, String refreshToken);

	String accountEmail(String accessToken);

	String saveEvent(String accessToken, String eventId, Map<String, Object> body);

	void deleteEvent(String accessToken, String eventId);

	List<GoogleCalendarItem> listEvents(String accessToken, Instant from, Instant to);

	void revoke(String token);
}
