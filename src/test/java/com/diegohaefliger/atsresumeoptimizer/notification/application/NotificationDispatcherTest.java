package com.diegohaefliger.atsresumeoptimizer.notification.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

	private final NotificationDraft draft =
			new NotificationDraft(NotificationType.SCHEDULE_REMINDER, "Lembrete", "Acme", null, null, 60);

	@Mock
	private NotificationSettingsService settingsService;

	@Mock
	private NotificationSender inApp;

	@Mock
	private NotificationSender email;

	@Test
	void sendsOnlyThroughTheEnabledChannels() {
		when(settingsService.current())
				.thenReturn(new NotificationSettings(List.of(60), Set.of(NotificationChannel.IN_APP), ZoneId.of("UTC")));
		when(inApp.channel()).thenReturn(NotificationChannel.IN_APP);
		when(email.channel()).thenReturn(NotificationChannel.EMAIL);

		new NotificationDispatcher(settingsService, List.of(inApp, email)).dispatch(draft);

		verify(inApp).send(draft);
		verify(email, never()).send(draft);
	}
}
