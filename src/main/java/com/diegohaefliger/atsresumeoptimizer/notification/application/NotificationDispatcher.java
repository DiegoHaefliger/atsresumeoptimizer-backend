package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NotificationDispatcher {

	private final NotificationSettingsService settingsService;
	private final List<NotificationSender> senders;

	NotificationDispatcher(NotificationSettingsService settingsService, List<NotificationSender> senders) {
		this.settingsService = settingsService;
		this.senders = senders;
	}

	void dispatch(NotificationDraft draft) {
		NotificationSettings settings = settingsService.current();
		senders.stream()
				.filter(sender -> settings.channels().contains(sender.channel()))
				.forEach(sender -> sender.send(draft));
	}
}
