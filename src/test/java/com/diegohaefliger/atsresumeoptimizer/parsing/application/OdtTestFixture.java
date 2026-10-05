package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Monta um .odt válido mínimo na mão (zip + XML do ODF), sem depender de nenhuma lib de terceiro pra escrever ODT nos testes. */
final class OdtTestFixture {

	private String bodyXml = "<text:p>Ana Silva</text:p>";
	private String headerXml = "";
	private String footerXml = "";
	private int columnCount = 1;

	static OdtTestFixture odt() {
		return new OdtTestFixture();
	}

	OdtTestFixture withParagraphs(String... paragraphs) {
		StringBuilder builder = new StringBuilder();
		for (String paragraph : paragraphs) {
			builder.append("<text:p>").append(escape(paragraph)).append("</text:p>");
		}
		this.bodyXml = builder.toString();
		return this;
	}

	OdtTestFixture withTable(String... cellValues) {
		StringBuilder row = new StringBuilder("<table:table-row>");
		for (String cell : cellValues) {
			row.append("<table:table-cell><text:p>").append(escape(cell)).append("</text:p></table:table-cell>");
		}
		row.append("</table:table-row>");
		this.bodyXml += "<table:table>" + row + "</table:table>";
		return this;
	}

	OdtTestFixture withHeader(String text) {
		this.headerXml = "<style:header><text:p>" + escape(text) + "</text:p></style:header>";
		return this;
	}

	OdtTestFixture withColumns(int count) {
		this.columnCount = count;
		return this;
	}

	OdtTestFixture withTextBox(String text) {
		this.bodyXml += "<draw:frame><draw:text-box><text:p>" + escape(text) + "</text:p></draw:text-box></draw:frame>";
		return this;
	}

	byte[] build() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(out)) {
			writeStored(zip, "mimetype", "application/vnd.oasis.opendocument.text".getBytes(StandardCharsets.UTF_8));
			writeDeflated(zip, "META-INF/manifest.xml", manifestXml());
			writeDeflated(zip, "content.xml", contentXml());
			writeDeflated(zip, "styles.xml", stylesXml());
		}
		return out.toByteArray();
	}

	private void writeStored(ZipOutputStream zip, String name, byte[] data) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setMethod(ZipEntry.STORED);
		entry.setSize(data.length);
		entry.setCompressedSize(data.length);
		CRC32 crc = new CRC32();
		crc.update(data);
		entry.setCrc(crc.getValue());
		zip.putNextEntry(entry);
		zip.write(data);
		zip.closeEntry();
	}

	private void writeDeflated(ZipOutputStream zip, String name, String content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private String contentXml() {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
				    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
				    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
				    xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
				    office:version="1.2">
				  <office:body>
				    <office:text>%s</office:text>
				  </office:body>
				</office:document-content>
				""".formatted(bodyXml);
	}

	private String stylesXml() {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
				    xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
				    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
				    xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
				    office:version="1.2">
				  <office:master-styles>
				    <style:master-page style:name="Standard" style:page-layout-name="PL1">
				      %s
				      %s
				    </style:master-page>
				  </office:master-styles>
				  <office:automatic-styles>
				    <style:page-layout style:name="PL1">
				      <style:page-layout-properties>
				        <style:columns fo:column-count="%d"/>
				      </style:page-layout-properties>
				    </style:page-layout>
				  </office:automatic-styles>
				</office:document-styles>
				""".formatted(headerXml, footerXml, columnCount);
	}

	private String manifestXml() {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
				  <manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.text"/>
				  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
				  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
				</manifest:manifest>
				""";
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
