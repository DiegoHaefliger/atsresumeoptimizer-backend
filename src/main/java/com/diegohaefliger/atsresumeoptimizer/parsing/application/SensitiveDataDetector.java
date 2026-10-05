package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** LGPD: heurística frouxa de propósito; mascarar a mais é melhor que vazar dado sensível pro provedor de IA. */
@Component
public class SensitiveDataDetector {

	private static final String MASK = "[DADO REMOVIDO]";
	private static final Pattern CPF = Pattern.compile("\\b(\\d{3})\\.?(\\d{3})\\.?(\\d{3})-?(\\d{2})\\b");
	private static final Pattern RG = Pattern.compile("\\b\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[\\dXx]\\b");
	private static final Pattern DATE_OF_BIRTH = Pattern.compile(
			"(?i)(?:nascimento|nasc\\.?|data\\s+de\\s+nasc\\w*)\\s*[:\\-]?\\s*(\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4})");

	public boolean containsSensitiveData(String text) {
		return hasValidCpf(text) || RG.matcher(text).find() || DATE_OF_BIRTH.matcher(text).find();
	}

	public String mask(String text) {
		String masked = maskValidCpfs(text);
		masked = RG.matcher(masked).replaceAll(MASK);
		masked = DATE_OF_BIRTH.matcher(masked).replaceAll(MASK);
		return masked;
	}

	private boolean hasValidCpf(String text) {
		Matcher matcher = CPF.matcher(text);
		while (matcher.find()) {
			if (isValidCpf(matcher)) {
				return true;
			}
		}
		return false;
	}

	private String maskValidCpfs(String text) {
		Matcher matcher = CPF.matcher(text);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(result, isValidCpf(matcher) ? MASK : matcher.group());
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private boolean isValidCpf(Matcher matcher) {
		String digits = matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4);
		if (allSameDigit(digits)) {
			return false;
		}
		int firstCheckDigit = checkDigit(digits.substring(0, 9), 10);
		int secondCheckDigit = checkDigit(digits.substring(0, 9) + firstCheckDigit, 11);
		return digits.charAt(9) - '0' == firstCheckDigit && digits.charAt(10) - '0' == secondCheckDigit;
	}

	private boolean allSameDigit(String digits) {
		return digits.chars().distinct().count() == 1;
	}

	private int checkDigit(String base, int firstWeight) {
		int sum = 0;
		int weight = firstWeight;
		for (int i = 0; i < base.length(); i++) {
			sum += (base.charAt(i) - '0') * weight--;
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}
}
