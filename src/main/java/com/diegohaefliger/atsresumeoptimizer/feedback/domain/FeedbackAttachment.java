package com.diegohaefliger.atsresumeoptimizer.feedback.domain;

public record FeedbackAttachment(String fileName, String contentType, byte[] content) {
}
