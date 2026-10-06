package com.diegohaefliger.atsresumeoptimizer.notification.domain;

public enum NotificationChannel {
	IN_APP(true),
	EMAIL(false),
	PUSH(false);

	private final boolean available;

	NotificationChannel(boolean available) {
		this.available = available;
	}

	public boolean available() {
		return available;
	}
}
