package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;

interface NotificationSender {

	NotificationChannel channel();

	void send(NotificationDraft draft);
}
