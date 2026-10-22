package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatAttachment;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversation;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversationDetail;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFile;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface ChatWebMapper {

	ConversationSummaryResponse toResponse(ChatConversation conversation);

	List<ConversationSummaryResponse> toResponse(List<ChatConversation> conversations);

	ConversationDetailResponse toResponse(ChatConversationDetail detail);

	AttachmentResponse toResponse(ChatAttachment attachment);

	MessageResponse toResponse(ChatMessage message);

	FileResponse toResponse(ChatFile file);
}
