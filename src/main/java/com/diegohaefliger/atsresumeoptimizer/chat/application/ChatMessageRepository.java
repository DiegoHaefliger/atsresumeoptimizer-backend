package com.diegohaefliger.atsresumeoptimizer.chat.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

	List<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

	List<ChatMessageEntity> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Limit limit);

	boolean existsByConversationId(UUID conversationId);
}
