package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

record ContactField(String text, ContactFieldType type) {

	private static final String CONTACT_LINE_SEPARATOR = " \\| ";
	private static final Pattern LINK_TOKEN = Pattern.compile(
			"^(https?://\\S+|www\\.\\S+|[\\w.+-]+@[\\w-]+\\.[\\w.-]+|(linkedin|github)\\.com/\\S+)$",
			Pattern.CASE_INSENSITIVE);

	/** GitHub, portfolio e localização vêm separados: link de portfolio não se distingue de telefone pelo formato. */
	static List<ContactField> collect(String contactLine, String githubLine, String portfolioLine, String location) {
		List<ContactField> fields = new ArrayList<>();
		if (StringUtils.hasText(contactLine)) {
			for (String token : contactLine.split(CONTACT_LINE_SEPARATOR)) {
				fields.add(new ContactField(token, classify(token)));
			}
		}
		if (StringUtils.hasText(githubLine)) {
			fields.add(new ContactField(githubLine, ContactFieldType.GITHUB));
		}
		if (StringUtils.hasText(portfolioLine)) {
			fields.add(new ContactField(portfolioLine, ContactFieldType.PORTFOLIO));
		}
		if (StringUtils.hasText(location)) {
			fields.add(new ContactField(location, ContactFieldType.LOCATION));
		}
		return List.copyOf(fields);
	}

	static boolean isLink(String token) {
		return LINK_TOKEN.matcher(token).matches();
	}

	static String uri(String token) {
		if (token.contains("@")) {
			return "mailto:" + token;
		}
		if (token.startsWith("http://") || token.startsWith("https://")) {
			return token;
		}
		return "https://" + token;
	}

	private static ContactFieldType classify(String token) {
		String lower = token.toLowerCase(Locale.ROOT);
		if (lower.contains("@")) {
			return ContactFieldType.EMAIL;
		}
		if (lower.contains("linkedin.com")) {
			return ContactFieldType.LINKEDIN;
		}
		return ContactFieldType.PHONE;
	}
}
