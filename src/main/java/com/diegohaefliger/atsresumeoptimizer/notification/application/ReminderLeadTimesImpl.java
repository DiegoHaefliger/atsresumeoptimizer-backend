package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.ReminderLeadTimes;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class ReminderLeadTimesImpl implements ReminderLeadTimes {

	private final NotificationSettingsService settingsService;

	ReminderLeadTimesImpl(NotificationSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@Override
	public List<Integer> leadMinutes() {
		return settingsService.current().leadMinutes();
	}
}
