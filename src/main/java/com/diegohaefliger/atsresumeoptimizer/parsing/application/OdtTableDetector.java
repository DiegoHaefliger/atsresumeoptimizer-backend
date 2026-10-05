package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.InputStream;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class OdtTableDetector implements TableDetector {

	@Override
	public boolean hasTable(InputStream input) {
		try {
			Optional<Document> parsed = OdtXml.document(input, OdtXml.CONTENT_ENTRY);
			if (parsed.isEmpty()) {
				return false;
			}
			Document document = parsed.get();
			XPath xpath = OdtXml.newXPath();
			NodeList tables = (NodeList) xpath.evaluate("//table:table", document, XPathConstants.NODESET);
			for (int i = 0; i < tables.getLength(); i++) {
				if (!tables.item(i).getTextContent().isBlank()) {
					return true;
				}
			}
			return false;
		} catch (Exception exception) {
			throw new DocumentExtractionException(SourceFormat.ODT, exception);
		}
	}
}
