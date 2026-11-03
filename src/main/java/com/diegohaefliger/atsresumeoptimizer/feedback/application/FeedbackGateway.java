package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import java.util.List;
import java.util.UUID;

interface FeedbackGateway {

	boolean deliver(UUID id, FeedbackSubmission submission, List<FeedbackAttachment> attachments);
}
