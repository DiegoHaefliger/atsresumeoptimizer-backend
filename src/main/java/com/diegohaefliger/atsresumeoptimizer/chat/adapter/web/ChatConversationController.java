package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatConversationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/conversations")
class ChatConversationController {

	private final ChatConversationService service;
	private final ChatWebMapper mapper;

	ChatConversationController(ChatConversationService service, ChatWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<ConversationSummaryResponse> list() {
		return mapper.toResponse(service.list());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ConversationSummaryResponse create(@Valid @RequestBody CreateConversationRequest request) {
		return mapper.toResponse(service.create(request.title()));
	}

	@GetMapping("/{conversationId}")
	ConversationDetailResponse get(@PathVariable UUID conversationId) {
		return mapper.toResponse(service.get(conversationId));
	}

	@PatchMapping("/{conversationId}")
	ConversationSummaryResponse rename(@PathVariable UUID conversationId, @Valid @RequestBody RenameConversationRequest request) {
		return mapper.toResponse(service.rename(conversationId, request.title()));
	}

	@DeleteMapping("/{conversationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID conversationId) {
		service.delete(conversationId);
	}

	@PutMapping("/{conversationId}/jobs/{jobId}")
	AttachmentResponse attach(@PathVariable UUID conversationId, @PathVariable UUID jobId,
			@RequestBody(required = false) AttachJobRequest request) {
		UUID resumeVersionId = request == null ? null : request.resumeVersionId();
		return mapper.toResponse(service.attach(conversationId, jobId, resumeVersionId));
	}

	@DeleteMapping("/{conversationId}/jobs/{jobId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void detach(@PathVariable UUID conversationId, @PathVariable UUID jobId) {
		service.detach(conversationId, jobId);
	}
}
