package com.diegohaefliger.atsresumeoptimizer.candidatequestion.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface CandidateQuestionSetRepository extends JpaRepository<CandidateQuestionSetEntity, UUID> {

	@EntityGraph(attributePaths = "questions")
	List<CandidateQuestionSetEntity> findByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);
}
