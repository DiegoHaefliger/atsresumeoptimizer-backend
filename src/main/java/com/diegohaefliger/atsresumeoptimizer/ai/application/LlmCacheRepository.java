package com.diegohaefliger.atsresumeoptimizer.ai.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface LlmCacheRepository extends JpaRepository<LlmCacheEntry, String> {

	// Nativa por causa do ON CONFLICT: duas chamadas iguais em paralelo gravam a mesma chave e não é erro.
	@Modifying
	@Transactional
	@Query(value = """
			insert into llm_cache (key_hash, prompt_key, prompt_version, response)
			values (:keyHash, :promptKey, :promptVersion, cast(:response as jsonb))
			on conflict (key_hash) do nothing""", nativeQuery = true)
	int saveIfAbsent(@Param("keyHash") String keyHash, @Param("promptKey") String promptKey,
			@Param("promptVersion") int promptVersion, @Param("response") String response);
}
