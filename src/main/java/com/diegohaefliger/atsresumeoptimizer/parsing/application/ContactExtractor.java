package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ContactExtractor {

	private static final int HEADER_LINES = 6;

	public ContactInfo extract(String text) {
		return new ContactInfo(
			firstMatch(ContactPatterns.EMAIL, text),
			firstMatch(ContactPatterns.PHONE_BR, text).map(ContactExtractor::normalizePhone),
			firstMatch(ContactPatterns.LINKEDIN, text),
			firstMatch(ContactPatterns.GITHUB, text),
			firstMatch(ContactPatterns.LOCATION, header(text)),
			firstPortfolioMatch(text)
		);
	}

	/** "Cidade, UF" no corpo costuma ser cidade da empresa, não do candidato. */
	private static String header(String text) {
		return text.lines().filter(line -> !line.isBlank()).limit(HEADER_LINES).collect(Collectors.joining("\n"));
	}

	private Optional<String> firstMatch(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
	}

	private static String normalizePhone(String raw) {
		String digits = raw.replaceAll("\\D", "");
		if (digits.length() > 11 && digits.startsWith("55")) {
			digits = digits.substring(2);
		}
		if (digits.length() == 11) {
			return "(%s) %s-%s".formatted(digits.substring(0, 2), digits.substring(2, 7), digits.substring(7));
		}
		if (digits.length() == 10) {
			return "(%s) %s-%s".formatted(digits.substring(0, 2), digits.substring(2, 6), digits.substring(6));
		}
		return raw;
	}

	private Optional<String> firstPortfolioMatch(String text) {
		Matcher matcher = ContactPatterns.PORTFOLIO.matcher(text);
		while (matcher.find()) {
			String candidate = matcher.group();
			String lower = candidate.toLowerCase(Locale.ROOT);
			if (!lower.contains("linkedin.com") && !lower.contains("github.com")) {
				return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}
}
