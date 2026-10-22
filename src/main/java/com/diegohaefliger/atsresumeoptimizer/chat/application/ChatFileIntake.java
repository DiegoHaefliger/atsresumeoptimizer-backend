package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.InvalidChatFileException;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.DocumentTextReader;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.UnsupportedDocumentTypeException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
class ChatFileIntake {

	static final int MAX_FILES = 5;
	static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
	static final int MAX_DOCUMENT_CHARS = 40_000;

	private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
	private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "csv");
	private static final String CUT_MARK = "\n[...texto cortado]";
	private static final String PLAIN_TEXT = "text/plain";

	private final DocumentTextReader documentReader;
	private final Tika tika = new Tika();

	ChatFileIntake(DocumentTextReader documentReader) {
		this.documentReader = documentReader;
	}

	List<PreparedChatFile> prepare(List<ChatUpload> uploads) {
		if (uploads.size() > MAX_FILES) {
			throw new InvalidChatFileException("Anexe no máximo %d arquivos por mensagem.".formatted(MAX_FILES),
					HttpStatus.BAD_REQUEST);
		}
		return uploads.stream().map(this::prepare).toList();
	}

	private PreparedChatFile prepare(ChatUpload upload) {
		String name = displayName(upload.fileName());
		byte[] content = upload.content();
		if (content.length == 0) {
			throw new InvalidChatFileException("O arquivo %s está vazio.".formatted(name), HttpStatus.BAD_REQUEST);
		}
		if (content.length > MAX_FILE_BYTES) {
			throw new InvalidChatFileException("O arquivo %s passa de 5 MB.".formatted(name), HttpStatus.CONTENT_TOO_LARGE);
		}
		String mimeType = tika.detect(content);
		if (IMAGE_TYPES.contains(mimeType)) {
			return new PreparedChatFile(ChatFileKind.IMAGE, name, mimeType, content.length, content, null);
		}
		return new PreparedChatFile(ChatFileKind.DOCUMENT, name, mimeType, content.length, null, documentText(name, mimeType, content));
	}

	private String documentText(String name, String mimeType, byte[] content) {
		String text;
		try {
			text = documentReader.read(content);
		} catch (UnsupportedDocumentTypeException notAnOfficeFormat) {
			text = plainText(name, mimeType, content);
		} catch (DocumentExtractionException unreadable) {
			throw new InvalidChatFileException("Não consegui ler o arquivo %s.".formatted(name), HttpStatus.UNPROCESSABLE_ENTITY);
		}
		if (text.isBlank()) {
			throw new InvalidChatFileException(
					"O arquivo %s não tem texto para ler (talvez seja um PDF escaneado).".formatted(name),
					HttpStatus.UNPROCESSABLE_ENTITY);
		}
		String clean = text.strip();
		return clean.length() <= MAX_DOCUMENT_CHARS ? clean : clean.substring(0, MAX_DOCUMENT_CHARS) + CUT_MARK;
	}

	private String plainText(String name, String mimeType, byte[] content) {
		if (!PLAIN_TEXT.equals(mimeType) || !TEXT_EXTENSIONS.contains(extension(name))) {
			throw new InvalidChatFileException(
					"O arquivo %s não é suportado. Envie imagem (PNG, JPEG, WebP, GIF) ou documento (PDF, DOCX, ODT, TXT, MD)."
							.formatted(name),
					HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		}
		try {
			return StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(content))
					.toString();
		} catch (CharacterCodingException notUtf8) {
			throw new InvalidChatFileException("O arquivo %s precisa estar em UTF-8.".formatted(name), HttpStatus.UNPROCESSABLE_ENTITY);
		}
	}

	private static String displayName(String fileName) {
		String raw = fileName == null || fileName.isBlank() ? "arquivo" : fileName;
		String base = raw.substring(Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\')) + 1).strip();
		return base.length() <= 200 ? base : base.substring(0, 200);
	}

	private static String extension(String name) {
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
