package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ResumeContentRepository extends JpaRepository<ResumeContentEntity, UUID> {

	Optional<ResumeContentEntity> findFirstByDocxVersionIdOrPdfVersionId(UUID docxVersionId, UUID pdfVersionId);
}
