package com.diegohaefliger.atsresumeoptimizer.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ChatTurn;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatHistoryBuilderTest {

	private static final byte[] BYTES = {1, 2, 3};

	private final Map<UUID, List<ChatFileEntity>> files = new HashMap<>();

	private ChatMessage message(ChatRole role, String content) {
		return new ChatMessage(UUID.randomUUID(), role, content, null, 0, 0, BigDecimal.ZERO, false, Instant.now(), List.of());
	}

	private void attach(ChatMessage message, PreparedChatFile file) {
		files.computeIfAbsent(message.id(), id -> new ArrayList<>()).add(new ChatFileEntity(message.id(), file));
	}

	private static PreparedChatFile image(String name) {
		return new PreparedChatFile(ChatFileKind.IMAGE, name, "image/png", BYTES.length, BYTES, null);
	}

	private static PreparedChatFile document(String name, String text) {
		return new PreparedChatFile(ChatFileKind.DOCUMENT, name, "application/pdf", 10, null, text);
	}

	@Test
	void appendsTheDocumentTextToTheQuestion() {
		ChatMessage question = message(ChatRole.USER, "o que acha?");
		attach(question, document("cv.pdf", "Java e Spring"));

		List<ChatTurn> turns = ChatHistoryBuilder.build(List.of(question), files);

		assertThat(turns.getFirst().text()).startsWith("o que acha?").contains("[Documento anexado: cv.pdf]").endsWith("Java e Spring");
	}

	@Test
	void sendsImagesAsBase64() {
		ChatMessage question = message(ChatRole.USER, "veja");
		attach(question, image("tela.png"));

		ChatTurn turn = ChatHistoryBuilder.build(List.of(question), files).getFirst();

		assertThat(turn.images()).singleElement().satisfies(image -> {
			assertThat(image.mimeType()).isEqualTo("image/png");
			assertThat(image.base64Data()).isEqualTo(Base64.getEncoder().encodeToString(BYTES));
		});
	}

	@Test
	void keepsOnlyTheNewestImagesAndNotesTheOthers() {
		List<ChatMessage> history = new ArrayList<>();
		for (int index = 0; index < ChatHistoryBuilder.MAX_IMAGES + 1; index++) {
			ChatMessage question = message(ChatRole.USER, "pergunta " + index);
			attach(question, image("img" + index + ".png"));
			history.add(question);
		}

		List<ChatTurn> turns = ChatHistoryBuilder.build(history, files);

		assertThat(turns.getFirst().images()).isEmpty();
		assertThat(turns.getFirst().text()).contains("img0.png").contains("já não está no contexto");
		assertThat(turns.stream().mapToInt(turn -> turn.images().size()).sum()).isEqualTo(ChatHistoryBuilder.MAX_IMAGES);
	}

	@Test
	void countsDocumentTextAgainstTheBudget() {
		ChatMessage old = message(ChatRole.USER, "antiga");
		attach(old, document("grande.pdf", "x".repeat(ChatHistoryBuilder.CHAR_BUDGET)));
		ChatMessage recent = message(ChatRole.USER, "nova");

		List<ChatTurn> turns = ChatHistoryBuilder.build(List.of(old, recent), files);

		assertThat(turns).extracting(ChatTurn::text).containsExactly("nova");
	}

	@Test
	void ignoresFilesOfAssistantMessages() {
		assertThat(ChatHistoryBuilder.hasFiles(message(ChatRole.ASSISTANT, "oi"))).isFalse();
	}
}
