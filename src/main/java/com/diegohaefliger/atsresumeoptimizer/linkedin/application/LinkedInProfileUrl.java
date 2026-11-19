package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record LinkedInProfileUrl(String url, String customUrl) {

	private static final Pattern PROFILE = Pattern.compile("linkedin\\.com/in/([A-Za-z0-9%_\\-]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern GENERATED_SUFFIX = Pattern.compile("^(?=.*\\d)[A-Za-z0-9]{8,}$");
	private static final String PROFILE_BASE = "https://www.linkedin.com/in/";
	private static final String CUSTOM_BASE = "linkedin.com/in/";

	static Optional<LinkedInProfileUrl> find(String text) {
		Matcher matcher = PROFILE.matcher(text);
		if (!matcher.find()) {
			return Optional.empty();
		}
		String slug = matcher.group(1);
		String suffix = slug.substring(slug.lastIndexOf('-') + 1);
		boolean generated = slug.contains("-") && GENERATED_SUFFIX.matcher(suffix).matches();
		return Optional.of(new LinkedInProfileUrl(PROFILE_BASE + slug + "/", generated ? null : CUSTOM_BASE + slug));
	}
}
