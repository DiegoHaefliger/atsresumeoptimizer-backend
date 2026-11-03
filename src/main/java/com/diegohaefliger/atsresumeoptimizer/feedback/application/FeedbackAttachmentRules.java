package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.InvalidFeedbackAttachmentException;
import java.util.List;
import java.util.Map;

final class FeedbackAttachmentRules {

	static final int MAX_FILES = 3;
	static final int MAX_BYTES = 3 * 1024 * 1024;

	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};
	private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/png", "png", "image/jpeg", "jpg", "image/webp", "webp");

	private FeedbackAttachmentRules() {
	}

	static void validate(List<FeedbackAttachment> attachments) {
		if (attachments.size() > MAX_FILES) {
			throw new InvalidFeedbackAttachmentException("Envie no máximo " + MAX_FILES + " imagens.");
		}
		attachments.forEach(FeedbackAttachmentRules::validate);
	}

	static String extension(String contentType) {
		return EXTENSIONS.get(contentType);
	}

	private static void validate(FeedbackAttachment attachment) {
		if (attachment.content().length == 0 || attachment.content().length > MAX_BYTES) {
			throw new InvalidFeedbackAttachmentException("Cada imagem deve ter até 3 MB.");
		}
		if (!EXTENSIONS.containsKey(attachment.contentType()) || !matchesSignature(attachment)) {
			throw new InvalidFeedbackAttachmentException("Use imagens PNG, JPG ou WebP.");
		}
	}

	private static boolean matchesSignature(FeedbackAttachment attachment) {
		byte[] content = attachment.content();
		return switch (attachment.contentType()) {
			case "image/png" -> startsWith(content, PNG);
			case "image/jpeg" -> startsWith(content, JPEG);
			default -> startsWith(content, RIFF);
		};
	}

	private static boolean startsWith(byte[] content, byte[] signature) {
		if (content.length < signature.length) {
			return false;
		}
		for (int index = 0; index < signature.length; index++) {
			if (content[index] != signature[index]) {
				return false;
			}
		}
		return true;
	}
}
