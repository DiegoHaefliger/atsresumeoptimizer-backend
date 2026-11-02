package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionProcessService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OverdueProcessJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(OverdueProcessJob.class);

	private final SelectionProcessService processes;
	private final NotificationSettingsService settingsService;
	private final NotificationRepository repository;
	private final NotificationDispatcher dispatcher;
	private final OverdueDraftFactory draftFactory;
	private final Clock clock;

	OverdueProcessJob(SelectionProcessService processes, NotificationSettingsService settingsService,
			NotificationRepository repository, NotificationDispatcher dispatcher, OverdueDraftFactory draftFactory,
			Clock clock) {
		this.processes = processes;
		this.settingsService = settingsService;
		this.repository = repository;
		this.dispatcher = dispatcher;
		this.draftFactory = draftFactory;
		this.clock = clock;
	}

	// sem o canal in-app não há registro para deduplicar, e os outros canais repetiriam a cada execução
	@Scheduled(fixedDelayString = "${app.notification.overdue-interval:PT15M}")
	void notifyOverdueProcesses() {
		NotificationSettings settings = settingsService.current();
		if (!settings.channels().contains(NotificationChannel.IN_APP)) {
			return;
		}
		Instant now = Instant.now(clock);
		ZoneId zone = settings.timezone();
		LocalDate today = LocalDate.ofInstant(now, zone);
		processes.list(Optional.empty()).stream()
				.filter(process -> !process.stage().isClosed())
				.forEach(process -> notifyIfOverdue(process, now, today, zone));
	}

	private void notifyIfOverdue(SelectionProcess process, Instant now, LocalDate today, ZoneId zone) {
		SelectionSchedule schedule = process.nextSchedule();
		if (schedule != null) {
			if (schedule.scheduledAt().isBefore(now)
					&& !repository.existsByTypeAndScheduleId(NotificationType.SCHEDULE_OVERDUE, schedule.id())) {
				dispatcher.dispatch(draftFactory.scheduleOverdue(process, schedule, zone));
				LOGGER.info("Etapa atrasada notificada: agendamento {}", schedule.id());
			}
			return;
		}
		if (process.nextStepOn() != null && process.nextStepOn().isBefore(today)
				&& !repository.existsByTypeAndProcessIdAndCreatedAtAfter(NotificationType.NEXT_STEP_OVERDUE, process.id(),
						process.updatedAt())) {
			dispatcher.dispatch(draftFactory.nextStepOverdue(process));
			LOGGER.info("Próximo passo atrasado notificado: processo {}", process.id());
		}
	}
}
