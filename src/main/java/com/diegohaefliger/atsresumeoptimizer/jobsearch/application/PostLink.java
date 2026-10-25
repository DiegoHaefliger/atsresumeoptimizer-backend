package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.InvalidPostLinkException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record PostLink(URI uri, String externalId) {

	private static final Pattern POST_ID = Pattern.compile("(?:activity|ugcPost|share)[-:](\\d{10,})");
	private static final String HOST = "linkedin.com";

	static PostLink parse(String raw) {
		URI uri = uri(raw == null ? "" : raw.strip());
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		boolean linkedIn = host.equals(HOST) || host.endsWith("." + HOST);
		if (!"https".equalsIgnoreCase(uri.getScheme()) || !linkedIn) {
			throw new InvalidPostLinkException();
		}
		Matcher matcher = POST_ID.matcher(uri.toString());
		String id = matcher.find() ? matcher.group(1) : Sha256.hex(host + uri.getPath());
		return new PostLink(uri, id);
	}

	private static URI uri(String raw) {
		try {
			return new URI(raw);
		} catch (URISyntaxException exception) {
			throw new InvalidPostLinkException();
		}
	}
}
