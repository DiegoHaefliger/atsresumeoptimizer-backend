package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ThemedPdfRenderer implements PdfTemplateRenderer {

	private static final float MARGIN = 50f;
	private static final float SECTION_GAP = 10f;
	private static final float ENTRY_GAP = 12f;
	private static final float BULLET_INDENT = 14f;
	private static final float HEADLINE_FONT_SIZE = 13f;
	private static final float SECTION_TITLE_FONT_SIZE = 12f;
	private static final float BODY_FONT_SIZE = 11f;
	private static final String RESULTS_LABEL = "Resultados:";
	private static final float TECHNOLOGIES_FONT_SIZE = 10f;
	private static final Color TEXT_COLOR = Color.BLACK;

	@Override
	public byte[] render(ResumeTheme theme, StructuredResume content, List<ContactField> contact) {
		Color accent = theme.accentColor();
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			try (PdfFlowWriter writer = new PdfFlowWriter(document, MARGIN)) {
				if (StringUtils.hasText(content.name())) {
					writer.writeLine(content.name(), true, false, theme.nameFontSize(), accent);
					writer.gap(2f);
				}
				if (StringUtils.hasText(content.headline())) {
					writer.writeLine(content.headline(), true, false, HEADLINE_FONT_SIZE, TEXT_COLOR);
					writer.gap(4f);
				}
				writer.writeContactFields(contact, BODY_FONT_SIZE, TEXT_COLOR, accent,
						theme.contactSeparator());
				for (ResumeSection section : content.sections()) {
					writer.gap(SECTION_GAP);
					writer.writeLine(section.title().toUpperCase(Locale.ROOT), true, false, SECTION_TITLE_FONT_SIZE, accent);
					writer.rule(accent);
					writer.gap(6f);
					renderSectionBody(writer, section, accent);
				}
			}
			document.save(out);
			return out.toByteArray();
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private void renderSectionBody(PdfFlowWriter writer, ResumeSection section, Color accent) throws IOException {
		switch (section.kind()) {
			case PARAGRAPH -> {
				for (RichText.Line line : RichText.fromMarkup(section.paragraph())) {
					writeLine(writer, line, false);
				}
			}
			case KEY_VALUE -> {
				for (KeyValueLine line : section.keyValues()) {
					writer.writeRich(keyValueSpans(line), BODY_FONT_SIZE, TEXT_COLOR, null, 0);
				}
			}
			case RICH_LINES -> {
				for (List<TextSpan> line : section.richLines()) {
					writeLine(writer, RichText.fromSpans(formatted(section, line)), false);
				}
			}
			case ENTRIES -> {
				for (ResumeEntry entry : section.entries()) {
					renderEntry(writer, entry, accent, section.semanticType());
				}
			}
		}
	}

	private static List<TextSpan> formatted(ResumeSection section, List<TextSpan> line) {
		return section.semanticType() == ResumeSectionSemanticType.CERTIFICATIONS ? CertificationLineFormatter.format(line)
				: line;
	}

	private void writeLine(PdfFlowWriter writer, RichText.Line line, boolean italic) throws IOException {
		boolean listItem = !line.marker().isEmpty();
		writer.writeRich(line.spans(), BODY_FONT_SIZE, TEXT_COLOR, listItem ? line.marker() : null,
				listItem ? BULLET_INDENT : 0, italic);
	}

	private static List<TextSpan> keyValueSpans(KeyValueLine line) {
		if (!StringUtils.hasText(line.label())) {
			return List.of(new TextSpan(line.value() == null ? "" : line.value(), false));
		}
		return List.of(new TextSpan(line.label() + ": ", true), new TextSpan(line.value(), false));
	}

	private void renderExperience(PdfFlowWriter writer, ResumeEntry entry, Color accent) throws IOException {
		List<TextSpan> headline = new ArrayList<>(List.of(new TextSpan(ExperienceEntryLayout.title(entry), true)));
		if (!ExperienceEntryLayout.location(entry).isEmpty()) {
			headline.add(new TextSpan(" | " + ExperienceEntryLayout.location(entry), false));
		}
		writer.writeRich(headline, BODY_FONT_SIZE, accent, null, 0);
		if (StringUtils.hasText(ExperienceEntryLayout.period(entry))) {
			writer.writeLine(ExperienceEntryLayout.period(entry), false, false, BODY_FONT_SIZE, TEXT_COLOR);
		}
		for (List<TextSpan> bullet : ExperienceEntryLayout.bulletLines(entry)) {
			writer.writeRich(RichText.fromSpans(bullet).spans(), BODY_FONT_SIZE, TEXT_COLOR, "• ", BULLET_INDENT);
		}
		if (StringUtils.hasText(entry.technologies())) {
			writer.writeRich(List.of(new TextSpan("Tecnologias: ", true), new TextSpan(entry.technologies(), false)),
					TECHNOLOGIES_FONT_SIZE, TEXT_COLOR, null, 0);
		}
		writer.gap(ENTRY_GAP);
	}

	private void renderEntry(PdfFlowWriter writer, ResumeEntry entry, Color accent,
			ResumeSectionSemanticType type) throws IOException {
		if (type == ResumeSectionSemanticType.EXPERIENCE) {
			renderExperience(writer, entry, accent);
			return;
		}
		if (type == ResumeSectionSemanticType.EDUCATION) {
			List<TextSpan> headline = new ArrayList<>(List.of(new TextSpan(EducationEntryLayout.title(entry), true)));
			if (!EducationEntryLayout.period(entry).isEmpty()) {
				headline.add(new TextSpan(EducationEntryLayout.PERIOD_SEPARATOR + EducationEntryLayout.period(entry), false));
			}
			writer.writeRich(headline, BODY_FONT_SIZE, accent, null, 0);
		} else {
			writer.writeHeadingWithTrailing(entry.heading(), entry.period(), BODY_FONT_SIZE, accent, TEXT_COLOR);
			if (StringUtils.hasText(entry.subheading())) {
				writer.writeLine(entry.subheading(), true, false, BODY_FONT_SIZE, TEXT_COLOR);
			}
		}
		if (StringUtils.hasText(entry.context())) {
			for (RichText.Line line : RichText.fromMarkup(entry.context())) {
				writeLine(writer, line, true);
			}
		}
		for (List<TextSpan> bullet : entry.bullets()) {
			writer.writeRich(RichText.fromSpans(bullet).spans(), BODY_FONT_SIZE, TEXT_COLOR, "• ", BULLET_INDENT);
		}
		if (!entry.results().isEmpty()) {
			writer.writeLine(RESULTS_LABEL, true, false, BODY_FONT_SIZE, TEXT_COLOR);
			for (List<TextSpan> result : entry.results()) {
				writer.writeRich(RichText.fromSpans(result).spans(), BODY_FONT_SIZE, TEXT_COLOR, "• ", BULLET_INDENT);
			}
		}
		if (StringUtils.hasText(entry.technologies())) {
			writer.writeRich(List.of(new TextSpan("Tecnologias: ", true), new TextSpan(entry.technologies(), false)),
					TECHNOLOGIES_FONT_SIZE, TEXT_COLOR, null, 0);
		}
		writer.gap(ENTRY_GAP);
	}
}
