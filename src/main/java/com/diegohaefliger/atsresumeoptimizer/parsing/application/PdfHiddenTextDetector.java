package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/** Assume fundo branco, o caso comum de currículo: texto branco ou invisível é keyword stuffing. */
class PdfHiddenTextDetector implements HiddenTextDetector {

	private static final float WHITE_COMPONENT_THRESHOLD = 0.95f;

	@Override
	public boolean hasHiddenText(InputStream input) {
		try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
			AtomicBoolean found = new AtomicBoolean(false);
			PDFTextStripper stripper = new PDFTextStripper() {
				{
					// PDFTextStripper não registra operadores de cor; sem eles o preenchimento fica preto pra sempre.
					addOperator(new SetNonStrokingColorSpace(this));
					addOperator(new SetNonStrokingColor(this));
					addOperator(new SetNonStrokingColorN(this));
					addOperator(new SetNonStrokingDeviceRGBColor(this));
					addOperator(new SetNonStrokingDeviceGrayColor(this));
					addOperator(new SetNonStrokingDeviceCMYKColor(this));
				}

				@Override
				protected void processTextPosition(TextPosition text) {
					if (text.getUnicode() != null && !text.getUnicode().isBlank() && isHidden()) {
						found.set(true);
					}
					super.processTextPosition(text);
				}

				private boolean isHidden() {
					return getGraphicsState().getTextState().getRenderingMode() == RenderingMode.NEITHER || isWhiteFill();
				}

				private boolean isWhiteFill() {
					float[] components = getGraphicsState().getNonStrokingColor().getComponents();
					if (components.length == 0) {
						return false;
					}
					for (float component : components) {
						if (component < WHITE_COMPONENT_THRESHOLD) {
							return false;
						}
					}
					return true;
				}
			};
			stripper.getText(document);
			return found.get();
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.PDF, exception);
		}
	}
}
