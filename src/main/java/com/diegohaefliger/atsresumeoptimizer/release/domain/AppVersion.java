package com.diegohaefliger.atsresumeoptimizer.release.domain;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AppVersion(int major, int minor, int patch, String preRelease) implements Comparable<AppVersion> {

	private static final Pattern SEMVER = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?$");
	private static final Comparator<AppVersion> ORDER = Comparator.comparingInt(AppVersion::major)
			.thenComparingInt(AppVersion::minor)
			.thenComparingInt(AppVersion::patch)
			.thenComparing(AppVersion::isStable);

	public static Optional<AppVersion> parse(String text) {
		Matcher matcher = SEMVER.matcher(text == null ? "" : text.strip());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		return Optional.of(new AppVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
				Integer.parseInt(matcher.group(3)), matcher.group(4)));
	}

	public boolean isStable() {
		return preRelease == null;
	}

	public boolean isNewerThan(AppVersion other) {
		return compareTo(other) > 0;
	}

	@Override
	public int compareTo(AppVersion other) {
		return ORDER.compare(this, other);
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch + (isStable() ? "" : "-" + preRelease);
	}
}
