package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

	List<Notification> list(boolean unreadOnly, int limit);

	long unreadCount();

	Notification markRead(UUID id);

	int markAllRead();

	void delete(UUID id);
}
