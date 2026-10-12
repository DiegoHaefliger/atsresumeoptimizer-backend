package com.diegohaefliger.atsresumeoptimizer.release.domain;

import java.time.Instant;

public record LatestRelease(AppVersion version, String url, Instant publishedAt) {
}
