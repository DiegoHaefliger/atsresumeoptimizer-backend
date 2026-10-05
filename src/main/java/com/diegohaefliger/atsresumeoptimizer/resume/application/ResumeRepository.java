package com.diegohaefliger.atsresumeoptimizer.resume.application;

import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ResumeRepository extends JpaRepository<Resume, UUID> {

	List<Resume> findTop50ByTitleNotOrderByCreatedAtDesc(String scrubbedPlaceholder);

	List<Resume> findByOriginAndFavoriteTrue(ResumeOrigin origin);

	Optional<Resume> findFirstBySourceAnalysisIdAndTitleNotOrderByCreatedAtDesc(UUID sourceAnalysisId,
			String scrubbedPlaceholder);
}
