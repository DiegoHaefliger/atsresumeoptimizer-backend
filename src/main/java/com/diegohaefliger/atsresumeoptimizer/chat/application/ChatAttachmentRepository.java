package com.diegohaefliger.atsresumeoptimizer.chat.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChatAttachmentRepository extends JpaRepository<ChatAttachmentEntity, UUID> {

	List<ChatAttachmentEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

	Optional<ChatAttachmentEntity> findByConversationIdAndJobPostingId(UUID conversationId, UUID jobPostingId);

	void deleteByConversationIdAndJobPostingId(UUID conversationId, UUID jobPostingId);
}
