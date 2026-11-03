package com.diegohaefliger.atsresumeoptimizer.feedback.adapter.web;

import java.time.Instant;

record FeedbackResponse(Instant createdAt, boolean delivered) {
}
