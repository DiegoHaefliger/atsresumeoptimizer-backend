package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

public record GoogleCalendarItem(String id, String title, String start, String end, boolean allDay, String link) {
}
