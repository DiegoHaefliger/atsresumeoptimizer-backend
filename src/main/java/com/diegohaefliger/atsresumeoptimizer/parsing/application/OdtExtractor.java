package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class OdtExtractor implements DocumentExtractor {

	@Override
	public NormalizedDocument extract(InputStream input) {
		try {
			Document document = OdtXml.document(input, OdtXml.CONTENT_ENTRY)
					.orElseThrow(() -> new IllegalStateException("ODT sem content.xml"));
			return new NormalizedDocument(SourceFormat.ODT, extractRawText(document), extractStructuredText(document), null);
		} catch (Exception exception) {
			throw new DocumentExtractionException(SourceFormat.ODT, exception);
		}
	}

	private String extractRawText(Document document) throws Exception {
		XPath xpath = OdtXml.newXPath();
		NodeList paragraphs = (NodeList) xpath.evaluate("//text:p | //text:h", document, XPathConstants.NODESET);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < paragraphs.getLength(); i++) {
			builder.append(paragraphs.item(i).getTextContent()).append(System.lineSeparator());
		}
		return builder.toString();
	}

	private String extractStructuredText(Document document) throws Exception {
		XPath xpath = OdtXml.newXPath();
		Node body = (Node) xpath.evaluate("//office:text", document, XPathConstants.NODE);
		if (body == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		appendChildren(builder, body, xpath);
		return builder.toString();
	}

	private void appendChildren(StringBuilder builder, Node parent, XPath xpath) throws Exception {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (!(children.item(i) instanceof Element element)) {
				continue;
			}
			String localName = element.getLocalName();
			if ("p".equals(localName) || "h".equals(localName)) {
				builder.append(element.getTextContent()).append(System.lineSeparator());
			} else if ("table".equals(localName)) {
				appendTable(builder, element, xpath);
			} else {
				appendChildren(builder, element, xpath);
			}
		}
	}

	private void appendTable(StringBuilder builder, Element table, XPath xpath) throws Exception {
		NodeList rows = (NodeList) xpath.evaluate(".//table:table-row", table, XPathConstants.NODESET);
		for (int r = 0; r < rows.getLength(); r++) {
			NodeList cells = (NodeList) xpath.evaluate(".//table:table-cell", rows.item(r), XPathConstants.NODESET);
			List<String> cellTexts = new ArrayList<>();
			for (int c = 0; c < cells.getLength(); c++) {
				cellTexts.add(cells.item(c).getTextContent());
			}
			builder.append(String.join(" | ", cellTexts)).append(System.lineSeparator());
		}
	}
}
