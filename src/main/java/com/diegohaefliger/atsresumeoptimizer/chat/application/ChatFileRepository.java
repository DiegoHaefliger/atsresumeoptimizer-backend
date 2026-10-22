package com.diegohaefliger.atsresumeoptimizer.chat.application;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChatFileRepository extends JpaRepository<ChatFileEntity, UUID> {

	List<ChatFileEntity> findByMessageIdInOrderByIdAsc(Collection<UUID> messageIds);
}
