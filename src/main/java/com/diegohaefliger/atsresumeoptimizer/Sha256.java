package com.diegohaefliger.atsresumeoptimizer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256 {

	private static final String ALGORITHM = "SHA-256";

	private Sha256() {
	}

	public static String hex(String value) {
		return hex(value.getBytes(StandardCharsets.UTF_8));
	}

	public static String hex(byte[] value) {
		return HexFormat.of().formatHex(digest(value));
	}

	public static byte[] digest(String value) {
		return digest(value.getBytes(StandardCharsets.UTF_8));
	}

	private static byte[] digest(byte[] value) {
		try {
			return MessageDigest.getInstance(ALGORITHM).digest(value);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
