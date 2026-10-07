package com.diegohaefliger.atsresumeoptimizer.notification.domain;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

public record NotificationSettings(List<Integer> leadMinutes, Set<NotificationChannel> channels, ZoneId timezone) {

	public static final NotificationSettings DEFAULT = new NotificationSettings(
			List.of(1440, 60), Set.of(NotificationChannel.IN_APP), ZoneId.of("America/Sao_Paulo"));

	public NotificationSettings {
		leadMinutes = leadMinutes.stream().sorted().toList();
		channels = Set.copyOf(channels);
	}
}
