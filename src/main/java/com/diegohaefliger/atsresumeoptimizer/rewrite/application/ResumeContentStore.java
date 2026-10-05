package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.EditableResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import com.github.f4b6a3.uuid.UuidCreator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class ResumeContentStore {

	private final ResumeContentRepository repository;
	private final ObjectMapper objectMapper;

	ResumeContentStore(ResumeContentRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	void save(StoredResumeDocuments documents, ResumeTemplate template, StructuredResume content, ResumeContact contact) {
		String document = objectMapper.writeValueAsString(new ResumeContentDocument(content, contact));
		repository.save(new ResumeContentEntity(UuidCreator.getTimeOrderedEpoch(), documents.resumeId(),
				documents.docxVersionId(), documents.pdfVersionId(), template, document));
	}

	Optional<EditableResume> find(UUID resumeVersionId) {
		return repository.findFirstByDocxVersionIdOrPdfVersionId(resumeVersionId, resumeVersionId).map(entity -> {
			ResumeContentDocument document = objectMapper.readValue(entity.document(), ResumeContentDocument.class);
			return new EditableResume(null, entity.template(), document.content(), document.contact(), false);
		});
	}
}
