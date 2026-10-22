package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ChatImage;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatTurn;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ChatHistoryBuilder {

	static final int CHAR_BUDGET = 48_000;
	static final int MAX_IMAGES = 4;

	private ChatHistoryBuilder() {
	}

	static boolean hasFiles(ChatMessage message) {
		return message.role() == ChatRole.USER && !message.files().isEmpty();
	}

	/** Do fim para o começo: a pergunta mais nova sempre entra; texto passa do teto e imagens passam de 4, as velhas saem. */
	static List<ChatTurn> build(List<ChatMessage> history, Map<UUID, List<ChatFileEntity>> filesByMessage) {
		Deque<ChatTurn> kept = new ArrayDeque<>();
		int usedChars = 0;
		int usedImages = 0;
		for (int index = history.size() - 1; index >= 0; index--) {
			ChatMessage message = history.get(index);
			StringBuilder text = new StringBuilder(message.content());
			List<ChatImage> images = new ArrayList<>();
			for (ChatFileEntity file : filesByMessage.getOrDefault(message.id(), List.of())) {
				if (file.getKind() == ChatFileKind.DOCUMENT) {
					text.append("\n\n[Documento anexado: %s]\n%s".formatted(file.getFileName(), file.getExtractedText()));
				} else if (usedImages < MAX_IMAGES) {
					images.add(new ChatImage(file.getMimeType(), Base64.getEncoder().encodeToString(file.getContent())));
					usedImages++;
				} else {
					text.append("\n\n[Imagem anexada: %s, que já não está no contexto]".formatted(file.getFileName()));
				}
			}
			usedChars += text.length();
			if (usedChars > CHAR_BUDGET && !kept.isEmpty()) {
				break;
			}
			kept.addFirst(new ChatTurn(
					message.role() == ChatRole.USER ? ChatTurn.Speaker.USER : ChatTurn.Speaker.ASSISTANT, text.toString(), images));
		}
		return List.copyOf(kept);
	}
}
