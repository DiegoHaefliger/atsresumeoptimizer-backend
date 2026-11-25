package com.diegohaefliger.atsresumeoptimizer;

import java.security.SecureRandom;
import java.util.Optional;
import org.slf4j.MDC;

public final class RequestId {

	public static final String MDC_KEY = "requestId";

	private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final int LENGTH = 8;
	private static final SecureRandom RANDOM = new SecureRandom();

	private RequestId() {
	}

	static String generate() {
		StringBuilder id = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			id.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return id.toString();
	}

	public static Optional<String> current() {
		return Optional.ofNullable(MDC.get(MDC_KEY));
	}
}
