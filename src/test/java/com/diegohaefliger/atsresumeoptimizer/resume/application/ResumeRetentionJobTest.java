package com.diegohaefliger.atsresumeoptimizer.resume.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeRetentionJobTest {

	@Mock
	private ResumeVersionRepository resumeVersionRepository;
	@Mock
	private ResumeStorage storage;

	@Test
	void scrubsFileAndTextOfVersionsOlderThanTheRetentionWindow() {
		ResumeRetentionJob job = new ResumeRetentionJob(resumeVersionRepository, storage, 90);
		ResumeVersion expired = new ResumeVersion(
				java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "some/key.pdf", "curriculo.pdf", "application/pdf",
				10, "sha", 1, "texto", "texto");
		when(resumeVersionRepository.findByCreatedAtBeforeAndStorageKeyNot(any(), eq(ResumeVersion.SCRUBBED_PLACEHOLDER)))
				.thenReturn(List.of(expired));

		job.expireOldVersions();

		verify(storage).delete("some/key.pdf");
		assertThat(expired.isScrubbed()).isTrue();
		verify(resumeVersionRepository).saveAll(List.of(expired));
	}

	@Test
	void doesNothingWhenNoVersionIsExpired() {
		ResumeRetentionJob job = new ResumeRetentionJob(resumeVersionRepository, storage, 90);
		when(resumeVersionRepository.findByCreatedAtBeforeAndStorageKeyNot(any(), eq(ResumeVersion.SCRUBBED_PLACEHOLDER)))
				.thenReturn(List.of());

		job.expireOldVersions();

		verifyNoInteractions(storage);
	}
}
