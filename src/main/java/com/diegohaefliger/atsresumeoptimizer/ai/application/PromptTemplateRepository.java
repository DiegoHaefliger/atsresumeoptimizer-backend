package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

	Optional<PromptTemplate> findFirstByKeyOrderByVersionDesc(String key);

	Optional<PromptTemplate> findByKeyAndVersion(String key, int version);

	List<PromptTemplate> findByKeyOrderByVersionDesc(String key);

	@Query("select t from PromptTemplate t where t.version = "
			+ "(select max(latest.version) from PromptTemplate latest where latest.key = t.key) order by t.key")
	List<PromptTemplate> findLatestOfEachKey();
}
