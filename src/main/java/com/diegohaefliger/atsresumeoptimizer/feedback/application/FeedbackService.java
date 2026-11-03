package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackReceipt;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import java.util.List;

public interface FeedbackService {

	FeedbackReceipt submit(FeedbackSubmission submission, List<FeedbackAttachment> attachments);
}
