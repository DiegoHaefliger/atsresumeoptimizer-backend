package com.diegohaefliger.atsresumeoptimizer.release.application;

import com.diegohaefliger.atsresumeoptimizer.release.domain.AppVersion;
import com.diegohaefliger.atsresumeoptimizer.release.domain.LatestRelease;
import com.diegohaefliger.atsresumeoptimizer.release.domain.UpdateStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
class UpdateCheckServiceImpl implements UpdateCheckService {

	private static final AppVersion UNKNOWN_VERSION = new AppVersion(0, 0, 0, "dev");

	private final ReleaseGateway gateway;
	private final ReleaseProperties properties;
	private final Clock clock;
	private final AppVersion current;

	private Optional<LatestRelease> cachedLatest = Optional.empty();
	private Instant nextCheck = Instant.MIN;

	UpdateCheckServiceImpl(ReleaseGateway gateway, ReleaseProperties properties, Clock clock,
			ObjectProvider<BuildProperties> buildProperties) {
		this.gateway = gateway;
		this.properties = properties;
		this.clock = clock;
		this.current = Optional.ofNullable(buildProperties.getIfAvailable())
				.flatMap(build -> AppVersion.parse(build.getVersion()))
				.orElse(UNKNOWN_VERSION);
	}

	@Override
	public synchronized UpdateStatus status() {
		if (!properties.enabled()) {
			return UpdateStatus.of(current, Optional.empty());
		}
		Instant now = clock.instant();
		if (!now.isBefore(nextCheck)) {
			cachedLatest = gateway.latest();
			nextCheck = now.plus(cachedLatest.isPresent() ? properties.cacheTtl() : properties.failureRetry());
		}
		return UpdateStatus.of(current, cachedLatest);
	}
}
