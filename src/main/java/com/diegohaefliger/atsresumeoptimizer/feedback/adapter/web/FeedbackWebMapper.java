package com.diegohaefliger.atsresumeoptimizer.feedback.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackReceipt;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.InvalidFeedbackAttachmentException;
import java.io.IOException;
import org.mapstruct.Mapper;
import org.springframework.web.multipart.MultipartFile;

@Mapper
interface FeedbackWebMapper {

	FeedbackSubmission toDomain(FeedbackRequest request);

	FeedbackResponse toResponse(FeedbackReceipt receipt);

	default FeedbackAttachment toAttachment(MultipartFile file) {
		try {
			return new FeedbackAttachment(file.getOriginalFilename(), file.getContentType(), file.getBytes());
		} catch (IOException exception) {
			throw new InvalidFeedbackAttachmentException("Não deu para ler a imagem enviada.");
		}
	}
}
