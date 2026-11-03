package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

interface FeedbackRepository extends JpaRepository<FeedbackEntity, UUID> {

	List<FeedbackEntity> findByDeliveredAtIsNullOrderByCreatedAtDesc(Limit limit);
}
