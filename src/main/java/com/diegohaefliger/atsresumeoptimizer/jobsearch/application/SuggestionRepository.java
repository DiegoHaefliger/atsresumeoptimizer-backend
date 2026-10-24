package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

interface SuggestionRepository extends JpaRepository<SuggestionEntity, UUID> {

	List<SuggestionEntity> findByStatusOrderByFoundAtDesc(SuggestionStatus status);

	List<SuggestionEntity> findByStatus(SuggestionStatus status, Sort sort);

	long countByStatus(SuggestionStatus status);

	boolean existsBySourceAndExternalId(String source, String externalId);

	boolean existsByDedupKey(String dedupKey);

	@Query("select s.externalId from SuggestionEntity s where s.source = :source")
	Set<String> findExternalIdsBySource(@Param("source") String source);

	@Query("select s.dedupKey from SuggestionEntity s")
	Set<String> findAllDedupKeys();
}
