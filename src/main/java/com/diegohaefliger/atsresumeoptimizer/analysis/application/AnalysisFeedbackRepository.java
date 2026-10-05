package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AnalysisFeedbackRepository extends JpaRepository<AnalysisFeedbackEntity, UUID> {
}
