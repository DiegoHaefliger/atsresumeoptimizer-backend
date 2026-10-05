package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;

final class OdtXml {

	static final String CONTENT_ENTRY = "content.xml";
	static final String STYLES_ENTRY = "styles.xml";
	static final String OFFICE_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
	static final String TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
	static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
	static final String DRAW_NS = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0";
	static final String STYLE_NS = "urn:oasis:names:tc:opendocument:xmlns:style:1.0";
	static final String FO_NS = "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0";

	private OdtXml() {
	}

	static Optional<Document> document(InputStream input, String entryName) throws Exception {
		try (ZipInputStream zip = new ZipInputStream(input)) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (entryName.equals(entry.getName())) {
					return Optional.of(parse(zip.readAllBytes()));
				}
			}
		}
		return Optional.empty();
	}

	private static Document parse(byte[] xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new ByteArrayInputStream(xml));
	}

	static XPath newXPath() {
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {
			@Override
			public String getNamespaceURI(String prefix) {
				return switch (prefix) {
					case "office" -> OFFICE_NS;
					case "text" -> TEXT_NS;
					case "table" -> TABLE_NS;
					case "draw" -> DRAW_NS;
					case "style" -> STYLE_NS;
					case "fo" -> FO_NS;
					default -> XMLConstants.NULL_NS_URI;
				};
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return null;
			}

			@Override
			public Iterator<String> getPrefixes(String namespaceURI) {
				return null;
			}
		});
		return xpath;
	}
}
