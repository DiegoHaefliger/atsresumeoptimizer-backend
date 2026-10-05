package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.util.Matrix;

final class PdfFlowWriter implements AutoCloseable {

	private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
	private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
	private static final float LINE_HEIGHT_FACTOR = 1.35f;
	private static final String HEADING_TRAILING_SEPARATOR = " | ";
	private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\\u200B\\u200C\\u200D\\uFEFF\\u2060]");
	private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n\\t\\f]+");

	private final PDDocument document;
	private final float margin;
	private final PDType1Font regular;
	private final PDType1Font bold;
	private final PDType1Font italic;
	private final PDType1Font boldItalic;
	private PDPageContentStream stream;
	private float y;
	private float lastBaseline;

	PdfFlowWriter(PDDocument document, float margin) throws IOException {
		this.document = document;
		this.margin = margin;
		this.regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
		this.bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
		this.italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
		this.boldItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
		newPage();
	}

	float maxWidth() {
		return PAGE_WIDTH - 2 * margin;
	}

	void gap(float height) throws IOException {
		y -= height;
		if (y < margin) {
			newPage();
		}
	}

	void writeLine(String text, boolean useBold, boolean useItalic, float fontSize, Color color) throws IOException {
		writeRich(List.of(new TextSpan(sanitize(text), useBold)), fontSize, color, null, 0, useItalic);
	}

	void writeRich(List<TextSpan> spans, float fontSize, Color color, String bulletPrefix, float hangingIndent)
			throws IOException {
		writeRich(spans, fontSize, color, bulletPrefix, hangingIndent, false);
	}

	/** Período colado ao heading, não alinhado à direita: um gap grande vira "segunda coluna" no PdfMultiColumnDetector. */
	void writeHeadingWithTrailing(String heading, String trailing, float fontSize, Color headingColor, Color trailingColor)
			throws IOException {
		String headingText = sanitize(heading);
		boolean hasTrailing = trailing != null && !trailing.isBlank();
		String trailingText = hasTrailing ? HEADING_TRAILING_SEPARATOR + sanitize(trailing) : "";
		float totalWidth = widthOf(bold, fontSize, headingText) + widthOf(regular, fontSize, trailingText);
		if (totalWidth > maxWidth()) {
			writeLine(headingText, true, false, fontSize, headingColor);
			if (hasTrailing) {
				writeLine(sanitize(trailing), false, false, fontSize, trailingColor);
			}
			return;
		}
		if (y < margin) {
			newPage();
		}
		lastBaseline = y;
		beginTextBlock();
		drawText(headingText, bold, fontSize, margin, y, headingColor);
		if (hasTrailing) {
			float x = margin + widthOf(bold, fontSize, headingText);
			drawText(HEADING_TRAILING_SEPARATOR, regular, fontSize, x, y, headingColor);
			x += widthOf(regular, fontSize, HEADING_TRAILING_SEPARATOR);
			drawText(sanitize(trailing), regular, fontSize, x, y, trailingColor);
		}
		endTextBlock();
		y -= fontSize * LINE_HEIGHT_FACTOR;
	}

	void rule(Color color) throws IOException {
		float ruleY = lastBaseline - 4f;
		stream.setStrokingColor(color);
		stream.setLineWidth(1f);
		stream.moveTo(margin, ruleY);
		stream.lineTo(PAGE_WIDTH - margin, ruleY);
		stream.stroke();
	}

	void writeContactFields(List<ContactField> fields, float fontSize, Color textColor, Color accentColor, String separator) throws IOException {
		if (fields.isEmpty()) {
			return;
		}
		if (y < margin) {
			newPage();
		}
		float separatorWidth = widthOf(regular, fontSize, separator);
		float rightEdge = PAGE_WIDTH - margin;
		lastBaseline = y;
		float x = margin;
		for (int i = 0; i < fields.size(); i++) {
			ContactField field = fields.get(i);
			String text = sanitize(field.text());
			boolean isLink = ContactField.isLink(text);
			Color tokenColor = isLink ? accentColor : textColor;
			float width = widthOf(regular, fontSize, text);
			if (x > margin && x + width > rightEdge) {
				y -= fontSize * LINE_HEIGHT_FACTOR;
				if (y < margin) {
					newPage();
				}
				lastBaseline = y;
				x = margin;
			}
			beginTextBlock();
			drawText(text, regular, fontSize, x, y, tokenColor);
			endTextBlock();
			if (isLink) {
				stream.setStrokingColor(tokenColor);
				stream.setLineWidth(0.6f);
				stream.moveTo(x, y - 1.5f);
				stream.lineTo(x + width, y - 1.5f);
				stream.stroke();
				addLinkAnnotation(x, y, width, fontSize, ContactField.uri(text));
			}
			x += width;
			if (i < fields.size() - 1) {
				beginTextBlock();
				drawText(separator, regular, fontSize, x, y, textColor);
				endTextBlock();
				x += separatorWidth;
			}
		}
		y -= fontSize * LINE_HEIGHT_FACTOR + 2f;
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

	void writeRich(
			List<TextSpan> spans, float fontSize, Color color, String bulletPrefix, float hangingIndent, boolean italicRun)
			throws IOException {
		List<Word> words = toWords(spans);
		if (words.isEmpty()) {
			if (y < margin) {
				newPage();
			}
			y -= fontSize * LINE_HEIGHT_FACTOR;
			return;
		}
		float bulletWidth = bulletPrefix == null ? 0 : widthOf(regular, fontSize, bulletPrefix);
		float firstLineMax = maxWidth() - hangingIndent - bulletWidth;
		float wrapMax = maxWidth() - hangingIndent;
		List<List<Word>> lines = wrap(words, fontSize, firstLineMax, wrapMax);
		float spaceWidth = widthOf(regular, fontSize, " ");
		for (int i = 0; i < lines.size(); i++) {
			if (y < margin) {
				newPage();
			}
			lastBaseline = y;
			float x = margin + hangingIndent;
			beginTextBlock();
			if (i == 0 && bulletPrefix != null) {
				drawText(bulletPrefix, regular, fontSize, x, y, color);
				x += bulletWidth;
			}
			boolean firstWordOnLine = true;
			List<float[]> underlines = new ArrayList<>();
			Word previousWord = null;
			for (Word word : lines.get(i)) {
				if (!firstWordOnLine) {
					drawText(" ", regular, fontSize, x, y, color);
					x += spaceWidth;
				}
				PDFont font = fontFor(word, italicRun);
				float width = widthOf(font, fontSize, word.text());
				drawText(word.text(), font, fontSize, x, y, color);
				if (word.underline()) {
					boolean continues = previousWord != null && previousWord.underline() && !underlines.isEmpty();
					if (continues) {
						underlines.get(underlines.size() - 1)[1] = x + width;
					} else {
						underlines.add(new float[] {x, x + width});
					}
				}
				x += width;
				previousWord = word;
				firstWordOnLine = false;
			}
			endTextBlock();
			drawUnderlines(underlines, color);
			y -= fontSize * LINE_HEIGHT_FACTOR;
		}
	}

	/** Uma linha visual = um só {@code BT...ET}; um bloco por palavra vira cluster de "colunas" no PdfMultiColumnDetector. */
	private void beginTextBlock() throws IOException {
		stream.beginText();
	}

	private void endTextBlock() throws IOException {
		stream.endText();
	}

	private void drawUnderlines(List<float[]> segments, Color color) throws IOException {
		for (float[] segment : segments) {
			stream.setStrokingColor(color);
			stream.setLineWidth(0.6f);
			stream.moveTo(segment[0], y - 1.5f);
			stream.lineTo(segment[1], y - 1.5f);
			stream.stroke();
		}
	}

	private void drawText(String text, PDFont font, float fontSize, float x, float y, Color color) throws IOException {
		stream.setNonStrokingColor(color);
		stream.setFont(font, fontSize);
		stream.setTextMatrix(Matrix.getTranslateInstance(x, y));
		stream.showText(text);
	}

	private List<Word> toWords(List<TextSpan> spans) {
		List<Word> words = new ArrayList<>();
		boolean previousEndedWithSpace = true;
		for (TextSpan span : spans) {
			String text = sanitize(span.text());
			if (text.isEmpty()) {
				continue;
			}
			boolean leadingSpace = words.isEmpty() || previousEndedWithSpace || Character.isWhitespace(text.charAt(0));
			String[] parts = text.strip().split("\\s+");
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].isEmpty()) {
					continue;
				}
				if (i == 0 && !leadingSpace) {
					Word previous = words.remove(words.size() - 1);
					words.add(new Word(previous.text() + parts[i], previous.bold(), previous.italic(), previous.underline()));
				} else {
					words.add(new Word(parts[i], span.bold(), span.italic(), span.underline()));
				}
			}
			previousEndedWithSpace = Character.isWhitespace(text.charAt(text.length() - 1));
		}
		return words;
	}

	private List<List<Word>> wrap(List<Word> words, float fontSize, float firstLineMax, float restMax) throws IOException {
		List<List<Word>> lines = new ArrayList<>();
		List<Word> current = new ArrayList<>();
		float currentWidth = 0;
		float maxWidth = firstLineMax;
		float spaceWidth = widthOf(regular, fontSize, " ");
		for (Word word : words) {
			float wordWidth = widthOf(fontFor(word, false), fontSize, word.text());
			float projected = current.isEmpty() ? wordWidth : currentWidth + spaceWidth + wordWidth;
			if (!current.isEmpty() && projected > maxWidth) {
				lines.add(current);
				current = new ArrayList<>();
				maxWidth = restMax;
				projected = wordWidth;
			}
			current.add(word);
			currentWidth = projected;
		}
		if (!current.isEmpty()) {
			lines.add(current);
		}
		return lines;
	}

	private float widthOf(PDFont font, float fontSize, String text) throws IOException {
		return font.getStringWidth(text) / 1000 * fontSize;
	}

	private void addLinkAnnotation(float x, float y, float width, float fontSize, String uri) throws IOException {
		PDAnnotationLink link = new PDAnnotationLink();
		link.setRectangle(new PDRectangle(x, y - 2f, width, fontSize + 2f));
		PDBorderStyleDictionary border = new PDBorderStyleDictionary();
		border.setWidth(0);
		link.setBorderStyle(border);
		PDActionURI action = new PDActionURI();
		action.setURI(uri);
		link.setAction(action);
		document.getPage(document.getNumberOfPages() - 1).getAnnotations().add(link);
	}

	private void newPage() throws IOException {
		if (stream != null) {
			stream.close();
		}
		PDPage page = new PDPage(PDRectangle.LETTER);
		document.addPage(page);
		stream = new PDPageContentStream(document, page);
		y = PAGE_HEIGHT - margin;
	}

	private static String sanitize(String text) {
		return text == null ? "" : LINE_BREAKS.matcher(INVISIBLE_CHARS.matcher(text).replaceAll("")).replaceAll(" ");
	}

	private PDFont fontFor(Word word, boolean forceItalic) {
		boolean slanted = forceItalic || word.italic();
		if (word.bold()) {
			return slanted ? boldItalic : bold;
		}
		return slanted ? italic : regular;
	}

	private record Word(String text, boolean bold, boolean italic, boolean underline) {
	}
}
