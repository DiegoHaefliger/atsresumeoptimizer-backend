package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionCreated;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ResumeDocumentWriter {

	private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String DOCX_EXTENSION = ".docx";
	private static final String ADAPTED_FILE_NAME = "curriculo-adaptado";
	private static final String EDITED_FILE_NAME = "curriculo-editado";
	private static final String COMPOSED_FILE_NAME = "curriculo";
	private static final String CONTACT_SEPARATOR = " | ";

	private final DocxTemplateRenderer docxRenderer;
	private final PdfTemplateRenderer pdfRenderer;
	private final ResumeService resumeService;
	private final ResumeContentStore contentStore;

	ResumeDocumentWriter(DocxTemplateRenderer docxRenderer, PdfTemplateRenderer pdfRenderer, ResumeService resumeService,
			ResumeContentStore contentStore) {
		this.docxRenderer = docxRenderer;
		this.pdfRenderer = pdfRenderer;
		this.resumeService = resumeService;
		this.contentStore = contentStore;
	}

	StoredResumeDocuments writeAdapted(UUID sourceVersionId, UUID analysisId, String titleSuffix,
			ResumeTemplate template, StructuredResume content, ResumeContact contact) {
		RenderedResume rendered = render(template, content, contact);
		ResumeVersionCreated docxVersion = resumeService.storeAdapted(sourceVersionId, analysisId, titleSuffix,
				rendered.docx(), ADAPTED_FILE_NAME + DOCX_EXTENSION, DOCX_MIME, rendered.text());
		return remember(new StoredResumeDocuments(docxVersion.resumeId(), docxVersion.resumeVersionId(), null,
				rendered.docx()), template, content, contact);
	}

	StoredResumeDocuments writeAdaptedVersion(UUID adaptedVersionId, ResumeTemplate template, StructuredResume content,
			ResumeContact contact) {
		RenderedResume rendered = render(template, content, contact);
		ResumeVersionCreated docxVersion = resumeService.storeGeneratedVersion(adaptedVersionId, rendered.docx(),
				ADAPTED_FILE_NAME + DOCX_EXTENSION, DOCX_MIME, rendered.text());
		return remember(new StoredResumeDocuments(docxVersion.resumeId(), docxVersion.resumeVersionId(), null,
				rendered.docx()), template, content, contact);
	}

	StoredResumeDocuments writeEdited(UUID sourceVersionId, ResumeTemplate template, StructuredResume content,
			ResumeContact contact) {
		RenderedResume rendered = render(template, content, contact);
		ResumeVersionCreated docxVersion = resumeService.storeGeneratedVersion(
				sourceVersionId, rendered.docx(), EDITED_FILE_NAME + DOCX_EXTENSION, DOCX_MIME, rendered.text());
		return remember(new StoredResumeDocuments(docxVersion.resumeId(), docxVersion.resumeVersionId(), null,
				rendered.docx()), template, content, contact);
	}

	StoredResumeDocuments writeNew(String title, ResumeTemplate template, StructuredResume content, ResumeContact contact) {
		RenderedResume rendered = render(template, content, contact);
		ResumeVersionCreated docxVersion = resumeService.storeUpload(
				title, rendered.docx(), COMPOSED_FILE_NAME + DOCX_EXTENSION, DOCX_MIME, rendered.text(), rendered.text(), null);
		return remember(new StoredResumeDocuments(docxVersion.resumeId(), docxVersion.resumeVersionId(), null,
				rendered.docx()), template, content, contact);
	}

	private StoredResumeDocuments remember(StoredResumeDocuments documents, ResumeTemplate template,
			StructuredResume content, ResumeContact contact) {
		contentStore.save(documents, template, content, contact);
		return documents;
	}

	RenderedResume render(ResumeTemplate template, StructuredResume content, ResumeContact contact) {
		String contactLine = Stream.of(contact.email(), contact.phone(), contact.linkedIn())
				.filter(StringUtils::hasText)
				.collect(Collectors.joining(CONTACT_SEPARATOR));
		ResumeTheme theme = ResumeTheme.of(template);
		List<ContactField> fields =
				ContactField.collect(contactLine, contact.github(), contact.portfolio(), contact.location());
		return new RenderedResume(docxRenderer.render(theme, content, fields), pdfRenderer.render(theme, content, fields),
				joinedText(content, contactLine, contact));
	}

	private String joinedText(StructuredResume content, String contactLine, ResumeContact contact) {
		StringBuilder builder = new StringBuilder();
		appendLine(builder, content.name());
		appendLine(builder, content.headline());
		appendLine(builder, Stream.of(contactLine, contact.github(), contact.portfolio(), contact.location())
				.filter(StringUtils::hasText).collect(Collectors.joining(CONTACT_SEPARATOR)));
		for (ResumeSection section : content.sections()) {
			builder.append(section.title()).append('\n');
			switch (section.kind()) {
				case PARAGRAPH -> appendLine(builder, section.paragraph());
				case KEY_VALUE -> {
					for (KeyValueLine line : section.keyValues()) {
						builder.append(line.label()).append(": ").append(line.value()).append('\n');
					}
				}
				case RICH_LINES -> section.richLines().forEach(line -> appendLine(builder, TextSpan.plainText(
						section.semanticType() == ResumeSectionSemanticType.CERTIFICATIONS
								? CertificationLineFormatter.format(line) : line)));
				case ENTRIES -> section.entries().forEach(entry -> {
					appendEntry(builder, entry, section.semanticType());
					builder.append('\n');
				});
			}
		}
		return builder.toString();
	}

	private void appendEntry(StringBuilder builder, ResumeEntry entry, ResumeSectionSemanticType type) {
		if (type == ResumeSectionSemanticType.EXPERIENCE) {
			appendLine(builder, ExperienceEntryLayout.headline(entry));
			appendLine(builder, ExperienceEntryLayout.period(entry));
			ExperienceEntryLayout.bulletLines(entry).forEach(line -> appendLine(builder, "• " + TextSpan.plainText(line)));
			if (StringUtils.hasText(entry.technologies())) {
				builder.append("Tecnologias: ").append(entry.technologies()).append('\n');
			}
			return;
		}
		if (type == ResumeSectionSemanticType.EDUCATION) {
			appendLine(builder, EducationEntryLayout.headline(entry));
		} else {
			builder.append(entry.heading());
			if (StringUtils.hasText(entry.period())) {
				builder.append(" | ").append(entry.period());
			}
			builder.append('\n');
			appendLine(builder, entry.subheading());
		}
		appendLine(builder, entry.context());
		entry.bullets().forEach(bullet -> appendLine(builder, TextSpan.plainText(bullet)));
		if (!entry.results().isEmpty()) {
			builder.append("Resultados:\n");
			entry.results().forEach(result -> appendLine(builder, TextSpan.plainText(result)));
		}
		if (StringUtils.hasText(entry.technologies())) {
			builder.append("Tecnologias: ").append(entry.technologies()).append('\n');
		}
	}

	private static void appendLine(StringBuilder builder, String text) {
		if (StringUtils.hasText(text)) {
			builder.append(text).append('\n');
		}
	}
}
