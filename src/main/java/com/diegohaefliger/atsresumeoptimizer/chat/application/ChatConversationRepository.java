package com.diegohaefliger.atsresumeoptimizer.chat.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, UUID> {

	List<ChatConversationEntity> findAllByOrderByUpdatedAtDesc();
}
