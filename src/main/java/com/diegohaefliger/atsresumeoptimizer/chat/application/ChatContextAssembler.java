package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ChatTool;
import com.diegohaefliger.atsresumeoptimizer.chat.application.tool.JobDescription;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ChatContextAssembler {

	static final String NO_ATTACHED_JOBS =
			"Vagas anexadas à conversa: nenhuma. Se precisar de uma vaga, consulte as ferramentas ou peça para o usuário anexar com @.";
	private static final String ATTACHED_HEADER = "Vagas anexadas à conversa pelo usuário:";
	private static final String SEPARATOR = "\n\n---\n\n";

	private final ChatAttachmentRepository attachments;
	private final JobStructuringService jobService;
	private final JobDescription description;
	private final List<ChatTool> tools;

	ChatContextAssembler(ChatAttachmentRepository attachments, JobStructuringService jobService, JobDescription description,
			List<ChatTool> tools) {
		this.attachments = attachments;
		this.jobService = jobService;
		this.description = description;
		this.tools = List.copyOf(tools);
	}

	String contextFor(UUID conversationId) {
		List<String> blocks = new ArrayList<>();
		for (ChatAttachmentEntity attachment : attachments.findByConversationIdOrderByCreatedAtAsc(conversationId)) {
			jobService.offer(attachment.getJobPostingId()).ifPresent(offer -> blocks.add(
					attachment.getResumeVersionId() == null
							? description.of(offer)
							: description.withResume(offer, attachment.getResumeVersionId())));
		}
		return blocks.isEmpty() ? NO_ATTACHED_JOBS : ATTACHED_HEADER + "\n\n" + String.join(SEPARATOR, blocks);
	}

	List<ChatTool> tools() {
		return tools;
	}
}
