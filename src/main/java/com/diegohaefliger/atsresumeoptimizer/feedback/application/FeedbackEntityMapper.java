package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import org.mapstruct.Mapper;

@Mapper
interface FeedbackEntityMapper {

	FeedbackSubmission toSubmission(FeedbackEntity entity);

	FeedbackAttachment toAttachment(FeedbackAttachmentEntity entity);
}
