package com.diegohaefliger.atsresumeoptimizer.selection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.RecruiterContact;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidScheduleException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelectionCalendarImplTest {

	private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2026-11-01T00:00:00Z");
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID PROCESS_ID = UUID.randomUUID();

	@Mock
	private SelectionScheduleRepository scheduleRepository;

	@Mock
	private SelectionProcessRepository processRepository;

	@Mock
	private JobStructuringService jobService;

	private SelectionCalendarImpl calendar() {
		return new SelectionCalendarImpl(scheduleRepository, processRepository, jobService);
	}

	@Test
	void describesEachScheduleWithItsCompanyAndJob() {
		SelectionScheduleEntity schedule = new SelectionScheduleEntity(PROCESS_ID, SelectionStage.SCREENING, FROM);
		schedule.setScheduledAt(FROM.plusSeconds(86_400));
		SelectionProcessEntity process = new SelectionProcessEntity(PROCESS_ID, SelectionStage.SCREENING, FROM);
		process.setJobPostingId(JOB_ID);
		process.setContactName("Joana");
		process.setContactEmail("joana@acme.com");
		JobOffer offer = new JobOffer(JOB_ID, 12L, "Dev Java", "Acme", null, null, null, null, null, null, List.of(), null,
				null, "texto");
		when(scheduleRepository.findByScheduledAtBetweenOrderByScheduledAtAsc(FROM, TO)).thenReturn(List.of(schedule));
		when(processRepository.findAllById(List.of(PROCESS_ID))).thenReturn(List.of(process));
		when(jobService.offers(List.of(JOB_ID))).thenReturn(Map.of(JOB_ID, offer));

		List<CalendarEvent> events = calendar().between(FROM, TO);

		assertThat(events).singleElement().satisfies(event -> {
			assertThat(event.company()).isEqualTo("Acme");
			assertThat(event.jobTitle()).isEqualTo("Dev Java");
			assertThat(event.status()).isEqualTo(ScheduleStatus.SCHEDULED);
			assertThat(event.recruiter()).isEqualTo(new RecruiterContact("Joana", "joana@acme.com", null));
			assertThat(event.scheduledAt()).isEqualTo(FROM.plusSeconds(86_400));
		});
	}

	@Test
	void returnsNothingWhenThereAreNoSchedules() {
		when(scheduleRepository.findByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(ScheduleStatus.SCHEDULED, FROM, TO))
				.thenReturn(List.of());

		assertThat(calendar().pendingBetween(FROM, TO)).isEmpty();
		verifyNoInteractions(processRepository, jobService);
	}

	@Test
	void refusesAnInvertedOrTooWideRange() {
		assertThatThrownBy(() -> calendar().between(TO, FROM)).isInstanceOf(InvalidScheduleException.class);
		assertThatThrownBy(() -> calendar().between(FROM, FROM.plusSeconds(86_400L * 400)))
				.isInstanceOf(InvalidScheduleException.class);
	}
}
