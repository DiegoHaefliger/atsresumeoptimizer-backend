package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
class OverdueDraftFactory {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", PT_BR);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM", PT_BR);

	NotificationDraft scheduleOverdue(SelectionProcess process, SelectionSchedule schedule, ZoneId zone) {
		String message = "%s · era %s e ainda está como agendada. Atualize o processo."
				.formatted(subject(process), WHEN.format(schedule.scheduledAt().atZone(zone)));
		return new NotificationDraft(NotificationType.SCHEDULE_OVERDUE, "Etapa atrasada: " + schedule.stage().label(),
				message, process.id(), schedule.id(), null);
	}

	NotificationDraft nextStepOverdue(SelectionProcess process) {
		String message = "%s · o próximo passo era %s. Atualize o processo."
				.formatted(subject(process), DAY.format(process.nextStepOn()));
		return new NotificationDraft(NotificationType.NEXT_STEP_OVERDUE, "Próximo passo atrasado: " + process.stage().label(),
				message, process.id(), null, null);
	}

	private static String subject(SelectionProcess process) {
		return Stream.of(process.company(), process.jobTitle())
				.filter(part -> part != null && !part.isBlank())
				.collect(Collectors.joining(" - "));
	}
}
