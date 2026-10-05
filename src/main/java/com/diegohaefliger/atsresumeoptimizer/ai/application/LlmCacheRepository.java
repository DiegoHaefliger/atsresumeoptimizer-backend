package com.diegohaefliger.atsresumeoptimizer.ai.application;

import org.springframework.data.jpa.repository.JpaRepository;

interface LlmCacheRepository extends JpaRepository<LlmCacheEntry, String> {
}
