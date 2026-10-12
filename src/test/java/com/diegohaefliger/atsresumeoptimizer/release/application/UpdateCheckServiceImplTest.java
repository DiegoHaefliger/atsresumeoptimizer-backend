package com.diegohaefliger.atsresumeoptimizer.release.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.release.domain.AppVersion;
import com.diegohaefliger.atsresumeoptimizer.release.domain.LatestRelease;
import com.diegohaefliger.atsresumeoptimizer.release.domain.UpdateStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;

class UpdateCheckServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");
	private static final LatestRelease RELEASE_1_1 = new LatestRelease(new AppVersion(1, 1, 0, null),
			"https://github.com/DiegoHaefliger/atsresumeoptimizer/releases/tag/v1.1.0", NOW);

	private final ReleaseGateway gateway = mock(ReleaseGateway.class);
	private final MutableClock clock = new MutableClock(NOW);

	@Test
	void reportsTheNewerReleaseWithItsPage() {
		when(gateway.latest()).thenReturn(Optional.of(RELEASE_1_1));

		UpdateStatus status = service(true, "1.0.0").status();

		assertThat(status.updateAvailable()).isTrue();
		assertThat(status.current()).hasToString("1.0.0");
		assertThat(status.latest()).hasToString("1.1.0");
		assertThat(status.releaseUrl()).endsWith("/v1.1.0");
	}

	@Test
	void saysNothingWhenAlreadyOnTheLatest() {
		when(gateway.latest()).thenReturn(Optional.of(RELEASE_1_1));

		assertThat(service(true, "1.1.0").status().updateAvailable()).isFalse();
	}

	@Test
	void asksGitHubOnlyOnceWithinTheCacheWindow() {
		when(gateway.latest()).thenReturn(Optional.of(RELEASE_1_1));
		UpdateCheckServiceImpl service = service(true, "1.0.0");

		service.status();
		clock.advance(Duration.ofHours(5));
		service.status();
		verify(gateway, times(1)).latest();

		clock.advance(Duration.ofHours(2));
		service.status();
		verify(gateway, times(2)).latest();
	}

	@Test
	void retriesSoonerAfterAFailure() {
		when(gateway.latest()).thenReturn(Optional.empty(), Optional.of(RELEASE_1_1));
		UpdateCheckServiceImpl service = service(true, "1.0.0");

		assertThat(service.status().updateAvailable()).isFalse();
		clock.advance(Duration.ofMinutes(31));

		assertThat(service.status().updateAvailable()).isTrue();
	}

	@Test
	void neverCallsGitHubWhenDisabled() {
		UpdateStatus status = service(false, "1.0.0").status();

		assertThat(status.updateAvailable()).isFalse();
		assertThat(status.current()).hasToString("1.0.0");
		verify(gateway, never()).latest();
	}

	private UpdateCheckServiceImpl service(boolean enabled, String version) {
		ReleaseProperties properties = new ReleaseProperties(enabled, "DiegoHaefliger/atsresumeoptimizer",
				"https://api.github.com", Duration.ofHours(6), Duration.ofMinutes(30), Duration.ofSeconds(5));
		Properties build = new Properties();
		build.setProperty("version", version);
		StaticListableBeanFactory beans = new StaticListableBeanFactory();
		beans.addBean("buildProperties", new BuildProperties(build));
		ObjectProvider<BuildProperties> provider = beans.getBeanProvider(BuildProperties.class);
		return new UpdateCheckServiceImpl(gateway, properties, clock, provider);
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}
	}
}
