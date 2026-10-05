package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.InputStream;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Texto fora do fluxo principal em ODF sempre mora dentro de {@code draw:frame > draw:text-box}. */
class OdtTextBoxDetector implements TextBoxDetector {

	@Override
	public boolean hasTextBox(InputStream input) {
		try {
			Optional<Document> parsed = OdtXml.document(input, OdtXml.CONTENT_ENTRY);
			if (parsed.isEmpty()) {
				return false;
			}
			Document document = parsed.get();
			XPath xpath = OdtXml.newXPath();
			NodeList textBoxes = (NodeList) xpath.evaluate("//draw:text-box", document, XPathConstants.NODESET);
			return textBoxes.getLength() > 0;
		} catch (Exception exception) {
			throw new DocumentExtractionException(SourceFormat.ODT, exception);
		}
	}
}
