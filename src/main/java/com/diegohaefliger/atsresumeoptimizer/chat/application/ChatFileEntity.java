package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "chat_message_file")
class ChatFileEntity {

	@Id
	private UUID id;

	private UUID messageId;

	@Enumerated(EnumType.STRING)
	private ChatFileKind kind;

	private String fileName;

	private String mimeType;

	private long sizeBytes;

	private byte[] content;

	private String extractedText;

	protected ChatFileEntity() {
	}

	ChatFileEntity(UUID messageId, PreparedChatFile file) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.messageId = messageId;
		this.kind = file.kind();
		this.fileName = file.fileName();
		this.mimeType = file.mimeType();
		this.sizeBytes = file.sizeBytes();
		this.content = file.content();
		this.extractedText = file.text();
	}

	public UUID getId() {
		return id;
	}

	public UUID getMessageId() {
		return messageId;
	}

	public ChatFileKind getKind() {
		return kind;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public byte[] getContent() {
		return content;
	}

	public String getExtractedText() {
		return extractedText;
	}
}
