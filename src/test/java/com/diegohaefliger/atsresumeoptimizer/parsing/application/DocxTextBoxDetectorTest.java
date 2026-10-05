package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxTextBoxDetectorTest {

	private static final String CONTENT_TYPES_XML = """
		<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
		<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
			<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
			<Default Extension="xml" ContentType="application/xml"/>
			<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
		</Types>
		""";

	private static final String PACKAGE_RELS_XML = """
		<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
		<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
			<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
		</Relationships>
		""";

	private static final String DOCUMENT_WITH_TEXTBOX_XML = """
		<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
		<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
					xmlns:v="urn:schemas-microsoft-com:vml">
			<w:body>
				<w:p>
					<w:r>
						<w:pict>
							<v:shape>
								<v:textbox>
									<w:txbxContent>
										<w:p><w:r><w:t>Habilidades</w:t></w:r></w:p>
									</w:txbxContent>
								</v:textbox>
							</v:shape>
						</w:pict>
					</w:r>
				</w:p>
			</w:body>
		</w:document>
		""";

	private final DocxTextBoxDetector detector = new DocxTextBoxDetector();

	@Test
	void plainParagraphsAreNotFlagged() throws IOException {
		InputStream docx = plainDocx();

		assertThat(detector.hasTextBox(docx)).isFalse();
	}

	@Test
	void textBoxContentIsFlagged() throws IOException {
		InputStream docx = docxPackage(DOCUMENT_WITH_TEXTBOX_XML);

		assertThat(detector.hasTextBox(docx)).isTrue();
	}

	private InputStream plainDocx() throws IOException {
		try (XWPFDocument document = new XWPFDocument()) {
			document.createParagraph().createRun().setText("Ana Silva");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}

	private InputStream docxPackage(String documentXml) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(out)) {
			writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML);
			writeEntry(zip, "_rels/.rels", PACKAGE_RELS_XML);
			writeEntry(zip, "word/document.xml", documentXml);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	private void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}
}
