package com.diegohaefliger.atsresumeoptimizer.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.InvalidChatFileException;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.DocumentTextReader;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.UnsupportedDocumentTypeException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ChatFileIntakeTest {

	private static final byte[] PNG = Base64.getDecoder()
			.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");

	private final DocumentTextReader reader = mock(DocumentTextReader.class);
	private final ChatFileIntake intake = new ChatFileIntake(reader);

	private static ChatUpload text(String name, String content) {
		return new ChatUpload(name, content.getBytes(StandardCharsets.UTF_8));
	}

	private void notAnOfficeFile() {
		when(reader.read(any())).thenThrow(new UnsupportedDocumentTypeException("text/plain"));
	}

	@Test
	void keepsAnImageWithItsRealTypeNoMatterTheName() {
		PreparedChatFile file = intake.prepare(List.of(new ChatUpload("foto.txt", PNG))).getFirst();

		assertThat(file.kind()).isEqualTo(ChatFileKind.IMAGE);
		assertThat(file.mimeType()).isEqualTo("image/png");
		assertThat(file.content()).isEqualTo(PNG);
		assertThat(file.text()).isNull();
	}

	@Test
	void extractsTheTextOfAnOfficeDocument() {
		when(reader.read(any())).thenReturn("  Experiência com Java  ");

		PreparedChatFile file = intake.prepare(List.of(new ChatUpload("cv.pdf", new byte[] {1, 2, 3}))).getFirst();

		assertThat(file.kind()).isEqualTo(ChatFileKind.DOCUMENT);
		assertThat(file.text()).isEqualTo("Experiência com Java");
		assertThat(file.content()).isNull();
	}

	@Test
	void cutsAHugeDocument() {
		when(reader.read(any())).thenReturn("a".repeat(ChatFileIntake.MAX_DOCUMENT_CHARS + 500));

		String result = intake.prepare(List.of(new ChatUpload("grande.docx", new byte[] {1}))).getFirst().text();

		assertThat(result).startsWith("aaa").endsWith("[...texto cortado]");
		assertThat(result.length()).isLessThan(ChatFileIntake.MAX_DOCUMENT_CHARS + 50);
	}

	@Test
	void readsPlainTextAndMarkdown() {
		notAnOfficeFile();

		assertThat(intake.prepare(List.of(text("notas.md", "# Título\ncorpo"))).getFirst().text()).isEqualTo("# Título\ncorpo");
		assertThat(intake.prepare(List.of(text("dados.txt", "linha"))).getFirst().kind()).isEqualTo(ChatFileKind.DOCUMENT);
	}

	@Test
	void refusesTextThatIsNotUtf8() {
		notAnOfficeFile();

		assertThatThrownBy(() -> intake.prepare(List.of(new ChatUpload("velho.txt", new byte[] {(byte) 0xE9, 0x61, 0x62}))))
				.isInstanceOf(InvalidChatFileException.class)
				.hasMessageContaining("UTF-8");
	}

	@Test
	void refusesAnUnknownBinaryFile() {
		notAnOfficeFile();

		assertThatThrownBy(() -> intake.prepare(List.of(new ChatUpload("programa.exe", new byte[] {0x4D, 0x5A, 0, 1, 2}))))
				.isInstanceOfSatisfying(InvalidChatFileException.class,
						failure -> assertThat(failure.status()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
	}

	@Test
	void refusesPlainTextWithAnUnexpectedExtension() {
		notAnOfficeFile();

		assertThatThrownBy(() -> intake.prepare(List.of(text("script.sh", "echo oi")))).isInstanceOf(InvalidChatFileException.class);
	}

	@Test
	void refusesAScannedDocumentWithoutText() {
		when(reader.read(any())).thenReturn("  \n ");

		assertThatThrownBy(() -> intake.prepare(List.of(new ChatUpload("scan.pdf", new byte[] {1}))))
				.isInstanceOf(InvalidChatFileException.class)
				.hasMessageContaining("escaneado");
	}

	@Test
	void refusesADocumentThatCannotBeRead() {
		when(reader.read(any())).thenThrow(new DocumentExtractionException(SourceFormat.PDF, new RuntimeException("corrompido")));

		assertThatThrownBy(() -> intake.prepare(List.of(new ChatUpload("quebrado.pdf", new byte[] {1}))))
				.isInstanceOf(InvalidChatFileException.class)
				.hasMessageContaining("quebrado.pdf");
	}

	@Test
	void refusesEmptyAndOversizedFiles() {
		assertThatThrownBy(() -> intake.prepare(List.of(new ChatUpload("vazio.png", new byte[0]))))
				.isInstanceOf(InvalidChatFileException.class);
		assertThatThrownBy(() -> intake.prepare(List.of(new ChatUpload("grande.png", new byte[(int) ChatFileIntake.MAX_FILE_BYTES + 1]))))
				.isInstanceOfSatisfying(InvalidChatFileException.class,
						failure -> assertThat(failure.status()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE));
	}

	@Test
	void refusesTooManyFiles() {
		List<ChatUpload> many = java.util.Collections.nCopies(ChatFileIntake.MAX_FILES + 1, new ChatUpload("a.png", PNG));

		assertThatThrownBy(() -> intake.prepare(many)).isInstanceOf(InvalidChatFileException.class).hasMessageContaining("no máximo");
	}

	@Test
	void keepsOnlyTheBaseNameOfTheUploadedPath() {
		assertThat(intake.prepare(List.of(new ChatUpload("C:\\Users\\eu\\foto.png", PNG))).getFirst().fileName()).isEqualTo("foto.png");
		assertThat(intake.prepare(List.of(new ChatUpload("../../etc/foto.png", PNG))).getFirst().fileName()).isEqualTo("foto.png");
	}
}
