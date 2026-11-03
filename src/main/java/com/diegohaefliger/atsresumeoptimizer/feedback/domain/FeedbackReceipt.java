package com.diegohaefliger.atsresumeoptimizer.feedback.domain;

import java.time.Instant;

public record FeedbackReceipt(Instant createdAt, boolean delivered) {
}
