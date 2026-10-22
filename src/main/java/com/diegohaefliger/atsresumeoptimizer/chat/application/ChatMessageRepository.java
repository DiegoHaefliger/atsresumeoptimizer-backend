package com.diegohaefliger.atsresumeoptimizer.chat.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

	List<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
