package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatAttachment;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversation;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversationDetail;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface ChatEntityMapper {

	ChatConversation toDomain(ChatConversationEntity entity);

	List<ChatConversation> toDomain(List<ChatConversationEntity> entities);

	ChatAttachment toDomain(ChatAttachmentEntity entity);

	List<ChatAttachment> toAttachments(List<ChatAttachmentEntity> entities);

	List<ChatMessage> toMessages(List<ChatMessageEntity> entities);

	ChatMessage toDomain(ChatMessageEntity entity);

	default ChatConversationDetail toDetail(
			ChatConversationEntity conversation, List<ChatAttachmentEntity> attachments, List<ChatMessageEntity> messages) {
		return new ChatConversationDetail(conversation.getId(), conversation.getTitle(), conversation.getCreatedAt(),
				conversation.getUpdatedAt(), toAttachments(attachments), toMessages(messages));
	}
}
