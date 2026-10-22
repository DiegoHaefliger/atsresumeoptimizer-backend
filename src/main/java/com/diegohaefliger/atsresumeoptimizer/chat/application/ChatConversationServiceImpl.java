package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatAttachment;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversation;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversationDetail;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownChatJobException;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownConversationException;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class ChatConversationServiceImpl implements ChatConversationService {

	static final String DEFAULT_TITLE = "Nova conversa";

	private final ChatConversationRepository conversations;
	private final ChatAttachmentRepository attachments;
	private final ChatMessageRepository messages;
	private final ChatEntityMapper mapper;
	private final JobStructuringService jobService;
	private final ResumeService resumeService;
	private final Clock clock;

	ChatConversationServiceImpl(ChatConversationRepository conversations, ChatAttachmentRepository attachments,
			ChatMessageRepository messages, ChatEntityMapper mapper, JobStructuringService jobService,
			ResumeService resumeService, Clock clock) {
		this.conversations = conversations;
		this.attachments = attachments;
		this.messages = messages;
		this.mapper = mapper;
		this.jobService = jobService;
		this.resumeService = resumeService;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatConversation> list() {
		return mapper.toDomain(conversations.findAllByOrderByUpdatedAtDesc());
	}

	@Override
	@Transactional
	public ChatConversation create(String title) {
		String name = StringUtils.hasText(title) ? title.strip() : DEFAULT_TITLE;
		return mapper.toDomain(conversations.save(new ChatConversationEntity(name, clock.instant())));
	}

	@Override
	@Transactional(readOnly = true)
	public ChatConversationDetail get(UUID conversationId) {
		ChatConversationEntity conversation = require(conversationId);
		return mapper.toDetail(conversation, attachments.findByConversationIdOrderByCreatedAtAsc(conversationId),
				messages.findByConversationIdOrderByCreatedAtAsc(conversationId));
	}

	@Override
	@Transactional
	public ChatConversation rename(UUID conversationId, String title) {
		ChatConversationEntity conversation = require(conversationId);
		conversation.rename(title.strip(), clock.instant());
		return mapper.toDomain(conversation);
	}

	@Override
	@Transactional
	public void delete(UUID conversationId) {
		conversations.delete(require(conversationId));
	}

	@Override
	@Transactional
	public ChatAttachment attach(UUID conversationId, UUID jobPostingId, UUID resumeVersionId) {
		require(conversationId);
		if (jobService.offer(jobPostingId).isEmpty()) {
			throw new UnknownChatJobException(jobPostingId);
		}
		if (resumeVersionId != null) {
			resumeService.getVersionText(resumeVersionId);
		}
		ChatAttachmentEntity attachment = attachments.findByConversationIdAndJobPostingId(conversationId, jobPostingId)
				.orElseGet(() -> new ChatAttachmentEntity(conversationId, jobPostingId, resumeVersionId, clock.instant()));
		attachment.useResumeVersion(resumeVersionId);
		return mapper.toDomain(attachments.save(attachment));
	}

	@Override
	@Transactional
	public void detach(UUID conversationId, UUID jobPostingId) {
		require(conversationId);
		attachments.deleteByConversationIdAndJobPostingId(conversationId, jobPostingId);
	}

	private ChatConversationEntity require(UUID conversationId) {
		return conversations.findById(conversationId).orElseThrow(() -> new UnknownConversationException(conversationId));
	}
}
