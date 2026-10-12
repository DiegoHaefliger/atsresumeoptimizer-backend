package com.diegohaefliger.atsresumeoptimizer.resume.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeRetentionJobTest {

	@Mock
	private ResumeVersionRepository resumeVersionRepository;

	@Test
	void scrubsTheTextOfVersionsOlderThanTheRetentionWindow() {
		ResumeRetentionJob job = new ResumeRetentionJob(resumeVersionRepository, 90);
		ResumeVersion expired = new ResumeVersion(
				java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "curriculo.pdf", "application/pdf", 10, "sha",
				new NormalizedDocument(SourceFormat.PDF, "texto", "texto", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false));
		when(resumeVersionRepository.findByCreatedAtBeforeAndFileNameNot(any(), eq(ResumeVersion.SCRUBBED_PLACEHOLDER)))
				.thenReturn(List.of(expired));

		job.expireOldVersions();

		assertThat(expired.isScrubbed()).isTrue();
		verify(resumeVersionRepository).saveAll(List.of(expired));
	}

	@Test
	void doesNothingWhenNoVersionIsExpired() {
		ResumeRetentionJob job = new ResumeRetentionJob(resumeVersionRepository, 90);
		when(resumeVersionRepository.findByCreatedAtBeforeAndFileNameNot(any(), eq(ResumeVersion.SCRUBBED_PLACEHOLDER)))
				.thenReturn(List.of());

		job.expireOldVersions();

		verify(resumeVersionRepository, never()).saveAll(any());
	}
}
