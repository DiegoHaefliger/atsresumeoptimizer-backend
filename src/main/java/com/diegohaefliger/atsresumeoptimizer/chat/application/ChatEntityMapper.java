package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatAttachment;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversation;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversationDetail;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFile;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface ChatEntityMapper {

	ChatConversation toDomain(ChatConversationEntity entity);

	List<ChatConversation> toDomain(List<ChatConversationEntity> entities);

	ChatAttachment toDomain(ChatAttachmentEntity entity);

	List<ChatAttachment> toAttachments(List<ChatAttachmentEntity> entities);

	@Mapping(target = "files", ignore = true)
	ChatMessage toDomain(ChatMessageEntity entity);

	ChatFile toDomain(ChatFileEntity entity);

	List<ChatFile> toFiles(List<ChatFileEntity> entities);

	default ChatConversationDetail toDetail(
			ChatConversationEntity conversation, List<ChatAttachmentEntity> attachments, List<ChatMessage> messages) {
		return new ChatConversationDetail(conversation.getId(), conversation.getTitle(), conversation.getCreatedAt(),
				conversation.getUpdatedAt(), toAttachments(attachments), messages);
	}
}
