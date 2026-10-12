package com.diegohaefliger.atsresumeoptimizer.interview.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface InterviewQuestionSetRepository extends JpaRepository<InterviewQuestionSetEntity, UUID> {

	@EntityGraph(attributePaths = "questions")
	List<InterviewQuestionSetEntity> findByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);
}
