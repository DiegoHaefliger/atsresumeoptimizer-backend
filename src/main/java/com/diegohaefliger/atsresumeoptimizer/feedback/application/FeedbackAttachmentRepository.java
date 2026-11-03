package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachmentEntity, UUID> {

	List<FeedbackAttachmentEntity> findByFeedbackIdOrderByPosition(UUID feedbackId);
}
