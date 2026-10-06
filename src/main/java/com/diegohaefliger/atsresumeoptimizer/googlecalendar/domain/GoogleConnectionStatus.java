package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

public record GoogleConnectionStatus(boolean configured, boolean connected, String accountEmail) {
}
