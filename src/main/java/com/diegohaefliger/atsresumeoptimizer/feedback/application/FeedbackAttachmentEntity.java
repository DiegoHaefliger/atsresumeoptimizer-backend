package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "feedback_attachment")
class FeedbackAttachmentEntity {

	@Id
	private UUID id;

	@Column(name = "feedback_id")
	private UUID feedbackId;

	private int position;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "content_type")
	private String contentType;

	private byte[] content;

	protected FeedbackAttachmentEntity() {
	}

	FeedbackAttachmentEntity(UUID id, UUID feedbackId, int position, FeedbackAttachment attachment) {
		this.id = id;
		this.feedbackId = feedbackId;
		this.position = position;
		this.fileName = attachment.fileName();
		this.contentType = attachment.contentType();
		this.content = attachment.content();
	}

	public String getFileName() {
		return fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getContent() {
		return content;
	}
}
