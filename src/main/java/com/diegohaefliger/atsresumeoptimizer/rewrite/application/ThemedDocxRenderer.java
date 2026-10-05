package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ThemedDocxRenderer implements DocxTemplateRenderer {

	private static final String TEXT_HEX = "000000";
	private static final String FONT_FAMILY = "Calibri";
	private static final int HEADLINE_FONT_SIZE = 13;
	private static final int SECTION_TITLE_FONT_SIZE = 12;
	private static final int BODY_FONT_SIZE = 11;
	private static final int TECHNOLOGIES_FONT_SIZE = 10;
	private static final int CONTENT_WIDTH_TWIPS = 9360;
	private static final int SECTION_SPACING_BEFORE = 220;
	private static final int SECTION_TITLE_SPACING_AFTER = 160;
	private static final int LINE_SPACING_AFTER = 60;
	private static final int ENTRY_SPACING_BEFORE = 240;
	private static final int BULLET_INDENT = 240;

	@Override
	public byte[] render(ResumeTheme theme, StructuredResume content, List<ContactField> contact) {
		String accent = theme.accentHex();
		try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			if (StringUtils.hasText(content.name())) {
				addRunsParagraph(document, List.of(new Run(content.name(), true, false, false, theme.nameFontSize(), accent)), 0, 40, 0);
			}
			if (StringUtils.hasText(content.headline())) {
				addRunsParagraph(document, List.of(new Run(content.headline(), true, false, false, HEADLINE_FONT_SIZE, TEXT_HEX)), 0, 100,
						0);
			}
			addContactParagraph(document, contact, theme);
			for (ResumeSection section : content.sections()) {
				addSectionTitle(document, section.title().toUpperCase(Locale.ROOT), accent);
				renderSectionBody(document, section, accent);
			}
			document.write(out);
			return out.toByteArray();
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private void renderSectionBody(XWPFDocument document, ResumeSection section, String accent) {
		switch (section.kind()) {
			case PARAGRAPH -> {
				for (RichText.Line line : RichText.fromMarkup(section.paragraph())) {
					addLineParagraph(document, line, false);
				}
			}
			case KEY_VALUE -> {
				for (KeyValueLine line : section.keyValues()) {
					addRunsParagraph(document, keyValueRuns(line), 0, LINE_SPACING_AFTER, 0);
				}
			}
			case RICH_LINES -> {
				for (List<TextSpan> line : section.richLines()) {
					addLineParagraph(document, RichText.fromSpans(formatted(section, line)), false);
				}
			}
			case ENTRIES -> {
				for (ResumeEntry entry : section.entries()) {
					renderEntry(document, entry, accent, section.semanticType());
				}
			}
		}
	}

	private static List<TextSpan> formatted(ResumeSection section, List<TextSpan> line) {
		return section.semanticType() == ResumeSectionSemanticType.CERTIFICATIONS ? CertificationLineFormatter.format(line)
				: line;
	}

	private void addLineParagraph(XWPFDocument document, RichText.Line line, boolean italic) {
		boolean listItem = !line.marker().isEmpty();
		List<Run> runs = toRuns(line.spans(), italic);
		if (listItem) {
			Run first = runs.get(0);
			runs.set(0, new Run(line.marker() + first.text(), first.bold(), first.italic(), first.underline(),
					first.fontSize(), first.colorHex()));
		}
		addRunsParagraph(document, runs, 0, italic ? 0 : LINE_SPACING_AFTER, listItem ? BULLET_INDENT : 0);
	}

	private List<Run> keyValueRuns(KeyValueLine line) {
		if (!StringUtils.hasText(line.label())) {
			return List.of(body(line.value() == null ? "" : line.value(), false));
		}
		return List.of(body(line.label() + ": ", true), body(line.value(), false));
	}

	private static final String RESULTS_LABEL = "Resultados:";

	private void renderExperience(XWPFDocument document, ResumeEntry entry, String accent) {
		addHeadingParagraph(document, ExperienceEntryLayout.title(entry), null, accent);
		if (!ExperienceEntryLayout.location(entry).isEmpty()) {
			XWPFParagraph headline = document.getParagraphs().getLast();
			addRun(headline, new Run(" | " + ExperienceEntryLayout.location(entry), false, false, false, BODY_FONT_SIZE, accent));
		}
		if (StringUtils.hasText(ExperienceEntryLayout.period(entry))) {
			addRunsParagraph(document, List.of(body(ExperienceEntryLayout.period(entry), false)), 0, 0, 0);
		}
		addBullets(document, ExperienceEntryLayout.bulletLines(entry));
		if (StringUtils.hasText(entry.technologies())) {
			addRunsParagraph(document,
					List.of(new Run("Tecnologias: ", true, false, false, TECHNOLOGIES_FONT_SIZE, TEXT_HEX),
							new Run(entry.technologies(), false, false, false, TECHNOLOGIES_FONT_SIZE, TEXT_HEX)),
					0, LINE_SPACING_AFTER, 0);
		}
	}

	private void renderEntry(XWPFDocument document, ResumeEntry entry, String accent,
			ResumeSectionSemanticType type) {
		if (type == ResumeSectionSemanticType.EXPERIENCE) {
			renderExperience(document, entry, accent);
			return;
		}
		if (type == ResumeSectionSemanticType.EDUCATION) {
			addHeadingParagraph(document, EducationEntryLayout.title(entry), null, accent);
			if (!EducationEntryLayout.period(entry).isEmpty()) {
				XWPFParagraph headline = document.getParagraphs().getLast();
				addRun(headline, new Run(EducationEntryLayout.PERIOD_SEPARATOR + EducationEntryLayout.period(entry), false, false,
						false, BODY_FONT_SIZE, accent));
			}
		} else {
			String heading = StringUtils.hasText(entry.period()) ? entry.heading() + " | " + entry.period() : entry.heading();
			addHeadingParagraph(document, heading, null, accent);
			if (StringUtils.hasText(entry.subheading())) {
				addRunsParagraph(document, List.of(body(entry.subheading(), true)), 0, 0, 0);
			}
		}
		if (StringUtils.hasText(entry.context())) {
			for (RichText.Line line : RichText.fromMarkup(entry.context())) {
				addLineParagraph(document, line, true);
			}
		}
		addBullets(document, entry.bullets());
		if (!entry.results().isEmpty()) {
			addRunsParagraph(document, List.of(body(RESULTS_LABEL, true)), 0, 0, 0);
			addBullets(document, entry.results());
		}
		if (StringUtils.hasText(entry.technologies())) {
			addRunsParagraph(document,
					List.of(new Run("Tecnologias: ", true, false, false, TECHNOLOGIES_FONT_SIZE, TEXT_HEX),
							new Run(entry.technologies(), false, false, false, TECHNOLOGIES_FONT_SIZE, TEXT_HEX)),
					0, LINE_SPACING_AFTER, 0);
		}
	}

	private void addBullets(XWPFDocument document, List<List<TextSpan>> bullets) {
		for (List<TextSpan> bullet : bullets) {
			List<Run> runs = toRuns(bullet);
			if (!StringUtils.hasText(TextSpan.plainText(bullet))) {
				addRunsParagraph(document, List.of(body("", false)), 0, LINE_SPACING_AFTER, 0);
				continue;
			}
			Run first = runs.get(0);
			runs.set(0, new Run("•  " + first.text(), first.bold(), first.italic(), first.underline(), first.fontSize(),
					first.colorHex()));
			addRunsParagraph(document, runs, 0, LINE_SPACING_AFTER, BULLET_INDENT);
		}
	}

	private void addContactParagraph(XWPFDocument document, List<ContactField> contact, ResumeTheme theme) {
		if (contact.isEmpty()) {
			return;
		}
		XWPFParagraph paragraph = document.createParagraph();
		paragraph.setAlignment(ParagraphAlignment.LEFT);
		paragraph.setSpacingAfter(SECTION_TITLE_SPACING_AFTER);
		for (int i = 0; i < contact.size(); i++) {
			ContactField field = contact.get(i);
			if (ContactField.isLink(field.text())) {
				XWPFHyperlinkRun link = paragraph.createHyperlinkRun(ContactField.uri(field.text()));
				link.setText(field.text());
				link.setFontSize(BODY_FONT_SIZE);
				link.setFontFamily(FONT_FAMILY);
				link.setColor(theme.accentHex());
				link.setUnderline(UnderlinePatterns.SINGLE);
			} else {
				addRun(paragraph, body(field.text(), false));
			}
			if (i < contact.size() - 1) {
				addRun(paragraph, body(theme.contactSeparator(), false));
			}
		}
	}

	private void addSectionTitle(XWPFDocument document, String title, String accent) {
		XWPFParagraph paragraph = document.createParagraph();
		paragraph.setAlignment(ParagraphAlignment.LEFT);
		paragraph.setSpacingBefore(SECTION_SPACING_BEFORE);
		paragraph.setSpacingAfter(SECTION_TITLE_SPACING_AFTER);
		addBottomBorder(paragraph, accent);
		addRun(paragraph, new Run(title, true, false, false, SECTION_TITLE_FONT_SIZE, accent));
	}

	private void addHeadingParagraph(XWPFDocument document, String heading, String period, String accent) {
		XWPFParagraph paragraph = document.createParagraph();
		paragraph.setAlignment(ParagraphAlignment.LEFT);
		paragraph.setSpacingBefore(ENTRY_SPACING_BEFORE);
		addRun(paragraph, new Run(heading, true, false, false, BODY_FONT_SIZE, accent));
		if (StringUtils.hasText(period)) {
			addRightTabStop(paragraph);
			paragraph.createRun().addTab();
			addRun(paragraph, body(period, false));
		}
	}

	private void addRunsParagraph(
			XWPFDocument document, List<Run> runs, int spacingBefore, int spacingAfter, int hangingIndent) {
		XWPFParagraph paragraph = document.createParagraph();
		paragraph.setAlignment(ParagraphAlignment.LEFT);
		paragraph.setSpacingBefore(spacingBefore);
		paragraph.setSpacingAfter(spacingAfter);
		if (hangingIndent > 0) {
			paragraph.setIndentationLeft(hangingIndent);
			paragraph.setIndentationHanging(hangingIndent);
		}
		runs.forEach(run -> addRun(paragraph, run));
	}

	private void addRun(XWPFParagraph paragraph, Run run) {
		XWPFRun xwpfRun = paragraph.createRun();
		xwpfRun.setText(run.text());
		xwpfRun.setBold(run.bold());
		xwpfRun.setItalic(run.italic());
		xwpfRun.setUnderline(run.underline() ? UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);
		xwpfRun.setFontSize(run.fontSize());
		xwpfRun.setFontFamily(FONT_FAMILY);
		xwpfRun.setColor(run.colorHex());
	}

	private void addBottomBorder(XWPFParagraph paragraph, String colorHex) {
		CTPPr paragraphProperties = paragraphProperties(paragraph);
		CTPBdr border = paragraphProperties.isSetPBdr() ? paragraphProperties.getPBdr() : paragraphProperties.addNewPBdr();
		CTBorder bottom = border.isSetBottom() ? border.getBottom() : border.addNewBottom();
		bottom.setVal(STBorder.SINGLE);
		bottom.setSz(BigInteger.valueOf(6));
		bottom.setSpace(BigInteger.valueOf(4));
		bottom.setColor(colorHex);
	}

	private void addRightTabStop(XWPFParagraph paragraph) {
		CTPPr paragraphProperties = paragraphProperties(paragraph);
		CTTabs tabs = paragraphProperties.isSetTabs() ? paragraphProperties.getTabs() : paragraphProperties.addNewTabs();
		CTTabStop tab = tabs.addNewTab();
		tab.setVal(STTabJc.RIGHT);
		tab.setPos(BigInteger.valueOf(CONTENT_WIDTH_TWIPS));
	}

	private CTPPr paragraphProperties(XWPFParagraph paragraph) {
		return paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
	}

	private List<Run> toRuns(List<TextSpan> spans) {
		return toRuns(spans, false);
	}

	private List<Run> toRuns(List<TextSpan> spans, boolean forceItalic) {
		List<Run> runs = new ArrayList<>();
		spans.forEach(span -> runs.add(new Run(span.text(), span.bold(), forceItalic || span.italic(), span.underline(),
				BODY_FONT_SIZE, TEXT_HEX)));
		return runs;
	}

	private Run body(String text, boolean bold) {
		return new Run(text, bold, false, false, BODY_FONT_SIZE, TEXT_HEX);
	}

	private record Run(String text, boolean bold, boolean italic, boolean underline, int fontSize, String colorHex) {
	}
}
