package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.util.Set;

public record LinkedInImage(byte[] data, String mimeType) {

	public static final int MAX_BYTES = 3 * 1024 * 1024;
	private static final Set<String> ACCEPTED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	public LinkedInImage {
		if (data == null || data.length == 0) {
			throw new InvalidLinkedInSnapshotException("Imagem vazia.");
		}
		if (data.length > MAX_BYTES) {
			throw new InvalidLinkedInSnapshotException("Imagem maior que 3 MB.");
		}
		if (mimeType == null || !ACCEPTED_TYPES.contains(mimeType.strip().toLowerCase())) {
			throw new InvalidLinkedInSnapshotException("Formato de imagem não aceito. Use JPEG, PNG ou WebP.");
		}
		mimeType = mimeType.strip().toLowerCase();
	}
}
