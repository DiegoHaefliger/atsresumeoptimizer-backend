package com.diegohaefliger.atsresumeoptimizer.googlecalendar.adapter.web;

record GoogleStatusResponse(
		boolean configured, boolean connected, String clientId, String accountEmail, String redirectUri) {
}
