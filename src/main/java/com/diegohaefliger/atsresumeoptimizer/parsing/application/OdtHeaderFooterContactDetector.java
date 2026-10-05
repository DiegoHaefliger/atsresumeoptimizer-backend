package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.InputStream;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class OdtHeaderFooterContactDetector implements HeaderFooterContactDetector {

	@Override
	public boolean hasContactInHeaderOrFooter(InputStream input) {
		try {
			Optional<Document> parsed = OdtXml.document(input, OdtXml.STYLES_ENTRY);
			if (parsed.isEmpty()) {
				return false;
			}
			Document document = parsed.get();
			XPath xpath = OdtXml.newXPath();
			NodeList nodes = (NodeList) xpath.evaluate("//style:header | //style:footer", document, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				if (ContactPatterns.containsContactInfo(nodes.item(i).getTextContent())) {
					return true;
				}
			}
			return false;
		} catch (Exception exception) {
			throw new DocumentExtractionException(SourceFormat.ODT, exception);
		}
	}
}
