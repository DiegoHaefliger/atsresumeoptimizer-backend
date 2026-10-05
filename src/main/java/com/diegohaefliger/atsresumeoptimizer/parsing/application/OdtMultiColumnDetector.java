package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.InputStream;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class OdtMultiColumnDetector implements MultiColumnDetector {

	@Override
	public boolean isMultiColumn(InputStream input) {
		try {
			Optional<Document> parsed = OdtXml.document(input, OdtXml.STYLES_ENTRY);
			if (parsed.isEmpty()) {
				return false;
			}
			Document document = parsed.get();
			XPath xpath = OdtXml.newXPath();
			NodeList columns = (NodeList) xpath.evaluate("//style:columns", document, XPathConstants.NODESET);
			for (int i = 0; i < columns.getLength(); i++) {
				String count = ((Element) columns.item(i)).getAttributeNS(OdtXml.FO_NS, "column-count");
				if (!count.isBlank() && Integer.parseInt(count) > 1) {
					return true;
				}
			}
			return false;
		} catch (Exception exception) {
			throw new DocumentExtractionException(SourceFormat.ODT, exception);
		}
	}
}
