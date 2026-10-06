package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import java.util.List;
import java.util.Set;

record NotificationSettingsResponse(
		List<Integer> leadMinutes,
		Set<NotificationChannel> channels,
		String timezone,
		List<ChannelOptionResponse> availableChannels) {
}
