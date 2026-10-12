package com.diegohaefliger.atsresumeoptimizer.release.domain;

import java.time.Instant;
import java.util.Optional;

public record UpdateStatus(AppVersion current, AppVersion latest, boolean updateAvailable, String releaseUrl,
		Instant publishedAt) {

	public static UpdateStatus of(AppVersion current, Optional<LatestRelease> latest) {
		return latest.map(release -> new UpdateStatus(current, release.version(), release.version().isNewerThan(current),
						release.url(), release.publishedAt()))
				.orElseGet(() -> new UpdateStatus(current, null, false, null, null));
	}
}
