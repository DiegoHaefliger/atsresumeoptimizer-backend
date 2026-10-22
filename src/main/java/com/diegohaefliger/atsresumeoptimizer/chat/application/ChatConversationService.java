package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatAttachment;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversation;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversationDetail;
import java.util.List;
import java.util.UUID;

public interface ChatConversationService {

	List<ChatConversation> list();

	ChatConversation create(String title);

	ChatConversationDetail get(UUID conversationId);

	ChatConversation rename(UUID conversationId, String title);

	void delete(UUID conversationId);

	ChatAttachment attach(UUID conversationId, UUID jobPostingId, UUID resumeVersionId);

	void detach(UUID conversationId, UUID jobPostingId);
}
