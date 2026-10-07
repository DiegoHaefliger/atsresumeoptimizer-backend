package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;

public interface NotificationSettingsService {

	NotificationSettings current();

	NotificationSettings save(NotificationSettings settings);
}
