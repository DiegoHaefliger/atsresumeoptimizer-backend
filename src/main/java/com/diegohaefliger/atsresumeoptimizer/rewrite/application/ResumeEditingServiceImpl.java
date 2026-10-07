package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.ResumeAnalysisPipeline;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionInfo;
import com.diegohaefliger.atsresumeoptimizer.rewrite.EditableResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ExportedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeFormat;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeEditingService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
class ResumeEditingServiceImpl implements ResumeEditingService {

	private final ResumeService resumeService;
	private final ResumeAnalysisPipeline parsingPipeline;
	private final ResumeContentStore contentStore;
	private final ResumeDocumentWriter documentWriter;
	private final ResumeExportName exportName;

	ResumeEditingServiceImpl(ResumeService resumeService, ResumeAnalysisPipeline parsingPipeline,
			ResumeContentStore contentStore, ResumeDocumentWriter documentWriter, ResumeExportName exportName) {
		this.resumeService = resumeService;
		this.parsingPipeline = parsingPipeline;
		this.contentStore = contentStore;
		this.documentWriter = documentWriter;
		this.exportName = exportName;
	}

	@Override
	@Transactional(readOnly = true)
	public EditableResume load(UUID resumeId, UUID resumeVersionId) {
		resumeService.assertExists(resumeId, resumeVersionId);
		return contentStore.find(resumeVersionId)
				.orElseGet(() -> importFromFile(resumeVersionId))
				.withTitle(resumeService.title(resumeId));
	}

	@Override
	@Transactional
	public ComposedResume save(UUID resumeId, UUID resumeVersionId, String title, ResumeTemplate template,
			StructuredResume content, ResumeContact contact) {
		resumeService.assertExists(resumeId, resumeVersionId);
		if (StringUtils.hasText(title)) {
			resumeService.rename(resumeId, title);
		}
		return documentWriter.writeEdited(resumeVersionId, template, StructuredResumeSanitizer.sanitizeNamed(content), contact)
				.toComposed();
	}

	@Override
	@Transactional(readOnly = true)
	public ExportedResume export(UUID resumeId, UUID resumeVersionId, ResumeFormat format) {
		ResumeVersionInfo stored = resumeService.getVersionInfo(resumeId, resumeVersionId);
		EditableResume editable = contentStore.find(resumeVersionId).orElseGet(() -> importFromFile(stored.content()));
		String fileName = exportName.baseName(resumeId, resumeVersionId, editable.content().name()) + format.extension();
		if (format.mimeType().equals(stored.mimeType())) {
			return new ExportedResume(stored.content(), fileName, format.mimeType());
		}
		RenderedResume rendered = documentWriter.render(editable.template(),
				StructuredResumeSanitizer.sanitizeNamed(editable.content()), editable.contact());
		byte[] content = format == ResumeFormat.PDF ? rendered.pdf() : rendered.docx();
		return new ExportedResume(content, fileName, format.mimeType());
	}

	private EditableResume importFromFile(UUID resumeVersionId) {
		return importFromFile(resumeService.downloadContent(resumeVersionId));
	}

	private EditableResume importFromFile(byte[] file) {
		ParsingResult parsingResult = parsingPipeline.analyze(file);
		return new EditableResume(null, ResumeTemplate.CLASSIC, ParsedResumeImporter.content(parsingResult),
				ParsedResumeImporter.contact(parsingResult), true);
	}
}
