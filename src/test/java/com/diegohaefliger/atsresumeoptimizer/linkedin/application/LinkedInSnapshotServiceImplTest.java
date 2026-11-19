package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidLinkedInSnapshotException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInImage;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkedInSnapshotServiceImplTest {

	@Mock
	private LinkedInSnapshotRepository repository;

	private LinkedInSnapshotServiceImpl service() {
		return new LinkedInSnapshotServiceImpl(repository, new LinkedInSnapshotEntityMapperImpl());
	}

	private static LinkedInProfile profileWithHeadline() {
		return new LinkedInProfile(null, "Ana", "Dev Java", null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null);
	}

	@Test
	void storesTheProfileAndTheImages() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		var photo = new LinkedInImage(new byte[] {1, 2}, "image/png");

		LinkedInCapture capture = service()
				.capture(new NewLinkedInSnapshot(SnapshotSource.EXTENSION, profileWithHeadline(), photo, null));
		LinkedInSnapshot saved = capture.snapshot();

		ArgumentCaptor<LinkedInSnapshotEntity> entity = ArgumentCaptor.forClass(LinkedInSnapshotEntity.class);
		verify(repository).save(entity.capture());
		assertThat(entity.getValue().getSource()).isEqualTo("EXTENSION");
		assertThat(entity.getValue().getPhotoMimeType()).isEqualTo("image/png");
		assertThat(entity.getValue().getBanner()).isNull();
		assertThat(saved.profile().headline()).isEqualTo("Dev Java");
		assertThat(saved.hasPhoto()).isTrue();
		assertThat(saved.hasBanner()).isFalse();
		assertThat(capture.unchanged()).isFalse();
		assertThat(capture.changedSections()).contains(ProfileSection.HEADLINE, ProfileSection.PHOTO);
	}

	@Test
	void doesNotCreateASnapshotWhenNothingChangedAndOnlyRefreshesTheCaptureTime() {
		var photo = new LinkedInImage(new byte[] {1, 2}, "image/png");
		var previous = new LinkedInSnapshotEntity(UUID.randomUUID());
		previous.setCapturedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
		previous.setSource("EXTENSION");
		previous.setContent(profileWithHeadline());
		previous.setPhoto(new byte[] {1, 2});
		previous.setPhotoMimeType("image/png");
		when(repository.findFirstByOrderByCapturedAtDesc()).thenReturn(Optional.of(previous));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		LinkedInCapture capture = service()
				.capture(new NewLinkedInSnapshot(SnapshotSource.EXTENSION, profileWithHeadline(), photo, null));

		assertThat(capture.unchanged()).isTrue();
		assertThat(capture.changedSections()).isEmpty();
		assertThat(capture.snapshot().id()).isEqualTo(previous.getId());
		assertThat(previous.getCapturedAt()).isAfter(java.time.Instant.parse("2026-01-01T00:00:00Z"));
	}

	@Test
	void createsANewSnapshotListingOnlyTheSectionsThatChanged() {
		var previous = new LinkedInSnapshotEntity(UUID.randomUUID());
		previous.setCapturedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
		previous.setSource("EXTENSION");
		previous.setContent(profileWithHeadline());
		when(repository.findFirstByOrderByCapturedAtDesc()).thenReturn(Optional.of(previous));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		var edited = new LinkedInProfile(null, "Ana", "Dev Java Sênior", null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null);

		LinkedInCapture capture = service().capture(new NewLinkedInSnapshot(SnapshotSource.EXTENSION, edited, null, null));

		assertThat(capture.unchanged()).isFalse();
		assertThat(capture.changedSections()).containsExactly(ProfileSection.HEADLINE);
		assertThat(capture.snapshot().id()).isNotEqualTo(previous.getId());
	}

	@Test
	void ignoresFollowersAndConnectionsWhenComparing() {
		var previous = new LinkedInSnapshotEntity(UUID.randomUUID());
		previous.setCapturedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
		previous.setSource("EXTENSION");
		previous.setContent(new LinkedInProfile(null, "Ana", "Dev Java", null, null, null, "500+", "100", null, null,
				null, null, null, null, null, null, null, null));
		when(repository.findFirstByOrderByCapturedAtDesc()).thenReturn(Optional.of(previous));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		var moreFollowers = new LinkedInProfile(null, "Ana", "Dev Java", null, null, null, "500+", "130", null, null,
				null, null, null, null, null, null, null, null);

		assertThat(service().capture(new NewLinkedInSnapshot(SnapshotSource.EXTENSION, moreFollowers, null, null)).unchanged())
				.isTrue();
	}

	@Test
	void refusesASnapshotWithNothingReadFromTheProfile() {
		var empty = new LinkedInProfile(null, "Ana", null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null);

		assertThatThrownBy(() -> service().capture(new NewLinkedInSnapshot(SnapshotSource.EXTENSION, empty, null, null)))
				.isInstanceOf(InvalidLinkedInSnapshotException.class);
		verifyNoInteractions(repository);
	}

	@Test
	void failsWhenTheSnapshotDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get(id)).isInstanceOf(LinkedInSnapshotNotFoundException.class);
	}
}
