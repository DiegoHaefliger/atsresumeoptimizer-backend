package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

	List<NotificationEntity> findAllByOrderByCreatedAtDesc(Limit limit);

	List<NotificationEntity> findByReadAtIsNullOrderByCreatedAtDesc(Limit limit);

	long countByReadAtIsNull();

	boolean existsByScheduleIdAndLeadMinutes(UUID scheduleId, Integer leadMinutes);

	boolean existsByTypeAndScheduleId(NotificationType type, UUID scheduleId);

	boolean existsByTypeAndProcessIdAndCreatedAtAfter(NotificationType type, UUID processId, Instant createdAt);

	@Modifying
	@Query("update NotificationEntity n set n.readAt = :now where n.readAt is null")
	int markAllRead(@Param("now") Instant now);
}
