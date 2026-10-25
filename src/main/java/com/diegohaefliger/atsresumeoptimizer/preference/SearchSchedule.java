package com.diegohaefliger.atsresumeoptimizer.preference;

import com.diegohaefliger.atsresumeoptimizer.preference.domain.InvalidJobPreferenceException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.scheduling.support.CronExpression;

public final class SearchSchedule {

	public static final String DEFAULT_CRON = "0 0 8,20 * * *";
	public static final Duration MIN_GAP = Duration.ofHours(1);
	private static final int OCCURRENCES_CHECKED = 400;
	private static final ZonedDateTime REFERENCE = LocalDateTime.of(2026, 1, 1, 0, 0).atZone(ZoneOffset.UTC);

	private SearchSchedule() {
	}

	public static CronExpression parse(String cron) {
		if (cron == null || !CronExpression.isValidExpression(cron)) {
			throw new InvalidJobPreferenceException(
					"Agendamento inválido. Use 6 campos: segundo minuto hora dia mês dia-da-semana.");
		}
		CronExpression expression = CronExpression.parse(cron);
		ZonedDateTime previous = expression.next(REFERENCE);
		for (int i = 0; i < OCCURRENCES_CHECKED && previous != null; i++) {
			ZonedDateTime next = expression.next(previous);
			if (next != null && Duration.between(previous, next).compareTo(MIN_GAP) < 0) {
				throw new InvalidJobPreferenceException("A busca automática não pode rodar com intervalo menor que 1 hora.");
			}
			previous = next;
		}
		return expression;
	}
}
