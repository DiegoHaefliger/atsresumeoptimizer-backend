package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.google")
record GoogleCalendarProperties(
		String redirectUri,
		String frontendUrl,
		@DefaultValue("https://accounts.google.com/o/oauth2/v2/auth") String authUrl,
		@DefaultValue("https://oauth2.googleapis.com/token") String tokenUrl,
		@DefaultValue("https://oauth2.googleapis.com/revoke") String revokeUrl,
		@DefaultValue("https://openidconnect.googleapis.com/v1/userinfo") String userinfoUrl,
		@DefaultValue("https://www.googleapis.com/calendar/v3/calendars/primary/events") String eventsUrl) {
}
