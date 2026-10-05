package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeCompositionService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ResumeCompositionServiceImpl implements ResumeCompositionService {

	private final ResumeDocumentWriter documentWriter;

	ResumeCompositionServiceImpl(ResumeDocumentWriter documentWriter) {
		this.documentWriter = documentWriter;
	}

	@Override
	public byte[] previewPdf(ResumeTemplate template, StructuredResume content, ResumeContact contact) {
		return documentWriter.render(template, StructuredResumeSanitizer.sanitizeGrammar(content), contact).pdf();
	}

	@Override
	@Transactional
	public ComposedResume compose(ResumeTemplate template, StructuredResume content, ResumeContact contact) {
		StructuredResume sanitized = StructuredResumeSanitizer.sanitizeNamed(content);
		return documentWriter.writeNew(sanitized.name(), template, sanitized, contact).toComposed();
	}
}
