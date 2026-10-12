package com.diegohaefliger.atsresumeoptimizer.release.adapter.web;

import java.time.Instant;

record AppVersionResponse(String current, String latest, boolean updateAvailable, String releaseUrl,
		Instant publishedAt) {
}
